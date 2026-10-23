<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { mountPrototype } from './engine.js'
import type { ContextSummary, PrototypeController } from './types'

const summary = ref<ContextSummary>({displayName: '', systemName: '', loggedIn: false, roleCount: 0, permissionCount: 0, annotations: false})
const annotated = ref(false)
let controller: PrototypeController | undefined
const scopeLabel = computed(() => summary.value.loggedIn ? `${summary.value.displayName} / ${summary.value.systemName}` : '未登录 · 本地模拟')
onMounted(() => { controller = mountPrototype(value => { summary.value = value; annotated.value = value.annotations }) })
onUnmounted(() => controller?.dispose())
function annotate(event: Event) {
  annotated.value = (event.target as HTMLInputElement).checked
  controller?.setApiAnnotations(annotated.value)
}
</script>

<template>
  <div class="vue-toolbar flex flex-wrap items-center justify-between gap-3 bg-slate-900 px-5 py-3 text-xs text-white" aria-label="Vue原型评审工具">
    <div class="flex flex-wrap items-center gap-3">
      <span class="font-semibold tracking-wide">RBAC0 · Vue 3 + TypeScript + Tailwind</span>
      <span class="text-slate-300">{{ scopeLabel }}</span>
      <span v-if="summary.loggedIn && summary.systemName !== '平台控制台'" class="text-sky-200">{{ summary.roleCount }} 个直接角色 / {{ summary.permissionCount }} 个有效权限</span>
    </div>
    <div class="flex flex-wrap items-center gap-4">
      <label class="flex cursor-pointer items-center gap-2"><input type="checkbox" aria-label="Vue评审API标注" :checked="annotated" @change="annotate">评审API标注</label>
      <a class="text-sky-200 underline" href="../../../docs/index.html">设计阅读版</a>
      <a class="text-sky-200 underline" href="../../index.html">独立HTML兼容版</a>
    </div>
  </div>
  <!-- The Vue host owns these stable nodes; the shared DOM engine owns their child views. -->
  <div id="app"></div>
  <dialog id="modal" aria-labelledby="modal-title"></dialog>
  <div id="live" role="status" aria-live="polite"></div>
</template>
