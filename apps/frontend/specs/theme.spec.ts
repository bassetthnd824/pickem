import { readFileSync, readdirSync, statSync } from "node:fs";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import {
  DEFAULT_THEME,
  THEME_KEYS,
  isThemeKey,
  resolveThemeId,
} from "../lib/theme";

const frontendRoot = resolve(dirname(fileURLToPath(import.meta.url)), "..");

const BRAND_COLORS: Record<
  (typeof THEME_KEYS)[number],
  { primary: string; secondary: string }
> = {
  light: { primary: "#1F2937", secondary: "#2563EB" },
  dark: { primary: "#0F172A", secondary: "#38BDF8" },
  alabama: { primary: "#9E1B32", secondary: "#828A8F" },
  arkansas: { primary: "#9D2235", secondary: "#FFFFFF" },
  auburn: { primary: "#0C2340", secondary: "#E87722" },
  florida: { primary: "#0021A5", secondary: "#FA4616" },
  georgia: { primary: "#BA0C2F", secondary: "#000000" },
  kentucky: { primary: "#0033A0", secondary: "#000000" },
  lsu: { primary: "#461D7C", secondary: "#FDD023" },
  "mississippi-state": { primary: "#660000", secondary: "#75787B" },
  missouri: { primary: "#000000", secondary: "#F1B82D" },
  oklahoma: { primary: "#841617", secondary: "#FDF9D8" },
  "ole-miss": { primary: "#CE1126", secondary: "#14213D" },
  "south-carolina": { primary: "#73000A", secondary: "#000000" },
  tennessee: { primary: "#FF8200", secondary: "#58595B" },
  texas: { primary: "#BF5700", secondary: "#333F48" },
  "texas-am": { primary: "#500000", secondary: "#FFFFFF" },
  vanderbilt: { primary: "#000000", secondary: "#866D4B" },
};

const TOKEN_NAMES = [
  "--color-bg",
  "--color-surface",
  "--color-text",
  "--color-muted",
  "--color-border",
  "--color-primary",
  "--color-primary-fg",
  "--color-secondary",
  "--color-accent",
  "--color-success",
  "--color-danger",
] as const;

function themeBlock(css: string, key: string): string {
  const pattern = new RegExp(
    `\\[data-theme="${key}"\\][^{]*\\{([^}]*)\\}`,
    "i",
  );
  const match = pattern.exec(css);
  if (!match?.[1]) {
    throw new Error(`missing data-theme block for ${key}`);
  }
  return match[1];
}

function sourceFiles(directory: string): string[] {
  const files: string[] = [];
  for (const entry of readdirSync(directory)) {
    const path = join(directory, entry);
    const stats = statSync(path);
    if (stats.isDirectory()) {
      files.push(...sourceFiles(path));
      continue;
    }
    if (/\.(tsx|ts|jsx|js|css)$/.test(entry)) {
      files.push(path);
    }
  }
  return files;
}

describe("resolveThemeId", () => {
  it("uses Light when the profile and cookie are missing", () => {
    expect(resolveThemeId(undefined, undefined)).toBe(DEFAULT_THEME);
    expect(resolveThemeId(null, null)).toBe("light");
    expect(resolveThemeId("", "")).toBe("light");
  });

  it("ignores an unknown theme key", () => {
    expect(resolveThemeId("not-a-theme", "also-no")).toBe("light");
    expect(resolveThemeId(undefined, "Alabama")).toBe("light");
    expect(isThemeKey("light")).toBe(true);
    expect(isThemeKey("prefers-color-scheme")).toBe(false);
  });

  it("prefers a known profile theme over the cookie", () => {
    expect(resolveThemeId("alabama", "dark")).toBe("alabama");
  });

  it("uses the cookie when the profile theme is unknown", () => {
    expect(resolveThemeId(undefined, "vanderbilt")).toBe("vanderbilt");
    expect(resolveThemeId("nope", "texas-am")).toBe("texas-am");
  });
});

describe("theme stylesheet", () => {
  const css = readFileSync(join(frontendRoot, "app", "global.css"), "utf8");

  it("defines exactly the 18 documented theme keys", () => {
    const keys = [...css.matchAll(/\[data-theme="([^"]+)"\]/g)].map(
      (match) => match[1],
    );
    expect(keys).toEqual([...THEME_KEYS]);
    expect(THEME_KEYS).toHaveLength(18);
  });

  it("paints Light from :root when data-theme is absent", () => {
    const lightRule = css.slice(0, css.indexOf('[data-theme="dark"]'));
    expect(lightRule).toContain(":root");
    expect(lightRule).toContain('[data-theme="light"]');
    expect(lightRule.toLowerCase()).toContain("--color-primary: #1f2937");
  });

  it("stores each documented primary and secondary hex in that theme block", () => {
    for (const key of THEME_KEYS) {
      const block = themeBlock(css, key).toLowerCase();
      const brand = BRAND_COLORS[key];
      expect(`${key}\n${block}`).toContain(
        `--color-primary: ${brand.primary.toLowerCase()}`,
      );
      expect(`${key}\n${block}`).toContain(
        `--color-secondary: ${brand.secondary.toLowerCase()}`,
      );
      for (const token of TOKEN_NAMES) {
        expect(`${key} ${token}\n${block}`).toMatch(
          new RegExp(`${token}:\\s*#[0-9a-f]{6}`),
        );
      }
    }
  });
});

describe("semantic color utilities", () => {
  const roots = ["app", "components", "lib"].map((name) =>
    join(frontendRoot, name),
  );

  it("keeps hex colors and palette utilities out of components", () => {
    const offenders: string[] = [];
    const palette =
      /(?:bg|text|border|fill|stroke|ring|outline|divide|from|to|via)-(?:neutral|slate|gray|zinc|stone|red|green|blue|sky|emerald|rose|amber|yellow|orange|purple|pink|indigo|teal|cyan|lime|fuchsia|violet|black|white)\b/;

    for (const root of roots) {
      for (const file of sourceFiles(root)) {
        if (file.endsWith(`${join("app", "global.css")}`)) {
          continue;
        }
        const source = readFileSync(file, "utf8");
        if (/#[0-9a-fA-F]{3,8}\b/.test(source) || palette.test(source)) {
          offenders.push(file);
        }
      }
    }

    expect(offenders).toEqual([]);
  });
});
