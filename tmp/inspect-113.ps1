# Inspect craft 113 materials + task 5 status
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

# craft 113 detail: find by craftNo
$r = HttpJson 'GET' '/api/plan/crafts?pageNum=1&pageSize=20' $null $T
$craft = @($r.json.data.rows) | Where-Object { $_.craftNo -eq 'PI20260930000113' } | Select-Object -First 1
Write-Host ("craft id=" + $craft.id + " status=" + $craft.docStatus + " batch=" + $craft.batchNo)

$r = HttpJson 'GET' "/api/plan/crafts/$($craft.id)" $null $T
$mats = @($r.json.data.materials)
Write-Host ("craft materials count=" + $mats.Count)
foreach ($m in $mats) { Write-Host ("  rawBatchNo=" + $m.rawBatchNo + " rawName=" + $m.rawName + " feedQty=" + $m.feedQty + " workshopQty=" + $m.workshopQty + " unit=" + $m.unit) }

# all tasks
$r = HttpJson 'GET' '/api/plan/tasks?pageNum=1&pageSize=20' $null $T
foreach ($t in @($r.json.data.rows)) {
    Write-Host ("task id=" + $t.id + " " + $t.taskNo + " status=" + $t.docStatus + " batch=" + $t.batchNo)
}

# task 5 & 6 materials
foreach ($tid in 5,6) {
    $r = HttpJson 'GET' "/api/plan/tasks/$tid" $null $T
    $tm = @($r.json.data.materials)
    Write-Host ("task $tid materials=" + $tm.Count + " status=" + $r.json.data.task.docStatus)
    foreach ($m in $tm) { Write-Host ("  rawBatchNo=" + $m.rawBatchNo + " planFeedQty=" + $m.planFeedQty) }
}
