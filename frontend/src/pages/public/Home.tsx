import { ArrowRight, ShieldCheck, CalendarCheck, BadgeDollarSign, Gauge } from 'lucide-react';
import { Link } from 'react-router-dom';
import { Page } from '../../components/common/Page'

export function Home(){
  return <>
    <section className="hero">
      <div className="container hero-grid">
        <div>
          <div className="eyebrow">Buy. Sell. Drive.</div>
          <h1>Find it. Drive it. Own it.</h1>
          <p>Real listings from real sellers. Book a test drive, make an offer, or list your own car — all in one place.</p>
          <div className="hero-actions">
            <Link className="btn btn-dark" to="/cars">Explore cars <ArrowRight size={17}/></Link>
            <Link className="btn btn-light" to="/register">Create account</Link>
          </div>
        </div>
        <div className="hero-art">
          <img className="hero-art-bg" src="/images/carImage.avif" alt="Featured car listing" />
          <div className="hero-art-inner">
            <span>CARVENTURE</span>
            <strong>DRIVE<br/>WHAT'S<br/>NEXT.</strong>
          </div>
        </div>
      </div>
    </section>
    <Page>
      <div className="feature-grid">
        <div className="feature">
          <div className="gauge"><span className="gauge-needle"></span><ShieldCheck/></div>
          <h3>Verified marketplace</h3>
          <p>Every account is authenticated, every listing tied to a real seller.</p>
        </div>
        <div className="feature">
          <div className="gauge"><span className="gauge-needle"></span><CalendarCheck/></div>
          <h3>Book a test drive</h3>
          <p>See real availability and lock in a time — no back-and-forth.</p>
        </div>
        <div className="feature">
          <div className="gauge"><span className="gauge-needle"></span><BadgeDollarSign/></div>
          <h3>Buy or sell with confidence</h3>
          <p>Secure payments on purchase, smart pricing guidance when you list.</p>
        </div>
        <div className="feature">
          <div className="gauge"><span className="gauge-needle"></span><Gauge/></div>
          <h3>List in minutes</h3>
          <p>Upload your car, get a suggested price, and go live fast.</p>
        </div>
      </div>
    </Page>
  </>
}