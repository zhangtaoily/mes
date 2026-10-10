import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { App, Button, Form, Input, InputNumber, Select } from 'antd'
import { pdaFrontReport, pdaWorkshops } from '@/api/pda'
import type { BaseWorkshop } from '@/api/plan'

const WRAP_ITEMS: { key: string; label: string }[] = [
  { key: 'wrapComber', label: '绕针板' },
  { key: 'wrapCircleComber', label: '绕圆梳' },
  { key: 'wrapBubble', label: '绕气泡' },
  { key: 'wrapApron', label: '绕皮辊' },
  { key: 'wrapRoller', label: '绕罗拉' },
]
const LEVELS = ['无', '轻微', '严重']

/** PDA 前纺生产情况：温湿度/含油 + 绕毛勾选（三级：无/轻微/严重） */
export default function PdaFrontReport() {
  const { message } = App.useApp()
  const navigate = useNavigate()
  const [workshops, setWorkshops] = useState<BaseWorkshop[]>([])
  const [wraps, setWraps] = useState<Record<string, string>>({})
  const [submitting, setSubmitting] = useState(false)
  const [form] = Form.useForm()

  useEffect(() => {
    pdaWorkshops().then(setWorkshops).catch(() => setWorkshops([]))
  }, [])

  const onFinish = async (v: {
    batchNo: string
    workshopId?: number
    temperature?: number
    humidity?: number
    rawOil?: number
    productOil?: number
    useTalcum?: string
    remark?: string
  }) => {
    setSubmitting(true)
    try {
      const id = await pdaFrontReport({ ...v, ...wraps })
      message.success(`前纺汇报成功（${id}）`)
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
        <Form form={form} layout="vertical" size="large" onFinish={onFinish} initialValues={{ useTalcum: '否' }}>
          <Form.Item name="batchNo" label="生产批号" rules={[{ required: true, message: '请输入生产批号' }]}>
            <Input placeholder="批号" />
          </Form.Item>
          <Form.Item name="workshopId" label="车间">
            <Select
              placeholder="选择车间（可选）"
              showSearch optionFilterProp="label"
              options={workshops.map((w) => ({ label: w.workshopName, value: w.id }))}
            />
          </Form.Item>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', columnGap: 10 }}>
            <Form.Item name="temperature" label="温度(℃)">
              <InputNumber style={{ width: '100%' }} placeholder="0.0" />
            </Form.Item>
            <Form.Item name="humidity" label="湿度(%)">
              <InputNumber style={{ width: '100%' }} placeholder="0.0" />
            </Form.Item>
            <Form.Item name="rawOil" label="原料含油(%)">
              <InputNumber style={{ width: '100%' }} placeholder="0.00" />
            </Form.Item>
            <Form.Item name="productOil" label="成品含油(%)">
              <InputNumber style={{ width: '100%' }} placeholder="0.00" />
            </Form.Item>
          </div>

          <Form.Item label="绕毛情况（无/轻微/严重）" required>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
              {WRAP_ITEMS.map((w) => (
                <div key={w.key} style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                  <span style={{ width: 64, fontSize: 14, flexShrink: 0 }}>{w.label}</span>
                  <div style={{ display: 'flex', gap: 6, flex: 1 }}>
                    {LEVELS.map((lv) => (
                      <Button
                        key={lv}
                        size="large"
                        style={{ flex: 1, height: 42, fontSize: 14 }}
                        type={wraps[w.key] === lv ? 'primary' : 'default'}
                        danger={wraps[w.key] === '严重'}
                        onClick={() => setWraps((prev) => ({ ...prev, [w.key]: lv }))}
                      >
                        {lv}
                      </Button>
                    ))}
                  </div>
                </div>
              ))}
            </div>
          </Form.Item>

          <Form.Item name="useTalcum" label="撒滑石粉">
            <Select options={[{ label: '是', value: '是' }, { label: '否', value: '否' }]} />
          </Form.Item>
          <Form.Item name="remark" label="备注">
            <Input.TextArea rows={2} placeholder="选填" />
          </Form.Item>
          <Form.Item style={{ marginTop: 6 }}>
            <Button type="primary" htmlType="submit" block loading={submitting} style={{ height: 48, fontSize: 17 }}>
              提交前纺汇报
            </Button>
          </Form.Item>
        </Form>
      </div>
    </div>
  )
}
