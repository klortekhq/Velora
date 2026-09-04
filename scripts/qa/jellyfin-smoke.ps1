[CmdletBinding()]
param(
    [string]$ServerUrl = $env:VELORA_JELLYFIN_URL,
    [string]$Username = $env:VELORA_JELLYFIN_USER,
    [string]$Password = $env:VELORA_JELLYFIN_PASSWORD
)

$ErrorActionPreference = 'Stop'

if ([string]::IsNullOrWhiteSpace($ServerUrl) -or
    [string]::IsNullOrWhiteSpace($Username) -or
    [string]::IsNullOrWhiteSpace($Password)) {
    throw 'Define VELORA_JELLYFIN_URL, VELORA_JELLYFIN_USER y VELORA_JELLYFIN_PASSWORD.'
}

$base = $ServerUrl.TrimEnd('/')
$clientHeader = 'MediaBrowser Client="Velora QA", Device="QA", DeviceId="velora-qa", Version="1.4.0"'

try {
    $public = Invoke-RestMethod -Uri "$base/System/Info/Public" -Headers @{
        'X-Emby-Authorization' = $clientHeader
    } -TimeoutSec 15

    $authBody = @{ Username = $Username; Pw = $Password } | ConvertTo-Json
    $auth = Invoke-RestMethod -Method Post -Uri "$base/Users/AuthenticateByName" `
        -Headers @{ 'X-Emby-Authorization' = $clientHeader } `
        -ContentType 'application/json' -Body $authBody -TimeoutSec 15

    $userId = $auth.User.Id
    $token = $auth.AccessToken
    $authHeader = $clientHeader + ', Token="' + $token + '"'
    $channels = Invoke-RestMethod -Uri "$base/LiveTv/Channels?UserId=$userId&Fields=Overview%2CMediaSources&EnableUserData=true&Limit=1000" `
        -Headers @{ 'X-Emby-Authorization' = $authHeader } -TimeoutSec 30

    $items = @($channels.Items)
    $multiSourceRows = @($items | Where-Object { $_.MediaSources -and $_.MediaSources.Count -gt 1 }).Count
    $duplicateIds = @($items | Group-Object Id | Where-Object { $_.Count -gt 1 }).Count

    if ($items.Count -gt 0) {
        $first = $items[0]
        $sourceId = $first.MediaSources | Select-Object -First 1 | Select-Object -ExpandProperty Id
        $sourceQuery = if ([string]::IsNullOrWhiteSpace($sourceId)) { '' } else { '&MediaSourceId=' + [uri]::EscapeDataString($sourceId) }
        $playbackInfo = Invoke-RestMethod -Uri "$base/Items/$([uri]::EscapeDataString($first.Id))/PlaybackInfo?UserId=$userId&StartTimeTicks=0&IsPlayback=true&AutoOpenLiveStream=true$sourceQuery" `
            -Headers @{ 'X-Emby-Authorization' = $authHeader } -TimeoutSec 30
        if (-not @($playbackInfo.MediaSources).Count) {
            throw 'PlaybackInfo no devolvió ninguna fuente.'
        }
        Write-Output 'PlaybackInfo Live TV: OK'
    } else {
        Write-Output 'PlaybackInfo Live TV: omitido (sin canales)'
    }

    Write-Output "Jellyfin $($public.Version) OK"
    Write-Output "Live TV: $($items.Count) canales; $multiSourceRows filas con varias fuentes; $duplicateIds IDs duplicadas"
}
catch {
    $status = $_.Exception.Response.StatusCode.value__
    if ($status) {
        throw "Smoke test Jellyfin fallido (HTTP $status)."
    }
    throw "Smoke test Jellyfin fallido ($($_.Exception.GetType().Name))."
}
