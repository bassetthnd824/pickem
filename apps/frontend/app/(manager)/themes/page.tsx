import type { Metadata } from "next";
import { TitledPage } from "../../../components/titled-page";

export const metadata: Metadata = { title: "Themes · Kenney's Pickem" };

export default function ThemesPage() {
  return <TitledPage title="Themes" />;
}
