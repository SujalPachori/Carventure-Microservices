import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import type { FormEvent } from 'react'
import { Elements, PaymentElement, useElements, useStripe } from '@stripe/react-stripe-js'
import { loadStripe } from '@stripe/stripe-js'
import type { Stripe } from '@stripe/stripe-js'
import { useNavigate, useParams, useSearchParams, Link } from 'react-router-dom'
import { carApi } from '../../api/car'
import { paymentApi } from '../../api/payment'
import { offerApi } from '../../api/offer'
import { userApi } from '../../api/user'
import { useAuth } from '../../context/AuthContext'
import { Page, PageHeader } from '../../components/common/Page'
import { ErrorState, Loading } from '../../components/common/UI'
import { money } from '../../utils/format'
import { apiMessage } from '../../api/client'
import type { PaymentType } from '../../types'

function PaymentForm({ orderId, onDone }: { orderId: number; onDone: () => void }) {
  const stripe = useStripe()
  const elements = useElements()
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  // stripe.confirmPayment()'s client-side result is NOT authoritative - a
  // network blip or a browser extension interfering with Stripe.js's iframe
  // messaging can make it report an error even though Stripe's servers
  // already processed the charge and our webhook already marked the order
  // PAID. Before trusting a client-side error, poll our own backend (the
  // webhook-driven source of truth) for a few seconds to see whether the
  // order actually went through anyway.
  async function pollForCompletion(): Promise<boolean> {
    for (let attempt = 0; attempt < 6; attempt++) {
      await new Promise(resolve => setTimeout(resolve, 1500))
      try {
        const order = await paymentApi.get(orderId)
        if (order.status === 'PAID' || order.status === 'FINANCING_ACTIVE') return true
        if (order.status === 'PAYMENT_FAILED' || order.status === 'CANCELLED' || order.status === 'EXPIRED') return false
      } catch {
        // transient fetch failure - keep polling rather than giving up early
      }
    }
    return false
  }

  async function submit(e: FormEvent) {
    e.preventDefault()
    if (!stripe || !elements) return
    setBusy(true); setError('')

    const result = await stripe.confirmPayment({ elements, redirect: 'if_required' })

    if (!result.error) {
      onDone()
      setBusy(false)
      return
    }

    // Client-side error - but check the backend before believing it.
    const actuallySucceeded = await pollForCompletion()
    if (actuallySucceeded) {
      onDone()
    } else {
      setError(result.error.message || 'Payment failed')
    }
    setBusy(false)
  }

  return <form className="card form" onSubmit={submit}><h2>Complete payment</h2><PaymentElement/><button className="btn btn-dark" disabled={!stripe || busy}>{busy ? 'Processing…' : 'Pay securely'}</button>{error && <div className="notice error">{error}</div>}</form>
}

export function Purchase() {
  const id = Number(useParams().carId)
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const offerId = params.get('offerId') || undefined
  const { auth } = useAuth()
  const profile = useQuery({ queryKey: ['profile', auth?.username, 'customer'], queryFn: () => userApi.getCustomer(auth!.username), enabled: !!auth?.username })
  const car = useQuery({ queryKey: ['car-payment', id], queryFn: () => carApi.paymentsView(id), enabled: Number.isFinite(id) })
  const offer = useQuery({ queryKey: ['offer', offerId], queryFn: () => offerApi.get(offerId!), enabled: !!offerId })
  const [type, setType] = useState<PaymentType>('FULL_PAYMENT')
  const [down, setDown] = useState('20')
  const [tenure, setTenure] = useState('36')
  const [order, setOrder] = useState<{ clientSecret: string; publishable: string; id: number } | null>(null)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  if (car.isLoading || profile.isLoading || (!!offerId && offer.isLoading)) return <Page><Loading/></Page>
  if (car.isError || !car.data) return <Page><ErrorState message="Could not load payment information for this car."/></Page>
  if (profile.isError || !profile.data?.userId) return <Page><ErrorState message="Could not load your numeric user ID from User Management. Please refresh your profile and try again."/></Page>
  if (!!offerId && (offer.isError || !offer.data)) return <Page><ErrorState message="Could not load the accepted offer for this purchase."/></Page>

  const buyerId = profile.data.userId
  const negotiated = offer.data?.status === 'ACCEPTED' ? offer.data.offeredAmount : undefined
  const agreedPrice = negotiated ?? car.data.price
    const canBuy = car.data.listed || negotiated !== undefined

  async function create(e: FormEvent) {
    e.preventDefault(); setError(''); setBusy(true)
    try {
      const result = await paymentApi.createOrder({ buyerId, carId: id, paymentType: type, downPaymentPercent: type === 'FINANCING' ? Number(down) : undefined, tenureMonths: type === 'FINANCING' ? Number(tenure) : undefined, offerId })
      setOrder({ clientSecret: result.stripeClientSecret, publishable: result.stripePublishableKeyHint, id: result.order.id })
    } catch (e) { setError(apiMessage(e)) } finally { setBusy(false) }
  }

  if (order) return <Page><PageHeader eyebrow="Checkout" title="Secure payment" description={`Order #${order.id} is awaiting payment. Complete the Stripe Payment Element below.`}/><Elements stripe={loadStripe(order.publishable) as Promise<Stripe|null>} options={{ clientSecret: order.clientSecret, appearance: { theme: 'stripe' } }}><PaymentForm orderId={order.id} onDone={() => navigate('/orders')}/></Elements><div className="notice">After payment, Stripe's webhook updates the order status in the backend. You can revisit the order from the order list.</div></Page>

  return <Page>
    <PageHeader eyebrow="Purchase" title={`Buy car #${car.data.id}`} description="Create a real purchase order through the Payment & Financing service."/>
    <div className="purchase-grid">
      <div className="card">
        <div className="eyebrow">Vehicle</div>
        <h2>Car #{car.data.id}</h2>
        <div className="detail-price">{money(agreedPrice, car.data.currency)}</div>
        {negotiated !== undefined
          ? <p className="sw-meta">Negotiated price · listed at {money(car.data.price, car.data.currency)}</p>
          : null}
        <p>{car.data.listed ? 'This listing is currently marked listed by the Car service.' : 'This car is not currently listed.'}</p>
      </div>
      <form className="card form" onSubmit={create}>
        <div className="notice">Buyer ID: <strong>{buyerId}</strong><br/><small>Loaded automatically from your User Management profile.</small></div>
        <label>Payment type<select value={type} onChange={e => setType(e.target.value as PaymentType)}><option value="FULL_PAYMENT">Full payment</option><option value="FINANCING">Financing</option></select></label>
        {type === 'FINANCING' && <>
          <label>Down payment %<input required type="number" min="5" max="90" value={down} onChange={e => setDown(e.target.value)}/></label>
          <label>Tenure (months)<input required type="number" min="3" max="84" value={tenure} onChange={e => setTenure(e.target.value)}/></label>
        </>}
        <button className="btn btn-dark" disabled={busy || !canBuy}>{busy ? 'Creating order…' : 'Create order & continue to payment'}</button>
        {error && <div className="notice error">{error}</div>}
      </form>
    </div>
    <Link className="back" to={`/cars/${id}`}>← Back to vehicle</Link>
  </Page>
}