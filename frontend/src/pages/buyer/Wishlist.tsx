import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { HeartOff, Trash2 } from 'lucide-react'
import { Page, PageHeader } from '../../components/common/Page'
import { ErrorState, Loading } from '../../components/common/UI'
import { CarImage } from '../../components/car/CarImage'
import { wishlistApi } from '../../api/engagement'
import { money } from '../../utils/format'
import { useAuth } from '../../context/AuthContext'

export function Wishlist(){

  const {auth}=useAuth(); const me=auth!.username; const qc=useQueryClient()
  const q=useQuery({queryKey:['wishlist',me],queryFn:()=>wishlistApi.list(me)})
  const remove=useMutation({mutationFn:(carId:string)=>wishlistApi.remove(me,carId),
    onSuccess:()=>qc.invalidateQueries({queryKey:['wishlist',me]})})

  if(q.isLoading) return <Page><Loading/></Page>
  if(q.isError) return <Page><ErrorState message="Could not load your wishlist."/></Page>

  return <Page>
    <PageHeader eyebrow="Saved" title="Wishlist" description="Cars you've saved to come back to."/>
    {!q.data?.length
      ? <div className="sw-empty">
          <HeartOff size={34}/>
          <strong>Nothing saved yet.</strong>
          <span>Tap Save on any listing to keep it here.</span>
        </div>
      : <div>{q.data.map(w=>
          <div key={w.wishlistItemId} className="sw-card">
            <div className="sw-row">
              {w.imageUrl
                ? <CarImage className="sw-thumb" src={w.imageUrl} alt={`${w.brand} ${w.model}`}/>
                : <div className="sw-thumb-empty">NO IMAGE</div>}
              <div>
                <div className="sw-title">{w.brand} {w.model}</div>
                <div className="sw-sub">
                  {money(w.price)}{w.manufactureYear?` · ${w.manufactureYear}`:''} · <Link to={`/cars/${w.carId}`}>view listing</Link>
                </div>
              </div>
              {!w.available && <span className="sw-pill sw-pill-closed sw-spacer">{w.carStatus}</span>}
            </div>
            <div className="sw-divider">
              <button className="sw-btn sw-btn-danger" onClick={()=>remove.mutate(w.carId)}>
                <Trash2 size={15}/> Remove
              </button>
            </div>
          </div>)}
        </div>}
  </Page>
}