$ErrorActionPreference = 'Stop'

$login = Invoke-RestMethod -Method Post -Uri http://localhost:8000/api/auth/login `
  -ContentType 'application/json' `
  -Body '{"username":"alice","password":"secret123"}'

$token = $login.token

Invoke-WebRequest -Method Post -Uri http://localhost:8000/api/transfers `
  -Headers @{ Authorization = "Bearer $token"; 'Idempotency-Key' = 'smoke-1' } `
  -ContentType 'application/json' `
  -Body '{"sourceAccount":"100001","destinationAccount":"200001","amount":500}'