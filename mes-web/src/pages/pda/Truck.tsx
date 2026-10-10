import { useEffect, useRef, useState } from 'react'
import { App, Button, Input, InputNumber, Segmented, Select, Spin, Tabs, Tag } from 'antd'
import type { InputRef } from 'antd'
import { CarOutlined, PlusOutlined, ScanOutlined, SendOutlined } from '@ant-design/icons'
import { pdaMoveTruck, pdaSaveTruck, pdaTruckDetail, pdaTrucks, pdaWorkshops } from '@/api/pda'
import type { TruckDetail } from '@/api/pda'
import type { RepTruck } from '@/api/site'
import type { BaseWorkshop } from '@/api/plan'
import { useAuth } from '@/store/auth'

const OP_NAMES = ['绑定', '称重', '入库', '移库', '出库'] as const
type OpName = (typeof OP_NAMES)[number]

function Row({ label, value }: { label: string; value?: React.ReactNode }) {
  return (
    <div style={{ display: 'flex', padding: '5px 0', fontSize: 13, borderBottom: '1px dashed #f0f0f0' }}>
      <span className="muted" style={{ width: 76, flexShrink: 0 }}>{label}</span>
      <span style={{ flex: 1, wordBreak: 'break-all' }}>{value ?? '-'}</span>
    </div>
  )
}

