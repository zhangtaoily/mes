import { useState } from 'react'
import { App, Button, Card, DatePicker, Form, Input, InputNumber, Modal, Popconfirm, Select, Table } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import dayjs from 'dayjs'
import type { Dayjs } from 'dayjs'
import { usePagedList } from '@/hooks/usePagedList'
import { useWorkshops } from '@/hooks/useOptions'
import { cleanParams } from '@/utils/cleanParams'
import { pageFrontReports, saveFrontReport, deleteFrontReport, WRAP_LEVELS } from '@/api/craft'
import type { FrontReport } from '@/api/craft'

type FormValues = Partial<Omit<FrontReport, 'reportDate'>> & { reportDate?: Dayjs }
const YES_NO = [{ label: '是', value: '是' }, { label: '否', value: '否' }]

/** 前纺生产情况汇报（过程工艺异常记录，绕毛类三级枚举：无/轻微/严重） */
export default function FrontReportPage() {
  const { message } = App.useApp()
  const { options: wsOptions, nameMap: wsName } = useWorkshops()
  const { rows, loading, pagination, setSearch, reload } = usePagedList<FrontReport>('/api/craft/front-report')
  const [form] = Form.useForm()
  const [editing, setEditing] = useState<FrontReport | null>(null)
  const [open, setOpen] = useState(false)

  const openForm = (row?: FrontReport) => {
    setEditing(row ?? null)
    form.resetFields()
    form.setFieldsValue(
      row
        ? { ...row, reportDate: row.reportDate ? dayjs(row.reportDate) : undefined }
        : { reportDate: dayjs(), useTalcum: '否' },
    )
    setOpen(true)
  }

  const submit = async (v: FormValues) => {
    const body: Partial<FrontReport> = {
      ...v,
      reportDate: v.reportDate ? v.reportDate.format('YYYY-MM-DD') : undefined,
    }
    if (editing) body.id = editing.id
    await saveFrontReport(body)
    message.success('已保存')
    setOpen(false)
    reload()
  }

  const wrap = (title: string, key: keyof FrontReport) => ({ title, dataIndex: key, width: 85 })
  const columns: ColumnsType<FrontReport> = [
    { title: '生产批号', dataIndex: 'batchNo', width: 120, fixed: 'left' },
    { title: '车间', dataIndex: 'workshopId', width: 120, render: (v) => wsName[v] ?? v ?? '-' },
    { title: '汇报日期', dataIndex: 'reportDate', width: 100 },
    { title: '温度℃', dataIndex: 'temperature', width: 75, align: 'right' },
    { title: '湿度%', dataIndex: 'humidity', width: 75, align: 'right' },
    { title: '原料含油%', dataIndex: 'rawOil', width: 90, align: 'right' },
    { title: '成品含油%', dataIndex: 'productOil', width: 90, align: 'right' },
    { title: '机型', dataIndex: 'machineModel', width: 100 },
    wrap('绕针板', 'wrapComber'),
    wrap('绕圆梳', 'wrapCircleComber'),
    wrap('绕气泡', 'wrapBubble'),
    wrap('绕皮辊', 'wrapApron'),
    wrap('绕罗拉', 'wrapRoller'),
    { title: '撒滑石粉', dataIndex: 'useTalcum', width: 85 },
    { title: '备注', dataIndex: 'remark', width: 150, ellipsis: true, render: (v) => v ?? '-' },
    { title: '汇报人', dataIndex: 'createBy', width: 85 },
    {
      title: '操作', width: 110, fixed: 'right',
      render: (_, r) => (
        <>
          <a onClick={() => openForm(r)}>编辑</a>
          <Popconfirm title="删除该汇报记录？" onConfirm={async () => { await deleteFrontReport(r.id); message.success('已删除'); reload() }}>
            <a style={{ color: '#f56c6c', marginLeft: 8 }}>删除</a>
          </Popconfirm>
        </>
      ),
    },
  ]

  const wrapItem = (label: string, name: keyof FrontReport) => (
    <Form.Item name={name} label={label}>
      <Select allowClear options={WRAP_LEVELS} />
    </Form.Item>
  )

  return (
    <div className="page">
      <div className="sec-title">前纺生产情况汇报</div>
      <Card size="small" style={{ marginBottom: 12 }}>
        <Form form={form} layout="inline" onFinish={(v) => setSearch(cleanParams(v))}>
          <Form.Item name="workshopId" label="车间">
            <Select allowClear placeholder="全部" showSearch optionFilterProp="label" style={{ width: 145 }} options={wsOptions} />
          </Form.Item>
          <Form.Item name="batchNo" label="生产批号"><Input allowClear placeholder="批号" style={{ width: 120 }} /></Form.Item>
          <Form.Item><Button type="primary" htmlType="submit">搜索</Button></Form.Item>
          <Form.Item><Button onClick={() => { form.resetFields(); setSearch({}) }}>重置</Button></Form.Item>
          <Form.Item><Button type="primary" onClick={() => openForm()}>新增汇报</Button></Form.Item>
        </Form>
      </Card>
      <Card size="small">
        <Table rowKey="id" size="small" columns={columns} dataSource={rows} loading={loading} pagination={pagination} scroll={{ x: 1750 }} />
      </Card>

      <Modal
        title={editing ? `编辑前纺汇报 - ${editing.batchNo}` : '新增前纺生产情况汇报'}
        open={open}
        onCancel={() => setOpen(false)}
        onOk={() => form.submit()}
        width={680}
        destroyOnClose
      >
        <Form form={form} layout="vertical" onFinish={submit}>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', columnGap: 12 }}>
            <Form.Item name="batchNo" label="生产批号" rules={[{ required: true, message: '请输入生产批号' }]}>
              <Input placeholder="批号" />
            </Form.Item>
            <Form.Item name="workshopId" label="车间">
              <Select allowClear showSearch optionFilterProp="label" placeholder="车间" options={wsOptions} />
            </Form.Item>
            <Form.Item name="reportDate" label="汇报日期"><DatePicker style={{ width: '100%' }} /></Form.Item>
            <Form.Item name="machineModel" label="机型"><Input placeholder="如 GC40" /></Form.Item>
            <Form.Item name="temperature" label="温度(℃)"><InputNumber style={{ width: '100%' }} /></Form.Item>
            <Form.Item name="humidity" label="湿度(%)"><InputNumber style={{ width: '100%' }} /></Form.Item>
            <Form.Item name="rawOil" label="原料含油(%)"><InputNumber style={{ width: '100%' }} /></Form.Item>
            <Form.Item name="productOil" label="成品含油(%)"><InputNumber style={{ width: '100%' }} /></Form.Item>
            <Form.Item name="useTalcum" label="撒滑石粉"><Select options={YES_NO} /></Form.Item>
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr 1fr 1fr', columnGap: 12 }}>
            {wrapItem('绕针板', 'wrapComber')}
            {wrapItem('绕圆梳', 'wrapCircleComber')}
            {wrapItem('绕气泡', 'wrapBubble')}
            {wrapItem('绕皮辊', 'wrapApron')}
            {wrapItem('绕罗拉', 'wrapRoller')}
          </div>
          <Form.Item name="remark" label="备注"><Input.TextArea rows={2} /></Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
