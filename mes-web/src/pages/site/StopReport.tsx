import { useState } from 'react'
import { App, Button, Card, DatePicker, Form, Input, InputNumber, Modal, Popconfirm, Select, Table } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import dayjs from 'dayjs'
import { usePagedList } from '@/hooks/usePagedList'
import { cleanParams } from '@/utils/cleanParams'
import { useWorkshops } from '@/hooks/useOptions'
import { useTeams } from '@/hooks/useTeams'
import { getEquipments } from '@/api/plan'
import { pageStops, saveStop, deleteStop } from '@/api/site'
import type { RepStartStop } from '@/api/site'

const STOP_REASONS = ['撞车', '落料', '停机', '其他']

/** 启停汇报（停机记录，设备利用率考核数据源） */
export default function StopReport() {
  const { message } = App.useApp()
  const { nameMap: wsName, options: wsOptions } = useWorkshops()
  const { nameMap: teamName, options: teamOptions } = useTeams()
  const { rows, loading, pagination, setSearch, reload } = usePagedList<RepStartStop>('/api/site/stop')
  const [form] = Form.useForm()
  const [open, setOpen] = useState(false)
  const [eqOptions, setEqOptions] = useState<{ label: string; value: number }[]>([])
  const eqName: Record<number, string> = {}
  eqOptions.forEach((e) => (eqName[e.value] = e.label))

  const loadEquipments = (ws?: number) => {
    if (ws) {
      getEquipments(ws).then((l) => setEqOptions(l.map((e) => ({ label: e.machineNo, value: e.id }))))
    } else {
      setEqOptions([])
    }
  }

  const openForm = () => {
    setOpen(true)
    form.resetFields()
    form.setFieldsValue({ reportDate: dayjs() })
  }

  const columns: ColumnsType<RepStartStop> = [
    { title: '停机编号', dataIndex: 'stopNo', width: 150 },
    { title: '车间', dataIndex: 'workshopId', width: 130, render: (v) => wsName[v] ?? v ?? '-' },
    { title: '机型', dataIndex: 'machineModel', width: 110 },
    { title: '机台', dataIndex: 'equipmentId', width: 110, render: (v) => eqName[v] ?? v ?? '-' },
    { title: '汇报班组', dataIndex: 'teamId', width: 100, render: (v) => teamName[v] ?? v ?? '-' },
    { title: '汇报人', dataIndex: 'reportUser', width: 90 },
    { title: '汇报日期', dataIndex: 'reportDate', width: 110 },
    { title: '停机时长(h)', dataIndex: 'stopDuration', width: 100, align: 'right' },
    { title: '停机原因', dataIndex: 'stopReason', width: 90 },
    { title: '备注', dataIndex: 'remark', width: 140, ellipsis: true },
    {
      title: '操作', width: 80,
      render: (_, r) => (
        <Popconfirm title="确认删除该停机记录？" onConfirm={async () => { await deleteStop(r.id); message.success('已删除'); reload() }}>
          <a style={{ color: '#f56c6c' }}>删除</a>
        </Popconfirm>
      ),
    },
  ]

  return (
    <div className="page">
      <div className="sec-title">启停汇报</div>
      <Card size="small" style={{ marginBottom: 12 }}>
        <Form layout="inline" onFinish={(v) => setSearch(cleanParams({
          ...v,
          reportDateBegin: v.range?.[0]?.format('YYYY-MM-DD'),
          reportDateEnd: v.range?.[1]?.format('YYYY-MM-DD'),
          range: undefined,
        }))}>
          <Form.Item name="workshopId" label="车间">
            <Select allowClear placeholder="全部" style={{ width: 150 }} showSearch optionFilterProp="label" options={wsOptions} onChange={loadEquipments} />
          </Form.Item>
          <Form.Item name="equipmentId" label="机台">
            <Select allowClear placeholder="全部" style={{ width: 130 }} showSearch optionFilterProp="label" options={eqOptions} />
          </Form.Item>
          <Form.Item name="stopNo" label="停机编号"><Input allowClear placeholder="ST..." style={{ width: 130 }} /></Form.Item>
          <Form.Item name="range" label="汇报日期">
            <DatePicker.RangePicker style={{ width: 230 }} />
          </Form.Item>
          <Form.Item><Button type="primary" htmlType="submit">搜索</Button></Form.Item>
          <Form.Item><Button onClick={() => setSearch({})}>重置</Button></Form.Item>
        </Form>
      </Card>
      <Card size="small">
        <div style={{ marginBottom: 10 }}>
          <Button type="primary" onClick={openForm}>新增停机记录</Button>
          <span className="muted" style={{ marginLeft: 12, fontSize: 12 }}>新增自动取号 ST 前缀</span>
        </div>
        <Table rowKey="id" size="small" columns={columns} dataSource={rows} loading={loading} pagination={pagination} />
      </Card>

      <Modal title="新增停机记录" open={open} onCancel={() => setOpen(false)} onOk={() => form.submit()} destroyOnClose>
        <Form form={form} layout="vertical" style={{ marginTop: 12 }} onFinish={async (v) => {
          await saveStop({ ...v, reportDate: v.reportDate?.format('YYYY-MM-DD') })
          message.success('保存成功，编号后端自动取号')
          setOpen(false); reload()
        }}>
          <Form.Item name="workshopId" label="车间" rules={[{ required: true }]}>
            <Select showSearch optionFilterProp="label" options={wsOptions} onChange={loadEquipments} />
          </Form.Item>
          <Form.Item name="equipmentId" label="机台">
            <Select allowClear showSearch optionFilterProp="label" options={eqOptions} />
          </Form.Item>
          <Form.Item name="teamId" label="汇报班组">
            <Select allowClear showSearch optionFilterProp="label" options={teamOptions} />
          </Form.Item>
          <Form.Item name="reportUser" label="汇报人" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="reportDate" label="汇报日期" rules={[{ required: true }]}>
            <DatePicker style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="stopDuration" label="停机时长(小时)" rules={[{ required: true }]}>
            <InputNumber style={{ width: '100%' }} min={0} precision={2} />
          </Form.Item>
          <Form.Item name="stopReason" label="停机原因" rules={[{ required: true }]}>
            <Select options={STOP_REASONS.map((r) => ({ label: r, value: r }))} />
          </Form.Item>
          <Form.Item name="remark" label="备注"><Input.TextArea rows={2} /></Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
