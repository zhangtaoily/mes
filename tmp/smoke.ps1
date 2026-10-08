# M6 后冒烟测试：覆盖 M0-M6 各里程碑核心接口（只读为主，不污染业务数据）
$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [Text.Encoding]::UTF8
$BASE = 'http://127.0.0.1:8080'
$script:PASS = 0
$script:FAIL = 0

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
    $json = $null
    $raw = [Text.Encoding]::UTF8.GetString($ms.ToArray())
    try { $json = $raw | ConvertFrom-Json } catch { $json = $null }
    return @{ http = [int]$resp.StatusCode; json = $json; raw = $raw }
}

function Check($name, $ok, $detail) {
    if ($ok) { $script:PASS++; Write-Host ("[PASS] {0} {1}" -f $name, $detail) }
    else { $script:FAIL++; Write-Host ("[FAIL] {0} {1}" -f $name, $detail) }
}

Write-Host '===== M0 系统域 ====='
$r = HttpJson 'GET' '/api/hello' $null $null
Check '连通性 /api/hello' ($r.http -eq 200 -and $r.json.code -eq 200) ("http=" + $r.http)

$r = HttpJson 'GET' '/api/hello/me' $null $null
Check '无token拦截 401' ($r.http -eq 401) ("http=" + $r.http)

$login = HttpJson 'POST' '/api/auth/login' '{"loginName":"admin","password":"admin@2026"}' $null
$T = $login.json.data.token
Check '登录 admin' ($login.json.code -eq 200 -and $T) ("token长度=" + ($T | ForEach-Object { $_.Length }))

$zl = HttpJson 'POST' '/api/auth/login' '{"loginName":"zhangt","password":"admin@2026"}' $null
$TZ = $zl.json.data.token
Check '登录 zhangt（工艺员+调度员）' ($zl.json.code -eq 200 -and $TZ) 'login ok'

$r = HttpJson 'GET' '/api/auth/info' $null $T
Check '当前用户 /auth/info' ($r.json.code -eq 200 -and $r.json.data.userName -eq '系统管理员') ("user=" + $r.json.data.userName)

$r = HttpJson 'GET' '/api/auth/menus' $null $T
$menuTotal = @($r.json.data).Count
foreach ($m in @($r.json.data)) { $menuTotal += @($m.children).Count }
Check '菜单树（超管全量）' ($r.json.code -eq 200 -and $menuTotal -gt 50) ("菜单总数=" + $menuTotal)

$r = HttpJson 'GET' '/api/hello/perm' $null $TZ
Check '按钮级权限 zhangt 无 sys:user:list 被拦截' ($r.http -eq 403 -and $r.json.code -eq 403) ("http=" + $r.http)

$r = HttpJson 'GET' '/api/sys/users?pageNum=1&pageSize=5' $null $T
Check '用户管理分页' ($r.json.code -eq 200 -and $r.json.data.total -ge 2) ("total=" + $r.json.data.total)

Write-Host '===== M1 引擎层 ====='
$encCode = [Uri]::EscapeDataString('计划工艺单')
$r = HttpJson 'GET' "/api/engine/code/next/$encCode" $null $T
Check '编码取号（计划工艺单）' ($r.json.code -eq 200 -and $r.json.data) ("next=" + $r.json.data)

$encBill = [Uri]::EscapeDataString('编制中')
$r = HttpJson 'GET' "/api/engine/flow/buttons?billType=qc_report&status=$encBill" $null $T
Check '状态机按钮下发(qc_report)' ($r.json.code -eq 200) ("按钮数=" + @($r.json.data).Count)

$r = HttpJson 'GET' '/api/engine/meta/columns/qc_report_item' $null $T
Check '元数据动态列' ($r.json.code -eq 200 -and @($r.json.data.columns).Count -ge 9) ("列数=" + @($r.json.data.columns).Count)

Write-Host '===== M2 plan 域 ====='
$r = HttpJson 'GET' '/api/plan/erp-orders?pageNum=1&pageSize=5' $null $T
Check 'K3订单镜像分页' ($r.json.code -eq 200) ("total=" + $r.json.data.total)

$r = HttpJson 'GET' '/api/plan/tasks?pageNum=1&pageSize=5' $null $T
Check '生产任务分页' ($r.json.code -eq 200) ("total=" + $r.json.data.total)

$r = HttpJson 'GET' '/api/plan/schedules?pageNum=1&pageSize=5' $null $T
Check '生产排程分页(只读)' ($r.json.code -eq 200) ("total=" + $r.json.data.total)

