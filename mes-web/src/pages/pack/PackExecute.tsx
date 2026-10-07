import { Button, Card, Form, Input, Table } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { usePagedList } from '@/hooks/usePagedList'
import { cleanParams } from '@/utils/cleanParams'
import type { PkExecute } from '@/api/pack'
import { BoolTag } from '@/components/StatusTag'

/** 包装执行：逐箱称重汇总（五列重量恒等式由后端校验） */
export default function PackExecute() {
  const { rows, loading, pagination, setSearch } = usePagedList<PkExecute>('/api/pack/executes')
  const [form] = Form.useForm()

  const columns: ColumnsType<PkExecute> = [
    { title: '执行单号', dataIndex: 'executeNo', width: 140, fixed: 'left' },
    { title: '包装批号', dataIndex: 'packBatchNo', width: 130 },
    { title: '入库批号', dataIndex: 'warehouseBatchNo', width: 120 },
    { title: '缸号', dataIndex: 'dyeVatNo', width: 90 },
    { title: '产品名称', dataIndex: 'productName', width: 150, ellipsis: true },
    { title: '色号', dataIndex: 'colorNo', width: 85 },
    { title: '规格', dataIndex: 'productSpec', width: 100 },
    { title: '箱数', dataIndex: 'boxCount', width: 70, align: 'right' },
    { title: '筒子数', dataIndex: 'sumTube', width: 80, align: 'right' },
    { title: '毛重合计', dataIndex: 'sumGrossWeight', width: 95, align: 'right' },
    { title: '净重合计', dataIndex: 'sumNetWeight', width: 95, align: 'right' },
    { title: '公重合计', dataIndex: 'sumPublicWeight', width: 95, align: 'right' },
    { title: '标准回潮', dataIndex: 'moistureStandard', width: 85, align: 'right' },
    { title: '实测回潮', dataIndex: 'moistureActual', width: 85, align: 'right' },
    { title: '了包', dataIndex: 'isPacked', width: 75, render: (v) => <BoolTag value={v} labels={['称重中', '已了包']} /> },
  ]

  return (
    <div className="page">
      <div className="sec-title">包装执行</div>
      <Card size="small" style={{ marginBottom: 12 }}>
        <Form form={form} layout="inline" onFinish={(v) => setSearch(cleanParams(v))}>
          <Form.Item name="executeNo" label="执行单号"><Input allowClear placeholder="PC..." style={{ width: 130 }} /></Form.Item>
          <Form.Item name="packBatchNo" label="包装批号"><Input allowClear placeholder="批号" style={{ width: 120 }} /></Form.Item>
          <Form.Item name="warehouseBatchNo" label="入库批号"><Input allowClear placeholder="入库批号" style={{ width: 120 }} /></Form.Item>
          <Form.Item><Button type="primary" htmlType="submit">搜索</Button></Form.Item>
          <Form.Item><Button onClick={() => { form.resetFields(); setSearch({}) }}>重置</Button></Form.Item>
        </Form>
      </Card>
      <Card size="small">
        <Table rowKey="id" size="small" columns={columns} dataSource={rows} loading={loading} pagination={pagination} scroll={{ x: 1600 }} />
      </Card>
    </div>
  )
}
