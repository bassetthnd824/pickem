import { parseAuthProfile, profileIsManager } from "../lib/auth-profile";

describe("parseAuthProfile", () => {
  it("reads the session profile and manager role", () => {
    expect(
      parseAuthProfile({
        uid: "uid-1",
        emailAddr: "a@example.com",
        firstName: "Kenney",
        lastName: "Curlee",
        nickName: "Kenney",
        themeId: "alabama",
        roles: ["player", "manager"],
      }),
    ).toEqual({
      uid: "uid-1",
      emailAddr: "a@example.com",
      firstName: "Kenney",
      lastName: "Curlee",
      nickName: "Kenney",
      themeId: "alabama",
      roles: ["player", "manager"],
    });
    expect(profileIsManager(["player", "manager"])).toBe(true);
    expect(profileIsManager(["player"])).toBe(false);
  });

  it("rejects a body without a uid", () => {
    expect(parseAuthProfile({ roles: ["manager"] })).toBeNull();
    expect(parseAuthProfile(null)).toBeNull();
  });
});
