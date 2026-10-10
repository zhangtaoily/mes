import { useRef, useState } from 'react'
import { App, Button, Input, InputNumber, Spin, Tabs, Tag } from 'antd'
import type { InputRef } from 'antd'
import { PrinterOutlined, ScanOutlined, SearchOutlined } from '@ant-design/icons'
import { pdaBoxInfo, pdaLabelReprint, pdaPackTasks, pdaPrintTube } from '@/api/pda'
import type { PdaBoxScanVO } from '@/api/pda'
import type { PkTask } from '@/api/pack'
import { useAuth } from '@/store/auth'

function Row({ label, value }: { label: string; value?: React.ReactNode }) {
  return (
    <div style={{ display: 'flex', padding: '6px 0', fontSize: 13, borderBottom: '1px dashed #f0f0f0' }}>
      <span className="muted" style={{ width: 76, flexShrink: 0 }}>{label}</span>
      <span style={{ flex: 1, wordBreak: 'break-all' }}>{value ?? '-'}</span>
    </div>
  )
}

/** PDA 打印作业：筒贴打印（按任务留痕）/ 箱贴补打（扫箱码带出信息，FG 取号） */
export default function PdaPrint() {
  return (
    <div style={{ padding: 14 }}>
      <Tabs
        items={[
          { key: 'tube', label: '筒贴打印', children: <TubePanel /> },
          { key: 'label', label: '箱贴补打', children: <LabelPanel /> },
        ]}
      />
    </div>
  )
}

/** 筒贴打印：查任务 → 数量 → 打印留痕 */
function TubePanel() {
  const { message } = App.useApp()
  const { hasPerm } = useAuth()
  const [keyword, setKeyword] = useState('')
  const [loading, setLoading] = useState(false)
  const [tasks, setTasks] = useState<PkTask[] | null>(null)
  const [task, setTask] = useState<PkTask | null>(null)
  const [qty, setQty] = useState<number | null>(1)
  const [printing, setPrinting] = useState(false)
  const canPrint = hasPerm('pack:task:list')

  const search = async () => {
    const kw = keyword.trim()
    if (!kw) return
    setLoading(true)
    try {
      setTasks(await pdaPackTasks(kw))
    } catch {
      setTasks([])
    } finally {
      setLoading(false)
    }
  }

  const print = async () => {
    if (!task) return
    setPrinting(true)
    try {
      await pdaPrintTube(task.id, qty ?? 1)
      message.success(`筒贴打印已留痕：${task.taskNo} x ${qty ?? 1}`)
    } finally {
      setPrinting(false)
    }
  }

  return (
    <>
      {!task && (
        <>
          <div style={{ background: '#fff', borderRadius: 12, padding: 12, marginBottom: 12, boxShadow: '0 1px 4px rgba(0,0,0,.06)' }}>
            <Input
              size="large" autoFocus prefix={<SearchOutlined style={{ color: '#409eff', fontSize: 18 }} />}
              placeholder="扫/输任务编号、生产批号、入库批号"
              value={keyword} onChange={(e) => setKeyword(e.target.value)} onPressEnter={search} style={{ fontSize: 15 }}
            />
            <Button type="primary" size="large" block style={{ height: 46, marginTop: 10, fontSize: 16 }} loading={loading} onClick={search}>
              查 询
            </Button>
          </div>
          {loading && <div style={{ textAlign: 'center', padding: 40 }}><Spin /></div>}
          {tasks?.map((t) => (
            <div key={t.id} style={{ background: '#fff', borderRadius: 12, padding: '10px 14px', marginBottom: 10, boxShadow: '0 1px 4px rgba(0,0,0,.06)' }} onClick={() => setTask(t)}>
              <div style={{ fontSize: 13, fontWeight: 600, display: 'flex', justifyContent: 'space-between' }}>
                <span>{t.taskNo}</span>
                {t.isFinished === 1 ? <Tag color="success">已了批</Tag> : <Tag color="processing">进行中</Tag>}
              </div>
              <div className="muted" style={{ fontSize: 12, marginTop: 4 }}>
                {t.productName} | 批号 {t.batchNo ?? '-'} | 入库 {t.warehouseBatchNo ?? '-'}
              </div>
            </div>
          ))}
          {tasks && tasks.length === 0 && !loading && (
            <div className="muted" style={{ textAlign: 'center', padding: 30, fontSize: 13 }}>未找到匹配的包装任务</div>
          )}
        </>
      )}
      {task && (
        <div style={{ background: '#fff', borderRadius: 12, padding: '12px 14px', boxShadow: '0 1px 4px rgba(0,0,0,.06)' }}>
          <div style={{ fontSize: 14, fontWeight: 600, marginBottom: 4 }}>{task.taskNo}</div>
          <Row label="生产批号" value={task.batchNo} />
          <Row label="入库批号" value={task.warehouseBatchNo} />
          <Row label="缸号" value={task.dyeVatNo} />
          <Row label="产品" value={task.productName} />
          <Row label="规格" value={task.productSpec} />
          <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginTop: 12 }}>
            <span className="muted" style={{ fontSize: 12, flexShrink: 0 }}>打印数量</span>
            <InputNumber size="large" min={1} precision={0} style={{ flex: 1 }} value={qty} onChange={setQty} />
          </div>
          <Button
            type="primary" size="large" block icon={<PrinterOutlined />}
            style={{ height: 48, fontSize: 16, marginTop: 12 }} loading={printing} disabled={!canPrint} onClick={print}
          >
            筒贴打印
          </Button>
          <Button block size="large" style={{ height: 40, marginTop: 8 }} onClick={() => { setTask(null); setTasks(null); setKeyword('') }}>
            换任务
          </Button>
        </div>
      )}
    </>
  )
}

