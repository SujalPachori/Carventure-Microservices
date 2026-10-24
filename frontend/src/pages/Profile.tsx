import { useQuery } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { Heart, CalendarClock, Receipt, ChevronRight , ShoppingBag, Car} from 'lucide-react';
import { userApi } from '../api/user';
import { useAuth } from '../context/AuthContext';
import type { UserProfile } from '../types';
import { Page, PageHeader } from '../components/common/Page';
import { ErrorState, Loading, Success } from '../components/common/UI';
import { apiMessage } from '../api/client';

export function Profile(){
  const { auth } = useAuth();
  const roles = auth?.roles instanceof Set ? Array.from(auth.roles) : auth?.roles || [];
  const kind = roles.includes('SELLER') ? 'seller' : 'customer';
  const q = useQuery({
    queryKey: ['profile', auth?.username, kind],
    queryFn: () => kind === 'seller' ? userApi.getSeller(auth!.username) : userApi.getCustomer(auth!.username),
    enabled: !!auth?.username
  });
  const [form, setForm] = useState<UserProfile | null>(null);
  const [editing, setEditing] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  useEffect(() => { if (q.data) setForm(q.data) }, [q.data]);

  if (q.isLoading) return <Page><Loading/></Page>;
  if (q.isError || !form) return <Page><ErrorState message="Profile data could not be loaded. The user-management service may not have a profile for this account yet."/></Page>;

  const set = (k: keyof UserProfile) => (e: React.ChangeEvent<HTMLInputElement>) => setForm({ ...form, [k]: e.target.value });

  async function submit(e: FormEvent){
    e.preventDefault();
    setError('');
    setMessage('');
    try {
      const updated = kind === 'seller' ? await userApi.updateSeller(form.username, form) : await userApi.updateCustomer(form.username, form);
      setForm(updated);
      setMessage('Profile updated.');
      setEditing(false);
    } catch (err) {
      setError(apiMessage(err));
    }
  }

  function cancelEdit(){
    if (q.data) setForm(q.data);
    setError('');
    setEditing(false);
  }

  return <Page>
    <PageHeader eyebrow="Account" title="Your profile" description="This profile is stored by the User Management service."/>
    <div className="profile-card">
      {!editing ? (
        <>
          <dl className="profile-view">
            <div><dt>Username</dt><dd>{form.username}</dd></div>
            <div><dt>Email</dt><dd>{form.email}</dd></div>
            <div><dt>First name</dt><dd>{form.firstName}</dd></div>
            <div><dt>Last name</dt><dd>{form.lastName}</dd></div>
            <div><dt>Phone number</dt><dd>{form.phoneNumber || '—'}</dd></div>
            <div><dt>Pincode</dt><dd>{form.pincode}</dd></div>
            <div className="full"><dt>Address</dt><dd>{form.address || '—'}</dd></div>
            <div><dt>Date of birth</dt><dd>{form.dateOfBirth || '—'}</dd></div>
            <div className="profile-view-actions">
              <button className="btn btn-dark" onClick={() => setEditing(true)}>Edit details</button>
            </div>
          </dl>
          {message && <Success>{message}</Success>}
        </>
      ) : (
        <form className="form grid-2" onSubmit={submit}>
          <label>Username<input value={form.username} disabled/></label>
          <label>Email<input required value={form.email} onChange={set('email')}/></label>
          <label>First name<input required value={form.firstName} onChange={set('firstName')}/></label>
          <label>Last name<input required value={form.lastName} onChange={set('lastName')}/></label>
          <label>Phone number<input required value={form.phoneNumber || ''} onChange={set('phoneNumber')}/></label>
          <label>Pincode<input required value={form.pincode} onChange={set('pincode')}/></label>
          <label className="full">Address<input value={form.address || ''} onChange={set('address')}/></label>
          <label>Date of birth<input type="date" value={form.dateOfBirth || ''} onChange={set('dateOfBirth')}/></label>
          <div className="form-actions">
            <button className="btn btn-dark">Save changes</button>
            <button type="button" className="btn btn-light" onClick={cancelEdit}>Cancel</button>
          </div>
          {error && <div className="notice error">{error}</div>}
        </form>
      )}
    </div>


    <div className="quick-grid"style={{ marginTop: '32px' }}>
      <Link className="quick-card" to="/orders">
        <Car/>
        <div>
          <strong>My Garage</strong>
          <span>Cars you've purchased</span>
        </div>
        <ChevronRight/>
      </Link>
      <Link className="quick-card" to="/offers">
        <Receipt/>
        <div>
          <strong>Past offers</strong>
          <span>Offers you've made or received</span>
        </div>
        <ChevronRight/>
      </Link>
      <Link className="quick-card" to="/buyer/test-drives">
        <CalendarClock/>
        <div>
          <strong>My test drives</strong>
          <span>Upcoming and past bookings</span>
        </div>
        <ChevronRight/>
      </Link>
      <Link className="quick-card" to="/wishlist">
        <Heart/>
        <div>
          <strong>My wishlist</strong>
          <span>Cars you've saved</span>
        </div>
        <ChevronRight/>
      </Link>
    </div>
  </Page>
}