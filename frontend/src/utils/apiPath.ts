export function encodePathSegment(segment: string) {
  const normalized = segment.trim()
  if (!normalized) {
    throw new Error('应用 ID 不能为空')
  }
  return encodeURIComponent(normalized)
}

export function assertPositiveVersionNo(versionNo: number) {
  if (!Number.isInteger(versionNo) || versionNo <= 0) {
    throw new Error('版本号无效')
  }
}
