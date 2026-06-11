/**
 * Anti-OOP Bank UI — role-separated login → dashboard.
 *
 * Architecture:
 *   Pure functions    — F.*     (data transform, zero side effects)
 *   Side-effect       — api.*   (HTTP), dom.* (DOM), S.* (session)
 *   Orchestration     — event handlers (gather → call → render)
 */

/* =====================================================================
 *  PURE FUNCTIONS — F
 * ===================================================================== */

const F = {
    m: (n) => Number(n).toFixed(2),
    bal: (n) => `<span class="${n < 0 ? 'error' : n > 0 ? 'success' : ''}">${F.m(n)}</span>`,
    date: (iso) => { if (!iso) return ''; const [y, m, d] = iso.split('-'); return `${d}.${m}.${y}`; },
    ok: (m) => `<span class="success">✓ ${m}</span>`,
    err: (m) => `<span class="error">✗ ${m}</span>`,

    badge: (t, cls) => `<span class="badge ${cls}">${t}</span>`,
    typeBadge: (t) => F.badge(t, t === 'SAVINGS' ? 'badge-savings' : 'badge-checking'),
    roleBadge: (o) => F.badge(o ? 'Owner' : 'Manager', o ? 'badge-owner' : 'badge-manager'),

    clientsTable: (list) => {
        if (!list || !list.length) return '<p>No clients.</p>';
        return `<table><tr><th>ID</th><th>Username</th><th>Birth</th></tr>
            ${list.map(c => `<tr><td>${c.id}</td><td>${c.username}</td><td>${F.date(c.birthDate)}</td></tr>`).join('')}
        </table>`;
    },

    accountsTable: (list) => {
        if (!list || !list.length) return '<p>No accounts yet. Open one above.</p>';
        return `<table><tr><th>#</th><th>Name</th><th>Type</th><th>Balance</th><th>Role</th></tr>
            ${list.map(a => `<tr>
                <td>${a.accountNo}</td><td>${a.accountName}</td>
                <td>${F.typeBadge(a.accountType)}</td><td>${F.bal(a.balance)}</td>
                <td>${F.roleBadge(a.isOwner)}</td>
            </tr>`).join('')}</table>`;
    },

    /** Client-facing rates — simplified, human-readable. */
    ratesTable: (rules) => {
        if (!rules || !rules.length) return '';
        const r = (k) => rules.find(x => x.key === k)?.value ?? '—';
        const pct = (k) => F.m(r(k)) + '%';
        const eur = (k) => F.m(r(k)) + ' €';
        return `<table>
            <tr><td>Savings interest rate</td><td>${pct('interest.rate')} per year</td></tr>
            <tr><td>Interest tax rate</td><td>${pct('interest.tax.rate')}</td></tr>
            <tr><td>Interest tax exemption</td><td>${eur('interest.tax.exemption')} per year</td></tr>
            <tr><td>Internal transfer fee</td><td>${eur('transfer.fee.internal')}</td></tr>
            <tr><td>External transfer fee</td><td>${eur('transfer.fee.external.flat')} + ${pct('transfer.fee.external.percent')} of amount</td></tr>
            <tr><td>Large transfer tax threshold</td><td>${eur('transfer.tax.threshold')}</td></tr>
            <tr><td>Large transfer tax rate</td><td>${pct('transfer.tax.rate')} on excess</td></tr>
        </table>`;
    },

    /** Banker-facing rules — editable table with save buttons. */
    rulesTable: (rules) => {
        if (!rules || !rules.length) return '<p>No rules.</p>';
        return `<table><tr><th>Key</th><th>Value</th><th>Description</th><th></th></tr>
            ${rules.map(r => `<tr>
                <td><code>${r.key}</code></td>
                <td>${Number(r.value).toFixed(4)}</td>
                <td style="font-size:0.78rem;color:var(--muted)">${r.description||''}</td>
                <td><span class="rule-edit-row">
                    <input type="number" id="rv-${r.key.replace(/\./g,'-')}" value="${r.value}" step="0.01">
                    <button class="small rule-save-btn" data-key="${r.key}">Save</button>
                </span></td>
            </tr>`).join('')}</table>`;
    },

    transfersTable: (list) => {
        if (!list || !list.length) return '<p>No transfers yet.</p>';
        return `<table><tr><th>ID</th><th>From</th><th>To</th><th>Amount</th><th>Fee</th><th>Penalty</th><th>Tax</th><th>Type</th><th>Date</th></tr>
            ${list.map(t => `<tr>
                <td>${t.id}</td><td>#${t.source}</td><td>#${t.destination}</td>
                <td>${F.m(t.amount)}</td><td>${F.m(t.fee)}</td>
                <td>${t.penalty>0?`<span class="warn">${F.m(t.penalty)}</span>`:'—'}</td>
                <td>${t.tax>0?`<span class="warn">${F.m(t.tax)}</span>`:'—'}</td>
                <td>${t.isInternal?'Internal':'<span class="warn">External</span>'}</td>
                <td>${t.date}</td>
            </tr>`).join('')}</table>`;
    },

    interestTable: (list) => {
        if (!list || !list.length) return '<p>No savings accounts.</p>';
        let tg=0, tt=0, tn=0;
        const rows = list.map(r => { tg+=r.grossInterest; tt+=r.taxAmount; tn+=r.netInterest;
            return `<tr><td>#${r.accountNo}</td><td>${r.accountName}</td><td>${F.bal(r.balance)}</td>
                <td>${F.m(r.grossInterest)}</td><td>${F.m(r.taxableInterest)}</td>
                <td>${F.m(r.taxAmount)}</td><td>${F.bal(r.netInterest)}</td></tr>`;
        }).join('');
        return `<table><tr><th>Acc</th><th>Name</th><th>Balance</th><th>Gross</th><th>Taxable</th><th>Tax</th><th>Net</th></tr>
            ${rows}<tr style="font-weight:600;border-top:2px solid var(--border);background:#f9fafb">
                <td colspan="3">Totals</td><td>${F.m(tg)}</td><td></td><td>${F.m(tt)}</td><td>${F.m(tn)}</td></tr></table>`;
    },

    feeBreakdown: (r) => {
        const p = [];
        if (r.baseFee > 0) p.push(`Base: ${F.m(r.baseFee)}€`);
        if (r.excessPenalty > 0) p.push(`Penalty: ${F.m(r.excessPenalty)}€`);
        if (r.transactionTax > 0) p.push(`Tax: ${F.m(r.transactionTax)}€`);
        return p.length
            ? `<div class="fee-breakdown">${p.join(' | ')} | <strong>Total fee: ${F.m(r.totalFee)}€</strong></div>`
            : `<div class="fee-breakdown">No fees</div>`;
    }
};

