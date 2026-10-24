import { clients } from './client'
import type { Notification, SpringPage } from '../types'
export const notificationApi = {
  list:(userId:string,page=0,size=20)=>clients.communication.get<SpringPage<Notification>>(`/api/v1/notifications/${encodeURIComponent(userId)}`,{params:{page,size}}).then(r=>r.data),
  unreadOnly:(userId:string,page=0,size=20)=>clients.communication.get<SpringPage<Notification>>(`/api/v1/notifications/${encodeURIComponent(userId)}`,{params:{page,size,unreadOnly:true}}).then(r=>r.data),
  unreadCount:(userId:string)=>clients.communication.get<number>(`/api/v1/notifications/unreadCount/${encodeURIComponent(userId)}`).then(r=>r.data),
  markRead:(notificationId:string)=>clients.communication.put<Notification>(`/api/v1/notifications/${notificationId}/read`).then(r=>r.data),
  markAllRead:(userId:string)=>clients.communication.put(`/api/v1/notifications/markAllRead/${encodeURIComponent(userId)}`),
  remove:(notificationId:string)=>clients.communication.delete(`/api/v1/notifications/${notificationId}`),
  streamUrl:(userId:string)=>`/backend/communication/api/v1/stream/${encodeURIComponent(userId)}`
}