import { readdirSync, statSync } from "node:fs";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";

function walk(dir: string): string[] {
  const found: string[] = [];
  for (const name of readdirSync(dir)) {
    const path = join(dir, name);
    if (statSync(path).isDirectory()) {
      found.push(...walk(path));
    } else {
      found.push(path);
    }
  }
  return found;
}

describe("app routes", () => {
  it("does not add a register route", () => {
    const appDir = resolve(dirname(fileURLToPath(import.meta.url)), "../app");
    const files = walk(appDir).map((file) => file.replaceAll("\\", "/"));
    expect(files.some((file) => file.includes("/register"))).toBe(false);
  });
});
