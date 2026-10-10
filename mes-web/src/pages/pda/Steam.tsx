import { useRef, useState } from 'react'
import { App, Button, Input, InputNumber, Spin, Tabs, Tag } from 'antd'
import type { InputRef } from 'antd'
import { PrinterOutlined, SaveOutlined, ScanOutlined, SearchOutlined } from '@ant-design/icons'
import { pdaPrintSteam, pdaReceiveSteam, pdaSaveSteam, pdaSteams } from '@/api/pda'
import type { RepYarnSteam } from '@/api/site'
import { useAuth } from '@/store/auth'

/** 蒸纱状态由打印/接收时间推导（与后端闭环校验同源：未打印不可接收） */
function steamState(r: RepYarnSteam): { label: string; color: string; canPrint: boolean; canReceive: boolean } {
  if (r.receiveTime) return { label: '已接收', color: 'success', canPrint: false, canReceive: false }
  if (r.printTime) return { label: '已打印', color: 'processing', canPrint: false, canReceive: true }
  return { label: '未打印', color: 'default', canPrint: true, canReceive: false }
}

/** PDA 蒸纱作业：登记（车号/批号/粒数/重量）+ 打印/接收闭环 */
export default function PdaSteam() {
  return (
    <div style={{ padding: 14 }}>
      <Tabs
        items={[
          { key: 'new', label: '登记', children: <NewPanel /> },
          { key: 'loop', label: '打印/接收', children: <LoopPanel /> },
        ]}
      />
    </div>
  )
}

/** 登记蒸纱信息（车号必填同 PC） */
function NewPanel() {
  const { message } = App.useApp()
  const { hasPerm } = useAuth()
  const firstRef = useRef<InputRef>(null)
  const [truckNo, setTruckNo] = useState('')
  const [batchNo, setBatchNo] = useState('')
  const [grainCount, setGrainCount] = useState<number | null>(null)
  const [weight, setWeight] = useState<number | null>(null)
  const [truckCount, setTruckCount] = useState<number | null>(1)
  const [submitting, setSubmitting] = useState(false)
  const canSave = hasPerm('site:steam:list')

  const submit = async () => {
    if (!truckNo.trim()) {
      message.warning('请输入车号')
      firstRef.current?.focus()
      return
    }
    if (!batchNo.trim()) {
      message.warning('请输入生产批号')
      return
    }
    if (grainCount == null || weight == null) {
      message.warning('请输入粒数与重量')
      return
    }
    setSubmitting(true)
    try {
      await pdaSaveSteam({
        truckNo: truckNo.trim(),
        batchNo: batchNo.trim(),
        grainCount,
        weight,
        truckCount: truckCount ?? 1,
      })
      message.success('蒸纱信息已登记')
      setTruckNo(''); setBatchNo(''); setGrainCount(null); setWeight(null); setTruckCount(1)
      firstRef.current?.focus()
    } finally {
      setSubmitting(false)
    }
  }

  const field = (label: string, node: React.ReactNode) => (
    <div style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '6px 0', borderBottom: '1px dashed #f0f0f0' }}>
      <span className="muted" style={{ fontSize: 13, width: 76, flexShrink: 0 }}>{label}</span>
      <div style={{ flex: 1 }}>{node}</div>
    </div>
  )

  return (
    <div style={{ background: '#fff', borderRadius: 12, padding: '12px 14px', boxShadow: '0 1px 4px rgba(0,0,0,.06)' }}>
      {field('车号', (
        <Input
          ref={firstRef} size="large" prefix={<ScanOutlined style={{ color: '#409eff', fontSize: 16 }} />}
          placeholder="扫/输如 TC001" value={truckNo} onChange={(e) => setTruckNo(e.target.value)} style={{ fontSize: 15 }}
        />
      ))}
      {field('生产批号', <Input size="large" value={batchNo} onChange={(e) => setBatchNo(e.target.value)} style={{ fontSize: 15 }} />)}
      {field('粒数', <InputNumber size="large" min={0} precision={0} style={{ width: '100%' }} value={grainCount} onChange={setGrainCount} />)}
      {field('重量(kg)', <InputNumber size="large" min={0} precision={2} style={{ width: '100%' }} value={weight} onChange={setWeight} />)}
      {field('车数', <InputNumber size="large" min={1} precision={0} style={{ width: '100%' }} value={truckCount} onChange={setTruckCount} />)}
      <Button
        type="primary" size="large" block icon={<SaveOutlined />}
        style={{ height: 48, fontSize: 16, marginTop: 12 }} loading={submitting} disabled={!canSave} onClick={submit}
      >
        登记蒸纱信息
      </Button>
    </div>
  )
}

