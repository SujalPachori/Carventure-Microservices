import axios, { AxiosError } from 'axios'

export const clients = {
  auth: axios.create({baseURL:'/backend/auth'}),
  car: axios.create({baseURL:'/backend/car'}),
  payment: axios.create({baseURL:'/backend/payment'}),
  scheduling: axios.create({baseURL:'/backend/scheduling'}),
  governance: axios.create({baseURL:'/backend/governance'}),
  reporting: axios.create({baseURL:'/backend/reporting'}),
  user: axios.create({baseURL:'/backend/user'}),
  offer: axios.create({baseURL:'/backend/offer'}),
  communication: axios.create({baseURL:'/backend/communication'}),
  engagement: axios.create({baseURL:'/backend/engagement'})
}

const token = () => localStorage.getItem('carventure_access_token')
const refresh = () => localStorage.getItem('carventure_refresh_token')
let refreshing: Promise<string|null>|null = null

for (const client of Object.values(clients)) {
  client.interceptors.request.use(config => {
    const t = token()
    if (t) config.headers.Authorization = `Bearer ${t}`
    return config
  })
  client.interceptors.response.use(r => r, async (error:AxiosError) => {
    const original = error.config as typeof error.config & {_retry?:boolean}|undefined
    if (error.response?.status === 401 && original && !original._retry && refresh()) {
      original._retry = true
      try {
        refreshing ??= clients.auth.post('/api/auth/refreshToken',{refreshToken:refresh()}).then(({data})=>{
          localStorage.setItem('carventure_access_token',data.accessToken)
          localStorage.setItem('carventure_refresh_token',data.refreshToken)
          localStorage.setItem('carventure_auth',JSON.stringify(data))
          return data.accessToken as string
        }).catch(()=>null).finally(()=>{refreshing=null})
        const next = await refreshing
        if (next) { original.headers.Authorization = `Bearer ${next}`; return client(original) }
      } catch { /* fall through */ }
    }
    return Promise.reject(error)
  })
}

export function apiMessage(error:unknown, fallback='Something went wrong') {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data as {message?:string; error?:string; details?:string}|undefined
    return data?.message || data?.error || data?.details || error.message || fallback
  }
  return error instanceof Error ? error.message : fallback
}
