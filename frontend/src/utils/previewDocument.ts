import type { GeneratedFile, ProjectType } from '../types/generatedProject'

const URL_ATTRIBUTES = ['action', 'href', 'poster', 'src']
const EXTERNAL_CSS_URL_PATTERN = /(?:@import\s+)?url\(\s*['"]?(?:https?:)?\/\/[^)]*\)/gi
const NETWORK_SCRIPT_PATTERN = /\b(fetch|XMLHttpRequest|WebSocket|EventSource)\s*\(/i
const DANGEROUS_SCRIPT_PATTERN = /\beval\s*\(|\bnew\s+Function\s*\(|\bset(?:Timeout|Interval)\s*\(\s*['"]/

export function buildPreviewDocument(files: GeneratedFile[], projectType: ProjectType) {
  const html = sanitizePreviewMarkup(buildPreviewMarkup(files, projectType))
  const css = sanitizePreviewStyles(buildPreviewStyles(files, projectType))
  const js = sanitizePreviewScript(
    projectType === 'html' ? (findProjectFile(files, 'js', '.js')?.content ?? '') : '',
  )

  return `<!doctype html>
<html lang="zh-CN">
  <head>
    <meta charset="UTF-8" />
    <meta http-equiv="Content-Security-Policy" content="default-src 'none'; img-src data: blob:; style-src 'unsafe-inline'; script-src 'unsafe-inline'; connect-src 'none'; font-src data:; media-src data: blob:;" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <style>${escapeStyleContent(css)}</style>
  </head>
  <body>
    ${html}
    <script>${escapeScriptContent(js)}<\/script>
  </body>
</html>`
}

function buildPreviewMarkup(files: GeneratedFile[], projectType: ProjectType) {
  if (projectType === 'vue') {
    const appFile = files.find((file) => file.filePath === 'src/App.vue')
    return appFile ? extractVueTemplate(appFile.content) : ''
  }

  if (projectType === 'react') {
    const appFile = files.find((file) => file.filePath === 'src/App.tsx')
    return appFile ? extractReactReturnMarkup(appFile.content) : ''
  }

  return findProjectFile(files, 'html', '.html')?.content ?? ''
}

function buildPreviewStyles(files: GeneratedFile[], projectType: ProjectType) {
  if (projectType === 'html') {
    return findProjectFile(files, 'css', '.css')?.content ?? ''
  }

  return files.find((file) => file.filePath.endsWith('style.css'))?.content ?? ''
}

function findProjectFile(files: GeneratedFile[], fileType: string, extension: string) {
  return files.find((file) => file.fileType === fileType || file.filePath.endsWith(extension))
}

function extractVueTemplate(content: string) {
  const match = content.match(/<template>([\s\S]*?)<\/template>/)
  return match?.[1]?.trim() ?? ''
}

function extractReactReturnMarkup(content: string) {
  const match = content.match(/return\s*\(([\s\S]*?)\n\s*\)/)
  return match?.[1]
    ?.replaceAll('className=', 'class=')
    .replaceAll('{prompt}', '')
    .trim() ?? ''
}

function escapeStyleContent(content: string) {
  return content.replaceAll('</style', '<\\/style')
}

function sanitizePreviewStyles(content: string) {
  return content.replace(EXTERNAL_CSS_URL_PATTERN, '')
}

function escapeScriptContent(content: string) {
  return content.replaceAll('</script', '<\\/script')
}

function sanitizePreviewScript(content: string) {
  if (NETWORK_SCRIPT_PATTERN.test(content) || DANGEROUS_SCRIPT_PATTERN.test(content)) {
    return ''
  }
  return content
}

function sanitizePreviewMarkup(markup: string) {
  if (typeof document === 'undefined') {
    return markup
  }

  const template = document.createElement('template')
  template.innerHTML = markup
  template.content.querySelectorAll('script').forEach((element) => element.remove())
  template.content.querySelectorAll('*').forEach((element) => {
    for (const attribute of Array.from(element.attributes)) {
      const attributeName = attribute.name.toLowerCase()
      if (attributeName.startsWith('on')) {
        element.removeAttribute(attribute.name)
        continue
      }
      if (URL_ATTRIBUTES.includes(attributeName) && isExternalUrl(attribute.value)) {
        element.removeAttribute(attribute.name)
      }
    }
  })

  return template.innerHTML
}

function isExternalUrl(value: string) {
  const normalized = value.trim().toLowerCase()
  return normalized.startsWith('http://') || normalized.startsWith('https://') || normalized.startsWith('//')
}
