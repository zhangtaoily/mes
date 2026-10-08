import { Button, Card, Form, Input, Table } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { usePagedList } from '@/hooks/usePagedList'
import { cleanParams } from '@/utils/cleanParams'
import { pageWmsIn } from '@/api/pack'
import type { PkWmsIn } from '@/api/pack'

/** WMS 入库记录（同步链路只读） */
export default function WmsRecord() {
  const { rows, loading, pagination, setSearch } = usePagedList<PkWmsIn>('/api/pack/trace/wms-in')

  const columns: ColumnsType<PkWmsIn> = [
    { title: '入库批次', dataIndex: 'warehouseBatchNo', width: 130, fixed: 'left' },
    { title: '生产批号', dataIndex: 'batchNo', width: 125 },
    { title: '缸号', dataIndex: 'dyeVatNo', width: 90 },
    { title: '箱子编码', dataIndex: 'boxCode', width: 130 },
    { title: '托盘码', dataIndex: 'palletCode', width: 120 },
    { title: '公重(kg)', dataIndex: 'publicWeight', width: 90, align: 'right' },
    { title: '毛重(kg)', dataIndex: 'grossWeight', width: 90, align: 'right' },
    { title: '皮重(kg)', dataIndex: 'tareWeight', width: 90, align: 'right' },
    { title: '净重(kg)', dataIndex: 'netWeight', width: 90, align: 'right' },
    {
      title: '同步状态', dataIndex: 'wmsStatus', width: 100,
      render: (v) => v === '成功' ? <span style={{ color: '#52c41a' }}>{v}</span> : v ?? '-',
    },
    { title: '入库时间', dataIndex: 'inTime', width: 160 },
  ]

  return (
    <div className="page">
      <div className="sec-title">WMS 入库记录</div>
      <Card size="small" style={{ marginBottom: 12 }}>
        <Form layout="inline" onFinish={(v) => setSearch(cleanParams(v))}>
          <Form.Item name="warehouseBatchNo" label="入库批次"><Input allowClear placeholder="批次" style={{ width: 140 }} /></Form.Item>
          <Form.Item><Button type="primary" htmlType="submit">搜索</Button></Form.Item>
          <Form.Item><Button onClick={() => setSearch({})}>重置</Button></Form.Item>
        </Form>
      </Card>
      <Card size="small">
        <Table rowKey="id" size="small" columns={columns} dataSource={rows} loading={loading} pagination={pagination} scroll={{ x: 1300 }} />
      </Card>
    </div>
  )
}
