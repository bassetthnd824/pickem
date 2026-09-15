import "./global.css";

export const metadata = {
  title: "Kenney's Pickem",
  description: "Season-long college football confidence pick'em",
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}
