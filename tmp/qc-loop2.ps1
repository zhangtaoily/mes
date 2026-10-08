# QC chain: register samples -> create report (scheme 1) -> calc -> submit -> statistics
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

# 1. register samples 3,4
$r = HttpJson 'POST' '/api/qc/samples/register' '{"ids":[3,4]}' $T
Write-Host ("register code=" + $r.json.code + " msg=" + $r.json.msg)

# 2. build report save body: scheme 1, indicators 1-5 x samples 1-5
$v1 = @('23.5','24.0','24.5','25.0','27.0')   # item weight (last one over upper 26)
$v2 = @('0.8','0.9','1.0','0.7','0.9')        # oil
$v3 = @('12.5','13.0','12.8','13.2','12.9')   # moisture
$v4 = @('95','96','94','97','95')             # length
$v5 = @('18.0','17.5','19.0','18.5','17.8')   # CV
$items = New-Object System.Collections.ArrayList
for ($s = 1; $s -le 5; $s++) {
    [void]$items.Add('{"indicatorId":1,"sampleNo":' + $s + ',"valueStr":"' + $v1[$s-1] + '"}')
    [void]$items.Add('{"indicatorId":2,"sampleNo":' + $s + ',"valueStr":"' + $v2[$s-1] + '"}')
    [void]$items.Add('{"indicatorId":3,"sampleNo":' + $s + ',"valueStr":"' + $v3[$s-1] + '"}')
    [void]$items.Add('{"indicatorId":4,"sampleNo":' + $s + ',"valueStr":"' + $v4[$s-1] + '"}')
    [void]$items.Add('{"indicatorId":5,"sampleNo":' + $s + ',"valueStr":"' + $v5[$s-1] + '"}')
}
$TODAY = (Get-Date).ToString('yyyy-MM-dd')
$body = '{"workshopId":1,"qcType":"\u5de5\u5e8f\u68c0","labType":0,"batchNo":"26091341","inspector":"admin","inspectDate":"' + $TODAY + '","sampleQty":5,"schemeId":1,"items":[' + ($items -join ',') + ']}'

$r = HttpJson 'POST' '/api/qc/reports' $body $T
$repId = $r.json.data
Write-Host ("report save code=" + $r.json.code + " msg=" + $r.json.msg + " id=" + $repId)
if ($r.json.code -ne 200) { Write-Host ("raw=" + $r.raw); exit 1 }

# 3. calc (CV formula + over-limit judgement)
$r = HttpJson 'POST' ("/api/qc/reports/" + $repId + "/calc") $null $T
Write-Host ("calc code=" + $r.json.code + " msg=" + $r.json.msg)
if ($r.json.code -eq 200) {
    $items2 = @($r.json.data.items)
    foreach ($it in $items2) {
        Write-Host ("  item ind=" + $it.indicatorId + " sample=" + $it.sampleNo + " value=" + $it.valueStr + " over=" + $it.isOver)
    }
} else { Write-Host ("raw=" + $r.raw) }

# 4. submit
$r = HttpJson 'POST' ("/api/qc/reports/" + $repId + "/submit") $null $T
Write-Host ("submit code=" + $r.json.code + " msg=" + $r.json.msg)

# 5. verify list + statistics
$r = HttpJson 'GET' '/api/qc/reports?pageNum=1&pageSize=3' $null $T
$rows = @($r.json.data.rows)
Write-Host ("reports total=" + $r.json.data.total)
foreach ($rp in $rows) {
    Write-Host ("  rpt " + $rp.reportNo + " batch=" + $rp.batchNo + " status=" + $rp.docStatus + " public=" + $rp.isPublic)
}
$r = HttpJson 'GET' '/api/qc/reports/statistics' $null $T
$st = $r.json.data
Write-Host ("statistics total=" + $st.total + " overCount=" + $st.overCount + " overRate=" + $st.overRate)
