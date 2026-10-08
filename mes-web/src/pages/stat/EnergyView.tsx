import { useState } from 'react'
import { Button, Card, Form, Input, Modal, Select, Table } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { usePagedList } from '@/hooks/usePagedList'
import { cleanParams } from '@/utils/cleanParams'
import { useWorkshops } from '@/hooks/useOptions'
import { getEnergyItems } from '@/api/site'
import type { StEnergy, StEnergyItem } from '@/api/site'

/** 车间能耗统计（只读视图，含机台明细查看） */
export default function EnergyView() {
  const { nameMap: wsName, options: wsOptions } = useWorkshops()
  const { rows, loading, pagination, setSearch } = usePagedList<StEnergy>('/api/site/energy')
  const [detail, setDetail] = useState<{ month: string; workshop: string; items: StEnergyItem[] } | null>(null)
  const [itemsLoading, setItemsLoading] = useState(false)

  const loadItems = async (r: StEnergy) => {
    setItemsLoading(true)
    try {
      const items = await getEnergyItems(r.id)
      setDetail({ month: r.periodMonth ?? '-', workshop: wsName[r.workshopId ?? 0] ?? String(r.workshopId ?? '-'), items })
    } finally {
      setItemsLoading(false)
    }
  }

  const columns: ColumnsType<StEnergy> = [
    { title: '期间', dataIndex: 'periodMonth', width: 90 },
    { title: '车间', dataIndex: 'workshopId', width: 120, render: (v) => wsName[v] ?? v ?? '-' },
    { title: '用水(吨)', dataIndex: 'waterUsage', width: 100, align: 'right' },
    { title: '用电(KWh)', dataIndex: 'powerUsage', width: 110, align: 'right' },
    { title: '用气(m³)', dataIndex: 'gasUsage', width: 100, align: 'right' },
    {
      title: '状态', dataIndex: 'docStatus', width: 80,
      render: (v) => v === '已提交' ? <span style={{ color: '#1677ff' }}>{v}</span> : v ?? '-',
    },
    {
      title: '操作', width: 80,
      render: (_, r) => <a onClick={() => loadItems(r)}>明细</a>,
    },
  ]

  const itemColumns: ColumnsType<StEnergyItem> = [
    { title: '机台', dataIndex: 'equipmentId', width: 110, render: (v) => (v != null ? `#${v}` : '-') },
    { title: '用电(KWh)', dataIndex: 'powerUsage', width: 110, align: 'right' },
    { title: '用水(吨)', dataIndex: 'waterUsage', width: 100, align: 'right' },
    { title: '用气(m³)', dataIndex: 'gasUsage', width: 100, align: 'right' },
  ]

  return (
    <div className="page">
      <div className="sec-title">车间能耗统计</div>
      <Card size="small" style={{ marginBottom: 12 }}>
        <Form layout="inline" onFinish={(v) => setSearch(cleanParams(v))}>
          <Form.Item name="periodMonth" label="期间"><Input allowClear placeholder="YYYY-MM" style={{ width: 110 }} /></Form.Item>
          <Form.Item name="workshopId" label="车间">
            <Select allowClear placeholder="全部" style={{ width: 150 }} showSearch optionFilterProp="label" options={wsOptions} />
          </Form.Item>
          <Form.Item name="docStatus" label="状态">
            <Select allowClear placeholder="全部" style={{ width: 110 }} options={['编制中', '已提交', '审核通过', '已撤回'].map((s) => ({ label: s, value: s }))} />
          </Form.Item>
          <Form.Item><Button type="primary" htmlType="submit">搜索</Button></Form.Item>
          <Form.Item><Button onClick={() => setSearch({})}>重置</Button></Form.Item>
        </Form>
      </Card>
      <Card size="small">
        <Table rowKey="id" size="small" columns={columns} dataSource={rows} loading={loading} pagination={pagination} />
      </Card>

      <Modal title={detail ? `机台能耗明细 - ${detail.workshop} ${detail.month}` : '机台能耗明细'} open={detail !== null} onCancel={() => setDetail(null)} footer={null} destroyOnClose>
        <Table rowKey="id" size="small" columns={itemColumns} dataSource={detail?.items ?? []} loading={itemsLoading} pagination={false} />
      </Modal>
    </div>
  )
}
