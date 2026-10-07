import { Button, Card, DatePicker, Form, Input, Table } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { usePagedList } from '@/hooks/usePagedList'
import { useProcesses } from '@/hooks/useOptions'
import { cleanParams } from '@/utils/cleanParams'
import type { RepOutput } from '@/api/site'

/** 产量汇报：了批任务被拦截，不可再报产量 */
export default function OutputReport() {
  const { nameMap: procName } = useProcesses()
  const { rows, loading, pagination, setSearch } = usePagedList<RepOutput>('/api/site/output')
  const [form] = Form.useForm()

  const columns: ColumnsType<RepOutput> = [
    { title: '汇报单号', dataIndex: 'outputNo', width: 150, fixed: 'left' },
    { title: '生产批号', dataIndex: 'batchNo', width: 115 },
    { title: '工序', dataIndex: 'processId', width: 90, render: (v) => procName[v] ?? v ?? '-' },
    { title: '产品名称', dataIndex: 'productName', width: 150, ellipsis: true },
    { title: '产量', dataIndex: 'outputQty', width: 90, align: 'right' },
    { title: '回用量', dataIndex: 'reworkQty', width: 85, align: 'right' },
    { title: '回毛量', dataIndex: 'returnWoolQty', width: 85, align: 'right' },
    { title: '回毛率%', dataIndex: 'returnWoolRate', width: 90, align: 'right' },
    { title: '毛重', dataIndex: 'woolQty', width: 85, align: 'right' },
    { title: '汇报人', dataIndex: 'reportUser', width: 90 },
    { title: '生产工', dataIndex: 'prodUsers', width: 110, ellipsis: true },
    { title: '汇报日期', dataIndex: 'reportDate', width: 100 },
    { title: '单位', dataIndex: 'unit', width: 55 },
  ]

  return (
    <div className="page">
      <div className="sec-title">产量汇报</div>
      <Card size="small" style={{ marginBottom: 12 }}>
        <Form
          form={form}
          layout="inline"
          onFinish={(v) => setSearch(cleanParams({
            ...v,
            reportDateBegin: v.range?.[0]?.format('YYYY-MM-DD'),
            reportDateEnd: v.range?.[1]?.format('YYYY-MM-DD'),
            range: undefined,
          }))}
        >
          <Form.Item name="outputNo" label="汇报单号"><Input allowClear placeholder="单号" style={{ width: 130 }} /></Form.Item>
          <Form.Item name="batchNo" label="生产批号"><Input allowClear placeholder="批号" style={{ width: 115 }} /></Form.Item>
          <Form.Item name="range" label="汇报日期"><DatePicker.RangePicker allowClear style={{ width: 220 }} /></Form.Item>
          <Form.Item><Button type="primary" htmlType="submit">搜索</Button></Form.Item>
          <Form.Item><Button onClick={() => { form.resetFields(); setSearch({}) }}>重置</Button></Form.Item>
        </Form>
      </Card>
      <Card size="small">
        <Table rowKey="id" size="small" columns={columns} dataSource={rows} loading={loading} pagination={pagination} scroll={{ x: 1400 }} />
      </Card>
    </div>
  )
}
