import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { App, Button, Form, InputNumber, Select, Switch } from 'antd'
import { pdaFeed, pdaTasks, pdaTeams, taskMaterials } from '@/api/pda'
import type { ProdTaskMaterial, PdaTeam } from '@/api/pda'
import type { ProdTask } from '@/api/plan'

/** PDA 投料：选任务 → 带出用料清单逐项投料（校验不超剩余可投） */
export default function PdaFeed() {
  const { message } = App.useApp()
  const navigate = useNavigate()
  const [tasks, setTasks] = useState<ProdTask[]>([])
  const [teams, setTeams] = useState<PdaTeam[]>([])
  const [taskNo, setTaskNo] = useState<string>()
  const [materials, setMaterials] = useState<ProdTaskMaterial[]>([])
  const [qtys, setQtys] = useState<Record<string, number>>({})
  const [teamId, setTeamId] = useState<number>()
  const [isFinish, setIsFinish] = useState(false)
  const [loadingM, setLoadingM] = useState(false)
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    pdaTasks().then(setTasks).catch(() => setTasks([]))
    pdaTeams().then(setTeams).catch(() => setTeams([]))
  }, [])

  const onTaskChange = (no?: string) => {
    setTaskNo(no)
    setMaterials([])
    setQtys({})
    if (!no) return
    setLoadingM(true)
    taskMaterials(no)
      .then(setMaterials)
      .catch(() => setMaterials([]))
      .finally(() => setLoadingM(false))
  }

  const remain = (m: ProdTaskMaterial) => {
    const plan = m.planFeedQty ?? 0
    const fed = m.fedQty ?? 0
    return Math.max(plan - fed, 0)
  }

  const submit = async () => {
    const items = materials
      .filter((m) => (qtys[m.rawBatchNo ?? ''] ?? 0) > 0)
      .map((m) => ({ rawBatchNo: m.rawBatchNo, thisFeedQty: qtys[m.rawBatchNo ?? ''] }))
    if (!taskNo) {
      message.warning('请选择生产任务')
      return
    }
    if (!items.length) {
      message.warning('请至少填写一项投料量')
      return
    }
    setSubmitting(true)
    try {
      const feedQty = items.reduce((sum, i) => sum + (i.thisFeedQty ?? 0), 0)
      const id = await pdaFeed({
        feed: { taskNo, teamId, isFinish: isFinish ? 1 : 0, feedQty },
        items,
      })
      message.success(`投料成功（${id}）${isFinish ? '，任务投料已完毕封账' : ''}`)
      navigate('/pda/home')
    } catch {
      // 拦截器已提示
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div style={{ padding: 14 }}>
      <div style={{ background: '#fff', borderRadius: 12, padding: '14px 14px 6px', marginBottom: 12, boxShadow: '0 1px 4px rgba(0,0,0,.06)' }}>
        <Form layout="vertical" size="large">
          <Form.Item label="生产任务" required>
            <Select
              placeholder="选择生效且未封账的任务"
              showSearch optionFilterProp="label"
              value={taskNo}
              onChange={onTaskChange}
              options={tasks.map((t) => ({
                label: `${t.taskNo}（${t.batchNo ?? '-'} ${t.productNo ?? ''}）`,
                value: t.taskNo,
              }))}
            />
          </Form.Item>
          <Form.Item label="班组">
            <Select
              placeholder="选择班组（可选）"
              showSearch optionFilterProp="label"
              value={teamId}
              onChange={setTeamId}
              options={teams.map((t) => ({ label: t.teamName ?? String(t.id), value: t.id }))}
            />
          </Form.Item>
        </Form>
      </div>

      {loadingM && <div className="muted" style={{ textAlign: 'center', padding: 20 }}>加载用料清单...</div>}

      {!!materials.length && (
        <div style={{ background: '#fff', borderRadius: 12, padding: '12px 14px', marginBottom: 12, boxShadow: '0 1px 4px rgba(0,0,0,.06)' }}>
          <div style={{ fontSize: 14, fontWeight: 600, marginBottom: 8 }}>用料清单（本次投料）</div>
          {materials.map((m) => {
            const r = remain(m)
            return (
              <div key={m.id} style={{ padding: '10px 0', borderBottom: '1px dashed #f0f0f0' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline' }}>
                  <div>
                    <div style={{ fontSize: 14, fontWeight: 600 }}>{m.rawBatchNo}</div>
                    <div className="muted" style={{ fontSize: 12, marginTop: 2 }}>
                      {m.rawName ?? '-'} · 剩余可投 {r} {m.unit ?? ''}
                    </div>
                  </div>
                  <InputNumber
                    size="large"
                    style={{ width: 120, fontSize: 17 }}
                    min={0}
                    max={r}
                    precision={2}
                    placeholder="0.00"
                    value={qtys[m.rawBatchNo ?? '']}
                    onChange={(v) => setQtys((prev) => ({ ...prev, [m.rawBatchNo ?? '']: v ?? 0 }))}
                  />
                </div>
              </div>
            )
          })}
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '12px 0 4px' }}>
            <span style={{ fontSize: 14 }}>投料完毕（封账）</span>
            <Switch checked={isFinish} onChange={setIsFinish} />
          </div>
        </div>
      )}

      <Button
        type="primary" size="large" block loading={submitting}
        disabled={!materials.length}
        onClick={submit}
        style={{ height: 48, fontSize: 17 }}
      >
        提交投料
      </Button>
    </div>
  )
}
