import { useEffect, useState } from 'react'
import { carApi } from '../../api/car'
import { imageFilename, normalizeImageUrl } from '../../utils/normalize'

type Props = { src?: string; alt?: string; className?: string }

export function CarImage({ src, alt = '', className }: Props) {
  const [resolved, setResolved] = useState('')
  const [failed, setFailed] = useState(false)

  useEffect(() => {
    let active = true
    let objectUrl = ''
    setResolved('')
    setFailed(false)
    if (!src) return () => { active = false }

    const value = src.trim()
    const normalized = normalizeImageUrl(value)
    const isDownload = /\/api\/v1\/car\/download\//.test(normalized)
    if (/^(data:|blob:)/i.test(value) || (/^https?:\/\//i.test(value) && !isDownload)) {
      setResolved(normalized)
      return () => { active = false }
    }

    const filename = imageFilename(value)
    if (!filename) { setFailed(true); return () => { active = false } }

    carApi.imageBlob(filename).then(blob => {
      if (!active) return
      objectUrl = URL.createObjectURL(blob)
      setResolved(objectUrl)
    }).catch(() => { if (active) setFailed(true) })

    return () => {
      active = false
      if (objectUrl) URL.revokeObjectURL(objectUrl)
    }
  }, [src])

  if (!resolved || failed) return <div className={`${className ?? ''} car-placeholder`}>CARVENTURE</div>
  return <img className={className} src={resolved} alt={alt} onError={() => setFailed(true)} />
}
