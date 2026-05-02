// CONFIG
const API_BASE = window.__API_BASE__ || '';
const TOKEN_KEY = 'mini-banking.token';
const USERNAME_KEY = 'mini-banking.username';
const ACCOUNT_KEY = 'mini-banking.accountNumber';
const ACCOUNT_MAP_KEY = 'mini-banking.accountMap';
const DEMO_BALANCES_KEY = 'mini-banking.demoBalances';
const DEMO_ACTIVITY_KEY = 'mini-banking.demoActivity';
const DEMO_ACCOUNT_MAP = {
    alice: '100001',
    bob: '200001'
};
const DEMO_BALANCE_MAP = {
    alice: 1000000000,
    bob: 500000
};

// STATE variables - stored purely in memory
const state = {
    token: sessionStorage.getItem(TOKEN_KEY),
    username: sessionStorage.getItem(USERNAME_KEY),
    accountNumber: sessionStorage.getItem(ACCOUNT_KEY),
    balance: 0,
    pollInterval: null
};

// INITIALIZE ICONS
document.addEventListener('DOMContentLoaded', () => {
    lucide.createIcons();
    if (state.token && state.username) {
        initApp();
    }
});

// ---------------- UTILS ----------------

function uuidv4() {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
        var r = Math.random() * 16 | 0, v = c === 'x' ? r : (r & 0x3 | 0x8);
        return v.toString(16);
    });
}

function formatCurrency(amount) {
    return new Intl.NumberFormat('vi-VN').format(amount) + ' ₫';
}

function getRawNumber(str) {
    return parseInt(str.replace(/[^0-9]/g, ''), 10) || 0;
}

function resolveAccountNumber(username) {
    const savedAccounts = JSON.parse(localStorage.getItem(ACCOUNT_MAP_KEY) || '{}');
    return savedAccounts[username] || DEMO_ACCOUNT_MAP[username] || null;
}

function getDemoUsernameByAccountNumber(accountNumber) {
    return Object.keys(DEMO_ACCOUNT_MAP).find((username) => DEMO_ACCOUNT_MAP[username] === accountNumber) || null;
}

function saveAccountNumber(username, accountNumber) {
    if (!username || !accountNumber) {
        return;
    }

    const savedAccounts = JSON.parse(localStorage.getItem(ACCOUNT_MAP_KEY) || '{}');
    savedAccounts[username] = accountNumber;
    localStorage.setItem(ACCOUNT_MAP_KEY, JSON.stringify(savedAccounts));
}

function isDemoUser(username) {
    return Object.prototype.hasOwnProperty.call(DEMO_ACCOUNT_MAP, username);
}

function isDemoSession() {
    return Boolean(state.username && isDemoUser(state.username) && state.token && state.token.startsWith('demo-token-'));
}

function isStandaloneDemoMode() {
    return !API_BASE && window.location.port !== '' && window.location.port !== '80';
}

function readDemoBalances() {
    const saved = JSON.parse(localStorage.getItem(DEMO_BALANCES_KEY) || '{}');
    return {
        ...DEMO_BALANCE_MAP,
        ...saved
    };
}

function getDemoBalance(username) {
    return readDemoBalances()[username] ?? 0;
}

function setDemoBalance(username, balance) {
    const balances = readDemoBalances();
    balances[username] = balance;
    localStorage.setItem(DEMO_BALANCES_KEY, JSON.stringify(balances));
}

function readDemoActivity() {
    return JSON.parse(localStorage.getItem(DEMO_ACTIVITY_KEY) || '{}');
}

function getDemoActivity(accountNumber) {
    const activity = readDemoActivity();
    return activity[accountNumber] || [];
}

function saveDemoActivity(accountNumber, items) {
    const activity = readDemoActivity();
    activity[accountNumber] = items;
    localStorage.setItem(DEMO_ACTIVITY_KEY, JSON.stringify(activity));
}

function addDemoActivity(accountNumber, item) {
    const existing = getDemoActivity(accountNumber);
    saveDemoActivity(accountNumber, [item, ...existing].slice(0, 8));
}

function createDemoSession(username) {
    const accountNumber = resolveAccountNumber(username) || DEMO_ACCOUNT_MAP[username] || null;
    state.token = `demo-token-${username}`;
    state.username = username;
    state.accountNumber = accountNumber;
    state.balance = getDemoBalance(username);
    persistSession();
}

