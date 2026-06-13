import { readErrorMessage, request, API_BASE_URL } from './client'
import type {
  AppSummary,
  AppVersion,
  Deployment,
  DeploymentTarget,
  GeneratedProject,
  GenerationResult,
} from '../types/generatedProject'
import { assertPositiveVersionNo, encodePathSegment } from '../utils/apiPath'

export type GenerateHtmlPayload = {
  prompt: string
  appId?: string
  projectType?: 'html' | 'vue' | 'react'
}

export function generateHtml(payload: GenerateHtmlPayload): Promise<GenerationResult> {
  return request<GenerationResult>('/generations/html', {
    method: 'POST',
    body: JSON.stringify(payload),
    timeout: 180000,
  })
}

export function listApps(): Promise<AppSummary[]> {
  return request<AppSummary[]>('/apps')
}

export function listAppVersions(appId: string): Promise<AppVersion[]> {
  return request<AppVersion[]>(`/apps/${encodePathSegment(appId)}/versions`)
}

export async function downloadAppVersionZip(appId: string, versionNo: number): Promise<Blob> {
  assertPositiveVersionNo(versionNo)
  const response = await fetch(
    `${API_BASE_URL}/apps/${encodePathSegment(appId)}/versions/${versionNo}/zip`,
  )

  if (!response.ok) {
    throw new Error(await readErrorMessage(response, '导出失败'))
  }

  return response.blob()
}

export function saveAppVersion(
  appId: string,
  prompt: string,
  project: GeneratedProject,
): Promise<AppVersion> {
  return request<AppVersion>(`/apps/${encodePathSegment(appId)}/versions`, {
    method: 'POST',
    body: JSON.stringify({ prompt, project }),
  })
}

export function createAppVersionDeployment(
  appId: string,
  versionNo: number,
  target: DeploymentTarget,
): Promise<Deployment> {
  assertPositiveVersionNo(versionNo)
  return request<Deployment>(
    `/apps/${encodePathSegment(appId)}/versions/${versionNo}/deployments`,
    {
      method: 'POST',
      body: JSON.stringify({ target }),
    },
  )
}
