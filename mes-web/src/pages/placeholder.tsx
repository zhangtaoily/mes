import { Empty, Result } from 'antd'
import { useLocation } from 'react-router-dom'
import { useAuth } from '@/store/auth'

/** 未实现页面的占位：菜单可见但接口/页面规划中 */
export default function Placeholder() {
  const location = useLocation()
  const { menus } = useAuth()

  // 从菜单树找当前路径名称
  const find = (nodes: typeof menus, path: string): string | null => {
    for (const n of nodes) {
      if (n.menuType === 2 && n.path === path) return n.menuName
      if (n.children?.length) {
        const r = find(n.children, path)
        if (r) return r
      }
    }
    return null
  }
  const name = find(menus, location.pathname)

  return (
    <div className="page">
      <div style={{ background: '#fff', borderRadius: 4, padding: 14, boxShadow: '0 1px 3px rgba(0,0,0,.06)' }}>
        <div className="sec-title">{name ?? '页面'}</div>
        <Result
          icon={<Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description={false} />}
          title="该模块规划中"
          subTitle="前端工程 P0 优先交付核心业务闭环页面，此菜单对应功能将在后续迭代开放。"
        />
      </div>
    </div>
  )
}
