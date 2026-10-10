import { useEffect, useRef, useState } from 'react'
import { App, Button, Input, InputNumber, Select, Spin, Tabs, Tag } from 'antd'
import type { InputRef } from 'antd'
import { SaveOutlined, ScanOutlined, SearchOutlined } from '@ant-design/icons'
import {
  pdaQcReportDetail, pdaQcReports, pdaSaveQcReport, pdaWorkshops,
} from '@/api/pda'
import { DEFECT_TYPES } from '@/api/qc'
import type { QcPackageReport } from '@/api/qc'
import type { BaseWorkshop } from '@/api/plan'
import { useAuth } from '@/store/auth'

type DefectMap = Record<string, number | null>

/** PDA 包装质检：按入库批次录入 15 类疵纱分类（合格率服务端计算，一等品数缺省取正纱数） */
export default function PdaQcPack() {
  return (
    <div style={{ padding: 14 }}>
      <Tabs
        items={[
          { key: 'edit', label: '报告录入', children: <ReportPanel /> },
          { key: 'list', label: '报告查询', children: <ListPanel /> },
        ]}
      />
    </div>
  )
}

/** 报告录入/编辑表单（新建与编辑共用） */
function ReportPanel({ editId, onDone }: { editId?: number; onDone?: () => void }) {
  const { message } = App.useApp()
  const { hasPerm } = useAuth()
  const firstRef = useRef<InputRef>(null)
  const [workshops, setWorkshops] = useState<BaseWorkshop[]>([])
  const [whBatch, setWhBatch] = useState('')
  const [batchNo, setBatchNo] = useState('')
  const [workshopId, setWorkshopId] = useState<number | undefined>()
  const [qcWorkshopId, setQcWorkshopId] = useState<number | undefined>()
  const [firstGradeQty, setFirstGradeQty] = useState<number | null>(null)
  const [remark, setRemark] = useState('')
  const [defects, setDefects] = useState<DefectMap>({})
  const [submitting, setSubmitting] = useState(false)
  const canSave = hasPerm('qc:packageReport:list')
  const isEdit = editId != null

  useEffect(() => {
    pdaWorkshops().then(setWorkshops).catch(() => setWorkshops([]))
    if (isEdit) {
      pdaQcReportDetail(editId!).then((d) => {
        setWhBatch(d.report.warehouseBatchNo)
        setBatchNo(d.report.batchNo ?? '')
        setWorkshopId(d.report.workshopId)
        setQcWorkshopId(d.report.qcWorkshopId)
        setFirstGradeQty(d.report.firstGradeQty ?? null)
        setRemark(d.report.remark ?? '')
        const map: DefectMap = {}
        d.defects.forEach((x) => { if (x.defectType) map[x.defectType] = x.value ?? null })
        setDefects(map)
      }).catch(() => { /* 详情加载失败留在新建态 */ })
    } else {
      firstRef.current?.focus()
    }
  }, [editId, isEdit])

  const submit = async () => {
    if (!whBatch.trim()) {
      message.warning('请输入入库批次')
      firstRef.current?.focus()
      return
    }
    const filled = Object.entries(defects).filter(([, v]) => v != null)
    if (filled.length === 0) {
      message.warning('请至少录入一类疵纱数量（正纱为正常品数量）')
      return
    }
    setSubmitting(true)
    try {
      await pdaSaveQcReport({
        id: isEdit ? editId : undefined,
        warehouseBatchNo: whBatch.trim(),
        batchNo: batchNo.trim() || undefined,
        workshopId,
        qcWorkshopId,
        firstGradeQty: firstGradeQty ?? undefined,
        remark: remark.trim() || undefined,
        defects: filled.map(([defectType, value]) => ({ defectType, value: value! })),
      })
      message.success(isEdit ? '报告已更新' : '报告已保存')
      if (isEdit) {
        onDone?.()
      } else {
        setWhBatch(''); setBatchNo(''); setFirstGradeQty(null); setRemark(''); setDefects({})
        firstRef.current?.focus()
      }
    } finally {
      setSubmitting(false)
    }
  }

  const wsOptions = workshops.map((w) => ({ label: w.workshopName, value: w.id }))

  return (
    <div style={{ background: '#fff', borderRadius: 12, padding: '12px 14px', boxShadow: '0 1px 4px rgba(0,0,0,.06)' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 10 }}>
        <span style={{ fontSize: 13, fontWeight: 600, flexShrink: 0 }}>入库批次</span>
        <Input
          ref={firstRef} size="large" disabled={isEdit}
          prefix={<ScanOutlined style={{ color: '#409eff', fontSize: 16 }} />}
          placeholder="扫/输入如 2607933-4"
          value={whBatch} onChange={(e) => setWhBatch(e.target.value)} style={{ fontSize: 15 }}
        />
      </div>
      <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 10 }}>
        <span className="muted" style={{ fontSize: 13, width: 60, flexShrink: 0 }}>生产批次</span>
        <Input size="large" value={batchNo} onChange={(e) => setBatchNo(e.target.value)} placeholder="选填" style={{ fontSize: 15 }} />
      </div>
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10, marginBottom: 10 }}>
        <Select
          size="large" allowClear placeholder="生产车间" showSearch optionFilterProp="label"
          options={wsOptions} value={workshopId} onChange={setWorkshopId}
        />
        <Select
          size="large" allowClear placeholder="质检车间" showSearch optionFilterProp="label"
          options={wsOptions} value={qcWorkshopId} onChange={setQcWorkshopId}
        />
      </div>

      <div style={{ fontSize: 13, fontWeight: 600, margin: '4px 0 6px' }}>
        疵纱分类数量（{DEFECT_TYPES.length} 类）
      </div>
      {DEFECT_TYPES.map((t) => (
        <div
          key={t}
          style={{
            display: 'flex', alignItems: 'center', gap: 8, padding: '5px 0',
            borderBottom: '1px dashed #f0f0f0',
            background: t === '正纱' ? 'rgba(82,196,26,.06)' : undefined, borderRadius: t === '正纱' ? 6 : 0,
            paddingLeft: t === '正纱' ? 6 : 0, paddingRight: t === '正纱' ? 6 : 0,
          }}
        >
          <span style={{ fontSize: 13, flex: 1 }}>
            {t}
            {t === '正纱' && <span className="muted" style={{ fontSize: 11 }}>（正常品）</span>}
          </span>
          <InputNumber
            size="large" min={0} precision={0} style={{ width: 120 }}
            placeholder="数量" value={defects[t] ?? null}
            onChange={(v) => setDefects((m) => ({ ...m, [t]: v }))}
          />
        </div>
      ))}

      <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginTop: 10 }}>
        <span className="muted" style={{ fontSize: 13, width: 84, flexShrink: 0 }}>一等品数</span>
        <InputNumber
          size="large" min={0} precision={0} style={{ flex: 1 }}
          placeholder="留空自动取正纱数" value={firstGradeQty} onChange={setFirstGradeQty}
        />
      </div>
      <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginTop: 10 }}>
        <span className="muted" style={{ fontSize: 13, width: 84, flexShrink: 0 }}>备注</span>
        <Input size="large" value={remark} onChange={(e) => setRemark(e.target.value)} placeholder="选填" />
      </div>

      <Button
        type="primary" size="large" block icon={<SaveOutlined />}
        style={{ height: 48, fontSize: 16, marginTop: 14 }} loading={submitting} disabled={!canSave} onClick={submit}
      >
        {isEdit ? '更新报告' : '保存报告'}
      </Button>
      {isEdit && (
        <Button block size="large" style={{ height: 40, marginTop: 8 }} onClick={onDone}>
          返回列表
        </Button>
      )}
    </div>
  )
}

