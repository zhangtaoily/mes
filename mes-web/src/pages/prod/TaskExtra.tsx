import { useEffect, useState } from 'react'
import { App, Button, Card, Form, Input, InputNumber, Modal, Select, Table } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { usePagedList } from '@/hooks/usePagedList'
import { cleanParams } from '@/utils/cleanParams'
import { pageTasks } from '@/api/plan'
import type { ProdTask } from '@/api/plan'
import { pageTaskExtras, saveTaskExtra } from '@/api/prod'
import type { TaskExtra } from '@/api/prod'
import { BoolTag } from '@/components/StatusTag'

/** 生产任务其它信息（环境与工艺开关，1:1 挂任务） */
export default function TaskExtraPage() {
  const { message } = App.useApp()
  const { rows, loading, pagination, setSearch, reload } = usePagedList<TaskExtra>('/api/prod/task-extras')
  const [form] = Form.useForm()
  const [editForm] = Form.useForm()
  const [open, setOpen] = useState(false)
  const [tasks, setTasks] = useState<ProdTask[]>([])

  useEffect(() => {
    pageTasks({ pageNum: 1, pageSize: 200 }).then((r) => setTasks(r.rows ?? [])).catch(() => setTasks([]))
  }, [])

  const openForm = async (row?: TaskExtra) => {
    editForm.resetFields()
    if (row) {
      editForm.setFieldsValue(row)
    } else {
      editForm.setFieldsValue({
        wrapNeedle: 0, wrapRing: 0, wrapBalloon: 0, wrapRoller: 0, wrapRoller2: 0, talcPowder: 0,
      })
    }
    setOpen(true)
  }

  const submit = async (v: Partial<TaskExtra>) => {
    await saveTaskExtra(v)
    message.success('已保存')
    setOpen(false)
    reload()
  }

  const yn = (title: string, key: keyof TaskExtra) => ({
    title, dataIndex: key, width: 80, render: (v: number) => <BoolTag value={v} />,
  })
  const columns: ColumnsType<TaskExtra> = [
    {
      title: '任务', dataIndex: 'taskId', width: 160, fixed: 'left',
      render: (v) => tasks.find((t) => t.id === v)?.taskNo ?? v,
    },
    { title: '生产批号', dataIndex: 'batchNo', width: 115, render: (v) => v ?? '-' },
    { title: '车间', dataIndex: 'workshopName', width: 120, render: (v) => v ?? '-' },
    { title: '温度', dataIndex: 'temperature', width: 70, align: 'right' },
    { title: '湿度', dataIndex: 'humidity', width: 70, align: 'right' },
    { title: '原料含油', dataIndex: 'rawOil', width: 85, align: 'right' },
    { title: '成品含油', dataIndex: 'productOil', width: 85, align: 'right' },
    { title: '机型', dataIndex: 'machineModel', width: 100, render: (v) => v ?? '-' },
    yn('绕针板', 'wrapNeedle'),
    yn('绕圆梳', 'wrapRing'),
    yn('绕气泡', 'wrapBalloon'),
    yn('绕皮辊', 'wrapRoller'),
    yn('绕罗拉', 'wrapRoller2'),
    yn('撒滑石粉', 'talcPowder'),
    { title: '类型', dataIndex: 'extraType', width: 90, render: (v) => v ?? '-' },
    { title: '备注', dataIndex: 'remark', width: 150, ellipsis: true, render: (v) => v ?? '-' },
    {
      title: '操作', width: 70, fixed: 'right',
      render: (_, r) => <a onClick={() => openForm(r)}>编辑</a>,
    },
  ]

  const ynItem = (label: string, name: keyof TaskExtra) => (
    <Form.Item name={name} label={label}>
      <Select options={[{ label: '是', value: 1 }, { label: '否', value: 0 }]} />
    </Form.Item>
  )

  return (
    <div className="page">
      <div className="sec-title">生产任务其它信息</div>
      <Card size="small" style={{ marginBottom: 12 }}>
        <Form form={form} layout="inline" onFinish={(v) => setSearch(cleanParams(v))}>
          <Form.Item name="batchNo" label="生产批号"><Input allowClear placeholder="批号" style={{ width: 120 }} /></Form.Item>
          <Form.Item><Button type="primary" htmlType="submit">搜索</Button></Form.Item>
          <Form.Item><Button onClick={() => { form.resetFields(); setSearch({}) }}>重置</Button></Form.Item>
          <Form.Item><Button type="primary" onClick={() => openForm()}>新增/覆盖</Button></Form.Item>
        </Form>
      </Card>
      <Card size="small">
        <Table rowKey="id" size="small" columns={columns} dataSource={rows} loading={loading} pagination={pagination} scroll={{ x: 1800 }} />
      </Card>

      <Modal
        title="任务其它信息（环境与工艺开关）"
        open={open}
        onCancel={() => setOpen(false)}
        onOk={() => editForm.submit()}
        width={680}
        destroyOnClose
      >
        <Form form={editForm} layout="vertical" onFinish={submit}>
          <Form.Item name="taskId" label="生产任务" rules={[{ required: true, message: '请选择任务' }]}>
            <Select
              showSearch
              optionFilterProp="label"
              placeholder="选择任务（已存在则覆盖更新）"
              options={tasks.map((t) => ({ label: `${t.taskNo}（${t.batchNo ?? '-'}）`, value: t.id }))}
            />
          </Form.Item>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', columnGap: 12 }}>
            <Form.Item name="batchNo" label="生产批号"><Input /></Form.Item>
            <Form.Item name="workshopName" label="车间名称"><Input /></Form.Item>
            <Form.Item name="machineModel" label="机型"><Input /></Form.Item>
            <Form.Item name="temperature" label="温度"><InputNumber style={{ width: '100%' }} /></Form.Item>
            <Form.Item name="humidity" label="湿度"><InputNumber style={{ width: '100%' }} /></Form.Item>
            <Form.Item name="extraType" label="类型"><Input placeholder="如 前纺" /></Form.Item>
            <Form.Item name="rawOil" label="原料含油"><InputNumber style={{ width: '100%' }} /></Form.Item>
            <Form.Item name="productOil" label="成品含油"><InputNumber style={{ width: '100%' }} /></Form.Item>
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr 1fr 1fr 1fr', columnGap: 10 }}>
            {ynItem('绕针板', 'wrapNeedle')}
            {ynItem('绕圆梳', 'wrapRing')}
            {ynItem('绕气泡', 'wrapBalloon')}
            {ynItem('绕皮辊', 'wrapRoller')}
            {ynItem('绕罗拉', 'wrapRoller2')}
            {ynItem('撒滑石粉', 'talcPowder')}
          </div>
          <Form.Item name="remark" label="备注"><Input.TextArea rows={2} /></Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
