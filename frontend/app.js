const API_BASE = window.__API_BASE__ || '';
const TOKEN_KEY = 'mini-banking-transfer.token';
const TRANSFER_KEY = 'mini-banking-transfer.lastTransferId';

const state = {
  token: sessionStorage.getItem(TOKEN_KEY) || '',
  profile: null,
  lastTransferId: sessionStorage.getItem(TRANSFER_KEY) || '',
};

const sessionStateEl = document.getElementById('session-state');
const resultEl = document.getElementById('result');
const loginForm = document.getElementById('login-form');
const transferForm = document.getElementById('transfer-form');
const statusForm = document.getElementById('status-form');
const logoutButton = document.getElementById('logout-button');
const statusTransferInput = document.getElementById('status-transfer-id');

function init() {
  hydrateProfile();
  syncSessionUi();
  showIdleResult();

  if (state.lastTransferId) {
    statusTransferInput.value = state.lastTransferId;
  }

  loginForm.addEventListener('submit', handleLoginSubmit);
  transferForm.addEventListener('submit', handleTransferSubmit);
  statusForm.addEventListener('submit', handleStatusSubmit);
  logoutButton.addEventListener('click', handleLogout);
}

function hydrateProfile() {
  if (!state.token) {
    state.profile = null;
    return;
  }

  state.profile = decodeJwtPayload(state.token);
}

function syncSessionUi() {
  const signedIn = Boolean(state.token);
  logoutButton.disabled = !signedIn;
  setFormEnabled(transferForm, signedIn);
  setFormEnabled(statusForm, signedIn);

  if (!signedIn) {
    sessionStateEl.textContent = 'Signed out';
    return;
  }

  const parts = [];
  if (state.profile?.sub) {
    parts.push(state.profile.sub);
  }
  if (state.profile?.uid !== undefined) {
    parts.push(`uid ${state.profile.uid}`);
  }
  sessionStateEl.textContent = parts.length ? `Signed in as ${parts.join(' / ')}` : 'Signed in';
}

function showIdleResult() {
  renderResult({
    tone: 'neutral',
    title: 'Waiting for action',
    summary: 'Sign in, submit a transfer, or look up a transfer ID to see the response here.',
    fields: [
      ['API base', API_BASE || 'relative Kong routes'],
      ['Transfer route', '/api/transfers'],
      ['Auth route', '/api/auth/login'],
    ],
  });
}

async function handleLoginSubmit(event) {
  event.preventDefault();
  const form = event.currentTarget;
  const payload = {
    username: form.username.value.trim(),
    password: form.password.value,
  };

  try {
    setBusy(form, true);
    const data = await requestJson('/api/auth/login', {
      method: 'POST',
      body: payload,
      auth: false,
    });

    if (!data.token) {
      throw new Error('Login response did not include a token.');
    }

    state.token = data.token;
    sessionStorage.setItem(TOKEN_KEY, state.token);
    hydrateProfile();
    syncSessionUi();

    renderResult({
      tone: 'success',
      title: 'Login succeeded',
      summary: 'The JWT was stored in session storage and is ready for transfer requests.',
      fields: [
        ['Username', state.profile?.sub || payload.username],
        ['User ID', String(state.profile?.uid ?? 'unknown')],
      ],
      raw: { token: abbreviateToken(data.token) },
    });
    form.reset();
  } catch (error) {
    renderError('Login failed', error);
  } finally {
    setBusy(form, false);
  }
}

async function handleTransferSubmit(event) {
  event.preventDefault();
  const form = event.currentTarget;

  if (!state.token) {
    renderResult({
      tone: 'danger',
      title: 'Sign in required',
      summary: 'Use the login form before submitting a transfer.',
    });
    return;
  }

  const payload = {
    sourceAccount: form.sourceAccount.value.trim(),
    destinationAccount: form.destinationAccount.value.trim(),
    amount: Number(form.amount.value),
  };

  try {
    setBusy(form, true);
    const response = await requestJson('/api/transfers', {
      method: 'POST',
      body: payload,
      headers: {
        'Idempotency-Key': crypto.randomUUID(),
      },
    });

    if (!response.transferId) {
      throw new Error('Transfer response did not include a transferId.');
    }

    state.lastTransferId = response.transferId;
    sessionStorage.setItem(TRANSFER_KEY, state.lastTransferId);
    statusTransferInput.value = state.lastTransferId;

    renderResult({
      tone: response.status === 'SUCCESS' ? 'success' : 'warning',
      title: 'Transfer submitted',
      summary: 'The transfer service accepted the request and returned a terminal status.',
      fields: [
        ['Transfer ID', response.transferId],
        ['Status', response.status],
      ],
      raw: response,
    });

    form.reset();
    form.amount.focus();
  } catch (error) {
    renderError('Transfer failed', error);
  } finally {
    setBusy(form, false);
  }
}

