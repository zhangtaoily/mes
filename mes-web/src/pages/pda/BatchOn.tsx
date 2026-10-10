import { useEffect, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { App, Button, Form, Select } from 'antd'
import { pdaBatchOn, pdaTeams } from '@/api/pda'
import type { PdaScanVO, PdaTeam } from '@/api/pda'

const SHIFTS = ['白班', '中班', '夜班'].map((s) => ({ label: s, value: s }))

/** PDA 上批：扫排程后确认上机（班组/班次），与 PC 同源校验链 */
export default function PdaBatchOn() {
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

  const onFinish = async (v: { teamId?: number; shift?: string }) => {
    setSubmitting(true)
    try {
      const id = await pdaBatchOn({ batchOn: { scheduleId: s.id, teamId: v.teamId, shift: v.shift } })
      message.success(`上批成功，单号已生成（${id}）`)
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
          <span className="muted" style={{ width: 76 }}>机台</span>
          <span style={{ flex: 1 }}>{scan.machineNo ?? '-'}</span>
        </div>
      </div>

      <div style={{ background: '#fff', borderRadius: 12, padding: '14px 14px 6px', boxShadow: '0 1px 4px rgba(0,0,0,.06)' }}>
        <Form layout="vertical" size="large" onFinish={onFinish} initialValues={{ shift: '白班' }}>
          <Form.Item name="teamId" label="班组" rules={[{ required: true, message: '请选择班组' }]}>
            <Select
              placeholder="选择班组"
              showSearch optionFilterProp="label"
              options={teams.map((t) => ({ label: t.teamName ?? String(t.id), value: t.id }))}
            />
          </Form.Item>
          <Form.Item name="shift" label="班次" rules={[{ required: true, message: '请选择班次' }]}>
            <Select options={SHIFTS} />
          </Form.Item>
          <Form.Item style={{ marginTop: 6 }}>
            <Button type="primary" htmlType="submit" block loading={submitting} style={{ height: 48, fontSize: 17 }}>
              确认上批
            </Button>
          </Form.Item>
        </Form>
      </div>

      <div className="muted" style={{ fontSize: 11, textAlign: 'center', marginTop: 10, lineHeight: 1.7 }}>
        校验：任务生效 · 制造卡已打印 · 同排程无活动上批
        <br />
        批号标识未被其他未了批批次占用
      </div>
    </div>
  )
}
