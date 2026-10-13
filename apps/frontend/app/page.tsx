import { PickResultIcon } from "../components/pick-result-icon";

export default function HomePage() {
  return (
    <main className="mx-auto flex max-w-3xl flex-col gap-6 px-6 py-16">
      <header className="rounded-md bg-primary px-4 py-3">
        <h1 className="text-3xl font-semibold tracking-tight text-primary-fg">
          Kenney&apos;s Pickem
        </h1>
      </header>
      <p className="text-base text-muted">
        Season-long college football confidence pick&apos;em.
      </p>
      <section
        aria-label="Pick results"
        className="flex flex-wrap items-center gap-6 rounded-md border border-border bg-surface px-4 py-3"
      >
        <PickResultIcon result="correct" />
        <PickResultIcon result="incorrect" />
      </section>
    </main>
  );
}
