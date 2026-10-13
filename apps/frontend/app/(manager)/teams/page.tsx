import type { Metadata } from "next";
import { TitledPage } from "../../../components/titled-page";

export const metadata: Metadata = { title: "Teams · Kenney's Pickem" };

export default function TeamsPage() {
  return <TitledPage title="Teams" />;
}
