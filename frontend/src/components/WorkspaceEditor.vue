<script setup lang="ts">
import { ref } from 'vue'

import type { GeneratedFile } from '../types/generatedProject'

defineProps<{
  canUseVisualEditor: boolean
  currentAppId?: string
  currentVersionNo?: number
  editorMode: 'code' | 'visual'
  errorMessage: string
  isExportingZip: boolean
  isSavingVersion: boolean
  previewDocument: string
  projectName: string
  selectedFile?: GeneratedFile
}>()

const emit = defineEmits<{
  exportZip: []
  saveVersion: []
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
      <div>
        <p>Current file</p>
        <h2>{{ projectName }} / {{ selectedFile?.filePath }}</h2>
        <p v-if="errorMessage" class="topbar-error">{{ errorMessage }}</p>
      </div>
      <div class="actions">
        <a-radio-group
          :model-value="editorMode"
          type="button"
          size="small"
          @update:model-value="emit('update:editorMode', $event as 'code' | 'visual')"
        >
          <a-radio value="code">Code</a-radio>
          <a-radio value="visual" :disabled="!canUseVisualEditor">Visual</a-radio>
        </a-radio-group>
        <a-button
          :disabled="!currentAppId"
          :loading="isSavingVersion"
          @click="emit('saveVersion')"
        >
          保存版本
        </a-button>
        <a-button
          type="primary"
          :disabled="!currentAppId || !currentVersionNo"
          :loading="isExportingZip"
          @click="emit('exportZip')"
        >
          导出 ZIP
        </a-button>
      </div>
    </header>

    <div class="content-grid">
      <section class="code-panel">
        <div
          v-show="editorMode === 'code'"
          ref="editorContainer"
          class="monaco-shell"
          aria-label="Selected file content"
        ></div>
        <div v-show="editorMode === 'visual' && canUseVisualEditor" class="visual-shell">
          <div ref="visualEditorContainer" class="visual-editor"></div>
        </div>
      </section>

      <section class="preview-panel">
        <div class="preview-toolbar">
          <span>Sandbox Preview</span>
          <small>iframe</small>
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
