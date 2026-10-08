import { Button, Card, Form, Input, Select, Table } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { usePagedList } from '@/hooks/usePagedList'
import { useWorkshops } from '@/hooks/useOptions'
import { cleanParams } from '@/utils/cleanParams'
import type { QcReport } from '@/api/qc'
import StatusTag from '@/components/StatusTag'

/** 公示检验报告（对外只读视图，仅展示公示中的报告） */
export default function ReportPublic() {
  const { options: wsOptions, nameMap: wsName } = useWorkshops()
  const { rows, loading, pagination, setSearch } = usePagedList<QcReport>('/api/qc/reports', { publicOnly: 1 })
  const [form] = Form.useForm()

  const columns: ColumnsType<QcReport> = [
    { title: '报告编号', dataIndex: 'qcNo', width: 150, fixed: 'left' },
    { title: '状态', dataIndex: 'docStatus', width: 85, render: (v) => <StatusTag status={v} /> },
    { title: '车间', dataIndex: 'workshopId', width: 130, render: (v) => wsName[v] ?? v ?? '-' },
    { title: '质检类型', dataIndex: 'qcType', width: 90 },
    { title: '生产批号', dataIndex: 'batchNo', width: 125 },
    { title: '缸号', dataIndex: 'dyeVatNo', width: 95 },
    { title: '检验员', dataIndex: 'inspector', width: 95 },
    { title: '检验日期', dataIndex: 'inspectDate', width: 110 },
    { title: '抽样数', dataIndex: 'sampleQty', width: 80, align: 'right' },
    { title: '留样数', dataIndex: 'retainQty', width: 80, align: 'right' },
    { title: '原料', dataIndex: 'rawMaterial', width: 150, ellipsis: true },
    { title: '支数', dataIndex: 'spinCount', width: 85 },
  ]

  return (
    <div className="page">
      <div className="sec-title">公示检验报告</div>
      <Card size="small" style={{ marginBottom: 12 }}>
        <Form form={form} layout="inline" onFinish={(v) => setSearch(cleanParams({ ...v, publicOnly: 1 }))}>
          <Form.Item name="qcNo" label="报告编号"><Input allowClear placeholder="QC..." style={{ width: 130 }} /></Form.Item>
          <Form.Item name="batchNo" label="生产批号"><Input allowClear placeholder="批号" style={{ width: 115 }} /></Form.Item>
          <Form.Item name="workshopId" label="车间">
            <Select allowClear placeholder="全部" style={{ width: 145 }} options={wsOptions} />
          </Form.Item>
          <Form.Item><Button type="primary" htmlType="submit">搜索</Button></Form.Item>
          <Form.Item><Button onClick={() => { form.resetFields(); setSearch({ publicOnly: 1 }) }}>重置</Button></Form.Item>
        </Form>
      </Card>
      <Card size="small">
        <div className="muted" style={{ marginBottom: 8, fontSize: 12 }}>仅展示公示中的报告；在"检验报告"页执行公示/取消公示</div>
        <Table rowKey="id" size="small" columns={columns} dataSource={rows} loading={loading} pagination={pagination} scroll={{ x: 1400 }} />
      </Card>
    </div>
  )
}
