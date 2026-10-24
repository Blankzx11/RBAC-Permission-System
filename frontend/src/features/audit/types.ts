export type JsonValue = string | number | boolean | null | JsonValue[] | { [key: string]: JsonValue }
export type Snapshot = Record<string, JsonValue> | null
export interface AuditLog {
  id: string; sysId: string | null; actorId: string | null; actorName: string
  action: string; targetType: string; targetId: string
  before: Snapshot; after: Snapshot; requestId: string; createdAt: string; ip?: string
}
export interface Page<T> { items: T[]; total: number; page: number; pageSize: number }
export type AuditScope = { kind: 'platform'; sysId?: string } | { kind: 'system'; sysId: string }
export interface AuditFilters { from?: string; to?: string; actorId?: string; action?: string; page: number; pageSize: number }
export interface ApiEnvelope<T> { data: T; requestId: string }
export interface DiffRow { field: string; change: '新增' | '移除' | '修改'; before: JsonValue | undefined; after: JsonValue | undefined }
