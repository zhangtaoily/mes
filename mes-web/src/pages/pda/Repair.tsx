import { useEffect, useRef, useState } from 'react'
import { App, Button, Input, Select, Spin, Tabs, Tag } from 'antd'
import type { InputRef } from 'antd'
import { SafetyCertificateOutlined, ScanOutlined, SendOutlined, ToolOutlined } from '@ant-design/icons'
import { pdaFaultTypes, pdaRepairs, pdaSaveRepair, pdaScanMachine, pdaSolveRepair } from '@/api/pda'
import type { PdaFaultType, PdaMachineVO, PdaRepairRecord } from '@/api/pda'
import { offlineEnqueue, isNetworkError } from '@/utils/offline'
import { useAuth } from '@/store/auth'

/** PDA 报修登记：扫机台 → 选故障分类 → 填详情提交（编码自动取号）；维修Tab闭环未解决报修单 */
export default function PdaRepair() {
  return (
    <div style={{ padding: 14 }}>
      <Tabs
        items={[
          { key: 'new', label: '报修登记', children: <NewPanel /> },
          { key: 'solve', label: '维修处理', children: <SolvePanel /> },
        ]}
      />
    </div>
  )
}

/** 报修登记面板 */
function NewPanel() {
  const { message } = App.useApp()
  const { hasPerm } = useAuth()
  const inputRef = useRef<InputRef>(null)
  const [keyword, setKeyword] = useState('')
  const [loadingMachine, setLoadingMachine] = useState(false)
  const [machine, setMachine] = useState<PdaMachineVO | null>(null)
  const [unsolved, setUnsolved] = useState<PdaRepairRecord[]>([])
  const [faultTypes, setFaultTypes] = useState<PdaFaultType[]>([])
  const [faultTypeId, setFaultTypeId] = useState<number | null>(null)
  const [detail, setDetail] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const canSave = hasPerm('equip:repairRecord:list')

  useEffect(() => {
    pdaFaultTypes().then(setFaultTypes).catch(() => setFaultTypes([]))
  }, [])

  const scan = async () => {
    const kw = keyword.trim()
    if (!kw) {
      message.warning('请扫描或输入机台号')
      inputRef.current?.focus()
      return
    }
    setLoadingMachine(true)
    try {
      const vo = await pdaScanMachine(kw)
      setMachine(vo)
      pdaRepairs({ equipmentId: vo.equipment.id, unsolved: true }).then(setUnsolved).catch(() => setUnsolved([]))
    } catch {
      setMachine(null)
      setUnsolved([])
    } finally {
      setLoadingMachine(false)
      inputRef.current?.focus()
    }
  }

  const submit = async () => {
    if (!machine) {
      message.warning('请先扫描机台')
      return
    }
    if (!detail.trim()) {
      message.warning('请填写故障详情')
      return
    }
    const body = {
      equipmentId: machine.equipment.id,
      workshopId: machine.equipment.workshopId ?? undefined,
      faultTypeId: faultTypeId ?? undefined,
      detail: detail.trim(),
    }
    setSubmitting(true)
    try {
      await pdaSaveRepair(body)
      message.success('报修已登记')
      setMachine(null)
      setKeyword('')
      setFaultTypeId(null)
      setDetail('')
      inputRef.current?.focus()
    } catch (e) {
      if (isNetworkError(e)) {
        offlineEnqueue('/api/pda/equip/repairs', body, `报修 机台#${machine.equipment.machineNo ?? '-'}`)
        message.info('网络异常，已存入离线待提交')
        setMachine(null)
        setKeyword('')
        setFaultTypeId(null)
        setDetail('')
        inputRef.current?.focus()
      }
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <>
      <div style={{ background: '#fff', borderRadius: 12, padding: 12, marginBottom: 12, boxShadow: '0 1px 4px rgba(0,0,0,.06)' }}>
        <Input
          ref={inputRef} size="large" autoFocus
          prefix={<ScanOutlined style={{ color: '#722ed1', fontSize: 18 }} />}
          placeholder="扫/输机台号"
          value={keyword} onChange={(e) => setKeyword(e.target.value)} onPressEnter={scan} style={{ fontSize: 15 }}
        />
        <Button type="primary" size="large" block style={{ height: 46, marginTop: 10, fontSize: 16, background: '#722ed1' }} loading={loadingMachine} onClick={scan}>
          扫 机 台
        </Button>
      </div>

      {loadingMachine && <div style={{ textAlign: 'center', padding: 40 }}><Spin /></div>}

      {machine && (
        <div style={{ background: '#fff', borderRadius: 12, padding: '12px 14px', boxShadow: '0 1px 4px rgba(0,0,0,.06)' }}>
          <div style={{ fontSize: 15, fontWeight: 600, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span>机台 #{machine.equipment.machineNo ?? machine.equipment.id}</span>
            {machine.equipment.machineModel && <Tag>{machine.equipment.machineModel}</Tag>}
          </div>
          <div className="muted" style={{ fontSize: 12, marginTop: 4 }}>
            车间 {machine.workshopName ?? '-'}
            {machine.dutyUsers ? ` | 责任人 ${machine.dutyUsers}` : ''}
          </div>

          {unsolved.length > 0 && (
            <div style={{ marginTop: 10, background: '#fff7e6', border: '1px solid #ffd591', borderRadius: 8, padding: '8px 10px' }}>
              <div style={{ fontSize: 12, color: '#d46b08' }}>
                该机台有 {unsolved.length} 条未解决报修{unsolved.map((r) => ` ${r.repairCode ?? ''}`).join('、')}
              </div>
            </div>
          )}

          <div style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '10px 0 6px', borderBottom: '1px dashed #f0f0f0' }}>
            <span className="muted" style={{ fontSize: 13, width: 76, flexShrink: 0 }}>故障分类</span>
            <Select
              allowClear size="large" style={{ flex: 1, fontSize: 15 }} placeholder="选故障分类（选填）"
              showSearch optionFilterProp="label"
              options={faultTypes.map((f) => ({ label: f.faultName ?? f.faultCode ?? `#${f.id}`, value: f.id }))}
              value={faultTypeId} onChange={setFaultTypeId}
            />
          </div>
          <div style={{ padding: '10px 0' }}>
            <Input.TextArea
              rows={3} size="large" style={{ fontSize: 15 }}
              placeholder="故障详情（必填）：故障现象、发生部位等"
              value={detail} onChange={(e) => setDetail(e.target.value)}
            />
          </div>

          <Button
            type="primary" size="large" block icon={<SendOutlined />}
            style={{ height: 48, fontSize: 16, background: '#722ed1' }}
            loading={submitting} disabled={!canSave} onClick={submit}
          >
            提交报修
          </Button>
        </div>
      )}
    </>
  )
}

/** 维修处理面板：未解决报修单列表 → 修复完成 */
function SolvePanel() {
  const { message } = App.useApp()
  const { hasPerm } = useAuth()
  const inputRef = useRef<InputRef>(null)
  const [keyword, setKeyword] = useState('')
  const [loading, setLoading] = useState(false)
  const [rows, setRows] = useState<PdaRepairRecord[] | null>(null)
  const [faultMap, setFaultMap] = useState<Record<number, string>>({})
  const [acting, setActing] = useState<number | null>(null)
  const canAct = hasPerm('equip:repairRecord:list')

  const loadFaults = () => {
    pdaFaultTypes().then((l) => {
      const m: Record<number, string> = {}
      l.forEach((f) => { m[f.id] = f.faultName ?? f.faultCode ?? `#${f.id}` })
      setFaultMap(m)
    }).catch(() => setFaultMap({}))
  }

  useEffect(() => { loadFaults() }, [])

  const search = async () => {
    setLoading(true)
    try {
      setRows(await pdaRepairs({ keyword: keyword.trim() || undefined }))
    } catch {
      setRows([])
    } finally {
      setLoading(false)
      inputRef.current?.focus()
    }
  }

  const solve = async (r: PdaRepairRecord) => {
    setActing(r.id)
    try {
      await pdaSolveRepair(r.id)
      message.success(`${r.repairCode ?? ''} 已修复`)
      search()
    } finally {
      setActing(null)
    }
  }

  return (
    <>
      <div style={{ background: '#fff', borderRadius: 12, padding: 12, marginBottom: 12, boxShadow: '0 1px 4px rgba(0,0,0,.06)' }}>
        <Input
          ref={inputRef} size="large" autoFocus
          prefix={<ScanOutlined style={{ color: '#722ed1', fontSize: 18 }} />}
          placeholder="扫/输维修编码（空=全部，未解决在前）"
          value={keyword} onChange={(e) => setKeyword(e.target.value)} onPressEnter={search} style={{ fontSize: 15 }}
        />
        <Button type="primary" size="large" block style={{ height: 46, marginTop: 10, fontSize: 16 }} loading={loading} onClick={search}>
          查 询
        </Button>
      </div>

      {loading && <div style={{ textAlign: 'center', padding: 40 }}><Spin /></div>}

      {rows?.map((r) => {
        const solved = !!r.solveTime
        return (
          <div key={r.id} style={{ background: '#fff', borderRadius: 12, padding: '10px 14px', marginBottom: 10, boxShadow: '0 1px 4px rgba(0,0,0,.06)' }}>
            <div style={{ fontSize: 13, fontWeight: 600, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <span>{r.repairCode ?? '-'}</span>
              <Tag color={solved ? 'success' : 'error'}>{solved ? '已解决' : '待维修'}</Tag>
            </div>
            <div className="muted" style={{ fontSize: 12, marginTop: 4 }}>
              机台 #{r.equipmentId ?? '-'} | {r.faultTypeId != null ? faultMap[r.faultTypeId] ?? r.faultTypeId : '未分类'} | 故障 {r.faultTime ?? '-'}
            </div>
            {r.detail && <div style={{ fontSize: 13, marginTop: 4 }}>{r.detail}</div>}
            {solved && (
              <div className="muted" style={{ fontSize: 12, marginTop: 2 }}>
                修复 {r.repairUser ?? '-'} {r.solveTime}
              </div>
            )}
            {!solved && (
              <Button
                type="primary" size="large" icon={<SafetyCertificateOutlined />}
                loading={acting === r.id} disabled={!canAct}
                style={{ height: 40, width: '100%', marginTop: 8, background: '#722ed1' }} onClick={() => solve(r)}
              >
                <ToolOutlined /> 修复完成
              </Button>
            )}
          </div>
        )
      })}

      {rows && rows.length === 0 && !loading && (
        <div className="muted" style={{ textAlign: 'center', fontSize: 13, padding: 30 }}>暂无报修单</div>
      )}
      {rows === null && !loading && (
        <div className="muted" style={{ textAlign: 'center', fontSize: 13, padding: 30 }}>
          <ToolOutlined style={{ fontSize: 22, color: '#722ed1' }} />
          <div style={{ marginTop: 6 }}>输入编码查询，或留空查看全部</div>
        </div>
      )}
    </>
  )
}
