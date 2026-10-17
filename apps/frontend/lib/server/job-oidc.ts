import "server-only";
import { OAuth2Client } from "google-auth-library";

const client = new OAuth2Client();

/**
 * Verifies a Cloud Scheduler OIDC token for an allowlisted account.
 * A blank audience, an empty allowlist, or a malformed token returns false
 * without a certificate fetch. A verified Google email is not enough.
 * Failures are not logged: the message can echo the token.
 */
export async function verifySchedulerOidc(
  idToken: string,
  audience: string,
  allowedEmails: readonly string[],
): Promise<boolean> {
  if (
    audience.trim() === "" ||
    allowedEmails.length === 0 ||
    idToken.split(".").length !== 3
  ) {
    return false;
  }
  try {
    const ticket = await client.verifyIdToken({ idToken, audience });
    const payload = ticket.getPayload();
    const email = payload?.email;
    if (
      !payload ||
      payload.email_verified !== true ||
      typeof email !== "string"
    ) {
      return false;
    }
    return allowedEmails.includes(email);
  } catch {
    return false;
  }
}
