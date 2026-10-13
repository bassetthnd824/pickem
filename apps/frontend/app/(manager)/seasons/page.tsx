import type { Metadata } from "next";
import { TitledPage } from "../../../components/titled-page";

export const metadata: Metadata = { title: "Seasons · Kenney's Pickem" };

export default function SeasonsPage() {
  return <TitledPage title="Seasons" />;
}
