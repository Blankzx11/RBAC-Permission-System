<script setup lang="ts">
import { computed } from 'vue'
import { buildAuditDiff, displayTime, stableJson } from './auditFunctions'
import type { AuditLog } from './types'
const props = defineProps<{ log: AuditLog; loading: boolean }>()
const rows = computed(() => buildAuditDiff(props.log.before, props.log.after))
</script>

<template>
  <div class="detail-meta">
    <span>{{ log.actorName }} · {{ displayTime(log.createdAt) }}</span>
    <span class="mono">{{ log.requestId }}</span>
    <span>目标：{{ log.targetType }} / {{ log.targetId }} · {{ log.sysId ? `系统 ${log.sysId}` : '平台' }}</span>
    <span v-if="log.ip">来源：{{ log.ip }}</span>
  </div>
  <p v-if="loading" class="muted" role="status">正在核对日志详情…</p>
  <div class="table-scroll">
    <table class="diff-table"><thead><tr><th>字段</th><th>变化</th><th>变更前</th><th>变更后</th></tr></thead>
      <tbody><tr v-for="row in rows" :key="row.field"><td class="mono">{{ row.field }}</td><td>{{ row.change }}</td><td><pre>{{ stableJson(row.before) }}</pre></td><td><pre>{{ stableJson(row.after) }}</pre></td></tr></tbody>
    </table>
  </div>
  <p v-if="!rows.length" class="empty">未记录字段差异</p>
  <details class="snapshots"><summary>查看完整脱敏快照</summary><div class="snapshot-grid"><div><h3>变更前</h3><pre>{{ JSON.stringify(log.before, null, 2) }}</pre></div><div><h3>变更后</h3><pre>{{ JSON.stringify(log.after, null, 2) }}</pre></div></div></details>
</template>
