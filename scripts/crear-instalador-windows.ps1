$ErrorActionPreference = "Stop"

$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
[xml]$pom = Get-Content -LiteralPath (Join-Path $projectRoot "pom.xml") -Raw
$appVersion = $pom.project.version
if ($appVersion -match "SNAPSHOT|[A-Za-z]") {
    throw "Para generar un instalador, usa una versión estable en pom.xml, por ejemplo 1.0.0."
}

$maven = Get-Command mvn.cmd -ErrorAction SilentlyContinue
if ($null -eq $maven) {
    $maven = "C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.1\plugins\maven\lib\maven3\bin\mvn.cmd"
    if (-not (Test-Path -LiteralPath $maven)) {
        throw "No se encontró Maven. Configura Maven en PATH o actualiza la ruta del Maven de IntelliJ en este script."
    }
    $maven = $maven
} else {
    $maven = $maven.Source
}

$jpackage = $null
if ($env:JAVA_HOME) {
    $jpackage = Join-Path $env:JAVA_HOME "bin\jpackage.exe"
}
if (-not $jpackage -or -not (Test-Path -LiteralPath $jpackage)) {
    $jpackage = (Get-Command jpackage.exe -ErrorAction SilentlyContinue).Source
}
if (-not $jpackage -or -not (Test-Path -LiteralPath $jpackage)) {
    throw "No se encontró jpackage. Configura JAVA_HOME con un JDK 25."
}

$inputDir = Join-Path $projectRoot "target\installer-input"
$outputDir = Join-Path $projectRoot "dist"
$icon = Join-Path $projectRoot "src\main\resources\org\drvo\reggisapp\ui\reggis.ico"
if (-not (Test-Path -LiteralPath $icon)) {
    throw "No se encontró el icono de Reggis: $icon"
}
New-Item -ItemType Directory -Force -Path $inputDir, $outputDir | Out-Null
Get-ChildItem -LiteralPath $inputDir -File | Remove-Item -Force

Push-Location $projectRoot
try {
    & $maven "-DskipTests" "clean" "package"
    if ($LASTEXITCODE -ne 0) { throw "La compilación Maven falló." }

    & $maven "org.apache.maven.plugins:maven-dependency-plugin:3.8.1:copy-dependencies" `
        "-DincludeScope=runtime" "-DoutputDirectory=$inputDir"
    if ($LASTEXITCODE -ne 0) { throw "No se pudieron copiar las dependencias de ejecución." }

    $mainJar = "ReggisApp-$appVersion.jar"
    $mainJarPath = Join-Path $inputDir $mainJar
    Copy-Item -LiteralPath (Join-Path $projectRoot "target\$mainJar") -Destination $mainJarPath
    $dependencyJars = Get-ChildItem -LiteralPath $inputDir -Filter "*.jar" -File |
        Where-Object Name -ne $mainJar |
        ForEach-Object Name
    $manifestLines = [System.Collections.Generic.List[string]]::new()
    $manifestLine = "Class-Path: "
    foreach ($dependencyJar in $dependencyJars) {
        $entry = if ($manifestLine.EndsWith(" ")) { $dependencyJar } else { " $dependencyJar" }
        if (($manifestLine.Length + $entry.Length) -gt 70) {
            $manifestLines.Add($manifestLine)
            $manifestLine = "  $dependencyJar"
        } else {
            $manifestLine += $entry
        }
    }
    if ($manifestLine -ne "Class-Path: ") { $manifestLines.Add($manifestLine) }
    $manifestPath = Join-Path $projectRoot "target\installer-manifest.mf"
    [System.IO.File]::WriteAllText($manifestPath, ($manifestLines -join "`r`n") + "`r`n", [System.Text.Encoding]::ASCII)
    $jarTool = Join-Path $env:JAVA_HOME "bin\jar.exe"
    if (-not (Test-Path -LiteralPath $jarTool)) { throw "No se encontró jar.exe en JAVA_HOME." }
    & $jarTool "ufm" $mainJarPath $manifestPath
    if ($LASTEXITCODE -ne 0) { throw "No se pudo añadir el class-path de dependencias al JAR." }

    $jpackageArgs = @(
        "--type", "exe",
        "--input", $inputDir,
        "--dest", $outputDir,
        "--name", "Reggis",
        "--app-version", $appVersion,
        "--vendor", "DRVO",
        "--description", "Registro local de clientes, pedidos y pagos",
        "--icon", $icon,
        "--main-jar", $mainJar,
        "--main-class", "org.drvo.reggisapp.ReggisApp",
        "--java-options", "--module-path=$APPDIR",
        "--java-options", "--add-modules=javafx.controls",
        "--win-per-user-install",
        "--win-menu",
        "--win-shortcut",
        "--win-upgrade-uuid", "7f97a44e-8e70-4f7b-9919-82f5f8d21752"
    )
    & $jpackage @jpackageArgs
    if ($LASTEXITCODE -ne 0) { throw "jpackage no pudo generar el instalador. Revisa que WiX Toolset esté instalado." }

    Write-Host "Instalador generado en: $(Join-Path $outputDir "Reggis-$appVersion.exe")"
} finally {
    Pop-Location
}
