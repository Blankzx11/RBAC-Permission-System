<script setup lang="ts">
import { computed, nextTick, ref } from 'vue'
import AuditLogPage from './features/audit/AuditLogPage.vue'

const mode = ref<'basic' | 'bearer'>('basic'), username = ref('auditor_a'), password = ref('DemoOnly12345'), token = ref('')
const revision = ref(0), auditPage = ref<InstanceType<typeof AuditLogPage> | null>(null)
const identityKey = computed(() => `${mode.value}:${username.value}:${revision.value}`)
function authorization() {
  if (mode.value === 'bearer') return `Bearer ${token.value}`
  const bytes = new TextEncoder().encode(`${username.value}:${password.value}`)
  return `Basic ${btoa(Array.from(bytes, byte => String.fromCharCode(byte)).join(''))}`
}
async function chooseUser() {
  revision.value++; await nextTick()
  auditPage.value?.setScope(username.value === 'platform_admin' ? 'platform' : 'system', username.value === 'auditor_b' ? '2' : '1')
}
function credentialsChanged() { revision.value++ }
</script>

<template>
  <div class="min-h-screen bg-slate-50">
    <header class="app-header"><a href="#" class="brand"><span>RB</span>权限管理平台</a><div class="header-meta"><span class="live-dot"></span>审计中心<span class="version">RBAC0 / F</span></div></header>
    <div class="app-layout"><aside><p class="eyebrow">ACCESS MANAGEMENT</p><div class="nav-active">审计日志</div><div class="aside-note">成功变更 · 独立安全事件<br>前后值可追溯<br><span>F 模块实现</span></div></aside>
      <main>
        <section class="connection-panel"><div><strong>接口连接</strong><p>独立演示使用 Basic 账号；集成 A/E 后使用 Bearer 会话。凭据只保存在当前页面内存。</p></div>
          <div class="connection-fields"><label>认证方式<select v-model="mode" aria-label="认证方式" @change="credentialsChanged"><option value="basic">本地 demo</option><option value="bearer">宿主 Bearer</option></select></label>
            <template v-if="mode === 'basic'"><label>演示身份<select v-model="username" aria-label="演示身份" @change="chooseUser"><option value="auditor_a">系统A审计员</option><option value="auditor_b">系统B审计员</option><option value="platform_admin">平台管理员</option><option value="platform_a">平台管理员（系统A）</option><option value="ordinary_a">普通成员（无审计权限）</option></select></label><label>演示密码<input v-model="password" aria-label="演示密码" type="password" autocomplete="current-password" @input="credentialsChanged"></label></template>
            <label v-else>访问令牌<input v-model="token" aria-label="访问令牌" type="password" autocomplete="off" @input="credentialsChanged"></label>
          </div>
        </section>
        <AuditLogPage ref="auditPage" :authorization="authorization" :identity-key="identityKey" />
        <footer>日志由后端查询，页面不提供修改或删除入口。© RBAC0 课程项目</footer>
      </main>
    </div>
  </div>
</template>
