import { Button, Card, Form, Input, Select, Table } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { usePagedList } from '@/hooks/usePagedList'
import { cleanParams } from '@/utils/cleanParams'
import { useWorkshops } from '@/hooks/useOptions'
import type { StAttendance } from '@/api/site'

/** 车间出勤统计（只读视图，数据源 /api/site/attendance） */
export default function AttendanceView() {
  const { nameMap: wsName, options: wsOptions } = useWorkshops()
  const { rows, loading, pagination, setSearch } = usePagedList<StAttendance>('/api/site/attendance')

  const columns: ColumnsType<StAttendance> = [
    { title: '期间', dataIndex: 'periodMonth', width: 90 },
    { title: '车间', dataIndex: 'workshopId', width: 120, render: (v) => wsName[v] ?? v ?? '-' },
    { title: '开工天数', dataIndex: 'workDays', width: 90, align: 'right' },
    { title: '管理人员', dataIndex: 'managerCount', width: 90, align: 'right' },
    { title: '常日班辅助', dataIndex: 'dayAuxCount', width: 95, align: 'right' },
    { title: '一线员工', dataIndex: 'frontlineCount', width: 90, align: 'right' },
    { title: '质检组出勤', dataIndex: 'qcCount', width: 95, align: 'right' },
    { title: '总出勤', dataIndex: 'totalCount', width: 80, align: 'right' },
    { title: '折算出勤', dataIndex: 'convertCount', width: 90, align: 'right' },
    {
      title: '状态', dataIndex: 'docStatus', width: 80,
      render: (v) => v === '已提交' ? <span style={{ color: '#1677ff' }}>{v}</span> : v ?? '-',
    },
  ]

  return (
    <div className="page">
      <div className="sec-title">车间出勤统计</div>
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
    </div>
  )
}
