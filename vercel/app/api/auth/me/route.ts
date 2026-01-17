import { NextResponse } from "next/server";
import { createClient } from "@/lib/supabase/server";
import { checkAccess } from "@/lib/access";
import { isAdmin } from "@/lib/admin";

export async function GET() {
  const supabase = await createClient();
  const { data: { user } } = await supabase.auth.getUser();

  if (!user) {
    return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  }

  const accessStatus = await checkAccess(user.id, user.email, supabase);

  return NextResponse.json({
    user: {
      id: user.id,
      email: user.email,
    },
    isAdmin: isAdmin(user.email),
    access: accessStatus,
  });
}
