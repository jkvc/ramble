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
      <header className="border-b border-[var(--border)] px-4 sm:px-6 py-3">
        <div className="w-full max-w-xl mx-auto flex items-center justify-between">
          <Link href="/dashboard" className="text-lg font-semibold text-[var(--foreground)]">
            Ramble
          </Link>
          
          <div className="flex items-center gap-3 sm:gap-4">
            {!access.hasAccess && (
              <Link
                href="/dashboard/redeem"
                className="text-sm text-[var(--accent)] hover:underline hidden sm:inline"
              >
                Redeem
              </Link>
            )}
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

      {/* Access Warning */}
      {!access.hasAccess && (
        <div className="bg-amber-50 border-b border-amber-200 px-4 sm:px-6 py-2">
          <div className="w-full max-w-xl mx-auto flex items-center justify-between gap-2">
            <span className="text-amber-700 text-sm">
              Access required for transcription.
            </span>
            <Link
              href="/dashboard/redeem"
              className="px-3 py-1 bg-amber-100 hover:bg-amber-200 text-amber-800 text-sm font-medium rounded transition-colors whitespace-nowrap"
            >
              Redeem Voucher
            </Link>
          </div>
        </div>
      )}

      {/* Content */}
      <main className="flex-1 px-4 sm:px-6 py-6">
        <div className="w-full max-w-xl mx-auto">
          {children}
        </div>
      </main>
    </div>
  );
}
