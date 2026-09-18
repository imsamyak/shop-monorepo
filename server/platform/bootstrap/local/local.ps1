<#
.SYNOPSIS
  Starts, stops or resets the local resources (Postgres, RabbitMQ, DynamoDB Local, MongoDB) that a service needs under the "local" profile.
.EXAMPLE
  ./local.ps1 up       # start everything and wait until every port answers
  ./local.ps1 down     # stop, keep the data
  ./local.ps1 reset    # stop and delete the data
  ./local.ps1 status
#>
param(
  [Parameter(Position = 0)]
  [ValidateSet('up', 'down', 'reset', 'status')]
  [string]$Action = 'up'
)

$ErrorActionPreference = 'Stop'
$composeFile = Join-Path $PSScriptRoot 'docker-compose.yml'

# name -> port: the contract with infra/src/main/resources/ecom-infra-local.yml
$endpoints = [ordered]@{ postgres = 5432; rabbitmq = 5672; dynamodb = 8000; mongo = 27017; s3 = 9090 }

# Windows PowerShell 5.1 turns anything a native command writes to stderr (Docker's progress output included)
# into an error, so errors are only taken from the exit code.
function Invoke-Docker {
  $previous = $ErrorActionPreference
  $ErrorActionPreference = 'Continue'
  try { & docker @args } finally { $ErrorActionPreference = $previous }
  if ($LASTEXITCODE -ne 0) { throw "docker $($args -join ' ') failed (exit code $LASTEXITCODE)" }
}

function Assert-DockerRunning {
  try { Invoke-Docker info *> $null } catch { throw 'Docker is not running. Start Docker Desktop and try again.' }
}

function Wait-ForPort([string]$name, [int]$port, [int]$timeoutSeconds = 120) {
  $deadline = (Get-Date).AddSeconds($timeoutSeconds)
  while ((Get-Date) -lt $deadline) {
    $client = New-Object System.Net.Sockets.TcpClient
    try {
      $client.Connect('127.0.0.1', $port)
      if ($client.Connected) { Write-Host "  $name is answering on localhost:$port"; return }
    } catch {
      Start-Sleep -Seconds 1
    } finally {
      $client.Close()
    }
  }
  throw "$name did not answer on localhost:$port within $timeoutSeconds seconds. See: docker compose -f '$composeFile' logs $name"
}

Assert-DockerRunning

switch ($Action) {
  'up' {
    Invoke-Docker compose -f $composeFile up -d --wait
    foreach ($entry in $endpoints.GetEnumerator()) { Wait-ForPort $entry.Key $entry.Value }
    Write-Host 'Local resources are ready. Run a service with --spring.profiles.active=local'
  }
  'down' {
    Invoke-Docker compose -f $composeFile down
  }
  'reset' {
    Invoke-Docker compose -f $composeFile down --volumes
    Write-Host 'Stopped and deleted all local data.'
  }
  'status' {
    Invoke-Docker compose -f $composeFile ps
  }
}
