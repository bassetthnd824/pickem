import type { Metadata } from "next";
import { TitledPage } from "../../../components/titled-page";

export const metadata: Metadata = { title: "Matchups · Kenney's Pickem" };

export default function MatchupsPage() {
  return <TitledPage title="Matchups" />;
}
