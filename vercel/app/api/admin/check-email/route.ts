import { NextResponse } from "next/server";
import { isAdmin } from "@/lib/admin";

export async function POST(request: Request) {
  try {
    const { email } = await request.json();

    if (!email) {
      return NextResponse.json({ error: "Email required" }, { status: 400 });
    }

    return NextResponse.json({ isAdmin: isAdmin(email) });
  } catch {
    return NextResponse.json({ error: "Invalid request" }, { status: 400 });
  }
}
