import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Star } from 'lucide-react'
import { carApi } from '../../api/car'
import { reviewApi } from '../../api/engagement'
import { apiMessage } from '../../api/client'
import { useAuth } from '../../context/AuthContext'

export function ReviewForm({carId}:{carId:number}){

  const {auth}=useAuth()
  const me=auth!.username
  const qc=useQueryClient()

  const [rating,setRating]=useState(0)
  const [hover,setHover]=useState(0)
  const [comment,setComment]=useState('')

  // Reviews are keyed by seller username; orders only carry the numeric
  // sellerId, so resolve it from the car this order was for.
  const car=useQuery({queryKey:['car',carId],queryFn:()=>carApi.get(carId)})
  const sellerId=car.data?.sellerUsername

  const existing=useQuery({
    queryKey:['reviews',sellerId],
    queryFn:()=>reviewApi.bySeller(sellerId!,0,100),
    enabled:!!sellerId
  })

  const alreadyReviewed=existing.data?.content?.some(r=>r.customerId===me)

  const submit=useMutation({
    mutationFn:()=>reviewApi.submit({sellerId:sellerId!,customerId:me,rating,comment:comment.trim()||undefined}),
    onSuccess:()=>{
      qc.invalidateQueries({queryKey:['reviews',sellerId]})
      qc.invalidateQueries({queryKey:['reviewSummary',sellerId]})
    }
  })

  if(car.isLoading||existing.isLoading) return null
  if(!sellerId) return null
  if(alreadyReviewed||submit.isSuccess) return <span className="sw-meta">You reviewed this seller.</span>

  return <div className="sw-review-form">
    <div className="sw-stars" onMouseLeave={()=>setHover(0)}>
      {[1,2,3,4,5].map(n=>
        <button key={n} type="button" className="sw-star"
          onMouseEnter={()=>setHover(n)} onClick={()=>setRating(n)}
          aria-label={`${n} star${n===1?'':'s'}`}>
          <Star size={20} fill={(hover||rating)>=n?'currentColor':'none'}/>
        </button>)}
    </div>
    <input className="sw-input" placeholder="Add a comment (optional)"
      value={comment} onChange={e=>setComment(e.target.value)}/>
    <button className="sw-btn sw-btn-primary" disabled={!rating||submit.isPending}
      onClick={()=>submit.mutate()}>
      {submit.isPending?'Sending…':'Submit review'}
    </button>
    {submit.isError && <span className="error-text">{apiMessage(submit.error)}</span>}
  </div>
}