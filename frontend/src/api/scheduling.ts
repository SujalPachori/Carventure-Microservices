import { clients } from './client'
import type { Availability, TestDrive } from '../types'
export const schedulingApi = {
  availability:(carId:number)=>clients.scheduling.get<Availability[]>(`/api/v1/cars/${carId}/availability`).then(r=>r.data),
  createAvailability:(p:{carId:number;dayOfWeek:string;startTime:string;endTime:string;durationMinutes?:number})=>clients.scheduling.post<Availability>('/api/v1/availability',p).then(r=>r.data),
  updateAvailability:(id:number,p:{startTime?:string;endTime?:string;active?:boolean})=>clients.scheduling.put<Availability>(`/api/v1/availability/${id}`,p).then(r=>r.data),
  deactivateAvailability:(id:number)=>clients.scheduling.delete(`/api/v1/availability/${id}`),
  createTestDrive:(p:{carId:number;startTime:string})=>clients.scheduling.post<TestDrive>('/api/v1/test-drives',p).then(r=>r.data),
  mine:()=>clients.scheduling.get<TestDrive[]>('/api/v1/test-drives/mine').then(r=>r.data),
  all:()=>clients.scheduling.get<TestDrive[]>('/api/v1/test-drives').then(r=>r.data),
  confirm:(id:number)=>clients.scheduling.post<TestDrive>(`/api/v1/test-drives/${id}/confirm`).then(r=>r.data),
  reject:(id:number,reason?:string)=>clients.scheduling.post<TestDrive>(`/api/v1/test-drives/${id}/reject`,reason?{reason}:{}).then(r=>r.data),
  cancel:(id:number,reason?:string)=>clients.scheduling.post<TestDrive>(`/api/v1/test-drives/${id}/cancel`,reason?{reason}:{}).then(r=>r.data),
  reschedule:(id:number,startTime:string)=>clients.scheduling.post<TestDrive>(`/api/v1/test-drives/${id}/reschedule`,{startTime}).then(r=>r.data)
}
