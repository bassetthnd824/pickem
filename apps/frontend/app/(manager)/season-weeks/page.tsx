import type { Metadata } from "next";
import { TitledPage } from "../../../components/titled-page";

export const metadata: Metadata = {
  title: "Season Weeks · Kenney's Pickem",
};

export default function SeasonWeeksPage() {
  return <TitledPage title="Season Weeks" />;
}
