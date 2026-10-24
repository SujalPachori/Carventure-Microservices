import { useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import { offerApi } from '../../api/offer'
import { apiMessage } from '../../api/client'
import { Success } from '../common/UI'
import { useAuth } from '../../context/AuthContext'

export function MakeOfferForm({carId}:{carId:string}){
  const {auth}=useAuth(); const [amount,setAmount]=useState('')
  const m=useMutation({mutationFn:()=>offerApi.create({carId,customerId:auth!.username,offeredAmount:Number(amount)})})
  if(m.isSuccess) return <Success>Offer sent. You'll be notified when the seller responds.</Success>
  return <div className="offer-form">
    <input type="number" value={amount} placeholder="Your offer ($)" onChange={e=>setAmount(e.target.value)}/>
    <button className="btn btn-dark" disabled={!amount||m.isPending} onClick={()=>m.mutate()}>
      {m.isPending?'Sending…':'Make an offer'}</button>
    {m.isError&&<span className="error-text">{apiMessage(m.error)}</span>}
  </div>
}