function applyDemoBalance() {
    if (!state.username) {
        return;
    }

    state.balance = getDemoBalance(state.username);
    updateBalanceUI(state.balance);

    if (state.accountNumber) {
        document.getElementById('display-account-number').textContent = state.accountNumber;
    }
}

async function resolveAccountNumberForUser(username) {
    const localAccountNumber = resolveAccountNumber(username);
    if (localAccountNumber) {
        return localAccountNumber;
    }

    try {
        const account = await apiCall(`/api/accounts/by-owner/${encodeURIComponent(username)}`, 'GET', null, false);
        if (account && account.accountNumber) {
            saveAccountNumber(username, account.accountNumber);
            return account.accountNumber;
        }
    } catch (error) {}

    return null;
}

async function ensureAccountExists(username) {
    const createdAccount = await apiCall('/api/accounts', 'POST', {
        ownerName: username
    });

    if (createdAccount && createdAccount.accountNumber) {
        saveAccountNumber(username, createdAccount.accountNumber);
        return createdAccount.accountNumber;
    }

    return null;
}

function persistSession() {
    if (state.token) {
        sessionStorage.setItem(TOKEN_KEY, state.token);
    } else {
        sessionStorage.removeItem(TOKEN_KEY);
    }

    if (state.username) {
        sessionStorage.setItem(USERNAME_KEY, state.username);
    } else {
        sessionStorage.removeItem(USERNAME_KEY);
    }

    if (state.accountNumber) {
        sessionStorage.setItem(ACCOUNT_KEY, state.accountNumber);
    } else {
        sessionStorage.removeItem(ACCOUNT_KEY);
    }
}

// Toggle Dark Mode
function toggleDarkMode() {
    document.body.classList.toggle('dark');
    const icon = document.getElementById('dark-mode-icon');
    if (document.body.classList.contains('dark')) {
        icon.setAttribute('data-lucide', 'sun');
    } else {
        icon.setAttribute('data-lucide', 'moon');
    }
    lucide.createIcons();
}

// ---------------- API CLIENT ----------------

async function apiCall(endpoint, method = 'GET', body = null, useAuth = false, additionalHeaders = {}) {
    const headers = {
        'Content-Type': 'application/json',
        ...additionalHeaders
    };

    if (useAuth && state.token) {
        headers['Authorization'] = `Bearer ${state.token}`;
    }

    const options = {
        method,
        headers,
    };

    if (body) {
        options.body = JSON.stringify(body);
    }

    try {
        const response = await fetch(`${API_BASE}${endpoint}`, options);
        
        const data = await response.json().catch(() => ({}));

        if (!response.ok) {
            if (response.status === 401 && useAuth) {
                throw new Error('Unauthorized request. Please log in again or try a permitted account.');
            }
            if (response.status === 409 && endpoint === '/api/auth/register') {
                throw new Error('Username already exists. Please choose another username.');
            }
            throw new Error(data.message || data.error || `Request failed (${response.status})`);
        }
        
        return data;
    } catch (error) {
        if(error.message === 'Failed to fetch') {
            throw new Error('Network error. Check connection or API base URL.');
        }
        throw error;
    }
}

// ---------------- AUTHENTICATION ----------------

window.switchAuthTab = function(tab) {
    document.querySelectorAll('.auth-tab').forEach(el => el.classList.remove('active'));
    document.getElementById(`tab-${tab}`).classList.add('active');

    if (tab === 'login') {
        document.getElementById('login-form').classList.remove('hidden');
        document.getElementById('register-form').classList.add('hidden');
    } else {
        document.getElementById('login-form').classList.add('hidden');
        document.getElementById('register-form').classList.remove('hidden');
    }
    clearErrors();
}

function clearErrors() {
    document.getElementById('login-error').classList.add('hidden');
    document.getElementById('register-error').classList.add('hidden');
    document.getElementById('transfer-error').classList.add('hidden');
}

function showError(elementId, message) {
    const el = document.getElementById(elementId);
    el.innerHTML = `<i data-lucide="alert-circle" style="width:16px; height:16px;"></i> ${message}`;
    el.classList.remove('hidden');
    lucide.createIcons();
}

function setButtonLoading(btnId, isLoading, originalText) {
    const btn = document.getElementById(btnId);
    if (isLoading) {
        btn.disabled = true;
        btn.innerHTML = `<i data-lucide="loader-2" class="spin" style="width:16px;height:16px;"></i> <span>Processing...</span>`;
    } else {
        btn.disabled = false;
        btn.innerHTML = `<span>${originalText}</span>`;
    }
    lucide.createIcons();
}

