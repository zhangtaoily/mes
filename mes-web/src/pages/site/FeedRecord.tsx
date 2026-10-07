import { Button, Card, Form, Input, Select, Table } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { usePagedList } from '@/hooks/usePagedList'
import { useWorkshops } from '@/hooks/useOptions'
import { cleanParams } from '@/utils/cleanParams'
import type { MtFeed } from '@/api/site'
import { BoolTag } from '@/components/StatusTag'

/** 投料记录：完毕封账后不可再投不可删 */
export default function FeedRecord() {
  const { options: wsOptions, nameMap: wsName } = useWorkshops()
  const { rows, loading, pagination, setSearch } = usePagedList<MtFeed>('/api/site/feed-record')
  const [form] = Form.useForm()

  const columns: ColumnsType<MtFeed> = [
    { title: '投料单号', dataIndex: 'feedNo', width: 150, fixed: 'left' },
    { title: '任务编号', dataIndex: 'taskNo', width: 140 },
    { title: '车间', dataIndex: 'workshopId', width: 130, render: (v) => wsName[v] ?? v ?? '-' },
    { title: '生产批号', dataIndex: 'batchNo', width: 120 },
    { title: '原料', dataIndex: 'rawDetail', width: 170, ellipsis: true },
    { title: '投料量', dataIndex: 'feedQty', width: 90, align: 'right' },
    { title: '是否完毕', dataIndex: 'isFinish', width: 90, render: (v) => <BoolTag value={v} labels={['未完毕', '已完毕']} /> },
    { title: '投料人', dataIndex: 'feedBy', width: 90 },
    { title: '投料时间', dataIndex: 'feedTime', width: 150 },
  ]

  return (
    <div className="page">
      <div className="sec-title">投料记录</div>
      <Card size="small" style={{ marginBottom: 12 }}>
        <Form form={form} layout="inline" onFinish={(v) => setSearch(cleanParams(v))}>
          <Form.Item name="feedNo" label="投料单号"><Input allowClear placeholder="单号" style={{ width: 130 }} /></Form.Item>
          <Form.Item name="batchNo" label="生产批号"><Input allowClear placeholder="批号" style={{ width: 115 }} /></Form.Item>
          <Form.Item name="taskNo" label="任务编号"><Input allowClear placeholder="PT..." style={{ width: 125 }} /></Form.Item>
          <Form.Item name="workshopId" label="车间">
            <Select allowClear placeholder="全部" style={{ width: 145 }} options={wsOptions} />
          </Form.Item>
          <Form.Item name="isFinish" label="完毕状态">
            <Select allowClear placeholder="全部" style={{ width: 100 }} options={[{ label: '未完毕', value: 0 }, { label: '已完毕', value: 1 }]} />
          </Form.Item>
          <Form.Item><Button type="primary" htmlType="submit">搜索</Button></Form.Item>
          <Form.Item><Button onClick={() => { form.resetFields(); setSearch({}) }}>重置</Button></Form.Item>
        </Form>
      </Card>
      <Card size="small">
        <Table rowKey="id" size="small" columns={columns} dataSource={rows} loading={loading} pagination={pagination} scroll={{ x: 1200 }} />
      </Card>
    </div>
  )
}
