import { useRef, useState } from 'react'
import { App, Button, Input, InputNumber, Spin, Tag } from 'antd'
import type { InputRef } from 'antd'
import { ScanOutlined } from '@ant-design/icons'
import { pdaPalletize, pdaScanBox } from '@/api/pda'
import type { PdaBoxScanVO } from '@/api/pda'
import { useAuth } from '@/store/auth'
import dayjs from 'dayjs'

/** PDA 组托作业：逐箱扫码加入清单 → 组托取号 TP */
export default function PdaPallet() {
  const { message, modal } = App.useApp()
  const { hasPerm } = useAuth()
  const inputRef = useRef<InputRef>(null)
  const [content, setContent] = useState('')
  const [scanning, setScanning] = useState(false)
  const [items, setItems] = useState<PdaBoxScanVO[]>([])
  const [standardTube, setStandardTube] = useState<number | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [palletCode, setPalletCode] = useState('')

  const canPallet = hasPerm('pack:pallet:list')
  const execute = items[0]?.execute

  const scan = async (code: string) => {
    const c = code.trim()
    if (!c) return
    if (items.some((it) => it.box.boxCode === c)) {
      message.warning(`箱子 ${c} 已在清单中`)
      clearInput()
      return
    }
    if (execute && items.some((it) => it.box.pkExecuteId !== items[0].box.pkExecuteId)) {
      message.warning('清单中存在不同执行单的箱子')
      clearInput()
      return
    }
    setScanning(true)
    try {
      const vo = await pdaScanBox(c)
      if (items.length > 0 && vo.box.pkExecuteId !== items[0].box.pkExecuteId) {
        message.warning(`箱子属于执行单 ${vo.execute.executeNo}，与清单不一致，未加入`)
      } else {
        setItems((prev) => [...prev, vo])
        message.success(`已加入：${vo.box.boxCode}`)
      }
    } finally {
      setScanning(false)
      clearInput()
    }
  }

  const clearInput = () => {
    setContent('')
    inputRef.current?.focus()
  }

  const remove = (boxCode: string) => {
    setItems((prev) => prev.filter((it) => it.box.boxCode !== boxCode))
  }

  const submit = () => {
    if (!items.length) return
    modal.confirm({
      title: `确认组托 ${items.length} 箱？`,
      content: '组托后箱子将绑定新托号，不可重复组托。',
      okText: '确认组托',
      cancelText: '取消',
      onOk: async () => {
        setSubmitting(true)
        try {
          const code = await pdaPalletize({
            boxCodes: items.map((it) => it.box.boxCode!),
            standardTube: standardTube ?? undefined,
          })
          setPalletCode(code)
          setItems([])
          setStandardTube(null)
          message.success(`组托成功：${code}`)
        } finally {
          setSubmitting(false)
        }
      },
    })
  }

  return (
    <div style={{ padding: 14 }}>
      <div style={{ background: '#fff', borderRadius: 12, padding: 12, marginBottom: 12, boxShadow: '0 1px 4px rgba(0,0,0,.06)' }}>
        <Input
          ref={inputRef}
          size="large"
          autoFocus
          prefix={<ScanOutlined style={{ color: '#409eff', fontSize: 18 }} />}
          placeholder="扫箱子编码 BX..."
          value={content}
          onChange={(e) => setContent(e.target.value)}
          onPressEnter={() => scan(content)}
          style={{ fontSize: 15 }}
          disabled={!canPallet}
        />
        <Button
          type="primary" size="large" block style={{ height: 46, marginTop: 10, fontSize: 16 }}
          loading={scanning} onClick={() => scan(content)}
        >
          扫 箱
        </Button>
      </div>

      {scanning && !items.length && (
        <div style={{ textAlign: 'center', padding: 40 }}><Spin /></div>
      )}

      {palletCode && (
        <div style={{ background: '#fff', borderRadius: 12, padding: '16px 14px', marginBottom: 12, textAlign: 'center', boxShadow: '0 1px 4px rgba(0,0,0,.06)' }}>
          <div className="muted" style={{ fontSize: 12 }}>组托成功，托号</div>
          <div style={{ fontSize: 24, fontWeight: 700, color: '#52c41a', margin: '6px 0' }}>{palletCode}</div>
          <Button size="small" onClick={() => setPalletCode('')}>知道了</Button>
        </div>
      )}

      {execute && (
        <div style={{ background: '#fff', borderRadius: 12, padding: '10px 14px', marginBottom: 12, boxShadow: '0 1px 4px rgba(0,0,0,.06)' }}>
          <div style={{ fontSize: 13, fontWeight: 600 }}>
            {execute.executeNo} <Tag color="processing" style={{ marginLeft: 6 }}>{execute.isPacked === 1 ? '已了包' : '称重中'}</Tag>
          </div>
          <div className="muted" style={{ fontSize: 12, marginTop: 4 }}>
            {execute.productName} | 入库批 {execute.warehouseBatchNo} | 缸号 {execute.dyeVatNo ?? '-'}
          </div>
        </div>
      )}

      {items.length > 0 && (
        <div style={{ background: '#fff', borderRadius: 12, padding: '12px 14px', marginBottom: 12, boxShadow: '0 1px 4px rgba(0,0,0,.06)' }}>
          <div style={{ fontSize: 14, fontWeight: 600, marginBottom: 6 }}>已扫清单（{items.length} 箱）</div>
          {items.map((it) => (
            <div key={it.box.id} style={{ display: 'flex', alignItems: 'center', padding: '8px 0', borderBottom: '1px dashed #f0f0f0', gap: 8 }}>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ fontSize: 13, fontWeight: 600 }}>#{it.box.seqNo} {it.box.boxCode}</div>
                <div className="muted" style={{ fontSize: 11, marginTop: 2 }}>
                  毛{it.box.grossWeight} 净{it.box.netWeight} 公{it.box.publicWeight}kg | 筒{it.box.actualTube ?? '-'}
                  {it.box.boxTime ? ` | ${dayjs(it.box.boxTime).format('MM-DD HH:mm')}` : ''}
                </div>
              </div>
              <Button size="small" onClick={() => remove(it.box.boxCode!)}>移除</Button>
            </div>
          ))}
          <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginTop: 10 }}>
            <span className="muted" style={{ fontSize: 12, flexShrink: 0 }}>标准筒数</span>
            <InputNumber size="large" min={0} precision={0} style={{ flex: 1 }} value={standardTube} onChange={setStandardTube} />
          </div>
          <Button
            type="primary" danger size="large" block style={{ height: 48, fontSize: 16, marginTop: 10 }}
            loading={submitting} disabled={!canPallet} onClick={submit}
          >
            组 托（{items.length} 箱）
          </Button>
          <Button block size="large" style={{ height: 40, marginTop: 8 }} onClick={() => setItems([])}>
            清空清单
          </Button>
        </div>
      )}

      {!items.length && !scanning && !palletCode && (
        <div className="muted" style={{ textAlign: 'center', padding: '30px 0', fontSize: 12 }}>
          扫箱子编码加入清单，同托箱子须属同一执行单
        </div>
      )}
    </div>
  )
}
