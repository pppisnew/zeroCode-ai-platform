const RESERVED_CHARS_PATTERN = /[<>:"/\\|?*\u0000-\u001f]/g
const TRAILING_DOTS_OR_SPACES_PATTERN = /[. ]+$/g

export function buildZipFileName(projectName: string, versionNo: number) {
  const safeProjectName = projectName
    .trim()
    .replace(RESERVED_CHARS_PATTERN, '-')
    .replace(TRAILING_DOTS_OR_SPACES_PATTERN, '')
    .slice(0, 80)

  return `${safeProjectName || 'zerocode-app'}-v${versionNo}.zip`
}
