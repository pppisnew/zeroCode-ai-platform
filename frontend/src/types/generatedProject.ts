export type ProjectType = 'html' | 'vue' | 'react'

export type DeploymentTarget = 'docker' | 'github-actions' | 'kubernetes'

export type GeneratedFile = {
  filePath: string
  fileType: string
  content: string
}

export type GeneratedProject = {
  projectName: string
  projectType?: ProjectType
  files: GeneratedFile[]
}

export type AppSummary = {
  id: string
  userId: string
  appName: string
  description: string
  type: string
  status: string
  deployUrl: string | null
  createTime: string
}

export type AppVersion = {
  id: string
  appId: string
  versionNo: number
  prompt: string
  project: GeneratedProject
  createTime: string
}

export type GenerationResult = {
  app: AppSummary
  version: AppVersion
  project: GeneratedProject
}

export type Deployment = {
  id: string
  appId: string
  versionNo: number
  projectType: ProjectType
  artifactUrl: string
  target: DeploymentTarget
  status: 'planned' | 'running' | 'succeeded' | 'failed' | 'skipped'
  plannedCommands: string[]
  executionLogs: string[]
  accessUrl: string | null
  createTime: string | null
}
