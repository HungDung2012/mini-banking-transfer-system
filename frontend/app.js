// CONFIG
const API_BASE = 'http://34.60.8.21:8000'; // Kong API Gateway

// STATE variables - stored purely in memory
const state = {
    token: null,
    username: null,
    accountNumber: null,
    balance: 0,
    pollInterval: null
};

// INITIALIZE ICONS
document.addEventListener('DOMContentLoaded', () => {
    lucide.createIcons();
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
        
        if (response.status === 401 && useAuth) {
            logout();
            throw new Error('Session expired. Please log in again.');
        }
        
        const data = await response.json().catch(() => ({}));

        if (!response.ok) {
            throw new Error(data.message || data.error || 'Request failed');
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
         initApp();
    setButtonLoading('btn-login', true, 'Sign In');
    try {
        const res = await apiCall('/api/auth/login', 'POST', { username: u, password: p });
        if (res.token) {
            state.token = res.token;
            state.username = u;
            state.accountNumber = res.accountNumber;
            state.balance = res.balance || 0;
            initApp();
        } else {
            showError('login-error', 'Login failed: Missing token');
        }
    } catch (err) {
        showError('login-error', err.message);
    } finally {
        setButtonLoading('btn-login', false, 'Sign In');
    }
}

window.handleRegister = async function(e) {
    e.preventDefault();
    clearErrors();
    const u = document.getElementById('reg-username').value;
    const a = document.getElementById('reg-account').value;
    const p = document.getElementById('reg-password').value;

    setButtonLoading('btn-register', true, 'Create Account');
    try {
        await apiCall('/api/auth/register', 'POST', { username: u, password: p, accountNumber: a });
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
        showError('register-error', err.message);
    } finally {
        setButtonLoading('btn-register', false, 'Create Account');
    }
}

function initApp() {
    document.getElementById('auth-view').style.display = 'none';
    document.getElementById('app-view').style.display = 'block';
    
    document.getElementById('display-username').textContent = state.username;
    document.getElementById('display-account-number').textContent = state.accountNumber;
    updateBalanceUI(state.balance);

    // Fetch latest real data
    fetchBalance();
    fetchNotifications();

    // Setup polling
    if(state.pollInterval) clearInterval(state.pollInterval);
    state.pollInterval = setInterval(fetchNotifications, 15000); // 15s poll
}

window.logout = function() {
    state.token = null;
    state.username = null;
    state.accountNumber = null;
    state.balance = 0;
    
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
        const res = await apiCall('/api/accounts/balance', 'GET', null, true);
        if (res.balance !== undefined) {
            state.balance = res.balance;
            updateBalanceUI(state.balance);
        }
    } catch (err) {
        console.error("Failed to fetch balance:", err);
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
    if (dest === state.accountNumber) {
        showError('transfer-error', "Cannot transfer to your own account");
        return;
    }

    setButtonLoading('btn-transfer', true, 'Processing Transfer');
    const idempotencyKey = uuidv4();
    
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
            // Fetch notifications to show the sent notification
            setTimeout(fetchNotifications, 1000);
        } else if (res.status === 'FAILED' || res.status === 'REJECTED') {
            showError('transfer-error', `Transfer failed: ${res.message || 'Unknown reason'}`);
        }

    } catch (err) {
        showError('transfer-error', err.message);
    } finally {
        setButtonLoading('btn-transfer', false, 'Confirm Transfer');
        updateSummary(); // Will re-evaluate if button should be disabled
    }
}

// Render Notifications
async function fetchNotifications() {
    try {
        const data = await apiCall('/api/notifications', 'GET', null, true);
        renderNotifications(data);
    } catch (err) {
        console.error("Poll notification error:", err);
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
