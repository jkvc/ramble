import { NextResponse } from "next/server";
import { createClient } from "@/lib/supabase/server";
import { checkAccess } from "@/lib/access";
import { isAdmin } from "@/lib/admin";

export async function POST() {
  const supabase = await createClient();
  const { data: { user } } = await supabase.auth.getUser();

  if (!user) {
    return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  }

  // Check access (admin, subscription, or voucher)
  const access = await checkAccess(user.id, user.email, supabase);

  if (!access.hasAccess && !isAdmin(user.email)) {
    return NextResponse.json({ 
      error: "Access denied. Please subscribe or redeem a voucher.",
      access 
    }, { status: 403 });
  }

  // Get Soniox API key from environment
  const sonioxApiKey = process.env.SONIOX_API_KEY;

  if (!sonioxApiKey) {
    console.error("SONIOX_API_KEY not configured");
    return NextResponse.json({ error: "Service not configured" }, { status: 500 });
  }

  // For now, we'll return the API key directly
  // In production, you'd want to create a temporary token via Soniox's API
  // See: https://soniox.com/docs/stt/demo-apps/soniox-live
  
  // TODO: Implement temporary token creation via Soniox API
  // For MVP, we'll use the API key directly (less secure but functional)
  
  return NextResponse.json({
    token: sonioxApiKey,
    expiresAt: Date.now() + 3600 * 1000, // 1 hour from now
    websocketUrl: "wss://stt-rt.soniox.com/transcribe-websocket",
  });
}
