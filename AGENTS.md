# Project Rules for niagara-launcher

- **No Local Gradle Builds**: Do NOT run `./gradlew assembleDebug` or build APKs locally on this sandbox to save disk space. Always clean `app/build` if created.
- **Local Commit First, Push Upon Confirmation**: After implementing each feature or fixing bugs in a multi-step batch, run `git add -A && git commit -m "<message>"` locally, and do **NOT** automatically run `git push`. Once **all** planned tasks in the current batch are completed and verified, explicitly ask the user whether to push to GitHub (`git push origin main`), and only execute the push after receiving confirmation so GitHub Actions builds and publishes `app-debug.apk`.
