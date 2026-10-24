import { clients } from './client'
import type { ListingVerification, Report, TestDrive } from '../types'
export const adminApi = {
  verifyListing:(carId:number,p:{decision:string;reason?:string;verifiedByAdminUsername?:string})=>clients.governance.put<ListingVerification>(`/api/v1/governance/verifyListing/${carId}`,p).then(r=>r.data),
  resolveDispute:(id:number,p:{status:string;resolutionNotes?:string;resolvedByAdminUsername?:string})=>clients.governance.put(`/api/v1/governance/resolveDispute/${id}`,p).then(r=>r.data),
  reports:()=>clients.reporting.get<Report[]>('/api/v1/reporting/viewReports').then(r=>r.data),
  testDrives:()=>clients.scheduling.get<TestDrive[]>('/api/v1/test-drives').then(r=>r.data)
}
