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
    redirect("/admin/login");
  }

  return (
    <div className="min-h-screen flex flex-col">
      {/* Admin Header */}
      <header className="border-b border-[var(--border)] px-6 py-4">
        <div className="max-w-7xl mx-auto flex items-center justify-between">
          <div className="flex items-center gap-6">
            <Link href="/admin" className="text-xl font-bold bg-gradient-to-r from-blue-400 to-purple-600 bg-clip-text text-transparent">
              Ramble Admin
            </Link>
            <nav className="hidden md:flex items-center gap-4 text-sm">
              <Link href="/admin" className="text-[var(--muted)] hover:text-white transition-colors">
                Dashboard
              </Link>
              <Link href="/admin/users" className="text-[var(--muted)] hover:text-white transition-colors">
                Users
              </Link>
              <Link href="/admin/vouchers" className="text-[var(--muted)] hover:text-white transition-colors">
                Vouchers
              </Link>
              <Link href="/admin/transcribe" className="text-[var(--muted)] hover:text-white transition-colors">
                Transcribe
              </Link>
            </nav>
          </div>
          
          <div className="flex items-center gap-4">
            <span className="text-sm text-[var(--muted)]">{user.email}</span>
            <form action="/api/auth/logout" method="POST">
              <button
                type="submit"
                className="text-sm text-[var(--muted)] hover:text-white transition-colors"
              >
                Sign out
              </button>
            </form>
          </div>
        </div>
      </header>

      {/* Content */}
      <main className="flex-1 p-6">
        <div className="max-w-7xl mx-auto">
          {children}
        </div>
      </main>
    </div>
  );
}
