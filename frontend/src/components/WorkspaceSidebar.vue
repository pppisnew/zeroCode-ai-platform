<script setup lang="ts">
import type {
  AppSummary,
  AppVersion,
  Deployment,
  DeploymentTarget,
  GeneratedFile,
  ProjectType,
} from '../types/generatedProject'

defineProps<{
  apps: AppSummary[]
  currentAppId?: string
  currentVersionNo?: number
  errorMessage: string
  files: GeneratedFile[]
  deployment?: Deployment
  deploymentTarget: DeploymentTarget
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
</script>

<template>
  <aside class="sidebar">
    <div class="brand">
      <span class="brand-mark">Z</span>
      <div>
        <h1>ZeroCode</h1>
        <p>AI Web App Generator</p>
      </div>
    </div>

    <section class="panel">
      <div class="panel-header">
        <h2>Prompt</h2>
        <span>{{ currentAppId ? `App #${currentAppId}` : 'Phase 1' }}</span>
      </div>
      <a-textarea
        :model-value="prompt"
        :auto-size="{ minRows: 5, maxRows: 8 }"
        placeholder="描述你想生成的 Web 应用"
        @update:model-value="emit('update:prompt', $event)"
      />
      <a-radio-group
        :model-value="projectType"
        type="button"
        size="small"
        @update:model-value="emit('update:projectType', $event as ProjectType)"
      >
        <a-radio value="html">HTML</a-radio>
        <a-radio value="vue">Vue</a-radio>
        <a-radio value="react">React</a-radio>
      </a-radio-group>
      <p v-if="errorMessage" class="error-message">{{ errorMessage }}</p>
      <a-button type="primary" long :loading="isGenerating" @click="emit('generate')">
        生成应用
      </a-button>
    </section>

    <section class="panel">
      <div class="panel-header">
        <h2>Apps</h2>
        <button
          class="text-action"
          type="button"
          :disabled="isLoadingApps"
          @click="emit('refreshApps')"
        >
          刷新
        </button>
      </div>
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
    </section>

    <section class="panel">
      <div class="panel-header">
        <h2>Files</h2>
        <span>{{ currentVersionNo ? `v${currentVersionNo}` : files.length }}</span>
      </div>
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
    </section>

    <section class="panel">
      <div class="panel-header">
        <h2>Versions</h2>
        <button
          class="text-action"
          type="button"
          :disabled="!currentAppId || isLoadingVersions"
          @click="emit('refreshVersions')"
        >
          刷新
        </button>
      </div>
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
    </section>

    <section class="panel">
      <div class="panel-header">
        <h2>Deployment</h2>
        <span>{{ currentVersionNo ? `v${currentVersionNo}` : 'Draft' }}</span>
      </div>
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
    </section>
  </aside>
</template>
