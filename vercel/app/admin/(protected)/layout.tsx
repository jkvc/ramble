import Link from "next/link";
import { redirect } from "next/navigation";
import { createClient } from "@/lib/supabase/server";
import { isAdmin } from "@/lib/admin";

export default async function AdminProtectedLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const supabase = await createClient();
  const { data: { user } } = await supabase.auth.getUser();

  if (!user || !isAdmin(user.email)) {
    redirect("/login");
  }

  return (
    <div className="min-h-screen flex flex-col">
      {/* Admin Header */}
      <header className="border-b border-[var(--border)] px-4 sm:px-6 py-3">
        <div className="max-w-5xl mx-auto flex items-center justify-between">
          <div className="flex items-center gap-4 sm:gap-6">
            <Link href="/admin" className="text-lg font-semibold text-[var(--foreground)]">
              Admin
            </Link>
            <nav className="hidden md:flex items-center gap-4 text-sm">
              <Link href="/admin" className="text-[var(--muted)] hover:text-[var(--foreground)] transition-colors">
                Dashboard
              </Link>
              <Link href="/admin/users" className="text-[var(--muted)] hover:text-[var(--foreground)] transition-colors">
                Users
              </Link>
              <Link href="/admin/vouchers" className="text-[var(--muted)] hover:text-[var(--foreground)] transition-colors">
                Vouchers
              </Link>
              <Link href="/admin/transcribe" className="text-[var(--muted)] hover:text-[var(--foreground)] transition-colors">
                Transcribe
              </Link>
            </nav>
          </div>
          
          <div className="flex items-center gap-3 sm:gap-4">
            <span className="text-sm text-[var(--muted)] hidden sm:inline truncate max-w-[150px]">{user.email}</span>
            <form action="/api/auth/logout" method="POST">
              <button
                type="submit"
                className="text-sm text-[var(--muted)] hover:text-[var(--foreground)] transition-colors"
              >
                Sign out
              </button>
            </form>
          </div>
        </div>
      </header>

      {/* Content */}
      <main className="flex-1 px-4 sm:px-6 py-6">
        <div className="max-w-5xl mx-auto">
          {children}
        </div>
      </main>
    </div>
  );
}
