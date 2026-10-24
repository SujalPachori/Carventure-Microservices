import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Handshake, Wallet } from 'lucide-react'
import { offerApi } from '../api/offer'
import { carApi } from '../api/car'
import { apiMessage } from '../api/client'
import { useAuth } from '../context/AuthContext'
import { useRealtime } from '../context/RealtimeContext'
import { Page, PageHeader } from '../components/common/Page'
import { ErrorState, Loading } from '../components/common/UI'
import { dateTime, money } from '../utils/format'
import type { Offer } from '../types'
import { CarImage } from '../components/car/CarImage'

export function Offers(){

  const { auth } = useAuth()
  const { onNotification, connected } = useRealtime()
  const qc = useQueryClient()
  const nav = useNavigate()

  const roles = auth?.roles instanceof Set ? Array.from(auth.roles) : auth?.roles || []
  const isSeller = roles.includes('SELLER') && !roles.includes('CUSTOMER')
  const username = auth?.username

  const [error,setError] = useState('')
  const [counteringId,setCounteringId] = useState<string|null>(null)
  const [amount,setAmount] = useState('')
  const [busy,setBusy] = useState(false)
  const [flashIds,setFlashIds] = useState<string[]>([])

  const q = useQuery({
    queryKey:['offers',username,isSeller?'seller':'customer'],
    queryFn:()=> isSeller ? offerApi.bySeller(username!) : offerApi.byCustomer(username!),
    enabled:!!username
  })

  // An offer stays ACCEPTED after the buyer pays, so the offer alone can't tell
  // us whether payment already happened. The car's status can: SOLD means paid.
  const carStatus = useQuery({
    queryKey:['offer-car-status', username, q.data?.length],
    queryFn: async () => {
      const ids = [...new Set((q.data||[]).filter(o=>o.status==='ACCEPTED').map(o=>o.carId))]
      const results = await Promise.all(ids.map(id => carApi.get(Number(id)).then(c=>[String(id), c.status] as const).catch(()=>null)))
      return Object.fromEntries(results.filter(Boolean) as (readonly [string,string])[])
    },
    enabled: !!q.data && q.data.some(o=>o.status==='ACCEPTED')
  })

  const isPaid = (o:Offer) => carStatus.data?.[o.carId] === 'SOLD'

  useEffect(()=>{
    return onNotification(notification=>{
      if(notification.referenceType!=='OFFER' && notification.referenceType!=='ORDER') return
      qc.invalidateQueries({queryKey:['offers']})
      qc.invalidateQueries({queryKey:['offer-car-status']})
      if(notification.referenceId){
        const id = notification.referenceId
        setFlashIds(current=>[...current,id])
        window.setTimeout(()=>setFlashIds(current=>current.filter(x=>x!==id)),1600)
      }
    })
  },[onNotification,qc])

  async function run(action:()=>Promise<unknown>){
    setBusy(true); setError('')
    try{ await action(); await qc.invalidateQueries({queryKey:['offers']}) }
    catch(e){ setError(apiMessage(e)) }
    finally{ setBusy(false) }
  }

  async function sendCounter(offer:Offer){
    const value = Number(amount)
    if(!value || value<=0){ setError('Enter a valid counter amount.'); return }
    await run(()=>offerApi.counter(offer.offerId, username!, value))
    setCounteringId(null); setAmount('')
  }

  const waitingOnMe = (o:Offer) => o.status==='PENDING' &&
    (o.offeredBy==='CUSTOMER' ? o.sellerId===username : o.customerId===username)

  const canWithdraw = (o:Offer) => o.status==='PENDING' && o.offeredBy==='CUSTOMER' && o.customerId===username

  const canPay = (o:Offer) => o.status==='ACCEPTED' && o.customerId===username && !isPaid(o)

  if(!username) return <Page><ErrorState message="Sign in to view offers."/></Page>

  const offers = q.data || []
  const active = offers.filter(o=>o.status==='PENDING')
  const settled = offers.filter(o=>o.status!=='PENDING')

  const card = (offer:Offer) => <div
    key={offer.offerId}
    className={flashIds.includes(offer.offerId)?'sw-card is-live':'sw-card'}
  >
    <div className="sw-row">
      {offer.carImageUrl
          ? <CarImage className="sw-thumb" src={offer.carImageUrl} alt={`${offer.carBrand} ${offer.carModel}`}/>
          : <div className="sw-thumb-empty">NO IMAGE</div>}
      <div>
        <div className="sw-title">{offer.carBrand||'Car'} {offer.carModel||''}</div>
        <div className="sw-sub">
          Listed at {money(offer.carPrice)} · <Link to={`/cars/${offer.carId}`}>view listing</Link>
        </div>
      </div>
      <span className={`sw-pill sw-pill-${offer.status.toLowerCase()} sw-spacer`}>{offer.status}</span>
    </div>

    <div className="sw-divider">
      <div className="sw-amount-label">
        {offer.offeredBy==='SELLER'
          ? (isSeller?'You countered':'Seller countered')
          : (isSeller?`${offer.customerName||offer.customerId} offered`:'You offered')}
      </div>
      <div className="sw-amount">{money(offer.offeredAmount)}</div>
      <div className="sw-meta">{dateTime(offer.createdAt)}</div>
    </div>

    {waitingOnMe(offer) && counteringId!==offer.offerId &&
      <div className="sw-actions">
        <button className="sw-btn sw-btn-success" disabled={busy}
          onClick={()=>run(()=>offerApi.accept(offer.offerId,username))}>
          Accept {money(offer.offeredAmount)}
        </button>
        <button className="sw-btn" disabled={busy}
          onClick={()=>{setCounteringId(offer.offerId);setAmount('');setError('')}}>
          Counter
        </button>
        <button className="sw-btn sw-btn-danger" disabled={busy}
          onClick={()=>run(()=>offerApi.reject(offer.offerId,username))}>
          Reject
        </button>
      </div>}

    {counteringId===offer.offerId &&
      <div className="sw-actions">
        <input className="sw-input" type="number" placeholder="Your counter amount"
          value={amount} onChange={e=>{setAmount(e.target.value);setError('')}}/>
        <button className="sw-btn sw-btn-primary" disabled={busy} onClick={()=>sendCounter(offer)}>Send counter</button>
        <button className="sw-btn" onClick={()=>{setCounteringId(null);setError('')}}>Cancel</button>
      </div>}

    {canWithdraw(offer) && counteringId!==offer.offerId &&
      <div className="sw-actions">
        <span className="sw-meta">Waiting for the seller to respond.</span>
        <button className="sw-btn sw-btn-danger sw-spacer" disabled={busy}
          onClick={()=>run(()=>offerApi.withdraw(offer.offerId,username))}>
          Withdraw
        </button>
      </div>}

    {canPay(offer) &&
      <div className="sw-actions">
        <button className="sw-btn sw-btn-primary" onClick={()=>nav(`/buyer/purchase/${offer.carId}?offerId=${offer.offerId}`)}>
          <Wallet size={16}/> Proceed to payment
        </button>
        <span className="sw-meta">Your offer was accepted — payment is now unlocked.</span>
      </div>}

    {offer.status==='ACCEPTED' && offer.customerId===username && isPaid(offer) &&
      <div className="sw-actions">
        <span className="sw-meta">Paid — this car is yours.</span>
        <Link className="sw-btn sw-spacer" to="/orders">View order</Link>
      </div>}

    {offer.status==='ACCEPTED' && isSeller && isPaid(offer) &&
      <div className="sw-actions"><span className="sw-meta">Paid — this sale is complete.</span></div>}

    {offer.status==='CLOSED' &&
      <div className="sw-actions"><span className="sw-meta">This offer is no longer active.</span></div>}
  </div>

  return <Page>
    <PageHeader
      eyebrow="Negotiation"
      title={isSeller?'Offers on your listings':'My offers'}
      description={isSeller
        ? 'Accept, counter or reject buyer offers. Accepting one automatically closes every other offer on that car.'
        : 'Track your offers. Payment unlocks only once a seller accepts.'}
      actions={<span className={connected?'sw-live':'sw-live is-off'}>
        <span className="sw-live-dot"/>{connected?'Live':'Offline'}
      </span>}
    />

    {error && <div className="notice error">{error}</div>}

    {q.isLoading ? <Loading/>
      : q.isError ? <ErrorState message="Could not load offers."/>
      : !offers.length ? <div className="sw-empty">
          <Handshake size={34}/>
          <strong>No offers yet.</strong>
          <span>{isSeller?'Buyer offers on your listings will appear here.':'Browse cars and make an offer to start negotiating.'}</span>
          {!isSeller && <Link className="sw-btn sw-btn-primary" to="/cars">Browse cars</Link>}
        </div>
      : <>
          {active.length>0 && <div className="sw-sub" style={{marginBottom:10}}>
            {active.length} active negotiation{active.length===1?'':'s'}
          </div>}
          {active.map(card)}
          {settled.length>0 && <div className="section-title" style={{marginTop:28,marginBottom:12}}>History</div>}
          {settled.map(card)}
        </>}
  </Page>
}