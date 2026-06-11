/**
 * Anti-OOP Bank UI
 *
 * Architecture follows CLAUDE.md:
 *   - Pure functions: data transform, formatting, row builders (no DOM, no fetch)
 *   - Side-effect functions: api.* (HTTP), dom.* (DOM mutation)
 *   - Orchestration: event handlers — 1. gather inputs → 2. call api → 3. render result
 */

/* ========================================================================
 *  PURE FUNCTIONS — transform data, zero side effects
 * ======================================================================== */

/** Format a balance string (e.g. "1500.00" or "-1000.00") → styled span */
const formatBalance = (balance) => {
    const n = parseFloat(balance);
    const cls = n < 0 ? 'error' : n > 0 ? 'success' : '';
    return `<span class="${cls}">${Number(n).toFixed(2)}</span>`;
};

/** Format ISO date (yyyy-mm-dd) → readable */
const formatDate = (iso) => {
    if (!iso) return '';
    const [y, m, d] = iso.split('-');
    return `${d}.${m}.${y}`;
};

/** Pure: build a client table row from a client fact */
const clientToRow = (c) =>
    `<tr><td>${c.id}</td><td>${c.username}</td><td>${formatDate(c.birthDate)}</td></tr>`;

/** Pure: build an access result row from an access fact */
const accessToRow = (a) =>
    `<tr>
        <td>${a.accountNo}</td>
        <td>${a.accountName}</td>
        <td>${a.isOwner ? 'Owner' : 'Manager'}</td>
        <td>${formatBalance(a.accountBalance)}</td>
    </tr>`;

/** Pure: build a single-line result message */
const successMsg = (msg) => `<span class="success">✓ ${msg}</span>`;
const errorMsg = (msg) => `<span class="error">✗ ${msg}</span>`;

/** Pure: build a client table from an array of client facts */
const clientsToTable = (clients) => {
    if (!clients || clients.length === 0) return '<p>No clients found.</p>';
    return `<table>
        <tr><th>ID</th><th>Username</th><th>Birth Date</th></tr>
        ${clients.map(clientToRow).join('')}
    </table>`;
};

/* ========================================================================
 *  SIDE-EFFECT FUNCTIONS — HTTP calls
 * ======================================================================== */

const BASE = '';

const api = {

    /** Call an endpoint, return parsed JSON or text. Throw on non-ok. */
    _fetch: async (url, opts = {}) => {
        const res = await fetch(url, opts);
        const text = await res.text();
        if (!res.ok) {
            // Try to extract a clean error message
            const clean = text.replace(/^.*"message":"([^"]+)".*$/, '$1');
            throw new Error(clean || text || `HTTP ${res.status}`);
        }
        try { return JSON.parse(text); } catch (_) { return text; }
    },

    /** Get X-Username header value from the identity input */
    _username: () => document.getElementById('my-username').value.trim(),

    _headers: function () {
        const u = this._username();
        return u ? { 'X-Username': u, 'Content-Type': 'application/json' }
                  : { 'Content-Type': 'application/json' };
    },

    // ---- Banker ----

    createClient: (username, birthDate) =>
        api._fetch(`${BASE}/bank/client`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, birthDate })
        }),

    deleteClient: (username) =>
        api._fetch(`${BASE}/bank/client/${encodeURIComponent(username)}`, { method: 'DELETE' }),

    findClients: (fromBirth, minBalance) => {
        const params = new URLSearchParams();
        if (fromBirth) params.set('fromBirth', fromBirth);
        if (minBalance) params.set('minBalance', minBalance);
        const qs = params.toString();
        return api._fetch(`${BASE}/bank/client${qs ? '?' + qs : ''}`);
    },

    // ---- Client ----

    createAccount: (name) =>
        api._fetch(`${BASE}/client/account`, {
            method: 'POST',
            headers: api._headers(),
            body: JSON.stringify({ name })
        }),

    deposit: (accountNo, amount) =>
        api._fetch(`${BASE}/client/deposit`, {
            method: 'POST',
            headers: api._headers(),
            body: JSON.stringify({ accountNo: Number(accountNo), amount: Number(amount) })
        }),

    transfer: (src, dst, amount) =>
        api._fetch(`${BASE}/client/transfer`, {
            method: 'POST',
            headers: api._headers(),
            body: JSON.stringify({
                sourceAccountNo: Number(src),
                destinationAccountNo: Number(dst),
                amount: Number(amount)
            })
        }),

    addManager: (accountNo, managerUsername) =>
        api._fetch(`${BASE}/client/manager`, {
            method: 'POST',
            headers: api._headers(),
            body: JSON.stringify({ accountNo: Number(accountNo), username: managerUsername })
        }),

    accountsReport: () =>
        api._fetch(`${BASE}/client/account`, { headers: api._headers() }),
};

