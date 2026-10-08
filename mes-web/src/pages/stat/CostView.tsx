import { Button, Card, Form, Input, Select, Table } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { usePagedList } from '@/hooks/usePagedList'
import { cleanParams } from '@/utils/cleanParams'
import { useWorkshops } from '@/hooks/useOptions'
import type { StCost } from '@/api/site'

/** 车间生产费用统计（只读视图） */
export default function CostView() {
  const { nameMap: wsName, options: wsOptions } = useWorkshops()
  const { rows, loading, pagination, setSearch } = usePagedList<StCost>('/api/site/cost')

  const columns: ColumnsType<StCost> = [
    { title: '期间', dataIndex: 'periodMonth', width: 85, fixed: 'left' },
    { title: '车间', dataIndex: 'workshopId', width: 110, render: (v) => wsName[v] ?? v ?? '-' },
    { title: '用水量', dataIndex: 'waterUsage', width: 85, align: 'right' },
    { title: '水费', dataIndex: 'waterCost', width: 90, align: 'right' },
    { title: '用电量', dataIndex: 'powerUsage', width: 85, align: 'right' },
    { title: '电费', dataIndex: 'powerCost', width: 90, align: 'right' },
    { title: '机物料', dataIndex: 'machineMaterialCost', width: 90, align: 'right' },
    { title: '包装费', dataIndex: 'packCost', width: 90, align: 'right' },
    { title: '月折旧', dataIndex: 'monthDepreciation', width: 90, align: 'right' },
    { title: '加工费分摊', dataIndex: 'processCostShare', width: 100, align: 'right' },
    { title: '工资', dataIndex: 'salary', width: 90, align: 'right' },
    { title: '月度考核', dataIndex: 'monthKpi', width: 90, align: 'right' },
    { title: '折算产量', dataIndex: 'convertOutput', width: 90, align: 'right' },
    { title: '用毛率', dataIndex: 'woolRate', width: 85, align: 'right' },
    {
      title: '状态', dataIndex: 'docStatus', width: 80, fixed: 'right',
      render: (v) => v === '已提交' ? <span style={{ color: '#1677ff' }}>{v}</span> : v ?? '-',
    },
  ]

  return (
    <div className="page">
      <div className="sec-title">车间生产费用统计</div>
      <Card size="small" style={{ marginBottom: 12 }}>
        <Form layout="inline" onFinish={(v) => setSearch(cleanParams(v))}>
          <Form.Item name="periodMonth" label="期间"><Input allowClear placeholder="YYYY-MM" style={{ width: 110 }} /></Form.Item>
          <Form.Item name="workshopId" label="车间">
            <Select allowClear placeholder="全部" style={{ width: 150 }} showSearch optionFilterProp="label" options={wsOptions} />
          </Form.Item>
          <Form.Item name="docStatus" label="状态">
            <Select allowClear placeholder="全部" style={{ width: 110 }} options={['编制中', '已提交', '审核通过', '驳回'].map((s) => ({ label: s, value: s }))} />
          </Form.Item>
          <Form.Item><Button type="primary" htmlType="submit">搜索</Button></Form.Item>
          <Form.Item><Button onClick={() => setSearch({})}>重置</Button></Form.Item>
        </Form>
      </Card>
      <Card size="small">
        <Table rowKey="id" size="small" columns={columns} dataSource={rows} loading={loading} pagination={pagination} scroll={{ x: 1600 }} />
      </Card>
    </div>
  )
}
