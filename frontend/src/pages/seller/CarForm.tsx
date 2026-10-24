import { useEffect, useState } from 'react';
import type { ChangeEvent, FormEvent } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { ImagePlus, FileText, CalendarClock, Sparkles } from 'lucide-react';
import { carApi } from '../../api/car';
import { schedulingApi } from '../../api/scheduling';
import { useAuth } from '../../context/AuthContext';
import { Page, PageHeader } from '../../components/common/Page';
import { apiMessage } from '../../api/client';
import { EmptyState, ErrorState, Loading, Success } from '../../components/common/UI';
import type { Car, FuelType } from '../../types';

const DAYS = ['MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY'];

const STATUS_BADGE: Record<string, string> = {
  ACTIVE: 'badge-approved',
  DRAFT: 'badge-pending',
  RESERVED: 'badge-pending',
  SOLD: 'badge-rejected',
};

const blank: Partial<Car> = {
  brand: '', model: '', manufactureYear: new Date().getFullYear(), mileage: 0,
  fuelType: 'PETROL', transmission: '', colour: '', price: 0, suggestedPrice: 0,
  condition: 'Used', location: '', description: '', status: 'ACTIVE',
  kmdriven: '', pincode: '', contact: '',
};

const NUMERIC_FIELDS: (keyof Car)[] = ['manufactureYear', 'mileage', 'price', 'suggestedPrice'];

function toPayload(form: Partial<Car>, sellerUsername?: string): Partial<Car> {
  const payload: Record<string, unknown> = { ...form, sellerUsername };
  for (const field of NUMERIC_FIELDS) {
    const value = payload[field];
    payload[field] = value === '' || value === null || value === undefined ? null : Number(value);
  }
  delete payload.sellerId;
  delete payload.postedDate;
  delete payload.imageUrls;
  return payload as Partial<Car>;
}

