import { Outlet } from 'react-router-dom'
import { Link } from 'react-router-dom'
import { Facebook, Instagram, Twitter } from 'lucide-react'
import { Header } from './Header'

export function AppLayout(){
  return <>
    <Header/>
    <main><Outlet/></main>
    <footer>
      <div className="container footer-main">
        <div className="footer-brand">
          <div className="brand">CARVENTURE</div>
          <p>Buy Your Dream Car.</p>
          <div className="footer-social">
            <a href="https://facebook.com" target="_blank" rel="noopener noreferrer" aria-label="Facebook"><Facebook size={18}/></a>
            <a href="https://instagram.com" target="_blank" rel="noopener noreferrer" aria-label="Instagram"><Instagram size={18}/></a>
            <a href="https://twitter.com" target="_blank" rel="noopener noreferrer" aria-label="Twitter"><Twitter size={18}/></a>
          </div>
        </div>
        <nav className="footer-sitemap">
          <div>
            <h4>Marketplace</h4>
            <Link to="/cars">Browse cars</Link>
            <Link to="/seller/listings">Sell your car</Link>
            <Link to="/buyer/test-drives">Test drives</Link>
          </div>
          <div>
            <h4>Account</h4>
            <Link to="/register">Create account</Link>
            <Link to="/login">Log in</Link>
            <Link to="/profile">Your profile</Link>
          </div>
          <div>
            <h4>Company</h4>
            <Link to="/about">About</Link>
            <Link to="/contact">Contact</Link>
            <Link to="/help">Help center</Link>
          </div>
        </nav>
      </div>
      <div className="container footer-inner">
        <span>© {new Date().getFullYear()} Carventure</span>
        <span>Buy Your Dream Car.</span>
      </div>
    </footer>
  </>
}