[Console]::OutputEncoding = New-Object System.Text.UTF8Encoding($false)
$ErrorActionPreference = "Stop"
$BaseUrl  = "http://localhost:8084"
$Password = "Test1234"
$Currency = "EUR"

function Banner($text, [ConsoleColor]$color = [ConsoleColor]::Cyan) { Write-Host "`n=== $text ===" -ForegroundColor $color }
function Show-ServerError([System.Exception]$ex){ if ($ex.Response){ try{$sr=New-Object IO.StreamReader($ex.Response.GetResponseStream());$b=$sr.ReadToEnd(); if($b){Write-Host "`n--- Server response ---`n$b" -ForegroundColor Yellow}}catch{} } }
function Invoke-Json{
  param([Parameter(Mandatory)][string]$Method,[Parameter(Mandatory)][string]$Url,[hashtable]$Body,[hashtable]$Headers)
  $jsonBody=$null;$ct=$null
  if($PSBoundParameters.ContainsKey('Body') -and $Body){ $jsonBody=$Body|ConvertTo-Json -Depth 10; $ct="application/json" }
  Invoke-RestMethod -Method $Method -Uri $Url -Headers $Headers -ContentType $ct -Body $jsonBody -ErrorAction Stop
}

try{
  Banner "Pre-check server ($BaseUrl)"
  $tc=Test-NetConnection -ComputerName "localhost" -Port ([uri]$BaseUrl).Port
  if(-not $tc.TcpTestSucceeded){ throw "Server not listening on port $($tc.RemotePort). Start your app first." }
  Write-Host "OK: server reachable." -ForegroundColor Green

  Banner "1) Register new client"
  $email="client.$((Get-Random -Minimum 1000 -Maximum 999999))@example.com"
  $reg=Invoke-Json -Method POST -Url "$BaseUrl/api/auth/register" -Body @{ prenom="Test"; nom="User"; email=$email; motDePasse=$Password }
  $cid=$reg.data.id
  Write-Host "Registered clientId=$cid  email=$email" -ForegroundColor Green

  Banner "2) Login (get JWT)"
  $login=Invoke-Json -Method POST -Url "$BaseUrl/api/auth/login" -Body @{ email=$email; motDePasse=$Password }
  $token=$login.data.token; $type=$login.data.tokenType
  if(-not $token){ throw "No token in response. Check /api/auth/login." }
  $auth=@{ Authorization="$type $token" }
  Write-Host "JWT OK (type=$type)" -ForegroundColor Green

  Banner "3) Create first account (CHECKING)"
  $acc1=Invoke-Json -Method POST -Url "$BaseUrl/api/clients/$cid/accounts" -Headers $auth -Body @{ type="CHECKING"; initialBalance=0; currency=$Currency }
  $accNo1=$acc1.data.numCompte
  if(-not $accNo1){ throw "No numCompte in create-account response." }
  Write-Host "Account1: $accNo1  status=$($acc1.data.status)" -ForegroundColor Green

  Banner "4) Activate account1"
  Invoke-Json -Method PUT -Url "$BaseUrl/api/accounts/$accNo1/activate" -Headers $auth | Out-Null
  Write-Host "Account1 activated." -ForegroundColor Green

  Banner "5) Deposit 500 on account1"
  Invoke-Json -Method POST -Url "$BaseUrl/api/accounts/$accNo1/deposit" -Headers $auth -Body @{ montant=500 } | Out-Null
  Write-Host "Deposit OK." -ForegroundColor Green

  Banner "6) Create second account (SAVINGS) + activate"
  $acc2=Invoke-Json -Method POST -Url "$BaseUrl/api/clients/$cid/accounts" -Headers $auth -Body @{ type="SAVINGS"; initialBalance=0; currency=$Currency }
  $accNo2=$acc2.data.numCompte
  Invoke-Json -Method PUT -Url "$BaseUrl/api/accounts/$accNo2/activate" -Headers $auth | Out-Null
  Write-Host "Account2: $accNo2 activated." -ForegroundColor Green

  Banner "7) Transfer 200 (account1 → account2)"
  Invoke-Json -Method POST -Url "$BaseUrl/api/accounts/transfer" -Headers $auth -Body @{
    numeroCompteSource       = $accNo1
    numeroCompteDestinataire = $accNo2
    montant                  = 200
    communication            = "E2E detailed transfer"
  } | Out-Null
  Write-Host "Transfer OK." -ForegroundColor Green

  Banner "8) List accounts for the client"
  $all=Invoke-Json -Method GET -Url "$BaseUrl/api/clients/$cid/accounts" -Headers $auth
  $all|ConvertTo-Json -Depth 10

  Write-Host "`n✅ Detailed E2E test completed successfully." -ForegroundColor Green
}catch{
  Write-Host "`n❌ Error: $($_.Exception.Message)" -ForegroundColor Red
  Show-ServerError $_.Exception
  exit 1
}
