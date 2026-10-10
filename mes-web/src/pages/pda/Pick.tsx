import { useEffect, useRef, useState } from 'react'
import { App, Button, Checkbox, Input, Segmented, Spin, Tag } from 'antd'
import type { InputRef } from 'antd'
import { CheckOutlined, SearchOutlined } from '@ant-design/icons'
import { pdaConfirmPicks, pdaPicks, pdaWorkshops } from '@/api/pda'
import type { MtPick } from '@/api/prod'
import { useAuth } from '@/store/auth'

/** PDA 领退料确认：查待确认单据（扫单号/订单号/批号）→ 勾选 → 批量确认（确认人/时间回填） */
export default function PdaPick() {
  const { message, modal } = App.useApp()
  const { hasPerm } = useAuth()
  const inputRef = useRef<InputRef>(null)
  const [keyword, setKeyword] = useState('')
  const [docStatus, setDocStatus] = useState<string>('待确认')
  const [loading, setLoading] = useState(false)
  const [rows, setRows] = useState<MtPick[] | null>(null)
  const [checked, setChecked] = useState<Set<number>>(new Set())
  const [confirming, setConfirming] = useState(false)
  const [wsName, setWsName] = useState<Record<number, string>>({})
  const canConfirm = hasPerm('prod:feed:list')

  useEffect(() => {
    pdaWorkshops().then((ws) => {
      setWsName(Object.fromEntries(ws.map((w) => [w.id, w.workshopName])))
    }).catch(() => setWsName({}))
  }, [])

  const search = async (status = docStatus) => {
    setLoading(true)
    setChecked(new Set())
    try {
      setRows(await pdaPicks(keyword.trim() || undefined, status || undefined))
    } catch {
      setRows([])
    } finally {
      setLoading(false)
      inputRef.current?.focus()
    }
  }

  useEffect(() => { search() }, [docStatus]) // eslint-disable-line react-hooks/exhaustive-deps

  const toggle = (id: number) => {
    setChecked((prev) => {
      const next = new Set(prev)
      if (next.has(id)) next.delete(id)
      else next.add(id)
      return next
    })
  }

  const confirm = () => {
    if (checked.size === 0) return
    modal.confirm({
      title: '批量确认领退料单？',
      content: `已选 ${checked.size} 条。确认后回填确认人/时间，不可撤销。`,
      okText: '确 认',
      cancelText: '取消',
      onOk: async () => {
        setConfirming(true)
        try {
          await pdaConfirmPicks([...checked])
          message.success(`已确认 ${checked.size} 条`)
          search()
        } finally {
          setConfirming(false)
        }
      },
    })
  }

  const waiting = rows?.filter((r) => r.docStatus === '待确认') ?? []

  return (
    <div style={{ padding: 14 }}>
      <div style={{ background: '#fff', borderRadius: 12, padding: 12, marginBottom: 12, boxShadow: '0 1px 4px rgba(0,0,0,.06)' }}>
        <Input
          ref={inputRef} size="large" autoFocus
          prefix={<SearchOutlined style={{ color: '#409eff', fontSize: 18 }} />}
          placeholder="扫/输领退料单号、订单号、批号"
          value={keyword} onChange={(e) => setKeyword(e.target.value)}
          onPressEnter={() => search()} style={{ fontSize: 15 }}
        />
        <Button type="primary" size="large" block style={{ height: 46, marginTop: 10, fontSize: 16 }} loading={loading} onClick={() => search()}>
          查 询
        </Button>
        <Segmented
          block size="large" style={{ marginTop: 10 }}
          value={docStatus}
          options={[
            { label: '待确认', value: '待确认' },
            { label: '已确认', value: '已确认' },
            { label: '全部', value: '' },
          ]}
          onChange={(v) => setDocStatus(v as string)}
        />
      </div>

      {loading && <div style={{ textAlign: 'center', padding: 40 }}><Spin /></div>}

      {rows?.map((r) => (
        <div
          key={r.id}
          style={{ background: '#fff', borderRadius: 12, padding: '10px 14px', marginBottom: 10, boxShadow: '0 1px 4px rgba(0,0,0,.06)' }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            {r.docStatus === '待确认' && (
              <Checkbox checked={checked.has(r.id)} onChange={() => toggle(r.id)} style={{ marginRight: 2 }} />
            )}
            <span style={{ fontSize: 13, fontWeight: 600, flex: 1, wordBreak: 'break-all' }}>{r.pickNo ?? '-'}</span>
            <Tag color={r.pickType === 1 ? 'blue' : 'orange'}>{r.pickType === 1 ? '领料' : r.pickType === 2 ? '退料' : '-'}</Tag>
            <Tag color={r.docStatus === '已确认' ? 'success' : 'processing'}>{r.docStatus}</Tag>
          </div>
          <div className="muted" style={{ fontSize: 12, marginTop: 4, paddingLeft: r.docStatus === '待确认' ? 30 : 0 }}>
            {r.materialName ?? '-'} {r.materialSpec ? `(${r.materialSpec})` : ''} | {r.qty ?? '-'}{r.unit ?? ''}
          </div>
          <div className="muted" style={{ fontSize: 12, marginTop: 2, paddingLeft: r.docStatus === '待确认' ? 30 : 0 }}>
            批号 {r.batchNo ?? '-'} | 订单 {r.orderNo ?? '-'} | {wsName[r.workshopId ?? -1] ?? r.workshopId ?? '-'}
            {r.docStatus === '已确认' ? ` | 确认人 ${r.picker ?? '-'}` : ''}
          </div>
        </div>
      ))}

      {rows && rows.length === 0 && !loading && (
        <div className="muted" style={{ textAlign: 'center', padding: 30, fontSize: 13 }}>未找到匹配的领退料单</div>
      )}

      {waiting.length > 0 && (
        <div style={{ position: 'sticky', bottom: 10, paddingTop: 6 }}>
          <Button
            type="primary" size="large" block icon={<CheckOutlined />}
            style={{ height: 50, fontSize: 16, boxShadow: '0 4px 12px rgba(64,158,255,.4)' }}
            disabled={!canConfirm || checked.size === 0} loading={confirming} onClick={confirm}
          >
            批量确认（{checked.size}/{waiting.length}）
          </Button>
        </div>
      )}
    </div>
  )
}
