# Verify pack execute list + palletize
$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [Text.Encoding]::UTF8
$BASE = 'http://127.0.0.1:8080'

function HttpJson($method, $path, $body, $token) {
    $req = [System.Net.HttpWebRequest]::Create("$BASE$path")
    $req.Method = $method
    $req.ContentType = 'application/json; charset=utf-8'
    $req.Timeout = 30000
    if ($token) { $req.Headers.Add('Authorization', "Bearer $token") }
    if ($body) {
        $bytes = [Text.Encoding]::UTF8.GetBytes($body)
        $req.ContentLength = $bytes.Length
        $s = $req.GetRequestStream(); $s.Write($bytes, 0, $bytes.Length); $s.Close()
    }
    try { $resp = $req.GetResponse() } catch [System.Net.WebException] { $resp = $_.Exception.Response }
    $ms = New-Object IO.MemoryStream
    $resp.GetResponseStream().CopyTo($ms)
    $raw = [Text.Encoding]::UTF8.GetString($ms.ToArray())
    $json = $null
    try { $json = $raw | ConvertFrom-Json } catch {}
    return @{ http = [int]$resp.StatusCode; json = $json; raw = $raw }
}

$r = HttpJson 'POST' '/api/auth/login' '{"loginName":"admin","password":"admin@2026"}' $null
$T = $r.json.data.token

# 1. execute list verify
$r = HttpJson 'GET' '/api/pack/executes?pageNum=1&pageSize=3' $null $T
$rows = @($r.json.data.rows)
Write-Host ("executes total=" + $r.json.data.total)
foreach ($e in $rows) {
    Write-Host ("  pc " + $e.executeNo + " boxes=" + $e.boxCount + " sumNet=" + $e.sumNetWeight + " sumPublic=" + $e.sumPublicWeight + " packed=" + $e.isPacked)
}

# 2. palletize (list pallets first)
$r = HttpJson 'GET' '/api/pack/pallets?pageNum=1&pageSize=3' $null $T
Write-Host ("pallets total=" + $r.json.data.total)
