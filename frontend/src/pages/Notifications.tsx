import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { BadgeCheck, BellOff, CalendarCheck, CalendarClock, CalendarX, CheckCheck, Clock, Handshake, ThumbsDown, ThumbsUp, Trash2, Wallet, XCircle } from 'lucide-react'
import { notificationApi } from '../api/notification'
import { apiMessage } from '../api/client'
import { useAuth } from '../context/AuthContext'
import { useRealtime } from '../context/RealtimeContext'
import { Page, PageHeader } from '../components/common/Page'
import { ErrorState, Loading } from '../components/common/UI'
import { dateTime } from '../utils/format'
import type { Notification as AppNotification } from '../types'

const ICON:Record<string,{node:React.ReactNode;bg:string;fg:string}> = {
  OFFER_RECEIVED:{node:<Handshake size={17}/>,bg:'#e0e7ff',fg:'#3730a3'},
  OFFER_COUNTERED:{node:<Handshake size={17}/>,bg:'#fef3c7',fg:'#92400e'},
  OFFER_ACCEPTED:{node:<ThumbsUp size={17}/>,bg:'#dcfce7',fg:'#166534'},
  OFFER_REJECTED:{node:<ThumbsDown size={17}/>,bg:'#fee2e2',fg:'#991b1b'},
  OFFER_CLOSED:{node:<XCircle size={17}/>,bg:'#f1f5f9',fg:'#475569'},
  OFFER_EXPIRED:{node:<Clock size={17}/>,bg:'#f1f5f9',fg:'#475569'},
  CAR_SOLD:{node:<BadgeCheck size={17}/>,bg:'#dcfce7',fg:'#166534'},
  PAYMENT_COMPLETED:{node:<Wallet size={17}/>,bg:'#dcfce7',fg:'#166534'},
  TEST_DRIVE_REQUESTED:{node:<CalendarClock size={17}/>,bg:'#e0e7ff',fg:'#3730a3'},
  TEST_DRIVE_CONFIRMED:{node:<CalendarCheck size={17}/>,bg:'#dcfce7',fg:'#166534'},
  TEST_DRIVE_REJECTED:{node:<CalendarX size={17}/>,bg:'#fee2e2',fg:'#991b1b'},
  TEST_DRIVE_CANCELLED:{node:<CalendarX size={17}/>,bg:'#f1f5f9',fg:'#475569'},
  TEST_DRIVE_RESCHEDULED:{node:<CalendarClock size={17}/>,bg:'#fef3c7',fg:'#92400e'}
}

export function Notifications(){

  const { auth } = useAuth()
  const { refreshCount, resetCount, onNotification, connected } = useRealtime()
  const qc = useQueryClient()
  const nav = useNavigate()

  const [error,setError] = useState('')
  const username = auth?.username
  const roles = auth?.roles instanceof Set ? Array.from(auth.roles) : auth?.roles || []
  const isSeller = roles.includes('SELLER') && !roles.includes('CUSTOMER')

  const q = useQuery({
    queryKey:['notifications',username],
    queryFn:()=>notificationApi.list(username!,0,50),
    enabled:!!username
  })

  useEffect(()=>{
    return onNotification(()=>qc.invalidateQueries({queryKey:['notifications']}))
  },[onNotification,qc])

  async function open(notification:AppNotification){
    try{
      if(!notification.read){
        await notificationApi.markRead(notification.notificationId)
        refreshCount()
        qc.invalidateQueries({queryKey:['notifications']})
      }
      if(notification.referenceType==='OFFER') nav('/offers')
      if(notification.referenceType==='TEST_DRIVE') nav(isSeller?'/seller/test-drives':'/buyer/test-drives')
      if(notification.referenceType==='ORDER') nav('/orders')
    }catch(e){ setError(apiMessage(e)) }
  }

  async function markAll(){
    if(!username) return
    resetCount()
    try{
      await notificationApi.markAllRead(username)
      qc.invalidateQueries({queryKey:['notifications']})
    }catch(e){ setError(apiMessage(e)) }
  }

  async function remove(event:React.MouseEvent, notification:AppNotification){
    event.stopPropagation()
    try{
      await notificationApi.remove(notification.notificationId)
      refreshCount()
      qc.invalidateQueries({queryKey:['notifications']})
    }catch(e){ setError(apiMessage(e)) }
  }

  if(!username) return <Page><ErrorState message="Sign in to see your notifications."/></Page>

  const items = q.data?.content || []
  const unread = items.filter(n=>!n.read).length

  return <Page>
    <PageHeader
      eyebrow="Updates"
      title="Notifications"
      description="Offer, test drive and payment activity, delivered live."
      actions={<>
        <span className={connected?'sw-live':'sw-live is-off'} style={{marginRight:12}}>
          <span className="sw-live-dot"/>{connected?'Live':'Offline'}
        </span>
        {unread>0 && <button className="sw-btn" onClick={markAll}><CheckCheck size={16}/> Mark all read</button>}
      </>}
    />

    {error && <div className="notice error">{error}</div>}

    {q.isLoading ? <Loading/>
      : q.isError ? <ErrorState message="Could not load notifications."/>
      : !items.length ? <div className="sw-empty">
          <BellOff size={34}/>
          <strong>Nothing here yet.</strong>
          <span>Activity will show up here as it happens.</span>
        </div>
      : <div>
          {items.map(n=>{
            const style = ICON[n.type] || {node:<Handshake size={17}/>,bg:'#f1f5f9',fg:'#475569'}
            return <div
              key={n.notificationId}
              className={n.read?'sw-note':'sw-note is-unread'}
              onClick={()=>open(n)}
            >
              <div className="sw-note-icon" style={{background:style.bg,color:style.fg}}>{style.node}</div>
              <div style={{flex:1,minWidth:0}}>
                <div style={{fontWeight:n.read?550:700,fontSize:15}}>{n.title}</div>
                <div className="sw-sub">{n.message}</div>
                <div className="sw-meta">{dateTime(n.createdAt)}</div>
              </div>
              <button className="sw-btn sw-btn-danger" title="Dismiss"
                style={{padding:'7px 9px'}} onClick={(e)=>remove(e,n)}>
                <Trash2 size={15}/>
              </button>
            </div>
          })}
        </div>}
  </Page>
}