export function CarForm() {
  const { id } = useParams();
  const edit = !!id;
  const { auth } = useAuth();
  const nav = useNavigate();
  const qc = useQueryClient();

  const q = useQuery({ queryKey: ['car', Number(id)], queryFn: () => carApi.get(Number(id)), enabled: edit });

  const [form, setForm] = useState<Partial<Car>>(blank);
  const [error, setError] = useState('');
  const [saved, setSaved] = useState('');
  const [busy, setBusy] = useState(false);

  useEffect(() => { if (q.data) setForm(q.data); }, [q.data]);

  if (q.isLoading) return <Page><Loading/></Page>;
  if (q.isError) return <Page><ErrorState message="Could not load this car."/></Page>;

  const set = (k: keyof Car) =>
    (e: ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) =>
      setForm(f => ({ ...f, [k]: e.target.value }));

  async function submit(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError('');
    setSaved('');
    try {
      if (edit) {
        await carApi.update(Number(id), toPayload(form, form.sellerUsername || auth?.username));
        await qc.invalidateQueries({ queryKey: ['seller-cars'] });
        await qc.invalidateQueries({ queryKey: ['car', Number(id)] });
        setSaved('Listing saved.');
      } else {
        const created = await carApi.create(toPayload(form, auth?.username));
        await qc.invalidateQueries({ queryKey: ['seller-cars'] });
        nav(`/seller/cars/${created.carId}/edit`, { replace: true });
      }
    } catch (err) {
      setError(apiMessage(err));
    } finally {
      setBusy(false);
    }
  }

  async function suggest() {
    const numberOrZero = (value) =>
      value === undefined || value === null || value === ""
        ? 0
        : Number(value);

    try {
      const response = await fetch("http://localhost:8000/predict", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          manufactureYear: numberOrZero(form.manufactureYear),
          kmsDriven: numberOrZero(form.kmdriven),
          engineCapacity: numberOrZero(form.engineCapacity),
          maxPower: numberOrZero(form.maxPower),
          mileage: numberOrZero(form.mileage),
          seats: numberOrZero(form.seats) || 5,

          brand: form.brand || "NA",
          model: form.model || "NA",
          fuelType: form.fuelType || "NA",
          transmission: form.transmission || "NA",
          ownerType: form.ownerType || "NA",
          bodyType: form.bodyType || "NA",
          city: form.city || "NA",
        }),
      });

      const data = await response.json();

      if (!response.ok) {
        throw new Error(
          Array.isArray(data.detail)
            ? data.detail.map((error) => error.msg).join(", ")
            : data.detail || "Prediction failed"
        );
      }

      setForm((formValues) => ({
        ...formValues,
        suggestedPrice: data.suggestedPrice,
      }));
    } catch (err) {
      setError(err.message);
    }
  }

  async function remove() {
    if (!id || !window.confirm('Delete this listing?')) return;
    try {
      await carApi.remove(Number(id));
      await qc.invalidateQueries({ queryKey: ['seller-cars'] });
      nav('/seller/listings');
    } catch (err) { setError(apiMessage(err)); }
  }

  return (
    <Page>
      <div className="page-header">
        <div>
          <PageHeader
            eyebrow="Seller"
            title={edit ? 'Edit listing' : 'Add a car'}
            description={edit
              ? 'Update the details, then add photos, documents and test-drive slots below.'
              : 'Save the listing first — photos, documents and test-drive slots unlock on the next step.'}
          />
        </div>
        {edit && form.status && (
          <span className={`badge ${STATUS_BADGE[form.status] || ''}`}>{form.status}</span>
        )}
      </div>

      <form className="form grid-2" onSubmit={submit}>
        <label>Brand<input required value={form.brand||''} onChange={set('brand')}/></label>
        <label>Model<input required value={form.model||''} onChange={set('model')}/></label>
        <label>Manufacture year<input type="number" required value={String(form.manufactureYear||'')} onChange={set('manufactureYear')}/></label>
        <label>Mileage<input type="number" value={String(form.mileage??'')} onChange={set('mileage')}/></label>
        <label>Fuel type
          <select value={form.fuelType||''} onChange={set('fuelType')}>
            <option value="">Select</option>
            {(['PETROL','DIESEL','CNG'] as FuelType[]).map(x => <option key={x}>{x}</option>)}
          </select>
        </label>
        <label>Transmission<input value={form.transmission||''} onChange={set('transmission')}/></label>
        <label>Colour<input value={form.colour||''} onChange={set('colour')}/></label>
        <label>Condition<input value={form.condition||''} onChange={set('condition')}/></label>
        <label>Price<input type="number" min="0" step="0.01" value={String(form.price??'')} onChange={set('price')}/></label>
        <label>Suggested price
          <div className="input-with-action">
            <input type="number" min="0" step="0.01" value={String(form.suggestedPrice??'')} onChange={set('suggestedPrice')}/>
            {edit && <button type="button" className="input-action" onClick={suggest} title="Get a suggested price"><Sparkles size={15}/></button>}
          </div>
        </label>
        <label>Location<input value={form.location||''} onChange={set('location')}/></label>
        <label>Km driven<input value={form.kmdriven||''} onChange={set('kmdriven')}/></label>
        <label>Pincode<input value={form.pincode||''} onChange={set('pincode')}/></label>
        <label>Contact<input value={form.contact||''} onChange={set('contact')}/></label>
        <label>Status
          <select value={form.status||''} onChange={set('status')}>
            {['DRAFT','ACTIVE','RESERVED','SOLD'].map(s => <option key={s}>{s}</option>)}
          </select>
        </label>
        <label className="full">Description<textarea rows={5} value={form.description||''} onChange={set('description')}/></label>

        <div className="form-actions full">
          <button className="btn btn-dark" disabled={busy}>
            {busy ? 'Saving…' : edit ? 'Save changes' : 'Create listing'}
          </button>
          {edit && <button type="button" className="btn btn-danger" onClick={remove}>Delete</button>}
          <Link className="btn btn-light" to="/seller/listings">Cancel</Link>
        </div>
      </form>

      {saved && <Success>{saved}</Success>}
      {error && <div className="notice error">{error}</div>}

      {edit && q.data && <MediaManager car={q.data}/>}
      {edit && <AvailabilityManager carId={Number(id)}/>}
    </Page>
  );
}

function MediaManager({ car }: { car: Car }) {
  const [doc, setDoc] = useState<Record<string, File|undefined>>({});
  const [picture, setPicture] = useState<File>();
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  async function docs() {
    const fd = new FormData();
    if (doc.rc) fd.append('rc', doc.rc);
    if (doc.insurance) fd.append('insurance', doc.insurance);
    if (doc.puc) fd.append('puc', doc.puc);
    try {
      await carApi.uploadDocuments(car.carId, fd);
      setMessage('Documents uploaded.');
    } catch (e) { setError(apiMessage(e)); }
  }

  async function pic() {
    if (!picture) return;
    try {
      await carApi.uploadPicture(car.carId, picture);
      setMessage('Picture uploaded.');
      setPicture(undefined);
    } catch (e) { setError(apiMessage(e)); }
  }

  return (
    <div className="media-manager">
      <div className="section-title-icon"><ImagePlus size={18}/><h2>Listing media</h2></div>
      <div className="grid-2">
        <label className="file-label">
          RC
          <div className="file-drop"><FileText size={16}/><span>{doc.rc?.name || 'Choose file'}</span></div>
          <input type="file" onChange={e => setDoc(d => ({...d, rc: e.target.files?.[0]}))}/>
        </label>
        <label className="file-label">
          Insurance
          <div className="file-drop"><FileText size={16}/><span>{doc.insurance?.name || 'Choose file'}</span></div>
          <input type="file" onChange={e => setDoc(d => ({...d, insurance: e.target.files?.[0]}))}/>
        </label>
        <label className="file-label">
          PUC
          <div className="file-drop"><FileText size={16}/><span>{doc.puc?.name || 'Choose file'}</span></div>
          <input type="file" onChange={e => setDoc(d => ({...d, puc: e.target.files?.[0]}))}/>
        </label>
        <button type="button" className="btn btn-light" onClick={docs}>Upload documents</button>
        <label className="file-label">
          Car picture
          <div className="file-drop"><ImagePlus size={16}/><span>{picture?.name || 'Choose an image'}</span></div>
          <input type="file" accept="image/*" onChange={e => setPicture(e.target.files?.[0])}/>
        </label>
        <button type="button" className="btn btn-light" onClick={pic}>Upload picture</button>
      </div>
      {message && <Success>{message}</Success>}
      {error && <div className="notice error">{error}</div>}
    </div>
  );
}

