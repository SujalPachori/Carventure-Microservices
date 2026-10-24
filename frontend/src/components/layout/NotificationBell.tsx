import { useNavigate } from 'react-router-dom'
import { useQueryClient } from '@tanstack/react-query'
import { Bell } from 'lucide-react'
import { useAuth } from '../../context/AuthContext'
import { useRealtime } from '../../context/RealtimeContext'
import { notificationApi } from '../../api/notification'

export function NotificationBell(){

  const { auth } = useAuth()
  const { unreadCount, connected, resetCount } = useRealtime()
  const qc = useQueryClient()
  const nav = useNavigate()

  async function open(){
    nav('/notifications')
    if(unreadCount>0 && auth?.username){
      resetCount()
      try{
        await notificationApi.markAllRead(auth.username)
        qc.invalidateQueries({queryKey:['notifications']})
      }catch{ /* count refreshes on next poll */ }
    }
  }

  return <button
    onClick={open}
    className={unreadCount>0?'sw-bell has-unread':'sw-bell'}
    title={connected?'Live updates connected':'Live updates offline'}
    style={{background:'none',border:'none',cursor:'pointer',padding:0,color:'inherit'}}
    aria-label={`Notifications, ${unreadCount} unread`}
  >
    <Bell size={18}/>
    {unreadCount>0 && <span className="sw-badge">{unreadCount>99?'99+':unreadCount}</span>}
  </button>
}