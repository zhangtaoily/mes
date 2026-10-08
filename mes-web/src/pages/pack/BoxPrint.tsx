import { useState } from 'react'
import { App, Button, Card, Form, Input, Modal, Table } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { usePagedList } from '@/hooks/usePagedList'
import { cleanParams } from '@/utils/cleanParams'
import { pageLabelReprints, reprintLabel } from '@/api/pack'
import type { PkLabelReprint } from '@/api/pack'

/** 箱贴补打（FG 单据留痕） */
export default function BoxPrint() {
  const { message } = App.useApp()
  const { rows, loading, pagination, setSearch, reload } = usePagedList<PkLabelReprint>('/api/pack/trace/label-reprints')
  const [open, setOpen] = useState(false)
  const [form] = Form.useForm()

  const columns: ColumnsType<PkLabelReprint> = [
    { title: '单据编号', dataIndex: 'docNo', width: 150, fixed: 'left' },
    { title: '箱子编码', dataIndex: 'boxCode', width: 130 },
    { title: '箱序', dataIndex: 'boxSeq', width: 60, align: 'right' },
    { title: '生产批号', dataIndex: 'batchNo', width: 125 },
    { title: '入库批号', dataIndex: 'warehouseBatchNo', width: 115 },
    { title: '缸号', dataIndex: 'dyeVatNo', width: 85 },
    { title: '产品名称', dataIndex: 'productName', width: 130, ellipsis: true },
    { title: '规格', dataIndex: 'productSpec', width: 95 },
    { title: '客户', dataIndex: 'customerName', width: 110, ellipsis: true },
    { title: '公重(kg)', dataIndex: 'publicWeight', width: 85, align: 'right' },
    { title: '净重(kg)', dataIndex: 'netWeight', width: 85, align: 'right' },
    { title: '毛重(kg)', dataIndex: 'grossWeight', width: 85, align: 'right' },
    { title: '托盘码', dataIndex: 'palletCode', width: 110 },
    { title: '补打时间', dataIndex: 'createTime', width: 160 },
  ]

  return (
    <div className="page">
      <div className="sec-title">箱贴补打</div>
      <Card size="small" style={{ marginBottom: 12 }}>
        <Form layout="inline" onFinish={(v) => setSearch(cleanParams(v))}>
          <Form.Item name="docNo" label="单据编号"><Input allowClear placeholder="FG..." style={{ width: 140 }} /></Form.Item>
          <Form.Item name="warehouseBatchNo" label="入库批号"><Input allowClear placeholder="批号" style={{ width: 120 }} /></Form.Item>
          <Form.Item><Button type="primary" htmlType="submit">搜索</Button></Form.Item>
          <Form.Item><Button onClick={() => setSearch({})}>重置</Button></Form.Item>
        </Form>
      </Card>
      <Card size="small">
        <div style={{ marginBottom: 10 }}>
          <Button type="primary" onClick={() => { setOpen(true); form.resetFields() }}>登记补打</Button>
          <span className="muted" style={{ marginLeft: 12, fontSize: 12 }}>补打自动取号 FG 前缀并留痕</span>
        </div>
        <Table rowKey="id" size="small" columns={columns} dataSource={rows} loading={loading} pagination={pagination} scroll={{ x: 1650 }} />
      </Card>

      <Modal title="登记箱贴补打" open={open} onCancel={() => setOpen(false)} onOk={() => form.submit()} destroyOnClose>
        <Form form={form} layout="vertical" style={{ marginTop: 12 }} onFinish={async (v) => {
          await reprintLabel(v)
          message.success('补打已登记')
          setOpen(false); reload()
        }}>
          <Form.Item name="boxCode" label="箱子编码" rules={[{ required: true }]}><Input placeholder="BX..." /></Form.Item>
          <Form.Item name="batchNo" label="生产批号"><Input /></Form.Item>
          <Form.Item name="warehouseBatchNo" label="入库批号"><Input /></Form.Item>
          <Form.Item name="productName" label="产品名称"><Input /></Form.Item>
          <Form.Item name="palletCode" label="托盘码"><Input /></Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
