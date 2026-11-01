const { spawn } = require("node:child_process");
const { readFileSync } = require("node:fs");
const path = require("node:path");

const backendDir = path.resolve(__dirname, "..");
const repoRoot = path.resolve(backendDir, "../..");
const envFile = path.join(backendDir, ".env.test");
const text = readFileSync(envFile, "utf8");
const env = { ...process.env };

for (const line of text.split(/\r?\n/)) {
  const trimmed = line.trim();
  if (trimmed === "" || trimmed.startsWith("#")) {
    continue;
  }
  const separator = trimmed.indexOf("=");
  if (separator <= 0) {
    continue;
  }
  const key = trimmed.slice(0, separator).trim();
  let value = trimmed.slice(separator + 1).trim();
  if (
    (value.startsWith('"') && value.endsWith('"')) ||
    (value.startsWith("'") && value.endsWith("'"))
  ) {
    value = value.slice(1, -1);
  }
  if (value === "") {
    delete env[key];
  } else {
    env[key] = value;
  }
}

const mvnw = path.join(repoRoot, process.platform === "win32" ? "mvnw.cmd" : "mvnw");
const args = ["test", "-pl", ":backend", ...process.argv.slice(2)];
const child = process.platform === "win32"
  ? spawn([quote(mvnw), ...args.map(quote)].join(" "), {
      cwd: repoRoot,
      env,
      stdio: "inherit",
      shell: true,
    })
  : spawn(mvnw, args, { cwd: repoRoot, env, stdio: "inherit" });

function quote(value) {
  return `"${value.replaceAll('"', '\\"')}"`;
}

child.on("exit", (code, signal) => {
  if (signal) {
    process.kill(process.pid, signal);
    return;
  }
  process.exit(code === null ? 1 : code);
});

child.on("error", (error) => {
  console.error(error.message);
  process.exit(1);
});