/** PDA 毛球周转车：扫车号 → 车辆信息 → 移动操作（绑定/称重/入库/移库/出库）+ 轨迹查看 */
export default function PdaTruck() {
  const { message } = App.useApp()
  const inputRef = useRef<InputRef>(null)
  const [keyword, setKeyword] = useState('')
  const [loading, setLoading] = useState(false)
  const [trucks, setTrucks] = useState<RepTruck[] | null>(null)
  const [truck, setTruck] = useState<RepTruck | null>(null)
  const [detail, setDetail] = useState<TruckDetail | null>(null)
  const [notFound, setNotFound] = useState(false)

  const search = async (code: string) => {
    const c = code.trim()
    if (!c) return
    setLoading(true)
    setNotFound(false)
    try {
      const rows = await pdaTrucks(c)
      setTrucks(rows)
      if (rows.length === 1) {
        setTruck(rows[0])
        setDetail(await pdaTruckDetail(rows[0].id).catch(() => null))
      } else if (rows.length === 0) {
        setNotFound(true)
      }
    } catch {
      setTrucks([])
      setNotFound(true)
    } finally {
      setLoading(false)
      setKeyword('')
      inputRef.current?.focus()
    }
  }

  const pickTruck = async (t: RepTruck) => {
    setTruck(t)
    setTrucks(null)
    setDetail(await pdaTruckDetail(t.id).catch(() => null))
  }

  const refresh = async () => {
    if (!truck) return
    const d = await pdaTruckDetail(truck.id).catch(() => null)
    setDetail(d)
    if (d) setTruck(d.truck)
  }

  return (
    <div style={{ padding: 14 }}>
      <div style={{ background: '#fff', borderRadius: 12, padding: 12, marginBottom: 12, boxShadow: '0 1px 4px rgba(0,0,0,.06)' }}>
        <Input
          ref={inputRef} size="large" autoFocus
          prefix={<ScanOutlined style={{ color: '#409eff', fontSize: 18 }} />}
          placeholder="扫/输周转车车号，如 TC001"
          value={keyword} onChange={(e) => setKeyword(e.target.value)} onPressEnter={() => search(keyword)} style={{ fontSize: 15 }}
        />
        <Button type="primary" size="large" block style={{ height: 46, marginTop: 10, fontSize: 16 }} loading={loading} onClick={() => search(keyword)}>
          查 询
        </Button>
      </div>

      {loading && <div style={{ textAlign: 'center', padding: 40 }}><Spin /></div>}

      {/* 多车匹配选择 */}
      {!loading && !truck && trucks && trucks.length > 0 && trucks.map((t) => (
        <div
          key={t.id} onClick={() => pickTruck(t)}
          style={{ background: '#fff', borderRadius: 12, padding: '10px 14px', marginBottom: 10, boxShadow: '0 1px 4px rgba(0,0,0,.06)', cursor: 'pointer' }}
        >
          <div style={{ fontSize: 13, fontWeight: 600, display: 'flex', justifyContent: 'space-between' }}>
            <span>{t.truckNo}</span>
            {t.status && <Tag>{t.status}</Tag>}
          </div>
          <div className="muted" style={{ fontSize: 12, marginTop: 4 }}>
            批号 {t.batchNo ?? '-'} | 库位 {t.location ?? '-'} | 毛球 {t.ballCount ?? 0} | 净重 {t.netWeight ?? 0}kg
          </div>
        </div>
      ))}

      {/* 未找到 → 现场登记空车 */}
      {!loading && !truck && notFound && <NewTruckPanel scanNo={keyword} onCreated={() => { setNotFound(false) }} />}

      {/* 选中车辆 */}
      {!loading && truck && (
        <div style={{ background: '#fff', borderRadius: 12, padding: '12px 14px', boxShadow: '0 1px 4px rgba(0,0,0,.06)' }}>
          <div style={{ fontSize: 15, fontWeight: 700, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span><CarOutlined style={{ color: '#409eff', marginRight: 6 }} />{truck.truckNo}</span>
            {truck.status && <Tag color={truck.status === '出库' ? 'default' : 'processing'}>{truck.status}</Tag>}
          </div>
          <div style={{ marginTop: 4 }}>
            <Row label="批号" value={truck.batchNo} />
            <Row label="原料" value={truck.rawMaterial} />
            <Row label="库位" value={truck.location} />
            <Row label="袋色" value={truck.bagColor} />
            <Row label="毛球数" value={truck.ballCount} />
            <Row label="毛/净/标重" value={`${truck.grossWeight ?? '-'} / ${truck.netWeight ?? '-'} / ${truck.standardWeight ?? '-'} kg`} />
            <Row label="回潮(标/实)" value={`${truck.moistureStandard ?? '-'} / ${truck.moistureActual ?? '-'}`} />
          </div>
          <Tabs
            items={[
              { key: 'move', label: '移动操作', children: <MovePanel truck={truck} onMoved={refresh} /> },
              { key: 'trace', label: '移动轨迹', children: <TracePanel detail={detail} /> },
            ]}
          />
          <Button block size="large" style={{ height: 40, marginTop: 4 }} onClick={() => { setTruck(null); setTrucks(null); setDetail(null); setNotFound(false); inputRef.current?.focus() }}>
            换车扫描
          </Button>
        </div>
      )}
    </div>
  )
}

/** 移动操作表单：按操作类型切换字段（scanWorkshop 后端按车辆车间自动补齐） */
function MovePanel({ truck, onMoved }: { truck: RepTruck; onMoved: () => void }) {
  const { message } = App.useApp()
  const { hasPerm } = useAuth()
  const [opName, setOpName] = useState<OpName>('称重')
  const [toLocation, setToLocation] = useState('')
  const [recombedBatchNo, setRecombedBatchNo] = useState('')
  const [spinBatchNo, setSpinBatchNo] = useState('')
  const [bagColor, setBagColor] = useState('')
  const [ballCount, setBallCount] = useState<number | null>(null)
  const [grossWeight, setGrossWeight] = useState<number | null>(null)
  const [netWeight, setNetWeight] = useState<number | null>(null)
  const [standardWeight, setStandardWeight] = useState<number | null>(null)
  const [moistureStandard, setMoistureStandard] = useState<number | null>(null)
  const [moistureActual, setMoistureActual] = useState<number | null>(null)
  const [moveNetWeight, setMoveNetWeight] = useState<number | null>(null)
  const [moveBallCount, setMoveBallCount] = useState<number | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const canMove = hasPerm('site:truck:list')

  const submit = async () => {
    if (opName === '移库' || opName === '入库' || opName === '出库') {
      if (!toLocation.trim()) {
        message.warning('请输入目标库位')
        return
      }
    }
    setSubmitting(true)
    try {
      await pdaMoveTruck({
        truckId: truck.id,
        opName,
        toLocation: toLocation.trim() || undefined,
        recombedBatchNo: opName === '绑定' ? (recombedBatchNo.trim() || undefined) : undefined,
        spinBatchNo: opName === '绑定' ? (spinBatchNo.trim() || undefined) : undefined,
        bagColor: opName === '绑定' ? (bagColor.trim() || undefined) : undefined,
        ballCount: ballCount ?? undefined,
        grossWeight: opName === '称重' ? (grossWeight ?? undefined) : undefined,
        netWeight: netWeight ?? undefined,
        standardWeight: opName === '称重' ? (standardWeight ?? undefined) : undefined,
        moistureStandard: opName === '称重' ? (moistureStandard ?? undefined) : undefined,
        moistureActual: opName === '称重' ? (moistureActual ?? undefined) : undefined,
        moveNetWeight: opName === '称重' || opName === '绑定' ? undefined : (moveNetWeight ?? undefined),
        moveBallCount: opName === '称重' || opName === '绑定' ? undefined : (moveBallCount ?? undefined),
      })
      message.success(`已${opName}：${truck.truckNo}`)
      setToLocation(''); setRecombedBatchNo(''); setSpinBatchNo(''); setBagColor('')
      setBallCount(null); setGrossWeight(null); setNetWeight(null); setStandardWeight(null)
      setMoistureStandard(null); setMoistureActual(null); setMoveNetWeight(null); setMoveBallCount(null)
      onMoved()
    } finally {
      setSubmitting(false)
    }
  }

  const num = (label: string, value: number | null, set: (v: number | null) => void, placeholder?: string) => (
    <div style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '6px 0', borderBottom: '1px dashed #f0f0f0' }}>
      <span className="muted" style={{ fontSize: 13, width: 90, flexShrink: 0 }}>{label}</span>
      <InputNumber size="large" min={0} precision={2} style={{ flex: 1 }} placeholder={placeholder ?? '0'} value={value} onChange={set} />
    </div>
  )
  const txt = (label: string, value: string, set: (v: string) => void, placeholder?: string) => (
    <div style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '6px 0', borderBottom: '1px dashed #f0f0f0' }}>
      <span className="muted" style={{ fontSize: 13, width: 90, flexShrink: 0 }}>{label}</span>
      <Input size="large" placeholder={placeholder ?? ''} value={value} onChange={(e) => set(e.target.value)} />
    </div>
  )

  return (
    <>
      <Segmented
        block size="large" style={{ margin: '8px 0 10px' }}
        options={OP_NAMES as unknown as string[]}
        value={opName} onChange={(v) => setOpName(v as OpName)}
      />
      {opName === '绑定' && (
        <>
          {txt('复精梳批号', recombedBatchNo, setRecombedBatchNo, '绑定毛球批号')}
          {txt('纺纱批号', spinBatchNo, setSpinBatchNo)}
          {txt('袋色', bagColor, setBagColor)}
        </>
      )}
      {(opName === '移库' || opName === '入库' || opName === '出库') && txt('目标库位', toLocation, setToLocation, '如 A区-01')}
      {num('毛球数', ballCount, setBallCount, opName === '绑定' ? '本车毛球数' : '称得毛球数')}
      {opName === '称重' && (
        <>
          {num('毛重(kg)', grossWeight, setGrossWeight)}
          {num('标重(kg)', standardWeight, setStandardWeight)}
          {num('回潮(标准)', moistureStandard, setMoistureStandard)}
          {num('回潮(实际)', moistureActual, setMoistureActual)}
        </>
      )}
      {num('净重(kg)', netWeight, setNetWeight)}
      {(opName === '移库' || opName === '入库' || opName === '出库') && (
        <>
          {num('移动净重', moveNetWeight, setMoveNetWeight, '本次移动量')}
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '6px 0', borderBottom: '1px dashed #f0f0f0' }}>
            <span className="muted" style={{ fontSize: 13, width: 90, flexShrink: 0 }}>移动球数</span>
            <InputNumber size="large" min={0} precision={0} style={{ flex: 1 }} placeholder="0" value={moveBallCount} onChange={setMoveBallCount} />
          </div>
        </>
      )}
      <Button
        type="primary" size="large" block icon={<SendOutlined />}
        style={{ height: 48, fontSize: 16, marginTop: 12 }} loading={submitting} disabled={!canMove} onClick={submit}
      >
        确认{opName}
      </Button>
    </>
  )
}

