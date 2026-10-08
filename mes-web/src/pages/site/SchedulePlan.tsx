import { useEffect, useState } from 'react'
import { App, Button, Card, DatePicker, Form, Input, Modal, Popconfirm, Select, Table, TimePicker } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import dayjs from 'dayjs'
import { usePagedList } from '@/hooks/usePagedList'
import { cleanParams } from '@/utils/cleanParams'
import { useWorkshops } from '@/hooks/useOptions'
import { useTeams } from '@/hooks/useTeams'
import { pageUsers } from '@/api/sys'
import { pageShifts, saveShift, deleteShift, getShiftPersons, addShiftPersons, deleteShiftPerson } from '@/api/site'
import type { StShift, StShiftPerson } from '@/api/site'

const SHIFT_NAMES = ['常日班', '甲班', '乙班', '丙班']

/** 车间排班（班次 + 排班人员） */
export default function SchedulePlan() {
  const { message } = App.useApp()
  const { nameMap: wsName, options: wsOptions } = useWorkshops()
  const { nameMap: teamName, options: teamOptions } = useTeams()
  const { rows, loading, pagination, setSearch, reload } = usePagedList<StShift>('/api/site/schedule')
  const [open, setOpen] = useState(false)
  const [form] = Form.useForm()
  const [shiftId, setShiftId] = useState<number | null>(null)
  const [persons, setPersons] = useState<StShiftPerson[]>([])
  const [personsLoading, setPersonsLoading] = useState(false)
  const [addForm] = Form.useForm()
  const [users, setUsers] = useState<{ label: string; value: number; empNo?: string }[]>([])

  useEffect(() => {
    pageUsers({ pageNum: 1, pageSize: 200 })
      .then((p) => setUsers((p.rows ?? []).map((u) => ({ label: u.userName, value: u.id, empNo: u.empNo }))))
      .catch(() => setUsers([]))
  }, [])

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
    { title: '车间', dataIndex: 'workshopId', width: 120, render: (v) => wsName[v] ?? v ?? '-' },
    { title: '班组', dataIndex: 'teamId', width: 110, render: (v) => teamName[v] ?? v ?? '-' },
    { title: '班次', dataIndex: 'shiftName', width: 90 },
    { title: '上班时间', dataIndex: 'onDutyTime', width: 90 },
    { title: '下班时间', dataIndex: 'offDutyTime', width: 90 },
    { title: '开始日期', dataIndex: 'startDate', width: 110 },
    { title: '结束日期', dataIndex: 'endDate', width: 110 },
    {
      title: '操作', width: 160,
      render: (_, r) => (
        <>
          <a onClick={() => loadPersons(r.id)}>人员</a>
          <a style={{ margin: '0 8px' }} onClick={() => {
            setOpen(true)
            form.resetFields()
            form.setFieldsValue({
              id: r.id,
              workshopId: r.workshopId,
              teamId: r.teamId,
              shiftName: r.shiftName,
              range: r.startDate && r.endDate ? [dayjs(r.startDate), dayjs(r.endDate)] : undefined,
              onDuty: r.onDutyTime ? dayjs(r.onDutyTime, 'HH:mm') : undefined,
              offDuty: r.offDutyTime ? dayjs(r.offDutyTime, 'HH:mm') : undefined,
            })
          }}>编辑</a>
          <Popconfirm title="确认删除该排班（含人员）？" onConfirm={async () => { await deleteShift(r.id); message.success('已删除'); reload() }}>
            <a style={{ color: '#f56c6c' }}>删除</a>
          </Popconfirm>
        </>
      ),
    },
  ]

  const personColumns: ColumnsType<StShiftPerson> = [
    { title: '工号', dataIndex: 'empNo', width: 90 },
    { title: '姓名', dataIndex: 'userName', width: 110 },
    { title: '机台', dataIndex: 'equipmentId', width: 100, render: (v) => v ?? '-' },
    {
      title: '操作', width: 80,
      render: (_, p) => (
        <Popconfirm title="移除该人员？" onConfirm={async () => { await deleteShiftPerson(p.id); message.success('已移除'); if (p.shiftId) await loadPersons(p.shiftId) }}>
          <a style={{ color: '#f56c6c' }}>移除</a>
        </Popconfirm>
      ),
    },
  ]

  return (
    <div className="page">
      <div className="sec-title">车间排班</div>
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
        <div style={{ marginBottom: 10 }}>
          <Button type="primary" onClick={() => { setOpen(true); form.resetFields() }}>新增排班</Button>
        </div>
        <Table rowKey="id" size="small" columns={columns} dataSource={rows} loading={loading} pagination={pagination} />
      </Card>

      <Modal title={form.getFieldValue('id') ? '编辑排班' : '新增排班'} open={open} onCancel={() => setOpen(false)} onOk={() => form.submit()} destroyOnClose>
        <Form form={form} layout="vertical" style={{ marginTop: 12 }} onFinish={async (v) => {
          await saveShift({
            id: v.id,
            workshopId: v.workshopId,
            teamId: v.teamId,
            shiftName: v.shiftName,
            onDutyTime: v.onDuty?.format('HH:mm'),
            offDutyTime: v.offDuty?.format('HH:mm'),
            startDate: v.range?.[0]?.format('YYYY-MM-DD'),
            endDate: v.range?.[1]?.format('YYYY-MM-DD'),
          })
          message.success('保存成功')
          setOpen(false); reload()
        }}>
          <Form.Item name="id" hidden><Input /></Form.Item>
          <Form.Item name="workshopId" label="车间" rules={[{ required: true }]}>
            <Select showSearch optionFilterProp="label" options={wsOptions} />
          </Form.Item>
          <Form.Item name="teamId" label="班组" rules={[{ required: true }]}>
            <Select showSearch optionFilterProp="label" options={teamOptions} />
          </Form.Item>
          <Form.Item name="shiftName" label="班次" rules={[{ required: true }]}>
            <Select options={SHIFT_NAMES.map((s) => ({ label: s, value: s }))} />
          </Form.Item>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', columnGap: 12 }}>
            <Form.Item name="onDuty" label="上班时间"><TimePicker format="HH:mm" style={{ width: '100%' }} /></Form.Item>
            <Form.Item name="offDuty" label="下班时间"><TimePicker format="HH:mm" style={{ width: '100%' }} /></Form.Item>
          </div>
          <Form.Item name="range" label="生效区间">
            <DatePicker.RangePicker style={{ width: '100%' }} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal title="排班人员" open={shiftId !== null} onCancel={() => setShiftId(null)} footer={null} destroyOnClose>
        <Form form={addForm} layout="inline" style={{ marginBottom: 10 }} onFinish={async (v) => {
          if (!shiftId) return
          await addShiftPersons(shiftId, [{ userId: v.userId, userName: users.find((u) => u.value === v.userId)?.label, empNo: users.find((u) => u.value === v.userId)?.empNo, equipmentId: v.equipmentId }])
          message.success('已添加')
          addForm.resetFields()
          await loadPersons(shiftId)
        }}>
          <Form.Item name="userId" rules={[{ required: true }]} style={{ marginBottom: 0 }}>
            <Select showSearch optionFilterProp="label" placeholder="选择人员" style={{ width: 160 }} options={users} />
          </Form.Item>
          <Form.Item style={{ marginBottom: 0, marginLeft: 8 }}>
            <Button type="primary" htmlType="submit">添加人员</Button>
          </Form.Item>
        </Form>
        <Table rowKey="id" size="small" columns={personColumns} dataSource={persons} loading={personsLoading} pagination={false} />
      </Modal>
    </div>
  )
}
