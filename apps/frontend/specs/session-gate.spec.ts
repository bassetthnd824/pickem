import { readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import {
  claimsFromVerifiedToken,
  gateDecision,
  type SessionClaims,
} from "../lib/session-gate";

const player: SessionClaims = { signedIn: true, isManager: false };
const manager: SessionClaims = { signedIn: true, isManager: true };
const signedOut: SessionClaims = { signedIn: false, isManager: false };

describe("claimsFromVerifiedToken", () => {
  it("reads a manager claim from a verified session", () => {
    expect(
      claimsFromVerifiedToken({
        uid: "manager-1",
        manager: true,
        roles: ["manager"],
      }),
    ).toEqual(manager);
  });

  it("treats a roles entry of manager as the manager claim", () => {
    expect(
      claimsFromVerifiedToken({
        uid: "manager-1",
        roles: ["player", "manager"],
      }),
    ).toEqual(manager);
  });

  it("signs in a player without the manager claim", () => {
    expect(
      claimsFromVerifiedToken({
        uid: "player-1",
        roles: ["player"],
      }),
    ).toEqual(player);
  });

  it("rejects a missing token or a token without a uid", () => {
    expect(claimsFromVerifiedToken(undefined)).toEqual(signedOut);
    expect(claimsFromVerifiedToken(null)).toEqual(signedOut);
    expect(claimsFromVerifiedToken({ manager: true })).toEqual(signedOut);
    expect(claimsFromVerifiedToken({ uid: "  ", manager: true })).toEqual(
      signedOut,
    );
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

describe("session gate source", () => {
  it("does not decode an unverified cookie payload", () => {
    const source = readFileSync(
      join(dirname(fileURLToPath(import.meta.url)), "../lib/session-gate.ts"),
      "utf8",
    );
    expect(source).not.toContain("atob");
    expect(source).not.toContain("readSessionClaims");
  });
});
