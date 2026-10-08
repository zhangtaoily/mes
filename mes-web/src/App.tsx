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
  '/prod/task-sj': lazy(() => import('@/pages/prod/TaskSj')),
  '/prod/task-change': lazy(() => import('@/pages/prod/TaskChange')),
  '/prod/task-dt': lazy(() => import('@/pages/prod/TaskDt')),
  '/prod/schedule': lazy(() => import('@/pages/prod/Schedule')),
  '/prod/schedule-view': lazy(() => import('@/pages/prod/ScheduleView')),
  '/prod/feed': lazy(() => import('@/pages/prod/PickConfirm')),
  '/prod/task-extra': lazy(() => import('@/pages/prod/TaskExtra')),
  // 工艺管理
  '/craft/plan': lazy(() => import('@/pages/craft/PlanCraft')),
  '/craft/plan-view': lazy(() => import('@/pages/craft/PlanView')),
  '/craft/machine-card': lazy(() => import('@/pages/craft/MachineCard')),
  '/craft/front-report': lazy(() => import('@/pages/craft/FrontReport')),
  '/craft/ws-craft': lazy(() => import('@/pages/craft/WsCraft')),
  '/craft/tech-craft': lazy(() => import('@/pages/craft/TechCraft')),
  // 现场管理
  '/site/feed-record': lazy(() => import('@/pages/site/FeedRecord')),
  '/site/batch-on': lazy(() => import('@/pages/site/BatchOn')),
  '/site/output-report': lazy(() => import('@/pages/site/OutputReport')),
  '/site/stop-report': lazy(() => import('@/pages/site/StopReport')),
  '/site/batch-change-log': lazy(() => import('@/pages/site/BatchChangeLog')),
  '/site/truck': lazy(() => import('@/pages/site/Truck')),
  '/site/yarn-clearer': lazy(() => import('@/pages/site/YarnClearer')),
  '/site/steam': lazy(() => import('@/pages/site/Steam')),
  '/site/attendance': lazy(() => import('@/pages/site/Attendance')),
  '/site/schedule-plan': lazy(() => import('@/pages/site/SchedulePlan')),
  '/site/work-hours': lazy(() => import('@/pages/site/WorkHours')),
  '/site/energy': lazy(() => import('@/pages/site/Energy')),
  '/site/cost': lazy(() => import('@/pages/site/Cost')),
  '/site/issue-feedback': lazy(() => import('@/pages/site/IssueFeedback')),
  // 包装管理
  '/pack/task': lazy(() => import('@/pages/pack/PackTask')),
  '/pack/execute': lazy(() => import('@/pages/pack/PackExecute')),
  '/pack/box-record': lazy(() => import('@/pages/pack/BoxRecord')),
  '/pack/pallet-record': lazy(() => import('@/pages/pack/PalletRecord')),
  '/pack/box-print': lazy(() => import('@/pages/pack/BoxPrint')),
  '/pack/wms-record': lazy(() => import('@/pages/pack/WmsRecord')),
  '/pack/task-dm': lazy(() => import('@/pages/pack/TaskDm')),
  '/pack/box-del-record': lazy(() => import('@/pages/pack/BoxDelRecord')),
  // 质检管理
  '/qc/sample': lazy(() => import('@/pages/qc/QcSample')),
  '/qc/report': lazy(() => import('@/pages/qc/QcReport')),
  '/qc/report-lab': lazy(() => import('@/pages/qc/ReportLab')),
  '/qc/report-public': lazy(() => import('@/pages/qc/ReportPublic')),
  '/qc/formula': lazy(() => import('@/pages/qc/Formula')),
  '/qc/report-pack': lazy(() => import('@/pages/qc/ReportPack')),
  // 报表
  '/report/center': lazy(() => import('@/pages/report/ReportCenter')),
  // 生产统计（只读视图，复用现场管理数据源）
  '/stat/attendance': lazy(() => import('@/pages/stat/AttendanceView')),
  '/stat/schedule-plan': lazy(() => import('@/pages/stat/SchedulePlanView')),
  '/stat/energy': lazy(() => import('@/pages/stat/EnergyView')),
  '/stat/cost': lazy(() => import('@/pages/stat/CostView')),
  '/stat/issue-feedback': lazy(() => import('@/pages/stat/IssueView')),
  // 系统管理
  '/sys/user': lazy(() => import('@/pages/sys/SysUser')),
  '/sys/dict': lazy(() => import('@/pages/base/Dict')),
  '/sys/code': lazy(() => import('@/pages/sys/SysCode')),
  // 基础信息
  '/base/workshop': lazy(() => import('@/pages/base/Workshop')),
  '/base/machine': lazy(() => import('@/pages/base/MachineInfo')),
  '/base/tag-board': lazy(() => import('@/pages/base/TagBoard')),
  '/base/pack-material': lazy(() => import('@/pages/base/PackMaterial')),
  '/base/qc-scheme': lazy(() => import('@/pages/base/QcScheme')),
  '/base/dict': lazy(() => import('@/pages/base/Dict')),
  // 设备管理
  '/equip/maintain-plan': lazy(() => import('@/pages/equip/MaintainPlan')),
  '/equip/maintain-record': lazy(() => import('@/pages/equip/MaintainRecord')),
  '/equip/check-record': lazy(() => import('@/pages/equip/CheckRecord')),
  '/equip/repair-record': lazy(() => import('@/pages/equip/RepairRecord')),
  '/equip/duty-machine': lazy(() => import('@/pages/equip/DutyMachine')),
  '/equip/dict': lazy(() => import('@/pages/equip/EquipDict')),
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
