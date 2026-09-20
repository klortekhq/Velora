[CmdletBinding()]
param(
    [Parameter(Mandatory)][string]$Adb,
    [ValidateSet('Phone','Tablet','Tv')][string]$Profile = 'Phone',
    [string]$Serial = 'emulator-5554'
)

$ErrorActionPreference = 'Stop'
if ($Serial -notmatch '^emulator-\d+$') { throw 'Este runner solo modifica tamaños de un emulador, no de dispositivos personales.' }
$qaRoot = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$qaOutput = Join-Path $qaRoot '.qa'
New-Item -ItemType Directory -Force -Path $qaOutput | Out-Null
$qaVariant = if ($Profile -eq 'Tv') { 'tv' } else { 'mobile' }
$qaPackage = "com.klortek.velora.$qaVariant"

function Invoke-QaAdb([string[]]$Arguments) {
    $result = & $Adb -s $Serial @Arguments 2>&1
    if ($LASTEXITCODE -ne 0) { throw "ADB falló: $($result -join '\n')" }
    return $result
}

if (([string](Invoke-QaAdb @('shell','getprop','sys.boot_completed'))).Trim() -ne '1') { throw 'El emulador todavía no ha arrancado.' }
try {
    switch ($Profile) {
        'Phone' {
            Invoke-QaAdb @('shell','wm','size','1080x2400')
            Invoke-QaAdb @('shell','wm','density','420')
        }
        'Tablet' {
            Invoke-QaAdb @('shell','wm','size','960x1536')
            Invoke-QaAdb @('shell','wm','density','160')
        }
        'Tv' {
            Invoke-QaAdb @('shell','wm','size','1280x720')
            Invoke-QaAdb @('shell','wm','density','160')
        }
    }
    Invoke-QaAdb @('install','-r',(Join-Path $qaRoot "app/build/outputs/apk/$qaVariant/debug/app-$qaVariant-debug.apk"))
    Invoke-QaAdb @('install','-r',(Join-Path $qaRoot "app/build/outputs/apk/androidTest/$qaVariant/debug/app-$qaVariant-debug-androidTest.apk"))
    $qaResult = Invoke-QaAdb @('shell','am','instrument','-r','-w',"$qaPackage.test/androidx.test.runner.AndroidJUnitRunner")
    $qaResult | Tee-Object -FilePath (Join-Path $qaOutput "live-tv-$Profile.txt")
    $qaText = $qaResult -join "`n"
    # am instrument can exit 0 even when the target process crashes.
    if ($qaText -match 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed' -or
        $qaText -notmatch 'INSTRUMENTATION_CODE: -1' -or $qaText -notmatch 'OK \(\d+ tests\)') {
        throw 'La instrumentación no terminó correctamente. Consultar el informe .qa.'
    }
    $qaMinimumPasses = if ($Profile -eq 'Tv') { 2 } else { 5 }
    if ([regex]::Matches($qaText, '(?m)^INSTRUMENTATION_STATUS_CODE: 0\s*$').Count -lt $qaMinimumPasses) {
        throw 'Faltan pruebas ejecutadas: los casos omitidos no cuentan como aprobados.'
    }
    $qaScreenshot = if ($Profile -eq 'Tv') { 'remote' } else { 'long' }
    Invoke-QaAdb @('pull',"/sdcard/Android/data/$qaPackage/files/picker-$qaScreenshot-$qaVariant.png",(Join-Path $qaOutput "picker-$Profile.png"))
} finally {
    Invoke-QaAdb @('shell','wm','size','reset')
    Invoke-QaAdb @('shell','wm','density','reset')
}
