<script setup lang="ts">
import { computed, nextTick, reactive, ref, watch } from 'vue'
import AuditDiffPanel from './AuditDiffPanel.vue'
import { createAuditApi } from './auditApi'
import { dateRangeToInstants, displayTime } from './auditFunctions'
import { useAuditLogs } from './useAuditLogs'
import type { AuditLog, AuditScope } from './types'

const props = defineProps<{ authorization: () => string; identityKey: string }>()
const kind = ref<'platform' | 'system'>('system'), sysId = ref('1'), platformFilter = ref('')
const form = reactive({ from: '', to: '', actorId: '', action: '', pageSize: 20 })
const page = ref(1), connected = ref(false), formError = ref(''), dialog = ref<HTMLDialogElement | null>(null)
const store = useAuditLogs(createAuditApi(props.authorization, import.meta.env.VITE_API_BASE ?? ''))
const { items, total, loading, error, selected, detailLoading, detailError } = store
const pages = computed(() => Math.max(1, Math.ceil(total.value / form.pageSize)))
const scope = (): AuditScope => kind.value === 'platform' ? { kind: 'platform', sysId: platformFilter.value || undefined } : { kind: 'system', sysId: sysId.value }
async function query(resetPage = true) {
  if (resetPage) page.value = 1
  formError.value = ''
  try {
    const range = dateRangeToInstants(form.from, form.to)
    connected.value = true
    await store.load(scope(), { ...range, actorId: form.actorId || undefined, action: form.action || undefined, page: page.value, pageSize: Number(form.pageSize) })
  } catch (failure) { store.reset(); formError.value = failure instanceof Error ? failure.message : '查询参数无效' }
}
async function clear() { Object.assign(form, { from: '', to: '', actorId: '', action: '', pageSize: 20 }); platformFilter.value = ''; await nextTick(); query() }
async function move(delta: number) { page.value += delta; await query(false) }
async function detail(row: AuditLog) { await store.openDetail(scope(), row) }
watch(() => props.identityKey, () => { connected.value = false; page.value = 1; store.reset() })
watch([kind, sysId, platformFilter], () => { connected.value = false; page.value = 1; store.reset() })
watch(selected, async value => { await nextTick(); if (value && !dialog.value?.open) dialog.value?.showModal(); else if (!value) dialog.value?.close() })
defineExpose({ setScope(nextKind: 'platform' | 'system', nextSystem: string) { kind.value = nextKind; sysId.value = nextSystem }, reset: store.reset })
</script>

<template>
  <section class="page-heading"><div><p class="eyebrow">AUDIT / F MODULE</p><h1>审计日志</h1><p class="muted">追踪授权变更，核对每一次操作的前后状态。</p></div><span class="scope-pill">{{ kind === 'platform' ? '平台范围' : `系统 ${sysId}` }}</span></section>
  <form class="filter-panel" @submit.prevent="query()">
    <div class="filter-grid">
      <label>查询范围<select v-model="kind" aria-label="查询范围"><option value="system">系统日志</option><option value="platform">平台日志</option></select></label>
      <label v-if="kind === 'system'">当前系统ID<input v-model="sysId" aria-label="当前系统ID" inputmode="numeric" required></label>
      <label v-else>筛选系统ID<input v-model="platformFilter" aria-label="筛选系统ID" placeholder="留空为全部"></label>
      <label>开始日期（上海时区）<input v-model="form.from" aria-label="开始日期" type="date"></label>
      <label>结束日期<input v-model="form.to" aria-label="结束日期" type="date"></label>
      <label>操作者ID<input v-model="form.actorId" aria-label="操作者ID" inputmode="numeric" placeholder="全部操作者"></label>
      <label>操作类型<input v-model="form.action" aria-label="操作类型" maxlength="64" placeholder="例如 MEMBER_ROLES_REPLACED" list="actions"></label>
    </div>
    <datalist id="actions"><option value="MEMBER_ROLES_REPLACED"></option><option value="ROLE_PERMISSIONS_REPLACED"></option><option value="UPDATED_MEMBER"></option><option value="CREATED_SYSTEM"></option><option value="PASSWORD_CHANGED"></option></datalist>
    <div class="filter-footer"><p class="muted">日期留空默认近 7 个自然日；成对填写，最多 90 日。</p><div class="buttons"><button type="button" @click="clear">清除筛选</button><button class="primary" type="submit" :disabled="loading">{{ loading ? '查询中…' : '查询日志' }}</button></div></div>
  </form>
  <div v-if="formError || error || detailError" class="error-box" role="alert">{{ formError || error || detailError }}<button v-if="error" type="button" @click="query(false)">重试</button></div>
  <section class="list-panel" :aria-busy="loading">
    <div class="list-heading"><h2>操作记录 <span>{{ total }}</span></h2><span class="muted">时间倒序 · 历史快照只读</span></div>
    <div v-if="loading" class="empty" role="status">正在读取日志…</div>
    <div v-else-if="!connected" class="empty">选择身份与查询范围后，点击“查询日志”连接后端。</div>
    <div v-else-if="!items.length && !error && !formError" class="empty">当前条件下没有日志记录</div>
    <div v-else-if="items.length" class="table-scroll"><table><thead><tr><th>操作者 / 时间</th><th>操作类型</th><th>范围</th><th>目标</th><th>详情</th></tr></thead><tbody>
      <tr v-for="row in items" :key="row.id"><td><strong>{{ row.actorName }}</strong><small>{{ displayTime(row.createdAt) }}</small></td><td><code>{{ row.action }}</code></td><td><span class="scope-label">{{ row.sysId ? `系统 ${row.sysId}` : '平台' }}</span></td><td><span>{{ row.targetType }}</span><small class="mono">{{ row.targetId }}</small></td><td><button type="button" @click="detail(row)">前后值</button></td></tr>
    </tbody></table></div>
    <div class="pagination"><label>每页<select v-model="form.pageSize" aria-label="每页条数" @change="query()"><option :value="20">20 条</option><option :value="50">50 条</option><option :value="100">100 条</option></select></label><div class="buttons"><span>{{ page }} / {{ pages }} 页</span><button type="button" :disabled="page <= 1 || loading" @click="move(-1)">上一页</button><button type="button" :disabled="page >= pages || loading" @click="move(1)">下一页</button></div></div>
  </section>
  <dialog ref="dialog" aria-labelledby="detail-title" @cancel.prevent="store.closeDetail()"><div class="dialog-head"><div><p class="eyebrow">CHANGE DETAIL</p><h2 id="detail-title">变更前后值</h2></div><button type="button" aria-label="关闭详情" @click="store.closeDetail()">关闭</button></div><AuditDiffPanel v-if="selected" :log="selected" :loading="detailLoading" /></dialog>
</template>
