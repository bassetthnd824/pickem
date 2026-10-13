import { readFileSync } from "node:fs";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

describe("frontend stack", () => {
  it("does not depend on moment or a Bootstrap datetime picker", () => {
    const packageJsonPath = resolve(
      dirname(fileURLToPath(import.meta.url)),
      "../../../package.json",
    );
    const packageJson = JSON.parse(readFileSync(packageJsonPath, "utf8")) as {
      dependencies?: Record<string, string>;
      devDependencies?: Record<string, string>;
    };
    const names = [
      ...Object.keys(packageJson.dependencies ?? {}),
      ...Object.keys(packageJson.devDependencies ?? {}),
    ];

    expect(names).not.toContain("moment");
    expect(
      names.some((name) => name.toLowerCase().includes("datetimepicker")),
    ).toBe(false);
    expect(names).toContain("@tanstack/react-query");
    expect(names).toContain("@tanstack/react-form");
    expect(names).toContain("valibot");
    expect(names).toContain("tailwindcss");
  });
});