/* ========================================================================
 *  SIDE-EFFECT FUNCTIONS — DOM manipulation
 * ======================================================================== */

const dom = {
    get: (id) => document.getElementById(id),
    val: (id) => dom.get(id).value.trim(),
    clear: (id) => { dom.get(id).innerHTML = ''; },
    show: (id, html) => { dom.get(id).innerHTML = html; },
    showLoading: (id) => { dom.get(id).innerHTML = '<span class="loading">Loading...</span>'; },
    showErr: (id, err) => { dom.get(id).innerHTML = errorMsg(err.message || err); },
};

/* ========================================================================
 *  ORCHESTRATION — event handlers: gather → call → render
 * ======================================================================== */

// ---- Banker: create client ----
dom.get('btn-create-client').onclick = async () => {
    const username = dom.val('create-username');
    const birthDate = dom.val('create-birthdate');
    const rid = 'create-client-result';
    if (!username || !birthDate) { dom.showErr(rid, new Error('Both fields required.')); return; }
    dom.showLoading(rid);
    try {
        const c = await api.createClient(username, birthDate);
        dom.show(rid, successMsg(`Client created: ID=${c.id}, ${c.username}, ${formatDate(c.birthDate)}`));
    } catch (e) { dom.showErr(rid, e); }
};

// ---- Banker: delete client ----
dom.get('btn-delete-client').onclick = async () => {
    const username = dom.val('delete-username');
    const rid = 'delete-client-result';
    if (!username) { dom.showErr(rid, new Error('Username required.')); return; }
    dom.showLoading(rid);
    try {
        await api.deleteClient(username);
        dom.show(rid, successMsg(`Client "${username}" deleted.`));
    } catch (e) { dom.showErr(rid, e); }
};

// ---- Banker: find clients ----
const doFindClients = async (fromBirth, minBalance) => {
    const rid = 'find-clients-result';
    dom.showLoading(rid);
    try {
        const clients = await api.findClients(fromBirth, minBalance);
        dom.show(rid, clientsToTable(clients));
    } catch (e) { dom.showErr(rid, e); }
};
dom.get('btn-find-clients').onclick = () =>
    doFindClients(dom.val('find-from-birth'), dom.val('find-min-balance'));
dom.get('btn-find-all').onclick = () => doFindClients('', '');

// ---- Client: create account ----
dom.get('btn-create-account').onclick = async () => {
    const name = dom.val('acct-name');
    const rid = 'create-account-result';
    if (!name) { dom.showErr(rid, new Error('Account name required.')); return; }
    dom.showLoading(rid);
    try {
        const a = await api.createAccount(name);
        dom.show(rid, successMsg(`Account created: No.${a.accountNo} "${a.accountName}"`));
    } catch (e) { dom.showErr(rid, e); }
};

// ---- Client: deposit ----
dom.get('btn-deposit').onclick = async () => {
    const acct = dom.val('dep-acct-no');
    const amt = dom.val('dep-amount');
    const rid = 'deposit-result';
    if (!acct || !amt) { dom.showErr(rid, new Error('Both fields required.')); return; }
    dom.showLoading(rid);
    try {
        await api.deposit(acct, amt);
        dom.show(rid, successMsg(`Deposited ${amt} EUR to account ${acct}.`));
    } catch (e) { dom.showErr(rid, e); }
};

// ---- Client: transfer ----
dom.get('btn-transfer').onclick = async () => {
    const src = dom.val('xfer-src');
    const dst = dom.val('xfer-dst');
    const amt = dom.val('xfer-amount');
    const rid = 'transfer-result';
    if (!src || !dst || !amt) { dom.showErr(rid, new Error('All fields required.')); return; }
    dom.showLoading(rid);
    try {
        await api.transfer(src, dst, amt);
        dom.show(rid, successMsg(`Transferred ${amt} EUR from ${src} to ${dst}.`));
    } catch (e) { dom.showErr(rid, e); }
};

// ---- Client: add manager ----
dom.get('btn-add-manager').onclick = async () => {
    const acct = dom.val('mgr-acct-no');
    const mgr = dom.val('mgr-username');
    const rid = 'add-manager-result';
    if (!acct || !mgr) { dom.showErr(rid, new Error('Both fields required.')); return; }
    dom.showLoading(rid);
    try {
        const a = await api.addManager(acct, mgr);
        dom.show(rid, successMsg(`"${mgr}" is now manager of account ${acct}.`));
    } catch (e) { dom.showErr(rid, e); }
};

// ---- Client: accounts report ----
dom.get('btn-report').onclick = async () => {
    const rid = 'report-result';
    dom.showLoading(rid);
    try {
        const report = await api.accountsReport();
        dom.show(rid, report);
    } catch (e) { dom.showErr(rid, e); }
};
