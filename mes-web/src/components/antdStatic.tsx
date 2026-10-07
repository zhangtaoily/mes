import type { MessageInstance } from 'antd/es/message/interface'
import type { ModalStaticFunctions } from 'antd/es/modal/confirm'
import { App } from 'antd'

/**
 * antd v5 静态方法桥：由 <AntdStaticMount /> 在 App 上下文内挂载 useApp 实例，
 * 供 React 之外（axios 拦截器等）调用 message/modal。
 */
type AntdStatic = {
  message: MessageInstance
  modal: Omit<ModalStaticFunctions, 'warn'>
}

export const antd: AntdStatic = {
  // @ts-ignore 初始占位，挂载后覆盖
  message: null,
  // @ts-ignore
  modal: null,
}

/** 挂载 useApp 实例（渲染一次即可） */
export default function AntdStaticMount() {
  const app = App.useApp()
  antd.message = app.message
  antd.modal = app.modal
  return null
}
