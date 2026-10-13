import type { Metadata } from "next";
import { TitledPage } from "../../../components/titled-page";

export const metadata: Metadata = {
  title: "Access restricted · Kenney's Pickem",
};

export default function RestrictedPage() {
  return (
    <TitledPage title="Access restricted">
      <p className="text-muted">This area is for managers.</p>
    </TitledPage>
  );
}
