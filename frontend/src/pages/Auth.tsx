import { useEffect, useState, type FormEvent } from 'react';
import { api } from '../api';
import { useAuth } from '../auth';
import { Link, navigate, useLocation } from '../router';
import type { AuthResponse, MessageResponse } from '../types';
import { errMsg, setMeta, toast } from '../util';

function nextUrl(q: URLSearchParams) {
  const n = q.get('next') || '/dashboard';
  return n.startsWith('/') && !n.startsWith('//') ? n : '/dashboard';
}

function devToast(r: MessageResponse) {
  toast(r.devCode ? `📧 ${r.message} (development mode code: ${r.devCode})` : '📧 ' + r.message);
}

export function Login() {
  const { query } = useLocation();
  const { login } = useAuth();
  const mode = query.get('mode') || 'code';
  const [email, setEmail] = useState(query.get('email') || '');
  const [password, setPassword] = useState('');
  const [code, setCode] = useState('');
  const [sent, setSent] = useState(query.get('sent') === '1');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const next = nextUrl(query);
  useEffect(() => setMeta('Log In'), []);

  const run = async (fn: () => Promise<void>) => {
    setBusy(true); setError('');
    try { await fn(); } catch (e) { setError(errMsg(e)); } finally { setBusy(false); }
  };
  const done = (r: AuthResponse) => { login(r); toast('Welcome, ' + r.user.name + '!'); navigate(next, true); };

  const sendCode = (e: FormEvent) => { e.preventDefault(); run(async () => { devToast(await api<MessageResponse>('/api/auth/otp/request', { body: { email } })); setSent(true); }); };
  const verify = (e: FormEvent) => { e.preventDefault(); run(async () => done(await api<AuthResponse>('/api/auth/otp/verify', { body: { email, code } }))); };
  const pwLogin = (e: FormEvent) => { e.preventDefault(); run(async () => done(await api<AuthResponse>('/api/auth/login', { body: { email, password } }))); };

  return (
    <div className="auth">
      {mode === 'password' ? (
        <form className="auth-box" onSubmit={pwLogin}>
          <h1>Log In</h1>
          <div className="field"><label>Email</label><input type="email" required value={email} onChange={e => setEmail(e.target.value)} placeholder="you@email.com" autoComplete="email" /></div>
          <div className="field"><label>Password</label><input type="password" required value={password} onChange={e => setPassword(e.target.value)} autoComplete="current-password" /></div>
          {error && <div className="error">{error}</div>}
          <button className="btn sq block" disabled={busy}>Log In</button>
          <p className="small-center"><Link to="/forgot-password">Forgot password?</Link></p>
          <div className="info-box"><span className="q">?</span><span>Prefer a password-free login? <Link to={'/login?next=' + encodeURIComponent(next)}>Log in with an email code</Link> instead.</span></div>
          <p className="small-center">Don't have an account? <Link to={'/signup?next=' + encodeURIComponent(next)}>Sign Up</Link></p>
        </form>
      ) : !sent ? (
        <form className="auth-box" onSubmit={sendCode}>
          <h1>Log In</h1>
          <div className="field"><label>Email</label><input type="email" required value={email} onChange={e => setEmail(e.target.value)} placeholder="you@email.com" autoComplete="email" /></div>
          <label className="check"><input type="checkbox" defaultChecked /> Remember me</label>
          {error && <div className="error">{error}</div>}
          <button className="btn sq block" disabled={busy}>Log In</button>
          <div className="info-box"><span className="q">?</span><span>We will email you a code for a password-free log in. Or you can <Link to={'/login?mode=password&next=' + encodeURIComponent(next)}>log in with a password</Link> instead.</span></div>
          <p className="small-center">Don't have an account? <Link to={'/signup?next=' + encodeURIComponent(next)}>Sign Up</Link></p>
        </form>
      ) : (
        <form className="auth-box" onSubmit={verify}>
          <h1>Check your email</h1>
          <p className="small-center">We sent a 6-digit code to <b>{email}</b></p>
          <div className="field"><label>Login code</label><input inputMode="numeric" maxLength={6} required value={code} onChange={e => setCode(e.target.value)} placeholder="123456" autoFocus autoComplete="one-time-code" /></div>
          {error && <div className="error">{error}</div>}
          <button className="btn sq block" disabled={busy}>Verify &amp; Log In</button>
          <p className="small-center">
            <a href="#" onClick={e => { e.preventDefault(); run(async () => devToast(await api<MessageResponse>('/api/auth/otp/request', { body: { email } }))); }}>Resend code</a>
            {' · '}
            <a href="#" onClick={e => { e.preventDefault(); setSent(false); setCode(''); }}>Use a different email</a>
          </p>
        </form>
      )}
    </div>
  );
}

