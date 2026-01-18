import { TranscriptionTool } from "@/components/TranscriptionTool";

export default function AdminTranscribePage() {
  return (
    <div className="max-w-xl">
      <div className="mb-4">
        <h1 className="text-lg font-semibold">Transcription Test</h1>
      </div>

      <TranscriptionTool hasAccess={true} />
    </div>
  );
}
