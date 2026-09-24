<#
.SYNOPSIS
  Runs the checkout and asset services, the chatbot and Cartly together in this window, each restarting on change.
  (Every other service has its own window: server/platform/bootstrap/local/dev-service.ps1 <name>.)
#>
$ErrorActionPreference = 'Stop'
$script = Join-Path $PSScriptRoot 'server\platform\bootstrap\local\dev-service.ps1'
$services = foreach ($name in 'checkout', 'asset') {
  Start-Process -FilePath powershell -ArgumentList '-NoProfile', '-File', $script, $name -NoNewWindow -PassThru
}
try {
  pnpm --dir $PSScriptRoot dev:node
} finally {
  foreach ($service in $services) { if (-not $service.HasExited) { & taskkill /T /F /PID $service.Id | Out-Null } }
}
