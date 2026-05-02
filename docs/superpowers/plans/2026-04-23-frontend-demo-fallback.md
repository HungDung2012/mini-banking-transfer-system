# Frontend Demo Fallback Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make `frontend/app.js` preserve a smooth demo flow for login, balance, and transfer even when backend calls fail.

**Architecture:** Keep the current real API-first flow and layer a minimal demo fallback path on top. Restrict fallback behavior to demo accounts so normal failures still surface for non-demo users.

**Tech Stack:** Vanilla JavaScript, browser `sessionStorage` and `localStorage`, existing frontend UI state.

---

### Task 1: Add demo fallback state

**Files:**
- Modify: `frontend/app.js`

- [ ] Add constants and in-memory helpers for demo users, demo token detection, fallback balances, and local activity storage.
- [ ] Keep the data shape small and compatible with the existing UI flow.

### Task 2: Add login fallback

**Files:**
- Modify: `frontend/app.js`

- [ ] Update login logic to try the real auth API first.
- [ ] If auth fails for `alice` or `bob`, create a demo session instead of blocking the demo.
- [ ] Preserve current error behavior for other usernames.

### Task 3: Add balance and notification fallback

**Files:**
- Modify: `frontend/app.js`

- [ ] Update balance loading to use real account data first.
- [ ] If balance loading fails in a demo session, render the locally tracked demo balance.
- [ ] Update notification loading to render local activity when real notifications are unavailable.

### Task 4: Add transfer fallback

**Files:**
- Modify: `frontend/app.js`

- [ ] Keep the real transfer call first.
- [ ] If the transfer call fails in a demo session, simulate a successful transfer locally.
- [ ] Reuse the current modal and balance refresh flow so the UI still looks consistent.

### Task 5: Verify demo script safety

**Files:**
- Modify: `frontend/app.js`

- [ ] Run a JavaScript syntax check against `frontend/app.js`.
- [ ] Review the changed code for obvious regressions in login, balance, and transfer paths.
