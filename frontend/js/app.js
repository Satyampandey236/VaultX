/**
 * app.js — VaultX Frontend Application Logic
 * Handles all UI rendering, routing, and API calls
 */

'use strict';

// ── Session State ─────────────────────────────────────────────────
let session = {
  userId: null, fullName: null, email: null, phone: null,
  role: null, accountNumber: null, accountType: null,
  balance: 0, accountStatus: null
};

// ══════════════════════════════════════════════════════════════════
//  UTILITIES
// ══════════════════════════════════════════════════════════════════

function fmt(n)     { return '₹' + Number(n).toLocaleString('en-IN', { minimumFractionDigits: 2 }); }
function fmtNum(n)  { return Number(n).toLocaleString('en-IN', { minimumFractionDigits: 2 }); }
function esc(s)     { return s == null ? '' : String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;'); }

function showToast(msg, type = 'success') {
  const t = document.createElement('div');
  t.className = `toast ${type}`;
  const icons = { success: '✓', error: '✕', info: 'ℹ' };
  t.innerHTML = `<span>${icons[type] || '●'}</span> ${esc(msg)}`;
  document.getElementById('toastContainer').appendChild(t);
  setTimeout(() => t.remove(), 4000);
}

function showMsg(elId, msg, type) {
  const el = document.getElementById(elId);
  if (el) el.innerHTML = `<div class="msg msg-${type}">${esc(msg)}</div>`;
}

function setBtn(id, loading, label) {
  const btn = document.getElementById(id);
  if (!btn) return;
  btn.disabled = loading;
  btn.innerHTML = loading
    ? '<span class="spinner"></span> Processing...'
    : label;
}

function showScreen(id) {
  document.querySelectorAll('.screen').forEach(s => s.classList.remove('active'));
  document.getElementById(id).classList.add('active');
}

function setNavActive(prefix, page) {
  document.querySelectorAll(`[id^="${prefix}nav-"]`).forEach(el =>
    el.classList.remove('active'));
  const el = document.getElementById(`${prefix}nav-${page}`);
  if (el) el.classList.add('active');
}

function closeModal() {
  document.getElementById('confirmModal').classList.remove('open');
}

function openConfirmModal(title, body, onConfirm) {
  document.getElementById('modalTitle').textContent = title;
  document.getElementById('modalBody').textContent  = body;
  document.getElementById('modalConfirmBtn').onclick = () => {
    closeModal();
    onConfirm();
  };
  document.getElementById('confirmModal').classList.add('open');
}

// ══════════════════════════════════════════════════════════════════
//  AUTH
// ══════════════════════════════════════════════════════════════════

function switchAuthTab(tab) {
  document.getElementById('tabLogin').classList.toggle('active',    tab === 'login');
  document.getElementById('tabRegister').classList.toggle('active', tab === 'register');
  document.getElementById('loginForm').classList.toggle('hidden',    tab !== 'login');
  document.getElementById('registerForm').classList.toggle('hidden', tab !== 'register');
}

function fillDemo(type) {
  if (type === 'user') {
    document.getElementById('loginEmail').value    = 'john@example.com';
    document.getElementById('loginPassword').value = 'User@123';
  } else {
    document.getElementById('loginEmail').value    = 'admin@vaultx.com';
    document.getElementById('loginPassword').value = 'admin123';
  }
}

async function doLogin() {
  const email    = document.getElementById('loginEmail').value.trim();
  const password = document.getElementById('loginPassword').value;
  if (!email || !password) { showMsg('loginMsg', 'Please enter email and password.', 'error'); return; }

  setBtn('loginBtn', true, 'Sign In to VaultX');
  const res = await API.login({ email, password });
  setBtn('loginBtn', false, 'Sign In to VaultX');

  if (!res.success) { showMsg('loginMsg', res.message || 'Login failed.', 'error'); return; }

  // Save session
  session = {
    userId: res.userId, fullName: res.fullName, email: res.email,
    phone: res.phone, role: res.role,
    accountNumber: res.accountNumber || null,
    accountType:   res.accountType   || null,
    balance:       res.balance       || 0,
    accountStatus: res.accountStatus || null
  };

  showToast(`Welcome back, ${res.fullName}!`, 'success');

  if (res.role === 'ADMIN') {
    document.getElementById('adminSidebarName').textContent = res.fullName;
    showScreen('adminPage');
    showAdminPage('overview');
    
    // Explicit toggle as requested
    document.getElementById("loginPage").style.display = "none";
    document.getElementById("adminPage").style.display = "block";
  } else {
    updateSidebar();
    showScreen('dashboardPage');
    showUserPage('overview');
    
    // Explicit toggle as requested
    document.getElementById("loginPage").style.display = "none";
    document.getElementById("dashboardPage").style.display = "block";
  }
}

async function doRegister() {
  const fullName    = document.getElementById('regName').value.trim();
  const phone       = document.getElementById('regPhone').value.trim();
  const email       = document.getElementById('regEmail').value.trim();
  const password    = document.getElementById('regPassword').value;
  const accountType = document.getElementById('regAccType').value;

  if (!fullName || !email || !password || !phone) {
    showMsg('registerMsg', 'Please fill all required fields.', 'error'); return;
  }
  if (password.length < 6) {
    showMsg('registerMsg', 'Password must be at least 6 characters.', 'error'); return;
  }

  setBtn('registerBtn', true, 'Open My Account');
  const res = await API.register({ fullName, email, phone, password, accountType });
  setBtn('registerBtn', false, 'Open My Account');

  if (!res.success) { showMsg('registerMsg', res.message || 'Registration failed.', 'error'); return; }

  showMsg('registerMsg',
    `✅ Account created! Your account no: ${res.accountNumber}. Please login.`, 'success');
  setTimeout(() => switchAuthTab('login'), 2500);
}

function logout() {
  session = { userId:null, fullName:null, email:null, phone:null,
    role:null, accountNumber:null, balance:0 };
  showScreen('loginPage');
  
  document.getElementById("dashboardPage").style.display = "none";
  const adPage = document.getElementById("adminPage");
  if (adPage) adPage.style.display = "none";
  
  document.getElementById("loginPage").style.display = "flex";
  
  showToast('Signed out successfully.', 'info');
}

// ══════════════════════════════════════════════════════════════════
//  USER PORTAL
// ══════════════════════════════════════════════════════════════════

function updateSidebar() {
  document.getElementById('sidebarName').textContent = session.fullName || '—';
  document.getElementById('sidebarAcc').textContent  =
    session.accountNumber ? maskAcc(session.accountNumber) : '—';
  document.getElementById('sidebarBal').textContent  = fmt(session.balance);
}

function maskAcc(acc) {
  if (!acc) return '—';
  return 'VAULT-****' + acc.slice(-4);
}

async function refreshBalance() {
  if (!session.accountNumber) return;
  const res = await API.getBalance({ accountNumber: session.accountNumber });
  if (res.success) {
    session.balance = res.balance;
    session.accountStatus = res.status;
    updateSidebar();
  }
}

function showUserPage(page) {
  setNavActive('', page);
  const main = document.getElementById('userMain');
  main.innerHTML = '<div style="padding:3rem 2.5rem;color:var(--muted)"><span class="spinner"></span> Loading...</div>';
  switch(page) {
    case 'overview':     renderOverview();     break;
    case 'deposit':      renderDeposit();      break;
    case 'withdraw':     renderWithdraw();     break;
    case 'transfer':     renderTransfer();     break;
    case 'transactions': renderTransactions(); break;
    case 'statement':    renderStatement();    break;
  }
}

// ── Overview ──────────────────────────────────────────────────────
async function renderOverview() {
  await refreshBalance();
  const txRes = await API.getTransactions({
    accountNumber: session.accountNumber, limit: 5 });
  const recentTxns = txRes.success ? txRes.transactions : [];

  const totalIn  = recentTxns.filter(t => t.isCredit).reduce((s,t) => s + t.amount, 0);
  const totalOut = recentTxns.filter(t => !t.isCredit).reduce((s,t) => s + t.amount, 0);

  const isFrozen = session.accountStatus === 'FROZEN';

  document.getElementById('userMain').innerHTML = `
  <div class="page-header">
    <div>
      <div class="page-title">Good day, ${esc(session.fullName?.split(' ')[0])} 👋</div>
      <div class="page-sub">Here's your financial summary</div>
    </div>
    ${isFrozen ? '<div class="status-badge frozen" style="padding:.5rem 1rem;font-size:.82rem">⚠ Account Frozen</div>' : ''}
  </div>
  <div class="content-area">

    <!-- Stats -->
    <div class="stats-grid">
      <div class="stat-card gold">
        <div class="stat-icon gold">💰</div>
        <div class="stat-val gold">${fmt(session.balance)}</div>
        <div class="stat-label">Available Balance</div>
      </div>
      <div class="stat-card green">
        <div class="stat-icon green">↓</div>
        <div class="stat-val green">${fmt(totalIn)}</div>
        <div class="stat-label">Recent Credits</div>
      </div>
      <div class="stat-card red">
        <div class="stat-icon red">↑</div>
        <div class="stat-val red">${fmt(totalOut)}</div>
        <div class="stat-label">Recent Debits</div>
      </div>
      <div class="stat-card blue">
        <div class="stat-icon blue">📋</div>
        <div class="stat-val blue">${recentTxns.length}</div>
        <div class="stat-label">Recent Transactions</div>
      </div>
    </div>

    <div class="grid-2">
      <!-- Bank Card -->
      <div>
        <div class="bank-card">
          <div class="flex items-center justify-between">
            <div>
              <div class="bank-card-chip">
                <div></div><div></div><div></div><div></div><div></div><div></div>
              </div>
            </div>
            <div class="bank-card-logo">VaultX</div>
          </div>
          <div class="bank-card-number">${formatCardNum(session.accountNumber)}</div>
          <div class="bank-card-bottom">
            <div>
              <div class="bank-card-label">Account Holder</div>
              <div class="bank-card-name">${esc(session.fullName)}</div>
              <div style="margin-top:.5rem">
                <span class="status-badge ${isFrozen ? 'frozen' : 'active'}">
                  ${isFrozen ? '❄ Frozen' : '✓ Active'}
                </span>
              </div>
            </div>
            <div style="text-align:right">
              <div class="bank-card-balance-label">Balance</div>
              <div class="bank-card-balance-val">${fmt(session.balance)}</div>
              <div style="font-size:.7rem;color:rgba(255,255,255,.4);margin-top:.2rem">
                ${esc(session.accountType)} · VLTX0001001
              </div>
            </div>
          </div>
        </div>

        <!-- Quick Actions -->
        <div class="card mt-3">
          <div class="card-header">
            <div class="card-title">⚡ Quick Actions</div>
          </div>
          <div class="card-body" style="display:grid;grid-template-columns:1fr 1fr;gap:.8rem">
            <button class="btn btn-success" onclick="showUserPage('deposit')">
              ↓ Deposit
            </button>
            <button class="btn btn-danger" onclick="showUserPage('withdraw')">
              ↑ Withdraw
            </button>
            <button class="btn btn-outline" onclick="showUserPage('transfer')" style="grid-column:span 2">
              ⇄ Transfer Money
            </button>
          </div>
        </div>
      </div>

      <!-- Recent Transactions -->
      <div class="card">
        <div class="card-header">
          <div class="card-title">🕐 Recent Transactions</div>
          <button class="btn btn-outline btn-sm" onclick="showUserPage('transactions')">View All</button>
        </div>
        ${recentTxns.length === 0
          ? '<div class="card-body"><p class="text-muted" style="font-size:.85rem">No transactions yet.</p></div>'
          : `<div class="txn-list">${recentTxns.map(t => txnItem(t)).join('')}</div>`
        }
      </div>
    </div>
  </div>`;
}

function formatCardNum(acc) {
  if (!acc) return '•••• •••• •••• ••••';
  const clean = acc.replace('VAULT-', '');
  return `VAULT ···· ···· ${clean.slice(-4)}`;
}

// ── Deposit ───────────────────────────────────────────────────────
function renderDeposit() {
  document.getElementById('userMain').innerHTML = `
  <div class="page-header">
    <div>
      <div class="page-title">Deposit Money</div>
      <div class="page-sub">Add funds to your account</div>
    </div>
  </div>
  <div class="content-area">
    <div class="op-card">
      <div class="amount-display">
        <span class="amount-symbol">₹</span>
        <input class="amount-input" type="number" id="depAmount" placeholder="0.00" min="1">
      </div>
      <div class="form-group">
        <label class="form-label">Description (optional)</label>
        <input class="form-input" type="text" id="depDesc" placeholder="e.g. Salary credit, Cash deposit">
      </div>
      <div style="background:var(--navy2);border:1px solid var(--border);border-radius:10px;padding:1rem;margin-bottom:1.2rem">
        <div style="font-size:.75rem;color:var(--muted);margin-bottom:.3rem">Depositing to</div>
        <div style="font-size:.9rem;font-weight:600">${esc(session.fullName)}</div>
        <div style="font-size:.8rem;color:var(--gold);margin-top:.1rem">${maskAcc(session.accountNumber)}</div>
      </div>
      <button class="btn btn-gold" onclick="doDeposit()" id="depBtn">Deposit Now</button>
      <div id="depMsg"></div>
    </div>
  </div>`;
}

async function doDeposit() {
  const amount = parseFloat(document.getElementById('depAmount').value);
  const desc   = document.getElementById('depDesc').value.trim();
  if (!amount || amount <= 0) { showMsg('depMsg', 'Please enter a valid amount.', 'error'); return; }

  setBtn('depBtn', true, 'Deposit Now');
  const res = await API.deposit({
    accountNumber: session.accountNumber, amount, description: desc || 'Cash Deposit' });
  setBtn('depBtn', false, 'Deposit Now');

  if (!res.success) { showMsg('depMsg', res.message, 'error'); return; }

  session.balance = res.newBalance;
  updateSidebar();
  showToast(`₹${amount.toLocaleString('en-IN')} deposited successfully!`, 'success');
  document.getElementById('depAmount').value = '';
  document.getElementById('depDesc').value   = '';
  showMsg('depMsg', `✅ ${res.message} | Txn ID: ${res.txnId}`, 'success');
}

// ── Withdraw ──────────────────────────────────────────────────────
function renderWithdraw() {
  document.getElementById('userMain').innerHTML = `
  <div class="page-header">
    <div>
      <div class="page-title">Withdraw Money</div>
      <div class="page-sub">Available: <span class="text-green" style="font-weight:700">${fmt(session.balance)}</span></div>
    </div>
  </div>
  <div class="content-area">
    <div class="op-card">
      <div class="amount-display">
        <span class="amount-symbol">₹</span>
        <input class="amount-input" type="number" id="wdAmount" placeholder="0.00" min="1">
      </div>
      <div class="form-group">
        <label class="form-label">Description (optional)</label>
        <input class="form-input" type="text" id="wdDesc" placeholder="e.g. ATM withdrawal, Bill payment">
      </div>
      <div style="background:var(--navy2);border:1px solid var(--border);border-radius:10px;padding:1rem;margin-bottom:1.2rem">
        <div class="flex justify-between" style="font-size:.83rem">
          <span class="text-muted">Account</span>
          <span>${maskAcc(session.accountNumber)}</span>
        </div>
        <div class="flex justify-between mt-1" style="font-size:.83rem">
          <span class="text-muted">Available Balance</span>
          <span class="text-green" style="font-weight:600">${fmt(session.balance)}</span>
        </div>
      </div>
      <button class="btn btn-gold" onclick="doWithdraw()" id="wdBtn">Withdraw Now</button>
      <div id="wdMsg"></div>
    </div>
  </div>`;
}

async function doWithdraw() {
  const amount = parseFloat(document.getElementById('wdAmount').value);
  const desc   = document.getElementById('wdDesc').value.trim();
  if (!amount || amount <= 0) { showMsg('wdMsg', 'Please enter a valid amount.', 'error'); return; }

  setBtn('wdBtn', true, 'Withdraw Now');
  const res = await API.withdraw({
    accountNumber: session.accountNumber, amount, description: desc || 'Cash Withdrawal' });
  setBtn('wdBtn', false, 'Withdraw Now');

  if (!res.success) { showMsg('wdMsg', res.message, 'error'); return; }

  session.balance = res.newBalance;
  updateSidebar();
  showToast(`₹${amount.toLocaleString('en-IN')} withdrawn successfully!`, 'success');
  document.getElementById('wdAmount').value = '';
  document.getElementById('wdDesc').value   = '';
  showMsg('wdMsg', `✅ ${res.message} | Txn ID: ${res.txnId}`, 'success');
}

// ── Transfer ──────────────────────────────────────────────────────
function renderTransfer() {
  document.getElementById('userMain').innerHTML = `
  <div class="page-header">
    <div>
      <div class="page-title">Transfer Money</div>
      <div class="page-sub">Send funds to another VaultX account instantly</div>
    </div>
  </div>
  <div class="content-area">
    <div class="op-card">
      <div class="form-group">
        <label class="form-label">Beneficiary Account Number *</label>
        <input class="form-input" type="text" id="tfToAcc" placeholder="VAULT-XXXXXXXX">
      </div>
      <div class="amount-display" style="margin-top:.5rem">
        <span class="amount-symbol">₹</span>
        <input class="amount-input" type="number" id="tfAmount" placeholder="0.00" min="1">
      </div>
      <div class="form-group mt-1">
        <label class="form-label">Remarks (optional)</label>
        <input class="form-input" type="text" id="tfDesc" placeholder="e.g. Rent payment, Split bill">
      </div>
      <div style="background:var(--navy2);border:1px solid var(--border);border-radius:10px;padding:1rem;margin-bottom:1.2rem">
        <div class="flex justify-between" style="font-size:.83rem">
          <span class="text-muted">From</span>
          <span>${maskAcc(session.accountNumber)}</span>
        </div>
        <div class="flex justify-between mt-1" style="font-size:.83rem">
          <span class="text-muted">Available Balance</span>
          <span class="text-green" style="font-weight:600">${fmt(session.balance)}</span>
        </div>
      </div>
      <button class="btn btn-gold" onclick="doTransfer()" id="tfBtn">Transfer Now</button>
      <div id="tfMsg"></div>
    </div>
  </div>`;
}

async function doTransfer() {
  const toAccount = document.getElementById('tfToAcc').value.trim().toUpperCase();
  const amount    = parseFloat(document.getElementById('tfAmount').value);
  const desc      = document.getElementById('tfDesc').value.trim();
  if (!toAccount) { showMsg('tfMsg', 'Please enter beneficiary account number.', 'error'); return; }
  if (!amount || amount <= 0) { showMsg('tfMsg', 'Please enter a valid amount.', 'error'); return; }
  if (toAccount === session.accountNumber) {
    showMsg('tfMsg', 'Cannot transfer to your own account.', 'error'); return;
  }

  openConfirmModal(
    'Confirm Transfer',
    `Transfer ${fmt(amount)} to account ${toAccount}?`,
    async () => {
      setBtn('tfBtn', true, 'Transfer Now');
      const res = await API.transfer({
        fromAccount: session.accountNumber, toAccount,
        amount, description: desc || 'Fund Transfer' });
      setBtn('tfBtn', false, 'Transfer Now');
      if (!res.success) { showMsg('tfMsg', res.message, 'error'); return; }
      session.balance = res.newBalance;
      updateSidebar();
      showToast(`₹${amount.toLocaleString('en-IN')} transferred!`, 'success');
      document.getElementById('tfToAcc').value = '';
      document.getElementById('tfAmount').value = '';
      showMsg('tfMsg', `✅ ${res.message} | Txn: ${res.txnId}`, 'success');
    }
  );
}

// ── Transactions ──────────────────────────────────────────────────
async function renderTransactions() {
  const res  = await API.getTransactions({
    accountNumber: session.accountNumber, limit: 50 });
  const txns = res.success ? res.transactions : [];

  document.getElementById('userMain').innerHTML = `
  <div class="page-header">
    <div>
      <div class="page-title">Transaction History</div>
      <div class="page-sub">${txns.length} transactions found</div>
    </div>
  </div>
  <div class="content-area">
    <div class="card">
      <div class="card-header">
        <div class="card-title">📋 All Transactions</div>
        <input class="form-input" type="text" id="txnSearch"
          placeholder="Search transactions..."
          style="width:220px;padding:.45rem .8rem;font-size:.82rem"
          oninput="filterTxns(this.value)">
      </div>
      <div id="txnListWrap">
        ${txns.length === 0
          ? '<div class="card-body"><p class="text-muted">No transactions found.</p></div>'
          : `<div class="txn-list" id="txnList">${txns.map(t => txnItem(t)).join('')}</div>`
        }
      </div>
    </div>
  </div>`;

  // Store for filtering
  window._txns = txns;
}

function filterTxns(q) {
  const txns = (window._txns || []).filter(t =>
    !q || t.description?.toLowerCase().includes(q.toLowerCase()) ||
    t.txnType?.toLowerCase().includes(q.toLowerCase()) ||
    t.txnId?.toLowerCase().includes(q.toLowerCase())
  );
  document.getElementById('txnList').innerHTML = txns.map(t => txnItem(t)).join('');
}

function txnItem(t) {
  const credit = t.isCredit;
  const icons  = { DEPOSIT:'↓', WITHDRAWAL:'↑', TRANSFER_IN:'←', TRANSFER_OUT:'→' };
  return `
  <div class="txn-item">
    <div class="txn-icon ${credit ? 'credit' : 'debit'}">
      ${icons[t.txnType] || '•'}
    </div>
    <div class="txn-info">
      <div class="txn-desc">${esc(t.description || t.txnType)}</div>
      <div class="txn-meta">${esc(t.txnDate)} · ${esc(t.txnId)}</div>
    </div>
    <div>
      <div class="txn-amount ${credit ? 'credit' : 'debit'}">
        ${credit ? '+' : '-'}${fmt(t.amount)}
      </div>
      <div class="txn-bal">Bal: ${fmt(t.balanceAfter)}</div>
    </div>
  </div>`;
}

// ── Statement ─────────────────────────────────────────────────────
async function renderStatement() {
  const res  = await API.getTransactions({
    accountNumber: session.accountNumber, limit: 100 });
  const txns = res.success ? res.transactions : [];

  const totalCredit = txns.filter(t =>  t.isCredit).reduce((s,t) => s+t.amount, 0);
  const totalDebit  = txns.filter(t => !t.isCredit).reduce((s,t) => s+t.amount, 0);

  document.getElementById('userMain').innerHTML = `
  <div class="page-header">
    <div>
      <div class="page-title">Bank Statement</div>
      <div class="page-sub">Complete account statement</div>
    </div>
    <button class="btn btn-outline" onclick="printStatement()">🖨 Print</button>
  </div>
  <div class="content-area" id="statementContent">
    <!-- Statement Header -->
    <div class="card mb-2" style="padding:2rem">
      <div class="flex justify-between items-center">
        <div>
          <div style="font-family:'Playfair Display',serif;font-size:1.8rem;color:var(--gold)">VaultX</div>
          <div style="font-size:.75rem;color:var(--muted);letter-spacing:.15em;text-transform:uppercase">Bank Statement</div>
        </div>
        <div style="text-align:right">
          <div style="font-size:.75rem;color:var(--muted)">Generated</div>
          <div style="font-size:.85rem">${new Date().toLocaleDateString('en-IN',{day:'2-digit',month:'long',year:'numeric'})}</div>
        </div>
      </div>
      <div style="border-top:1px solid var(--border);margin:1.2rem 0;padding-top:1.2rem;display:grid;grid-template-columns:repeat(3,1fr);gap:1rem">
        <div>
          <div style="font-size:.72rem;color:var(--muted);text-transform:uppercase;letter-spacing:.08em">Account Holder</div>
          <div style="font-weight:600;margin-top:.2rem">${esc(session.fullName)}</div>
          <div style="font-size:.8rem;color:var(--muted)">${esc(session.email)}</div>
        </div>
        <div>
          <div style="font-size:.72rem;color:var(--muted);text-transform:uppercase;letter-spacing:.08em">Account Number</div>
          <div style="font-weight:600;color:var(--gold);margin-top:.2rem">${esc(session.accountNumber)}</div>
          <div style="font-size:.8rem;color:var(--muted)">${esc(session.accountType)} · VLTX0001001</div>
        </div>
        <div>
          <div style="font-size:.72rem;color:var(--muted);text-transform:uppercase;letter-spacing:.08em">Current Balance</div>
          <div style="font-weight:700;color:var(--green);font-size:1.2rem;margin-top:.2rem">${fmt(session.balance)}</div>
        </div>
      </div>
      <div style="display:grid;grid-template-columns:1fr 1fr 1fr;gap:1rem;padding-top:1rem;border-top:1px solid var(--border)">
        <div style="text-align:center;padding:.8rem;background:rgba(45,212,160,.08);border-radius:10px">
          <div style="font-size:.72rem;color:var(--muted)">Total Credits</div>
          <div style="color:var(--green);font-weight:700;margin-top:.2rem">${fmt(totalCredit)}</div>
        </div>
        <div style="text-align:center;padding:.8rem;background:rgba(248,113,113,.08);border-radius:10px">
          <div style="font-size:.72rem;color:var(--muted)">Total Debits</div>
          <div style="color:var(--red);font-weight:700;margin-top:.2rem">${fmt(totalDebit)}</div>
        </div>
        <div style="text-align:center;padding:.8rem;background:rgba(200,169,110,.08);border-radius:10px">
          <div style="font-size:.72rem;color:var(--muted)">Net Flow</div>
          <div style="color:var(--gold);font-weight:700;margin-top:.2rem">${fmt(totalCredit - totalDebit)}</div>
        </div>
      </div>
    </div>

    <!-- Statement Table -->
    <div class="card">
      <div class="card-header"><div class="card-title">Transaction Details</div></div>
      <div style="overflow-x:auto">
        <table class="data-table">
          <thead>
            <tr>
              <th>Date</th>
              <th>Transaction ID</th>
              <th>Description</th>
              <th>Type</th>
              <th style="text-align:right">Credit (₹)</th>
              <th style="text-align:right">Debit (₹)</th>
              <th style="text-align:right">Balance (₹)</th>
            </tr>
          </thead>
          <tbody>
            ${txns.map(t => `
            <tr>
              <td style="white-space:nowrap;font-size:.8rem">${esc(t.txnDate)}</td>
              <td style="font-size:.75rem;color:var(--muted)">${esc(t.txnId)}</td>
              <td>${esc(t.description)}</td>
              <td><span class="status-badge ${t.isCredit ? 'credit' : 'debit'}">${esc(t.txnType.replace('_',' '))}</span></td>
              <td style="text-align:right;color:var(--green)">${t.isCredit  ? fmtNum(t.amount) : '—'}</td>
              <td style="text-align:right;color:var(--red)">${!t.isCredit ? fmtNum(t.amount) : '—'}</td>
              <td style="text-align:right;font-weight:600">${fmtNum(t.balanceAfter)}</td>
            </tr>`).join('')}
          </tbody>
        </table>
      </div>
    </div>
  </div>`;
}

function printStatement() {
  window.print();
}

// ══════════════════════════════════════════════════════════════════
//  ADMIN PORTAL
// ══════════════════════════════════════════════════════════════════

function showAdminPage(page) {
  setNavActive('a', page);
  const main = document.getElementById('adminMain');
  main.innerHTML = '<div style="padding:3rem 2.5rem;color:var(--muted)"><span class="spinner"></span> Loading...</div>';
  switch(page) {
    case 'overview':     renderAdminOverview();     break;
    case 'accounts':     renderAdminAccounts();     break;
    case 'transactions': renderAdminTransactions(); break;
  }
}

// ── Admin Overview ────────────────────────────────────────────────
async function renderAdminOverview() {
  const [accRes, txnRes] = await Promise.all([
    API.adminAccounts(), API.adminTransactions()
  ]);
  const accounts = accRes.success  ? accRes.accounts     : [];
  const txns     = txnRes.success  ? txnRes.transactions : [];

  const totalBalance = accounts.reduce((s,a) => s + a.balance, 0);
  const frozen       = accounts.filter(a => a.status === 'FROZEN').length;
  const todayTxns    = txns.filter(t => t.txnDate.includes(new Date().toLocaleDateString('en-IN',{day:'2-digit',month:'short'}))).length;

  document.getElementById('adminMain').innerHTML = `
  <div class="page-header">
    <div>
      <div class="page-title">Admin Dashboard</div>
      <div class="page-sub">VaultX Banking System — Full Control</div>
    </div>
    <div class="status-badge active" style="padding:.5rem 1rem">System Active</div>
  </div>
  <div class="content-area">
    <div class="stats-grid">
      <div class="stat-card blue">
        <div class="stat-icon blue">👥</div>
        <div class="stat-val blue">${accounts.length}</div>
        <div class="stat-label">Total Accounts</div>
      </div>
      <div class="stat-card gold">
        <div class="stat-icon gold">💳</div>
        <div class="stat-val gold">${fmt(totalBalance)}</div>
        <div class="stat-label">Total Deposits</div>
      </div>
      <div class="stat-card red">
        <div class="stat-icon red">❄</div>
        <div class="stat-val red">${frozen}</div>
        <div class="stat-label">Frozen Accounts</div>
      </div>
      <div class="stat-card green">
        <div class="stat-icon green">📊</div>
        <div class="stat-val green">${txns.length}</div>
        <div class="stat-label">Total Transactions</div>
      </div>
    </div>

    <!-- Recent Accounts -->
    <div class="card mb-2">
      <div class="card-header">
        <div class="card-title">👤 Recent Accounts</div>
        <button class="btn btn-outline btn-sm" onclick="showAdminPage('accounts')">View All</button>
      </div>
      <div style="overflow-x:auto">
        <table class="data-table">
          <thead><tr><th>Name</th><th>Account No.</th><th>Type</th><th>Balance</th><th>Status</th></tr></thead>
          <tbody>
            ${accounts.slice(0,5).map(a => `
            <tr>
              <td><div style="font-weight:500">${esc(a.fullName)}</div><div style="font-size:.75rem;color:var(--muted)">${esc(a.email)}</div></td>
              <td style="font-size:.82rem;color:var(--gold)">${esc(a.accountNumber)}</td>
              <td>${esc(a.accountType)}</td>
              <td style="font-weight:600">${fmt(a.balance)}</td>
              <td><span class="status-badge ${a.status.toLowerCase()}">${esc(a.status)}</span></td>
            </tr>`).join('')}
          </tbody>
        </table>
      </div>
    </div>

    <!-- Recent Transactions -->
    <div class="card">
      <div class="card-header">
        <div class="card-title">📋 Recent Transactions</div>
        <button class="btn btn-outline btn-sm" onclick="showAdminPage('transactions')">View All</button>
      </div>
      <div class="txn-list">
        ${txns.slice(0,5).map(t => `
        <div class="txn-item">
          <div class="txn-icon ${t.isCredit ? 'credit' : 'debit'}">${t.isCredit ? '↓' : '↑'}</div>
          <div class="txn-info">
            <div class="txn-desc">${esc(t.fullName)} — ${esc(t.description)}</div>
            <div class="txn-meta">${esc(t.accountNumber)} · ${esc(t.txnDate)}</div>
          </div>
          <div class="txn-amount ${t.isCredit ? 'credit' : 'debit'}">
            ${t.isCredit ? '+' : '-'}${fmt(t.amount)}
          </div>
        </div>`).join('')}
      </div>
    </div>
  </div>`;
}

// ── Admin Accounts ────────────────────────────────────────────────
async function renderAdminAccounts() {
  const res      = await API.adminAccounts();
  const accounts = res.success ? res.accounts : [];

  document.getElementById('adminMain').innerHTML = `
  <div class="page-header">
    <div>
      <div class="page-title">All Accounts</div>
      <div class="page-sub">${accounts.length} accounts registered</div>
    </div>
    <input class="form-input" type="text" id="accSearch"
      placeholder="Search accounts..."
      style="width:240px;padding:.5rem .9rem;font-size:.82rem"
      oninput="filterAccounts(this.value)">
  </div>
  <div class="content-area">
    <div class="card">
      <div style="overflow-x:auto">
        <table class="data-table" id="accTable">
          <thead>
            <tr>
              <th>Account Holder</th>
              <th>Account Number</th>
              <th>Type</th>
              <th>Balance</th>
              <th>Status</th>
              <th>Created</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody id="accTableBody">
            ${renderAccRows(accounts)}
          </tbody>
        </table>
      </div>
    </div>
  </div>`;
  window._accounts = accounts;
}

function renderAccRows(accounts) {
  if (accounts.length === 0) return '<tr><td colspan="7" style="text-align:center;color:var(--muted);padding:2rem">No accounts found.</td></tr>';
  return accounts.map(a => `
  <tr id="row-${a.accountNumber.replace('-','_')}">
    <td>
      <div style="font-weight:500">${esc(a.fullName)}</div>
      <div style="font-size:.75rem;color:var(--muted)">${esc(a.email)}</div>
    </td>
    <td style="color:var(--gold);font-size:.82rem;font-weight:500">${esc(a.accountNumber)}</td>
    <td>${esc(a.accountType)}</td>
    <td style="font-weight:600">${fmt(a.balance)}</td>
    <td><span class="status-badge ${a.status.toLowerCase()}" id="badge-${a.accountNumber.replace('-','_')}">${esc(a.status)}</span></td>
    <td style="font-size:.78rem;color:var(--muted)">${new Date(a.createdAt).toLocaleDateString('en-IN')}</td>
    <td>
      <div class="flex gap-2">
        <button class="btn btn-outline btn-sm"
          onclick="toggleFreeze('${esc(a.accountNumber)}','${a.status}')"
          id="freeze-${a.accountNumber.replace('-','_')}">
          ${a.status === 'FROZEN' ? '▶ Unfreeze' : '❄ Freeze'}
        </button>
        <button class="btn btn-danger btn-sm"
          onclick="confirmDelete('${esc(a.accountNumber)}')">
          🗑 Delete
        </button>
      </div>
    </td>
  </tr>`).join('');
}

function filterAccounts(q) {
  const filtered = (window._accounts || []).filter(a =>
    !q ||
    a.fullName?.toLowerCase().includes(q.toLowerCase()) ||
    a.email?.toLowerCase().includes(q.toLowerCase()) ||
    a.accountNumber?.toLowerCase().includes(q.toLowerCase())
  );
  document.getElementById('accTableBody').innerHTML = renderAccRows(filtered);
}

async function toggleFreeze(accountNumber, currentStatus) {
  const freeze = currentStatus !== 'FROZEN';
  openConfirmModal(
    freeze ? 'Freeze Account' : 'Unfreeze Account',
    `${freeze ? 'Freeze' : 'Unfreeze'} account ${accountNumber}?`,
    async () => {
      const res = await API.freezeAccount({ accountNumber, freeze });
      if (!res.success) { showToast(res.message, 'error'); return; }
      showToast(res.message, 'success');
      const key    = accountNumber.replace('-','_');
      const badge  = document.getElementById('badge-'  + key);
      const btn    = document.getElementById('freeze-' + key);
      if (badge) { badge.textContent = freeze ? 'FROZEN' : 'ACTIVE'; badge.className = `status-badge ${freeze ? 'frozen' : 'active'}`; }
      if (btn)   btn.textContent = freeze ? '▶ Unfreeze' : '❄ Freeze';
      // Update local data
      const acc = (window._accounts || []).find(a => a.accountNumber === accountNumber);
      if (acc) acc.status = freeze ? 'FROZEN' : 'ACTIVE';
    }
  );
}

function confirmDelete(accountNumber) {
  openConfirmModal(
    'Delete Account',
    `Permanently delete account ${accountNumber}? All transactions will be erased.`,
    async () => {
      const res = await API.deleteAccount({ accountNumber });
      if (!res.success) { showToast(res.message, 'error'); return; }
      showToast(res.message, 'success');
      window._accounts = (window._accounts || []).filter(a => a.accountNumber !== accountNumber);
      document.getElementById('accTableBody').innerHTML = renderAccRows(window._accounts);
    }
  );
}

// ── Admin Transactions ────────────────────────────────────────────
async function renderAdminTransactions() {
  const res  = await API.adminTransactions();
  const txns = res.success ? res.transactions : [];

  document.getElementById('adminMain').innerHTML = `
  <div class="page-header">
    <div>
      <div class="page-title">All Transactions</div>
      <div class="page-sub">${txns.length} transactions in system</div>
    </div>
    <input class="form-input" type="text" id="adminTxnSearch"
      placeholder="Search transactions..."
      style="width:240px;padding:.5rem .9rem;font-size:.82rem"
      oninput="filterAdminTxns(this.value)">
  </div>
  <div class="content-area">
    <div class="card">
      <div style="overflow-x:auto">
        <table class="data-table">
          <thead>
            <tr>
              <th>Date</th>
              <th>Txn ID</th>
              <th>Account Holder</th>
              <th>Account No.</th>
              <th>Type</th>
              <th>Description</th>
              <th style="text-align:right">Amount</th>
              <th style="text-align:right">Balance After</th>
            </tr>
          </thead>
          <tbody id="adminTxnBody">
            ${renderTxnRows(txns)}
          </tbody>
        </table>
      </div>
    </div>
  </div>`;
  window._adminTxns = txns;
}

function renderTxnRows(txns) {
  if (txns.length === 0) return '<tr><td colspan="8" style="text-align:center;color:var(--muted);padding:2rem">No transactions found.</td></tr>';
  return txns.map(t => `
  <tr>
    <td style="white-space:nowrap;font-size:.78rem">${esc(t.txnDate)}</td>
    <td style="font-size:.72rem;color:var(--muted)">${esc(t.txnId)}</td>
    <td style="font-size:.83rem">${esc(t.fullName)}</td>
    <td style="color:var(--gold);font-size:.78rem">${esc(t.accountNumber)}</td>
    <td><span class="status-badge ${t.isCredit ? 'credit' : 'debit'}" style="font-size:.68rem">${esc(t.txnType.replace('_',' '))}</span></td>
    <td style="font-size:.82rem">${esc(t.description)}</td>
    <td style="text-align:right;font-weight:600;color:${t.isCredit ? 'var(--green)' : 'var(--red)'}">
      ${t.isCredit ? '+' : '-'}${fmt(t.amount)}
    </td>
    <td style="text-align:right">${fmt(t.balanceAfter)}</td>
  </tr>`).join('');
}

function filterAdminTxns(q) {
  const filtered = (window._adminTxns || []).filter(t =>
    !q ||
    t.fullName?.toLowerCase().includes(q.toLowerCase()) ||
    t.accountNumber?.toLowerCase().includes(q.toLowerCase()) ||
    t.txnId?.toLowerCase().includes(q.toLowerCase()) ||
    t.description?.toLowerCase().includes(q.toLowerCase())
  );
  document.getElementById('adminTxnBody').innerHTML = renderTxnRows(filtered);
}