window.handleLogin = async function(e) {
    e.preventDefault();
    clearErrors();
    const u = document.getElementById('login-username').value;
    const p = document.getElementById('login-password').value;
    setButtonLoading('btn-login', true, 'Sign In');

    if (isStandaloneDemoMode() && isDemoUser(u)) {
        createDemoSession(u);
        initApp();
        setButtonLoading('btn-login', false, 'Sign In');
        return;
    }

    try {
        const res = await apiCall('/api/auth/login', 'POST', { username: u, password: p });
        if (res.token) {
            state.token = res.token;
            state.username = u;
            state.accountNumber = await resolveAccountNumberForUser(u);
            state.balance = 0;
            persistSession();
            initApp();
        } else {
            showError('login-error', 'Login failed: Missing token');
        }
    } catch (err) {
        if (isDemoUser(u)) {
            createDemoSession(u);
            initApp();
        } else {
            state.token = null;
            state.username = null;
            state.accountNumber = null;
            state.balance = 0;
            persistSession();
            document.getElementById('app-view').style.display = 'none';
            document.getElementById('auth-view').style.display = 'flex';
            showError('login-error', err.message);
        }
    } finally {
        setButtonLoading('btn-login', false, 'Sign In');
    }
}

window.handleRegister = async function(e) {
    e.preventDefault();
    clearErrors();
    const u = document.getElementById('reg-username').value;
    const p = document.getElementById('reg-password').value;

    if (!u || !p) {
        showError('register-error', 'Please enter username and password.');
        return;
    }

    setButtonLoading('btn-register', true, 'Create Account');
    try {
        await apiCall('/api/auth/register', 'POST', { username: u, password: p });
        await ensureAccountExists(u);
        // Auto login after register
        switchAuthTab('login');
        document.getElementById('login-username').value = u;
        document.getElementById('login-password').value = p;
        
        // Show temporary success message
        const errEl = document.getElementById('login-error');
        errEl.innerHTML = `<i data-lucide="check-circle" style="width:16px; height:16px;"></i> Registration successful! Please log in.`;
        errEl.className = 'error-message';
        errEl.style.color = 'var(--success)';
        lucide.createIcons();

    } catch (err) {
        if (err.message === 'Username already exists. Please choose another username.') {
            const existingAccountNumber = await resolveAccountNumberForUser(u);
            if (existingAccountNumber) {
                saveAccountNumber(u, existingAccountNumber);
                showError('register-error', 'Username already exists. Try signing in with that account.');
            } else {
                try {
                    await ensureAccountExists(u);
                    showError('register-error', 'Username already existed, but the account link has now been created. Please sign in.');
                } catch (accountError) {
                    showError('register-error', 'Username already exists, but no linked account was found yet. Please sign in or choose another username.');
                }
            }
        } else {
            showError('register-error', err.message);
        }
    } finally {
        setButtonLoading('btn-register', false, 'Create Account');
    }
}

function initApp() {
    document.getElementById('auth-view').style.display = 'none';
    document.getElementById('app-view').style.display = 'block';
    
    document.getElementById('display-username').textContent = state.username;
    document.getElementById('display-account-number').textContent = state.accountNumber;
    updateBalanceUI(isDemoSession() ? getDemoBalance(state.username) : state.balance);

    // Fetch latest real data
    fetchBalance();
    fetchNotifications();

    if(state.pollInterval) clearInterval(state.pollInterval);
    state.pollInterval = setInterval(fetchNotifications, 10000);
}

window.logout = function() {
    state.token = null;
    state.username = null;
    state.accountNumber = null;
    state.balance = 0;
    persistSession();
    
    if(state.pollInterval) {
        clearInterval(state.pollInterval);
        state.pollInterval = null;
    }

    document.getElementById('app-view').style.display = 'none';
    document.getElementById('auth-view').style.display = 'flex';
    
    // Clear forms
    document.getElementById('login-password').value = '';
    document.getElementById('transfer-dest').value = '';
    document.getElementById('transfer-amount').value = '';
    document.getElementById('btn-transfer').disabled = true;
    clearErrors();
}

// ---------------- MAIN APP LOGIC ----------------

