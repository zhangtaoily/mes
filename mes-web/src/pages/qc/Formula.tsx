import { useEffect, useState } from 'react'
import { App, Button, Card, Form, Input, InputNumber, Modal, Popconfirm, Select, Table } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { usePagedList } from '@/hooks/usePagedList'
import { useProcesses } from '@/hooks/useOptions'
import { cleanParams } from '@/utils/cleanParams'
import { pageFormulas, saveFormula, updateFormula, deleteFormula } from '@/api/qc'
import type { QcFormula } from '@/api/qc'
import { getQcIndicators } from '@/api/base'
import type { BaseQcIndicator } from '@/api/base'

const WORKSHOP_TYPES = ['整精梳车间', '复精梳车间', '前纺', '后纺'].map((s) => ({ label: s, value: s }))

/** 质检公式：JS 脚本配置化（热生效无需重启），$BQ 取值计算衍生指标 */
export default function Formula() {
  const { message } = App.useApp()
  const { options: procOptions, nameMap: procName } = useProcesses()
  const { rows, loading, pagination, setSearch, reload } = usePagedList<QcFormula>('/api/qc/formulas')
  const [form] = Form.useForm()
  const [indicators, setIndicators] = useState<BaseQcIndicator[]>([])
  const [editing, setEditing] = useState<QcFormula | null>(null)
  const [open, setOpen] = useState(false)

  useEffect(() => {
    getQcIndicators().then(setIndicators).catch(() => setIndicators([]))
  }, [])
  const indName = (id?: number) => indicators.find((i) => i.id === id)?.indicatorName ?? id ?? '-'
  const indTagOptions = indicators.map((i) => ({ label: i.indicatorName, value: i.indicatorName }))

  const openForm = (row?: QcFormula) => {
    setEditing(row ?? null)
    form.resetFields()
    if (row) {
      form.setFieldsValue({ ...row, affectIndicators: row.affectIndicators ? row.affectIndicators.split(',') : [] })
    } else {
      form.setFieldsValue({ sortNo: 0 })
    }
    setOpen(true)
  }

  const submit = async (v: Partial<QcFormula> & { affectIndicators?: string[] }) => {
    const body: Partial<QcFormula> = { ...v, affectIndicators: (v.affectIndicators ?? []).join(',') }
    if (editing) {
      await updateFormula(editing.id, body)
    } else {
      await saveFormula(body)
    }
    message.success('已保存，公式热生效')
    setOpen(false)
    reload()
  }

  const columns: ColumnsType<QcFormula> = [
    { title: '公式名称', dataIndex: 'formulaName', width: 170, fixed: 'left' },
    { title: '公式类型', dataIndex: 'formulaType', width: 100 },
    { title: '车间类型', dataIndex: 'workshopType', width: 110, render: (v) => v ?? '-' },
    { title: '工序', dataIndex: 'processId', width: 100, render: (v) => procName[v] ?? '-' },
    { title: '目标指标', dataIndex: 'targetIndicatorId', width: 120, render: (v) => indName(v) },
    { title: '影响质检项', dataIndex: 'affectIndicators', width: 200, ellipsis: true, render: (v) => v ?? '-' },
    { title: '排序', dataIndex: 'sortNo', width: 65, align: 'right' },
    { title: '更新时间', dataIndex: 'updateTime', width: 160 },
    {
      title: '操作', width: 110, fixed: 'right',
      render: (_, r) => (
        <>
          <a onClick={() => openForm(r)}>编辑</a>
          <Popconfirm title="删除公式后相关衍生指标将无法计算，确认？" onConfirm={async () => { await deleteFormula(r.id); message.success('已删除'); reload() }}>
            <a style={{ color: '#f56c6c', marginLeft: 8 }}>删除</a>
          </Popconfirm>
        </>
      ),
    },
  ]

  return (
    <div className="page">
      <div className="sec-title">质检公式</div>
      <Card size="small" style={{ marginBottom: 12 }}>
        <Form form={form} layout="inline" onFinish={(v) => setSearch(cleanParams(v))}>
          <Form.Item name="formulaName" label="公式名称"><Input allowClear placeholder="名称" style={{ width: 150 }} /></Form.Item>
          <Form.Item name="formulaType" label="公式类型"><Input allowClear placeholder="类型" style={{ width: 110 }} /></Form.Item>
          <Form.Item name="processId" label="工序">
            <Select allowClear placeholder="全部" style={{ width: 130 }} options={procOptions} />
          </Form.Item>
          <Form.Item><Button type="primary" htmlType="submit">搜索</Button></Form.Item>
          <Form.Item><Button onClick={() => { form.resetFields(); setSearch({}) }}>重置</Button></Form.Item>
          <Form.Item><Button type="primary" onClick={() => openForm()}>新增公式</Button></Form.Item>
        </Form>
      </Card>
      <Card size="small">
        <Table rowKey="id" size="small" columns={columns} dataSource={rows} loading={loading} pagination={pagination} scroll={{ x: 1150 }} />
      </Card>

      <Modal
        title={editing ? `编辑公式 - ${editing.formulaName}` : '新增公式'}
        open={open}
        onCancel={() => setOpen(false)}
        onOk={() => form.submit()}
        width={680}
        destroyOnClose
      >
        <Form form={form} layout="vertical" onFinish={submit}>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', columnGap: 12 }}>
            <Form.Item name="formulaName" label="公式名称" rules={[{ required: true, message: '请输入公式名称' }]}>
              <Input placeholder="如：百米重量不匀率" />
            </Form.Item>
            <Form.Item name="formulaType" label="公式类型">
              <Input placeholder="工序检 / 成品检 / 包装称重" />
            </Form.Item>
            <Form.Item name="workshopType" label="车间类型">
              <Select allowClear placeholder="全部车间" options={WORKSHOP_TYPES} />
            </Form.Item>
            <Form.Item name="processId" label="工序">
              <Select allowClear placeholder="全部工序" options={procOptions} />
            </Form.Item>
            <Form.Item name="targetIndicatorId" label="目标指标" rules={[{ required: true, message: '请选择目标指标' }]}>
              <Select
                allowClear
                showSearch
                optionFilterProp="label"
                placeholder="计算结果写入的指标"
                options={indicators.map((i) => ({ label: i.indicatorName, value: i.id }))}
              />
            </Form.Item>
            <Form.Item name="sortNo" label="排序">
              <InputNumber style={{ width: '100%' }} placeholder="越小越靠前" />
            </Form.Item>
          </div>
          <Form.Item name="affectIndicators" label="影响质检项（可多选/手输）">
            <Select mode="tags" allowClear placeholder="该公式取值涉及的质检项" options={indTagOptions} />
          </Form.Item>
          <Form.Item
            name="script"
            label="公式脚本（JS，$BQ(质检项ID) 取值，返回计算结果）"
            rules={[{ required: true, message: '公式脚本不能为空' }]}
          >
            <Input.TextArea
              rows={8}
              placeholder={'var calculateBYL = function() {\n  var zlObj = $BQ(质检项ID);\n  ...\n  return result;\n};\ncalculateBYL();'}
            />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
