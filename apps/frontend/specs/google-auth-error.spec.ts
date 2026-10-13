import {
  FirebaseNotConfiguredError,
  isRedirectFallback,
} from "../lib/google-auth-error";

describe("isRedirectFallback", () => {
  it("falls back when the popup cannot open", () => {
    expect(isRedirectFallback({ code: "auth/popup-blocked" })).toBe(true);
    expect(
      isRedirectFallback({
        code: "auth/operation-not-supported-in-this-environment",
      }),
    ).toBe(true);
    expect(isRedirectFallback({ code: "auth/popup-closed-by-user" })).toBe(
      false,
    );
    expect(isRedirectFallback(new FirebaseNotConfiguredError())).toBe(false);
  });
});
