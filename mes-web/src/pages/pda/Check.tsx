import { useEffect, useRef, useState } from 'react'
import { App, Button, Input, Segmented, Spin, Tag } from 'antd'
import type { InputRef } from 'antd'
import { CheckCircleOutlined, ScanOutlined, SendOutlined } from '@ant-design/icons'
import { pdaCheckItems, pdaSaveCheck, pdaScanMachine } from '@/api/pda'
import type { PdaCheckItem, PdaMachineVO } from '@/api/pda'
import { offlineEnqueue, isNetworkError } from '@/utils/offline'
import { useAuth } from '@/store/auth'

type ItemState = '正常' | '异常'

/** PDA 点检执行：扫机台 → 逐项点检（正常/异常）→ 提交台账（编码自动取号） */
export default function PdaCheck() {
  const { message } = App.useApp()
  const { hasPerm } = useAuth()
  const inputRef = useRef<InputRef>(null)
  const [keyword, setKeyword] = useState('')
  const [loadingMachine, setLoadingMachine] = useState(false)
  const [machine, setMachine] = useState<PdaMachineVO | null>(null)
  const [items, setItems] = useState<PdaCheckItem[]>([])
  const [states, setStates] = useState<Record<number, ItemState>>({})
  const [badNote, setBadNote] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const canSave = hasPerm('equip:checkRecord:list')

  useEffect(() => {
    pdaCheckItems().then((l) => {
      setItems(l)
      setStates(Object.fromEntries(l.map((i) => [i.id, '正常' as ItemState])))
    }).catch(() => setItems([]))
  }, [])

  const scan = async () => {
    const kw = keyword.trim()
    if (!kw) {
      message.warning('请扫描或输入机台号')
      inputRef.current?.focus()
      return
    }
    setLoadingMachine(true)
    try {
      setMachine(await pdaScanMachine(kw))
    } catch {
      setMachine(null)
    } finally {
      setLoadingMachine(false)
      inputRef.current?.focus()
    }
  }

  const submit = async () => {
    if (!machine) {
      message.warning('请先扫描机台')
      return
    }
    const eq = machine.equipment
    const abnormal = items.filter((i) => states[i.id] === '异常')
    const describe =
      `机台#${eq.machineNo}${eq.machineModel ? `[${eq.machineModel}]` : ''}：` +
      items.map((i) => `${i.itemName ?? i.itemCode ?? i.id}=${states[i.id] ?? '正常'}`).join('；') +
      (badNote.trim() ? `；异常说明：${badNote.trim()}` : '')
    const body = { workshopId: eq.workshopId ?? undefined, describe }
    setSubmitting(true)
    try {
      await pdaSaveCheck(body)
      message.success(`点检已提交${abnormal.length > 0 ? `（${abnormal.length}项异常）` : ''}`)
      setMachine(null)
      setKeyword('')
      setBadNote('')
      setStates(Object.fromEntries(items.map((i) => [i.id, '正常' as ItemState])))
      inputRef.current?.focus()
    } catch (e) {
      if (isNetworkError(e)) {
        offlineEnqueue('/api/pda/equip/checks', body, `点检 机台#${eq.machineNo ?? '-'}`)
        message.info('网络异常，已存入离线待提交')
        setMachine(null)
        setKeyword('')
        setBadNote('')
        setStates(Object.fromEntries(items.map((i) => [i.id, '正常' as ItemState])))
        inputRef.current?.focus()
      }
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div style={{ padding: 14 }}>
      <div style={{ background: '#fff', borderRadius: 12, padding: 12, marginBottom: 12, boxShadow: '0 1px 4px rgba(0,0,0,.06)' }}>
        <Input
          ref={inputRef} size="large" autoFocus
          prefix={<ScanOutlined style={{ color: '#722ed1', fontSize: 18 }} />}
          placeholder="扫/输机台号"
          value={keyword} onChange={(e) => setKeyword(e.target.value)} onPressEnter={scan} style={{ fontSize: 15 }}
        />
        <Button type="primary" size="large" block style={{ height: 46, marginTop: 10, fontSize: 16 }} loading={loadingMachine} onClick={scan}>
          扫 机 台
        </Button>
      </div>

      {loadingMachine && <div style={{ textAlign: 'center', padding: 40 }}><Spin /></div>}

      {machine && (
        <div style={{ background: '#fff', borderRadius: 12, padding: '12px 14px', marginBottom: 12, boxShadow: '0 1px 4px rgba(0,0,0,.06)' }}>
          <div style={{ fontSize: 15, fontWeight: 600, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span>机台 #{machine.equipment.machineNo ?? machine.equipment.id}</span>
            {machine.equipment.machineModel && <Tag>{machine.equipment.machineModel}</Tag>}
          </div>
          <div className="muted" style={{ fontSize: 12, marginTop: 4 }}>
            车间 {machine.workshopName ?? '-'}
            {machine.dutyUsers ? ` | 责任人 ${machine.dutyUsers}` : ''}
          </div>

          <div style={{ marginTop: 10, borderTop: '1px dashed #f0f0f0', paddingTop: 6 }}>
            {items.length === 0 && <div className="muted" style={{ fontSize: 13, padding: '8px 0' }}>暂无启用的点检项目，可直接提交</div>}
            {items.map((i) => (
              <div key={i.id} style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 8, padding: '8px 0', borderBottom: '1px dashed #f0f0f0' }}>
                <span style={{ fontSize: 14 }}>{i.itemName ?? i.itemCode}</span>
                <Segmented
                  value={states[i.id] ?? '正常'}
                  onChange={(v) => setStates((s) => ({ ...s, [i.id]: v as ItemState }))}
                  options={[
                    { label: '正常', value: '正常' },
                    { label: '异常', value: '异常' },
                  ]}
                />
              </div>
            ))}
          </div>

          {items.some((i) => states[i.id] === '异常') && (
            <div style={{ marginTop: 10 }}>
              <Input.TextArea
                rows={2} size="large" style={{ fontSize: 14 }}
                placeholder="异常说明（选填，随点检记录一并提交）"
                value={badNote} onChange={(e) => setBadNote(e.target.value)}
              />
            </div>
          )}

          <Button
            type="primary" size="large" block icon={<SendOutlined />}
            style={{ height: 48, fontSize: 16, marginTop: 12, background: '#722ed1' }}
            loading={submitting} disabled={!canSave} onClick={submit}
          >
            提交点检
          </Button>
        </div>
      )}

      {!machine && !loadingMachine && items.length > 0 && (
        <div className="muted" style={{ textAlign: 'center', fontSize: 12, padding: 20 }}>
          <CheckCircleOutlined style={{ fontSize: 22, color: '#722ed1' }} />
          <div style={{ marginTop: 6 }}>共 {items.length} 个点检项目待检查</div>
        </div>
      )}
    </div>
  )
}
