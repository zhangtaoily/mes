import { useEffect, useState } from 'react'
import { App, Button, Card, DatePicker, Form, Input, InputNumber, Modal, Popconfirm, Select, Table } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import dayjs from 'dayjs'
import type { Dayjs } from 'dayjs'
import { usePagedList } from '@/hooks/usePagedList'
import { cleanParams } from '@/utils/cleanParams'
import { pageTasks } from '@/api/plan'
import type { ProdTask } from '@/api/plan'
import { pageTaskChanges, saveTaskChange, submitTaskChange, deleteTaskChange, getTaskBaseline } from '@/api/prod'
import type { TaskChange } from '@/api/prod'
import StatusTag from '@/components/StatusTag'

const DOC_STATUS = ['编制中', '已提交'].map((s) => ({ label: s, value: s }))
type FormValues = Partial<Omit<TaskChange, 'newDeliveryDate'>> & { newDeliveryDate?: Dayjs }

/** 任务变更：编制中→已提交（无审批流），提交时新值回写原任务 */
export default function TaskChangePage() {
  const { message } = App.useApp()
  const { rows, loading, pagination, setSearch, reload } = usePagedList<TaskChange>('/api/prod/task-changes')
  const [form] = Form.useForm()
  const [editing, setEditing] = useState<TaskChange | null>(null)
  const [open, setOpen] = useState(false)
  const [tasks, setTasks] = useState<ProdTask[]>([])
  const [baseline, setBaseline] = useState<ProdTask | null>(null)

  useEffect(() => {
    pageTasks({ pageNum: 1, pageSize: 200 }).then((r) => setTasks(r.rows ?? [])).catch(() => setTasks([]))
  }, [])

  const openForm = async (row?: TaskChange) => {
    setEditing(row ?? null)
    form.resetFields()
    setBaseline(null)
    if (row) {
      form.setFieldsValue({ ...row, newDeliveryDate: row.newDeliveryDate ? dayjs(row.newDeliveryDate) : undefined })
      getTaskBaseline(row.taskId).then(setBaseline).catch(() => setBaseline(null))
    }
    setOpen(true)
  }

  const submit = async (v: FormValues) => {
    const body: Partial<TaskChange> = {
      ...v,
      id: editing?.id,
      docStatus: editing?.docStatus,
      newDeliveryDate: v.newDeliveryDate ? v.newDeliveryDate.format('YYYY-MM-DD') : undefined,
    }
    await saveTaskChange(body)
    message.success('已保存')
    setOpen(false)
    reload()
  }

  const columns: ColumnsType<TaskChange> = [
    { title: '变更单号', dataIndex: 'changeNo', width: 150, fixed: 'left' },
    { title: '状态', dataIndex: 'docStatus', width: 85, render: (v) => <StatusTag status={v} /> },
    {
      title: '原任务', dataIndex: 'taskId', width: 160,
      render: (v) => tasks.find((t) => t.id === v)?.taskNo ?? v,
    },
    { title: '变更内容', dataIndex: 'changeContent', width: 220, ellipsis: true, render: (v) => v ?? '-' },
    { title: '新计划投料量', dataIndex: 'newPlanFeedQty', width: 110, align: 'right', render: (v) => v ?? '-' },
    { title: '新计划产量', dataIndex: 'newPlanQty', width: 100, align: 'right', render: (v) => v ?? '-' },
    { title: '新交期', dataIndex: 'newDeliveryDate', width: 100, render: (v) => v ?? '-' },
    { title: '创建人', dataIndex: 'createBy', width: 85 },
    { title: '创建时间', dataIndex: 'createTime', width: 160 },
    {
      title: '操作', width: 130, fixed: 'right',
      render: (_, r) => (
        <>
          {r.docStatus === '编制中' ? (
            <>
              <a onClick={() => openForm(r)}>编辑</a>
              <Popconfirm title="提交后新值将回写原任务，确认？" onConfirm={async () => { await submitTaskChange(r.id); message.success('已提交并回写任务'); reload() }}>
                <a style={{ marginLeft: 8 }}>提交</a>
              </Popconfirm>
              <Popconfirm title="删除该变更单？" onConfirm={async () => { await deleteTaskChange(r.id); message.success('已删除'); reload() }}>
                <a style={{ color: '#f56c6c', marginLeft: 8 }}>删除</a>
              </Popconfirm>
            </>
          ) : (
            <span className="muted">已提交</span>
          )}
        </>
      ),
    },
  ]

  return (
    <div className="page">
      <div className="sec-title">任务变更</div>
      <Card size="small" style={{ marginBottom: 12 }}>
        <Form form={form} layout="inline" onFinish={(v) => setSearch(cleanParams(v))}>
          <Form.Item name="changeNo" label="变更单号"><Input allowClear placeholder="BG..." style={{ width: 130 }} /></Form.Item>
          <Form.Item name="docStatus" label="状态">
            <Select allowClear placeholder="全部" style={{ width: 100 }} options={DOC_STATUS} />
          </Form.Item>
          <Form.Item><Button type="primary" htmlType="submit">搜索</Button></Form.Item>
          <Form.Item><Button onClick={() => { form.resetFields(); setSearch({}) }}>重置</Button></Form.Item>
          <Form.Item><Button type="primary" onClick={() => openForm()}>新增变更单</Button></Form.Item>
        </Form>
      </Card>
      <Card size="small">
        <Table rowKey="id" size="small" columns={columns} dataSource={rows} loading={loading} pagination={pagination} scroll={{ x: 1350 }} />
      </Card>

      <Modal
        title={editing ? `编辑变更单 - ${editing.changeNo}` : '新增任务变更单'}
        open={open}
        onCancel={() => setOpen(false)}
        onOk={() => form.submit()}
        width={640}
        destroyOnClose
      >
        <Form form={form} layout="vertical" onFinish={submit}>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', columnGap: 12 }}>
            <Form.Item name="changeNo" label="变更单号" rules={[{ required: true, message: '请输入变更单号' }]}>
              <Input placeholder="如 BG20261008001" disabled={!!editing} />
            </Form.Item>
            <Form.Item name="taskId" label="原任务" rules={[{ required: true, message: '请选择原任务' }]}>
              <Select
                showSearch
                optionFilterProp="label"
                placeholder="选择生产任务"
                disabled={!!editing}
                options={tasks.map((t) => ({ label: `${t.taskNo}（${t.batchNo ?? '-'}）`, value: t.id }))}
                onChange={(id) => { setBaseline(null); if (id) getTaskBaseline(id).then(setBaseline).catch(() => setBaseline(null)) }}
              />
            </Form.Item>
          </div>
          {baseline ? (
            <div className="muted" style={{ marginBottom: 10 }}>
              当前基准：投料量 {baseline.planFeedQty ?? '-'} / 计划产量 {baseline.planQty ?? '-'} / 交期 {baseline.deliveryDate ?? '-'}
            </div>
          ) : null}
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', columnGap: 12 }}>
            <Form.Item name="newPlanFeedQty" label="新计划投料量"><InputNumber style={{ width: '100%' }} placeholder="留空不变" /></Form.Item>
            <Form.Item name="newPlanQty" label="新计划产量"><InputNumber style={{ width: '100%' }} placeholder="留空不变" /></Form.Item>
            <Form.Item name="newDeliveryDate" label="新交期"><DatePicker style={{ width: '100%' }} /></Form.Item>
          </div>
          <Form.Item name="changeContent" label="变更内容" rules={[{ required: true, message: '请描述变更内容' }]}>
            <Input.TextArea rows={3} placeholder="变更原因与内容说明" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
