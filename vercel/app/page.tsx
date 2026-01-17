import Link from "next/link";

export default function Home() {
  return (
    <main className="min-h-screen flex flex-col">
      {/* Hero Section */}
      <div className="flex-1 flex flex-col items-center justify-center px-4 sm:px-6 py-16 sm:py-24">
        <div className="w-full max-w-xl mx-auto text-center">
          <h1 className="text-4xl sm:text-5xl md:text-6xl font-semibold tracking-tight mb-4">
            Ramble
          </h1>
          
          <p className="text-lg sm:text-xl text-[var(--muted)] mb-8 leading-relaxed">
            Speak naturally. Watch your words appear in real-time.
          </p>
          
          <div className="flex flex-col sm:flex-row gap-3 justify-center">
            <Link
              href="/signup"
              className="px-6 py-3 bg-[var(--accent)] hover:bg-[var(--accent-hover)] text-white font-medium rounded-lg transition-colors"
            >
              Get Started
            </Link>
            <Link
              href="/login"
              className="px-6 py-3 bg-[var(--surface)] hover:bg-[var(--surface-hover)] text-[var(--foreground)] font-medium rounded-lg border border-[var(--border)] transition-colors"
            >
              Sign In
            </Link>
          </div>
        </div>
        
        {/* Features */}
        <div className="mt-16 sm:mt-20 grid grid-cols-1 sm:grid-cols-3 gap-4 sm:gap-6 w-full max-w-2xl mx-auto px-4 sm:px-0">
          <div className="p-4 sm:p-5 rounded-lg bg-[var(--surface)] border border-[var(--border)]">
            <h3 className="text-sm font-medium mb-1">Real-time</h3>
            <p className="text-sm text-[var(--muted)]">
              Words appear as you speak.
            </p>
          </div>
          
          <div className="p-4 sm:p-5 rounded-lg bg-[var(--surface)] border border-[var(--border)]">
            <h3 className="text-sm font-medium mb-1">Accurate</h3>
            <p className="text-sm text-[var(--muted)]">
              Powered by Soniox.
            </p>
          </div>
          
          <div className="p-4 sm:p-5 rounded-lg bg-[var(--surface)] border border-[var(--border)]">
            <h3 className="text-sm font-medium mb-1">Everywhere</h3>
            <p className="text-sm text-[var(--muted)]">
              Web and Android.
            </p>
          </div>
        </div>
      </div>
      
      {/* Footer */}
      <footer className="border-t border-[var(--border)] py-4 px-4 sm:px-6">
        <div className="max-w-xl mx-auto flex justify-between items-center text-xs text-[var(--muted)]">
          <span>Ramble</span>
          <Link href="/admin/login" className="hover:text-[var(--foreground)] transition-colors">
            Admin
          </Link>
        </div>
      </footer>
    </main>
  );
}
