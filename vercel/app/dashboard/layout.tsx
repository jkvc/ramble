import Link from "next/link";
import { redirect } from "next/navigation";
import { createClient } from "@/lib/supabase/server";
import { checkAccess } from "@/lib/access";

export default async function DashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const supabase = await createClient();
  const { data: { user } } = await supabase.auth.getUser();

  if (!user) {
    redirect("/login");
  }

  const access = await checkAccess(user.id, user.email, supabase);

  return (
    <div className="min-h-screen flex flex-col">
      {/* Header */}
      <header className="border-b border-[var(--border)] px-6 py-4">
        <div className="max-w-5xl mx-auto flex items-center justify-between">
          <Link href="/dashboard" className="text-xl font-bold bg-gradient-to-r from-blue-400 to-purple-600 bg-clip-text text-transparent">
            Ramble
          </Link>
          
          <div className="flex items-center gap-4">
            {!access.hasAccess && (
              <Link
                href="/dashboard/redeem"
                className="text-sm text-[var(--accent)] hover:underline"
              >
                Redeem Voucher
              </Link>
            )}
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

      {/* Access Warning */}
      {!access.hasAccess && (
        <div className="bg-yellow-500/10 border-b border-yellow-500/20 px-6 py-3">
          <div className="max-w-5xl mx-auto flex items-center justify-between">
            <span className="text-yellow-400 text-sm">
              You need a subscription or voucher to use transcription.
            </span>
            <Link
              href="/dashboard/redeem"
              className="px-4 py-1 bg-yellow-500/20 hover:bg-yellow-500/30 text-yellow-400 text-sm font-medium rounded transition-colors"
            >
              Redeem Voucher
            </Link>
          </div>
        </div>
      )}

      {/* Content */}
      <main className="flex-1 p-6">
        <div className="max-w-5xl mx-auto">
          {children}
        </div>
      </main>
    </div>
  );
}
