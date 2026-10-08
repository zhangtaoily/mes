import { Button, Card, Form, Input, Select, Table } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { usePagedList } from '@/hooks/usePagedList'
import { useWorkshops } from '@/hooks/useOptions'
import { cleanParams } from '@/utils/cleanParams'
import type { ProdTask } from '@/api/plan'
import StatusTag from '@/components/StatusTag'

const DOC_STATUS = ['编制中', '已提交', '生效', '了批', '已关闭', '已作废'].map((s) => ({ label: s, value: s }))

/** 按车间类型过滤的生产任务（复精梳=2 / 倒筒=3）：查看 + 批量提交/撤回 */
export default function TaskTyped({ workshopType, title }: { workshopType: number; title: string }) {
  const { options: wsOptions, nameMap: wsName } = useWorkshops()
  const { rows, loading, pagination, setSearch, reload } = usePagedList<ProdTask>('/api/plan/tasks', { workshopType })
  const [form] = Form.useForm()

  const fixed = { workshopType }

  const columns: ColumnsType<ProdTask> = [
    { title: '任务编号', dataIndex: 'taskNo', width: 150, fixed: 'left' },
    { title: '状态', dataIndex: 'docStatus', width: 85, render: (v) => <StatusTag status={v} /> },
    { title: '车间', dataIndex: 'workshopId', width: 130, render: (v) => wsName[v] ?? v ?? '-' },
    { title: '生产批号', dataIndex: 'batchNo', width: 115 },
    { title: '原料', dataIndex: 'rawMaterial', width: 170, ellipsis: true },
    { title: '色号', dataIndex: 'colorNo', width: 85 },
    { title: '实纺支数', dataIndex: 'actualCount', width: 80 },
    { title: '计划投料量', dataIndex: 'planFeedQty', width: 100, align: 'right' },
    { title: '计划产量', dataIndex: 'planQty', width: 90, align: 'right' },
    { title: 'K3订单号', dataIndex: 'k3OrderNo', width: 130 },
    { title: '交期', dataIndex: 'deliveryDate', width: 95 },
  ]

  return (
    <div className="page">
      <div className="sec-title">{title}</div>
      <Card size="small" style={{ marginBottom: 12 }}>
        <Form form={form} layout="inline" onFinish={(v) => setSearch(cleanParams({ ...v, ...fixed }))}>
          <Form.Item name="workshopId" label="车间">
            <Select allowClear placeholder="全部" showSearch optionFilterProp="label" style={{ width: 145 }} options={wsOptions} />
          </Form.Item>
          <Form.Item name="batchNo" label="生产批号"><Input allowClear placeholder="批号" style={{ width: 120 }} /></Form.Item>
          <Form.Item name="docStatus" label="状态">
            <Select allowClear placeholder="全部" style={{ width: 100 }} options={DOC_STATUS} />
          </Form.Item>
          <Form.Item><Button type="primary" htmlType="submit">搜索</Button></Form.Item>
          <Form.Item><Button onClick={() => { form.resetFields(); setSearch(fixed) }}>重置</Button></Form.Item>
        </Form>
      </Card>
      <Card size="small">
        <Table rowKey="id" size="small" columns={columns} dataSource={rows} loading={loading} pagination={pagination} scroll={{ x: 1400 }} />
      </Card>
    </div>
  )
}
