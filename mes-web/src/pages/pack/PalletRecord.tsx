import { useState } from 'react'
import { App, Button, Card, Form, Input, InputNumber, Modal, Select, Table } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { usePagedList } from '@/hooks/usePagedList'
import { cleanParams } from '@/utils/cleanParams'
import { pagePallets, palletize, pagePackExecutes } from '@/api/pack'
import type { PkPallet } from '@/api/pack'

/** 组托记录（托号 TP + 箱号绑定） */
export default function PalletRecord() {
  const { message } = App.useApp()
  const { rows, loading, pagination, setSearch, reload } = usePagedList<PkPallet>('/api/pack/pallets')
  const [open, setOpen] = useState(false)
  const [form] = Form.useForm()
  const [executes, setExecutes] = useState<{ label: string; value: number }[]>([])

  const openForm = async () => {
    setOpen(true)
    form.resetFields()
    try {
      const p = await pagePackExecutes({ pageNum: 1, pageSize: 100 })
      setExecutes((p.rows ?? []).map((e) => ({ label: `${e.executeNo}(${e.productName ?? ''})`, value: e.id })))
    } catch {
      setExecutes([])
    }
  }

  const columns: ColumnsType<PkPallet> = [
    { title: '托盘码', dataIndex: 'palletCode', width: 130, fixed: 'left' },
    { title: '箱子编码', dataIndex: 'boxCode', width: 130 },
    { title: '生产批号', dataIndex: 'batchNo', width: 130 },
    { title: '缸号', dataIndex: 'dyeVatNo', width: 90 },
    { title: '标重(kg)', dataIndex: 'standardWeight', width: 90, align: 'right' },
    { title: '毛重(kg)', dataIndex: 'grossWeight', width: 90, align: 'right' },
    { title: '净重(kg)', dataIndex: 'netWeight', width: 90, align: 'right' },
    { title: '标准筒数', dataIndex: 'standardTube', width: 85, align: 'right' },
    { title: '组托人', dataIndex: 'palletUser', width: 90 },
    { title: '组托时间', dataIndex: 'palletTime', width: 160 },
  ]

  return (
    <div className="page">
      <div className="sec-title">组托记录</div>
      <Card size="small" style={{ marginBottom: 12 }}>
        <Form layout="inline" onFinish={(v) => setSearch(cleanParams(v))}>
          <Form.Item name="palletCode" label="托盘码"><Input allowClear placeholder="TP..." style={{ width: 130 }} /></Form.Item>
          <Form.Item name="batchNo" label="生产批号"><Input allowClear placeholder="批号" style={{ width: 130 }} /></Form.Item>
          <Form.Item><Button type="primary" htmlType="submit">搜索</Button></Form.Item>
          <Form.Item><Button onClick={() => setSearch({})}>重置</Button></Form.Item>
        </Form>
      </Card>
      <Card size="small">
        <div style={{ marginBottom: 10 }}>
          <Button type="primary" onClick={openForm}>组托</Button>
          <span className="muted" style={{ marginLeft: 12, fontSize: 12 }}>系统自动生成托号 TP 前缀</span>
        </div>
        <Table rowKey="id" size="small" columns={columns} dataSource={rows} loading={loading} pagination={pagination} />
      </Card>

      <Modal title="组托" open={open} onCancel={() => setOpen(false)} onOk={() => form.submit()} destroyOnClose>
        <Form form={form} layout="vertical" style={{ marginTop: 12 }} onFinish={async (v) => {
          const boxCodes = (v.boxCodesText as string).split(/[\s,，;；]+/).filter(Boolean)
          if (!boxCodes.length) { message.warning('请填写箱子编码'); return }
          const palletCode = await palletize({ executeId: v.executeId, boxCodes, standardTube: v.standardTube })
          message.success(`组托成功，托号 ${palletCode}`)
          setOpen(false); reload()
        }}>
          <Form.Item name="executeId" label="包装执行单">
            <Select allowClear showSearch optionFilterProp="label" placeholder="选择执行单" options={executes} />
          </Form.Item>
          <Form.Item name="boxCodesText" label="箱子编码（换行/逗号分隔，可多个）" rules={[{ required: true }]}>
            <Input.TextArea rows={3} placeholder="BX2601..., BX2601..." />
          </Form.Item>
          <Form.Item name="standardTube" label="标准筒数">
            <InputNumber style={{ width: '100%' }} min={0} precision={0} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
