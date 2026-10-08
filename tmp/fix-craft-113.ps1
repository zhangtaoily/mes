# Add materials to craft 113 (PI20260930000113) then verify
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

# 1. get craft detail
$r = HttpJson 'GET' '/api/plan/crafts/11' $null $T
Write-Host ("detail http=" + $r.http + " status=" + $r.json.data.craft.docStatus + " prods=" + @($r.json.data.prods).Count + " materials=" + @($r.json.data.materials).Count + " rawQcs=" + @($r.json.data.rawQcs).Count)

# 2. build save payload (add one material row)
# PS5.1 陷阱：单对象/空数组经管道 ConvertTo-Json 不是合法 JSON 数组，需按 0/1/N 兜底
function ToJsonArray($arr) {
    if ($null -eq $arr -or @($arr).Count -eq 0) { return '[]' }
    if (@($arr).Count -eq 1) { return '[' + ($arr | ConvertTo-Json -Depth 6 -Compress) + ']' }
    return ($arr | ConvertTo-Json -Depth 6 -Compress)
}
$craft = $r.json.data.craft | ConvertTo-Json -Depth 6 -Compress
$prods = ToJsonArray $r.json.data.prods
$rawQcs = ToJsonArray $r.json.data.rawQcs
$materials = '[{"rawBatchNo":"RAW-113-001","rawName":"AU-WOOL-TOP","workshopQty":500,"mixQty":500,"feedQty":500,"unit":"kg"}]'
$body = '{"craft":' + $craft + ',"prods":' + $prods + ',"materials":' + $materials + ',"rawQcs":' + $rawQcs + '}'
Write-Host ("payload len=" + $body.Length + " prods=" + $prods + " rawQcs=" + $rawQcs)

$r = HttpJson 'POST' '/api/plan/crafts' $body $T
Write-Host ("save http=" + $r.http + " code=" + $r.json.code + " msg=" + $r.json.msg)

# 3. verify
$r = HttpJson 'GET' '/api/plan/crafts/11' $null $T
$mats = @($r.json.data.materials)
Write-Host ("verify status=" + $r.json.data.craft.docStatus + " materials=" + $mats.Count)
foreach ($m in $mats) {
    Write-Host ("  mat " + $m.rawBatchNo + " | " + $m.rawName + " | feed=" + $m.feedQty + " " + $m.unit)
}
