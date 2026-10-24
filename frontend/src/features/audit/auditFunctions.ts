import type { DiffRow, JsonValue, Snapshot } from './types'

export function assertId(value: string, name = 'ID'): string {
  if (!/^[1-9][0-9]{0,18}$/.test(value) || BigInt(value) > 9223372036854775807n) throw new Error(`${name}必须为有效的正BIGINT字符串`)
  return value
}

/** 页面日期为上海自然日，接口时间区间为UTC的[from,to)。 */
export function dateRangeToInstants(start: string, end: string): { from?: string; to?: string } {
  if (!start && !end) return {}
  if (!start || !end) throw new Error('开始日期和结束日期必须成对填写')
  const parse = (value: string) => {
    if (!/^\d{4}-\d{2}-\d{2}$/.test(value) || value < '1000-01-01' || value > '9999-12-31') throw new Error('日期格式无效')
    const [year, month, day] = value.split('-').map(Number)
    if (new Date(Date.UTC(year, month - 1, day)).toISOString().slice(0, 10) !== value) throw new Error('日期不存在')
    return Date.parse(`${value}T00:00:00+08:00`)
  }
  const from = parse(start), last = parse(end), days = (last - from) / 86400000 + 1
  if (days < 1) throw new Error('开始日期不得晚于结束日期')
  if (days > 90) throw new Error('单次查询最多90个自然日')
  return { from: new Date(from).toISOString(), to: new Date(last + 86400000).toISOString() }
}

export function stableJson(value: JsonValue | undefined): string {
  if (value === undefined) return '未记录'
  if (value === null || typeof value !== 'object') return JSON.stringify(value)
  if (Array.isArray(value)) return `[${value.map(stableJson).join(',')}]`
  return `{${Object.keys(value).sort().map(key => `${JSON.stringify(key)}:${stableJson(value[key])}`).join(',')}}`
}

export function buildAuditDiff(before: Snapshot, after: Snapshot): DiffRow[] {
  const keys = [...new Set([...Object.keys(before ?? {}), ...Object.keys(after ?? {})])].sort()
  return keys.filter(field => stableJson(before?.[field]) !== stableJson(after?.[field])).map(field => ({
    field,
    change: !Object.hasOwn(before ?? {}, field) ? '新增' : !Object.hasOwn(after ?? {}, field) ? '移除' : '修改',
    before: before?.[field], after: after?.[field],
  }))
}

export function displayTime(value: string): string {
  return new Intl.DateTimeFormat('zh-CN', { timeZone: 'Asia/Shanghai', dateStyle: 'medium', timeStyle: 'medium', hour12: false }).format(new Date(value))
}
