import type { Metadata } from "next";
import { TitledPage } from "../../../components/titled-page";

export const metadata: Metadata = { title: "Home · Kenney's Pickem" };

export default function MainPage() {
  return <TitledPage title="Home" />;
}
