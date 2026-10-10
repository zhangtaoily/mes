# MES closed-loop API test driver (ASCII output only; UTF-8 body via byte arrays)
param([string]$Stage = "probe", [string]$Base = "http://localhost:8080")

$ErrorActionPreference = "Stop"
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

function Invoke-Json {
    param([string]$Method, [string]$Url, $BodyObj, [string]$Token)
    $headers = @{}
    if ($Token) { $headers["Authorization"] = "Bearer $Token" }
    try {
        if ($null -ne $BodyObj) {
            $json = if ($BodyObj -is [string]) { $BodyObj } else { $BodyObj | ConvertTo-Json -Depth 8 -Compress }
            $bytes = [System.Text.Encoding]::UTF8.GetBytes($json)
            $resp = Invoke-WebRequest -Uri $Url -Method $Method -Headers $headers -ContentType "application/json; charset=utf-8" -Body $bytes -UseBasicParsing -TimeoutSec 20
        } else {
            $resp = Invoke-WebRequest -Uri $Url -Method $Method -Headers $headers -UseBasicParsing -TimeoutSec 20
        }
        $raw = [System.Text.Encoding]::UTF8.GetString($resp.RawContentStream.ToArray())
        return @{ code = $resp.StatusCode; ok = $true; json = ($raw | ConvertFrom-Json) }
    } catch {
        $r = $_.Exception.Response
        if ($null -eq $r) { return @{ code = -1; ok = $false; err = $_.Exception.Message } }
        $code = [int]$r.StatusCode
        $raw = ""
        try { $sr = New-Object IO.StreamReader($r.GetResponseStream(), [Text.Encoding]::UTF8); $raw = $sr.ReadToEnd() } catch {}
        return @{ code = $code; ok = $false; raw = $raw }
    }
}

function Assert-Case {
    param([string]$Id, [bool]$Cond, [string]$Note = "")
    if ($Cond) { Write-Output ("RESULT {0} PASS {1}" -f $Id, $Note) }
    else { Write-Output ("RESULT {0} FAIL {1}" -f $Id, $Note) }
}

function Get-RId { param($json) if ($json -and $json.data -ne $null) { return $json.data } return $null }
function Get-RCode { param($x) if ($x.ok) { return $x.json.code } return $x.code }

# ---- login ----
$loginBody = @{ loginName = "admin"; password = "admin@2026" }
$lr = Invoke-Json -Method Post -Url "$Base/api/auth/login" -BodyObj $loginBody
if (-not $lr.ok -or $lr.json.code -ne 200) { Write-Output "LOGIN FAIL"; exit 1 }
$T = $lr.json.data.token
Write-Output "LOGIN OK len=$($T.Length)"

