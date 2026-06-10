import type { GeneratedFile } from '../types/generatedProject'

const MAX_PROJECT_FILES = 100
const MAX_PROJECT_NAME_LENGTH = 128
const MAX_FILE_PATH_LENGTH = 500
const MAX_FILE_TYPE_LENGTH = 32
const MAX_FILE_CONTENT_LENGTH = 200_000
const EXTERNAL_HTML_URL_PATTERN = /\s(?:src|href|action|poster)\s*=\s*['"]?\s*(?:https?:)?\/\//i
const INLINE_EVENT_HANDLER_PATTERN = /\son[a-z]+\s*=/i
const SCRIPT_TAG_PATTERN = /<script\b([^>]*)>/gi
const SCRIPT_SRC_PATTERN = /\ssrc\s*=/i
const EXTERNAL_CSS_URL_PATTERN = /(?:@import\s+)?url\(\s*['"]?(?:https?:)?\/\//i
const NETWORK_SCRIPT_PATTERN = /\b(fetch|XMLHttpRequest|WebSocket|EventSource)\s*\(/i
const DANGEROUS_SCRIPT_PATTERN = /\beval\s*\(|\bnew\s+Function\s*\(|\bset(?:Timeout|Interval)\s*\(\s*['"]/
const SCRIPT_FILE_TYPES = new Set(['js', 'jsx', 'ts', 'tsx', 'vue'])

export function validateProjectFiles(files: GeneratedFile[], projectName?: string): string | undefined {
  const seenPaths = new Set<string>()
  if (projectName !== undefined && projectName.length > MAX_PROJECT_NAME_LENGTH) {
    return `项目名称不能超过 ${MAX_PROJECT_NAME_LENGTH} 个字符`
  }
  if (files.length > MAX_PROJECT_FILES) {
    return `项目文件数量不能超过 ${MAX_PROJECT_FILES} 个`
  }

  for (const file of files) {
    const normalizedPath = normalizeProjectPath(file.filePath)
    if (file.filePath.length > MAX_FILE_PATH_LENGTH) {
      return `文件路径不能超过 ${MAX_FILE_PATH_LENGTH} 个字符：${file.filePath}`
    }
    if (file.fileType.length > MAX_FILE_TYPE_LENGTH) {
      return `文件类型不能超过 ${MAX_FILE_TYPE_LENGTH} 个字符：${normalizedPath}`
    }
    if (!isSafeProjectPath(file.filePath)) {
      return `文件路径不安全：${file.filePath}`
    }
    if (seenPaths.has(normalizedPath)) {
      return `文件路径重复：${normalizedPath}`
    }
    const contentError = validateProjectFileContent(file, normalizedPath)
    if (contentError) {
      return contentError
    }
    seenPaths.add(normalizedPath)
  }

  return undefined
}

export function normalizeProjectPath(filePath: string) {
  return filePath.replaceAll('\\', '/')
}

export function isSafeProjectPath(filePath: string) {
  const normalizedPath = normalizeProjectPath(filePath)
  if (normalizedPath.startsWith('/') || !normalizedPath.trim()) {
    return false
  }

  return normalizedPath
    .split('/')
    .every((segment) => segment !== '' && segment !== '.' && segment !== '..')
}

function validateProjectFileContent(file: GeneratedFile, normalizedPath: string) {
  const fileType = file.fileType.toLowerCase()
  if (file.content.length > MAX_FILE_CONTENT_LENGTH) {
    return `项目文件内容过大：${normalizedPath}`
  }
  if (isHtmlFile(normalizedPath, fileType)) {
    const scriptError = validateHtmlScriptTags(file.content)
    if (scriptError) {
      return scriptError
    }
    if (INLINE_EVENT_HANDLER_PATTERN.test(file.content)) {
      return '项目文件不能使用内联事件处理器'
    }
    if (EXTERNAL_HTML_URL_PATTERN.test(file.content)) {
      return '项目文件不能引用外部 URL'
    }
  }
  if (isStyleFile(normalizedPath, fileType) && EXTERNAL_CSS_URL_PATTERN.test(file.content)) {
    return '项目文件不能引用外部 URL'
  }
  if (isScriptFile(normalizedPath, fileType)) {
    if (NETWORK_SCRIPT_PATTERN.test(file.content)) {
      return '项目文件不能执行网络请求'
    }
    if (DANGEROUS_SCRIPT_PATTERN.test(file.content)) {
      return '项目文件不能使用动态代码执行'
    }
  }
  return undefined
}

function validateHtmlScriptTags(content: string) {
  for (const match of content.matchAll(SCRIPT_TAG_PATTERN)) {
    if (!SCRIPT_SRC_PATTERN.test(match[1] ?? '')) {
      return '项目文件不能内联脚本'
    }
  }
  return undefined
}

function isHtmlFile(filePath: string, fileType: string) {
  return fileType === 'html' || filePath.endsWith('.html')
}

function isStyleFile(filePath: string, fileType: string) {
  return fileType === 'css' || filePath.endsWith('.css')
}

function isScriptFile(filePath: string, fileType: string) {
  return SCRIPT_FILE_TYPES.has(fileType)
    || filePath.endsWith('.js')
    || filePath.endsWith('.jsx')
    || filePath.endsWith('.ts')
    || filePath.endsWith('.tsx')
    || filePath.endsWith('.vue')
}
