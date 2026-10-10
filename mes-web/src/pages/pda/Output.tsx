import { useEffect, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { App, Button, Form, InputNumber, Select } from 'antd'
import { pdaOutput, pdaTeams } from '@/api/pda'
import type { PdaScanVO, PdaTeam } from '@/api/pda'

/** PDA 产量汇报：扫排程后大数字报产，发布事件累计任务/订单产出 */
export default function PdaOutput() {
  const { message } = App.useApp()
  const navigate = useNavigate()
  const location = useLocation()
  const scan = (location.state as { scan?: PdaScanVO } | null)?.scan ?? null
  const [teams, setTeams] = useState<PdaTeam[]>([])
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    pdaTeams().then(setTeams).catch(() => setTeams([]))
  }, [])

  if (!scan) {
    return (
      <div style={{ padding: 20, textAlign: 'center' }}>
        <div className="muted" style={{ margin: '26px 0' }}>请先扫码获取排程信息</div>
        <Button type="primary" size="large" block style={{ height: 46 }} onClick={() => navigate('/pda/scan')}>
          去扫码
        </Button>
      </div>
    )
  }

  const s = scan.schedule

  const onFinish = async (v: { outputQty: number; teamId?: number }) => {
    setSubmitting(true)
    try {
      const id = await pdaOutput({ scheduleId: s.id, outputQty: v.outputQty, teamId: v.teamId })
      message.success(`产量汇报成功（${id}）`)
      navigate('/pda/home')
    } catch {
      // 拦截器已提示
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div style={{ padding: 14 }}>
      <div style={{ background: '#fff', borderRadius: 12, padding: '12px 14px', marginBottom: 12, boxShadow: '0 1px 4px rgba(0,0,0,.06)' }}>
        <div style={{ fontSize: 14, fontWeight: 600, marginBottom: 6 }}>排程确认</div>
        <div style={{ display: 'flex', padding: '6px 0', fontSize: 13 }}>
          <span className="muted" style={{ width: 76 }}>排程单号</span>
          <span style={{ flex: 1 }}>{s.scheduleNo}</span>
        </div>
        <div style={{ display: 'flex', padding: '6px 0', fontSize: 13 }}>
          <span className="muted" style={{ width: 76 }}>生产批号</span>
          <span style={{ flex: 1 }}>{s.batchNo ?? '-'}</span>
        </div>
        <div style={{ display: 'flex', padding: '6px 0', fontSize: 13 }}>
          <span className="muted" style={{ width: 76 }}>工序</span>
          <span style={{ flex: 1 }}>{scan.processName ?? '-'}</span>
        </div>
        <div style={{ display: 'flex', padding: '6px 0', fontSize: 13 }}>
          <span className="muted" style={{ width: 76 }}>计划产量</span>
          <span style={{ flex: 1 }}>{s.scheduleQty ?? '-'}</span>
        </div>
      </div>

      <div style={{ background: '#fff', borderRadius: 12, padding: '14px 14px 6px', boxShadow: '0 1px 4px rgba(0,0,0,.06)' }}>
        <Form layout="vertical" size="large" onFinish={onFinish}>
          <Form.Item
            name="outputQty"
            label="本次产量"
            rules={[{ required: true, message: '请输入产量' }, { type: 'number', min: 0.01, message: '产量必须大于0' }]}
          >
            <InputNumber
              style={{ width: '100%', fontSize: 24, height: 56 }}
              min={0} precision={2} placeholder="0.00"
              inputMode="decimal"
            />
          </Form.Item>
          <Form.Item name="teamId" label="班组">
            <Select
              placeholder="选择班组（可选）"
              showSearch optionFilterProp="label"
              options={teams.map((t) => ({ label: t.teamName ?? String(t.id), value: t.id }))}
            />
          </Form.Item>
          <Form.Item style={{ marginTop: 6 }}>
            <Button type="primary" htmlType="submit" block loading={submitting} style={{ height: 48, fontSize: 17 }}>
              提交汇报
            </Button>
          </Form.Item>
        </Form>
      </div>

      <div className="muted" style={{ fontSize: 11, textAlign: 'center', marginTop: 10 }}>
        任务生效状态方可报产，产出自动累计任务工序与订单
      </div>
    </div>
  )
}