/* =====================================================================
 *  SIDE-EFFECT: DOM — D
 * ===================================================================== */

const D = {
    get: (id) => document.getElementById(id),
    val: (id) => D.get(id)?.value.trim() ?? '',
    checked: (id) => D.get(id)?.checked ?? false,
    show: (id, h) => { const e = D.get(id); if (e) e.innerHTML = h; },
    load: (id) => D.show(id, '<span class="loading">Loading...</span>'),
    ok: (id, m) => D.show(id, F.ok(m)),
    err: (id, e) => D.show(id, F.err(e.message || e)),
};

/* =====================================================================
 *  SIDE-EFFECT: SESSION — S
 * ===================================================================== */

const S = {
    _key: null,
    _user: null,
    _role: null,

    bankerKey: () => S._key,
    setBanker: (key) => { S._key = key; S._role = 'banker'; },
    setClient: (user) => { S._user = user; S._role = 'client'; },
    user: () => S._user,
    role: () => S._role,
    clear: () => { S._key = null; S._user = null; S._role = null; }
};

/* =====================================================================
 *  SIDE-EFFECT: API
 * ===================================================================== */

const BASE = '';

const api = {
    _fetch: async (url, opts = {}) => {
        const res = await fetch(url, opts);
        const text = await res.text();
        if (!res.ok) {
            const clean = text.replace(/^.*"message":"([^"]+)".*$/, '$1');
            throw new Error(clean || text || `HTTP ${res.status}`);
        }
        try { return JSON.parse(text); } catch (_) { return text; }
    },

    // ---- Banker auth ----
    bankerLogin: (u, p) => api._fetch(`${BASE}/bank/login`, {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: u, password: p }) }),

    _bkHeaders: () => {
        const h = { 'Content-Type': 'application/json' };
        if (S.bankerKey()) h['X-Banker-Key'] = S.bankerKey();
        return h;
    },

    // ---- Banker operations (authenticated) ----
    createClient: (u, b) => api._fetch(`${BASE}/bank/client`, {
        method: 'POST', headers: api._bkHeaders(),
        body: JSON.stringify({ username: u, birthDate: b }) }),
    deleteClient: (u) => api._fetch(`${BASE}/bank/client/${encodeURIComponent(u)}`, {
        method: 'DELETE', headers: api._bkHeaders() }),
    updateRule: (k, v) => api._fetch(`${BASE}/bank/rules`, {
        method: 'PUT', headers: api._bkHeaders(),
        body: JSON.stringify({ ruleKey: k, value: v }) }),
    accrueInterest: (d) => api._fetch(`${BASE}/bank/accrue-interest`, {
        method: 'POST', headers: api._bkHeaders(),
        body: JSON.stringify({ days: d }) }),

    // ---- Public read endpoints ----
    findClients: (birth, bal) => {
        const p = new URLSearchParams();
        if (birth) p.set('fromBirth', birth);
        if (bal) p.set('minBalance', bal);
        const q = p.toString();
        return api._fetch(`${BASE}/bank/client${q ? '?' + q : ''}`);
    },
    getRules: () => api._fetch(`${BASE}/bank/rules`),
    allTransfers: (n) => api._fetch(`${BASE}/bank/transfers?limit=${n || 20}`),

    // ---- Client (X-Username auth) ----
    _clHeaders: () => {
        const u = S.user();
        return u ? { 'X-Username': u, 'Content-Type': 'application/json' } : { 'Content-Type': 'application/json' };
    },
    myAccounts: () => api._fetch(`${BASE}/client/accounts`, { headers: api._clHeaders() }),
    createAccount: (n, t) => api._fetch(`${BASE}/client/account`, {
        method: 'POST', headers: api._clHeaders(),
        body: JSON.stringify({ name: n, accountType: t }) }),
    deposit: (acct, amt) => api._fetch(`${BASE}/client/deposit`, {
        method: 'POST', headers: api._clHeaders(),
        body: JSON.stringify({ accountNo: Number(acct), amount: Number(amt) }) }),
    transfer: (src, dst, amt, internal) => api._fetch(`${BASE}/client/transfer`, {
        method: 'POST', headers: api._clHeaders(),
        body: JSON.stringify({ sourceAccountNo: Number(src), destinationAccountNo: Number(dst),
            amount: Number(amt), isInternal: internal }) }),
    addManager: (acct, mgr) => api._fetch(`${BASE}/client/manager`, {
        method: 'POST', headers: api._clHeaders(),
        body: JSON.stringify({ accountNo: Number(acct), username: mgr }) }),
    myTransfers: () => api._fetch(`${BASE}/client/transfers`, { headers: api._clHeaders() }),
};

