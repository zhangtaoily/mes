import { Button, Card, Form, Input, Select, Table } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { usePagedList } from '@/hooks/usePagedList'
import { useWorkshops } from '@/hooks/useOptions'
import { cleanParams } from '@/utils/cleanParams'
import type { PlanCraft } from '@/api/plan'
import StatusTag from '@/components/StatusTag'

const DOC_STATUS = ['编制中', '已提交', '生效', '了批', '已关闭', '已作废'].map((s) => ({ label: s, value: s }))

/** 计划工艺单查看（只读视图，复用 /api/plan/crafts） */
export default function PlanView() {
  const { options: wsOptions, nameMap: wsName } = useWorkshops()
  const { rows, loading, pagination, setSearch } = usePagedList<PlanCraft>('/api/plan/crafts')
  const [form] = Form.useForm()

  const columns: ColumnsType<PlanCraft> = [
    { title: '工艺单号', dataIndex: 'craftNo', width: 140, fixed: 'left' },
    { title: '状态', dataIndex: 'docStatus', width: 85, render: (v) => <StatusTag status={v} /> },
    { title: '车间', dataIndex: 'workshopId', width: 130, render: (v) => wsName[v] ?? v ?? '-' },
    { title: '生产批号', dataIndex: 'batchNo', width: 115 },
    { title: '追溯批号', dataIndex: 'traceBatchNo', width: 115 },
    { title: '原料', dataIndex: 'rawMaterial', width: 170, ellipsis: true },
    { title: '色号', dataIndex: 'colorNo', width: 85 },
    { title: '实纺支数', dataIndex: 'actualCount', width: 80 },
    { title: '合并投料量', dataIndex: 'mergeFeedQty', width: 95, align: 'right' },
    { title: '投料量', dataIndex: 'feedQty', width: 85, align: 'right' },
    { title: '计划产量', dataIndex: 'planQty', width: 90, align: 'right' },
    { title: 'K3订单号', dataIndex: 'k3OrderNo', width: 130 },
    { title: '交期', dataIndex: 'deliveryDate', width: 95 },
  ]

  return (
    <div className="page">
      <div className="sec-title">计划工艺单查看</div>
      <Card size="small" style={{ marginBottom: 12 }}>
        <Form form={form} layout="inline" onFinish={(v) => setSearch(cleanParams(v))}>
          <Form.Item name="workshopId" label="车间">
            <Select allowClear placeholder="全部" showSearch optionFilterProp="label" style={{ width: 150 }} options={wsOptions} />
          </Form.Item>
          <Form.Item name="batchNo" label="生产批号"><Input allowClear placeholder="批号" style={{ width: 120 }} /></Form.Item>
          <Form.Item name="actualCount" label="实纺支数"><Input allowClear placeholder="1/40" style={{ width: 100 }} /></Form.Item>
          <Form.Item name="rawMaterial" label="原料"><Input allowClear placeholder="原料" style={{ width: 140 }} /></Form.Item>
          <Form.Item name="docStatus" label="状态">
            <Select allowClear placeholder="全部" style={{ width: 100 }} options={DOC_STATUS} />
          </Form.Item>
          <Form.Item><Button type="primary" htmlType="submit">搜索</Button></Form.Item>
          <Form.Item><Button onClick={() => { form.resetFields(); setSearch({}) }}>重置</Button></Form.Item>
        </Form>
      </Card>
      <Card size="small">
        <Table rowKey="id" size="small" columns={columns} dataSource={rows} loading={loading} pagination={pagination} scroll={{ x: 1550 }} />
        <div className="muted" style={{ marginTop: 8 }}>只读视图：编辑与提交流程请使用「计划工艺单」菜单。</div>
      </Card>
    </div>
  )
}
