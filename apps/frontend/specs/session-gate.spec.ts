import { config } from "../middleware";
import {
  GAME_ROUTE_PREFIXES,
  MANAGER_ROUTE_PREFIXES,
  gateDecision,
  readSessionClaims,
  type SessionClaims,
} from "../lib/session-gate";

const NOW = 1_700_000_000_000;

function sessionToken(payload: Record<string, unknown>): string {
  const encode = (value: unknown) =>
    Buffer.from(JSON.stringify(value)).toString("base64url");
  return `${encode({ alg: "none", typ: "JWT" })}.${encode(payload)}.sig`;
}

function playerToken(extra: Record<string, unknown> = {}): string {
  return sessionToken({
    sub: "player-1",
    exp: Math.floor(NOW / 1000) + 3600,
    player: true,
    roles: ["player"],
    ...extra,
  });
}

const player: SessionClaims = { signedIn: true, isManager: false };
const manager: SessionClaims = { signedIn: true, isManager: true };
const signedOut: SessionClaims = { signedIn: false, isManager: false };

describe("readSessionClaims", () => {
  it("reads a manager claim from the session cookie payload", () => {
    expect(
      readSessionClaims(
        playerToken({ manager: true, roles: ["manager"] }),
        NOW,
      ),
    ).toEqual(manager);
  });

  it("treats a roles entry of manager as the manager claim", () => {
    expect(
      readSessionClaims(playerToken({ roles: ["player", "manager"] }), NOW),
    ).toEqual(manager);
  });

  it("signs in a player without the manager claim", () => {
    expect(readSessionClaims(playerToken(), NOW)).toEqual(player);
  });

  it("rejects a missing, malformed, or expired cookie", () => {
    expect(readSessionClaims(undefined, NOW)).toEqual(signedOut);
    expect(readSessionClaims("not-a-jwt", NOW)).toEqual(signedOut);
    expect(
      readSessionClaims(
        sessionToken({
          sub: "player-1",
          exp: Math.floor(NOW / 1000) - 10,
          manager: true,
        }),
        NOW,
      ),
    ).toEqual(signedOut);
    expect(
      readSessionClaims(
        sessionToken({ exp: Math.floor(NOW / 1000) + 10 }),
        NOW,
      ),
    ).toEqual(signedOut);
  });
});

describe("gateDecision", () => {
  it("sends an unauthenticated game or manager visit to login", () => {
    expect(gateDecision("/main", signedOut)).toBe("login");
    expect(gateDecision("/leaderboard/", signedOut)).toBe("login");
    expect(gateDecision("/team-schedule", signedOut)).toBe("login");
    expect(gateDecision("/account", signedOut)).toBe("login");
    expect(gateDecision("/users", signedOut)).toBe("login");
    expect(gateDecision("/season-weeks/1", signedOut)).toBe("login");
  });

  it("allows a signed-in player on game routes and restricts manager routes", () => {
    expect(gateDecision("/main", player)).toBe("allow");
    expect(gateDecision("/team-schedule", player)).toBe("allow");
    expect(gateDecision("/users", player)).toBe("restricted");
    expect(gateDecision("/teams", player)).toBe("restricted");
    expect(gateDecision("/seasons", player)).toBe("restricted");
    expect(gateDecision("/season-weeks", player)).toBe("restricted");
  });

  it("allows a manager on manager routes", () => {
    expect(gateDecision("/users", manager)).toBe("allow");
    expect(gateDecision("/matchups/new", manager)).toBe("allow");
  });

  it("sends a signed-in visitor from login or the public home to main", () => {
    expect(gateDecision("/login", player)).toBe("main");
    expect(gateDecision("/", manager)).toBe("main");
    expect(gateDecision("/login", signedOut)).toBe("allow");
    expect(gateDecision("/", signedOut)).toBe("allow");
  });
});

describe("middleware matcher", () => {
  it("includes every game and manager route", () => {
    expect(Array.isArray(config.matcher)).toBe(true);
    if (!Array.isArray(config.matcher)) {
      return;
    }
    for (const prefix of [
      "/",
      "/login",
      ...GAME_ROUTE_PREFIXES,
      ...MANAGER_ROUTE_PREFIXES,
    ]) {
      expect(config.matcher).toContain(prefix);
    }
  });
});
