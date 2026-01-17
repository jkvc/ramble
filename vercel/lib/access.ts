import { SupabaseClient } from "@supabase/supabase-js";
import { isAdmin } from "./admin";

export interface AccessStatus {
  hasAccess: boolean;
  reason: "admin" | "subscription" | "voucher" | "none";
  subscription?: {
    status: string;
    expiresAt: string | null;
  };
  voucher?: {
    code: string;
    redeemedAt: string;
  };
}

/**
 * Check if a user has access to transcription features
 */
export async function checkAccess(
  userId: string,
  userEmail: string | undefined,
  supabase: SupabaseClient
): Promise<AccessStatus> {
  // Check if admin
  if (isAdmin(userEmail)) {
    return { hasAccess: true, reason: "admin" };
  }

  // Check for active subscription
  const { data: subscription } = await supabase
    .from("subscriptions")
    .select("status, expires_at")
    .eq("user_id", userId)
    .eq("status", "active")
    .maybeSingle();

  if (subscription) {
    const isExpired = subscription.expires_at && 
      new Date(subscription.expires_at) < new Date();
    
    if (!isExpired) {
      return {
        hasAccess: true,
        reason: "subscription",
        subscription: {
          status: subscription.status,
          expiresAt: subscription.expires_at,
        },
      };
    }
  }

  // Check for voucher redemption
  const { data: redemption } = await supabase
    .from("voucher_redemptions")
    .select(`
      redeemed_at,
      vouchers (code)
    `)
    .eq("user_id", userId)
    .limit(1)
    .maybeSingle();

  if (redemption) {
    // Supabase returns the joined voucher as an object (single) or array (multiple)
    const vouchers = redemption.vouchers as unknown;
    let voucherCode = "unknown";
    if (vouchers && typeof vouchers === "object") {
      if (Array.isArray(vouchers) && vouchers.length > 0) {
        voucherCode = (vouchers[0] as { code: string }).code;
      } else if ("code" in vouchers) {
        voucherCode = (vouchers as { code: string }).code;
      }
    }
    return {
      hasAccess: true,
      reason: "voucher",
      voucher: {
        code: voucherCode,
        redeemedAt: redemption.redeemed_at,
      },
    };
  }

  return { hasAccess: false, reason: "none" };
}