/* =====================================================================
 *  ORCHESTRATION: Screen switching
 * ===================================================================== */

function showScreen(name) {
    document.querySelectorAll('.screen').forEach(s => s.classList.remove('active'));
    D.get('screen-' + name).classList.add('active');
    // Clear all operation-result divs when switching screens
    clearAllResults();
}

/** Clear all operation result messages and login inputs so they don't persist across sessions. */
function clearAllResults() {
    var ids = [
        'login-error',
        'cl-create-account-result', 'cl-deposit-result', 'cl-transfer-result', 'cl-add-manager-result',
        'bk-create-client-result', 'bk-delete-client-result', 'bk-find-result', 'bk-interest-result'
    ];
    ids.forEach(function(id) { D.show(id, ''); });
    // Clear login form inputs on screen switch
    ['login-client-user', 'login-banker-user', 'login-banker-pass'].forEach(function(id) {
        var el = D.get(id); if (el) el.value = '';
    });
}

/* =====================================================================
 *  ORCHESTRATION: Login screen
 * ===================================================================== */

// Role toggle buttons
document.querySelectorAll('.login-role').forEach(btn => {
    btn.addEventListener('click', () => {
        document.querySelectorAll('.login-role').forEach(b => b.classList.remove('active'));
        document.querySelectorAll('.login-form').forEach(f => f.classList.remove('active'));
        btn.classList.add('active');
        D.get('login-form-' + btn.dataset.role).classList.add('active');
        D.show('login-error', '');
    });
});

