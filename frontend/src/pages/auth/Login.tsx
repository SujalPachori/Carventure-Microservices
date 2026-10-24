import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import { apiMessage } from '../../api/client'
import { Page } from '../../components/common/Page'

export function Login(){

  const { login } = useAuth()
  const nav = useNavigate()
  const loc = useLocation()

  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function submit(e: FormEvent){
    e.preventDefault()
    setBusy(true)
    setError('')
    try {
      const a = await login(username, password)
      const roles = a.roles instanceof Set ? Array.from(a.roles) : a.roles

      const from = (loc.state as { from?: string } | null)?.from
      const home = roles.includes('ADMIN') ? '/admin'
        : roles.includes('SELLER') ? '/seller'
        : '/dashboard'

      // `from` was captured under whoever was signed in before. Signing in as a
      // different role must not send them back to a path that role can't reach,
      // or the guard bounces them straight to /forbidden.
      const wrongRole = !!from && (
        (from.startsWith('/buyer') && !roles.includes('CUSTOMER')) ||
        (from.startsWith('/seller') && !roles.includes('SELLER')) ||
        (from.startsWith('/admin') && !roles.includes('ADMIN'))
      )

      nav(!from || wrongRole ? home : from)

    } catch (err) {
      setError(apiMessage(err, 'Invalid username or password'))
    } finally {
      setBusy(false)
    }
  }

  return <Page className="auth-page">
    <div className="auth-card">
      <div className="eyebrow">Welcome back</div>
      <h1>Sign in to Carventure</h1>
      <p className="lead">Use the credentials from your Carventure account.</p>
      {error && <div className="notice error">{error}</div>}
      <form onSubmit={submit} className="form">
        <label>Username
          <input required minLength={4} value={username}
            onChange={e => setUsername(e.target.value)} autoComplete="username"/>
        </label>
        <label>Password
          <input required type="password" value={password}
            onChange={e => setPassword(e.target.value)} autoComplete="current-password"/>
        </label>
        <button className="btn btn-dark btn-block" disabled={busy}>
          {busy ? 'Signing in…' : 'Sign in'}
        </button>
      </form>
      <p className="form-foot">New here? <Link to="/register">Create an account</Link></p>
    </div>
  </Page>
}