async function handleStatusSubmit(event) {
  event.preventDefault();
  const form = event.currentTarget;

  if (!state.token) {
    renderResult({
      tone: 'danger',
      title: 'Sign in required',
      summary: 'Use the login form before looking up transfer status.',
    });
    return;
  }

  const transferId = form.transferId.value.trim();
  if (!transferId) {
    renderResult({
      tone: 'danger',
      title: 'Missing transfer ID',
      summary: 'Paste a transfer ID before querying status.',
    });
    return;
  }

  try {
    setBusy(form, true);
    const response = await requestJson(`/api/transfers/${encodeURIComponent(transferId)}`, {
      method: 'GET',
    });

    state.lastTransferId = response.transferId || transferId;
    sessionStorage.setItem(TRANSFER_KEY, state.lastTransferId);

    renderResult({
      tone: 'info',
      title: 'Status loaded',
      summary: 'The transfer service returned the current stored status for that transfer ID.',
      fields: [
        ['Transfer ID', response.transferId || transferId],
        ['Status', response.status],
      ],
      raw: response,
    });
  } catch (error) {
    renderError('Status lookup failed', error);
  } finally {
    setBusy(form, false);
  }
}

function handleLogout() {
  state.token = '';
  state.profile = null;
  sessionStorage.removeItem(TOKEN_KEY);
  syncSessionUi();
  renderResult({
    tone: 'neutral',
    title: 'Session cleared',
    summary: 'You can sign in again at any time.',
  });
}

async function requestJson(path, { method, body, auth = true, headers: extraHeaders = {} }) {
  const headers = { 'Content-Type': 'application/json' };
  if (auth && state.token) {
    headers.Authorization = `Bearer ${state.token}`;
  }
  Object.assign(headers, extraHeaders);

  const response = await fetch(`${API_BASE}${path}`, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  });

  const payload = await readResponseBody(response);
  if (!response.ok) {
    const message = payload?.message || payload?.error || response.statusText || 'Request failed';
    throw new Error(`${message} (${response.status})`);
  }

  return payload ?? {};
}

async function readResponseBody(response) {
  const text = await response.text();
  if (!text) {
    return null;
  }

  try {
    return JSON.parse(text);
  } catch {
    return { message: text };
  }
}

function renderError(title, error) {
  renderResult({
    tone: 'danger',
    title,
    summary: error instanceof Error ? error.message : String(error),
  });
}

function renderResult({ tone, title, summary, fields = [], raw = null }) {
  resultEl.replaceChildren(buildResultNode(tone, title, summary, fields, raw));
}

function buildResultNode(tone, title, summary, fields, raw) {
  const panel = document.createElement('article');
  panel.className = `result-panel result-panel--${tone}`;

  const header = document.createElement('div');
  header.className = 'result-header';

  const kicker = document.createElement('p');
  kicker.className = 'eyebrow';
  kicker.textContent = tone === 'success' ? 'Success' : tone === 'danger' ? 'Error' : 'Info';

  const heading = document.createElement('h3');
  heading.textContent = title;

  const summaryEl = document.createElement('p');
  summaryEl.className = 'result-summary';
  summaryEl.textContent = summary;

  header.append(kicker, heading, summaryEl);
  panel.append(header);

  if (fields.length) {
    const dl = document.createElement('dl');
    dl.className = 'result-grid';

    fields.forEach(([label, value]) => {
      const wrapper = document.createElement('div');
      wrapper.className = 'result-item';

      const dt = document.createElement('dt');
      dt.textContent = label;

      const dd = document.createElement('dd');
      dd.textContent = value;

      wrapper.append(dt, dd);
      dl.append(wrapper);
    });

    panel.append(dl);
  }

  if (raw) {
    const pre = document.createElement('pre');
    pre.className = 'result-json';
    pre.textContent = JSON.stringify(raw, null, 2);
    panel.append(pre);
  }

  return panel;
}

function setBusy(form, busy) {
  form.querySelectorAll('input, button').forEach((control) => {
    if (control === logoutButton) {
      return;
    }
    control.disabled = busy;
  });
}

function setFormEnabled(form, enabled) {
  form.querySelectorAll('input, button').forEach((control) => {
    if (control === logoutButton) {
      return;
    }
    control.disabled = !enabled;
  });
}

function decodeJwtPayload(token) {
  const parts = token.split('.');
  if (parts.length !== 3) {
    return null;
  }

  try {
    const payload = base64UrlDecode(parts[1]);
    return JSON.parse(payload);
  } catch {
    return null;
  }
}

function base64UrlDecode(value) {
  const normalized = value.replace(/-/g, '+').replace(/_/g, '/');
  const padded = normalized + '='.repeat((4 - (normalized.length % 4)) % 4);
  return atob(padded);
}

function abbreviateToken(token) {
  if (token.length <= 24) {
    return token;
  }
  return `${token.slice(0, 16)}...${token.slice(-8)}`;
}

init();
