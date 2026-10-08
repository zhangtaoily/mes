# Feed retry for task PT20261007000002 using real task materials
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

$TASKNO = 'PT20261007000002'
$BATCH = '26091341'
$WSID = 1
$NOW = (Get-Date).ToString('yyyy-MM-ddTHH:mm:ss')

# task materials
$r = HttpJson 'GET' "/api/site/feed-record/materials?taskNo=$TASKNO" $null $T
$mats = @($r.json.data)
Write-Host ("materials count=" + $mats.Count)
foreach ($m in $mats) {
    Write-Host ("  id=" + $m.id + " rawBatchNo=" + $m.rawBatchNo + " qty=" + $m.pickQty + " fedQty=" + $m.fedQty + " raw=" + $m.raw)
}
if ($mats.Count -eq 0) { Write-Host 'NO-MATERIALS'; exit 1 }

$m1 = $mats[0]
$feedBody = '{"feed":{"taskNo":"' + $TASKNO + '","workshopId":' + $WSID + ',"batchNo":"' + $BATCH + '","rawDetail":"E2E-raw","feedQty":500,"isFinish":1,"feedBy":"admin","feedTime":"' + $NOW + '"},"items":[{"rawBatchNo":"' + $m1.rawBatchNo + '","materialNo":"' + $m1.materialNo + '","materialName":"' + $m1.materialName + '","materialSpec":"' + $m1.materialSpec + '","pickQty":500,"thisFeedQty":500,"fedQty":500}]}'
$r = HttpJson 'POST' '/api/site/feed-record' $feedBody $T
Write-Host ("feed code=" + $r.json.code + " msg=" + $r.json.msg + " data=" + $r.json.data)
if ($r.json.code -ne 200) { Write-Host ("raw=" + $r.raw) }
