# Claude Rules

## Git Operations

**NEVER automatically commit or push code.** Only commit and push when the user explicitly requests it in a separate message. This applies even after completing a task or fixing an error.

## Build Validation

**Don't run `pnpm build` after every small change.** Rely on the pre-push Husky hook to catch build errors. Only run build manually when:
- Making complex or large-scale changes
- The user explicitly asks to verify the build
- Debugging a build failure

**When running build manually, use the separate output directory** to avoid interfering with the running dev server:
```bash
cd vercel
NEXT_BUILD_DIR=.next-prepush pnpm build
rm -rf .next-prepush
```