/** 箱贴补打：扫箱码 → 回显信息 → 登记补打（FG 取号） */
function LabelPanel() {
  const { message, modal } = App.useApp()
  const { hasPerm } = useAuth()
  const inputRef = useRef<InputRef>(null)
  const [content, setContent] = useState('')
  const [loading, setLoading] = useState(false)
  const [info, setInfo] = useState<PdaBoxScanVO | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const canReprint = hasPerm('pack:trace:list')

  const scan = async (code: string) => {
    const c = code.trim()
    if (!c) return
    setLoading(true)
    try {
      setInfo(await pdaBoxInfo(c))
    } catch {
      setInfo(null)
    } finally {
      setLoading(false)
      setContent('')
      inputRef.current?.focus()
    }
  }

  const submit = () => {
    if (!info) return
    modal.confirm({
      title: '登记箱贴补打？',
      content: `箱子 ${info.box.boxCode}，自动取号 FG 并留痕。`,
      okText: '登记补打',
      cancelText: '取消',
      onOk: async () => {
        setSubmitting(true)
        try {
          await pdaLabelReprint(info.box.boxCode!)
          message.success('箱贴补打已登记')
          setInfo(null)
        } finally {
          setSubmitting(false)
        }
      },
    })
  }

  return (
    <>
      <div style={{ background: '#fff', borderRadius: 12, padding: 12, marginBottom: 12, boxShadow: '0 1px 4px rgba(0,0,0,.06)' }}>
        <Input
          ref={inputRef} size="large" autoFocus
          prefix={<ScanOutlined style={{ color: '#409eff', fontSize: 18 }} />}
          placeholder="扫箱子编码 BX..."
          value={content} onChange={(e) => setContent(e.target.value)} onPressEnter={() => scan(content)} style={{ fontSize: 15 }}
        />
        <Button type="primary" size="large" block style={{ height: 46, marginTop: 10, fontSize: 16 }} loading={loading} onClick={() => scan(content)}>
          查 询
        </Button>
      </div>

      {loading && <div style={{ textAlign: 'center', padding: 40 }}><Spin /></div>}

      {info && (
        <div style={{ background: '#fff', borderRadius: 12, padding: '12px 14px', boxShadow: '0 1px 4px rgba(0,0,0,.06)' }}>
          <div style={{ fontSize: 14, fontWeight: 600, marginBottom: 4 }}>
            #{info.box.seqNo} {info.box.boxCode}
            {info.box.palletCode && <Tag style={{ marginLeft: 6 }}>{info.box.palletCode}</Tag>}
          </div>
          <Row label="执行单号" value={info.execute.executeNo} />
          <Row label="产品" value={info.execute.productName} />
          <Row label="规格/色号" value={`${info.execute.productSpec ?? '-'} / ${info.execute.colorNo ?? '-'}`} />
          <Row label="入库批号" value={info.execute.warehouseBatchNo} />
          <Row label="毛重(kg)" value={info.box.grossWeight} />
          <Row label="净重(kg)" value={info.box.netWeight} />
          <Row label="公重(kg)" value={info.box.publicWeight} />
          <Row label="皮重(kg)" value={info.box.tareWeight} />
          <Row label="包装规格" value={info.box.packSpec} />
          <Button
            type="primary" size="large" block icon={<PrinterOutlined />}
            style={{ height: 48, fontSize: 16, marginTop: 12 }} loading={submitting} disabled={!canReprint} onClick={submit}
          >
            登记补打
          </Button>
        </div>
      )}

      {!info && !loading && (
        <div className="muted" style={{ textAlign: 'center', padding: '30px 0', fontSize: 12 }}>
          扫箱子编码带出信息，补打自动取号 FG 前缀并留痕
        </div>
      )}
    </>
  )
}
