# NOUS GitHub CI/CD & Build Guide

NOUS is fully configured with automated **GitHub Actions CI/CD** so that every push to your GitHub repository automatically compiles, tests, and packages a ready-to-install Android APK.

---

## 1. Automated GitHub Actions Workflow (`.github/workflows/build-apk.yml`)

When you push this repository to GitHub:
1. GitHub Actions automatically checks out the repository.
2. Sets up JDK 17 with Gradle caching.
3. Automatically decodes or initializes the keystore credentials.
4. Executes `./gradlew assembleDebug`.
5. **Uploads the generated APK as an Artifact** (`NOUS-debug-apk`).
6. **Creates a GitHub Release** containing `app-debug.apk` ready for direct phone download.

---

## 2. How to Download the APK from GitHub

### Method A: From GitHub Actions (Artifacts)
1. Go to your repository on GitHub (`https://github.com/<your-username>/<repo-name>`).
2. Click on the **Actions** tab at the top.
3. Click on the latest workflow run named **"Build NOUS Android APK"**.
4. Scroll down to the **Artifacts** section at the bottom of the page.
5. Click on **`NOUS-debug-apk`** to download the ZIP file containing `app-debug.apk`.
6. Transfer or unzip and open the `.apk` on your Android phone to install.

### Method B: From GitHub Releases
1. Go to your repository main page.
2. In the right-hand sidebar, click on **Releases**.
3. Under the latest release (e.g., `v1.0.1`), click on `app-debug.apk` to download it directly onto your device.

---

## 3. How to Push this Project to Your GitHub

If you haven't pushed the project to GitHub yet:

```bash
# 1. Initialize git (if not already done)
git init

# 2. Add all files
git add .

# 3. Commit
git commit -m "feat: NOUS autonomous agent with voice, vision, and CI/CD"

# 4. Add your GitHub remote repository
git remote add origin https://github.com/<your-username>/<your-repo-name>.git

# 5. Push to main branch
git branch -M main
git push -u origin main
```

Once pushed, GitHub Actions will immediately trigger and build your APK!
