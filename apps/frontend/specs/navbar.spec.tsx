import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { AuthProvider } from "../components/auth-provider";
import { Navbar } from "../components/navbar";
import { QueryProvider } from "../components/query-provider";
import type { AuthProfile } from "../lib/auth-profile";
import { MANAGER_LINKS } from "../lib/nav-links";

const navigation = vi.hoisted(() => ({
  replace: vi.fn<(path: string) => void>(),
  refresh: vi.fn<() => void>(),
}));

const firebaseAuth = vi.hoisted(() => ({
  signOutFromGoogle: vi.fn<() => Promise<void>>(async () => undefined),
}));

vi.mock("next/navigation", () => ({
  useRouter: () => navigation,
}));

vi.mock("../lib/firebase-auth", () => firebaseAuth);

const player: AuthProfile = {
  uid: "player-1",
  emailAddr: "player@example.com",
  firstName: "Pat",
  lastName: "Player",
  nickName: "Pat",
  themeId: "light",
  roles: ["player"],
};

const manager: AuthProfile = {
  ...player,
  uid: "manager-1",
  roles: ["player", "manager"],
};

function renderNavbar(profile: AuthProfile, sessionIsManager = false) {
  return render(
    <QueryProvider>
      <AuthProvider
        initialProfile={profile}
        sessionPresent
        sessionIsManager={sessionIsManager}
      >
        <Navbar />
      </AuthProvider>
    </QueryProvider>,
  );
}

describe("Navbar", () => {
  const fetchMock = vi.fn<typeof fetch>();

  beforeEach(() => {
    fetchMock.mockReset();
    navigation.replace.mockReset();
    navigation.refresh.mockReset();
    firebaseAuth.signOutFromGoogle.mockClear();
    fetchMock.mockResolvedValue(new Response(null, { status: 204 }));
    vi.stubGlobal("fetch", fetchMock);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("shows game links and hides Admin for a player", () => {
    renderNavbar(player);

    expect(
      screen.getByRole("link", { name: "Home" }).getAttribute("href"),
    ).toBe("/main");
    expect(
      screen.getByRole("link", { name: "Leader Board" }).getAttribute("href"),
    ).toBe("/leaderboard");
    expect(
      screen.getByRole("link", { name: "Team Schedule" }).getAttribute("href"),
    ).toBe("/team-schedule");
    expect(
      screen.getByRole("link", { name: "My Account" }).getAttribute("href"),
    ).toBe("/account");
    expect(screen.getByRole("button", { name: "Logout" })).toBeTruthy();
    expect(screen.queryByRole("button", { name: "Admin" })).toBeNull();
  });

  it("opens the manager links from Admin", () => {
    renderNavbar(manager, true);
    fireEvent.click(screen.getByRole("button", { name: "Admin" }));

    for (const link of MANAGER_LINKS) {
      const item = screen.getByRole("menuitem", { name: link.label });
      expect(item.getAttribute("href")).toBe(link.href);
      expect(item.closest("[class*='data-anchor-hidden:hidden']")).toBeTruthy();
      expect(item.closest("[class*='overflow-y-auto']")).toBeTruthy();
      expect(
        item.closest("[class*='max-h-[var(--available-height)]']"),
      ).toBeTruthy();
    }
  });

  it("closes the admin menu when the mobile menu closes", () => {
    const firstManagerLink = MANAGER_LINKS[0];
    renderNavbar(manager, true);
    fireEvent.click(screen.getByRole("button", { name: "Menu" }));
    fireEvent.click(screen.getByRole("button", { name: "Admin" }));
    expect(
      screen.getByRole("menuitem", { name: firstManagerLink.label }),
    ).toBeTruthy();

    fireEvent.click(screen.getByRole("button", { name: "Menu" }));

    expect(
      screen.queryByRole("menuitem", { name: firstManagerLink.label }),
    ).toBeNull();
  });

  it("logs out through the session endpoint and returns to login", async () => {
    renderNavbar(player);
    fireEvent.click(screen.getByRole("button", { name: "Logout" }));

    await waitFor(() => {
      expect(navigation.replace).toHaveBeenCalledWith("/login");
    });
    expect(navigation.refresh).toHaveBeenCalled();
    expect(firebaseAuth.signOutFromGoogle).toHaveBeenCalled();
    const logoutCall = fetchMock.mock.calls.find(
      (call) => call[0] === "/api/auth/logout",
    );
    expect(logoutCall?.[1]).toMatchObject({
      method: "POST",
      credentials: "include",
    });
  });

  it("hides Admin when the session cookie is not a manager", () => {
    renderNavbar(manager, false);
    expect(screen.queryByRole("button", { name: "Admin" })).toBeNull();
  });

  it("stays on the page and shows an error when logout fails", async () => {
    fetchMock.mockResolvedValue(
      new Response(
        JSON.stringify({ detail: "Sign-in service is unavailable" }),
        {
          status: 503,
          headers: { "content-type": "application/json" },
        },
      ),
    );
    renderNavbar(player);
    fireEvent.click(screen.getByRole("button", { name: "Logout" }));

    expect((await screen.findByRole("alert")).textContent).toBe(
      "Sign-out failed. Try again.",
    );
    expect(navigation.replace).not.toHaveBeenCalled();
  });
});
