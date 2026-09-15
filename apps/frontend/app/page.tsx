import styles from "./page.module.css";

export default function Index() {
  return (
    <main className={styles.page}>
      <h1>Kenney&apos;s Pickem</h1>
      <p>
        Nx workspace is up. Frontend lives in <code>apps/frontend</code>. Backend
        lives in <code>apps/backend</code>.
      </p>
    </main>
  );
}
