function messageOf(error: unknown): string | undefined {
  if (typeof error === "string") {
    return error.length > 0 ? error : undefined;
  }
  if (Array.isArray(error)) {
    return fieldErrorMessage(error);
  }
  if (typeof error === "object" && error !== null && "message" in error) {
    const message = error.message;
    if (typeof message === "string" && message.length > 0) {
      return message;
    }
  }
  return undefined;
}

/** First user-visible message from a TanStack Form field error list. */
export function fieldErrorMessage(
  errors: readonly unknown[],
): string | undefined {
  for (const error of errors) {
    const message = messageOf(error);
    if (message) {
      return message;
    }
  }
  return undefined;
}
