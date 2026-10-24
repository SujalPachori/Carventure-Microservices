import { clients } from './client'
import type { Offer } from '../types'
export const offerApi = {
  create:(p:{carId:string;customerId:string;offeredAmount:number})=>clients.offer.post<Offer>('/api/v1/offers',p).then(r=>r.data),
  get:(offerId:string)=>clients.offer.get<Offer>(`/api/v1/offers/${offerId}`).then(r=>r.data),
  byCar:(carId:string)=>clients.offer.get<Offer[]>(`/api/v1/offers/car/${carId}`).then(r=>r.data),
  byCustomer:(customerId:string)=>clients.offer.get<Offer[]>(`/api/v1/offers/customer/${encodeURIComponent(customerId)}`).then(r=>r.data),
  bySeller:(sellerId:string)=>clients.offer.get<Offer[]>(`/api/v1/offers/seller/${encodeURIComponent(sellerId)}`).then(r=>r.data),
  accept:(offerId:string,actorId:string)=>clients.offer.put<Offer>(`/api/v1/offers/${offerId}/accept`,null,{params:{actorId}}).then(r=>r.data),
  reject:(offerId:string,actorId:string)=>clients.offer.put<Offer>(`/api/v1/offers/${offerId}/reject`,null,{params:{actorId}}).then(r=>r.data),
  counter:(offerId:string,actorId:string,amount:number)=>clients.offer.put<Offer>(`/api/v1/offers/${offerId}/counter`,{actorId,amount}).then(r=>r.data),
  withdraw:(offerId:string,actorId:string)=>clients.offer.delete(`/api/v1/offers/${offerId}`,{params:{actorId}})
}