import { useQuery } from '@tanstack/react-query'
import { Star } from 'lucide-react'
import { reviewApi } from '../../api/engagement'
import { dateTime } from '../../utils/format'

function Stars({value,size=14}:{value:number;size?:number}){
  return <span className="sw-stars-static">
    {[1,2,3,4,5].map(n=>
      <Star key={n} size={size} fill={value>=n?'currentColor':'none'}
        strokeWidth={value>=n?0:1.5}/>)}
  </span>
}

export function SellerReviews({sellerId}:{sellerId:string}){

  const summary=useQuery({queryKey:['reviewSummary',sellerId],queryFn:()=>reviewApi.summary(sellerId)})
  const list=useQuery({queryKey:['reviews',sellerId],queryFn:()=>reviewApi.bySeller(sellerId)})

  const reviews=list.data?.content??[]
  const count=summary.data?.reviewCount??0
  const average=summary.data?.averageRating??0

  return <section className="sw-reviews">
    <div className="sw-reviews-head">
      <h3>Seller reviews</h3>
      {count>0 && <div className="sw-reviews-score">
        <span className="sw-score-value">{average.toFixed(1)}</span>
        <Stars value={Math.round(average)} size={16}/>
        <span className="sw-meta">{count} review{count===1?'':'s'}</span>
      </div>}
    </div>

    {count===0
      ? <p className="sw-meta">No reviews yet for this seller.</p>
      : <div className="sw-reviews-list">
          {reviews.map(r=>
            <div key={r.reviewId} className="sw-review">
              <div className="sw-avatar">{r.customerId.charAt(0).toUpperCase()}</div>
              <div className="sw-review-body">
                <div className="sw-review-top">
                  <strong>{r.customerId}</strong>
                  <Stars value={r.rating}/>
                  <span className="sw-meta sw-spacer">{dateTime(r.createdAt)}</span>
                </div>
                {r.comment && <p className="sw-review-text">{r.comment}</p>}
              </div>
            </div>)}
        </div>}
  </section>
}