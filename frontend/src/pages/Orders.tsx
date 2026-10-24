import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { paymentApi } from '../api/payment'
import { userApi } from '../api/user'
import { useAuth } from '../context/AuthContext'
import { Page, PageHeader } from '../components/common/Page'
import { EmptyState, ErrorState, Loading, StatusBadge } from '../components/common/UI'
import { dateTime, money, titleCase } from '../utils/format'
import { apiMessage } from '../api/client'
import { ReviewForm } from '../components/car/ReviewForm'

export function Orders() {

  const { auth } = useAuth()
  const roles = auth?.roles instanceof Set ? Array.from(auth.roles) : auth?.roles || []
  const isSeller = roles.includes('SELLER') && !roles.includes('CUSTOMER')

  const profile = useQuery({
    queryKey: ['order-profile', auth?.username, isSeller ? 'seller' : 'customer'],
    queryFn: () => isSeller ? userApi.getSeller(auth!.username) : userApi.getCustomer(auth!.username),
    enabled: !!auth?.username
  })

  const buyerId = !isSeller ? profile.data?.userId : undefined
  const sellerId = isSeller ? profile.data?.userId : undefined

  const q = useQuery({
    queryKey: ['orders', isSeller ? sellerId : buyerId],
    queryFn: () => paymentApi.list(isSeller ? { sellerId } : { buyerId }),
    enabled: !!profile.data?.userId
  })

  const qc = useQueryClient()
  const [error, setError] = useState('')

  async function cancel(id: number) {
    if (!buyerId) return
    try {
      await paymentApi.cancel(id, buyerId)
      qc.invalidateQueries({ queryKey: ['orders'] })
    } catch (e) { setError(apiMessage(e)) }
  }

  if (profile.isLoading) return <Page><Loading/></Page>
  if (profile.isError || !profile.data?.userId) return <Page><ErrorState message="Could not load your numeric user ID from User Management."/></Page>

  return <Page>
    <PageHeader
      eyebrow="Commerce"
      title={isSeller ? 'Orders for your cars' : 'My orders'}
      description="Orders are loaded using the numeric userId returned by User Management."
    />

    {error && <div className="notice error">{error}</div>}

    {q.isLoading ? <Loading/>
      : q.isError ? <ErrorState message="Could not load orders."/>
      : q.data?.length ? <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Order</th>
                {isSeller && <th>Buyer</th>}
                <th>Car</th>
                <th>Amount</th>
                <th>Type</th>
                <th>Status</th>
                <th>Created</th>
                <th></th>
                {!isSeller && <th>Review</th>}
              </tr>
            </thead>
            <tbody>
              {q.data.map(o =>
                <tr key={o.id}>
                  <td>#{o.id}</td>
                  {isSeller && <td>{o.buyerName || o.buyerUsername || `#${o.buyerId}`}</td>}
                  <td>#{o.carId}</td>
                  <td>{money(o.totalAmount, o.currency)}</td>
                  <td>{titleCase(o.paymentType)}</td>
                  <td><StatusBadge>{o.status}</StatusBadge></td>
                  <td>{dateTime(o.createdAt)}</td>
                  <td>
                    {!isSeller && (o.status === 'CREATED' || o.status === 'AWAITING_PAYMENT') &&
                      <button className="text-btn danger-text" onClick={() => cancel(o.id)}>Cancel</button>}
                  </td>
                  {!isSeller && <td>
                    {(o.status === 'PAID' || o.status === 'FINANCING_ACTIVE') && <ReviewForm carId={o.carId}/>}
                  </td>}
                </tr>)}
            </tbody>
          </table>
        </div>
      : <EmptyState message={isSeller ? 'No orders returned for your sellerId.' : 'No orders found.'}/>}

    <div className="section-title">
      <Link className="btn btn-light" to="/cars">Continue shopping</Link>
    </div>
  </Page>
}