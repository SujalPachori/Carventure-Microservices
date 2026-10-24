import { useQuery } from '@tanstack/react-query'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { CalendarDays, ChevronLeft, Gauge, MapPin, ShieldCheck, Tag, Route, Hash, Phone } from 'lucide-react'
import { carApi } from '../../api/car'
import { Page } from '../../components/common/Page'
import { ErrorState, Loading } from '../../components/common/UI'
import { money } from '../../utils/format'
import { CarImage } from '../../components/car/CarImage'
import { WishlistButton } from '../../components/car/WishlistButton'
import { MakeOfferForm } from '../../components/car/MakeOfferForm'
import { SellerReviews } from '../../components/car/SellerReviews'
import { useAuth } from '../../context/AuthContext'

export function CarDetails(){
  const id=Number(useParams().id)
  const {auth}=useAuth()
  const nav=useNavigate()
  const q=useQuery({queryKey:['car',id],queryFn:()=>carApi.get(id),enabled:Number.isFinite(id)})

  if(q.isLoading) return <Page><Loading/></Page>
  if(q.isError||!q.data) return <Page><ErrorState message="Car not found or unavailable."/></Page>

  const c=q.data
  const image=c.imageUrls?.[0]
  const roles=auth?.roles instanceof Set?Array.from(auth.roles):auth?.roles||[]
  const isCustomer=roles.includes('CUSTOMER')
  const isOwner=roles.includes('SELLER')&&c.sellerUsername===auth?.username
  const available=c.status!=='SOLD'&&c.status!=='RESERVED'

  return <Page>
    <Link className="back" to="/cars"><ChevronLeft size={17}/> Back to cars</Link>
    <div className="detail-grid">
      <div>
        <div className="detail-image">
          {image?<CarImage src={image} alt={`${c.brand} ${c.model}`}/>:<div className="car-placeholder">NO IMAGE</div>}
        </div>
        {c.imageUrls&&c.imageUrls.length>1&&<div className="thumb-row">
          {c.imageUrls.map((x,i)=><CarImage key={i} src={x} alt={`${c.brand} ${c.model} thumbnail ${i+1}`}/>)}
        </div>}
      </div>
      <div className="detail-copy">
        <div className="eyebrow">{c.manufactureYear} · {c.condition||'Used'} · {c.status||'Listed'}</div>
        <h1>{c.brand} {c.model}</h1>
        <div className="detail-price">{money(c.price)}</div>
        <p>{c.description||'No description was provided for this listing.'}</p>
        <div className="detail-spec-grid">
          <span><Gauge/> {c.mileage?.toLocaleString()||'—'} km</span>
          <span><Tag/> {c.fuelType||'—'}</span>
          <span><ShieldCheck/> {c.transmission||'—'}</span>
          <span><MapPin/> {c.location||'—'}</span>
          {c.kmdriven&&<span><Route/> {c.kmdriven} km driven</span>}
          {c.pincode&&<span><Hash/> {c.pincode}</span>}
        </div>
        <div className="detail-actions">
          {isCustomer&&available&&<>
            <Link className="btn btn-dark" to={`/buyer/test-drives/${c.carId}`}><CalendarDays/> Book test drive</Link>
            <button className="btn btn-light" onClick={()=>nav(`/buyer/purchase/${c.carId}`)}>Buy this car</button>
            <WishlistButton carId={String(c.carId)}/>
          </>}
          {isCustomer&&!available&&<>
            <span className="sw-pill sw-pill-closed">{c.status}</span>
            <WishlistButton carId={String(c.carId)}/>
          </>}
          {isOwner&&<Link className="btn btn-light" to={`/seller/cars/${c.carId}/edit`}>Edit listing</Link>}
        </div>
        {c.contact&&(isCustomer||isOwner)&&<p className="detail-contact"><Phone size={15}/> {c.contact}</p>}
        {isCustomer&&available&&<MakeOfferForm carId={String(c.carId)}/>}
        <SellerReviews sellerId={c.sellerUsername}/>
      </div>
    </div>
  </Page>
}