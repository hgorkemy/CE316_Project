# Windows Installer

## Prerequisites

- JDK 21 with `jpackage` available on `PATH`
- Maven available on `PATH`
- Inno Setup 6 installed in its default location

## Build

From the `IAE_CE316` directory, run:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\packaging\build-installer.ps1
```

The generated installer is written to:

```text
dist\IAE_Setup.exe
```

Generated installer files are intentionally ignored by Git. Commit the files
under `packaging\`, not the contents of `dist\` or `target\`.
