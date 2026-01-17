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
    <div>
      <div className="mb-8">
        <h1 className="text-2xl font-bold mb-2">Transcription</h1>
        <p className="text-[var(--muted)]">
          Press the record button and start speaking. Your words will appear in real-time.
        </p>
      </div>

      <TranscriptionTool hasAccess={access.hasAccess} />
    </div>
  );
}
