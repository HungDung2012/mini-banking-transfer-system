# Frontend Demo Fallback Design

**Goal:** Make the banking frontend safe for a classroom demo even if backend endpoints fail intermittently.

## Scope

- Keep the current UI structure.
- Only support the demo flow: login, view balance, transfer money.
- Avoid backend changes.
- Prefer real API responses when available.
- Fall back to local demo behavior when auth, account, or transfer calls fail.

## Design

### Login

- Keep normal login against `/api/auth/login`.
- If login fails and the username is `alice` or `bob`, allow a demo login with a synthetic session token.
- Persist the demo session in the same frontend session state so the rest of the UI works unchanged.

### Balance

- Keep fetching real balance from `/api/accounts/{accountNumber}` when possible.
- If the account API fails during a demo session, show a locally stored fallback balance.
- Initialize fallback balances for demo users and update them after demo transfers.

### Transfer

- Keep sending real transfer requests to `/api/transfers` first.
- If the transfer API fails during a demo session, simulate success locally:
  - subtract the amount from the local source balance
  - show the success modal
  - append a local activity item

### Activity

- Keep loading real notifications first.
- If notification loading fails in a demo session, render local demo activity items instead.

## Error Handling

- Non-demo users still see the current real error messages.
- Demo fallback only activates for `alice` and `bob`.
- Prevent invalid local transfers the same way as real transfers.

## Files

- Modify `frontend/app.js` to add demo fallback state and fallback logic.
- Leave `frontend/index.html` and `frontend/styles.css` unchanged unless a tiny text tweak becomes necessary.
