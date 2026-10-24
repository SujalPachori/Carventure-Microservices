import { clients } from './client'
import type { Review, ReviewSummary, SpringPage, WishlistItem } from '../types'

export const wishlistApi = {
  add:(customerId:string,carId:string)=>clients.engagement.post<WishlistItem>('/api/v1/wishlist',{customerId,carId}).then(r=>r.data),
  list:(customerId:string)=>clients.engagement.get<WishlistItem[]>(`/api/v1/wishlist/${encodeURIComponent(customerId)}`).then(r=>r.data),
  contains:(customerId:string,carId:string)=>clients.engagement.get<boolean>(`/api/v1/wishlist/${encodeURIComponent(customerId)}/contains/${carId}`).then(r=>r.data),
  remove:(customerId:string,carId:string)=>clients.engagement.delete(`/api/v1/wishlist/${encodeURIComponent(customerId)}/${carId}`)
}

export const reviewApi = {
  submit:(p:{sellerId:string;customerId:string;rating:number;comment?:string})=>clients.engagement.post<Review>('/api/v1/reviews',p).then(r=>r.data),
  bySeller:(sellerId:string,page=0,size=10)=>clients.engagement.get<SpringPage<Review>>(`/api/v1/reviews/seller/${encodeURIComponent(sellerId)}`,{params:{page,size}}).then(r=>r.data),
  summary:(sellerId:string)=>clients.engagement.get<ReviewSummary>(`/api/v1/reviews/seller/${encodeURIComponent(sellerId)}/summary`).then(r=>r.data),
  update:(reviewId:string,p:{customerId:string;rating:number;comment?:string})=>clients.engagement.put<Review>(`/api/v1/reviews/${reviewId}`,p).then(r=>r.data),
  remove:(reviewId:string,customerId:string)=>clients.engagement.delete(`/api/v1/reviews/${reviewId}`,{params:{customerId}})
}