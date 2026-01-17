/**
 * Check if an email is in the admin whitelist
 */
export function isAdmin(email: string | undefined | null): boolean {
  if (!email) return false;
  
  const adminEmails = process.env.ADMIN_EMAILS?.split(",").map((e) =>
    e.trim().toLowerCase()
  ) ?? [];
  
  return adminEmails.includes(email.toLowerCase());
}
