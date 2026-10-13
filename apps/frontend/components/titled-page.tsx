import type { ReactNode } from "react";

export function TitledPage({
  title,
  children,
}: {
  title: string;
  children?: ReactNode;
}) {
  return (
    <main className="mx-auto flex max-w-3xl flex-col gap-4 px-6 py-10">
      <h1 className="text-2xl font-semibold text-text">{title}</h1>
      {children}
    </main>
  );
}
