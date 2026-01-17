import Link from "next/link";

export default function AuthLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="min-h-screen flex flex-col">
      {/* Header */}
      <header className="border-b border-[var(--border)] px-4 sm:px-6 py-3">
        <div className="w-full max-w-xl mx-auto">
          <Link href="/" className="text-lg font-semibold text-[var(--foreground)]">
            Ramble
          </Link>
        </div>
      </header>
      
      {/* Content */}
      <main className="flex-1 flex items-center justify-center px-4 sm:px-6 py-8 sm:py-12">
        {children}
      </main>
    </div>
  );
}
