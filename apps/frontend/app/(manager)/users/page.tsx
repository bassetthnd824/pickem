import type { Metadata } from "next";
import { TitledPage } from "../../../components/titled-page";

export const metadata: Metadata = { title: "Users · Kenney's Pickem" };

export default function UsersPage() {
  return <TitledPage title="Users" />;
}
