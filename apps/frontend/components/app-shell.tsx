import type { ReactNode } from "react";
import { Navbar } from "./navbar";

export function AppShell({ children }: { children: ReactNode }) {
  return (
    <div className="min-h-dvh bg-bg text-text">
      <Navbar />
      {children}
    </div>
  );
}
