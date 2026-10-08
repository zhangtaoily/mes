# Pack chain: create pack task -> execute -> add box -> finish execute -> finish task
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

# 1. create pack task (weightMode = BIAO ZHONG JIAO HUO via unicode escape)
$body = '{"warehouseBatchNo":"26091341","batchNo":"26091341","taskNo":"PT20261007000003","weightMode":"\u6807\u91cd\u4ea4\u8d27"}'
$r = HttpJson 'POST' '/api/pack/tasks' $body $T
$pkTaskId = $r.json.data
Write-Host ("pack-task save code=" + $r.json.code + " msg=" + $r.json.msg + " id=" + $pkTaskId)
if ($r.json.code -ne 200) { Write-Host ("raw=" + $r.raw); exit 1 }

# 2. execute -> pack execute PC...
$r = HttpJson 'POST' ("/api/pack/tasks/" + $pkTaskId + "/execute") $null $T
$execId = $r.json.data
Write-Host ("execute code=" + $r.json.code + " msg=" + $r.json.msg + " execId=" + $execId)
if ($r.json.code -ne 200) { Write-Host ("raw=" + $r.raw); exit 1 }

# 3. add box: tare 0.5 + net 20.5 = gross 21.0
$body = '{"executeId":' + $execId + ',"netWeight":20.5,"tareWeight":0.5,"grossWeight":21.0,"grainCount":12,"actualTube":48,"packUser":"admin"}'
$r = HttpJson 'POST' ("/api/pack/executes/" + $execId + "/boxes") $body $T
Write-Host ("add-box code=" + $r.json.code + " msg=" + $r.json.msg + " boxNo=" + $r.json.data.boxCode + " publicW=" + $r.json.data.publicWeight)
if ($r.json.code -ne 200) { Write-Host ("raw=" + $r.raw); exit 1 }

# 4. verify execute summary
$r = HttpJson 'GET' ("/api/pack/executes/" + $execId) $null $T
$e = $r.json.data
Write-Host ("exec " + $e.executeNo + " boxes=" + $e.boxCount + " sumNet=" + $e.sumNetWeight + " sumPublic=" + $e.sumPublicWeight + " packed=" + $e.isPacked)

# 5. finish execute (le-bao)
$r = HttpJson 'POST' ("/api/pack/executes/" + $execId + "/finish") $null $T
Write-Host ("exec-finish code=" + $r.json.code + " msg=" + $r.json.msg)

# 6. finish pack task
$body = '{"ids":[' + $pkTaskId + ']}'
$r = HttpJson 'POST' '/api/pack/tasks/finish' $body $T
Write-Host ("pack-task finish code=" + $r.json.code + " msg=" + $r.json.msg)

# 7. verify pack task
$r = HttpJson 'GET' ("/api/pack/tasks/" + $pkTaskId) $null $T
$pt = $r.json.data
Write-Host ("pkTask " + $pt.taskNo + " finished=" + $pt.isFinished + " vatFinished=" + $pt.vatFinished)
