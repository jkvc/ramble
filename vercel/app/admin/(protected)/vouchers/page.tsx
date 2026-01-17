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
      <div className="flex items-center justify-center py-8">
        <div className="text-sm text-[var(--muted)]">Loading...</div>
      </div>
    );
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-lg font-semibold">Vouchers</h1>
        <button
          onClick={() => setShowForm(!showForm)}
          className="px-3 py-1.5 bg-[var(--accent)] hover:bg-[var(--accent-hover)] text-white text-sm font-medium rounded-lg transition-colors"
        >
          {showForm ? "Cancel" : "Create"}
        </button>
      </div>

      {error && (
        <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg text-red-600 text-sm">
          {error}
        </div>
      )}

      {/* Create Form */}
      {showForm && (
        <div className="mb-6 p-4 bg-[var(--surface)] border border-[var(--border)] rounded-lg">
          <h2 className="text-sm font-medium mb-3">New Voucher</h2>
          <form onSubmit={createVoucher} className="space-y-3">
            <div>
              <label className="block text-xs font-medium mb-1">Code</label>
              <div className="flex gap-2">
                <input
                  type="text"
                  value={newVoucher.code}
                  onChange={(e) => setNewVoucher({ ...newVoucher, code: e.target.value.toUpperCase() })}
                  required
                  className="flex-1 px-3 py-2 bg-[var(--background)] border border-[var(--border)] rounded-lg focus:outline-none focus:ring-2 focus:ring-[var(--accent)] text-sm"
                  placeholder="RAMBLE-XXXXXXXX"
                />
                <button
                  type="button"
                  onClick={generateCode}
                  className="px-3 py-2 bg-[var(--surface-hover)] hover:bg-[var(--border)] rounded-lg transition-colors text-sm"
                >
                  Generate
                </button>
              </div>
            </div>

            <div>
              <label className="block text-xs font-medium mb-1">Description (optional)</label>
              <input
                type="text"
                value={newVoucher.description}
                onChange={(e) => setNewVoucher({ ...newVoucher, description: e.target.value })}
                className="w-full px-3 py-2 bg-[var(--background)] border border-[var(--border)] rounded-lg focus:outline-none focus:ring-2 focus:ring-[var(--accent)] text-sm"
                placeholder="e.g., Beta tester"
              />
            </div>

            <div>
              <label className="block text-xs font-medium mb-1">Max Redemptions</label>
              <input
                type="number"
                min="1"
                value={newVoucher.max_redemptions}
                onChange={(e) => setNewVoucher({ ...newVoucher, max_redemptions: parseInt(e.target.value) || 1 })}
                className="w-24 px-3 py-2 bg-[var(--background)] border border-[var(--border)] rounded-lg focus:outline-none focus:ring-2 focus:ring-[var(--accent)] text-sm"
              />
            </div>

            <button
              type="submit"
              disabled={creating}
              className="px-4 py-2 bg-[var(--accent)] hover:bg-[var(--accent-hover)] disabled:opacity-50 text-white text-sm font-medium rounded-lg transition-colors"
            >
              {creating ? "Creating..." : "Create"}
            </button>
          </form>
        </div>
      )}

      {/* Vouchers Table */}
      <div className="bg-[var(--surface)] border border-[var(--border)] rounded-lg overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead className="border-b border-[var(--border)]">
              <tr className="text-left text-[var(--muted)]">
                <th className="px-4 py-3 font-medium">Code</th>
                <th className="px-4 py-3 font-medium hidden sm:table-cell">Description</th>
                <th className="px-4 py-3 font-medium">Used</th>
                <th className="px-4 py-3 font-medium">Status</th>
                <th className="px-4 py-3 font-medium">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[var(--border)]">
              {vouchers.map((voucher) => (
                <tr key={voucher.id} className="hover:bg-[var(--surface-hover)]">
                  <td className="px-4 py-3">
                    <code className="px-1.5 py-0.5 bg-[var(--background)] rounded text-xs">
                      {voucher.code}
                    </code>
                  </td>
                  <td className="px-4 py-3 text-[var(--muted)] hidden sm:table-cell">
                    {voucher.description || "—"}
                  </td>
                  <td className="px-4 py-3">
                    {voucher.current_redemptions}/{voucher.max_redemptions}
                  </td>
                  <td className="px-4 py-3">
                    {voucher.is_active ? (
                      <span className="px-2 py-0.5 text-xs font-medium bg-green-50 text-green-700 border border-green-200 rounded">
                        Active
                      </span>
                    ) : (
                      <span className="px-2 py-0.5 text-xs font-medium bg-[var(--surface-hover)] text-[var(--muted)] border border-[var(--border)] rounded">
                        Inactive
                      </span>
                    )}
                  </td>
                  <td className="px-4 py-3">
                    <button
                      onClick={() => toggleVoucher(voucher.id, voucher.is_active)}
                      className="text-xs text-[var(--accent)] hover:underline"
                    >
                      {voucher.is_active ? "Deactivate" : "Activate"}
                    </button>
                  </td>
                </tr>
              ))}
              {vouchers.length === 0 && (
                <tr>
                  <td colSpan={5} className="px-4 py-8 text-center text-[var(--muted)]">
                    No vouchers yet
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
