export function normalizeImageUrl(url:string|undefined, carBase='/backend/car') {
  if (!url) return ''
  const value = url.trim()
  if (!value) return ''
  // Car service stores newly uploaded images as bare filenames.
  // Older records may contain a full /download/<filename> URL.
  if (!/^https?:\/\//i.test(value) && !value.startsWith('data:') && !value.startsWith('blob:')) {
    const clean = value.replace(/^\/+/, '')
    if (clean.startsWith('download/')) return `${carBase}/api/v1/car/${clean}`
    if (clean.startsWith('api/v1/car/download/')) return `${carBase}/${clean}`
    return `${carBase}/api/v1/car/download/${encodeURIComponent(clean)}`
  }
  try {
    const parsed = new URL(value, window.location.origin)
    const match = parsed.pathname.match(/\/download\/([^/]+)$/)
    if (match) return `${carBase}/api/v1/car/download/${encodeURIComponent(decodeURIComponent(match[1]))}`
    return value
  } catch { return value }
}

export function imageFilename(url:string|undefined) {
  if (!url) return ''
  const value = url.trim()
  try {
    const parsed = new URL(value, window.location.origin)
    const match = parsed.pathname.match(/\/download\/([^/]+)$/)
    if (match) return decodeURIComponent(match[1])
  } catch {}
  return value.replace(/^\/+/, '').replace(/^.*\//, '')
}
