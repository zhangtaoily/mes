import { Suspense, lazy } from 'react'
import type { LazyExoticComponent, ComponentType, ReactNode } from 'react'
import { Navigate, Route, Routes } from 'react-router-dom'
import { Spin } from 'antd'
import { AuthProvider, useAuth } from '@/store/auth'
import AntdStaticMount from '@/components/antdStatic'
import MainLayout from '@/layouts/MainLayout'
import Login from '@/pages/login'

/**
 * 路由注册表：key 对齐后端菜单表 sys_menu.path。
 * 未注册的菜单路径渲染占位页（Placeholder）。
 */
const registry: Record<string, LazyExoticComponent<ComponentType>> = {
  '/dashboard': lazy(() => import('@/pages/dashboard')),
  // 生产管理
  '/prod/k3-order': lazy(() => import('@/pages/prod/K3Order')),
  '/prod/task': lazy(() => import('@/pages/prod/ProdTask')),
  '/prod/schedule': lazy(() => import('@/pages/prod/Schedule')),
  // 工艺管理
  '/craft/plan': lazy(() => import('@/pages/craft/PlanCraft')),
  '/craft/machine-card': lazy(() => import('@/pages/craft/MachineCard')),
  // 现场管理
  '/site/feed-record': lazy(() => import('@/pages/site/FeedRecord')),
  '/site/batch-on': lazy(() => import('@/pages/site/BatchOn')),
  '/site/output-report': lazy(() => import('@/pages/site/OutputReport')),
  // 包装管理
  '/pack/task': lazy(() => import('@/pages/pack/PackTask')),
  '/pack/execute': lazy(() => import('@/pages/pack/PackExecute')),
  // 质检管理
  '/qc/sample': lazy(() => import('@/pages/qc/QcSample')),
  '/qc/report': lazy(() => import('@/pages/qc/QcReport')),
  // 报表
  '/report/center': lazy(() => import('@/pages/report/ReportCenter')),
  // 系统管理
  '/sys/user': lazy(() => import('@/pages/sys/SysUser')),
  '/sys/dict': lazy(() => import('@/pages/sys/SysDict')),
  '/sys/code': lazy(() => import('@/pages/sys/SysCode')),
}

const Placeholder = lazy(() => import('@/pages/placeholder'))

const Loading = (
  <div style={{ padding: 80, textAlign: 'center' }}>
    <Spin size="large" />
  </div>
)

function Page({ comp: Comp }: { comp: LazyExoticComponent<ComponentType> }) {
  return <Suspense fallback={Loading}>{Comp ? <Comp /> : <Placeholder />}</Suspense>
}

/** 登录守卫 */
function RequireAuth({ children }: { children: ReactNode }) {
  const { token, ready } = useAuth()
  if (!token) return <Navigate to="/login" replace />
  if (!ready) return Loading
  return <>{children}</>
}

export default function App() {
  return (
    <AuthProvider>
      <AntdStaticMount />
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route
          path="/"
          element={
            <RequireAuth>
              <MainLayout />
            </RequireAuth>
          }
        >
          <Route index element={<Navigate to="/dashboard" replace />} />
          {Object.entries(registry).map(([path, comp]) => (
            <Route key={path} path={path.replace(/^\//, '')} element={<Page comp={comp} />} />
          ))}
          <Route path="*" element={<Page comp={Placeholder as LazyExoticComponent<ComponentType>} />} />
        </Route>
      </Routes>
    </AuthProvider>
  )
}
