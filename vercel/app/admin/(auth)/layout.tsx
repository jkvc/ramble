// Simple pass-through layout for admin auth pages (no auth check)
export default function AdminAuthLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return <>{children}</>;
}
