import type { ReactNode } from "react";
import { QueryProvider } from "../components/query-provider";
import "./global.css";

export const metadata = {
  title: "Kenney's Pickem",
  description: "Season-long college football confidence pick'em",
};

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    <html lang="en">
      <body className="antialiased">
        <QueryProvider>{children}</QueryProvider>
      </body>
    </html>
  );
}
