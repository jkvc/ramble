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
      <h1 className="text-2xl font-bold mb-8">Dashboard</h1>

      {/* Stats Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-12">
        <StatCard
          title="Total Users"
          value={stats.totalUsers.toString()}
          icon="👥"
        />
        <StatCard
          title="Active Subscriptions"
          value={stats.activeSubscriptions.toString()}
          icon="💳"
        />
        <StatCard
          title="Vouchers Redeemed"
          value={stats.voucherRedemptions.toString()}
          icon="🎟️"
        />
        <StatCard
          title="Minutes Transcribed"
          value={stats.totalMinutes.toLocaleString()}
          icon="🎙️"
        />
      </div>

      {/* Quick Actions */}
      <h2 className="text-lg font-semibold mb-4">Quick Actions</h2>
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <a
          href="/admin/vouchers"
          className="p-4 bg-[var(--surface)] border border-[var(--border)] rounded-lg hover:bg-[var(--surface-hover)] transition-colors"
        >
          <div className="font-medium mb-1">Create Voucher</div>
          <div className="text-sm text-[var(--muted)]">Generate new voucher codes for users</div>
        </a>
        <a
          href="/admin/users"
          className="p-4 bg-[var(--surface)] border border-[var(--border)] rounded-lg hover:bg-[var(--surface-hover)] transition-colors"
        >
          <div className="font-medium mb-1">Manage Users</div>
          <div className="text-sm text-[var(--muted)]">View and manage user accounts</div>
        </a>
        <a
          href="/admin/transcribe"
          className="p-4 bg-[var(--surface)] border border-[var(--border)] rounded-lg hover:bg-[var(--surface-hover)] transition-colors"
        >
          <div className="font-medium mb-1">Test Transcription</div>
          <div className="text-sm text-[var(--muted)]">Try the Soniox integration</div>
        </a>
      </div>
    </div>
  );
}

function StatCard({ title, value, icon }: { title: string; value: string; icon: string }) {
  return (
    <div className="p-6 bg-[var(--surface)] border border-[var(--border)] rounded-xl">
      <div className="flex items-center justify-between mb-2">
        <span className="text-2xl">{icon}</span>
      </div>
      <div className="text-3xl font-bold mb-1">{value}</div>
      <div className="text-sm text-[var(--muted)]">{title}</div>
    </div>
  );
}
