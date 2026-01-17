import { NextResponse } from "next/server";
import { createClient, createServiceClient } from "@/lib/supabase/server";

export async function POST(request: Request) {
  try {
    const { code } = await request.json();

    if (!code) {
      return NextResponse.json({ error: "Code is required" }, { status: 400 });
    }

    // Get current user
    const supabase = await createClient();
    const { data: { user } } = await supabase.auth.getUser();

    if (!user) {
      return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
    }

    // Use service client for admin operations
    const serviceClient = await createServiceClient();

    // Find the voucher
    const { data: voucher, error: voucherError } = await serviceClient
      .from("vouchers")
      .select("*")
      .eq("code", code.toUpperCase())
      .eq("is_active", true)
      .maybeSingle();

    if (voucherError) {
      console.error("Voucher lookup error:", voucherError);
      return NextResponse.json({ error: "Error looking up voucher: " + voucherError.message }, { status: 500 });
    }

    if (!voucher) {
      // Try to find the voucher without is_active filter to give better error message
      const { data: inactiveVoucher } = await serviceClient
        .from("vouchers")
        .select("*")
        .eq("code", code.toUpperCase())
        .maybeSingle();
      
      if (inactiveVoucher && !inactiveVoucher.is_active) {
        return NextResponse.json({ error: "This voucher has been deactivated" }, { status: 400 });
      }
      
      return NextResponse.json({ error: "Invalid voucher code" }, { status: 400 });
    }

    // Check if max redemptions reached
    if (voucher.current_redemptions >= voucher.max_redemptions) {
      return NextResponse.json({ error: "This voucher has reached its maximum redemptions" }, { status: 400 });
    }

    // Check if user already redeemed this voucher
    const { data: existingRedemption } = await serviceClient
      .from("voucher_redemptions")
      .select("id")
      .eq("user_id", user.id)
      .eq("voucher_id", voucher.id)
      .maybeSingle();

    if (existingRedemption) {
      return NextResponse.json({ error: "You have already redeemed this voucher" }, { status: 400 });
    }

    // Ensure user has a profile (might not exist if they signed up via magic link)
    const { data: profile } = await serviceClient
      .from("profiles")
      .select("id")
      .eq("id", user.id)
      .maybeSingle();

    if (!profile) {
      // Create profile if it doesn't exist
      const { error: profileError } = await serviceClient
        .from("profiles")
        .insert({
          id: user.id,
          email: user.email!,
        });
      
      if (profileError) {
        console.error("Failed to create profile:", profileError);
        return NextResponse.json({ error: "Failed to create user profile" }, { status: 500 });
      }
    }

    // Create redemption
    const { error: redemptionError } = await serviceClient
      .from("voucher_redemptions")
      .insert({
        user_id: user.id,
        voucher_id: voucher.id,
      });

    if (redemptionError) {
      console.error("Failed to redeem voucher:", redemptionError);
      return NextResponse.json({ error: "Failed to redeem voucher: " + redemptionError.message }, { status: 500 });
    }

    // Update voucher redemption count
    await serviceClient
      .from("vouchers")
      .update({ current_redemptions: voucher.current_redemptions + 1 })
      .eq("id", voucher.id);

    return NextResponse.json({ 
      success: true,
      message: "Voucher redeemed successfully! You now have access to Ramble."
    });
  } catch {
    return NextResponse.json({ error: "Invalid request" }, { status: 400 });
  }
}
