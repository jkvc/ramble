import { createServiceClient } from "@/lib/supabase/server";

async function getStats() {
  const supabase = await createServiceClient();
  
  // Get total users
  const { count: totalUsers } = await supabase
    .from("profiles")
    .select("*", { count: "exact", head: true });

  // Get active subscriptions
  const { count: activeSubscriptions } = await supabase
    .from("subscriptions")
    .select("*", { count: "exact", head: true })
    .eq("status", "active");

  // Get total voucher redemptions
  const { count: voucherRedemptions } = await supabase
    .from("voucher_redemptions")
    .select("*", { count: "exact", head: true });

  // Get total usage (seconds)
  const { data: usageData } = await supabase
    .from("usage_logs")
    .select("duration_seconds");
  
  const totalSeconds = usageData?.reduce((sum, log) => sum + (log.duration_seconds || 0), 0) || 0;
  const totalMinutes = Math.round(totalSeconds / 60);

  return {
    totalUsers: totalUsers || 0,
    activeSubscriptions: activeSubscriptions || 0,
    voucherRedemptions: voucherRedemptions || 0,
    totalMinutes,
  };
}

export default async function AdminDashboard() {
  const stats = await getStats();

  return (
    <div>
      <h1 className="text-lg font-semibold mb-6">Dashboard</h1>

      {/* Stats Grid */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-3 sm:gap-4 mb-8">
        <StatCard title="Users" value={stats.totalUsers.toString()} />
        <StatCard title="Subscriptions" value={stats.activeSubscriptions.toString()} />
        <StatCard title="Vouchers Redeemed" value={stats.voucherRedemptions.toString()} />
        <StatCard title="Minutes" value={stats.totalMinutes.toLocaleString()} />
      </div>

      {/* Quick Actions */}
      <h2 className="text-sm font-medium text-[var(--muted)] mb-3">Quick Actions</h2>
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
        <a
          href="/admin/vouchers"
          className="p-3 bg-[var(--surface)] border border-[var(--border)] rounded-lg hover:bg-[var(--surface-hover)] transition-colors"
        >
          <div className="text-sm font-medium mb-0.5">Create Voucher</div>
          <div className="text-xs text-[var(--muted)]">Generate new codes</div>
        </a>
        <a
          href="/admin/users"
          className="p-3 bg-[var(--surface)] border border-[var(--border)] rounded-lg hover:bg-[var(--surface-hover)] transition-colors"
        >
          <div className="text-sm font-medium mb-0.5">Manage Users</div>
          <div className="text-xs text-[var(--muted)]">View accounts</div>
        </a>
        <a
          href="/admin/transcribe"
          className="p-3 bg-[var(--surface)] border border-[var(--border)] rounded-lg hover:bg-[var(--surface-hover)] transition-colors"
        >
          <div className="text-sm font-medium mb-0.5">Test Transcription</div>
          <div className="text-xs text-[var(--muted)]">Try Soniox</div>
        </a>
      </div>
    </div>
  );
}

function StatCard({ title, value }: { title: string; value: string }) {
  return (
    <div className="p-4 bg-[var(--surface)] border border-[var(--border)] rounded-lg">
      <div className="text-2xl font-semibold mb-0.5">{value}</div>
      <div className="text-xs text-[var(--muted)]">{title}</div>
    </div>
  );
}
