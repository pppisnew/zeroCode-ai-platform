<script setup lang="ts">
import { storeToRefs } from 'pinia'
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'

import WorkspaceEditor from '../components/WorkspaceEditor.vue'
import WorkspaceSidebar from '../components/WorkspaceSidebar.vue'
import { useMonacoEditor } from '../hooks/useMonacoEditor'
import { useVisualEditor } from '../hooks/useVisualEditor'
import { useWorkspaceActions } from '../hooks/useWorkspaceActions'
import { useWorkspaceStore } from '../stores/workspace'
import { buildPreviewDocument } from '../utils/previewDocument'

const workspaceStore = useWorkspaceStore()
const {
  prompt,
  projectName,
  projectType,
  currentAppId,
  currentVersionNo,
  selectedFilePath,
  files,
} = storeToRefs(workspaceStore)
const editorMode = ref<'code' | 'visual'>('code')
const workspaceEditor = ref<InstanceType<typeof WorkspaceEditor>>()
const editorContainer = computed(() => workspaceEditor.value?.editorContainer)
const visualEditorContainer = computed(() => workspaceEditor.value?.visualEditorContainer)
const selectedFile = computed(() => files.value.find((file) => file.filePath === selectedFilePath.value))
const canUseVisualEditor = computed(() => projectType.value === 'html')
const { createEditor, syncEditorValue, layoutEditor, disposeEditor } = useMonacoEditor(
  editorContainer,
  selectedFile,
)
const { createVisualEditor, syncVisualEditor, disposeVisualEditor } = useVisualEditor(
  visualEditorContainer,
  files,
  canUseVisualEditor,
)

function ensureCompatibleEditorMode() {
  if (!canUseVisualEditor.value && editorMode.value === 'visual') {
    editorMode.value = 'code'
  }
}

const {
  apps,
  versions,
  isGenerating,
  isLoadingApps,
  isLoadingVersions,
  isExportingZip,
  isSavingVersion,
  isDeploying,
  errorMessage,
  deploymentTarget,
  deployment,
  refreshApps,
  openApp,
  handleGenerate,
  refreshVersions,
  handleSaveVersion,
  handleExportZip,
  handleDeploy,
  restoreVersion,
} = useWorkspaceActions(ensureCompatibleEditorMode)

const previewDocument = computed(() => buildPreviewDocument(files.value, projectType.value))

onMounted(() => {
  void refreshApps()
  void nextTick(createEditor)
})

watch(selectedFilePath, syncEditorValue)

watch(projectType, () => {
  ensureCompatibleEditorMode()
})

watch(editorMode, async (mode) => {
  await nextTick()
  if (mode === 'code' || !canUseVisualEditor.value) {
    editorMode.value = 'code'
    await createEditor()
    syncEditorValue()
    layoutEditor()
    return
  }

  await createVisualEditor()
  syncVisualEditor()
})

watch(
  files,
  () => {
    syncEditorValue()
    if (editorMode.value === 'visual' && canUseVisualEditor.value) {
      syncVisualEditor()
    }
  },
  { deep: false },
)

onBeforeUnmount(() => {
  disposeEditor()
  disposeVisualEditor()
})
</script>

<template>
  <main class="workspace">
    <WorkspaceSidebar
      v-model:deployment-target="deploymentTarget"
      v-model:project-type="projectType"
      v-model:prompt="prompt"
      v-model:selected-file-path="selectedFilePath"
      :apps="apps"
      :current-app-id="currentAppId"
      :current-version-no="currentVersionNo"
      :deployment="deployment"
      :error-message="errorMessage"
      :files="files"
      :is-deploying="isDeploying"
      :is-generating="isGenerating"
      :is-loading-apps="isLoadingApps"
      :is-loading-versions="isLoadingVersions"
      :versions="versions"
      @deploy="handleDeploy"
      @generate="handleGenerate"
      @open-app="openApp"
      @refresh-apps="refreshApps"
      @refresh-versions="refreshVersions"
      @restore-version="restoreVersion"
    />

    <WorkspaceEditor
      ref="workspaceEditor"
      v-model:editor-mode="editorMode"
      :can-use-visual-editor="canUseVisualEditor"
      :current-app-id="currentAppId"
      :current-version-no="currentVersionNo"
      :error-message="errorMessage"
      :is-exporting-zip="isExportingZip"
      :is-saving-version="isSavingVersion"
      :preview-document="previewDocument"
      :project-name="projectName"
      :selected-file="selectedFile"
      @export-zip="handleExportZip"
      @save-version="handleSaveVersion"
    />
  </main>
</template>
