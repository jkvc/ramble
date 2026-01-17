import { TranscriptionTool } from "@/components/TranscriptionTool";

export default function AdminTranscribePage() {
  return (
    <div>
      <div className="mb-8">
        <h1 className="text-2xl font-bold mb-2">Transcription Test</h1>
        <p className="text-[var(--muted)]">
          Test the Soniox integration. Press record and start speaking.
        </p>
      </div>

      <TranscriptionTool hasAccess={true} />
    </div>
  );
}
