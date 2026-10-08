# MES 业务流程闭环测试：K3引入→工艺单→任务→排程→制造卡→投料→上批→产量→了批→包装→质检→报表
# 全链写操作，每步 FAIL 即输出原始响应并中止后续依赖步骤
$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [Text.Encoding]::UTF8
$BASE = 'http://127.0.0.1:8080'
$script:PASS = 0; $script:FAIL = 0; $script:ABORT = $false

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

function Step($name, $ok, $detail) {
    if ($ok) { $script:PASS++; Write-Host ("[PASS] {0}  {1}" -f $name, $detail) -ForegroundColor Green }
    else { $script:FAIL++; $script:ABORT = $true; Write-Host ("[FAIL] {0}`n        {1}" -f $name, $detail) -ForegroundColor Red }
}

$TODAY = Get-Date -Format 'yyyy-MM-dd'
$NOW = (Get-Date).ToString('yyyy-MM-ddTHH:mm:ss')

Write-Host '===== 0. 登录 ====='
$r = HttpJson 'POST' '/api/auth/login' '{"loginName":"admin","password":"admin@2026"}' $null
$T = $r.json.data.token
Step '登录 admin' ($r.json.code -eq 200 -and $T) ("token长度=" + ($T.Length))

if (-not $script:ABORT) {
Write-Host '===== 1. 基础数据探查 ====='
$r = HttpJson 'GET' '/api/plan/base/workshops' $null $T
$ws = @($r.json.data) | Where-Object { $_.workshopName -match '纺四' } | Select-Object -First 1
if (-not $ws) { $ws = @($r.json.data) | Select-Object -First 1 }
$WSID = $ws.id
Step '基础-车间' ($r.json.code -eq 200 -and $WSID) ("workshopId=$WSID name=" + $ws.workshopName)

$r = HttpJson 'GET' '/api/plan/base/processes' $null $T
$procs = @($r.json.data)
$P1 = $procs[0]; $P2 = $procs[1]
Step '基础-工序' ($r.json.code -eq 200 -and $P1) ("P1=$($P1.id)/$($P1.processName) P2=$($P2.id)/$($P2.processName)")

$r = HttpJson 'GET' '/api/plan/base/equipments' $null $T
$EQ = @($r.json.data) | Where-Object { $_.workshopId -eq $WSID } | Select-Object -First 1
if (-not $EQ) { $EQ = @($r.json.data) | Select-Object -First 1 }
Step '基础-设备' ($r.json.code -eq 200) ("equipmentId=$($EQ.id) name=" + $EQ.equipmentName)

$r = HttpJson 'GET' '/api/plan/base/batch-flags' $null $T
Step '基础-批号标识' ($r.json.code -eq 200) ("flags=" + @($r.json.data).Count)

Write-Host '===== 2. K3订单引入 ====='
$r = HttpJson 'GET' '/api/plan/erp-orders?pageNum=1&pageSize=50' $null $T
$orders = @($r.json.data.rows)
$avail = $orders | Where-Object { $_.importedQty -lt $_.k3Qty } | Select-Object -First 1
if ($avail) {
    $ORDER = $avail
    $IMPORTQTY = [math]::Min(300, ($ORDER.k3Qty - $ORDER.importedQty))
    $r2 = HttpJson 'POST' '/api/plan/crafts/import-k3' ('{"orderId":' + $ORDER.id + ',"workshopId":' + $WSID + ',"importQty":' + $IMPORTQTY.ToString('0.##') + '}') $T
    Step '引入K3订单→工艺单' ($r2.json.code -eq 200) ("orderNo=$($ORDER.orderNo) importQty=$IMPORTQTY => " + $(if($r2.json.code -eq 200){$r2.json.data | ConvertTo-Json -Compress}else{$r2.json.msg}))
} else {
    # 订单都已引入：找现有编制中工艺单复用
    $r2 = HttpJson 'GET' '/api/plan/crafts?pageNum=1&pageSize=20&docStatus=' $null $T
    $draft = @($r2.json.data.rows) | Where-Object { $_.docStatus -eq '编制中' } | Select-Object -First 1
    if ($draft) {
        $script:CRAFTID = $draft.id
        Step '无未引入订单，复用编制中工艺单' $true ("craftId=$($draft.id) batchNo=$($draft.batchNo)")
    } else {
        Step '查找可引入订单/可复用工艺单' $false '订单全部引入且无编制中工艺单'
    }
}

Write-Host '===== 3. 工艺单编制 ====='
if (-not $script:CRAFTID) {
    # 从引入结果拿工艺单 id
    $impData = $r2.json.data
    if ($impData -is [array]) { $script:CRAFTID = $impData[0].id } elseif ($impData.id) { $script:CRAFTID = $impData.id } else { $script:CRAFTID = $impData }
}
if (-not $script:ABORT -and $script:CRAFTID) {
    $r = HttpJson 'GET' "/api/plan/crafts/$($script:CRAFTID)" $null $T
    $CRAFT = $r.json.data
    $CRAFTBATCH = $CRAFT.batchNo
    Step '工艺单详情' ($r.json.code -eq 200 -and $CRAFT) ("craftId=$($script:CRAFTID) no=$($CRAFT.craftNo) batchNo=$CRAFTBATCH status=$($CRAFT.docStatus)")

    # 保存工序路线（两道工序）
    $routesBody = @"
{"planCraftId":$($script:CRAFTID),"routes":[
 {"route":{"seq":1,"lineType":1,"processId":$($P1.id),"isFeed":1,"isReport":1,"isLoad":0,"planQty":300,"workshopId":$WSID},"crafts":[]},
 {"route":{"seq":2,"lineType":1,"processId":$($P2.id),"isFeed":0,"isReport":1,"isLoad":0,"planQty":290,"workshopId":$WSID},"crafts":[]}]}
"@
    $r = HttpJson 'PUT' "/api/plan/crafts/$($script:CRAFTID)/routes" $routesBody $T
    Step '保存工序路线(2道)' ($r.json.code -eq 200) $r.json.msg

    # 一键带出工艺项（尽力而为，失败不阻断）
    $r3 = HttpJson 'GET' "/api/plan/tasks/craft-schemes?refSource=1&processId=$($P1.id)" $null $T
    $scheme = @($r3.json.data) | Select-Object -First 1
    if ($scheme) {
        $rc = HttpJson 'POST' "/api/plan/crafts/$($script:CRAFTID)/apply-craft" ('{"planCraftId":' + $script:CRAFTID + ',"refSource":1,"schemeId":' + $scheme.id + '}') $T
        if ($rc.json.code -eq 200) { Step '一键带出工艺项' $true ("schemeId=$($scheme.id)") }
        else { Write-Host ("[WARN] 工艺项带出失败（不阻断）: " + $rc.json.msg) -ForegroundColor Yellow }
    } else { Write-Host '[WARN] 无可用工艺库方案，跳过工艺项带出' -ForegroundColor Yellow }
}
}

