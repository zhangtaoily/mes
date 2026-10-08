# 为工艺单 PI20260930000113 保存工序路线（UI 无编制入口，走后端 API）
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
Write-Host ("login code={0}" -f $r.json.code)

$r = HttpJson 'GET' '/api/plan/crafts?pageNum=1&pageSize=20' $null $T
$craft = @($r.json.data.rows) | Where-Object { $_.craftNo -eq 'PI20260930000113' } | Select-Object -First 1
$CID = $craft.id
Write-Host ("craft id={0} no={1} batch={2} status={3} workshop={4}" -f $CID, $craft.craftNo, $craft.batchNo, $craft.docStatus, $craft.workshopName)

$r = HttpJson 'GET' '/api/plan/base/workshops' $null $T
$ws = @($r.json.data) | Where-Object { $_.workshopName -match '纺十' } | Select-Object -First 1
if (-not $ws) { $ws = @($r.json.data) | Select-Object -First 1 }
$WSID = $ws.id
Write-Host ("workshop id={0} name={1}" -f $WSID, $ws.workshopName)

$r = HttpJson 'GET' '/api/plan/base/processes' $null $T
$procs = @($r.json.data)
$P1 = $procs[0]; $P2 = $procs[1]
Write-Host ("P1={0}/{1} P2={2}/{3}" -f $P1.id, $P1.processName, $P2.id, $P2.processName)

$routesBody = @"
{"planCraftId":$CID,"routes":[
 {"route":{"seq":1,"lineType":1,"processId":$($P1.id),"isFeed":1,"isReport":1,"isLoad":0,"planQty":500,"workshopId":$WSID},"crafts":[]},
 {"route":{"seq":2,"lineType":1,"processId":$($P2.id),"isFeed":0,"isReport":1,"isLoad":0,"planQty":480,"workshopId":$WSID},"crafts":[]}]}
"@
$r = HttpJson 'PUT' "/api/plan/crafts/$CID/routes" $routesBody $T
Write-Host ("save-routes code={0} msg={1}" -f $r.json.code, $r.json.msg)
