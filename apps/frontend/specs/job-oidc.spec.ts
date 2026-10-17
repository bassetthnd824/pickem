import { verifySchedulerOidc } from "../lib/server/job-oidc";

describe("verifySchedulerOidc", () => {
  it("rejects a token that is not a Google ID token", async () => {
    const audience = "https://pickem.example/api/internal/jobs/cfbd-scores";
    const emails = ["scheduler@pickem.iam.gserviceaccount.com"];
    await expect(
      verifySchedulerOidc("not-a-jwt", audience, emails),
    ).resolves.toBe(false);
    await expect(verifySchedulerOidc("a.b.c", "", emails)).resolves.toBe(false);
    await expect(verifySchedulerOidc("a.b.c", audience, [])).resolves.toBe(
      false,
    );
  });
});
