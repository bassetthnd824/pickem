import type { Metadata } from "next";
import { PicksGrid } from "../../../components/picks-grid";
import { TitledPage } from "../../../components/titled-page";

export const metadata: Metadata = { title: "Home · Kenney's Pickem" };

export default function MainPage() {
  return (
    <TitledPage title="Home">
      <PicksGrid />
    </TitledPage>
  );
}
