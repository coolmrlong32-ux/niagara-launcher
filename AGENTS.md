# Project Rules for niagara-launcher

- **No Local Gradle Builds**: Do NOT run `./gradlew assembleDebug` or build APKs locally on this sandbox to save disk space. Always clean `app/build` if created.
- **Auto-Deploy via GitHub**: After implementing features or fixing bugs, always run `git add -A && git commit -m "<message>" && git push origin main` so GitHub Actions automatically builds and publishes `app-debug.apk` to GitHub Releases.
