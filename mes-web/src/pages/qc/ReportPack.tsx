import { useState } from 'react'
import { App, Button, Card, DatePicker, Form, Input, InputNumber, Modal, Select, Space, Table } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { MinusCircleOutlined, PlusOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import type { Dayjs } from 'dayjs'
import { usePagedList } from '@/hooks/usePagedList'
import { useWorkshops } from '@/hooks/useOptions'
import { cleanParams } from '@/utils/cleanParams'
import { pagePackageReports, getPackageReportDetail, savePackageReport, DEFECT_TYPES } from '@/api/qc'
import type { QcPackageReport, QcPackageDefect } from '@/api/qc'

type DefectRow = { defectType?: string; value?: number; remark?: string }
type FormValues = Partial<Omit<QcPackageReport, 'docDate'>> & { docDate?: Dayjs; defects?: DefectRow[] }

/** 包装质检报告：按入库批次录入 15 类疵纱分类，合格率服务端计算（缺省一等品数=正纱数） */
export default function ReportPack() {
  const { message } = App.useApp()
  const { options: wsOptions, nameMap: wsName } = useWorkshops()
  const { rows, loading, pagination, setSearch, reload } = usePagedList<QcPackageReport>('/api/qc/package-reports')
  const [form] = Form.useForm()
  const [editing, setEditing] = useState<QcPackageReport | null>(null)
  const [open, setOpen] = useState(false)

  const openForm = async (row?: QcPackageReport) => {
    setEditing(row ?? null)
    form.resetFields()
    if (row) {
      const d = await getPackageReportDetail(row.id)
      form.setFieldsValue({
        ...d.report,
        docDate: d.report.docDate ? dayjs(d.report.docDate) : undefined,
        defects: d.defects.map((x: QcPackageDefect) => ({ defectType: x.defectType, value: x.value, remark: x.remark })),
      })
    } else {
      form.setFieldsValue({ defects: [{}] })
    }
    setOpen(true)
  }

  const submit = async (v: FormValues) => {
    await savePackageReport({
      id: editing?.id,
      warehouseBatchNo: v.warehouseBatchNo,
      batchNo: v.batchNo,
      workshopId: v.workshopId,
      qcWorkshopId: v.qcWorkshopId,
      docDate: v.docDate ? v.docDate.format('YYYY-MM-DD') : undefined,
      firstGradeQty: v.firstGradeQty,
      remark: v.remark,
      defects: (v.defects ?? []).filter((x) => x.defectType),
    })
    message.success('已保存')
    setOpen(false)
    reload()
  }

  const columns: ColumnsType<QcPackageReport> = [
    { title: '入库批次', dataIndex: 'warehouseBatchNo', width: 130, fixed: 'left' },
    { title: '生产批次', dataIndex: 'batchNo', width: 120, render: (v) => v ?? '-' },
    { title: '生产车间', dataIndex: 'workshopId', width: 130, render: (v) => wsName[v] ?? v ?? '-' },
    { title: '质检车间', dataIndex: 'qcWorkshopId', width: 120, render: (v) => wsName[v] ?? v ?? '-' },
    { title: '检验日期', dataIndex: 'docDate', width: 100 },
    { title: '一等品数', dataIndex: 'firstGradeQty', width: 85, align: 'right' },
    { title: '合格率', dataIndex: 'passRate', width: 90, align: 'right', render: (v) => (v != null ? `${v}%` : '-') },
    { title: '备注', dataIndex: 'remark', width: 160, ellipsis: true, render: (v) => v ?? '-' },
    {
      title: '操作', width: 80, fixed: 'right',
      render: (_, r) => <a onClick={() => openForm(r)}>编辑</a>,
    },
  ]

  return (
    <div className="page">
      <div className="sec-title">包装质检报告</div>
      <Card size="small" style={{ marginBottom: 12 }}>
        <Form form={form} layout="inline" onFinish={(v) => setSearch(cleanParams(v))}>
          <Form.Item name="warehouseBatchNo" label="入库批次"><Input allowClear placeholder="如 2607933-4" style={{ width: 140 }} /></Form.Item>
          <Form.Item name="batchNo" label="生产批次"><Input allowClear placeholder="批号" style={{ width: 130 }} /></Form.Item>
          <Form.Item><Button type="primary" htmlType="submit">搜索</Button></Form.Item>
          <Form.Item><Button onClick={() => { form.resetFields(); setSearch({}) }}>重置</Button></Form.Item>
          <Form.Item><Button type="primary" onClick={() => openForm()}>录入报告</Button></Form.Item>
        </Form>
      </Card>
      <Card size="small">
        <Table rowKey="id" size="small" columns={columns} dataSource={rows} loading={loading} pagination={pagination} scroll={{ x: 1000 }} />
      </Card>

      <Modal
        title={editing ? `编辑包装质检 - ${editing.warehouseBatchNo}` : '录入包装质检报告'}
        open={open}
        onCancel={() => setOpen(false)}
        onOk={() => form.submit()}
        width={720}
        destroyOnClose
      >
        <Form form={form} layout="vertical" onFinish={submit}>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', columnGap: 12 }}>
            <Form.Item name="warehouseBatchNo" label="入库批次" rules={[{ required: true, message: '请输入入库批次' }]}>
              <Input placeholder="如 2607933-4" />
            </Form.Item>
            <Form.Item name="batchNo" label="生产批次"><Input /></Form.Item>
            <Form.Item name="workshopId" label="生产车间">
              <Select allowClear placeholder="全部" showSearch optionFilterProp="label" options={wsOptions} />
            </Form.Item>
            <Form.Item name="qcWorkshopId" label="质检车间">
              <Select allowClear placeholder="全部" showSearch optionFilterProp="label" options={wsOptions} />
            </Form.Item>
            <Form.Item name="docDate" label="检验日期"><DatePicker style={{ width: '100%' }} /></Form.Item>
            <Form.Item name="firstGradeQty" label="一等品数（缺省取正纱数）">
              <InputNumber style={{ width: '100%' }} min={0} placeholder="留空由服务端计算" />
            </Form.Item>
          </div>
          <Form.Item style={{ marginBottom: 4 }}>
            <span style={{ fontWeight: 600 }}>疵纱分类明细（15 类）</span>
          </Form.Item>
          <Form.List name="defects">
            {(fields, { add, remove }) => (
              <>
                {fields.map((field) => (
                  <Space key={field.key} align="baseline" style={{ display: 'flex', marginBottom: 4 }}>
                    <Form.Item name={[field.name, 'defectType']} rules={[{ required: true, message: '选择类型' }]} style={{ marginBottom: 0 }}>
                      <Select showSearch optionFilterProp="label" placeholder="疵纱类型" style={{ width: 130 }} options={DEFECT_TYPES.map((s) => ({ label: s, value: s }))} />
                    </Form.Item>
                    <Form.Item name={[field.name, 'value']} style={{ marginBottom: 0 }}>
                      <InputNumber placeholder="数量/值" min={0} style={{ width: 110 }} />
                    </Form.Item>
                    <Form.Item name={[field.name, 'remark']} style={{ marginBottom: 0 }}>
                      <Input placeholder="备注" style={{ width: 260 }} />
                    </Form.Item>
                    <MinusCircleOutlined onClick={() => remove(field.name)} />
                  </Space>
                ))}
                <Button type="dashed" block icon={<PlusOutlined />} onClick={() => add({})}>添加疵纱分类</Button>
              </>
            )}
          </Form.List>
          <Form.Item name="remark" label="整单备注" style={{ marginTop: 12 }}>
            <Input.TextArea rows={2} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
