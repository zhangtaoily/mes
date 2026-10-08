# Fix Redis code counters: set mes:code:{ruleId}:{period} = max(current, db serial)
$ErrorActionPreference = 'Stop'

function RedisCmd($cmd) {
    $client = New-Object System.Net.Sockets.TcpClient('127.0.0.1', 6379)
    $stream = $client.GetStream()
    $data = [Text.Encoding]::ASCII.GetBytes($cmd + "`r`n")
    $stream.Write($data, 0, $data.Length)
    Start-Sleep -Milliseconds 80
    $buf = New-Object byte[] 4096
    $n = $stream.Read($buf, 0, 4096)
    $client.Close()
    return [Text.Encoding]::UTF8.GetString($buf, 0, $n).Trim()
}

# ruleId, period, serial (from mes.sys_auto_code where serial>0 and period not null)
$rules = @(
    @{id=1;  period='20261007'; serial=3},
    @{id=2;  period='20261007'; serial=3},
    @{id=3;  period='20261007'; serial=4},
    @{id=5;  period='20261007'; serial=3},
    @{id=6;  period='20261007'; serial=3},
    @{id=7;  period='20261007'; serial=3},
    @{id=9;  period='20261007'; serial=3},
    @{id=11; period='20261007'; serial=2},
    @{id=12; period='20261007'; serial=4},
    @{id=13; period='261007';   serial=3},
    @{id=14; period='261007';   serial=50},
    @{id=15; period='261007';   serial=1},
    @{id=21; period='261007';   serial=6}
)

foreach ($r in $rules) {
    $key = "mes:code:" + $r.id + ":" + $r.period
    $cur = RedisCmd ("GET " + $key)
    if ($cur -like '*$-1*') { $curVal = 0 } else { $curVal = [int]($cur -replace '^\$\d+\r\n', '') }
    if ($r.serial -gt $curVal) {
        $resp = RedisCmd ("SET " + $key + " " + $r.serial)
        Write-Host ($key + " " + $curVal + " -> " + $r.serial + " [" + $resp + "]")
    } else {
        Write-Host ($key + " keep " + $curVal)
    }
}
