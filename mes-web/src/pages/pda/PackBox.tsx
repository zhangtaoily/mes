import { useRef, useState } from 'react'
import { App, Button, Input, InputNumber, Popconfirm, Spin, Tag } from 'antd'
import type { InputRef } from 'antd'
import { ScanOutlined, SearchOutlined } from '@ant-design/icons'
import {
  pdaAddBox, pdaDeleteBox, pdaFinishExecute, pdaPackExecuteDetail, pdaPackExecutes,
} from '@/api/pda'
import type { PkExecuteDetail } from '@/api/pda'
import type { PkExecute } from '@/api/pack'
import { useAuth } from '@/store/auth'

function Row({ label, value }: { label: string; value?: React.ReactNode }) {
  return (
    <div style={{ display: 'flex', padding: '7px 0', fontSize: 13, borderBottom: '1px dashed #f0f0f0' }}>
      <span className="muted" style={{ width: 76, flexShrink: 0 }}>{label}</span>
      <span style={{ flex: 1, wordBreak: 'break-all' }}>{value ?? '-'}</span>
    </div>
  )
}

function Card({ title, extra, children }: { title: React.ReactNode; extra?: React.ReactNode; children: React.ReactNode }) {
  return (
    <div style={{ background: '#fff', borderRadius: 12, padding: '12px 14px', marginBottom: 12, boxShadow: '0 1px 4px rgba(0,0,0,.06)' }}>
      <div style={{ fontSize: 14, fontWeight: 600, marginBottom: 4, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <span>{title}</span>
        {extra}
      </div>
      {children}
    </div>
  )
}

/** PDA 装箱作业：查执行单 → 逐箱称重（净/皮自动算毛） → 删箱/了包 */
export default function PdaPackBox() {
  const { message, modal } = App.useApp()
  const { hasPerm } = useAuth()
  const inputRef = useRef<InputRef>(null)
  const [keyword, setKeyword] = useState('')
  const [searching, setSearching] = useState(false)
  const [list, setList] = useState<PkExecute[] | null>(null)
  const [detail, setDetail] = useState<PkExecuteDetail | null>(null)
  const [detailLoading, setDetailLoading] = useState(false)
  const [submitting, setSubmitting] = useState(false)

  // 称重表单：净/皮/毛三项，输任意两项自动算第三项
  const [netWeight, setNetWeight] = useState<number | null>(null)
  const [tareWeight, setTareWeight] = useState<number | null>(null)
  const [grossWeight, setGrossWeight] = useState<number | null>(null)
  const [actualTube, setActualTube] = useState<number | null>(null)
  const [grainCount, setGrainCount] = useState<number | null>(null)
  const [tieLineColor, setTieLineColor] = useState('')
  const [lastBoxCode, setLastBoxCode] = useState('')

  const canPack = hasPerm('pack:execute:list')

  const search = async () => {
    const kw = keyword.trim()
    if (!kw) return
    setSearching(true)
    try {
      setList(await pdaPackExecutes(kw))
    } catch {
      setList([])
    } finally {
      setSearching(false)
    }
  }

  const openDetail = async (id: number) => {
    setDetailLoading(true)
    try {
      setDetail(await pdaPackExecuteDetail(id))
      setList(null)
    } finally {
      setDetailLoading(false)
    }
  }

  const reload = async () => {
    if (!detail) return
    setDetail(await pdaPackExecuteDetail(detail.execute.id))
  }

  const changeNet = (v: number | null) => {
    setNetWeight(v)
    if (v != null && tareWeight != null) setGrossWeight(+(v + tareWeight).toFixed(2))
  }
  const changeTare = (v: number | null) => {
    setTareWeight(v)
    if (v != null && netWeight != null) setGrossWeight(+(netWeight + v).toFixed(2))
  }
  const changeGross = (v: number | null) => {
    setGrossWeight(v)
    if (v != null && netWeight != null) setTareWeight(+(v - netWeight).toFixed(2))
    else if (v != null && tareWeight != null) setNetWeight(+(v - tareWeight).toFixed(2))
  }

  const submitBox = async () => {
    if (!detail) return
    if (netWeight == null || tareWeight == null || grossWeight == null) {
      message.warning('请填写净重/皮重/毛重（任意两项自动算第三项）')
      return
    }
    setSubmitting(true)
    try {
      const box = await pdaAddBox({
        executeId: detail.execute.id,
        netWeight, tareWeight, grossWeight,
        actualTube: actualTube ?? undefined,
        grainCount: grainCount ?? undefined,
        tieLineColor: tieLineColor || undefined,
      })
      setLastBoxCode(box.boxCode ?? '')
      message.success(`已装箱：${box.boxCode}（公重 ${box.publicWeight}kg）`)
      // 连续称重：保留皮重/筒数，清净重待输
      setNetWeight(null)
      setGrossWeight(null)
      await reload()
    } finally {
      setSubmitting(false)
    }
  }

  const removeBox = async (boxId: number) => {
    await pdaDeleteBox(boxId)
    message.success('已删箱（审计留痕）')
    await reload()
  }

  const finish = () => {
    if (!detail) return
    modal.confirm({
      title: '确认了包？',
      content: '了包后不可再称重/删箱。',
      okText: '确认了包',
      cancelText: '取消',
      onOk: async () => {
        await pdaFinishExecute(detail.execute.id)
        message.success('已了包')
        await reload()
      },
    })
  }

  const e = detail?.execute
  const packed = e?.isPacked === 1

  return (
    <div style={{ padding: 14 }}>
      {!detail && (
        <div style={{ background: '#fff', borderRadius: 12, padding: 12, marginBottom: 12, boxShadow: '0 1px 4px rgba(0,0,0,.06)' }}>
          <Input
            ref={inputRef}
            size="large"
            autoFocus
            prefix={<SearchOutlined style={{ color: '#409eff', fontSize: 18 }} />}
            placeholder="扫/输执行单号、包装批号、入库批号"
            value={keyword}
            onChange={(ev) => setKeyword(ev.target.value)}
            onPressEnter={search}
            style={{ fontSize: 15 }}
          />
          <Button type="primary" size="large" block style={{ height: 46, marginTop: 10, fontSize: 16 }} loading={searching} onClick={search}>
            查 询
          </Button>
        </div>
      )}

      {searching && !list && (
        <div style={{ textAlign: 'center', padding: 40 }}><Spin /></div>
      )}

      {list && list.length === 0 && !searching && (
        <div className="muted" style={{ textAlign: 'center', padding: 30, fontSize: 13 }}>未找到匹配的包装执行单</div>
      )}

      {list?.map((it) => (
        <Card
          key={it.id}
          title={it.executeNo}
          extra={it.isPacked === 1 ? <Tag color="success">已了包</Tag> : <Tag color="processing">称重中</Tag>}
        >
          <Row label="包装批号" value={it.packBatchNo} />
          <Row label="入库批号" value={it.warehouseBatchNo} />
          <Row label="产品" value={it.productName} />
          <Row label="箱数" value={it.boxCount ?? 0} />
          <Button type="primary" block size="large" style={{ height: 44, marginTop: 10 }} onClick={() => openDetail(it.id)}>
            进入称重
          </Button>
        </Card>
      ))}

      {detailLoading && (
        <div style={{ textAlign: 'center', padding: 40 }}><Spin /></div>
      )}

      {detail && e && (
        <>
          <Card
            title={e.executeNo}
            extra={packed ? <Tag color="success">已了包</Tag> : <Tag color="processing">称重中</Tag>}
          >
            <Row label="包装批号" value={e.packBatchNo} />
            <Row label="入库批号" value={e.warehouseBatchNo} />
            <Row label="产品" value={e.productName} />
            <Row label="规格/色号" value={`${e.productSpec ?? '-'} / ${e.colorNo ?? '-'}`} />
            <Row label="缸号" value={e.dyeVatNo} />
            <Row label="回潮(标/实)" value={`${e.moistureStandard ?? '-'} / ${e.moistureActual ?? '-'}`} />
            <Row label="箱数/筒数" value={`${e.boxCount ?? 0} 箱 / ${e.sumTube ?? 0} 筒`} />
            <Row label="总净重" value={`${e.sumNetWeight ?? 0} kg`} />
            <Row label="总公重" value={`${e.sumPublicWeight ?? 0} kg`} />
            <Button block size="large" style={{ height: 40, marginTop: 10 }} onClick={() => { setDetail(null); setKeyword(''); setList(null) }}>
              换执行单
            </Button>
          </Card>

          {!packed && canPack && (
            <Card title="逐箱称重" extra={lastBoxCode ? <span className="muted" style={{ fontSize: 11 }}>上箱 {lastBoxCode}</span> : undefined}>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
                <div>
                  <div className="muted" style={{ fontSize: 12, marginBottom: 4 }}>净重(kg) *</div>
                  <InputNumber size="large" min={0} precision={2} style={{ width: '100%' }} value={netWeight} onChange={changeNet} />
                </div>
                <div>
                  <div className="muted" style={{ fontSize: 12, marginBottom: 4 }}>皮重(kg) *</div>
                  <InputNumber size="large" min={0} precision={2} style={{ width: '100%' }} value={tareWeight} onChange={changeTare} />
                </div>
                <div>
                  <div className="muted" style={{ fontSize: 12, marginBottom: 4 }}>毛重(kg) *</div>
                  <InputNumber size="large" min={0} precision={2} style={{ width: '100%' }} value={grossWeight} onChange={changeGross} />
                </div>
                <div>
                  <div className="muted" style={{ fontSize: 12, marginBottom: 4 }}>实际筒数</div>
                  <InputNumber size="large" min={0} precision={0} style={{ width: '100%' }} value={actualTube} onChange={setActualTube} />
                </div>
                <div>
                  <div className="muted" style={{ fontSize: 12, marginBottom: 4 }}>颗数</div>
                  <InputNumber size="large" min={0} precision={0} style={{ width: '100%' }} value={grainCount} onChange={setGrainCount} />
                </div>
                <div>
                  <div className="muted" style={{ fontSize: 12, marginBottom: 4 }}>扎线颜色</div>
                  <Input size="large" value={tieLineColor} onChange={(ev) => setTieLineColor(ev.target.value)} />
                </div>
              </div>
              <div className="muted" style={{ fontSize: 11, margin: '8px 0' }}>净/皮/毛输任意两项，第三项自动计算；公重按回潮公式换算</div>
              <Button type="primary" size="large" block style={{ height: 48, fontSize: 16 }} loading={submitting} onClick={submitBox}>
                装 箱
              </Button>
            </Card>
          )}

          <Card title={`箱子明细（${detail.boxes.length}）`}>
            {detail.boxes.length === 0 && (
              <div className="muted" style={{ textAlign: 'center', padding: '14px 0', fontSize: 12 }}>暂无箱子，称重后显示</div>
            )}
            {[...detail.boxes].reverse().map((b) => (
              <div key={b.id} style={{ display: 'flex', alignItems: 'center', padding: '8px 0', borderBottom: '1px dashed #f0f0f0', gap: 8 }}>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ fontSize: 13, fontWeight: 600 }}>
                    #{b.seqNo} {b.boxCode}
                    {b.palletCode && <Tag style={{ marginLeft: 6 }}>{b.palletCode}</Tag>}
                  </div>
                  <div className="muted" style={{ fontSize: 11, marginTop: 2 }}>
                    筒{b.actualTube ?? '-'} | 毛{b.grossWeight} 净{b.netWeight} 皮{b.tareWeight} | 公<b style={{ color: '#409eff' }}>{b.publicWeight}</b>kg
                  </div>
                </div>
                {!packed && canPack && (
                  <Popconfirm title="删箱将审计留痕并回退汇总，确认？" onConfirm={() => removeBox(b.id)}>
                    <Button size="small" danger>删箱</Button>
                  </Popconfirm>
                )}
              </div>
            ))}
          </Card>

          {!packed && canPack && detail.boxes.length > 0 && (
            <Button danger size="large" block style={{ height: 48, fontSize: 16 }} onClick={finish}>
              了 包
            </Button>
          )}
        </>
      )}
    </div>
  )
}