function AvailabilityManager({ carId }: { carId: number }) {
  const qc = useQueryClient();
  const q = useQuery({ queryKey: ['availability', carId], queryFn: () => schedulingApi.availability(carId) });

  const [form, setForm] = useState({ dayOfWeek: 'MONDAY', startTime: '10:00', endTime: '18:00', durationMinutes: '45' });
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [busy, setBusy] = useState(false);

  async function submit(e: FormEvent) {
    e.preventDefault();
    setError('');
    setMessage('');
    if (form.startTime >= form.endTime) {
      setError('The end time has to be after the start time.');
      return;
    }
    setBusy(true);
    try {
      await schedulingApi.createAvailability({
        carId,
        dayOfWeek: form.dayOfWeek,
        startTime: form.startTime,
        endTime: form.endTime,
        durationMinutes: Number(form.durationMinutes),
      });
      setMessage('Availability added.');
      await qc.invalidateQueries({ queryKey: ['availability', carId] });
    } catch (err) {
      setError(apiMessage(err));
    } finally {
      setBusy(false);
    }
  }

  async function deactivate(id: number) {
    setError('');
    try {
      await schedulingApi.deactivateAvailability(id);
      await qc.invalidateQueries({ queryKey: ['availability', carId] });
    } catch (err) { setError(apiMessage(err)); }
  }

  const sorted = q.data ? [...q.data].sort((a, b) => DAYS.indexOf(a.dayOfWeek) - DAYS.indexOf(b.dayOfWeek)) : [];
  const activeCount = sorted.filter(a => a.active).length;

  return (
    <div className="media-manager">
      <div className="section-title-icon">
        <CalendarClock size={18}/>
        <h2>Test-drive availability</h2>
        {activeCount > 0 && <span className="badge badge-approved">{activeCount} active</span>}
      </div>
      <p>Weekly windows when buyers can book a test drive on this car.</p>

      <form className="grid-2 availability-form" onSubmit={submit}>
        <label>Day
          <select value={form.dayOfWeek} onChange={e => setForm(f => ({...f, dayOfWeek: e.target.value}))}>
            {DAYS.map(day => <option key={day}>{day}</option>)}
          </select>
        </label>
        <label>Slot length (minutes)
          <input type="number" min="15" step="15" value={form.durationMinutes}
                 onChange={e => setForm(f => ({...f, durationMinutes: e.target.value}))}/>
        </label>
        <label>Start time
          <input type="time" value={form.startTime} onChange={e => setForm(f => ({...f, startTime: e.target.value}))}/>
        </label>
        <label>End time
          <input type="time" value={form.endTime} onChange={e => setForm(f => ({...f, endTime: e.target.value}))}/>
        </label>
        <div className="form-actions full">
          <button className="btn btn-dark" type="submit" disabled={busy}>
            {busy ? 'Adding…' : 'Add availability'}
          </button>
        </div>
      </form>

      {message && <Success>{message}</Success>}
      {error && <div className="notice error">{error}</div>}

      {q.isLoading ? <Loading label="Loading availability…"/> : sorted.length ? (
        <div className="table-wrap">
          <table>
            <thead>
              <tr><th>Day</th><th>Time</th><th>Slot</th><th>Status</th><th/></tr>
            </thead>
            <tbody>
              {sorted.map(a => (
                <tr key={a.id}>
                  <td>{a.dayOfWeek.charAt(0) + a.dayOfWeek.slice(1).toLowerCase()}</td>
                  <td>{a.startTime} – {a.endTime}</td>
                  <td>{a.durationMinutes ? `${a.durationMinutes} min` : '—'}</td>
                  <td><span className={`badge ${a.active ? 'badge-approved' : 'badge-rejected'}`}>{a.active ? 'Active' : 'Inactive'}</span></td>
                  <td>
                    {a.active && (
                      <button type="button" className="text-btn danger-text" onClick={() => deactivate(a.id)}>
                        Deactivate
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : <EmptyState message="No availability configured yet."/>}
    </div>
  );
}