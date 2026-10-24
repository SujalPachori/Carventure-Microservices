export const money = (value:number|undefined, currency='USD') => value == null ? '—' : new Intl.NumberFormat('en-US',{style:'currency',currency,maximumFractionDigits:0}).format(value)
export const dateTime = (value?:string) => {
  if(!value) return '—'
  const d = new Date(value)
  return isNaN(d.getTime()) ? '—' : new Intl.DateTimeFormat(undefined,{dateStyle:'medium',timeStyle:'short'}).format(d)
}
export const dateOnly = (value?:string) => value ? new Intl.DateTimeFormat(undefined,{dateStyle:'medium'}).format(new Date(`${value}T00:00:00`)) : '—'
export const titleCase = (s:string) => s.toLowerCase().replace(/_/g,' ').replace(/\b\w/g,c=>c.toUpperCase())
export const initials = (s:string) => s.split(/[ ._-]+/).filter(Boolean).slice(0,2).map(x=>x[0].toUpperCase()).join('')