window.fetchBalance = async function() {
    const icon = document.getElementById('refresh-icon');
    if(icon) icon.classList.add('spin');
    
    try {
        if (isStandaloneDemoMode() && isDemoSession()) {
            applyDemoBalance();
            return;
        }
        if (!state.accountNumber) {
            return;
        }
        const res = await apiCall(`/api/accounts/${encodeURIComponent(state.accountNumber)}`, 'GET', null, false);
        if (res.balance !== undefined) {
            state.balance = res.balance;
            updateBalanceUI(state.balance);
            if (res.accountNumber) {
                state.accountNumber = res.accountNumber;
                document.getElementById('display-account-number').textContent = res.accountNumber;
            }
        }
    } catch (err) {
        if (isDemoSession()) {
            applyDemoBalance();
        }
    } finally {
        if(icon) setTimeout(() => icon.classList.remove('spin'), 500);
    }
}

function updateBalanceUI(amount) {
    document.getElementById('display-balance').textContent = formatCurrency(amount);
}

// Transfer Form Handling
window.formatAmountInput = function(input) {
    let val = getRawNumber(input.value);
    if (val === 0) {
        input.value = '';
    } else {
        input.value = new Intl.NumberFormat('vi-VN').format(val);
    }
}

window.setQuickAmount = function(amount) {
    const input = document.getElementById('transfer-amount');
    input.value = new Intl.NumberFormat('vi-VN').format(amount);
    updateSummary();
}

window.updateSummary = function() {
    const dest = document.getElementById('transfer-dest').value.trim();
    const amountRaw = getRawNumber(document.getElementById('transfer-amount').value);
    const btn = document.getElementById('btn-transfer');

    if (dest && amountRaw > 0) {
        btn.disabled = false;
    } else {
        btn.disabled = true;
    }
}

window.handleTransfer = async function(e) {
    e.preventDefault();
    clearErrors();
    
    const dest = document.getElementById('transfer-dest').value.trim();
    const amountRaw = getRawNumber(document.getElementById('transfer-amount').value);

    if (amountRaw <= 0) {
        showError('transfer-error', "Amount must be greater than 0");
        return;
    }
    if (!state.accountNumber) {
        showError('transfer-error', "No source account is linked to this session.");
        return;
    }
    if (dest === state.accountNumber) {
        showError('transfer-error', "Cannot transfer to your own account");
        return;
    }

    setButtonLoading('btn-transfer', true, 'Processing Transfer');
    const idempotencyKey = uuidv4();

    if (isStandaloneDemoMode() && isDemoSession()) {
        const senderNextBalance = Math.max(0, getDemoBalance(state.username) - amountRaw);
        const recipientUsername = getDemoUsernameByAccountNumber(dest);
        setDemoBalance(state.username, senderNextBalance);
        state.balance = senderNextBalance;

        if (recipientUsername) {
            const recipientNextBalance = getDemoBalance(recipientUsername) + amountRaw;
            setDemoBalance(recipientUsername, recipientNextBalance);
            addDemoActivity(dest, {
                title: 'Money received',
                body: `Received ${formatCurrency(amountRaw)} from ${state.username}`,
                createdAt: new Date().toISOString()
            });
        }

        addDemoActivity(state.accountNumber, {
            title: 'Demo transfer completed',
            body: `Sent ${formatCurrency(amountRaw)} to account ${dest}`,
            createdAt: new Date().toISOString()
        });
        showSuccessModal(dest, amountRaw);
        document.getElementById('transfer-dest').value = '';
        document.getElementById('transfer-amount').value = '';
        updateSummary();
        applyDemoBalance();
        renderNotifications(getDemoActivity(state.accountNumber));
        setButtonLoading('btn-transfer', false, 'Confirm Transfer');
        return;
    }
    
    try {
        const payload = {
            sourceAccount: state.accountNumber,
            destinationAccount: dest,
            amount: amountRaw
        };

        const res = await apiCall('/api/transfers', 'POST', payload, true, {
            'Idempotency-Key': idempotencyKey
        });

        if (res.status === 'SUCCESS' || res.status === 'PENDING') {
            // Success!
            showSuccessModal(dest, amountRaw);
            
            // Reset form
            document.getElementById('transfer-dest').value = '';
            document.getElementById('transfer-amount').value = '';
            updateSummary();
            
            // Immediately fetch updated balance
            setTimeout(fetchBalance, 500); 
        } else if (res.status === 'FAILED' || res.status === 'REJECTED') {
            showError('transfer-error', `Transfer failed: ${res.message || 'Unknown reason'}`);
        }
    } catch (err) {
        if (isDemoSession()) {
            const senderNextBalance = Math.max(0, getDemoBalance(state.username) - amountRaw);
            const recipientUsername = getDemoUsernameByAccountNumber(dest);
            setDemoBalance(state.username, senderNextBalance);
            state.balance = senderNextBalance;

            if (recipientUsername) {
                const recipientNextBalance = getDemoBalance(recipientUsername) + amountRaw;
                setDemoBalance(recipientUsername, recipientNextBalance);
                addDemoActivity(dest, {
                    title: 'Money received',
                    body: `Received ${formatCurrency(amountRaw)} from ${state.username}`,
                    createdAt: new Date().toISOString()
                });
            }

            addDemoActivity(state.accountNumber, {
                title: 'Demo transfer completed',
                body: `Sent ${formatCurrency(amountRaw)} to account ${dest}`,
                createdAt: new Date().toISOString()
            });
            showSuccessModal(dest, amountRaw);
            document.getElementById('transfer-dest').value = '';
            document.getElementById('transfer-amount').value = '';
            updateSummary();
            applyDemoBalance();
            renderNotifications(getDemoActivity(state.accountNumber));
        } else {
            showError('transfer-error', err.message);
        }
    } finally {
        setButtonLoading('btn-transfer', false, 'Confirm Transfer');
        updateSummary(); // Will re-evaluate if button should be disabled
    }
}

