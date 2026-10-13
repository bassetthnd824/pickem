import type { Metadata } from "next";
import { TitledPage } from "../../../components/titled-page";

export const metadata: Metadata = { title: "My Account · Kenney's Pickem" };

export default function AccountPage() {
  return <TitledPage title="My Account" />;
}
