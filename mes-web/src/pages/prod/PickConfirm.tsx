import { useState } from 'react'
import { App, Button, Card, DatePicker, Form, Input, InputNumber, Modal, Popconfirm, Select, Table } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import dayjs from 'dayjs'
import type { Dayjs } from 'dayjs'
import { usePagedList } from '@/hooks/usePagedList'
import { useWorkshops } from '@/hooks/useOptions'
import { cleanParams } from '@/utils/cleanParams'
import { pagePicks, savePick, confirmPicks, deletePick, PICK_TYPES } from '@/api/prod'
import type { MtPick } from '@/api/prod'
import StatusTag, { BoolTag } from '@/components/StatusTag'

const DOC_STATUS = ['待确认', '已确认'].map((s) => ({ label: s, value: s }))
type FormValues = Partial<Omit<MtPick, 'pickDate'>> & { pickDate?: Dayjs }

/** 领退料确认：车间对仓库推送单据批量确认（领料 JDSC1L / 退料 JDSC1T） */
export default function PickConfirm() {
  const { message } = App.useApp()
  const { options: wsOptions, nameMap: wsName } = useWorkshops()
  const { rows, loading, pagination, setSearch, reload } = usePagedList<MtPick>('/api/prod/pick')
  const [form] = Form.useForm()
  const [sel, setSel] = useState<MtPick[]>([])
  const [open, setOpen] = useState(false)
  const [editForm] = Form.useForm()

  const submit = async (v: FormValues) => {
    await savePick({
      ...v,
      pickDate: v.pickDate ? v.pickDate.format('YYYY-MM-DD') : undefined,
      docStatus: '待确认',
    })
    message.success('已保存，待确认')
    setOpen(false)
    reload()
  }

  const columns: ColumnsType<MtPick> = [
    { title: '领退料单号', dataIndex: 'pickNo', width: 150, fixed: 'left' },
    { title: '状态', dataIndex: 'docStatus', width: 85, render: (v) => <StatusTag status={v} /> },
    {
      title: '类型', dataIndex: 'pickType', width: 85,
      render: (v) => (v == null ? '-' : <BoolTag value={v === 1 ? 1 : 0} labels={['退料', '领料']} />),
    },
    { title: '订单号', dataIndex: 'orderNo', width: 130, ellipsis: true, render: (v) => v ?? '-' },
    { title: '生产批号', dataIndex: 'batchNo', width: 115, render: (v) => v ?? '-' },
    { title: '车间', dataIndex: 'workshopId', width: 120, render: (v) => wsName[v] ?? v ?? '-' },
    { title: '任务号', dataIndex: 'taskNo', width: 145, render: (v) => v ?? '-' },
    { title: '物料名称', dataIndex: 'materialName', width: 160, ellipsis: true, render: (v) => v ?? '-' },
    { title: '规格', dataIndex: 'materialSpec', width: 130, ellipsis: true, render: (v) => v ?? '-' },
    { title: '数量', dataIndex: 'qty', width: 85, align: 'right' },
    { title: '单位', dataIndex: 'unit', width: 60 },
    { title: '仓库', dataIndex: 'warehouse', width: 100, render: (v) => v ?? '-' },
    { title: '确认人', dataIndex: 'picker', width: 85, render: (v) => v ?? '-' },
    { title: '领退料日期', dataIndex: 'pickDate', width: 100 },
    {
      title: '操作', width: 70, fixed: 'right',
      render: (_, r) => (
        r.docStatus === '待确认' ? (
          <Popconfirm title="删除该单据？" onConfirm={async () => { await deletePick(r.id); message.success('已删除'); reload() }}>
            <a style={{ color: '#f56c6c' }}>删除</a>
          </Popconfirm>
        ) : <span className="muted">-</span>
      ),
    },
  ]

  return (
    <div className="page">
      <div className="sec-title">领退料确认</div>
      <Card size="small" style={{ marginBottom: 12 }}>
        <Form form={form} layout="inline" onFinish={(v) => setSearch(cleanParams(v))}>
          <Form.Item name="orderNo" label="订单号"><Input allowClear placeholder="订单号" style={{ width: 120 }} /></Form.Item>
          <Form.Item name="batchNo" label="生产批号"><Input allowClear placeholder="批号" style={{ width: 110 }} /></Form.Item>
          <Form.Item name="workshopId" label="车间">
            <Select allowClear placeholder="全部" showSearch optionFilterProp="label" style={{ width: 140 }} options={wsOptions} />
          </Form.Item>
          <Form.Item name="pickType" label="类型">
            <Select allowClear placeholder="全部" style={{ width: 95 }} options={PICK_TYPES} />
          </Form.Item>
          <Form.Item name="docStatus" label="状态">
            <Select allowClear placeholder="全部" style={{ width: 95 }} options={DOC_STATUS} />
          </Form.Item>
          <Form.Item><Button type="primary" htmlType="submit">搜索</Button></Form.Item>
          <Form.Item><Button onClick={() => { form.resetFields(); setSearch({}) }}>重置</Button></Form.Item>
        </Form>
      </Card>
      <Card size="small">
        <div style={{ display: 'flex', gap: 8, marginBottom: 10 }}>
          <Button
            type="primary"
            disabled={!sel.length}
            onClick={() => Modal.confirm({
              title: '批量确认',
              content: `已选 ${sel.length} 条。确认后回填确认人/时间。确认？`,
              onOk: async () => { await confirmPicks(sel.map((r) => r.id)); message.success('已确认'); setSel([]); reload() },
            })}
          >
            批量确认（{sel.length}）
          </Button>
          <Button type="primary" ghost onClick={() => { editForm.resetFields(); editForm.setFieldsValue({ pickType: 1, pickDate: dayjs() }); setOpen(true) }}>
            补录单据
          </Button>
        </div>
        <Table
          rowKey="id"
          size="small"
          columns={columns}
          dataSource={rows}
          loading={loading}
          pagination={pagination}
          scroll={{ x: 1650 }}
          rowSelection={{ selectedRowKeys: sel.filter((r) => r.docStatus === '待确认').map((r) => r.id), onChange: (_, rows2) => setSel(rows2), getCheckboxProps: (r) => ({ disabled: r.docStatus !== '待确认' }) }}
        />
      </Card>

      <Modal
        title="补录领退料单"
        open={open}
        onCancel={() => setOpen(false)}
        onOk={() => editForm.submit()}
        width={680}
        destroyOnClose
      >
        <Form form={editForm} layout="vertical" onFinish={submit}>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', columnGap: 12 }}>
            <Form.Item name="pickNo" label="领退料单号" rules={[{ required: true, message: '请输入单号' }]}>
              <Input placeholder="领料JDSC1L/退料JDSC1T" />
            </Form.Item>
            <Form.Item name="pickType" label="单据类型" rules={[{ required: true, message: '请选择类型' }]}>
              <Select options={PICK_TYPES} />
            </Form.Item>
            <Form.Item name="pickDate" label="领退料日期"><DatePicker style={{ width: '100%' }} /></Form.Item>
            <Form.Item name="orderNo" label="订单号"><Input /></Form.Item>
            <Form.Item name="batchNo" label="生产批号"><Input /></Form.Item>
            <Form.Item name="taskNo" label="任务号"><Input /></Form.Item>
            <Form.Item name="workshopId" label="车间">
              <Select allowClear showSearch optionFilterProp="label" placeholder="车间" options={wsOptions} />
            </Form.Item>
            <Form.Item name="warehouse" label="仓库"><Input /></Form.Item>
            <Form.Item name="location" label="库位"><Input /></Form.Item>
            <Form.Item name="materialNo" label="物料号"><Input /></Form.Item>
            <Form.Item name="materialName" label="物料名称"><Input /></Form.Item>
            <Form.Item name="materialSpec" label="物料规格"><Input /></Form.Item>
            <Form.Item name="qty" label="数量" rules={[{ required: true, message: '请输入数量' }]}>
              <InputNumber style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item name="unit" label="单位"><Input placeholder="KG" /></Form.Item>
            <Form.Item name="colorNo" label="色号"><Input /></Form.Item>
          </div>
        </Form>
      </Modal>
    </div>
  )
}
