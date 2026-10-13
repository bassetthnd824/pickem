import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { FirebaseNotConfiguredError } from "../lib/google-auth-error";
import LoginPage from "../app/(auth)/login/page";

const navigation = vi.hoisted(() => ({
  replace: vi.fn<(path: string) => void>(),
  refresh: vi.fn<() => void>(),
}));

const firebaseAuth = vi.hoisted(() => ({
  consumeGoogleRedirect: vi.fn<() => Promise<string | null>>(async () => null),
  signInWithGooglePopup: vi.fn<() => Promise<string>>(),
  signInWithGoogleRedirect: vi.fn<() => Promise<void>>(async () => undefined),
}));

vi.mock("next/navigation", () => ({
  useRouter: () => navigation,
}));

vi.mock("../lib/firebase-auth", () => firebaseAuth);

describe("LoginPage", () => {
  const fetchMock = vi.fn<typeof fetch>();

  beforeEach(() => {
    fetchMock.mockReset();
    navigation.replace.mockReset();
    navigation.refresh.mockReset();
    firebaseAuth.consumeGoogleRedirect.mockReset();
    firebaseAuth.consumeGoogleRedirect.mockResolvedValue(null);
    firebaseAuth.signInWithGooglePopup.mockReset();
    firebaseAuth.signInWithGoogleRedirect.mockReset();
    firebaseAuth.signInWithGoogleRedirect.mockResolvedValue(undefined);
    vi.stubGlobal("fetch", fetchMock);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("shows one Google button and no register or password form", () => {
    render(<LoginPage />);

    expect(
      screen.getByRole("button", { name: "Sign in with Google" }),
    ).toBeTruthy();
    expect(screen.getAllByRole("button")).toHaveLength(1);
    expect(screen.queryByRole("link", { name: /register/i })).toBeNull();
    expect(document.querySelector("input[type='password']")).toBeNull();
    expect(
      screen.getByRole("heading", { name: "Kenney's Pickem" }),
    ).toBeTruthy();
  });

  it("posts the Google ID token and lands on main", async () => {
    firebaseAuth.signInWithGooglePopup.mockResolvedValue("id-token");
    fetchMock.mockResolvedValue(new Response(null, { status: 204 }));
    render(<LoginPage />);

    fireEvent.click(
      screen.getByRole("button", { name: "Sign in with Google" }),
    );

    await waitFor(() => {
      expect(navigation.replace).toHaveBeenCalledWith("/main");
    });
    expect(navigation.refresh).toHaveBeenCalled();
    expect(fetchMock).toHaveBeenCalledTimes(1);
    const [url, init] = fetchMock.mock.calls[0] ?? [];
    expect(url).toBe("/api/auth/session");
    expect(init).toMatchObject({
      method: "POST",
      credentials: "include",
      body: JSON.stringify({ idToken: "id-token" }),
    });
    expect(JSON.stringify(init)).not.toContain("localhost:8080");
  });

  it("uses the redirect flow when the popup cannot open", async () => {
    firebaseAuth.signInWithGooglePopup.mockRejectedValue({
      code: "auth/popup-blocked",
    });
    render(<LoginPage />);

    fireEvent.click(
      screen.getByRole("button", { name: "Sign in with Google" }),
    );

    await waitFor(() => {
      expect(firebaseAuth.signInWithGoogleRedirect).toHaveBeenCalledTimes(1);
    });
    expect(fetchMock).not.toHaveBeenCalled();
    expect(navigation.replace).not.toHaveBeenCalled();
  });

  it("posts a redirect result and stays on login when sign-in fails", async () => {
    firebaseAuth.consumeGoogleRedirect.mockResolvedValue("redirect-token");
    fetchMock.mockResolvedValue(new Response(null, { status: 204 }));
    const { unmount } = render(<LoginPage />);

    await waitFor(() => {
      expect(navigation.replace).toHaveBeenCalledWith("/main");
    });
    const [url, init] = fetchMock.mock.calls[0] ?? [];
    expect(url).toBe("/api/auth/session");
    expect(init).toMatchObject({
      body: JSON.stringify({ idToken: "redirect-token" }),
    });

    unmount();
    navigation.replace.mockReset();
    fetchMock.mockReset();
    firebaseAuth.consumeGoogleRedirect.mockResolvedValue(null);
    firebaseAuth.signInWithGooglePopup.mockRejectedValue(
      new FirebaseNotConfiguredError(),
    );
    render(<LoginPage />);
    fireEvent.click(
      screen.getByRole("button", { name: "Sign in with Google" }),
    );

    expect((await screen.findByRole("alert")).textContent).toBe(
      "Google sign-in is not configured.",
    );
    expect(navigation.replace).not.toHaveBeenCalled();
  });
});
