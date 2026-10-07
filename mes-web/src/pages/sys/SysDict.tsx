import { useEffect, useState } from 'react'
import { Card, Table, Tabs } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { getWorkshops, getProdLines, getProcesses, getBatchFlags, getEquipments } from '@/api/plan'
import type { BaseWorkshop, BaseProdLine, BaseProcess, BaseBatchFlag, BaseEquipment } from '@/api/plan'
import { useWorkshops } from '@/hooks/useOptions'

const STATUS = (v?: number) => (v === 1 ? '启用' : v === 0 ? '停用' : '-')

/** 基础字典：车间 / 产线 / 工序 / 批号标识 / 设备 */
export default function SysDict() {
  const [tab, setTab] = useState('workshop')
  const [workshops, setWorkshops] = useState<BaseWorkshop[]>([])
  const [lines, setLines] = useState<BaseProdLine[]>([])
  const [processes, setProcesses] = useState<BaseProcess[]>([])
  const [flags, setFlags] = useState<BaseBatchFlag[]>([])
  const [equipments, setEquipments] = useState<BaseEquipment[]>([])
  const [loading, setLoading] = useState(false)
  const { nameMap: wsName } = useWorkshops()

  useEffect(() => {
    setLoading(true)
    const loaders: Record<string, () => Promise<unknown>> = {
      workshop: () => getWorkshops().then(setWorkshops),
      line: () => getProdLines().then(setLines),
      process: () => getProcesses().then(setProcesses),
      flag: () => getBatchFlags().then(setFlags),
      equipment: () => getEquipments().then(setEquipments),
    }
    loaders[tab]().catch(() => undefined).finally(() => setLoading(false))
  }, [tab])

  const wsColumns: ColumnsType<BaseWorkshop> = [
    { title: '车间编码', dataIndex: 'workshopCode', width: 110 },
    { title: '车间名称', dataIndex: 'workshopName', width: 180 },
    { title: '类型', dataIndex: 'workshopType', width: 90 },
    { title: '状态', dataIndex: 'status', width: 80, render: STATUS },
  ]
  const lineColumns: ColumnsType<BaseProdLine> = [
    { title: '所属车间', dataIndex: 'workshopId', width: 180, render: (v) => wsName[v] ?? v },
    { title: '产线名称', dataIndex: 'lineName', width: 160 },
    { title: 'K3产线ID', dataIndex: 'k3LineId', width: 110 },
    { title: '状态', dataIndex: 'status', width: 80, render: STATUS },
  ]
  const procColumns: ColumnsType<BaseProcess> = [
    { title: '工序编码', dataIndex: 'processCode', width: 110 },
    { title: '工序名称', dataIndex: 'processName', width: 140 },
    { title: '工序类型', dataIndex: 'processType', width: 100 },
    { title: '排序', dataIndex: 'sortNo', width: 70 },
    { title: '计量单位', dataIndex: 'prodUnit', width: 90 },
    { title: '状态', dataIndex: 'status', width: 80, render: STATUS },
  ]
  const flagColumns: ColumnsType<BaseBatchFlag> = [
    { title: '标识名称', dataIndex: 'flagName', width: 140 },
    { title: '别名', dataIndex: 'alias', width: 140 },
    { title: '关联工序', dataIndex: 'processId', width: 120 },
    { title: '状态', dataIndex: 'status', width: 80, render: STATUS },
  ]
  const eqColumns: ColumnsType<BaseEquipment> = [
    { title: '所属车间', dataIndex: 'workshopId', width: 180, render: (v) => wsName[v] ?? v },
    { title: '机型', dataIndex: 'machineModel', width: 140 },
    { title: '机台号', dataIndex: 'machineNo', width: 110 },
    { title: '小时产能', dataIndex: 'hourCapacity', width: 100, align: 'right' },
    { title: '状态', dataIndex: 'status', width: 80, render: STATUS },
  ]

  const tables: Record<string, React.ReactNode> = {
    workshop: <Table rowKey="id" size="small" columns={wsColumns} dataSource={workshops} loading={loading} pagination={false} />,
    line: <Table rowKey="id" size="small" columns={lineColumns} dataSource={lines} loading={loading} pagination={false} />,
    process: <Table rowKey="id" size="small" columns={procColumns} dataSource={processes} loading={loading} pagination={false} />,
    flag: <Table rowKey="id" size="small" columns={flagColumns} dataSource={flags} loading={loading} pagination={false} />,
    equipment: <Table rowKey="id" size="small" columns={eqColumns} dataSource={equipments} loading={loading} pagination={false} />,
  }

  return (
    <div className="page">
      <div className="sec-title">基础字典</div>
      <Card size="small">
        <Tabs
          activeKey={tab}
          onChange={setTab}
          items={[
            { key: 'workshop', label: '车间', children: tables.workshop },
            { key: 'line', label: '产线', children: tables.line },
            { key: 'process', label: '工序', children: tables.process },
            { key: 'flag', label: '批号标识', children: tables.flag },
            { key: 'equipment', label: '设备', children: tables.equipment },
          ]}
        />
      </Card>
    </div>
  )
}
