/**
 * api.js — VaultX API Client
 * All fetch() calls to the Java backend (port 8080)
 */

const API = (() => {
  const BASE = 'http://localhost:8080/api';

  async function request(path, body) {
    try {
      const res  = await fetch(BASE + path, {
        method:  'POST',
        headers: { 'Content-Type': 'application/json' },
        body:    JSON.stringify(body)
      });
      const data = await res.json();
      return { ok: res.ok, status: res.status, ...data };
    } catch (err) {
      console.error('[VaultX API]', path, err);
      return { ok: false, success: false, message: 'Cannot reach VaultX server. Is Java running on port 8080?' };
    }
  }

  async function get(path) {
    try {
      const res  = await fetch(BASE + path);
      const data = await res.json();
      return { ok: res.ok, ...data };
    } catch (err) {
      return { ok: false, success: false, message: 'Server unreachable.' };
    }
  }

  return {
    register:         (d) => request('/register', d),
    login:            (d) => request('/login', d),
    deposit:          (d) => request('/deposit', d),
    withdraw:         (d) => request('/withdraw', d),
    transfer:         (d) => request('/transfer', d),
    getBalance:       (d) => request('/balance', d),
    getTransactions:  (d) => request('/transactions', d),
    adminAccounts:    ()  => get('/admin/accounts'),
    adminTransactions:()  => get('/admin/transactions'),
    freezeAccount:    (d) => request('/admin/freeze', d),
    deleteAccount:    (d) => request('/admin/delete', d),
  };
})();
