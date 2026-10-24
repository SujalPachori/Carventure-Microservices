import { useEffect } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { notificationApi } from '../api/notification'
import { useAuth } from '../context/AuthContext'

export function useUnreadCount(){
  const {auth}=useAuth()
  const qc=useQueryClient()
  const username=auth?.username
  const q=useQuery({queryKey:['unreadCount',username],queryFn:()=>notificationApi.unreadCount(username!),enabled:!!username})

  useEffect(()=>{
    if(!username) return
    const es=new EventSource(notificationApi.streamUrl(username))
    es.addEventListener('unreadCount',(e)=>{
      qc.setQueryData(['unreadCount',username],Number((e as MessageEvent).data))
    })
    es.addEventListener('notification',()=>{
      qc.invalidateQueries({queryKey:['unreadCount',username]})
      qc.invalidateQueries({queryKey:['notifications',username]})
    })
    es.onerror=()=>{es.close()}
    return ()=>es.close()
  },[username,qc])

  return q.data??0
}