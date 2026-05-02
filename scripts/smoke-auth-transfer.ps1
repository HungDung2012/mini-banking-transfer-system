$ErrorActionPreference = 'Stop'

$gatewayBaseUrl = $env:GATEWAY_BASE_URL
if (-not $gatewayBaseUrl) { $gatewayBaseUrl = 'http://localhost:8000' }

function Invoke-JsonRequest {
  param(
    [Parameter(Mandatory = $true)] [string] $Method,
    [Parameter(Mandatory = $true)] [string] $Uri,
    [hashtable] $Headers = @{},
    [object] $Body = $null,
    [bool] $ReturnRaw = $false
  )

  $invokeParams = @{
    Method = $Method
    Uri = $Uri
    Headers = $Headers
  }

  if ($null -ne $Body) {
    $invokeParams.ContentType = 'application/json'
    $invokeParams.Body = ($Body | ConvertTo-Json -Depth 5 -Compress)
  }

  if ($ReturnRaw) {
    return Invoke-WebRequest @invokeParams
  }

  return Invoke-RestMethod @invokeParams
}

$login = Invoke-JsonRequest -Method Post -Uri "$gatewayBaseUrl/api/auth/login" -Body @{
  username = 'alice'
  password = 'secret123'
}

$token = $login.token
$headers = @{ Authorization = "Bearer $token"; 'Idempotency-Key' = 'smoke-duplicate-1' }

$transferBody = @{
  sourceAccount = '100001'
  destinationAccount = '200001'
  amount = 500
}

$first = Invoke-JsonRequest -Method Post -Uri "$gatewayBaseUrl/api/transfers" -Headers $headers -Body $transferBody
$second = Invoke-JsonRequest -Method Post -Uri "$gatewayBaseUrl/api/transfers" -Headers $headers -Body $transferBody

if ($first.transferId -ne $second.transferId) {
  throw 'Idempotency failed'
}

Write-Host "Transfer ID: $($first.transferId)"
Write-Host "Status: $($first.status)"
Write-Host 'Smoke verification passed.'
