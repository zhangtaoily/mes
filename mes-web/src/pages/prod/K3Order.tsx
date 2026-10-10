import { useState } from 'react'
import { Card, Form, Input, Select, Table, Button, Modal, DatePicker, InputNumber, Row, Col, message } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import dayjs from 'dayjs'
import { usePagedList } from '@/hooks/usePagedList'
import { useWorkshops } from '@/hooks/useOptions'
import { useAuth } from '@/store/auth'
import { cleanParams } from '@/utils/cleanParams'
import type { ErpK3Order, ErpOrderSave } from '@/api/plan'
import { getErpOrder, createErpOrder, updateErpOrder } from '@/api/plan'
import { BoolTag } from '@/components/StatusTag'

/** K3订单（镜像查看 + 手工录入/编辑） */
export default function K3Order() {
  const { rows, loading, pagination, setSearch, reload } = usePagedList<ErpK3Order>('/api/plan/erp-orders')
  const { hasPerm } = useAuth()
  const { list: workshops } = useWorkshops()
  const [form] = Form.useForm()
  const [editForm] = Form.useForm()
  const [editOpen, setEditOpen] = useState(false)
  const [editing, setEditing] = useState<ErpK3Order | null>(null)
  const [saving, setSaving] = useState(false)
  const canEdit = hasPerm('prod:k3:add')

  const openCreate = () => {
    setEditing(null)
    editForm.resetFields()
    setEditOpen(true)
  }

  const openEdit = async (r: ErpK3Order) => {
    const d = await getErpOrder(r.id)
    setEditing(d)
    editForm.setFieldsValue({
      ...d,
      orderDate: d.orderDate ? dayjs(d.orderDate) : undefined,
      planStartDate: d.planStartDate ? dayjs(d.planStartDate) : undefined,
      planEndDate: d.planEndDate ? dayjs(d.planEndDate) : undefined,
    })
    setEditOpen(true)
  }

  const fmt = (v: { format: (s: string) => string }) => (v ? v.format('YYYY-MM-DD') : undefined)

  const submitEdit = async () => {
    const v = await editForm.validateFields()
    const payload: ErpOrderSave = {
      ...v,
      orderDate: fmt(v.orderDate),
      planStartDate: fmt(v.planStartDate),
      planEndDate: fmt(v.planEndDate),
    }
    setSaving(true)
    try {
      if (editing) {
        await updateErpOrder(editing.id, payload)
        message.success('保存成功')
      } else {
        await createErpOrder(payload)
        message.success('录入成功')
      }
      setEditOpen(false)
      reload()
    } finally {
      setSaving(false)
    }
  }

  const columns: ColumnsType<ErpK3Order> = [
    { title: '序号', width: 60, render: (_, __, i) => (pagination.current! - 1) * pagination.pageSize! + i + 1 },
    { title: '订单编号', dataIndex: 'orderNo', width: 130 },
    { title: '订单日期', dataIndex: 'orderDate', width: 100 },
    { title: '车间', dataIndex: 'workshopName', width: 130 },
    { title: '生产批号', dataIndex: 'batchNo', width: 110 },
    { title: '产品名称', dataIndex: 'productName', width: 150, ellipsis: true },
    { title: '色号', dataIndex: 'colorNo', width: 90 },
    { title: '实纺支数', dataIndex: 'spinCount', width: 80 },
    { title: '订单量', dataIndex: 'k3Qty', width: 90, align: 'right' },
    { title: '已引入量', dataIndex: 'importedQty', width: 90, align: 'right' },
    { title: '原料明细', dataIndex: 'materialDetail', width: 160, ellipsis: true },
    { title: '客户', dataIndex: 'customerName', width: 120, ellipsis: true },
    { title: '销售部门', dataIndex: 'saleDept', width: 100 },
    { title: '交期', dataIndex: 'planEndDate', width: 100 },
    { title: '完结', dataIndex: 'isClosed', width: 70, render: (v) => <BoolTag value={v} /> },
    ...(canEdit ? [{ title: '操作', width: 70, fixed: 'right' as const, render: (_: unknown, r: ErpK3Order) => <a onClick={() => openEdit(r)}>编辑</a> }] : []),
  ]

  return (
    <div className="page">
      <div className="sec-title">K3订单查看</div>
      <Card size="small" style={{ marginBottom: 12 }}>
        <Form
          form={form}
          layout="inline"
          onFinish={(v) => setSearch(cleanParams(v))}
          initialValues={{}}
        >
          <Form.Item name="orderNo" label="订单编号"><Input allowClear placeholder="FBDC..." style={{ width: 150 }} /></Form.Item>
          <Form.Item name="batchNo" label="生产批号"><Input allowClear placeholder="批号" style={{ width: 130 }} /></Form.Item>
          <Form.Item name="productName" label="产品名称"><Input allowClear placeholder="产品" style={{ width: 150 }} /></Form.Item>
          <Form.Item name="isClosed" label="完结状态">
            <Select allowClear style={{ width: 100 }} options={[{ label: '未完结', value: 0 }, { label: '已完结', value: 1 }]} />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit">搜索</Button>
          </Form.Item>
          <Form.Item>
            <Button onClick={() => { form.resetFields(); setSearch({}) }}>重置</Button>
          </Form.Item>
          {canEdit && (
            <Form.Item>
              <Button type="primary" ghost onClick={openCreate}>手工录入</Button>
            </Form.Item>
          )}
        </Form>
      </Card>
      <Card size="small">
        <Table rowKey="id" size="small" columns={columns} dataSource={rows} loading={loading} pagination={pagination} scroll={{ x: 1670 }} />
      </Card>

      <Modal
        title={editing ? `编辑订单 ${editing.orderNo}` : '手工录入订单'}
        open={editOpen}
        onCancel={() => setEditOpen(false)}
        onOk={submitEdit}
        confirmLoading={saving}
        width={760}
        destroyOnClose
      >
        <Form form={editForm} layout="vertical">
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="orderNo" label="订单号" extra={editing ? '订单号不可修改' : '留空自动取号(SO)'}>
                <Input disabled={!!editing} allowClear placeholder="留空自动取号(SO)" maxLength={50} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="batchNo" label="生产批号" rules={[{ required: true, message: '生产批号不能为空' }]}>
                <Input allowClear placeholder="批号" maxLength={50} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="k3Qty" label="订单数量" rules={[{ required: true, message: '订单数量不能为空' }]}>
                <InputNumber min={0.01} precision={2} style={{ width: '100%' }} placeholder="数量" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="unit" label="单位" initialValue="KG">
                <Input allowClear placeholder="KG" maxLength={10} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="orderDate" label="订单日期">
                <DatePicker style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="workshopName" label="车间">
                <Select
                  allowClear
                  showSearch
                  optionFilterProp="label"
                  placeholder="选择车间"
                  options={workshops.map((w) => ({ label: w.workshopName, value: w.workshopName }))}
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="productName" label="品名">
                <Input allowClear placeholder="产品名称" maxLength={100} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="colorNo" label="色号">
                <Input allowClear placeholder="色号" maxLength={50} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="spinCount" label="实纺支数">
                <Input allowClear placeholder="支数" maxLength={50} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="dyeType" label="染色类别">
                <Input allowClear placeholder="条染/绞纱/胚纱" maxLength={20} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="materialDetail" label="原料明细">
                <Input allowClear placeholder="物料明细" maxLength={200} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="materialSpec" label="物料规格">
                <Input allowClear placeholder="物料规格" maxLength={100} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="rawMaterial" label="原料">
                <Input allowClear placeholder="原料" maxLength={100} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="customerName" label="客户">
                <Input allowClear placeholder="客户名称" maxLength={100} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="planStartDate" label="计划开始">
                <DatePicker style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="planEndDate" label="交期(计划结束)">
                <DatePicker style={{ width: '100%' }} />
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Modal>
    </div>
  )
}