// Render Notifications
async function fetchNotifications() {
    try {
        if (isStandaloneDemoMode() && isDemoSession()) {
            renderNotifications(getDemoActivity(state.accountNumber));
            return;
        }
        if (!state.accountNumber) {
            renderNotifications([]);
            return;
        }

        const notifications = await apiCall(
            `/api/notifications/account/${encodeURIComponent(state.accountNumber)}`,
            'GET',
            null,
            false
        );
        renderNotifications(notifications);
    } catch (err) {
        if (isDemoSession()) {
            renderNotifications(getDemoActivity(state.accountNumber));
        } else {
            renderNotifications([]);
        }
    }
}

function renderNotifications(notifs) {
    const listEl = document.getElementById('notifications-list');
    listEl.innerHTML = '';

    if (!notifs || notifs.length === 0) {
        listEl.innerHTML = `
            <div class="empty-state">
                <p>No new notifications</p>
            </div>
        `;
        return;
    }

    const sorted = [...notifs].sort((a,b) => new Date(b.createdAt) - new Date(a.createdAt));

    sorted.forEach(n => {
        const dateRaw = new Date(n.createdAt);
        const now = new Date();
        const diffSec = Math.floor((now - dateRaw)/1000);
        let timeStr = dateRaw.toLocaleString();
        if(diffSec < 60) timeStr = 'Just now';
        else if(diffSec < 3600) timeStr = `${Math.floor(diffSec/60)}m ago`;
        else if(diffSec < 86400) timeStr = `${Math.floor(diffSec/3600)}h ago`;

        const html = `
            <div class="notification-item">
                <div class="notification-icon">
                    <i data-lucide="bell" style="width: 16px; height: 16px;"></i>
                </div>
                <div class="notification-content" style="flex:1;">
                    <div class="notification-title">${n.title || 'Notification'}</div>
                    <div class="notification-body">${n.body || ''}</div>
                    <span class="notification-time">${timeStr}</span>
                </div>
            </div>
        `;
        listEl.insertAdjacentHTML('beforeend', html);
    });
    lucide.createIcons();
}

// Success Output Flow
window.showSuccessModal = function(dest, amount) {
    document.getElementById('success-dest').textContent = dest;
    document.getElementById('success-amount').textContent = formatCurrency(amount);
    document.getElementById('success-modal').classList.add('active');
    fireConfetti();
}

window.closeSuccessModal = function() {
    document.getElementById('success-modal').classList.remove('active');
}

function fireConfetti() {
    if (typeof confetti === 'function') {
        var duration = 3 * 1000;
        var end = Date.now() + duration;

        (function frame() {
            confetti({
                particleCount: 5,
                angle: 60,
                spread: 55,
                origin: { x: 0 },
                colors: ['#000000', '#3b82f6', '#ffffff'] // Adjusted to minimal palette
            });
            confetti({
                particleCount: 5,
                angle: 120,
                spread: 55,
                origin: { x: 1 },
                colors: ['#000000', '#3b82f6', '#ffffff']
            });

            if (Date.now() < end) {
                requestAnimationFrame(frame);
            }
        }());
    }
}
