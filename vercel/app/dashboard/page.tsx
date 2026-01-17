import { createClient } from "@/lib/supabase/server";
import { checkAccess } from "@/lib/access";
import { TranscriptionTool } from "@/components/TranscriptionTool";

export default async function DashboardPage() {
  const supabase = await createClient();
  const { data: { user } } = await supabase.auth.getUser();

  if (!user) {
    return null;
  }

  const access = await checkAccess(user.id, user.email, supabase);

  return (
    <div className="space-y-6">
      <TranscriptionTool hasAccess={access.hasAccess} />
    </div>
  );
}
