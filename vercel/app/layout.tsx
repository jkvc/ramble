import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "Ramble - Voice to Text",
  description: "Real-time speech-to-text transcription powered by Soniox",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <body className="antialiased min-h-screen">
        {children}
      </body>
    </html>
  );
}
