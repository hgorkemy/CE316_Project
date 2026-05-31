$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$targetDir = Join-Path $projectRoot "target"
$inputDir = Join-Path $targetDir "package-input"
$imageDir = Join-Path $targetDir "installer-image"
$distDir = Join-Path $projectRoot "dist"
$appJar = Join-Path $targetDir "IAE_CE316-1.0-SNAPSHOT.jar"
$iscc = "${env:ProgramFiles(x86)}\Inno Setup 6\ISCC.exe"

Set-Location $projectRoot

Write-Host "Building application JAR and runtime dependencies..."
& mvn clean package -DskipTests
if ($LASTEXITCODE -ne 0) { throw "Maven build failed." }

foreach ($path in @($inputDir, $imageDir, $distDir)) {
    if (Test-Path $path) {
        Remove-Item -LiteralPath $path -Recurse -Force
    }
}

New-Item -ItemType Directory -Path $inputDir | Out-Null
Copy-Item -LiteralPath $appJar -Destination $inputDir
Copy-Item -Path (Join-Path $targetDir "lib\*.jar") -Destination $inputDir

Write-Host "Creating Windows application image..."
& jpackage `
    --type app-image `
    --name IAE `
    --app-version 1.0.0 `
    --input $inputDir `
    --main-jar (Split-Path -Leaf $appJar) `
    --main-class com.iae.Launcher `
    --dest $imageDir
if ($LASTEXITCODE -ne 0) { throw "jpackage failed." }

if (-not (Test-Path $iscc)) {
    throw "Inno Setup compiler not found at '$iscc'. Install Inno Setup 6 first."
}

Write-Host "Creating installer..."
& $iscc (Join-Path $PSScriptRoot "IAE_Setup.iss")
if ($LASTEXITCODE -ne 0) { throw "Inno Setup compilation failed." }

Write-Host ""
Write-Host "Installer created: $(Join-Path $distDir 'IAE_Setup.exe')"
