import { useRef, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { App, Button, Input, Spin, Tag } from 'antd'
import type { InputRef } from 'antd'
import { ReloadOutlined, ScanOutlined } from '@ant-design/icons'
import { pdaBatchOff, pdaScan } from '@/api/pda'
import type { PdaScanVO } from '@/api/pda'
import { useAuth } from '@/store/auth'
import dayjs from 'dayjs'

function Row({ label, value }: { label: string; value?: React.ReactNode }) {
  return (
    <div style={{ display: 'flex', padding: '7px 0', fontSize: 13, borderBottom: '1px dashed #f0f0f0' }}>
      <span className="muted" style={{ width: 76, flexShrink: 0 }}>{label}</span>
      <span style={{ flex: 1, wordBreak: 'break-all' }}>{value ?? '-'}</span>
    </div>
  )
}

function Card({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div style={{ background: '#fff', borderRadius: 12, padding: '12px 14px', marginBottom: 12, boxShadow: '0 1px 4px rgba(0,0,0,.06)' }}>
      <div style={{ fontSize: 14, fontWeight: 600, marginBottom: 4 }}>{title}</div>
      {children}
    </div>
  )
}

/** PDA 扫码工作台：扫制造卡/排程单 → 回显排程+任务+工艺项 → 上批/下批/报产 */
export default function PdaScan() {
  const { message, modal } = App.useApp()
  const navigate = useNavigate()
  const location = useLocation()
  const { hasPerm } = useAuth()
  const inputRef = useRef<InputRef>(null)
  const [content, setContent] = useState('')
  const [loading, setLoading] = useState(false)
  const [scan, setScan] = useState<PdaScanVO | null>(null)

  const doScan = async (code: string) => {
    const c = code.trim()
    if (!c) return
    setLoading(true)
    try {
      const vo = await pdaScan(c)
      setScan(vo)
      message.success(`已识别：${vo.schedule.scheduleNo}`)
    } catch {
      setScan(null)
    } finally {
      setLoading(false)
      setContent('')
      inputRef.current?.focus()
    }
  }

  const offBatch = (id: number) => {
    modal.confirm({
      title: '确认下批？',
      content: '下批后该机台批次结束，可重新上批。',
      okText: '确认下批',
      cancelText: '取消',
      onOk: async () => {
        await pdaBatchOff(id)
        message.success('下批成功')
        doScan(content || scan?.schedule.scheduleNo || '')
      },
    })
  }

  const s = scan?.schedule
  const t = scan?.task
  const active = scan?.activeBatchOn

  return (
    <div style={{ padding: 14 }}>
      <div style={{ background: '#fff', borderRadius: 12, padding: 12, marginBottom: 12, boxShadow: '0 1px 4px rgba(0,0,0,.06)' }}>
        <Input
          ref={inputRef}
          size="large"
          autoFocus
          prefix={<ScanOutlined style={{ color: '#409eff', fontSize: 18 }} />}
          placeholder="扫制造卡二维码 / 输入排程单号"
          value={content}
          onChange={(e) => setContent(e.target.value)}
          onPressEnter={() => doScan(content)}
          style={{ fontSize: 15 }}
        />
        <Button
          type="primary" size="large" block style={{ height: 46, marginTop: 10, fontSize: 16 }}
          loading={loading}
          onClick={() => doScan(content)}
        >
         识 别
        </Button>
      </div>

      {loading && !scan && (
        <div style={{ textAlign: 'center', padding: 40 }}>
          <Spin />
        </div>
      )}

      {scan && s && (
        <>
          <Card title="排程信息">
            <Row label="排程单号" value={s.scheduleNo} />
            <Row label="生产批号" value={s.batchNo} />
            <Row label="追溯批号" value={s.traceBatchNo} />
            <Row label="工序" value={scan.processName} />
            <Row label="机台" value={scan.machineNo} />
            <Row label="排程日期" value={s.scheduleDate} />
            <Row label="计划产量" value={s.scheduleQty} />
          </Card>

          {t && (
            <Card title="任务信息">
              <Row label="任务号" value={t.taskNo} />
              <Row label="产品编码" value={t.productNo} />
              <Row label="产品规格" value={t.productSpec} />
              <Row label="原料" value={t.rawMaterial} />
              <Row label="任务状态" value={t.docStatus} />
            </Card>
          )}

          {!!scan.crafts?.length && (
            <Card title={`上机工艺项（${scan.crafts.length}）`}>
              {scan.crafts.map((c) => (
                <Row
                  key={c.id}
                  label={c.paramName ?? ''}
                  value={
                    <>
                      <b style={{ color: '#409eff' }}>{c.paramValue ?? '-'}</b>
                      {(c.lowerLimit || c.upperLimit) && (
                        <span className="muted" style={{ fontSize: 11, marginLeft: 6 }}>
                          [{c.lowerLimit ?? '?'} ~ {c.upperLimit ?? '?'}]
                        </span>
                      )}
                    </>
                  }
                />
              ))}
            </Card>
          )}

          <Card title="当前批次">
            {active ? (
              <>
                <Tag color="processing">活动中 {active.batchOnNo}</Tag>
                <Row label="上批时间" value={active.batchOnTime ? dayjs(active.batchOnTime).format('MM-DD HH:mm') : '-'} />
                <Row label="班次" value={active.shift} />
              </>
            ) : (
              <Tag color="success">无活动上批，可上批</Tag>
            )}
          </Card>

          <div style={{ display: 'flex', flexDirection: 'column', gap: 10, marginTop: 4 }}>
            {!active && hasPerm('site:batchOn:list') && (
              <Button type="primary" size="large" style={{ height: 48, fontSize: 16 }} onClick={() => navigate('/pda/batch-on', { state: { scan } })}>
                上 批
              </Button>
            )}
            {active && hasPerm('site:batchOn:list') && (
              <Button danger size="large" style={{ height: 48, fontSize: 16 }} onClick={() => offBatch(active.id)}>
                下 批
              </Button>
            )}
            {active && hasPerm('site:outputReport:list') && (
              <Button type="primary" size="large" ghost style={{ height: 48, fontSize: 16 }} onClick={() => navigate('/pda/output', { state: { scan } })}>
                报产量
              </Button>
            )}
            <Button size="large" style={{ height: 44 }} icon={<ReloadOutlined />} onClick={() => setScan(null)}>
              清除结果
            </Button>
          </div>
        </>
      )}
      {!scan && !loading && (
        <div className="muted" style={{ textAlign: 'center', padding: '30px 0', fontSize: 12 }}>
          {location.state ? '' : '支持制造卡二维码 / 排程单号（如 PS...）'}
        </div>
      )}
    </div>
  )
}
