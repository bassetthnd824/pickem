/**
 * `GET /api/auth/me` body. Roles mirror `users.roles`.
 */

export type AuthProfile = {
  uid: string;
  emailAddr: string;
  firstName: string;
  lastName: string;
  nickName: string;
  themeId: string;
  roles: string[];
};

export function profileIsManager(
  roles: readonly string[] | null | undefined,
): boolean {
  return (
    roles?.some((role) => role.trim().toLowerCase() === "manager") ?? false
  );
}

function stringField(value: unknown): string {
  return typeof value === "string" ? value : "";
}

export function parseAuthProfile(value: unknown): AuthProfile | null {
  if (!value || typeof value !== "object") {
    return null;
  }
  const record = value as Record<string, unknown>;
  if (typeof record.uid !== "string" || record.uid.trim() === "") {
    return null;
  }
  const roles = Array.isArray(record.roles)
    ? record.roles.filter((role): role is string => typeof role === "string")
    : [];
  return {
    uid: record.uid,
    emailAddr: stringField(record.emailAddr),
    firstName: stringField(record.firstName),
    lastName: stringField(record.lastName),
    nickName: stringField(record.nickName),
    themeId: stringField(record.themeId),
    roles,
  };
}