/** 报告查询：关键词 → 报告卡片 → 点击编辑 */
function ListPanel() {
  const [keyword, setKeyword] = useState('')
  const [loading, setLoading] = useState(false)
  const [rows, setRows] = useState<QcPackageReport[] | null>(null)
  const [editId, setEditId] = useState<number | null>(null)

  const search = async () => {
    setLoading(true)
    try {
      setRows(await pdaQcReports(keyword.trim() || undefined))
    } catch {
      setRows([])
    } finally {
      setLoading(false)
    }
  }

  if (editId != null) {
    return <ReportPanel editId={editId} onDone={() => { setEditId(null); search() }} />
  }

  return (
    <>
      <div style={{ background: '#fff', borderRadius: 12, padding: 12, marginBottom: 12, boxShadow: '0 1px 4px rgba(0,0,0,.06)' }}>
        <Input
          size="large" autoFocus prefix={<SearchOutlined style={{ color: '#409eff', fontSize: 18 }} />}
          placeholder="扫/输入入库批次、生产批次"
          value={keyword} onChange={(e) => setKeyword(e.target.value)} onPressEnter={search} style={{ fontSize: 15 }}
        />
        <Button type="primary" size="large" block style={{ height: 46, marginTop: 10, fontSize: 16 }} loading={loading} onClick={search}>
          查 询
        </Button>
      </div>
      {loading && <div style={{ textAlign: 'center', padding: 40 }}><Spin /></div>}
      {rows?.map((r) => (
        <div
          key={r.id}
          onClick={() => setEditId(r.id)}
          style={{ background: '#fff', borderRadius: 12, padding: '10px 14px', marginBottom: 10, boxShadow: '0 1px 4px rgba(0,0,0,.06)', cursor: 'pointer' }}
        >
          <div style={{ fontSize: 13, fontWeight: 600, display: 'flex', justifyContent: 'space-between' }}>
            <span>{r.warehouseBatchNo}</span>
            {r.passRate != null && (
              <Tag color={r.passRate >= 95 ? 'success' : r.passRate >= 80 ? 'warning' : 'error'}>
                {r.passRate}%
              </Tag>
            )}
          </div>
          <div className="muted" style={{ fontSize: 12, marginTop: 4 }}>
            {r.docDate ?? '-'} | 生产批次 {r.batchNo ?? '-'} | 一等品 {r.firstGradeQty ?? '-'}
          </div>
        </div>
      ))}
      {rows && rows.length === 0 && !loading && (
        <div className="muted" style={{ textAlign: 'center', padding: 30, fontSize: 13 }}>未找到匹配的质检报告</div>
      )}
    </>
  )
}
