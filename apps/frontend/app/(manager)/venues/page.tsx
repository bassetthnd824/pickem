import type { Metadata } from "next";
import { TitledPage } from "../../../components/titled-page";

export const metadata: Metadata = { title: "Venues · Kenney's Pickem" };

export default function VenuesPage() {
  return <TitledPage title="Venues" />;
}
