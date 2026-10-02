<#
.SYNOPSIS
  Runs one service and restarts it whenever its code, a shared module or infra changes (like nodemon for Spring Boot).
.EXAMPLE
  ./dev-service.ps1 identity
.NOTES
  A change under module/* or infra is installed first (mvn install of that module), because services use the installed jar.
  Logs stay in this window; Ctrl-C stops the service and the watcher.
#>
param(
  [Parameter(Mandatory = $true, Position = 0)]
  [ValidateSet('identity', 'listing', 'inventory', 'checkout', 'order', 'catalog', 'asset')]
  [string]$Name
)

$root = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path

# The AWS keys and region and the RabbitMQ settings live in the repo-root .env (git-ignored). Only AWS_ and RABBITMQ_ values
# are passed on to the service, and they win over anything already set on the machine, so a stray AWS_REGION cannot point
# the service at the wrong region.
$envFile = Join-Path $root '..\..\.env'
if (Test-Path $envFile) {
  foreach ($line in Get-Content $envFile) {
    if ($line -match '^\s*((?:AWS|RABBITMQ)_[A-Z_]+)\s*=\s*"?([^"\r\n]*)"?\s*$') { Set-Item -Path "Env:$($Matches[1])" -Value $Matches[2].Trim() }
  }
}
$mvn = (Get-Command mvn.cmd, mvn -ErrorAction SilentlyContinue | Select-Object -First 1).Source
$install = New-Object System.Threading.Mutex($false, 'shop-platform-mvn-install')

# shared with the watchers' event handlers, which run in their own scope
$state = [hashtable]::Synchronized(@{ changed = $null; stamp = Get-Date })

# source files only: java, yml, xml, properties, sql under a src folder
$onChange = {
  $path = $Event.SourceEventArgs.FullPath
  if ($path -match '\\src\\.*\.(java|yml|yaml|xml|properties|sql)$') {
    $Event.MessageData.changed = $path
    $Event.MessageData.stamp = Get-Date
  }
}

$watchers = foreach ($folder in "service\$Name\src", 'module', 'infra') {
  $w = New-Object System.IO.FileSystemWatcher (Join-Path $root $folder)
  $w.IncludeSubdirectories = $true
  $w.EnableRaisingEvents = $true
  $null = Register-ObjectEvent $w Changed -Action $onChange -MessageData $state
  $null = Register-ObjectEvent $w Created -Action $onChange -MessageData $state
  $w
}

function Stop-Tree($process) {
  if ($process -and -not $process.HasExited) { & taskkill /T /F /PID $process.Id | Out-Null }
}

function Start-Service-Process {
  Write-Host "[$Name] starting" -ForegroundColor Green
  Start-Process -FilePath $mvn -ArgumentList '-o', '-q', '-pl', "service/$Name", 'spring-boot:run' -WorkingDirectory $root -NoNewWindow -PassThru
}

$process = Start-Service-Process
try {
  while ($true) {
    Start-Sleep -Milliseconds 500
    $file = $state.changed
    if ($file -and ((Get-Date) - $state.stamp).TotalMilliseconds -gt 1500) {
      $state.changed = $null
      Write-Host "[$Name] changed: $file - restarting" -ForegroundColor Yellow
      Stop-Tree $process
      $relative = $file.Substring($root.Length + 1)
      if ($relative -match '^(module\\[^\\]+|infra)\\') {
        $module = $Matches[1] -replace '\\', '/'
        $null = $install.WaitOne()
        try {
          Write-Host "[$Name] installing $module" -ForegroundColor Yellow
          & $mvn -o -q -DskipTests -pl $module install
        } finally { $install.ReleaseMutex() }
      }
      $process = Start-Service-Process
    } elseif ($process.HasExited -and -not $file) {
      Write-Host "[$Name] stopped (exit $($process.ExitCode)); waiting for a change to start it again" -ForegroundColor Red
      while (-not $state.changed) { Start-Sleep -Milliseconds 500 }
    }
  }
} finally {
  Stop-Tree $process
  foreach ($w in $watchers) { $w.Dispose() }
}
