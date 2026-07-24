const ACCESS_TOKEN_KEY = "pet_hub_access_token";
const USER_KEY = "pet_hub_user";

export const AUTH_CHANGED_EVENT = "pet-hub-auth-changed";

export function readSession() {
  const accessToken = localStorage.getItem(ACCESS_TOKEN_KEY);
  const rawUser = localStorage.getItem(USER_KEY);
  try {
    return {
      accessToken,
      user: rawUser ? JSON.parse(rawUser) : null,
    };
  } catch {
    clearSession();
    return { accessToken: null, user: null };
  }
}

export function saveSession(response) {
  const session = {
    accessToken: response?.accessToken ?? null,
    user: response?.user ?? null,
  };
  if (session.accessToken) {
    localStorage.setItem(ACCESS_TOKEN_KEY, session.accessToken);
  }
  if (session.user) {
    localStorage.setItem(USER_KEY, JSON.stringify(session.user));
  }
  window.dispatchEvent(new CustomEvent(AUTH_CHANGED_EVENT, { detail: session }));
  return session;
}

export function clearSession() {
  localStorage.removeItem(ACCESS_TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
  const session = { accessToken: null, user: null };
  window.dispatchEvent(new CustomEvent(AUTH_CHANGED_EVENT, { detail: session }));
  return session;
}

export function getAccessToken() {
  return localStorage.getItem(ACCESS_TOKEN_KEY);
}
