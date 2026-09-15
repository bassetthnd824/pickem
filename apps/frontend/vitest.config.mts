/// <reference types="vitest" />
import react from "@vitejs/plugin-react-swc";
import tsconfigPaths from "vite-tsconfig-paths";
import { defineConfig } from "vitest/config";

export default defineConfig({
  root: import.meta.dirname,
  cacheDir: "../../node_modules/.vite/apps/frontend",
  plugins: [react(), tsconfigPaths()],
  test: {
    name: "frontend",
    watch: false,
    globals: true,
    environment: "jsdom",
    include: [
      "{src,specs,app,tests}/**/*.{test,spec}.{js,mjs,cjs,ts,mts,cts,jsx,tsx}",
    ],
    reporters: ["default"],
    coverage: {
      reportsDirectory: "../../coverage/apps/frontend",
      provider: "v8",
    },
  },
});
