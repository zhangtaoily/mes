import { useState } from 'react'
import { App, Button, Card, Form, Input, InputNumber, Modal, Popconfirm, Select, Table } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { usePagedList } from '@/hooks/usePagedList'
import { cleanParams } from '@/utils/cleanParams'
import { useWorkshops } from '@/hooks/useOptions'
import { pageTrucks, getTruckDetail, saveTruck, moveTruck, splitTruck, deleteTruck } from '@/api/site'
import type { RepTruck, RepTruckMove } from '@/api/site'

const OP_NAMES = ['绑定', '称重', '入库', '移库', '出库']

/** 复精梳毛球周转车（绑定/称重/入库/移库/出库 + 拆分，全程移动轨迹） */
export default function Truck() {
  const { message } = App.useApp()
  const { nameMap: wsName, options: wsOptions } = useWorkshops()
  const { rows, loading, pagination, setSearch, reload } = usePagedList<RepTruck>('/api/site/truck')

  const [saveOpen, setSaveOpen] = useState(false)
  const [saveForm] = Form.useForm()
  const [moveRow, setMoveRow] = useState<RepTruck | null>(null)
  const [moveForm] = Form.useForm()
  const [splitRow, setSplitRow] = useState<RepTruck | null>(null)
  const [splitForm] = Form.useForm()
  const [detail, setDetail] = useState<{ truck: RepTruck; moves: RepTruckMove[] } | null>(null)
  const [detailLoading, setDetailLoading] = useState(false)

  const openDetail = async (r: RepTruck) => {
    setDetailLoading(true)
    setDetail({ truck: r, moves: [] })
    try {
      setDetail(await getTruckDetail(r.id))
    } catch {
      setDetail(null)
    } finally {
      setDetailLoading(false)
    }
  }

  const columns: ColumnsType<RepTruck> = [
    { title: '车号', dataIndex: 'truckNo', width: 110, fixed: 'left' },
    { title: '状态', dataIndex: 'status', width: 80, render: (v) => v ?? '-' },
    { title: '车间', dataIndex: 'workshopId', width: 120, render: (v) => wsName[v] ?? v ?? '-' },
    { title: '生产批号', dataIndex: 'batchNo', width: 130 },
    { title: '原料', dataIndex: 'rawMaterial', width: 120, ellipsis: true },
    { title: '库位', dataIndex: 'location', width: 100 },
    { title: '袋色', dataIndex: 'bagColor', width: 80 },
    { title: '毛球数', dataIndex: 'ballCount', width: 80, align: 'right' },
    { title: '毛重(kg)', dataIndex: 'grossWeight', width: 90, align: 'right' },
    { title: '净重(kg)', dataIndex: 'netWeight', width: 90, align: 'right' },
    { title: '标重(kg)', dataIndex: 'standardWeight', width: 90, align: 'right' },
    { title: '回潮(标)', dataIndex: 'moistureStandard', width: 85, align: 'right' },
    { title: '回潮(实)', dataIndex: 'moistureActual', width: 85, align: 'right' },
    {
      title: '操作', width: 200, fixed: 'right',
      render: (_, r) => (
        <>
          <a onClick={() => { setMoveRow(r); moveForm.resetFields(); moveForm.setFieldsValue({ opName: '移库' }) }}>移动</a>
          <a style={{ margin: '0 8px' }} onClick={() => { setSplitRow(r); splitForm.resetFields() }}>拆分</a>
          <a style={{ marginRight: 8 }} onClick={() => openDetail(r)}>轨迹</a>
          <Popconfirm title="确认删除该车及其移动轨迹？" onConfirm={async () => { await deleteTruck(r.id); message.success('已删除'); reload() }}>
            <a style={{ color: '#f56c6c' }}>删除</a>
          </Popconfirm>
        </>
      ),
    },
  ]

  const moveColumns: ColumnsType<RepTruckMove> = [
    { title: '操作', dataIndex: 'opName', width: 80 },
    { title: '车号', dataIndex: 'truckNo', width: 100 },
    { title: '从库位', dataIndex: 'fromLocation', width: 90 },
    { title: '到库位', dataIndex: 'toLocation', width: 90 },
    { title: '复精梳批号', dataIndex: 'recombedBatchNo', width: 120 },
    { title: '纺纱批号', dataIndex: 'spinBatchNo', width: 120 },
    { title: '袋色', dataIndex: 'bagColor', width: 70 },
    { title: '毛球数', dataIndex: 'ballCount', width: 75, align: 'right' },
    { title: '净重(kg)', dataIndex: 'netWeight', width: 85, align: 'right' },
    { title: '移动净重', dataIndex: 'moveNetWeight', width: 85, align: 'right' },
    { title: '移动球数', dataIndex: 'moveBallCount', width: 80, align: 'right' },
    { title: '首车', dataIndex: 'isFirst', width: 60, render: (v) => (v === 1 ? '是' : '-') },
    { title: '末车', dataIndex: 'isFinished', width: 60, render: (v) => (v === 1 ? '是' : '-') },
    { title: '时间', dataIndex: 'createTime', width: 150 },
  ]

  return (
    <div className="page">
      <div className="sec-title">毛球周转车</div>
      <Card size="small" style={{ marginBottom: 12 }}>
        <Form layout="inline" onFinish={(v) => setSearch(cleanParams(v))}>
          <Form.Item name="truckNo" label="车号"><Input allowClear placeholder="车号" style={{ width: 120 }} /></Form.Item>
          <Form.Item name="batchNo" label="批号"><Input allowClear placeholder="批号" style={{ width: 130 }} /></Form.Item>
          <Form.Item name="workshopId" label="车间">
            <Select allowClear placeholder="全部" style={{ width: 150 }} showSearch optionFilterProp="label" options={wsOptions} />
          </Form.Item>
          <Form.Item name="status" label="状态">
            <Select allowClear placeholder="全部" style={{ width: 110 }} options={[...OP_NAMES, '车辆拆分'].map((s) => ({ label: s, value: s }))} />
          </Form.Item>
          <Form.Item><Button type="primary" htmlType="submit">搜索</Button></Form.Item>
          <Form.Item><Button onClick={() => setSearch({})}>重置</Button></Form.Item>
        </Form>
      </Card>
      <Card size="small">
        <div style={{ marginBottom: 10 }}>
          <Button type="primary" onClick={() => { setSaveOpen(true); saveForm.resetFields() }}>新增周转车</Button>
        </div>
        <Table rowKey="id" size="small" columns={columns} dataSource={rows} loading={loading} pagination={pagination} scroll={{ x: 1500 }} />
      </Card>

      <Modal title="新增周转车" open={saveOpen} onCancel={() => setSaveOpen(false)} onOk={() => saveForm.submit()} destroyOnClose>
        <Form form={saveForm} layout="vertical" style={{ marginTop: 12 }} onFinish={async (v) => {
          await saveTruck(v)
          message.success('保存成功')
          setSaveOpen(false); reload()
        }}>
          <Form.Item name="truckNo" label="车号" rules={[{ required: true }]}><Input placeholder="如 TC001" /></Form.Item>
          <Form.Item name="workshopId" label="车间" rules={[{ required: true }]}>
            <Select showSearch optionFilterProp="label" options={wsOptions} />
          </Form.Item>
          <Form.Item name="batchNo" label="生产批号"><Input /></Form.Item>
          <Form.Item name="rawMaterial" label="原料"><Input /></Form.Item>
          <Form.Item name="location" label="库位"><Input /></Form.Item>
          <Form.Item name="bagColor" label="袋色"><Input /></Form.Item>
          <Form.Item name="ballCount" label="毛球数"><InputNumber style={{ width: '100%' }} min={0} precision={0} /></Form.Item>
          <Form.Item name="grossWeight" label="毛重(kg)"><InputNumber style={{ width: '100%' }} min={0} precision={2} /></Form.Item>
          <Form.Item name="netWeight" label="净重(kg)"><InputNumber style={{ width: '100%' }} min={0} precision={2} /></Form.Item>
        </Form>
      </Modal>

      <Modal title={`移动 - ${moveRow?.truckNo ?? ''}`} open={!!moveRow} onCancel={() => setMoveRow(null)} onOk={() => moveForm.submit()} destroyOnClose width={560}>
        <Form form={moveForm} layout="vertical" style={{ marginTop: 12 }} onFinish={async (v) => {
          await moveTruck({ ...v, truckId: moveRow!.id })
          message.success('移动成功')
          setMoveRow(null); reload()
        }}>
          <Form.Item name="opName" label="操作" rules={[{ required: true }]}>
            <Select options={OP_NAMES.map((s) => ({ label: s, value: s }))} />
          </Form.Item>
          <Form.Item name="toLocation" label="目标库位"><Input /></Form.Item>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', columnGap: 12 }}>
            <Form.Item name="recombedBatchNo" label="复精梳批号"><Input /></Form.Item>
            <Form.Item name="spinBatchNo" label="纺纱批号"><Input /></Form.Item>
            <Form.Item name="bagColor" label="袋色"><Input /></Form.Item>
            <Form.Item name="ballCount" label="毛球数"><InputNumber style={{ width: '100%' }} min={0} precision={0} /></Form.Item>
            <Form.Item name="grossWeight" label="毛重(kg)"><InputNumber style={{ width: '100%' }} min={0} precision={2} /></Form.Item>
            <Form.Item name="netWeight" label="净重(kg)"><InputNumber style={{ width: '100%' }} min={0} precision={2} /></Form.Item>
            <Form.Item name="standardWeight" label="标重(kg)"><InputNumber style={{ width: '100%' }} min={0} precision={2} /></Form.Item>
            <Form.Item name="moistureStandard" label="回潮(标准)"><InputNumber style={{ width: '100%' }} min={0} precision={2} /></Form.Item>
            <Form.Item name="moistureActual" label="回潮(实际)"><InputNumber style={{ width: '100%' }} min={0} precision={2} /></Form.Item>
            <Form.Item name="moveNetWeight" label="本次移动净重"><InputNumber style={{ width: '100%' }} min={0} precision={2} /></Form.Item>
            <Form.Item name="moveBallCount" label="本次移动球数"><InputNumber style={{ width: '100%' }} min={0} precision={0} /></Form.Item>
          </div>
        </Form>
      </Modal>

      <Modal title={`拆分 - ${splitRow?.truckNo ?? ''}`} open={!!splitRow} onCancel={() => setSplitRow(null)} onOk={() => splitForm.submit()} destroyOnClose>
        <div className="muted" style={{ marginBottom: 4 }}>
          当前净重 {splitRow?.netWeight ?? 0}kg / 毛球 {splitRow?.ballCount ?? 0}
        </div>
        <Form form={splitForm} layout="vertical" style={{ marginTop: 12 }} onFinish={async (v) => {
          await splitTruck({ ...v, truckId: splitRow!.id })
          message.success('拆分成功')
          setSplitRow(null); reload()
        }}>
          <Form.Item name="newTruckNo" label="新车号" rules={[{ required: true }]}><Input placeholder="如 TC002" /></Form.Item>
          <Form.Item name="moveNetWeight" label="转移净重(kg)" rules={[{ required: true }]}>
            <InputNumber style={{ width: '100%' }} min={0} precision={2} />
          </Form.Item>
          <Form.Item name="moveBallCount" label="转移毛球数" rules={[{ required: true }]}>
            <InputNumber style={{ width: '100%' }} min={0} precision={0} />
          </Form.Item>
          <Form.Item name="toLocation" label="目标库位"><Input /></Form.Item>
        </Form>
      </Modal>

      <Modal title={`移动轨迹 - ${detail?.truck.truckNo ?? ''}`} open={!!detail} onCancel={() => setDetail(null)} footer={null} width={1200} destroyOnClose>
        <Table rowKey="id" size="small" columns={moveColumns} dataSource={detail?.moves ?? []} loading={detailLoading} pagination={false} scroll={{ x: 1400 }} style={{ marginTop: 8 }} />
      </Modal>
    </div>
  )
}
