<script setup lang="ts">
import { ref } from 'vue'

import type { GeneratedFile } from '../types/generatedProject'

defineProps<{
  canUseVisualEditor: boolean
  codeRatio: number
  currentAppId?: string
  currentVersionNo?: number
  editorMode: 'code' | 'visual'
  errorMessage: string
  isDeploying: boolean
  isExportingZip: boolean
  isSavingVersion: boolean
  previewDocument: string
  projectName: string
  selectedFile?: GeneratedFile
}>()

const emit = defineEmits<{
  deploy: []
  exportZip: []
  saveVersion: []
  toggleSidebar: []
  'update:editorMode': [editorMode: 'code' | 'visual']
}>()

const editorContainer = ref<HTMLElement>()
const visualEditorContainer = ref<HTMLElement>()

defineExpose({
  editorContainer,
  visualEditorContainer,
})
</script>

<template>
  <section class="editor-pane">
    <header class="topbar">
      <div class="topbar-left">
        <button
          class="sidebar-toggle"
          type="button"
          title="折叠侧边栏"
          @click="emit('toggleSidebar')"
        >
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
            <path d="M2 3h12M2 8h8M2 13h12" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
          </svg>
        </button>
        <div class="app-selector">
          <span class="app-name">{{ currentAppId ? `App #${currentAppId}` : '未选择' }}</span>
          <span v-if="currentVersionNo" class="version-badge">v{{ currentVersionNo }}</span>
        </div>
        <span class="topbar-divider">/</span>
        <span class="project-title">{{ projectName || 'untitled' }}</span>
        <span v-if="selectedFile" class="current-file">
          <span class="topbar-divider">/</span>
          {{ selectedFile.filePath }}
        </span>
        <p v-if="errorMessage" class="topbar-error">{{ errorMessage }}</p>
      </div>

      <div class="topbar-actions">
        <a-radio-group
          :model-value="editorMode"
          type="button"
          size="mini"
          @update:model-value="emit('update:editorMode', $event as 'code' | 'visual')"
        >
          <a-radio value="code">Code</a-radio>
          <a-radio value="visual" :disabled="!canUseVisualEditor">Visual</a-radio>
        </a-radio-group>
        <a-button
          size="small"
          :disabled="!currentAppId"
          :loading="isSavingVersion"
          @click="emit('saveVersion')"
        >
          保存
        </a-button>
        <a-button
          size="small"
          type="primary"
          :disabled="!currentAppId || !currentVersionNo"
          :loading="isExportingZip"
          @click="emit('exportZip')"
        >
          导出 ZIP
        </a-button>
        <a-button
          size="small"
          :disabled="!currentAppId || !currentVersionNo"
          :loading="isDeploying"
          @click="emit('deploy')"
        >
          部署
        </a-button>
      </div>
    </header>

    <div class="content-grid">
      <section class="code-panel" :style="{ flex: codeRatio }">
        <div
          v-show="editorMode === 'code'"
          ref="editorContainer"
          class="monaco-shell"
          aria-label="Selected file content"
        ></div>
        <div
          v-show="editorMode === 'visual' && canUseVisualEditor"
          class="visual-shell"
        >
          <div
            ref="visualEditorContainer"
            class="visual-editor"
          ></div>
        </div>
      </section>

      <div class="splitter-wrapper">
        <slot name="splitter" />
      </div>

      <section class="preview-panel" :style="{ flex: 1 - codeRatio }">
        <div class="preview-toolbar">
          <span>Preview</span>
          <small>sandbox</small>
        </div>
        <iframe
          title="Generated application preview"
          referrerpolicy="no-referrer"
          sandbox="allow-scripts"
          :srcdoc="previewDocument"
        />
      </section>
    </div>
  </section>
</template>
