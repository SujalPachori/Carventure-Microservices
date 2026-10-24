import { createContext, useCallback, useContext, useEffect, useRef, useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { useAuth } from './AuthContext'
import { notificationApi } from '../api/notification'
import type { Notification } from '../types'

type Handler = (payload:Notification) => void

type RealtimeValue = {
  unreadCount:number
  connected:boolean
  refreshCount:()=>void
  decrementCount:()=>void
  resetCount:()=>void
  onNotification:(handler:Handler)=>()=>void
}

const RealtimeContext = createContext<RealtimeValue|null>(null)

export function RealtimeProvider({children}:{children:React.ReactNode}){

  const { auth } = useAuth()
  const qc = useQueryClient()

  const [unreadCount,setUnreadCount] = useState(0)
  const [connected,setConnected] = useState(false)
  const handlers = useRef<Handler[]>([])

  const username = auth?.username

  const refreshCount = useCallback(()=>{
    if(!username) return
    notificationApi.unreadCount(username).then(setUnreadCount).catch(()=>{})
  },[username])

  const onNotification = useCallback((handler:Handler)=>{
    handlers.current.push(handler)
    return () => { handlers.current = handlers.current.filter(h=>h!==handler) }
  },[])

  useEffect(()=>{

    if(!username){ setUnreadCount(0); setConnected(false); return }

    refreshCount()

    const source = new EventSource(notificationApi.streamUrl(username))

    source.addEventListener('connected',()=>setConnected(true))

    source.addEventListener('unreadCount',(event)=>{
      const value = Number((event as MessageEvent).data)
      if(!Number.isNaN(value)) setUnreadCount(value)
    })

    source.addEventListener('notification',(event)=>{
      try {
        const notification = JSON.parse((event as MessageEvent).data) as Notification
        handlers.current.forEach(handler=>handler(notification))
        qc.invalidateQueries({queryKey:['notifications']})
        if(notification.referenceType==='OFFER') qc.invalidateQueries({queryKey:['offers']})
      } catch { /* ignore malformed frame */ }
    })

    source.onerror = () => setConnected(false)

    const poll = window.setInterval(refreshCount, 60_000)

    return () => { source.close(); window.clearInterval(poll) }

  },[username, refreshCount, qc])

  const value:RealtimeValue = {
    unreadCount,
    connected,
    refreshCount,
    decrementCount: () => setUnreadCount(c=>Math.max(0,c-1)),
    resetCount: () => setUnreadCount(0),
    onNotification
  }

  return <RealtimeContext.Provider value={value}>{children}</RealtimeContext.Provider>
}

export const useRealtime = () => {
  const context = useContext(RealtimeContext)
  if(!context) throw new Error('useRealtime must be used within RealtimeProvider')
  return context
}