import { clients } from './client'
import type { AccountStatus, UserProfile } from '../types'
export const userApi = {
  registerCustomer:(p:UserProfile)=>clients.user.post<UserProfile>('/api/v1/user/registerCustomer',p).then(r=>r.data),
  registerSeller:(p:UserProfile)=>clients.user.post<UserProfile>('/api/v1/user/registerSeller',p).then(r=>r.data),
  customers:()=>clients.user.get<UserProfile[]>('/api/v1/user/viewAllCustomers').then(r=>r.data),
  sellers:()=>clients.user.get<UserProfile[]>('/api/v1/user/viewAllSellers').then(r=>r.data),
  getCustomer:(u:string)=>clients.user.get<UserProfile>(`/api/v1/user/viewCustomer/${encodeURIComponent(u)}`).then(r=>r.data),
  getSeller:(u:string)=>clients.user.get<UserProfile>(`/api/v1/user/viewSeller/${encodeURIComponent(u)}`).then(r=>r.data),
  updateCustomer:(u:string,p:UserProfile)=>clients.user.put<UserProfile>(`/api/v1/user/updateCustomer/${encodeURIComponent(u)}`,p).then(r=>r.data),
  updateSeller:(u:string,p:UserProfile)=>clients.user.put<UserProfile>(`/api/v1/user/updateSeller/${encodeURIComponent(u)}`,p).then(r=>r.data),
  updateSellerStatus:(u:string,status:AccountStatus)=>clients.user.put<UserProfile>(`/api/v1/user/updateAccountStatus/${encodeURIComponent(u)}`,null,{params:{accountStatus:status}}).then(r=>r.data),
  deleteCustomer:(u:string)=>clients.user.delete(`/api/v1/user/deleteCustomer/${encodeURIComponent(u)}`),
  deleteSeller:(u:string)=>clients.user.delete(`/api/v1/user/deleteSeller/${encodeURIComponent(u)}`),
  exists:(u:string)=>clients.user.get<boolean>(`/api/v1/user/exists/${encodeURIComponent(u)}`).then(r=>r.data),
  dispute:(p:unknown)=>clients.user.post('/api/v1/user/dispute',p).then(r=>r.data)
}
