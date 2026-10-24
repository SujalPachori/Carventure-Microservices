import { useState } from 'react';
import type { ChangeEvent, FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { apiMessage } from '../../api/client';
import type { Role, UserProfile } from '../../types';
import { Page } from '../../components/common/Page';

const empty: UserProfile = {
  username: '',
  firstName: '',
  lastName: '',
  email: '',
  phoneNumber: '',
  address: '',
  pincode: '',
  dateOfBirth: '',
};

export function Register() {
  const { register } = useAuth();
  const nav = useNavigate();

  const [role, setRole] = useState<Role>('CUSTOMER');
  const [profile, setProfile] = useState<UserProfile>(empty);
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const set =
    (key: keyof UserProfile) => (e: ChangeEvent<HTMLInputElement>) =>
      setProfile((p) => ({ ...p, [key]: e.target.value }));

  async function submit(e: FormEvent) {
    e.preventDefault();
    setError('');

    if (password !== confirmPassword) {
      setError('Passwords do not match');
      return;
    }

    setBusy(true);

    try {
      await register({
        ...profile,
        // Both are optional in the form. An empty string is not a valid
        // LocalDate or a meaningful address — Jackson throws on "" for a
        // LocalDate field, so send null instead of blank.
        dateOfBirth: profile.dateOfBirth || null,
        address: profile.address || null,
        password,
        role,
      });

      nav('/dashboard');
    } catch (err) {
      setError(apiMessage(err, 'Registration failed.'));
    } finally {
      setBusy(false);
    }
  }

  return (
    <Page className="auth-page">
      <div className="auth-card wide">
        <div className="eyebrow">Create account</div>
        <h1>Join Carventure</h1>
        <p className="lead">
          Self-registration creates either a CUSTOMER or SELLER account. Seller
          profiles start PENDING in the backend.
        </p>

        {error && <div className="notice error">{error}</div>}

        <form onSubmit={submit} className="form grid-2">
          <label>
            Account type
            <select
              value={role}
              onChange={(e) => setRole(e.target.value as Role)}
            >
              <option value="CUSTOMER">Buyer / Customer</option>
              <option value="SELLER">Seller</option>
            </select>
          </label>
          <span aria-hidden="true" />

          <label>
            Username
            <input
              required
              minLength={4}
              maxLength={100}
              value={profile.username}
              onChange={set('username')}
            />
          </label>

          <label>
            Email
            <input
              required
              type="email"
              value={profile.email}
              onChange={set('email')}
            />
          </label>

          <label>
            Password
            <input
              required
              minLength={8}
              type="password"
              autoComplete="new-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
          </label>

          <label>
            Confirm password
            <input
              required
              minLength={8}
              type="password"
              autoComplete="new-password"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
            />
          </label>

          <label>
            First name
            <input required value={profile.firstName} onChange={set('firstName')} />
          </label>

          <label>
            Last name
            <input required value={profile.lastName} onChange={set('lastName')} />
          </label>

          <label>
            Phone number
            <input
              required
              inputMode="numeric"
              value={profile.phoneNumber}
              onChange={set('phoneNumber')}
            />
          </label>

          <label>
            Pincode
            <input
              required
              inputMode="numeric"
              value={profile.pincode}
              onChange={set('pincode')}
            />
          </label>

          <label className="full">
            Address
            <input value={profile.address} onChange={set('address')} />
          </label>

          <label>
            Date of birth
            <input
              type="date"
              value={profile.dateOfBirth}
              onChange={set('dateOfBirth')}
            />
          </label>

          <button className="btn btn-dark btn-block" disabled={busy}>
            {busy ? 'Creating…' : 'Create account'}
          </button>
        </form>

        <p className="form-foot">
          Already registered? <Link to="/login">Sign in</Link>
        </p>
      </div>
    </Page>
  );
}