Write-Host '===== 4. 工艺单提交→生成生产任务 ====='
if (-not $script:ABORT) {
    $r = HttpJson 'POST' '/api/plan/crafts/submit' ('{"ids":[' + $script:CRAFTID + ']}') $T
    Step '工艺单提交(生成任务)' ($r.json.code -eq 200) $r.json.msg

    $r = HttpJson 'GET' "/api/plan/tasks?pageNum=1&pageSize=10&batchNo=$CRAFTBATCH" $null $T
    $TASK = @($r.json.data.rows) | Select-Object -First 1
    $TASKID = $TASK.id
    Step '查询生成任务' ($TASK -and $TASK.docStatus -eq '生效') ("taskId=$TASKID taskNo=$($TASK.taskNo) status=$($TASK.docStatus) batchNo=$CRAFTBATCH")
}

Write-Host '===== 5. 排程 + 制造卡 ====='
if (-not $script:ABORT) {
    $r = HttpJson 'POST' "/api/plan/crafts/$($script:CRAFTID)/generate-schedule" $null $T
    $SCHID = $null
    if ($r.json.code -eq 200) {
        $gd = $r.json.data
        if ($gd -is [array]) { $SCHID = $gd[0].scheduleId; if (-not $SCHID) { $SCHID = $gd[0].id } }
        elseif ($gd.scheduleId) { $SCHID = $gd.scheduleId } elseif ($gd.id) { $SCHID = $gd.id }
    }
    Step '生成排程' ($r.json.code -eq 200 -and $SCHID) ("scheduleId=$SCHID => " + $(if($r.json.code -eq 200){$r.json.data | ConvertTo-Json -Compress -Depth 3 | ForEach-Object { $_.Substring(0,[math]::Min(150,$_.Length)) }}else{$r.json.msg}))

    if ($SCHID) {
        $r = HttpJson 'POST' '/api/plan/cards/generate' ("{`"ids`":[{0}]}" -f $SCHID) $T
        Step '生成制造卡' ($r.json.code -eq 200) $r.json.msg
    }
}

Write-Host '===== 6. 投料（含完毕标志） ====='
if (-not $script:ABORT) {
    $feedBody = @"
{"feed":{"taskNo":"$($TASK.taskNo)","workshopId":$WSID,"batchNo":"$CRAFTBATCH","rawDetail":"测试原料-澳毛","feedQty":300,"isFinish":1,"feedBy":"闭环测试","feedTime":"$NOW"},
 "items":[{"rawBatchNo":"RAW-LOOP-001","materialNo":"M001","materialName":"澳毛条","materialSpec":"21.5μ","pickQty":300,"thisFeedQty":300,"fedQty":300}]}
"@
    $r = HttpJson 'POST' '/api/site/feed-record' $feedBody $T
    Step '创建投料单(投料完毕)' ($r.json.code -eq 200) $r.json.msg
}

Write-Host '===== 7. 上批 → 下批 ====='
if (-not $script:ABORT) {
    $boBody = @"
{"batchOn":{"scheduleId":$SCHID,"workshopId":$WSID,"batchNo":"$CRAFTBATCH","traceBatchNo":"$CRAFTBATCH","processId":$($P1.id),"processFlag":"测试标识","equipmentId":$($EQ.id),"source":1,"batchOnTime":"$NOW","planQty":300},"flags":[],"crafts":[]}
"@
    $r = HttpJson 'POST' '/api/site/batch-on' $boBody $T
    $BOID = $null
    if ($r.json.code -eq 200) {
        $bd = $r.json.data
        if ($bd -is [array]) { $BOID = $bd[0].id } elseif ($bd.id) { $BOID = $bd.id } else { $BOID = $bd }
    }
    Step '创建上批汇报' ($r.json.code -eq 200 -and $BOID) ("batchOnId=$BOID => " + $(if($r.json.code -eq 200){''}else{$r.json.msg}))

    if ($BOID) {
        $r = HttpJson 'POST' "/api/site/batch-on/$BOID/off" $null $T
        Step '下批' ($r.json.code -eq 200) $r.json.msg
    }
}

Write-Host '===== 8. 产量汇报 ====='
if (-not $script:ABORT) {
    $outBody = @"
{"scheduleId":$SCHID,"taskId":$TASKID,"batchNo":"$CRAFTBATCH","processId":$($P1.id),"equipmentId":$($EQ.id),"outputQty":120,"reportUser":"闭环测试","reportDate":"$TODAY","unit":"kg","reportType":"正常","isWarehouse":0}
"@
    $r = HttpJson 'POST' '/api/site/output' $outBody $T
    Step '产量汇报(120kg)' ($r.json.code -eq 200) $r.json.msg
}

Write-Host '===== 9. 生产任务了批（闭环核心校验） ====='
if (-not $script:ABORT) {
    $r = HttpJson 'POST' '/api/plan/tasks/finish' ("{`"ids`":[{0}]}" -f $TASKID) $T
    Step '任务了批(投料完毕+上批闭环)' ($r.json.code -eq 200) $r.json.msg

    $r = HttpJson 'GET' "/api/plan/tasks?pageNum=1&pageSize=10&batchNo=$CRAFTBATCH" $null $T
    Step '复核任务状态=了批' (@($r.json.data.rows)[0].docStatus -eq '了批') ("status=" + @($r.json.data.rows)[0].docStatus)
}

Write-Host '===== 10. 包装链：建任务→执行→称重→了包→任务了批 ====='
if (-not $script:ABORT) {
    $pkBody = @"
{"taskVariant":"普通","batchNo":"$CRAFTBATCH","warehouseBatchNo":"WH$CRAFTBATCH","k3OrderNo":"$($ORDER.orderNo)","workshopId":$WSID,"packWorkshopId":$WSID,"productName":"测试毛条","rawMaterial":"澳毛","productSpec":"1/72 NM","weightMode":"实磅交货","packRequire":"内销纸箱(标准)","tubeCount":60,"deliveryDate":"$TODAY"}
"@
    $r = HttpJson 'POST' '/api/pack/tasks' $pkBody $T
    $PKID = $null
    if ($r.json.code -eq 200) {
        $pd = $r.json.data
        if ($pd -is [array]) { $PKID = $pd[0].id } elseif ($pd.id) { $PKID = $pd.id } else { $PKID = $pd }
    }
    Step '创建包装任务' ($r.json.code -eq 200 -and $PKID) ("pkTaskId=$PKID => " + $(if($r.json.code -eq 200){''}else{$r.json.msg}))

    if ($PKID) {
        $r = HttpJson 'POST' "/api/pack/tasks/$PKID/execute" $null $T
        $EXECID = $null
        if ($r.json.code -eq 200) {
            $ed = $r.json.data
            if ($ed -is [array]) { $EXECID = $ed[0].id } elseif ($ed.id) { $EXECID = $ed.id } else { $EXECID = $ed }
        }
        Step '包装执行单' ($r.json.code -eq 200 -and $EXECID) ("executeId=$EXECID => " + $(if($r.json.code -eq 200){''}else{$r.json.msg}))

        if ($EXECID) {
            $boxBody = '{"executeId":' + $EXECID + ',"grainCount":12,"netWeight":6.28,"grossWeight":8.78,"tareWeight":2.5,"packSpec":"25kg/箱","lineType":"手动线","packUser":"闭环测试","actualTube":12}'
            $r = HttpJson 'POST' "/api/pack/executes/$EXECID/boxes" $boxBody $T
            Step '逐箱称重(毛8.78=皮2.5+净6.28)' ($r.json.code -eq 200) $r.json.msg

            $r = HttpJson 'POST' "/api/pack/executes/$EXECID/finish" $null $T
            Step '了包' ($r.json.code -eq 200) $r.json.msg
        }

        $r = HttpJson 'POST' '/api/pack/tasks/finish' ("{`"ids`":[{0}]}" -f $PKID) $T
        Step '包装任务了批' ($r.json.code -eq 200) $r.json.msg
    }
}

Write-Host '===== 11. 质检链：抽样登记→报告→计算→提交→公示 ====='
if (-not $script:ABORT) {
    $r = HttpJson 'GET' '/api/qc/samples?pageNum=1&pageSize=50' $null $T
    $unreg = @($r.json.data.rows) | Where-Object { $_.isRegistered -ne 1 } | Select-Object -First 3
    if ($unreg) {
        $ids = ($unreg | ForEach-Object { $_.id }) -join ','
        $r2 = HttpJson 'POST' '/api/qc/samples/register' ("{`"ids`":[$ids]}") $T
        Step "抽样登记($($unreg.Count)条)" ($r2.json.code -eq 200) ("ids=[$ids] " + $r2.json.msg)
    } else { Write-Host '[WARN] 无未登记抽样，跳过登记' -ForegroundColor Yellow }

    $repBody = @"
{"workshopId":$WSID,"qcType":"工序检","labType":0,"batchNo":"$CRAFTBATCH","inspector":"闭环测试","inspectDate":"$TODAY","schemeId":1,"sampleQty":5,"rawMaterial":"澳毛","remark":"闭环测试自动创建"}
"@
    $r = HttpJson 'POST' '/api/qc/reports' $repBody $T
    $QCID = $null
    if ($r.json.code -eq 200) {
        $qd = $r.json.data
        if ($qd -is [array]) { $QCID = $qd[0].id } elseif ($qd.id) { $QCID = $qd.id } else { $QCID = $qd }
    }
    Step '创建检验报告(方案1)' ($r.json.code -eq 200 -and $QCID) ("qcReportId=$QCID => " + $(if($r.json.code -eq 200){''}else{$r.json.msg}))

    if ($QCID) {
        $r = HttpJson 'POST' "/api/qc/reports/$QCID/calc" $null $T
        Step '报告计算(公式+越限判定)' ($r.json.code -eq 200) $r.json.msg
        $r = HttpJson 'POST' "/api/qc/reports/$QCID/submit" $null $T
        Step '报告提交' ($r.json.code -eq 200) $r.json.msg
        $r = HttpJson 'POST' "/api/qc/reports/$QCID/public" $null $T
        Step '报告公示' ($r.json.code -eq 200) $r.json.msg
    }
}

Write-Host '===== 12. 报表中心（12张全查） ====='
if (-not $script:ABORT) {
    $endpoints = @(
        @{ n='质检日报';     p='/api/reports/qc-daily?dateBegin=2026-09-01&dateEnd=2026-10-07' },
        @{ n='疵纱统计';     p='/api/reports/defect-stats?dateBegin=2026-09-01&dateEnd=2026-10-07' },
        @{ n='筒子称重产量'; p='/api/reports/tube-weight-output?dateBegin=2026-01-01&dateEnd=2026-10-07' },
        @{ n='包装入库汇总'; p='/api/reports/pack-inbound?dateBegin=2026-01-01&dateEnd=2026-10-07' },
        @{ n='生产进度跟踪'; p='/api/reports/progress-tracking' },
        @{ n='纺纱生产日报'; p='/api/reports/spinning-daily?dateBegin=2026-09-01&dateEnd=2026-10-07' },
        @{ n='细毛产量日报'; p='/api/reports/wool-top-daily?dateBegin=2026-09-01&dateEnd=2026-10-07' },
        @{ n='前纺产量统计'; p='/api/reports/fore-spin-stats?dateBegin=2026-09-01&dateEnd=2026-10-07' },
        @{ n='用毛率统计';   p='/api/reports/wool-rate?periodMonth=2026-09' },
        @{ n='物理指标统计'; p='/api/reports/yarn-physical-stats?dateBegin=2026-09-01&dateEnd=2026-10-07' },
        @{ n='条干CV统计';   p='/api/reports/cv-stats?dateBegin=2026-09-01&dateEnd=2026-10-07' },
        @{ n='原料检验汇总'; p='/api/reports/raw-qc-summary' }
    )
    foreach ($e in $endpoints) {
        $r = HttpJson 'GET' $e.p $null $T
        $cnt = if ($r.json -and $r.json.data) { @($r.json.data).Count } else { 0 }
        if ($r.json.code -eq 200) { $script:PASS++; Write-Host ("[PASS] 报表-" + $e.n + "  rows=" + $cnt) -ForegroundColor Green }
        else { $script:FAIL++; Write-Host ("[FAIL] 报表-" + $e.n + "  " + $r.json.msg) -ForegroundColor Red }
    }
}

Write-Host ''
Write-Host ("========== 闭环测试结论: PASS={0} FAIL={1} ==========" -f $script:PASS, $script:FAIL)
if ($script:FAIL -eq 0) { Write-Host '业务流程闭环全部通过' -ForegroundColor Green; exit 0 } else { exit 1 }