// Client login
D.get('btn-login-client').onclick = async () => {
    const u = D.val('login-client-user');
    if (!u) { D.err('login-error', 'Enter a username.'); return; }
    D.load('login-error');
    S.setClient(u);
    try {
        await api.myAccounts();
        showScreen('client');
        D.show('cl-nav-user', u);
        await loadClientDashboard();
    } catch (e) {
        S.clear();
        D.err('login-error', `Client "${u}" not found. Ask a banker to register you first.`);
    }
};

// Banker login
D.get('btn-login-banker').onclick = async () => {
    const u = D.val('login-banker-user'), p = D.val('login-banker-pass');
    if (!u || !p) { D.err('login-error', 'Enter username and password.'); return; }
    D.load('login-error');
    try {
        const r = await api.bankerLogin(u, p);
        S.setBanker(r.token);
        showScreen('banker');
        await loadBankerDashboard();
    } catch (e) {
        S.clear();
        D.err('login-error', 'Invalid credentials. Demo: banker / banker123');
    }
};

// Allow Enter key on login inputs
['login-client-user', 'login-banker-user', 'login-banker-pass'].forEach(id => {
    D.get(id).addEventListener('keydown', (e) => {
        if (e.key === 'Enter') {
            if (S.role() === 'banker' || id.includes('banker')) D.get('btn-login-banker').click();
            else D.get('btn-login-client').click();
        }
    });
});

/* =====================================================================
 *  ORCHESTRATION: Client dashboard
 * ===================================================================== */

async function loadClientDashboard() {
    try {
        const [accounts, rules] = await Promise.all([api.myAccounts(), api.getRules()]);
        D.show('cl-accounts-table', F.accountsTable(accounts));
        D.show('cl-rates-table', F.ratesTable(rules));
        D.show('cl-transfers-table', F.transfersTable(await api.myTransfers()));
    } catch (e) { /* silently skip — user can retry */ }
}

// Logout
D.get('btn-cl-logout').onclick = () => { S.clear(); showScreen('login'); };

// Refresh buttons
D.get('btn-cl-refresh-accounts').onclick = loadClientDashboard;
D.get('btn-cl-refresh-transfers').onclick = loadClientDashboard;

// Open account
D.get('btn-cl-create-account').onclick = async () => {
    const n = D.val('cl-acct-name'), t = D.get('cl-acct-type').value;
    const rid = 'cl-create-account-result';
    if (!n) { D.err(rid, 'Account name required.'); return; }
    D.load(rid);
    try {
        const a = await api.createAccount(n, t);
        D.ok(rid, `Account #${a.accountNo} "${a.accountName}" (${a.accountType}) opened.`);
        loadClientDashboard();
    } catch (e) { D.err(rid, e); }
};

// Deposit
D.get('btn-cl-deposit').onclick = async () => {
    const a = D.val('cl-dep-acct'), amt = D.val('cl-dep-amount');
    const rid = 'cl-deposit-result';
    if (!a || !amt) { D.err(rid, 'Both fields required.'); return; }
    D.load(rid);
    try {
        await api.deposit(a, amt);
        D.ok(rid, `Deposited ${amt}€ to #${a}.`);
        loadClientDashboard();
    } catch (e) { D.err(rid, e); }
};

// Transfer
D.get('btn-cl-transfer').onclick = async () => {
    const src = D.val('cl-xfer-src'), dst = D.val('cl-xfer-dst'),
          amt = D.val('cl-xfer-amount'), internal = D.checked('cl-xfer-internal');
    const rid = 'cl-transfer-result';
    if (!src || !dst || !amt) { D.err(rid, 'All fields required.'); return; }
    D.load(rid);
    try {
        const r = await api.transfer(src, dst, amt, internal);
        D.show(rid, F.ok(`Transferred ${amt}€ from #${src} to #${dst}.`) +
            `<br>Source: ${F.m(r.sourceNewBalance)}€ → Dest: ${F.m(r.destNewBalance)}€` +
            F.feeBreakdown(r));
        loadClientDashboard();
    } catch (e) { D.err(rid, e); }
};

