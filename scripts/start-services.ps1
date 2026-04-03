$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot

$services = @(
  @{ Name = 'auth-service'; Module = 'services/auth-service' },
  @{ Name = 'account-service'; Module = 'services/account-service' },
  @{ Name = 'fraud-detection-service'; Module = 'services/fraud-detection-service' },
  @{ Name = 'transaction-service'; Module = 'services/transaction-service' },
  @{ Name = 'notification-service'; Module = 'services/notification-service' },
  @{ Name = 'audit-service'; Module = 'services/audit-service' }
)

foreach ($service in $services) {
  $title = "mini-banking :: $($service.Name)"
  $command = "Set-Location '$repoRoot'; mvn -q -pl $($service.Module) spring-boot:run"

  Start-Process powershell -ArgumentList @(
    '-NoExit',
    '-Command',
    "`$Host.UI.RawUI.WindowTitle = '$title'; $command"
  ) -WorkingDirectory $repoRoot | Out-Null
}

Write-Host 'Started service windows for:'
foreach ($service in $services) {
  Write-Host "- $($service.Name)"
}
Write-Host 'Wait until each service finishes booting before running seed or smoke scripts.'
