# Output report for schedule 17 (task PT20261007000003)
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
Write-Host ("login code=" + $r.json.code)

$TODAY = (Get-Date).ToString('yyyy-MM-dd')
$body = '{"scheduleId":17,"batchNo":"26091341","processId":1,"equipmentId":1,"outputQty":200,"unit":"kg","reportUser":"admin","reportType":"normal","reportDate":"' + $TODAY + '"}'
$r = HttpJson 'POST' '/api/site/output' $body $T
Write-Host ("output save code=" + $r.json.code + " msg=" + $r.json.msg + " data=" + $r.json.data)
if ($r.json.code -ne 200) { Write-Host ("raw=" + $r.raw) }

# verify
$r = HttpJson 'GET' '/api/site/output?pageNum=1&pageSize=5' $null $T
$rows = @($r.json.data.rows)
Write-Host ("output total=" + $r.json.data.total)
foreach ($o in $rows) {
    Write-Host ("  or " + $o.outputNo + " sched=" + $o.scheduleId + " qty=" + $o.outputQty + " " + $o.unit + " date=" + $o.reportDate)
}
