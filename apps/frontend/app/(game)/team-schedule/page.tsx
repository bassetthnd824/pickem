import type { Metadata } from "next";
import { TitledPage } from "../../../components/titled-page";

export const metadata: Metadata = {
  title: "Team Schedule · Kenney's Pickem",
};

export default function TeamSchedulePage() {
  return <TitledPage title="Team Schedule" />;
}