switch ($Stage) {

"probe" {
    # TC-00-03: DJ/WX code rules
    $db = "checked-manually"
    # endpoints alive check (M1-M4)
    $r1 = Invoke-Json -Method Get -Url "$Base/api/pda/workshops" -Token $T
    Assert-Case "TC-00-04a" ($r1.ok -and $r1.json.code -eq 200) "pda workshops n=$(@($r1.json.data).Count)"
    $r2 = Invoke-Json -Method Get -Url "$Base/api/pda/equip/check-items" -Token $T
    Assert-Case "TC-00-04b" ($r2.ok -and $r2.json.code -eq 200) "equip check-items n=$(@($r2.json.data).Count)"
    $r3 = Invoke-Json -Method Get -Url "$Base/api/pda/equip/fault-types" -Token $T
    Assert-Case "TC-00-04c" ($r3.ok -and $r3.json.code -eq 200) "fault-types n=$(@($r3.json.data).Count)"
    $r4 = Invoke-Json -Method Get -Url "$Base/api/pda/pack/executes" -Token $T
    Assert-Case "TC-00-04d" ($r4.ok -and $r4.json.code -eq 200) "pack executes n=$(@($r4.json.data).Count)"
    $r5 = Invoke-Json -Method Get -Url "$Base/api/pda/qc/package-reports" -Token $T
    Assert-Case "TC-00-04e" ($r5.ok -and $r5.json.code -eq 200) "qc reports n=$(@($r5.json.data).Count)"
    $r6 = Invoke-Json -Method Get -Url "$Base/api/pda/site/steams" -Token $T
    Assert-Case "TC-00-04f" ($r6.ok -and $r6.json.code -eq 200) "steams n=$(@($r6.json.data).Count)"
}

"tc80" {
    # TC-80-02: invalid token -> 401
    $bad = Invoke-Json -Method Get -Url "$Base/api/pda/workshops" -Token "invalid.token.xxx"
    Assert-Case "TC-80-02" ($bad.code -eq 401) "invalid token http=$($bad.code)"
    # TC-80-01: auth/info roles
    $info = Invoke-Json -Method Get -Url "$Base/api/auth/info" -Token $T
    $roles = ($info.json.data.roles -join ",")
    Assert-Case "TC-80-01" ($info.ok -and $roles -like "*ROLE_ADMIN*") "roles=$roles"
    # TC-80-03: admin has wildcard perms
    $perms = ($info.json.data.perms -join ",")
    Assert-Case "TC-80-03a" ($perms -eq "*") "perms wildcard=$perms"
    # TC-80-04: serial code no-duplicate (three outputs same batch)
    $s1 = Invoke-Json -Method Get -Url "$Base/api/pda/tasks" -Token $T
    $taskCount = @($s1.json.data).Count
    Assert-Case "TC-80-04pre" ($taskCount -gt 0) "available tasks n=$taskCount"
}

"tc10" {
    # data-existence based checks (planning chain built earlier)
    $sch = Invoke-Json -Method Get -Url "$Base/api/pda/workshops" -Token $T
    Assert-Case "TC-10-05" ($sch.ok) "workshops ok"
    $tk = Invoke-Json -Method Get -Url "$Base/api/pda/tasks" -Token $T
    Assert-Case "TC-10-05b" ($tk.ok -and @($tk.json.data).Count -gt 0) "tasks n=$(@($tk.json.data).Count)"
    # machines for scan targets
    $m = Invoke-Json -Method Get -Url "$Base/api/pda/machines" -Token $T
    $first = @($m.json.data)[0]
    Assert-Case "TC-10-05c" ($m.ok -and $null -ne $first) "machine0=$($first.machineNo)"
}

"tc50" {
    # scan machine
    $mach = Invoke-Json -Method Get -Url "$Base/api/pda/machines" -Token $T
    $m0 = @($mach.json.data)[0]
    $no = $m0.machineNo
    $scan = Invoke-Json -Method Get -Url "$Base/api/pda/equip/scan-machine?keyword=$no" -Token $T
    Assert-Case "TC-50-01" ($scan.ok -and $scan.json.code -eq 200 -and $scan.json.data.equipment.machineNo -eq $no) "machine=$no ws=$($scan.json.data.workshopName) duty=$($scan.json.data.dutyUsers)"
    $wsId = $scan.json.data.equipment.workshopId
    # submit check
    $desc = "PDA-TEST check m/$no item1=OK item2=BAD note=abnormal-2"
    $chk = Invoke-Json -Method Post -Url "$Base/api/pda/equip/checks" -Token $T -BodyObj @{ workshopId = $wsId; describe = $desc }
    Assert-Case "TC-50-02" ($chk.ok -and $chk.json.code -eq 200 -and $chk.json.data -gt 0) "check id=$(Get-RId $chk.json)"
    $checkId = Get-RId $chk.json
    # PC list same-source
    $pcList = Invoke-Json -Method Get -Url "$Base/api/equip/check-records?recordCode=" -Token $T
    $found = @($pcList.json.data.rows) | Where-Object { $_.id -eq $checkId }
    Assert-Case "TC-50-03" ($null -ne $found -and $found.checkUser -ne $null -and $found.checkDate -ne $null) "pc-list found checkUser=$($found.checkUser) date=$($found.checkDate) code=$($found.recordCode)"
    # repair register
    $rep = Invoke-Json -Method Post -Url "$Base/api/pda/equip/repairs" -Token $T -BodyObj @{ equipmentId = $m0.id; detail = "PDA-TEST fault on $no" }
    Assert-Case "TC-50-04" ($rep.ok -and $rep.json.code -eq 200 -and $rep.json.data -gt 0) "repair id=$(Get-RId $rep.json)"
    $repId = Get-RId $rep.json
    # repairs list unsolved first
    $lst = Invoke-Json -Method Get -Url "$Base/api/pda/equip/repairs?unsolved=true" -Token $T
    $foundRep = @($lst.json.data) | Where-Object { $_.id -eq $repId }
    Assert-Case "TC-50-06" ($null -ne $foundRep -and $null -eq $foundRep.solveTime) "repairs-list contains unsolved"
    # solve
    $sv = Invoke-Json -Method Post -Url "$Base/api/pda/equip/repairs/$repId/solve" -Token $T
    Assert-Case "TC-50-07" ($sv.ok -and $sv.json.code -eq 200) "solved"
    # duplicate solve blocked
    $sv2 = Invoke-Json -Method Post -Url "$Base/api/pda/equip/repairs/$repId/solve" -Token $T
    Assert-Case "TC-50-08" (-not $sv2.ok -or $sv2.json.code -ne 200) "dup-solve blocked code=$(Get-RCode $sv2)"
    # PC repair list same-source
    $pcr = Invoke-Json -Method Get -Url "$Base/api/equip/repair-records" -Token $T
    $foundPcr = @($pcr.json.data.rows) | Where-Object { $_.id -eq $repId }
    Assert-Case "TC-50-09" ($null -ne $foundPcr -and $foundPcr.solveTime -ne $null) "pc-repair solveTime=$($foundPcr.solveTime)"
    Write-Output "CLEANUP equip check=$checkId repair=$repId"
}

"tc40" {
    # find a warehouse batch from pack executes
    $ex = Invoke-Json -Method Get -Url "$Base/api/pda/pack/executes" -Token $T
    $e0 = @($ex.json.data) | Select-Object -First 1
    if ($null -eq $e0) { Assert-Case "TC-40-02" $false "no pack execute to derive batch"; break }
    $wb = $e0.warehouseBatchNo
    $body = @{ warehouseBatchNo = "$wb"; defects = @(@{ defectType = "棉结"; value = 2 }, @{ defectType = "细节"; value = 1 }) }
    $sv = Invoke-Json -Method Post -Url "$Base/api/pda/qc/package-reports" -Token $T -BodyObj $body
    $rid = Get-RId $sv.json
    Assert-Case "TC-40-02" ($sv.ok -and $sv.json.code -eq 200 -and $rid -gt 0) "report id=$rid wb=$wb"
    # detail re-edit save
    $dt = Invoke-Json -Method Get -Url "$Base/api/pda/qc/package-reports/$rid" -Token $T
    $defN = @($dt.json.data.defects).Count
    Assert-Case "TC-40-03a" ($dt.ok -and $defN -ge 2) "detail defects n=$defN"
    $sv2 = Invoke-Json -Method Post -Url "$Base/api/pda/qc/package-reports" -Token $T -BodyObj @{ id = $rid; warehouseBatchNo = "$wb"; defects = @(@{ defectType = "棉结"; value = 3 }) }
    Assert-Case "TC-40-03b" ($sv2.ok -and $sv2.json.code -eq 200) "re-save ok"
    $dt2 = Invoke-Json -Method Get -Url "$Base/api/pda/qc/package-reports/$rid" -Token $T
    $defN2 = @($dt2.json.data.defects).Count
    Assert-Case "TC-40-03c" ($defN2 -eq 1) "defects replaced n=$defN2"
    Write-Output "CLEANUP qc-report=$rid"
}

"tc20" {
    $SCHED = 13; $CARD = "MC20260930000007"; $TASKNO = "PT20260930000005"; $BATCH = "26091340"
    # TC-20-01 scan by card qrCode
    $scan = Invoke-Json -Method Post -Url "$Base/api/pda/scan" -Token $T -BodyObj @{ content = $CARD }
    $schOk = $scan.ok -and $scan.json.code -eq 200 -and $scan.json.data.schedule.id -eq $SCHED
    Assert-Case "TC-20-01" $schOk "schedule=$($scan.json.data.schedule.scheduleNo) machine=$($scan.json.data.machineNo) active=$($scan.json.data.activeBatchOn)"
    # TC-20-02 batch-on
    $bon = Invoke-Json -Method Post -Url "$Base/api/pda/batch-on" -Token $T -BodyObj @{
        batchOn = @{ scheduleId = $SCHED; batchNo = $BATCH; workshopId = 1; equipmentId = 1; teamId = 1 }
    }
    $bonId = Get-RId $bon.json
    Assert-Case "TC-20-02" ($bon.ok -and $bon.json.code -eq 200 -and $bonId -gt 0) "batchOn id=$bonId err=$(if(-not $bon.ok){$bon.raw.Substring(0,[Math]::Min(150,$bon.raw.Length))})"
    # TC-20-03 duplicate batch-on blocked
    $bon2 = Invoke-Json -Method Post -Url "$Base/api/pda/batch-on" -Token $T -BodyObj @{
        batchOn = @{ scheduleId = $SCHED; batchNo = $BATCH; workshopId = 1; equipmentId = 1 }
    }
    Assert-Case "TC-20-03" (-not $bon2.ok -or $bon2.json.code -ne 200) "dup blocked code=$(Get-RCode $bon2)"
    # TC-20-04 feed
    $fd = Invoke-Json -Method Post -Url "$Base/api/pda/feed" -Token $T -BodyObj @{
        feed = @{ taskNo = $TASKNO; teamId = 1; isFinish = 0; feedQty = 10 }
        items = @(@{ rawBatchNo = "RL26090002"; thisFeedQty = 10 })
    }
    $fdId = Get-RId $fd.json
    Assert-Case "TC-20-04" ($fd.ok -and $fd.json.code -eq 200 -and $fdId -gt 0) "feed id=$fdId err=$(if(-not $fd.ok){$fd.raw.Substring(0,[Math]::Min(150,$fd.raw.Length))})"
    # TC-20-06 output
    $out = Invoke-Json -Method Post -Url "$Base/api/pda/output" -Token $T -BodyObj @{
        scheduleId = $SCHED; taskId = 4; batchNo = $BATCH; equipmentId = 1; outputQty = 5
    }
    $outId = Get-RId $out.json
    Assert-Case "TC-20-06" ($out.ok -and $out.json.code -eq 200 -and $outId -gt 0) "output id=$outId err=$(if(-not $out.ok){$out.raw.Substring(0,[Math]::Min(150,$out.raw.Length))})"
    # TC-20-07 stop (auto reportUser/date/machineModel)
    $stop = Invoke-Json -Method Post -Url "$Base/api/pda/stop" -Token $T -BodyObj @{
        equipmentId = 1; stopReason = "PDA-TEST-STOP"
    }
    $stopId = Get-RId $stop.json
    Assert-Case "TC-20-07" ($stop.ok -and $stop.json.code -eq 200 -and $stopId -gt 0) "stop id=$stopId err=$(if(-not $stop.ok){$stop.raw.Substring(0,[Math]::Min(150,$stop.raw.Length))})"
    # TC-20-08 front report with empty workshop
    $front = Invoke-Json -Method Post -Url "$Base/api/pda/front-report" -Token $T -BodyObj @{
        batchNo = $BATCH; teamName = "PDA-TEST-TEAM"; situation = "PDA-TEST-FRONT"
    }
    $frId = Get-RId $front.json
    Assert-Case "TC-20-08" ($front.ok -and $front.json.code -eq 200 -and $frId -gt 0) "front id=$frId err=$(if(-not $front.ok){$front.raw.Substring(0,[Math]::Min(150,$front.raw.Length))})"
    # TC-20-09 batch-off
    $boff = Invoke-Json -Method Post -Url "$Base/api/pda/batch-off/$bonId" -Token $T
    Assert-Case "TC-20-09" ($boff.ok -and $boff.json.code -eq 200) "batch-off ok"
    # TC-20-12 PC list same-source: batch-on list contains record (PageResult.records)
    $pcl = Invoke-Json -Method Get -Url "$Base/api/site/batch-on?batchNo=$BATCH" -Token $T
    $rows = $pcl.json.data.records
    if ($null -eq $rows) { $rows = $pcl.json.data.rows }
    $pclOk = $pcl.ok -and (@($rows) | Where-Object { $_.batchOn.id -eq $bonId -or $_.id -eq $bonId }) -ne $null
    Assert-Case "TC-20-12" $pclOk "pc batch-on list contains id=$bonId"
    Write-Output "CLEANUP bon=$bonId feed=$fdId out=$outId stop=$stopId front=$frId"
}

"tc20-11" {
    # task 4 must be set to le-pi via SQL before this stage; then scan+batch-on blocked
    $scan = Invoke-Json -Method Post -Url "$Base/api/pda/scan" -Token $T -BodyObj @{ content = "MC20260930000007" }
    if (-not ($scan.ok -and $scan.json.code -eq 200)) { Assert-Case "TC-20-11" $false "scan failed"; break }
    $bon = Invoke-Json -Method Post -Url "$Base/api/pda/batch-on" -Token $T -BodyObj @{
        batchOn = @{ scheduleId = 13; batchNo = "26091340"; workshopId = 1; equipmentId = 1 }
    }
    Assert-Case "TC-20-11" (-not $bon.ok -or $bon.json.code -ne 200) "blocked code=$(Get-RCode $bon)"
}

"tc30" {
    # pick an unpacked execute
    $exs = Invoke-Json -Method Get -Url "$Base/api/pda/pack/executes" -Token $T
    $e = @($exs.json.data) | Where-Object { $_.isPacked -eq 0 } | Select-Object -First 1
    if ($null -eq $e) { Assert-Case "TC-30-01" $false "no unpacked execute"; break }
    Write-Output ("EXECUTE id={0} no={1} wb={2}" -f $e.id, $e.executeNo, $e.warehouseBatchNo)
    # TC-30-02 add box net+tare=gross
    $b1 = Invoke-Json -Method Post -Url "$Base/api/pda/pack/boxes" -Token $T -BodyObj @{
        executeId = $e.id; netWeight = 10; tareWeight = 0.5; grossWeight = 10.5
    }
    $b1id = $b1.json.data.id
    Assert-Case "TC-30-02" ($b1.ok -and $b1.json.code -eq 200 -and $b1id -gt 0) "box id=$b1id code=$($b1.json.data.boxCode) err=$(if(-not $b1.ok){$b1.raw.Substring(0,[Math]::Min(120,$b1.raw.Length))})"
    # TC-30-03 identity violated blocked
    $bad = Invoke-Json -Method Post -Url "$Base/api/pda/pack/boxes" -Token $T -BodyObj @{
        executeId = $e.id; netWeight = 10; tareWeight = 0.5; grossWeight = 11
    }
    Assert-Case "TC-30-03" (-not $bad.ok -or $bad.json.code -ne 200) "identity blocked code=$(Get-RCode $bad)"
    # TC-30-04 delete wrong box
    $dl = Invoke-Json -Method Delete -Url "$Base/api/pda/pack/boxes/$b1id" -Token $T
    Assert-Case "TC-30-04" ($dl.ok -and $dl.json.code -eq 200) "box deleted"
    # two boxes for palletize
    $b2 = Invoke-Json -Method Post -Url "$Base/api/pda/pack/boxes" -Token $T -BodyObj @{
        executeId = $e.id; netWeight = 12; tareWeight = 0.5; grossWeight = 12.5
    }
    $b3 = Invoke-Json -Method Post -Url "$Base/api/pda/pack/boxes" -Token $T -BodyObj @{
        executeId = $e.id; netWeight = 13; tareWeight = 0.5; grossWeight = 13.5
    }
    $b2id = $b2.json.data.id; $b3id = $b3.json.data.id
    $b2code = $b2.json.data.boxCode; $b3code = $b3.json.data.boxCode
    # TC-30-05 scan + palletize
    $sb = Invoke-Json -Method Post -Url "$Base/api/pda/pack/scan-box" -Token $T -BodyObj @{ boxCode = $b2code }
    Assert-Case "TC-30-05a" ($sb.ok -and $sb.json.code -eq 200 -and $sb.json.data.execute.id -eq $e.id) "scan-box ok execute resolved"
    $pl = Invoke-Json -Method Post -Url "$Base/api/pda/pack/palletize" -Token $T -BodyObj @{ boxCodes = @($b2code, $b3code) }
    $pallet = Get-RId $pl.json
    Assert-Case "TC-30-05b" ($pl.ok -and $pl.json.code -eq 200 -and $pallet) "pallet=$pallet err=$(if(-not $pl.ok){$pl.raw.Substring(0,[Math]::Min(120,$pl.raw.Length))})"
    # TC-30-06 re-palletize blocked
    $sb2 = Invoke-Json -Method Post -Url "$Base/api/pda/pack/scan-box" -Token $T -BodyObj @{ boxCode = $b2code }
    Assert-Case "TC-30-06" (-not $sb2.ok -or $sb2.json.code -ne 200) "re-palletize scan blocked code=$(Get-RCode $sb2)"
    # box-info still queryable (reprint scenario)
    $bi = Invoke-Json -Method Get -Url "$Base/api/pda/pack/box-info?boxCode=$b2code" -Token $T
    Assert-Case "TC-30-06b" ($bi.ok -and $bi.json.code -eq 200) "box-info ok (palletized not blocked)"
    # TC-30-08 label reprint by boxCode
    $rp = Invoke-Json -Method Post -Url "$Base/api/pda/pack/label-reprint" -Token $T -BodyObj @{ boxCode = $b2code }
    Assert-Case "TC-30-08" ($rp.ok -and $rp.json.code -eq 200) "reprint id=$(Get-RId $rp.json) err=$(if(-not $rp.ok){$rp.raw.Substring(0,[Math]::Min(120,$rp.raw.Length))})"
    # TC-30-07 tube print on a task
    $tks = Invoke-Json -Method Get -Url "$Base/api/pda/pack/tasks" -Token $T
    $t0 = @($tks.json.data) | Select-Object -First 1
    if ($null -ne $t0) {
        $tp = Invoke-Json -Method Post -Url "$Base/api/pda/pack/print-tube/$($t0.id)?qty=1" -Token $T
        Assert-Case "TC-30-07" ($tp.ok -and $tp.json.code -eq 200) "tube print task=$($t0.taskNo)"
    } else { Assert-Case "TC-30-07" $false "no pack task" }
    # TC-30-09 finish (restored via SQL in cleanup)
    $fin = Invoke-Json -Method Post -Url "$Base/api/pda/pack/executes/$($e.id)/finish" -Token $T
    Assert-Case "TC-30-09" ($fin.ok -and $fin.json.code -eq 200) "finished"
    $fin2 = Invoke-Json -Method Post -Url "$Base/api/pda/pack/executes/$($e.id)/finish" -Token $T
    Assert-Case "TC-30-09b" (-not $fin2.ok -or $fin2.json.code -ne 200) "re-finish blocked code=$(Get-RCode $fin2)"
    $boxAfter = Invoke-Json -Method Post -Url "$Base/api/pda/pack/boxes" -Token $T -BodyObj @{
        executeId = $e.id; netWeight = 1; tareWeight = 0.1; grossWeight = 1.1
    }
    Assert-Case "TC-30-09c" (-not $boxAfter.ok -or $boxAfter.json.code -ne 200) "add-box after finish blocked code=$(Get-RCode $boxAfter)"
    Write-Output "CLEANUP execute=$($e.id) boxes=$b2id,$b3id pallet=$pallet"
}

"tc60" {
    # picks inserted via SQL beforehand (CLEANUP-PICK-IDS)
    $pk = Invoke-Json -Method Get -Url "$Base/api/pda/site/picks" -Token $T
    $pending = @($pk.json.data) | Where-Object { $_.pickNo -like "PDATEST*" }
    Assert-Case "TC-60-01" (@($pk.json.data).Count -gt 0 -and @($pending).Count -ge 2) "picks total=$(@($pk.json.data).Count) pending-test=$(@($pending).Count)"
    if (@($pending).Count -lt 2) { break }
    $ids = @($pending | Select-Object -First 2 | ForEach-Object { $_.id })
    # TC-60-02 batch confirm (sql replaced status marker to 待确认 already)
    $cf = Invoke-Json -Method Post -Url "$Base/api/pda/site/picks/confirm" -Token $T -BodyObj $ids
    Assert-Case "TC-60-02" ($cf.ok -and $cf.json.code -eq 200) "confirmed ids=$($ids -join ',')"
    $pk2 = Invoke-Json -Method Get -Url "$Base/api/pda/site/picks" -Token $T
    $c1 = @($pk2.json.data) | Where-Object { $_.id -eq $ids[0] }
    Assert-Case "TC-60-02b" ($null -ne $c1 -and $c1.docStatus -eq "已确认" -and $c1.picker) "status=$($c1.docStatus) picker=$($c1.picker) time=$($c1.pickTime)"
    # TC-60-03 re-confirm blocked
    $cf2 = Invoke-Json -Method Post -Url "$Base/api/pda/site/picks/confirm" -Token $T -BodyObj $ids
    Assert-Case "TC-60-03" (-not $cf2.ok -or $cf2.json.code -ne 200) "re-confirm blocked code=$(Get-RCode $cf2)"
    # TC-60-04 register truck
    $tv = Invoke-Json -Method Post -Url "$Base/api/pda/site/trucks" -Token $T -BodyObj @{ truck = @{ truckNo = "PDA-T01"; workshopId = 1 } }
    $tid = Get-RId $tv.json
    Assert-Case "TC-60-04" ($tv.ok -and $tv.json.code -eq 200 -and $tid -gt 0) "truck id=$tid err=$(if(-not $tv.ok){$tv.raw.Substring(0,[Math]::Min(120,$tv.raw.Length))})"
    $dup = Invoke-Json -Method Post -Url "$Base/api/pda/site/trucks" -Token $T -BodyObj @{ truck = @{ truckNo = "PDA-T01"; workshopId = 1 } }
    Assert-Case "TC-60-04b" (-not $dup.ok -or $dup.json.code -ne 200) "dup truckNo blocked code=$(Get-RCode $dup)"
    # TC-60-05 five moves (scanWorkshop omitted -> backend fills)
    $okMoves = 0
    foreach ($op in @("绑定","称重","入库","移库","出库")) {
        $mv = Invoke-Json -Method Post -Url "$Base/api/pda/site/trucks/move" -Token $T -BodyObj @{ truckId = $tid; opName = $op; toLocation = "LOC-$op" }
        if ($mv.ok -and $mv.json.code -eq 200) { $okMoves++ } else { Write-Output "MOVE-FAIL $op $(if(-not $mv.ok){$mv.raw.Substring(0,[Math]::Min(100,$mv.raw.Length))})" }
    }
    $dt = Invoke-Json -Method Get -Url "$Base/api/pda/site/trucks/$tid" -Token $T
    $mvN = @($dt.json.data.moves).Count
    Assert-Case "TC-60-05" ($okMoves -eq 5 -and $mvN -eq 5) "moves ok=$okMoves traceN=$mvN"
    # TC-60-07 steam register
    $st = Invoke-Json -Method Post -Url "$Base/api/pda/site/steams" -Token $T -BodyObj @{ truckNo = "PDA-T01"; batchNo = "26091340"; grainCount = 100; weight = 50 }
    $sid = Get-RId $st.json
    Assert-Case "TC-60-07" ($st.ok -and $st.json.code -eq 200 -and $sid -gt 0) "steam id=$sid"
    # TC-60-08 receive before print blocked
    $rc = Invoke-Json -Method Post -Url "$Base/api/pda/site/steams/$sid/receive" -Token $T
    Assert-Case "TC-60-08" (-not $rc.ok -or $rc.json.code -ne 200) "receive-before-print blocked code=$(Get-RCode $rc)"
    # TC-60-09 print then receive
    $pr = Invoke-Json -Method Post -Url "$Base/api/pda/site/steams/$sid/print" -Token $T
    $rc2 = Invoke-Json -Method Post -Url "$Base/api/pda/site/steams/$sid/receive" -Token $T
    Assert-Case "TC-60-09" ($pr.ok -and $rc2.ok -and $pr.json.code -eq 200 -and $rc2.json.code -eq 200) "print+receive ok"
    # TC-60-10 ordering: unprinted first
    $stl = Invoke-Json -Method Get -Url "$Base/api/pda/site/steams" -Token $T
    $list = @($stl.json.data)
    $firstUnprinted = $list | Where-Object { -not $_.printTime } | Select-Object -First 1
    $okOrder = $true
    if ($null -ne $firstUnprinted -and $list[0].printTime) { $okOrder = $false }
    Assert-Case "TC-60-10" $okOrder "first=$($list[0].truckNo) printTime=$($list[0].printTime)"
    Write-Output "CLEANUP picks=$($ids -join ',') truck=$tid steam=$sid"
}

"tc90" {
    # TC-90-02 concurrent outputs -> OR serial no duplicate (2 parallel jobs)
    $loginBody2 = @{ loginName = "admin"; password = "admin@2026" }
    $lr2 = Invoke-Json -Method Post -Url "$Base/api/auth/login" -BodyObj $loginBody2
    $T2 = $lr2.json.data.token
    $outBody = @{ scheduleId = 14; taskId = 4; batchNo = "26091340"; equipmentId = 2; outputQty = 1 } | ConvertTo-Json -Compress
    $job1 = Start-Job -ScriptBlock { param($u,$t,$b) try { $r=Invoke-WebRequest -Uri "$u/api/pda/output" -Method Post -Headers @{Authorization="Bearer $t"} -ContentType "application/json; charset=utf-8" -Body ([Text.Encoding]::UTF8.GetBytes($b)) -UseBasicParsing; [Text.Encoding]::UTF8.GetString($r.RawContentStream.ToArray()) } catch { "ERR" } } -ArgumentList $Base, $T, $outBody
    $job2 = Start-Job -ScriptBlock { param($u,$t,$b) try { $r=Invoke-WebRequest -Uri "$u/api/pda/output" -Method Post -Headers @{Authorization="Bearer $t"} -ContentType "application/json; charset=utf-8" -Body ([Text.Encoding]::UTF8.GetBytes($b)) -UseBasicParsing; [Text.Encoding]::UTF8.GetString($r.RawContentStream.ToArray()) } catch { "ERR" } } -ArgumentList $Base, $T2, $outBody
    $r1 = Receive-Job -Job $job1 -Wait; $r2 = Receive-Job -Job $job2 -Wait
    Remove-Job $job1,$job2 -Force
    Write-Output "RAW1=$r1"
    Write-Output "RAW2=$r2"
    $o1 = ($r1 | ConvertFrom-Json).data; $o2 = ($r2 | ConvertFrom-Json).data
    Assert-Case "TC-90-02" ($o1 -gt 0 -and $o2 -gt 0 -and $o1 -ne $o2) "concurrent outputs id1=$o1 id2=$o2"
    # TC-90-03 evidence: public weight formula computed (non-NaN) in tc30 boxes
    $pd = Invoke-Json -Method Get -Url "$Base/api/pda/pack/executes/23" -Token $T
    $pw = @($pd.json.data.boxes) | Select-Object -First 1
    Assert-Case "TC-90-03" ($null -ne $pw -and $pw.publicWeight -gt 0) "publicWeight=$($pw.publicWeight) (GraalJS formula ok)"
    Write-Output "CLEANUP outputs=$o1,$o2"
}

default { Write-Output "UNKNOWN stage $Stage" }
}
