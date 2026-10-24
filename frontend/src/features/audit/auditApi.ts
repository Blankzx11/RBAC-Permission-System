import { assertId } from './auditFunctions'
import type { ApiEnvelope, AuditFilters, AuditLog, AuditScope, Page } from './types'

export class AuditApiError extends Error {
  constructor(public status: number, public code: string, message: string, public requestId: string) { super(message) }
}

export function createAuditApi(authorization: () => string, base = '') {
  async function get<T>(path: string, signal?: AbortSignal): Promise<T> {
    const response = await fetch(`${base.replace(/\/$/, '')}${path}`, {
      headers: { Authorization: authorization(), Accept: 'application/json' }, signal, cache: 'no-store',
    })
    let body: ApiEnvelope<T> & { code?: string; message?: string }
    try { body = await response.json() }
    catch { throw new AuditApiError(response.status, 'INVALID_RESPONSE', '服务响应格式无效', response.headers.get('X-Request-Id') ?? '') }
    if (!response.ok) throw new AuditApiError(response.status, body.code ?? 'REQUEST_FAILED', body.message ?? '日志查询失败', body.requestId ?? '')
    return body.data
  }
  const resource = (scope: AuditScope) => scope.kind === 'platform' ? '/api/v1/platform/audit-logs' : `/api/v1/systems/${assertId(scope.sysId, '系统ID')}/audit-logs`
  return {
    list(scope: AuditScope, filters: AuditFilters, signal?: AbortSignal): Promise<Page<AuditLog>> {
      const query = new URLSearchParams({ page: String(filters.page), pageSize: String(filters.pageSize) })
      for (const key of ['from', 'to', 'actorId', 'action'] as const) if (filters[key]) query.set(key, filters[key]!)
      if (filters.actorId) assertId(filters.actorId, '操作者ID')
      if (scope.kind === 'platform' && scope.sysId) query.set('sysId', assertId(scope.sysId, '筛选系统ID'))
      return get(`${resource(scope)}?${query}`, signal)
    },
    detail(scope: AuditScope, id: string, signal?: AbortSignal): Promise<AuditLog> { return get(`${resource(scope)}/${assertId(id, '日志ID')}`, signal) },
  }
}

export type AuditApi = ReturnType<typeof createAuditApi>
