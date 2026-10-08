# Report center loop verification: catalog + every implemented report query
$ErrorActionPreference = 'Continue'
$BASE = 'http://localhost:8080'
$TODAY = '2026-10-07'

function HttpJson([string]$method, [string]$url, [string]$body, [string]$token) {
  $headers = @{ 'Content-Type' = 'application/json' }
  if ($token) { $headers['Authorization'] = 'Bearer ' + $token }
  try {
    if ($method -eq 'GET') {
      $r = Invoke-WebRequest -Uri $url -Headers $headers -Method GET -UseBasicParsing -TimeoutSec 30
    } else {
      $r = Invoke-WebRequest -Uri $url -Headers $headers -Method $method -Body $body -UseBasicParsing -TimeoutSec 60
    }
    return @{ code = [int]$r.StatusCode; json = ($r.Content | ConvertFrom-Json) }
  } catch {
    $resp = $_.Exception.Response
    if ($resp) {
      try {
        $sr = New-Object IO.StreamReader($resp.GetResponseStream())
        $txt = $sr.ReadToEnd()
        return @{ code = [int]$resp.StatusCode; json = ($txt | ConvertFrom-Json); err = $txt }
      } catch { return @{ code = 0; err = $_.Exception.Message } }
    }
    return @{ code = 0; err = $_.Exception.Message }
  }
}

# 1. login
$login = HttpJson 'POST' "$BASE/api/auth/login" '{"loginName":"admin","password":"admin@2026"}' $null
$token = $login.json.data.token
Write-Output ("login code=" + $login.code)

# 2. catalog
$cat = HttpJson 'GET' "$BASE/api/reports/catalog" $null $token
Write-Output ("catalog code=" + $cat.code + " categories=" + $cat.json.data.Count)
$impCodes = @()
foreach ($c in $cat.json.data) {
  $names = ($c.reports | ForEach-Object { $_.code + '(' + $_.implemented + ')' }) -join ','
  Write-Output ("  [" + $c.category + "] " + $names)
  foreach ($r in $c.reports) { if ($r.implemented) { $impCodes += $r.code } }
}
Write-Output ("implemented reports: " + $impCodes.Count)

# 3. query each implemented report (month range + today)
$ep = @{
  'qc-daily'            = '/api/reports/qc-daily'
  'defect-stats'        = '/api/reports/defect-stats'
  'tube-weight-output'  = '/api/reports/tube-weight-output'
  'pack-inbound'        = '/api/reports/pack-inbound'
  'progress-tracking'   = '/api/reports/progress-tracking'
  'spinning-daily'      = '/api/reports/spinning-daily'
  'wool-top-daily'      = '/api/reports/wool-top-daily'
  'fore-spin-stats'     = '/api/reports/fore-spin-stats'
  'wool-rate'           = '/api/reports/wool-rate'
  'yarn-physical-stats' = '/api/reports/yarn-physical-stats'
  'cv-stats'            = '/api/reports/cv-stats'
  'raw-qc-summary'      = '/api/reports/raw-qc-summary'
}
foreach ($code in $impCodes) {
  $url = $BASE + $ep[$code] + "?dateBegin=2026-10-01&dateEnd=$TODAY"
  $q = HttpJson 'GET' $url $null $token
  $rows = @()
  if ($q.json -and $q.json.data) { $rows = @($q.json.data) }
  Write-Output ("RPT " + $code + " code=" + $q.code + " rows=" + $rows.Count)
  if ($rows.Count -gt 0) {
    $first = $rows[0] | ConvertTo-Json -Compress -Depth 3
    if ($first.Length -gt 220) { $first = $first.Substring(0, 220) + '...' }
    Write-Output ("    row1: " + $first)
  }
}
Write-Output 'DONE'
