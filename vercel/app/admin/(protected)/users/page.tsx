import { createServiceClient } from "@/lib/supabase/server";

interface User {
  id: string;
  email: string;
  created_at: string;
  subscription_status: string | null;
  has_voucher: boolean;
}

async function getUsers(): Promise<User[]> {
  const supabase = await createServiceClient();

  // Get all profiles with their subscription and voucher status
  const { data: profiles } = await supabase
    .from("profiles")
    .select(`
      id,
      email,
      created_at,
      subscriptions (status),
      voucher_redemptions (id)
    `)
    .order("created_at", { ascending: false });

  return (profiles || []).map((profile) => ({
    id: profile.id,
    email: profile.email,
    created_at: profile.created_at,
    subscription_status: (profile.subscriptions as { status: string }[])?.[0]?.status || null,
    has_voucher: ((profile.voucher_redemptions as { id: string }[])?.length || 0) > 0,
  }));
}

export default async function AdminUsersPage() {
  const users = await getUsers();

  return (
    <div>
      <div className="flex items-center justify-between mb-8">
        <h1 className="text-2xl font-bold">Users</h1>
        <div className="text-sm text-[var(--muted)]">
          {users.length} total users
        </div>
      </div>

      <div className="bg-[var(--surface)] border border-[var(--border)] rounded-xl overflow-hidden">
        <table className="w-full">
          <thead className="border-b border-[var(--border)]">
            <tr className="text-left text-sm text-[var(--muted)]">
              <th className="px-6 py-4 font-medium">Email</th>
              <th className="px-6 py-4 font-medium">Access</th>
              <th className="px-6 py-4 font-medium">Created</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-[var(--border)]">
            {users.map((user) => (
              <tr key={user.id} className="hover:bg-[var(--surface-hover)]">
                <td className="px-6 py-4">
                  <span className="font-medium">{user.email}</span>
                </td>
                <td className="px-6 py-4">
                  <AccessBadge
                    subscription={user.subscription_status}
                    hasVoucher={user.has_voucher}
                  />
                </td>
                <td className="px-6 py-4 text-[var(--muted)]">
                  {new Date(user.created_at).toLocaleDateString()}
                </td>
              </tr>
            ))}
            {users.length === 0 && (
              <tr>
                <td colSpan={3} className="px-6 py-12 text-center text-[var(--muted)]">
                  No users yet
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function AccessBadge({ subscription, hasVoucher }: { subscription: string | null; hasVoucher: boolean }) {
  if (subscription === "active") {
    return (
      <span className="px-2 py-1 text-xs font-medium bg-green-500/10 text-green-400 border border-green-500/20 rounded">
        Subscribed
      </span>
    );
  }
  
  if (hasVoucher) {
    return (
      <span className="px-2 py-1 text-xs font-medium bg-blue-500/10 text-blue-400 border border-blue-500/20 rounded">
        Voucher
      </span>
    );
  }
  
  return (
    <span className="px-2 py-1 text-xs font-medium bg-[var(--surface-hover)] text-[var(--muted)] border border-[var(--border)] rounded">
      No Access
    </span>
  );
}
