import { Button, Card, Form, Input, Select, Table, Tag } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { usePagedList } from '@/hooks/usePagedList'
import { cleanParams } from '@/utils/cleanParams'
import type { StIssue } from '@/api/site'

const RECTIFY_STATUS = ['未整改', '整改中', '已整改', '已闭环']

/** 车间现场问题反馈统计（只读视图） */
export default function IssueView() {
  const { rows, loading, pagination, setSearch } = usePagedList<StIssue>('/api/site/issue')

  const statusColor = (v?: string) =>
    v === '已闭环' ? 'green' : v === '已整改' ? 'blue' : v === '整改中' ? 'orange' : 'red'

  const columns: ColumnsType<StIssue> = [
    { title: '检查日期', dataIndex: 'checkDate', width: 110 },
    { title: '检查人', dataIndex: 'checkUser', width: 100 },
    { title: '问题所在', dataIndex: 'issueLocation', width: 180, ellipsis: true },
    { title: '问题描述', dataIndex: 'issueDesc', ellipsis: true },
    {
      title: '整改状态', dataIndex: 'rectifyStatus', width: 100,
      render: (v) => v ? <Tag color={statusColor(v)}>{v}</Tag> : '-',
    },
  ]

  return (
    <div className="page">
      <div className="sec-title">车间现场问题反馈统计</div>
      <Card size="small" style={{ marginBottom: 12 }}>
        <Form layout="inline" onFinish={(v) => setSearch(cleanParams(v))}>
          <Form.Item name="checkUser" label="检查人"><Input allowClear placeholder="检查人" style={{ width: 110 }} /></Form.Item>
          <Form.Item name="issueLocation" label="问题所在"><Input allowClear placeholder="位置" style={{ width: 140 }} /></Form.Item>
          <Form.Item name="rectifyStatus" label="整改状态">
            <Select allowClear placeholder="全部" style={{ width: 110 }} options={RECTIFY_STATUS.map((s) => ({ label: s, value: s }))} />
          </Form.Item>
          <Form.Item><Button type="primary" htmlType="submit">搜索</Button></Form.Item>
          <Form.Item><Button onClick={() => setSearch({})}>重置</Button></Form.Item>
        </Form>
      </Card>
      <Card size="small">
        <Table rowKey="id" size="small" columns={columns} dataSource={rows} loading={loading} pagination={pagination} />
      </Card>
    </div>
  )
}
