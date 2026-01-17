"use client";

import { useState, useEffect } from "react";

interface Voucher {
  id: string;
  code: string;
  description: string | null;
  max_redemptions: number;
  current_redemptions: number;
  is_active: boolean;
  created_at: string;
}

export default function AdminVouchersPage() {
  const [vouchers, setVouchers] = useState<Voucher[]>([]);
  const [loading, setLoading] = useState(true);
  const [creating, setCreating] = useState(false);
  const [showForm, setShowForm] = useState(false);
  const [newVoucher, setNewVoucher] = useState({
    code: "",
    description: "",
    max_redemptions: 1,
  });
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchVouchers();
  }, []);

  async function fetchVouchers() {
    try {
      const response = await fetch("/api/admin/vouchers");
      const data = await response.json();
      setVouchers(data.vouchers || []);
    } catch {
      setError("Failed to load vouchers");
    } finally {
      setLoading(false);
    }
  }

  async function createVoucher(e: React.FormEvent) {
    e.preventDefault();
    setCreating(true);
    setError(null);

    try {
      const response = await fetch("/api/admin/vouchers", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(newVoucher),
      });

      if (!response.ok) {
        const data = await response.json();
        throw new Error(data.error || "Failed to create voucher");
      }

      setNewVoucher({ code: "", description: "", max_redemptions: 1 });
      setShowForm(false);
      fetchVouchers();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to create voucher");
    } finally {
      setCreating(false);
    }
  }

  async function toggleVoucher(id: string, isActive: boolean) {
    try {
      await fetch(`/api/admin/vouchers/${id}`, {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ is_active: !isActive }),
      });
      fetchVouchers();
    } catch {
      setError("Failed to update voucher");
    }
  }

  function generateCode() {
    const chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    let code = "RAMBLE-";
    for (let i = 0; i < 8; i++) {
      code += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    setNewVoucher({ ...newVoucher, code });
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center py-12">
        <div className="text-[var(--muted)]">Loading...</div>
      </div>
    );
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-8">
        <h1 className="text-2xl font-bold">Vouchers</h1>
        <button
          onClick={() => setShowForm(!showForm)}
          className="px-4 py-2 bg-[var(--accent)] hover:bg-[var(--accent-hover)] text-white font-medium rounded-lg transition-colors"
        >
          {showForm ? "Cancel" : "Create Voucher"}
        </button>
      </div>

      {error && (
        <div className="mb-6 p-4 bg-red-500/10 border border-red-500/20 rounded-lg text-red-400">
          {error}
        </div>
      )}

      {/* Create Form */}
      {showForm && (
        <div className="mb-8 p-6 bg-[var(--surface)] border border-[var(--border)] rounded-xl">
          <h2 className="text-lg font-semibold mb-4">New Voucher</h2>
          <form onSubmit={createVoucher} className="space-y-4">
            <div>
              <label className="block text-sm font-medium mb-2">Code</label>
              <div className="flex gap-2">
                <input
                  type="text"
                  value={newVoucher.code}
                  onChange={(e) => setNewVoucher({ ...newVoucher, code: e.target.value.toUpperCase() })}
                  required
                  className="flex-1 px-4 py-2 bg-[var(--background)] border border-[var(--border)] rounded-lg focus:outline-none focus:ring-2 focus:ring-[var(--accent)]"
                  placeholder="RAMBLE-XXXXXXXX"
                />
                <button
                  type="button"
                  onClick={generateCode}
                  className="px-4 py-2 bg-[var(--surface-hover)] hover:bg-[var(--border)] rounded-lg transition-colors"
                >
                  Generate
                </button>
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium mb-2">Description (optional)</label>
              <input
                type="text"
                value={newVoucher.description}
                onChange={(e) => setNewVoucher({ ...newVoucher, description: e.target.value })}
                className="w-full px-4 py-2 bg-[var(--background)] border border-[var(--border)] rounded-lg focus:outline-none focus:ring-2 focus:ring-[var(--accent)]"
                placeholder="e.g., Beta tester, Friend & family"
              />
            </div>

            <div>
              <label className="block text-sm font-medium mb-2">Max Redemptions</label>
              <input
                type="number"
                min="1"
                value={newVoucher.max_redemptions}
                onChange={(e) => setNewVoucher({ ...newVoucher, max_redemptions: parseInt(e.target.value) || 1 })}
                className="w-32 px-4 py-2 bg-[var(--background)] border border-[var(--border)] rounded-lg focus:outline-none focus:ring-2 focus:ring-[var(--accent)]"
              />
            </div>

            <button
              type="submit"
              disabled={creating}
              className="px-6 py-2 bg-[var(--accent)] hover:bg-[var(--accent-hover)] disabled:opacity-50 text-white font-medium rounded-lg transition-colors"
            >
              {creating ? "Creating..." : "Create Voucher"}
            </button>
          </form>
        </div>
      )}

      {/* Vouchers Table */}
      <div className="bg-[var(--surface)] border border-[var(--border)] rounded-xl overflow-hidden">
        <table className="w-full">
          <thead className="border-b border-[var(--border)]">
            <tr className="text-left text-sm text-[var(--muted)]">
              <th className="px-6 py-4 font-medium">Code</th>
              <th className="px-6 py-4 font-medium">Description</th>
              <th className="px-6 py-4 font-medium">Redemptions</th>
              <th className="px-6 py-4 font-medium">Status</th>
              <th className="px-6 py-4 font-medium">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-[var(--border)]">
            {vouchers.map((voucher) => (
              <tr key={voucher.id} className="hover:bg-[var(--surface-hover)]">
                <td className="px-6 py-4">
                  <code className="px-2 py-1 bg-[var(--background)] rounded text-sm">
                    {voucher.code}
                  </code>
                </td>
                <td className="px-6 py-4 text-[var(--muted)]">
                  {voucher.description || "—"}
                </td>
                <td className="px-6 py-4">
                  {voucher.current_redemptions} / {voucher.max_redemptions}
                </td>
                <td className="px-6 py-4">
                  {voucher.is_active ? (
                    <span className="px-2 py-1 text-xs font-medium bg-green-500/10 text-green-400 border border-green-500/20 rounded">
                      Active
                    </span>
                  ) : (
                    <span className="px-2 py-1 text-xs font-medium bg-[var(--surface-hover)] text-[var(--muted)] border border-[var(--border)] rounded">
                      Inactive
                    </span>
                  )}
                </td>
                <td className="px-6 py-4">
                  <button
                    onClick={() => toggleVoucher(voucher.id, voucher.is_active)}
                    className="text-sm text-[var(--accent)] hover:underline"
                  >
                    {voucher.is_active ? "Deactivate" : "Activate"}
                  </button>
                </td>
              </tr>
            ))}
            {vouchers.length === 0 && (
              <tr>
                <td colSpan={5} className="px-6 py-12 text-center text-[var(--muted)]">
                  No vouchers yet. Create one to get started.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
