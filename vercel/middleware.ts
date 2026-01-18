import { type NextRequest, NextResponse } from "next/server";
import { updateSession } from "@/lib/supabase/middleware";
import { isAdmin } from "@/lib/admin";

export async function middleware(request: NextRequest) {
    const { pathname } = request.nextUrl;

    // Update Supabase session
    const { user, supabaseResponse } = await updateSession(request);

    // Public routes - no protection needed
    const publicRoutes = ["/", "/login", "/signup", "/auth/callback"];
    if (publicRoutes.includes(pathname)) {
        return supabaseResponse;
    }

    // Protect /dashboard/* routes
    if (pathname.startsWith("/dashboard")) {
        if (!user) {
            const url = request.nextUrl.clone();
            url.pathname = "/login";
            url.searchParams.set("redirect", pathname);
            return NextResponse.redirect(url);
        }
        return supabaseResponse;
    }

    // Protect /admin/* routes - admins use regular login
    if (pathname.startsWith("/admin")) {
        if (!user) {
            const url = request.nextUrl.clone();
            url.pathname = "/login";
            url.searchParams.set("redirect", pathname);
            return NextResponse.redirect(url);
        }

        // Check if user is admin
        if (!isAdmin(user.email)) {
            const url = request.nextUrl.clone();
            url.pathname = "/dashboard";
            return NextResponse.redirect(url);
        }

        return supabaseResponse;
    }

    // API routes that handle their own auth (support both cookie and Bearer token)
    // Skip middleware protection - let the route handler do it
    if (pathname.startsWith("/api/soniox") || pathname.startsWith("/api/vouchers/redeem") || pathname.startsWith("/api/auth/login") || pathname.startsWith("/api/auth/refresh")) {
        return supabaseResponse;
    }

    // Protect admin API routes
    if (pathname.startsWith("/api/admin")) {
        if (!user || !isAdmin(user.email)) {
            return NextResponse.json({ error: "Forbidden" }, { status: 403 });
        }
    }

    return supabaseResponse;
}

export const config = {
    matcher: [
        /*
         * Match all request paths except for the ones starting with:
         * - _next/static (static files)
         * - _next/image (image optimization files)
         * - favicon.ico (favicon file)
         * Feel free to modify this pattern to include more paths.
         */
        "/((?!_next/static|_next/image|favicon.ico|.*\\.(?:svg|png|jpg|jpeg|gif|webp)$).*)",
    ],
};
