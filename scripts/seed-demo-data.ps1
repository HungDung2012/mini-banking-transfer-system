$ErrorActionPreference = 'Stop'

$authBaseUrl = $env:AUTH_BASE_URL
if (-not $authBaseUrl) { $authBaseUrl = 'http://localhost:8000' }

$accountBaseUrl = $env:ACCOUNT_BASE_URL
if (-not $accountBaseUrl) { $accountBaseUrl = 'http://localhost:8082' }

function Invoke-SeedRequest {
  param(
    [Parameter(Mandatory = $true)] [string] $Method,
    [Parameter(Mandatory = $true)] [string] $Uri,
    [Parameter(Mandatory = $true)] [string] $Body
  )

  try {
    Invoke-RestMethod -Method $Method -Uri $Uri -ContentType 'application/json' -Body $Body | Out-Null
  } catch {
    $status = $_.Exception.Response.StatusCode.Value__
    if ($status -ne 409) {
      throw
    }
  }
}

Invoke-SeedRequest -Method Post -Uri "$authBaseUrl/api/auth/register" -Body '{"username":"alice","password":"secret123"}'
Invoke-SeedRequest -Method Post -Uri "$accountBaseUrl/accounts" -Body '{"accountNumber":"100001","ownerName":"Alice","balance":1000000}'
Invoke-SeedRequest -Method Post -Uri "$accountBaseUrl/accounts" -Body '{"accountNumber":"200001","ownerName":"Bob","balance":500000}'

Write-Host 'Demo data seeded.'