/** 移动轨迹（最近在前） */
function TracePanel({ detail }: { detail: TruckDetail | null }) {
  if (!detail) return <div className="muted" style={{ textAlign: 'center', padding: 24, fontSize: 12 }}>暂无轨迹</div>
  if (!detail.moves.length) return <div className="muted" style={{ textAlign: 'center', padding: 24, fontSize: 12 }}>该车暂无移动轨迹</div>
  return (
    <div>
      {detail.moves.map((m) => (
        <div key={m.id} style={{ padding: '6px 0', borderBottom: '1px dashed #f0f0f0', fontSize: 12 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', fontWeight: 600, fontSize: 13 }}>
            <span>{m.opName ?? '-'}</span>
            <span className="muted">{m.createTime ?? ''}</span>
          </div>
          <div className="muted" style={{ marginTop: 2 }}>
            {m.fromLocation ?? '-'} → {m.toLocation ?? '-'}
            {m.moveNetWeight != null ? ` | 移动净重 ${m.moveNetWeight}kg` : ''}
            {m.moveBallCount != null ? ` | 移动球数 ${m.moveBallCount}` : ''}
            {m.isFirst === 1 ? ' | 首车' : ''}{m.isFinished === 1 ? ' | 末车' : ''}
          </div>
        </div>
      ))}
    </div>
  )
}

/** 未找到车辆时：现场登记空车（车号唯一校验由后端同源拦截） */
function NewTruckPanel({ scanNo, onCreated }: { scanNo: string; onCreated: () => void }) {
  const { message } = App.useApp()
  const { hasPerm } = useAuth()
  const [truckNo, setTruckNo] = useState('')
  const [workshops, setWorkshops] = useState<BaseWorkshop[]>([])
  const [workshopId, setWorkshopId] = useState<number | undefined>()
  const [location, setLocation] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const canSave = hasPerm('site:truck:list')

  useEffect(() => {
    setTruckNo(scanNo)
    pdaWorkshops().then(setWorkshops).catch(() => setWorkshops([]))
  }, [scanNo])

  const submit = async () => {
    if (!truckNo.trim()) {
      message.warning('请输入车号')
      return
    }
    setSubmitting(true)
    try {
      await pdaSaveTruck({ truckNo: truckNo.trim(), workshopId, location: location.trim() || undefined })
      message.success(`空车已登记：${truckNo.trim()}`)
      onCreated()
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div style={{ background: '#fff', borderRadius: 12, padding: '12px 14px', boxShadow: '0 1px 4px rgba(0,0,0,.06)' }}>
      <div style={{ fontSize: 14, fontWeight: 600, marginBottom: 6 }}>
        <PlusOutlined style={{ color: '#52c41a', marginRight: 6 }} />未找到车辆，登记空车
      </div>
      <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 10 }}>
        <span className="muted" style={{ fontSize: 13, width: 60, flexShrink: 0 }}>车号</span>
        <Input size="large" value={truckNo} onChange={(e) => setTruckNo(e.target.value)} placeholder="如 TC001" style={{ fontSize: 15 }} />
      </div>
      <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 10 }}>
        <span className="muted" style={{ fontSize: 13, width: 60, flexShrink: 0 }}>车间</span>
        <Select
          size="large" style={{ flex: 1 }} placeholder="选择车间" showSearch optionFilterProp="label" allowClear
          options={workshops.map((w) => ({ label: w.workshopName, value: w.id }))}
          value={workshopId} onChange={setWorkshopId}
        />
      </div>
      <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 10 }}>
        <span className="muted" style={{ fontSize: 13, width: 60, flexShrink: 0 }}>库位</span>
        <Input size="large" value={location} onChange={(e) => setLocation(e.target.value)} placeholder="选填" />
      </div>
      <Button type="primary" size="large" block style={{ height: 46, fontSize: 16 }} loading={submitting} disabled={!canSave} onClick={submit}>
        登记空车
      </Button>
    </div>
  )
}
