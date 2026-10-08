import TaskTyped from './TaskTyped'

/** 生产任务（复精梳车间）：workshopType=2 固定过滤 */
export default function TaskSj() {
  return <TaskTyped workshopType={2} title="生产任务（复精梳）" />
}
