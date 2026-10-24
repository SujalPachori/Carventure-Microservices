import { clients } from './client'
import type { Order, OrderWithPayment, PaymentType } from '../types'
export const paymentApi = {
  createOrder:(p:{buyerId:number;carId:number;paymentType:PaymentType;downPaymentPercent?:number;tenureMonths?:number;offerId?:string})=>clients.payment.post<OrderWithPayment>('/api/v1/orders',p).then(r=>r.data),
  get:(id:number)=>clients.payment.get<Order>(`/api/v1/orders/${id}`).then(r=>r.data),
  list:(params:{buyerId?:number;sellerId?:number})=>clients.payment.get<Order[]>('/api/v1/orders',{params}).then(r=>r.data),
  cancel:(id:number,buyerId:number)=>clients.payment.post<Order>(`/api/v1/orders/${id}/cancel`,null,{params:{buyerId}}).then(r=>r.data)
}