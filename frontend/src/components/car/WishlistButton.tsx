import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Heart } from 'lucide-react'
import { wishlistApi } from '../../api/engagement'
import { useAuth } from '../../context/AuthContext'

export function WishlistButton({carId}:{carId:string}){
  const {auth}=useAuth(); const me=auth!.username; const qc=useQueryClient()
  const q=useQuery({queryKey:['wishlist',me,carId],queryFn:()=>wishlistApi.contains(me,carId)})
    const toggle=useMutation({
      mutationFn:async()=>{
        if(q.data) await wishlistApi.remove(me,carId)
        else await wishlistApi.add(me,carId)
      },
      onSuccess:()=>{qc.invalidateQueries({queryKey:['wishlist',me]})}})
  return <button className="btn btn-light" disabled={toggle.isPending} onClick={()=>toggle.mutate()}>
    <Heart size={16} fill={q.data?'currentColor':'none'}/> {q.data?'Saved':'Save'}
  </button>
}