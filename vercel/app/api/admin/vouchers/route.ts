import { NextResponse } from "next/server";
import { createServiceClient } from "@/lib/supabase/server";

export async function GET() {
  const supabase = await createServiceClient();

  const { data: vouchers, error } = await supabase
    .from("vouchers")
    .select("*")
    .order("created_at", { ascending: false });

  if (error) {
    return NextResponse.json({ error: error.message }, { status: 500 });
  }

  return NextResponse.json({ vouchers });
}

export async function POST(request: Request) {
  try {
    const body = await request.json();
    const { code, description, max_redemptions } = body;

    if (!code) {
      return NextResponse.json({ error: "Code is required" }, { status: 400 });
    }

    const supabase = await createServiceClient();

    // Check if code already exists
    const { data: existing } = await supabase
      .from("vouchers")
      .select("id")
      .eq("code", code.toUpperCase())
      .maybeSingle();

    if (existing) {
      return NextResponse.json({ error: "Voucher code already exists" }, { status: 400 });
    }

    const { data: voucher, error } = await supabase
      .from("vouchers")
      .insert({
        code: code.toUpperCase(),
        description: description || null,
        max_redemptions: max_redemptions || 1,
      })
      .select()
      .single();

    if (error) {
      return NextResponse.json({ error: error.message }, { status: 500 });
    }

    return NextResponse.json({ voucher }, { status: 201 });
  } catch {
    return NextResponse.json({ error: "Invalid request" }, { status: 400 });
  }
}
