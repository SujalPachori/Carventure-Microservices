import { Link, useNavigate } from 'react-router-dom'
import { CarFront, LogOut, Menu, UserCircle } from 'lucide-react'
import { useState } from 'react'
import { useAuth } from '../../context/AuthContext'
import { NotificationBell } from './NotificationBell'

export function Header(){
  const {auth,logout}=useAuth()
  const nav=useNavigate()
  const [open,setOpen]=useState(false)
  const roles=auth?.roles instanceof Set?Array.from(auth.roles):auth?.roles||[]

  return <header className="topbar">
    <div className="container topbar-inner">
      <Link className="brand" to={auth?'/dashboard':'/'}><CarFront/>CARVENTURE</Link>
      <button className="icon-btn mobile-only" onClick={()=>setOpen(!open)}><Menu/></button>
      <nav className={open?'nav open':'nav'}>
        {auth?<>
          <Link to="/cars">Cars</Link>
          {roles.includes('CUSTOMER')&&<>
            <Link to="/buyer/test-drives">Test Drives</Link>
            <Link to="/offers">My Offers</Link>
            <Link to="/buyer/wishlist">Wishlist</Link>
            <Link to="/orders">Orders</Link>
          </>}
          {roles.includes('SELLER')&&<>
            <Link to="/seller/listings">Listings</Link>
            <Link to="/seller/test-drives">Bookings</Link>
            <Link to="/offers">Offers</Link>
            <Link to="/orders">Orders</Link>
          </>}
          {roles.includes('ADMIN')&&<Link to="/admin">Admin</Link>}
          <Link to="/profile"><UserCircle size={18}/> {auth.username}</Link>
          <NotificationBell/>
          <button className="ghost-btn" onClick={()=>{logout();nav('/login')}}><LogOut size={17}/> Logout</button>
        </>:<>
          <Link to="/login">Login</Link>
          <Link className="btn btn-dark" to="/register">Get started</Link>
        </>}
      </nav>
    </div>
  </header>
}