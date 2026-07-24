import { beforeEach, describe, expect, it, vi } from "vitest";
import {
  AUTH_CHANGED_EVENT,
  clearSession,
  readSession,
  saveSession,
} from "./session";

describe("browser session", () => {
  beforeEach(() => localStorage.clear());

  it("stores and restores access token and user", () => {
    const payload = {
      accessToken: "test-access-token",
      user: { id: 1, name: "PetHub User", role: "USER" },
    };
    saveSession(payload);
    expect(readSession()).toEqual(payload);
  });

  it("clears storage and announces the change", () => {
    const listener = vi.fn();
    window.addEventListener(AUTH_CHANGED_EVENT, listener);
    saveSession({ accessToken: "token", user: { id: 1 } });
    clearSession();
    expect(readSession()).toEqual({ accessToken: null, user: null });
    expect(listener).toHaveBeenCalled();
    window.removeEventListener(AUTH_CHANGED_EVENT, listener);
  });
});
