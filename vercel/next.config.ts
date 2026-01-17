import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Allow custom build directory via env var (for pre-push hook)
  distDir: process.env.NEXT_BUILD_DIR || ".next",
};

export default nextConfig;
