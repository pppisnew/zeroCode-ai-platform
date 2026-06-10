import { storeToRefs } from 'pinia'
import { Message } from '@arco-design/web-vue'
import { ref } from 'vue'

import {
  createAppVersionDeployment,
  downloadAppVersionZip,
  generateHtml,
  listApps,
  listAppVersions,
  saveAppVersion,
} from '../api/generations'
import { useWorkspaceStore } from '../stores/workspace'
import type {
  AppSummary,
  AppVersion,
  Deployment,
  DeploymentTarget,
  ProjectType,
} from '../types/generatedProject'
import { buildZipFileName } from '../utils/downloadFileName'
import { validateProjectFiles } from '../utils/projectFileSecurity'

export function useWorkspaceActions(ensureCompatibleEditorMode: () => void) {
  const workspaceStore = useWorkspaceStore()
  const {
    prompt,
    projectName,
    projectType,
    currentAppId,
    currentVersionNo,
    files,
  } = storeToRefs(workspaceStore)
  const isGenerating = ref(false)
  const isLoadingApps = ref(false)
  const isLoadingVersions = ref(false)
  const isExportingZip = ref(false)
  const isSavingVersion = ref(false)
  const isDeploying = ref(false)
  const errorMessage = ref('')
  const apps = ref<AppSummary[]>([])
  const versions = ref<AppVersion[]>([])
  const deploymentTarget = ref<DeploymentTarget>('docker')
  const deployment = ref<Deployment>()

  function resolveErrorMessage(error: unknown, fallback: string) {
    return error instanceof Error && error.message ? error.message : fallback
  }

  function reportError(message: string) {
    errorMessage.value = message
    Message.error(message)
  }

  async function refreshApps() {
    if (isLoadingApps.value) {
      return
    }

    isLoadingApps.value = true
    errorMessage.value = ''

    try {
      apps.value = await listApps()
    } catch (error) {
      reportError(resolveErrorMessage(error, '应用加载失败'))
    } finally {
      isLoadingApps.value = false
    }
  }

  async function openApp(app: AppSummary) {
    if (isLoadingVersions.value) {
      return
    }

    currentAppId.value = app.id
    projectName.value = app.appName
    isLoadingVersions.value = true
    errorMessage.value = ''

    try {
      versions.value = await listAppVersions(app.id)
      const latestVersion = versions.value.at(-1)
      if (latestVersion) {
        restoreVersion(latestVersion)
      } else {
        currentVersionNo.value = undefined
        deployment.value = undefined
      }
    } catch (error) {
      reportError(resolveErrorMessage(error, '应用打开失败'))
    } finally {
      isLoadingVersions.value = false
    }
  }

  async function handleGenerate() {
    const normalizedPrompt = prompt.value.trim()
    if (!normalizedPrompt || isGenerating.value) {
      return
    }

    isGenerating.value = true
    errorMessage.value = ''

    try {
      const result = await generateHtml({
        prompt: normalizedPrompt,
        appId: currentAppId.value,
        projectType: projectType.value,
      })
      const project = result.project
      currentAppId.value = result.app.id
      currentVersionNo.value = result.version.versionNo
      deployment.value = undefined
      projectName.value = project.projectName
      projectType.value = resolveProjectType(project)
      ensureCompatibleEditorMode()
      workspaceStore.setFiles(project.files)
      versions.value = await listAppVersions(result.app.id)
      apps.value = await listApps()
    } catch (error) {
      reportError(resolveErrorMessage(error, '生成失败'))
    } finally {
      isGenerating.value = false
    }
  }

  async function refreshVersions() {
    if (!currentAppId.value || isLoadingVersions.value) {
      return
    }

    isLoadingVersions.value = true
    errorMessage.value = ''

    try {
      versions.value = await listAppVersions(currentAppId.value)
    } catch (error) {
      reportError(resolveErrorMessage(error, '版本加载失败'))
    } finally {
      isLoadingVersions.value = false
    }
  }

  async function handleSaveVersion() {
    const normalizedPrompt = prompt.value.trim()
    if (!currentAppId.value || !normalizedPrompt || isSavingVersion.value) {
      return
    }

    isSavingVersion.value = true
    errorMessage.value = ''

    try {
      const validationError = validateProjectFiles(files.value, projectName.value)
      if (validationError) {
        reportError(validationError)
        return
      }
      const version = await saveAppVersion(currentAppId.value, normalizedPrompt, {
        projectName: projectName.value,
        projectType: projectType.value,
        files: files.value.map((file) => ({ ...file })),
      })
      currentVersionNo.value = version.versionNo
      deployment.value = undefined
      versions.value = await listAppVersions(currentAppId.value)
    } catch (error) {
      reportError(resolveErrorMessage(error, '保存版本失败'))
    } finally {
      isSavingVersion.value = false
    }
  }

  async function handleExportZip() {
    if (!currentAppId.value || !currentVersionNo.value || isExportingZip.value) {
      return
    }

    isExportingZip.value = true
    errorMessage.value = ''

    try {
      const blob = await downloadAppVersionZip(currentAppId.value, currentVersionNo.value)
      const downloadUrl = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = downloadUrl
      link.download = buildZipFileName(projectName.value, currentVersionNo.value)
      link.click()
      URL.revokeObjectURL(downloadUrl)
    } catch (error) {
      reportError(resolveErrorMessage(error, '导出失败'))
    } finally {
      isExportingZip.value = false
    }
  }

  async function handleDeploy() {
    if (!currentAppId.value || !currentVersionNo.value || isDeploying.value) {
      return
    }

    isDeploying.value = true
    errorMessage.value = ''

    try {
      deployment.value = await createAppVersionDeployment(
        currentAppId.value,
        currentVersionNo.value,
        deploymentTarget.value,
      )
      Message.success('部署请求已创建')
    } catch (error) {
      reportError(resolveErrorMessage(error, '部署失败'))
    } finally {
      isDeploying.value = false
    }
  }

  function restoreVersion(version: AppVersion) {
    currentAppId.value = version.appId
    currentVersionNo.value = version.versionNo
    deployment.value = undefined
    prompt.value = version.prompt
    projectName.value = version.project.projectName
    projectType.value = resolveProjectType(version.project)
    ensureCompatibleEditorMode()
    workspaceStore.setFiles(version.project.files)
  }

  return {
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
  }
}

function resolveProjectType(project?: { projectType?: ProjectType; projectName: string }) {
  if (project?.projectType) {
    return project.projectType
  }
  if (project?.projectName.includes('-vue-')) {
    return 'vue'
  }
  if (project?.projectName.includes('-react-')) {
    return 'react'
  }
  return 'html'
}