export function Signup() {
  const { query } = useLocation();
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [country, setCountry] = useState('IN');
  const [marketing, setMarketing] = useState(false);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  useEffect(() => setMeta('Sign Up'), []);

  const submit = async (e: FormEvent) => {
    e.preventDefault(); setBusy(true); setError('');
    try {
      devToast(await api<MessageResponse>('/api/auth/register', { body: { name, email, password, marketing, country } }));
      navigate(`/login?sent=1&email=${encodeURIComponent(email)}&next=${encodeURIComponent(nextUrl(query))}`);
    } catch (err) { setError(errMsg(err)); } finally { setBusy(false); }
  };
  return (
    <div className="auth">
      <form className="auth-box" onSubmit={submit}>
        <h1>Sign Up</h1>
        <div className="field"><label>Full name</label><input required maxLength={120} value={name} onChange={e => setName(e.target.value)} placeholder="your name" autoComplete="name" /></div>
        <div className="field"><label>Email</label><input type="email" required value={email} onChange={e => setEmail(e.target.value)} placeholder="you@email.com" autoComplete="email" /></div>
        <div className="field"><label>Password (optional)</label><input type="password" minLength={8} value={password} onChange={e => setPassword(e.target.value)} placeholder="min 8 characters" autoComplete="new-password" /></div>
        <div className="field"><label>Country</label>
          <select value={country} onChange={e => setCountry(e.target.value)}>
            <option value="IN">India</option><option value="US">United States</option><option value="GB">United Kingdom</option>
            <option value="AE">United Arab Emirates</option><option value="SG">Singapore</option><option value="AU">Australia</option>
            <option value="CA">Canada</option><option value="DE">Germany</option><option value="OTHER">Other</option>
          </select>
        </div>
        <label className="check"><input type="checkbox" checked={marketing} onChange={e => setMarketing(e.target.checked)} /> I agree to receive promotional and instructional emails from CloudNest Academy</label>
        <p className="legal">By signing up, I agree to CloudNest Academy's <Link to="/privacy">Privacy</Link> &amp; <Link to="/terms">Terms</Link>.</p>
        {error && <div className="error">{error}</div>}
        <button className="btn sq block" disabled={busy}>Send code</button>
        <p className="small-center">Already have an account? <Link to="/login">Log in</Link></p>
      </form>
    </div>
  );
}

export function ForgotPassword() {
  const [email, setEmail] = useState('');
  const [code, setCode] = useState('');
  const [pw, setPw] = useState('');
  const [sent, setSent] = useState(false);
  const [error, setError] = useState('');
  useEffect(() => setMeta('Reset password'), []);
  const send = async (e: FormEvent) => {
    e.preventDefault(); setError('');
    try { devToast(await api<MessageResponse>('/api/auth/password/forgot', { body: { email } })); setSent(true); } catch (err) { setError(errMsg(err)); }
  };
  const reset = async (e: FormEvent) => {
    e.preventDefault(); setError('');
    try {
      const r = await api<MessageResponse>('/api/auth/password/reset', { body: { email, code, newPassword: pw } });
      toast(r.message); navigate('/login?mode=password');
    } catch (err) { setError(errMsg(err)); }
  };
  return (
    <div className="auth">
      {!sent ? (
        <form className="auth-box" onSubmit={send}>
          <h1>Reset password</h1>
          <div className="field"><label>Email</label><input type="email" required value={email} onChange={e => setEmail(e.target.value)} /></div>
          {error && <div className="error">{error}</div>}
          <button className="btn sq block">Send reset code</button>
        </form>
      ) : (
        <form className="auth-box" onSubmit={reset}>
          <h1>Choose a new password</h1>
          <div className="field"><label>Reset code</label><input required value={code} onChange={e => setCode(e.target.value)} maxLength={6} /></div>
          <div className="field"><label>New password</label><input type="password" required minLength={8} value={pw} onChange={e => setPw(e.target.value)} /></div>
          {error && <div className="error">{error}</div>}
          <button className="btn sq block">Update password</button>
        </form>
      )}
    </div>
  );
}