Write-Host '===== M3 produce 域 ====='
$r = HttpJson 'GET' '/api/site/batch-on?pageNum=1&pageSize=5' $null $T
Check '上批汇报分页' ($r.json.code -eq 200) ("total=" + $r.json.data.total)

$r = HttpJson 'GET' '/api/site/output?pageNum=1&pageSize=5' $null $T
Check '产量汇报分页' ($r.json.code -eq 200) ("total=" + $r.json.data.total)

$r = HttpJson 'GET' '/api/site/feed-record?pageNum=1&pageSize=5' $null $T
Check '投料记录分页' ($r.json.code -eq 200) ("total=" + $r.json.data.total)

$r = HttpJson 'GET' '/api/pda/tasks' $null $T
Check 'PDA 可投料任务（了批不下发）' ($r.json.code -eq 200) ("rows=" + @($r.json.data).Count)

Write-Host '===== M4 pack/qc 域 ====='
$r = HttpJson 'GET' '/api/pack/tasks?pageNum=1&pageSize=5' $null $T
Check '包装任务分页' ($r.json.code -eq 200) ("total=" + $r.json.data.total)

$r = HttpJson 'GET' '/api/qc/reports/statistics' $null $T
Check '质检统计' ($r.json.code -eq 200) ("total=" + $r.json.data.total)

$r = HttpJson 'GET' '/api/qc/reports?pageNum=1&pageSize=5' $null $T
Check '检验报告分页' ($r.json.code -eq 200) ("total=" + $r.json.data.total)

Write-Host '===== M5 集成层 ====='
$r = HttpJson 'GET' '/api/plugins' $null $T
Check '插件清单' ($r.json.code -eq 200 -and @($r.json.data).Count -eq 5) ("插件数=" + @($r.json.data).Count)

$r = HttpJson 'GET' '/api/integration/scale/read' $null $T
Check '秤重直读' ($r.json.code -eq 200 -and $r.json.data.unit -eq 'kg') ("weight=" + $r.json.data.weight)

$r = HttpJson 'GET' '/api/integration/sync/logs?pageNum=1&pageSize=5' $null $T
Check '同步日志分页' ($r.json.code -eq 200) ("total=" + $r.json.data.total)

Write-Host '===== M6 报表中心 ====='
$r = HttpJson 'GET' '/api/reports/catalog' $null $T
Check '报表目录' ($r.json.code -eq 200 -and @($r.json.data).Count -ge 8) ("分类数=" + @($r.json.data).Count)

$endpoints = @(
    @{ n='质检日报';        p='/api/reports/qc-daily?dateBegin=2026-09-01&dateEnd=2026-10-07' },
    @{ n='疵纱统计';        p='/api/reports/defect-stats?dateBegin=2026-09-01&dateEnd=2026-10-07' },
    @{ n='筒子称重产量';    p='/api/reports/tube-weight-output?dateBegin=2026-01-01&dateEnd=2026-10-07' },
    @{ n='包装入库汇总';    p='/api/reports/pack-inbound?dateBegin=2026-01-01&dateEnd=2026-10-07' },
    @{ n='生产进度跟踪';    p='/api/reports/progress-tracking' },
    @{ n='纺纱生产日报';    p='/api/reports/spinning-daily?dateBegin=2026-09-01&dateEnd=2026-10-07' },
    @{ n='细毛产量日报';    p='/api/reports/wool-top-daily?dateBegin=2026-09-01&dateEnd=2026-10-07' },
    @{ n='前纺产量统计';    p='/api/reports/fore-spin-stats?dateBegin=2026-09-01&dateEnd=2026-10-07' },
    @{ n='用毛率统计';      p='/api/reports/wool-rate?periodMonth=2026-09' },
    @{ n='物理指标统计';    p='/api/reports/yarn-physical-stats?dateBegin=2026-09-01&dateEnd=2026-10-07' },
    @{ n='条干CV统计';      p='/api/reports/cv-stats?dateBegin=2026-09-01&dateEnd=2026-10-07' },
    @{ n='原料检验汇总';    p='/api/reports/raw-qc-summary' }
)
foreach ($e in $endpoints) {
    $r = HttpJson 'GET' $e.p $null $T
    $cnt = if ($r.json -and $r.json.data) { @($r.json.data).Count } else { 0 }
    Check ("报表 " + $e.n) ($r.json.code -eq 200) ("rows=" + $cnt)
}

Write-Host ''
Write-Host ("========== 冒烟结论: PASS={0} FAIL={1} ==========" -f $script:PASS, $script:FAIL)
if ($script:FAIL -eq 0) { Write-Host '全部通过'; exit 0 } else { exit 1 }