// Add manager
D.get('btn-cl-add-manager').onclick = async () => {
    const a = D.val('cl-mgr-acct'), m = D.val('cl-mgr-user');
    const rid = 'cl-add-manager-result';
    if (!a || !m) { D.err(rid, 'Both fields required.'); return; }
    D.load(rid);
    try {
        await api.addManager(a, m);
        D.ok(rid, `"${m}" added as manager of #${a}.`);
        loadClientDashboard();
    } catch (e) { D.err(rid, e); }
};

/* =====================================================================
 *  ORCHESTRATION: Banker dashboard
 * ===================================================================== */

async function loadBankerDashboard() {
    D.show('bk-transfers-table', F.transfersTable(await api.allTransfers()));
    loadBkRules();
}

// Logout
D.get('btn-bk-logout').onclick = () => { S.clear(); showScreen('login'); };

// Create client
D.get('btn-bk-create-client').onclick = async () => {
    const u = D.val('bk-create-user'), b = D.val('bk-create-birth');
    const rid = 'bk-create-client-result';
    if (!u || !b) { D.err(rid, 'Both fields required.'); return; }
    D.load(rid);
    try {
        const c = await api.createClient(u, b);
        D.ok(rid, `Client created: ID=${c.id}, ${c.username}, ${F.date(c.birthDate)}`);
    } catch (e) { D.err(rid, e); }
};

// Delete client
D.get('btn-bk-delete-client').onclick = async () => {
    const u = D.val('bk-delete-user');
    const rid = 'bk-delete-client-result';
    if (!u) { D.err(rid, 'Username required.'); return; }
    D.load(rid);
    try { await api.deleteClient(u); D.ok(rid, `Client "${u}" deleted.`); }
    catch (e) { D.err(rid, e); }
};

// Find clients
const doFind = async (birth, bal) => {
    const rid = 'bk-find-result';
    D.load(rid);
    try { D.show(rid, F.clientsTable(await api.findClients(birth, bal))); }
    catch (e) { D.err(rid, e); }
};
D.get('btn-bk-find').onclick = () => doFind(D.val('bk-find-birth'), D.val('bk-find-balance'));
D.get('btn-bk-find-all').onclick = () => doFind('', '');

// Rules (banker — editable)
async function loadBkRules() {
    const rid = 'bk-rules-table';
    D.load(rid);
    try {
        D.show(rid, F.rulesTable(await api.getRules()));
        document.querySelectorAll('.rule-save-btn').forEach(btn => {
            btn.onclick = async () => {
                const k = btn.dataset.key;
                const v = parseFloat(D.get('rv-' + k.replace(/\./g, '-')).value);
                if (isNaN(v)) { alert('Invalid number'); return; }
                try {
                    await api.updateRule(k, v);
                    btn.textContent = '✓'; btn.style.background = 'var(--success)';
                    setTimeout(() => { btn.textContent = 'Save'; btn.style.background = ''; }, 1500);
                } catch (e) { alert(e.message); }
            };
        });
    } catch (e) { D.err(rid, e); }
}
D.get('btn-bk-rules').onclick = loadBkRules;

// Accrue interest
D.get('btn-bk-accrue').onclick = async () => {
    const d = parseInt(D.val('bk-interest-days'));
    const rid = 'bk-interest-result';
    if (!d || d < 1) { D.err(rid, 'Days ≥ 1 required.'); return; }
    D.load(rid);
    try { D.show(rid, F.interestTable(await api.accrueInterest(d))); }
    catch (e) { D.err(rid, e); }
};

// Refresh transfers
D.get('btn-bk-transfers').onclick = async () => {
    const rid = 'bk-transfers-table';
    D.load(rid);
    try { D.show(rid, F.transfersTable(await api.allTransfers())); }
    catch (e) { D.err(rid, e); }
};
