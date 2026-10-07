import { Card, Form, Input, Select, Table, Button } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { usePagedList } from '@/hooks/usePagedList'
import { cleanParams } from '@/utils/cleanParams'
import type { ErpK3Order } from '@/api/plan'
import { BoolTag } from '@/components/StatusTag'

/** K3订单查看（只读镜像） */
export default function K3Order() {
  const { rows, loading, pagination, setSearch } = usePagedList<ErpK3Order>('/api/plan/erp-orders')
  const [form] = Form.useForm()

  const columns: ColumnsType<ErpK3Order> = [
    { title: '序号', width: 60, render: (_, __, i) => (pagination.current! - 1) * pagination.pageSize! + i + 1 },
    { title: '订单编号', dataIndex: 'orderNo', width: 130 },
    { title: '订单日期', dataIndex: 'orderDate', width: 100 },
    { title: '车间', dataIndex: 'workshopName', width: 130 },
    { title: '生产批号', dataIndex: 'batchNo', width: 110 },
    { title: '产品名称', dataIndex: 'productName', width: 150, ellipsis: true },
    { title: '色号', dataIndex: 'colorNo', width: 90 },
    { title: '实纺支数', dataIndex: 'spinCount', width: 80 },
    { title: '订单量', dataIndex: 'k3Qty', width: 90, align: 'right' },
    { title: '已引入量', dataIndex: 'importedQty', width: 90, align: 'right' },
    { title: '原料明细', dataIndex: 'materialDetail', width: 160, ellipsis: true },
    { title: '客户', dataIndex: 'customerName', width: 120, ellipsis: true },
    { title: '销售部门', dataIndex: 'saleDept', width: 100 },
    { title: '交期', dataIndex: 'planEndDate', width: 100 },
    { title: '完结', dataIndex: 'isClosed', width: 70, render: (v) => <BoolTag value={v} /> },
  ]

  return (
    <div className="page">
      <div className="sec-title">K3订单查看</div>
      <Card size="small" style={{ marginBottom: 12 }}>
        <Form
          form={form}
          layout="inline"
          onFinish={(v) => setSearch(cleanParams(v))}
          initialValues={{}}
        >
          <Form.Item name="orderNo" label="订单编号"><Input allowClear placeholder="FBDC..." style={{ width: 150 }} /></Form.Item>
          <Form.Item name="batchNo" label="生产批号"><Input allowClear placeholder="批号" style={{ width: 130 }} /></Form.Item>
          <Form.Item name="productName" label="产品名称"><Input allowClear placeholder="产品" style={{ width: 150 }} /></Form.Item>
          <Form.Item name="isClosed" label="完结状态">
            <Select allowClear style={{ width: 100 }} options={[{ label: '未完结', value: 0 }, { label: '已完结', value: 1 }]} />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit">搜索</Button>
          </Form.Item>
          <Form.Item>
            <Button onClick={() => { form.resetFields(); setSearch({}) }}>重置</Button>
          </Form.Item>
        </Form>
      </Card>
      <Card size="small">
        <Table rowKey="id" size="small" columns={columns} dataSource={rows} loading={loading} pagination={pagination} scroll={{ x: 1600 }} />
      </Card>
    </div>
  )
}
