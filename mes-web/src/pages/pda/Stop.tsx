import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { App, Button, Form, Input, InputNumber, Select } from 'antd'
import { pdaMachines, pdaStop, pdaTeams, pdaWorkshops } from '@/api/pda'
import type { PdaTeam } from '@/api/pda'
import type { BaseEquipment, BaseWorkshop } from '@/api/plan'

const STOP_REASONS = ['撞车', '落料', '停机', '其他']

/** PDA 启停汇报：选车间机台 → 停机原因大按钮 → 时长（汇报人/日期/机型后端自动补齐） */
export default function PdaStop() {
  const { message } = App.useApp()
  const navigate = useNavigate()
  const [workshops, setWorkshops] = useState<BaseWorkshop[]>([])
  const [teams, setTeams] = useState<PdaTeam[]>([])
  const [machines, setMachines] = useState<BaseEquipment[]>([])
  const [reason, setReason] = useState<string>()
  const [submitting, setSubmitting] = useState(false)
  const [form] = Form.useForm()

  useEffect(() => {
    pdaWorkshops().then(setWorkshops).catch(() => setWorkshops([]))
    pdaTeams().then(setTeams).catch(() => setTeams([]))
  }, [])

  const onWorkshopChange = (ws?: number) => {
    form.setFieldValue('equipmentId', undefined)
    setMachines([])
    if (ws) {
      pdaMachines(ws).then(setMachines).catch(() => setMachines([]))
    }
  }

  const onFinish = async (v: {
    workshopId?: number
    equipmentId?: number
    teamId?: number
    stopDuration: number
    remark?: string
  }) => {
    if (!reason) {
      message.warning('请选择停机原因')
      return
    }
    setSubmitting(true)
    try {
      const id = await pdaStop({ ...v, stopReason: reason })
      message.success(`停机汇报成功（${id}），编号自动取号`)
      navigate('/pda/home')
    } catch {
      // 拦截器已提示
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div style={{ padding: 14 }}>
      <div style={{ background: '#fff', borderRadius: 12, padding: '14px 14px 6px', boxShadow: '0 1px 4px rgba(0,0,0,.06)' }}>
        <Form form={form} layout="vertical" size="large" onFinish={onFinish}>
          <Form.Item name="workshopId" label="车间" rules={[{ required: true, message: '请选择车间' }]}>
            <Select
              placeholder="选择车间"
              showSearch optionFilterProp="label"
              onChange={onWorkshopChange}
              options={workshops.map((w) => ({ label: w.workshopName, value: w.id }))}
            />
          </Form.Item>
          <Form.Item name="equipmentId" label="机台" rules={[{ required: true, message: '请选择机台' }]}>
            <Select
              placeholder="选择机台"
              showSearch optionFilterProp="label"
              notFoundContent={!machines.length ? '请先选择车间' : undefined}
              options={machines.map((m) => ({ label: `${m.machineNo}（${m.machineModel}）`, value: m.id }))}
            />
          </Form.Item>
          <Form.Item name="teamId" label="班组">
            <Select
              placeholder="选择班组（可选）"
              showSearch optionFilterProp="label"
              options={teams.map((t) => ({ label: t.teamName ?? String(t.id), value: t.id }))}
            />
          </Form.Item>

          <Form.Item label="停机原因" required>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
              {STOP_REASONS.map((r) => (
                <Button
                  key={r}
                  size="large"
                  style={{ height: 52, fontSize: 16 }}
                  type={reason === r ? 'primary' : 'default'}
                  onClick={() => setReason(r)}
                >
                  {r}
                </Button>
              ))}
            </div>
          </Form.Item>

          <Form.Item
            name="stopDuration"
            label="停机时长（小时）"
            rules={[{ required: true, message: '请输入时长' }, { type: 'number', min: 0.01, message: '时长必须大于0' }]}
          >
            <InputNumber style={{ width: '100%', fontSize: 20, height: 50 }} min={0} step={0.5} precision={2} placeholder="0.00" />
          </Form.Item>
          <Form.Item name="remark" label="备注">
            <Input.TextArea rows={2} placeholder="选填" />
          </Form.Item>
          <Form.Item style={{ marginTop: 6 }}>
            <Button type="primary" htmlType="submit" block loading={submitting} style={{ height: 48, fontSize: 17 }}>
              提交停机汇报
            </Button>
          </Form.Item>
        </Form>
      </div>
    </div>
  )
}
