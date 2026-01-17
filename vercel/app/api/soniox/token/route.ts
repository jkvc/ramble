import { NextResponse } from "next/server";
import { createClient, createServiceClient } from "@/lib/supabase/server";
import { createClient as createSupabaseClient } from "@supabase/supabase-js";
import { checkAccess } from "@/lib/access";
import { isAdmin } from "@/lib/admin";

export async function POST(request: Request) {
  // Check for Bearer token (mobile app) or cookie session (web)
  const authHeader = request.headers.get("authorization");
  
  let user;
  let supabaseForAccess; // Client to use for access checks
  
  if (authHeader?.startsWith("Bearer ")) {
    // Mobile app with access token
    const accessToken = authHeader.substring(7);
    
    // Create a Supabase client to verify the token
    const supabase = createSupabaseClient(
      process.env.NEXT_PUBLIC_SUPABASE_URL!,
      process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY!
    );
    
    const { data: userData, error } = await supabase.auth.getUser(accessToken);
    if (error || !userData.user) {
      return NextResponse.json({ error: "Invalid token" }, { status: 401 });
    }
    user = userData.user;
    
    // Use service client for access checks (bypasses RLS)
    supabaseForAccess = await createServiceClient();
  } else {
    // Web with cookie session
    const supabase = await createClient();
    const { data: { user: sessionUser } } = await supabase.auth.getUser();
    user = sessionUser;
    supabaseForAccess = supabase;
  }

  if (!user) {
    return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  }

  // Check access (admin, subscription, or voucher)
  const access = await checkAccess(user.id, user.email, supabaseForAccess);

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
