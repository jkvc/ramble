import Link from "next/link";

export default function Home() {
  return (
    <main className="min-h-screen flex flex-col">
      {/* Hero Section */}
      <div className="flex-1 flex flex-col items-center justify-center px-6 py-24">
        <div className="max-w-3xl mx-auto text-center">
          <h1 className="text-5xl md:text-7xl font-bold tracking-tight mb-6">
            <span className="bg-gradient-to-r from-blue-400 via-blue-500 to-purple-600 bg-clip-text text-transparent">
              Ramble
            </span>
          </h1>
          
          <p className="text-xl md:text-2xl text-[var(--muted)] mb-8 leading-relaxed">
            Speak naturally. Watch your words appear in real-time.
            <br />
            Voice-to-text transcription that keeps up with your thoughts.
          </p>
          
          <div className="flex flex-col sm:flex-row gap-4 justify-center">
            <Link
              href="/signup"
              className="px-8 py-4 bg-[var(--accent)] hover:bg-[var(--accent-hover)] text-white font-medium rounded-lg transition-colors text-lg"
            >
              Get Started
            </Link>
            <Link
              href="/login"
              className="px-8 py-4 bg-[var(--surface)] hover:bg-[var(--surface-hover)] text-white font-medium rounded-lg border border-[var(--border)] transition-colors text-lg"
            >
              Sign In
            </Link>
          </div>
        </div>
        
        {/* Features */}
        <div className="mt-24 grid grid-cols-1 md:grid-cols-3 gap-8 max-w-5xl mx-auto">
          <div className="p-6 rounded-xl bg-[var(--surface)] border border-[var(--border)]">
            <div className="text-3xl mb-4">⚡</div>
            <h3 className="text-lg font-semibold mb-2">Real-time</h3>
            <p className="text-[var(--muted)]">
              See words appear as you speak. No waiting, no lag.
            </p>
          </div>
          
          <div className="p-6 rounded-xl bg-[var(--surface)] border border-[var(--border)]">
            <div className="text-3xl mb-4">🎯</div>
            <h3 className="text-lg font-semibold mb-2">Accurate</h3>
            <p className="text-[var(--muted)]">
              Powered by Soniox, the most accurate speech recognition engine.
            </p>
          </div>
          
          <div className="p-6 rounded-xl bg-[var(--surface)] border border-[var(--border)]">
            <div className="text-3xl mb-4">📱</div>
            <h3 className="text-lg font-semibold mb-2">Everywhere</h3>
            <p className="text-[var(--muted)]">
              Web app now, Android keyboard coming soon.
            </p>
          </div>
        </div>
      </div>
      
      {/* Footer */}
      <footer className="border-t border-[var(--border)] py-6 px-6">
        <div className="max-w-5xl mx-auto flex justify-between items-center text-sm text-[var(--muted)]">
          <span>© 2024 Ramble</span>
          <Link href="/admin/login" className="hover:text-white transition-colors">
            Admin
          </Link>
        </div>
      </footer>
    </main>
  );
}
