import { Navigate, Route, Routes } from 'react-router-dom'
import { AppLayout } from './components/layout/AppLayout'
import { RequireAuth } from './routes/guards'
import { Offers } from './pages/Offers'
import { Home } from './pages/public/Home'
import { Cars } from './pages/public/Cars'
import { CarDetails } from './pages/public/CarDetails'
import { Login } from './pages/auth/Login'
import { Register } from './pages/auth/Register'
import { Dashboard } from './pages/Dashboard'
import { Orders } from './pages/Orders'
import { Profile } from './pages/Profile'
import { Notifications } from './pages/Notifications'
import { Forbidden } from './pages/Forbidden'
import { NotFound } from './pages/NotFound'

import { Listings } from './pages/seller/Listings'
import { CarForm } from './pages/seller/CarForm'
import { TestDriveManagement } from './pages/seller/TestDriveManagement'
import { Availability } from './pages/seller/Availability'

import { TestDrives as BuyerTestDrives } from './pages/buyer/TestDrives'
import { BookTestDrive } from './pages/buyer/BookTestDrive'
import { Purchase } from './pages/buyer/Purchase'
import { Wishlist } from './pages/buyer/Wishlist'

import { AdminDashboard } from './pages/admin/AdminDashboard'
import { Users } from './pages/admin/Users'
import { Reports } from './pages/admin/Reports'
import { VerifyListing } from './pages/admin/VerifyListing'
import { ResolveDispute } from './pages/admin/ResolveDispute'
import { TestDrives as AdminTestDrives } from './pages/admin/TestDrives'

export default function App(){
  return <Routes>
    <Route element={<AppLayout/>}>
      <Route path="/" element={<Home/>}/>
      <Route path="/login" element={<Login/>}/>
      <Route path="/register" element={<Register/>}/>
      <Route path="/forbidden" element={<Forbidden/>}/>

      <Route path="/cars" element={<RequireAuth><Cars/></RequireAuth>}/>
      <Route path="/cars/:id" element={<RequireAuth><CarDetails/></RequireAuth>}/>
      <Route path="/dashboard" element={<RequireAuth><Dashboard/></RequireAuth>}/>
      <Route path="/profile" element={<RequireAuth><Profile/></RequireAuth>}/>
      <Route path="/orders" element={<RequireAuth><Orders/></RequireAuth>}/>
      <Route path="/notifications" element={<RequireAuth><Notifications/></RequireAuth>}/>
      <Route path="/offers" element={<RequireAuth><Offers/></RequireAuth>}/>

      <Route path="/buyer/test-drives" element={<RequireAuth roles={['CUSTOMER']}><BuyerTestDrives/></RequireAuth>}/>
      <Route path="/buyer/test-drives/:carId" element={<RequireAuth roles={['CUSTOMER']}><BookTestDrive/></RequireAuth>}/>
      <Route path="/buyer/purchase/:carId" element={<RequireAuth roles={['CUSTOMER']}><Purchase/></RequireAuth>}/>
      <Route path="/buyer/wishlist" element={<RequireAuth roles={['CUSTOMER']}><Wishlist/></RequireAuth>}/>

      <Route path="/seller" element={<RequireAuth roles={['SELLER']}><Navigate to="/seller/listings" replace/></RequireAuth>}/>
      <Route path="/seller/listings" element={<RequireAuth roles={['SELLER']}><Listings/></RequireAuth>}/>
      <Route path="/seller/cars/new" element={<RequireAuth roles={['SELLER']}><CarForm/></RequireAuth>}/>
      <Route path="/seller/cars/:id/edit" element={<RequireAuth roles={['SELLER']}><CarForm/></RequireAuth>}/>
      <Route path="/seller/test-drives" element={<RequireAuth roles={['SELLER']}><TestDriveManagement/></RequireAuth>}/>
      <Route path="/seller/availability" element={<RequireAuth roles={['SELLER']}><Availability/></RequireAuth>}/>

      <Route path="/admin" element={<RequireAuth roles={['ADMIN']}><AdminDashboard/></RequireAuth>}/>
      <Route path="/admin/users" element={<RequireAuth roles={['ADMIN']}><Users/></RequireAuth>}/>
      <Route path="/admin/reports" element={<RequireAuth roles={['ADMIN']}><Reports/></RequireAuth>}/>
      <Route path="/admin/verify" element={<RequireAuth roles={['ADMIN']}><VerifyListing/></RequireAuth>}/>
      <Route path="/admin/disputes" element={<RequireAuth roles={['ADMIN']}><ResolveDispute/></RequireAuth>}/>
      <Route path="/admin/test-drives" element={<RequireAuth roles={['ADMIN']}><AdminTestDrives/></RequireAuth>}/>

      <Route path="*" element={<NotFound/>}/>
    </Route>
  </Routes>
}