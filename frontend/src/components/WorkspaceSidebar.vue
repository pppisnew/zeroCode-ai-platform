<script setup lang="ts">
import { computed, ref } from 'vue'
import type {
  AppSummary,
  AppVersion,
  Deployment,
  DeploymentTarget,
  GeneratedFile,
  ProjectType,
} from '../types/generatedProject'

const props = defineProps<{
  apps: AppSummary[]
  currentAppId?: string
  currentVersionNo?: number
  deployment?: Deployment
  deploymentTarget: DeploymentTarget
  errorMessage: string
  files: GeneratedFile[]
  isDeploying: boolean
  isGenerating: boolean
  isLoadingApps: boolean
  isLoadingVersions: boolean
  projectType: ProjectType
  prompt: string
  selectedFilePath: string
  versions: AppVersion[]
}>()

const emit = defineEmits<{
  deploy: []
  generate: []
  openApp: [app: AppSummary]
  refreshApps: []
  refreshVersions: []
  restoreVersion: [version: AppVersion]
  'update:deploymentTarget': [target: DeploymentTarget]
  'update:projectType': [projectType: ProjectType]
  'update:prompt': [prompt: string]
  'update:selectedFilePath': [filePath: string]
}>()

type TabId = 'files' | 'apps' | 'versions' | 'deploy'
const activeTab = ref<TabId>('files')

const tabs = computed(() => [
  { id: 'files' as TabId, label: 'Files', count: props.files.length },
  { id: 'apps' as TabId, label: 'Apps', count: props.apps.length },
  { id: 'versions' as TabId, label: 'Vers.', count: props.versions.length },
  { id: 'deploy' as TabId, label: 'Deploy' },
])

const projectTypes = [
  { value: 'html' as ProjectType, label: 'HTML' },
  { value: 'vue' as ProjectType, label: 'Vue' },
  { value: 'react' as ProjectType, label: 'React' },
]
</script>

<template>
  <aside class="sidebar">
    <!-- Prompt — fixed top -->
    <section class="prompt-section">
      <div class="prompt-header">
        <div class="brand-mark">Z</div>
        <span class="prompt-label">Create</span>
      </div>
      <a-textarea
        :model-value="prompt"
        :auto-size="{ minRows: 3, maxRows: 6 }"
        placeholder="描述你想生成的 Web 应用..."
        @update:model-value="emit('update:prompt', $event)"
      />
      <div class="prompt-actions">
        <a-select
          :model-value="projectType"
          size="small"
          style="width: 100px"
          @update:model-value="emit('update:projectType', $event as ProjectType)"
        >
          <a-option
            v-for="pt in projectTypes"
            :key="pt.value"
            :value="pt.value"
          >
            {{ pt.label }}
          </a-option>
        </a-select>
        <a-button
          type="primary"
          long
          :loading="isGenerating"
          @click="emit('generate')"
        >
          生成
        </a-button>
      </div>
      <p v-if="errorMessage" class="error-message">{{ errorMessage }}</p>
    </section>

    <!-- Tab navigation -->
    <nav class="sidebar-tabs">
      <button
        v-for="tab in tabs"
        :key="tab.id"
        class="tab-btn"
        :class="{ active: activeTab === tab.id }"
        type="button"
        @click="activeTab = tab.id"
      >
        {{ tab.label }}
        <span v-if="tab.count != null" class="tab-count">{{ tab.count }}</span>
      </button>
    </nav>

    <!-- Tab content -->
    <section class="tab-content">
      <!-- Files -->
      <div v-show="activeTab === 'files'" class="tab-panel">
        <button
          v-for="file in files"
          :key="file.filePath"
          class="file-row"
          :class="{ active: selectedFilePath === file.filePath }"
          type="button"
          @click="emit('update:selectedFilePath', file.filePath)"
        >
          <span>{{ file.filePath }}</span>
          <small>{{ file.fileType }}</small>
        </button>
      </div>

      <!-- Apps -->
      <div v-show="activeTab === 'apps'" class="tab-panel">
        <button
          class="text-action tab-action"
          type="button"
          :disabled="isLoadingApps"
          @click="emit('refreshApps')"
        >
          刷新应用列表
        </button>
        <button
          v-for="app in apps"
          :key="app.id"
          class="app-row"
          :class="{ active: currentAppId === app.id }"
          type="button"
          @click="emit('openApp', app)"
        >
          <span>{{ app.appName }}</span>
          <small>{{ app.description }}</small>
        </button>
        <p v-if="apps.length === 0" class="empty-message">暂无应用</p>
      </div>

      <!-- Versions -->
      <div v-show="activeTab === 'versions'" class="tab-panel">
        <button
          class="text-action tab-action"
          type="button"
          :disabled="!currentAppId || isLoadingVersions"
          @click="emit('refreshVersions')"
        >
          刷新版本列表
        </button>
        <button
          v-for="version in versions"
          :key="version.id"
          class="version-row"
          :class="{ active: currentVersionNo === version.versionNo }"
          type="button"
          @click="emit('restoreVersion', version)"
        >
          <span>v{{ version.versionNo }}</span>
          <small>{{ version.prompt }}</small>
        </button>
        <p v-if="currentAppId && versions.length === 0" class="empty-message">暂无历史版本</p>
        <p v-else-if="!currentAppId" class="empty-message">生成后自动保存版本</p>
      </div>

      <!-- Deploy -->
      <div v-show="activeTab === 'deploy'" class="tab-panel">
        <a-radio-group
          :model-value="deploymentTarget"
          type="button"
          size="small"
          @update:model-value="emit('update:deploymentTarget', $event as DeploymentTarget)"
        >
          <a-radio value="docker">Docker</a-radio>
          <a-radio value="github-actions">Actions</a-radio>
          <a-radio value="kubernetes">K8s</a-radio>
        </a-radio-group>
        <a-button
          type="primary"
          long
          size="small"
          :disabled="!currentAppId || !currentVersionNo"
          :loading="isDeploying"
          @click="emit('deploy')"
        >
          部署当前版本
        </a-button>
        <div v-if="deployment" class="deployment-status">
          <div>
            <span class="status-pill" :data-status="deployment.status">{{ deployment.status }}</span>
            <small>{{ deployment.target }} · {{ deployment.id }}</small>
          </div>
          <p v-if="deployment.executionLogs.length > 0">
            {{ deployment.executionLogs[deployment.executionLogs.length - 1] }}
          </p>
        </div>
        <p v-else class="empty-message">暂无部署记录</p>
      </div>
    </section>
  </aside>
</template>