/** 打印/接收闭环：未打印在前、未接收在前 */
function LoopPanel() {
  const { message } = App.useApp()
  const { hasPerm } = useAuth()
  const inputRef = useRef<InputRef>(null)
  const [keyword, setKeyword] = useState('')
  const [loading, setLoading] = useState(false)
  const [rows, setRows] = useState<RepYarnSteam[] | null>(null)
  const [acting, setActing] = useState<number | null>(null)
  const canAct = hasPerm('site:steam:list')

  const search = async () => {
    setLoading(true)
    try {
      setRows(await pdaSteams(keyword.trim() || undefined))
    } catch {
      setRows([])
    } finally {
      setLoading(false)
      inputRef.current?.focus()
    }
  }

  const act = async (r: RepYarnSteam, action: 'print' | 'receive') => {
    setActing(r.id)
    try {
      if (action === 'print') {
        await pdaPrintSteam(r.id)
        message.success('已打印')
      } else {
        await pdaReceiveSteam(r.id)
        message.success('已接收')
      }
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
          prefix={<SearchOutlined style={{ color: '#409eff', fontSize: 18 }} />}
          placeholder="扫/输车号、生产批号"
          value={keyword} onChange={(e) => setKeyword(e.target.value)} onPressEnter={search} style={{ fontSize: 15 }}
        />
        <Button type="primary" size="large" block style={{ height: 46, marginTop: 10, fontSize: 16 }} loading={loading} onClick={search}>
          查 询
        </Button>
      </div>

      {loading && <div style={{ textAlign: 'center', padding: 40 }}><Spin /></div>}

      {rows?.map((r) => {
        const st = steamState(r)
        return (
          <div key={r.id} style={{ background: '#fff', borderRadius: 12, padding: '10px 14px', marginBottom: 10, boxShadow: '0 1px 4px rgba(0,0,0,.06)' }}>
            <div style={{ fontSize: 13, fontWeight: 600, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <span>{r.truckNo ?? '-'}</span>
              <Tag color={st.color}>{st.label}</Tag>
            </div>
            <div className="muted" style={{ fontSize: 12, marginTop: 4 }}>
              批号 {r.batchNo ?? '-'} | 粒数 {r.grainCount ?? '-'} | 重量 {r.weight ?? '-'}kg | 车数 {r.truckCount ?? '-'}
            </div>
            {(r.printTime || r.receiveTime) && (
              <div className="muted" style={{ fontSize: 12, marginTop: 2 }}>
                {r.printTime ? `打印 ${r.printUser ?? '-'} ${r.printTime}` : ''}
                {r.receiveTime ? ` | 接收 ${r.receiveUser ?? '-'} ${r.receiveTime}` : ''}
              </div>
            )}
            {(st.canPrint || st.canReceive) && (
              <div style={{ display: 'flex', gap: 8, marginTop: 8 }}>
                {st.canPrint && (
                  <Button
                    type="primary" size="large" icon={<PrinterOutlined />} loading={acting === r.id}
                    disabled={!canAct} style={{ height: 40, flex: 1 }} onClick={() => act(r, 'print')}
                  >
                    打印
                  </Button>
                )}
                {st.canReceive && (
                  <Button
                    type="primary" size="large" loading={acting === r.id}
                    disabled={!canAct} style={{ height: 40, flex: 1 }} onClick={() => act(r, 'receive')}
                  >
                    接收
                  </Button>
                )}
              </div>
            )}
          </div>
        )
      })}

      {rows && rows.length === 0 && !loading && (
        <div className="muted" style={{ textAlign: 'center', padding: 30, fontSize: 13 }}>未找到匹配的蒸纱信息</div>
      )}
    </>
  )
}
