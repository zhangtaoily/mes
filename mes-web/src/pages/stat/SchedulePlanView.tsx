import { useState } from 'react'
import { Button, Card, Form, Modal, Select, Table } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { usePagedList } from '@/hooks/usePagedList'
import { cleanParams } from '@/utils/cleanParams'
import { useWorkshops } from '@/hooks/useOptions'
import { useTeams } from '@/hooks/useTeams'
import { getShiftPersons } from '@/api/site'
import type { StShift, StShiftPerson } from '@/api/site'

const SHIFT_NAMES = ['常日班', '甲班', '乙班', '丙班']

/** 车间排班统计（只读视图，含人员查看） */
export default function SchedulePlanView() {
  const { nameMap: wsName, options: wsOptions } = useWorkshops()
  const { nameMap: teamName, options: teamOptions } = useTeams()
  const { rows, loading, pagination, setSearch } = usePagedList<StShift>('/api/site/schedule')
  const [shiftId, setShiftId] = useState<number | null>(null)
  const [persons, setPersons] = useState<StShiftPerson[]>([])
  const [personsLoading, setPersonsLoading] = useState(false)

  const loadPersons = async (id: number) => {
    setShiftId(id)
    setPersonsLoading(true)
    try {
      setPersons(await getShiftPersons(id))
    } finally {
      setPersonsLoading(false)
    }
  }

  const columns: ColumnsType<StShift> = [
    { title: '车间', dataIndex: 'workshopId', width: 130, render: (v) => wsName[v] ?? v ?? '-' },
    { title: '班组', dataIndex: 'teamId', width: 120, render: (v) => teamName[v] ?? v ?? '-' },
    { title: '班次', dataIndex: 'shiftName', width: 100 },
    { title: '上班时间', dataIndex: 'onDutyTime', width: 100 },
    { title: '下班时间', dataIndex: 'offDutyTime', width: 100 },
    { title: '开始日期', dataIndex: 'startDate', width: 120 },
    { title: '结束日期', dataIndex: 'endDate', width: 120 },
    {
      title: '操作', width: 80,
      render: (_, r) => <a onClick={() => loadPersons(r.id)}>人员</a>,
    },
  ]

  const personColumns: ColumnsType<StShiftPerson> = [
    { title: '工号', dataIndex: 'empNo', width: 100 },
    { title: '姓名', dataIndex: 'userName', width: 120 },
    { title: '机台', dataIndex: 'equipmentId', width: 110, render: (v) => v ?? '-' },
  ]

  return (
    <div className="page">
      <div className="sec-title">车间排班统计</div>
      <Card size="small" style={{ marginBottom: 12 }}>
        <Form layout="inline" onFinish={(v) => setSearch(cleanParams(v))}>
          <Form.Item name="workshopId" label="车间">
            <Select allowClear placeholder="全部" style={{ width: 150 }} showSearch optionFilterProp="label" options={wsOptions} />
          </Form.Item>
          <Form.Item name="teamId" label="班组">
            <Select allowClear placeholder="全部" style={{ width: 130 }} showSearch optionFilterProp="label" options={teamOptions} />
          </Form.Item>
          <Form.Item name="shiftName" label="班次">
            <Select allowClear placeholder="全部" style={{ width: 110 }} options={SHIFT_NAMES.map((s) => ({ label: s, value: s }))} />
          </Form.Item>
          <Form.Item><Button type="primary" htmlType="submit">搜索</Button></Form.Item>
          <Form.Item><Button onClick={() => setSearch({})}>重置</Button></Form.Item>
        </Form>
      </Card>
      <Card size="small">
        <Table rowKey="id" size="small" columns={columns} dataSource={rows} loading={loading} pagination={pagination} />
      </Card>

      <Modal title="排班人员" open={shiftId !== null} onCancel={() => setShiftId(null)} footer={null} destroyOnClose>
        <Table rowKey="id" size="small" columns={personColumns} dataSource={persons} loading={personsLoading} pagination={false} />
      </Modal>
    </div>
  )
}
