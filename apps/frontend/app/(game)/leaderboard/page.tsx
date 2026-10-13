import type { Metadata } from "next";
import { TitledPage } from "../../../components/titled-page";

export const metadata: Metadata = {
  title: "Leader Board · Kenney's Pickem",
};

export default function LeaderboardPage() {
  return <TitledPage title="Leader Board" />;
}
