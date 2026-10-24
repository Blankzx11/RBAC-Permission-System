import { onUnmounted, ref } from 'vue'
import type { AuditApi } from './auditApi'
import { AuditApiError } from './auditApi'
import type { AuditFilters, AuditLog, AuditScope } from './types'

export function useAuditLogs(api: AuditApi) {
  const items = ref<AuditLog[]>([]), total = ref(0), loading = ref(false), error = ref(''), selected = ref<AuditLog | null>(null)
  const detailLoading = ref(false), detailError = ref('')
  let listRequest: AbortController | undefined, detailRequest: AbortController | undefined
  let generation = 0, detailGeneration = 0
  const message = (failure: unknown) => failure instanceof AuditApiError ? `${failure.status} ${failure.message}${failure.requestId ? `（追踪ID：${failure.requestId}）` : ''}` : failure instanceof Error ? failure.message : '网络异常，请稍后重试'
  function closeDetail() { detailRequest?.abort(); detailGeneration++; selected.value = null; detailError.value = ''; detailLoading.value = false }
  function reset() {
    listRequest?.abort(); generation++; items.value = []; total.value = 0; loading.value = false; error.value = ''; closeDetail()
  }
  async function load(scope: AuditScope, filters: AuditFilters) {
    listRequest?.abort(); closeDetail(); const current = ++generation
    const controller = new AbortController(); listRequest = controller
    loading.value = true; error.value = ''; items.value = []; total.value = 0
    try {
      const page = await api.list(scope, filters, controller.signal)
      if (current !== generation) return
      items.value = page.items; total.value = page.total
    } catch (failure) { if (!controller.signal.aborted && current === generation) error.value = message(failure) }
    finally { if (current === generation) loading.value = false }
  }
  async function openDetail(scope: AuditScope, row: AuditLog) {
    closeDetail(); selected.value = row; detailLoading.value = true
    const current = ++detailGeneration, controller = new AbortController(); detailRequest = controller
    try { const detail = await api.detail(scope, row.id, controller.signal); if (current === detailGeneration) selected.value = detail }
    catch (failure) { if (!controller.signal.aborted && current === detailGeneration) { detailError.value = message(failure); selected.value = null } }
    finally { if (current === detailGeneration) detailLoading.value = false }
  }
  onUnmounted(reset)
  return { items, total, loading, error, selected, detailLoading, detailError, load, openDetail, closeDetail, reset }
}
