import { useEffect, useState, type FormEvent, type ReactNode } from 'react';
import { Link, navigate } from '../router';
import { useAuth } from '../auth';
import { api } from '../api';
import { SITE } from '../config';
import { toast, useCurrency } from '../util';

export function Brand() {
  return (
    <span className="brand">
      <img src="/logo.png" alt="Logo" className="brand-img" onError={(e) => { (e.target as HTMLElement).style.display = 'none'; }} />
      <span className="brand-text">CloudNest<small>ACADEMY</small></span>
    </span>
  );
}

export function Header() {
  const { user, logout } = useAuth();
  const [open, setOpen] = useState(false);
  const [more, setMore] = useState(false);
  const [unread, setUnread] = useState(0);
  const [cur, setCur] = useCurrency();

  useEffect(() => {
    if (!user) { setUnread(0); return; }
    const load = () => api<number>('/api/me/notifications/unread').then(setUnread).catch(() => undefined);
    load();
    const t = setInterval(load, 60000);
    window.addEventListener('app:notifications', load);
    return () => { clearInterval(t); window.removeEventListener('app:notifications', load); };
  }, [user]);

  useEffect(() => {
    const close = () => setMore(false);
    document.addEventListener('click', close);
    return () => document.removeEventListener('click', close);
  }, []);

  const close = () => { setOpen(false); setMore(false); };
  return (
    <header className="topbar">
      <div className="topbar-inner">
        <Link to="/" onClick={close}><Brand /></Link>
        <button className="burger" aria-label="Menu" onClick={() => setOpen(!open)}>☰</button>
        <nav className={'mainnav' + (open ? ' open' : '')}>
          <Link to="/subscribe" onClick={close}>Subscribe</Link>
          <Link to="/offers" onClick={close}>50% off Launch Offer</Link>
          <Link to="/courses" onClick={close}>All Courses</Link>
          <Link to="/courses?category=Azure" onClick={close}>Azure Courses &amp; Bundles</Link>
          <Link to="/roadmap" onClick={close}>Roadmap</Link>
          <div className={'dropdown' + (more ? ' open' : '')} onClick={e => e.stopPropagation()}>
            <button className="dropbtn" onClick={() => setMore(!more)}>More ▾</button>
            <div className="dropmenu">
              <Link to="/courses?category=SailPoint" onClick={close}>SailPoint ISC</Link>
              <Link to="/courses?category=Kubernetes" onClick={close}>DevOps &amp; Kubernetes</Link>
              <Link to="/teams" onClick={close}>Team Training</Link>
              <Link to="/verify" onClick={close}>Verify Certificate</Link>
              <Link to="/faq" onClick={close}>FAQ</Link>
              <Link to="/contact" onClick={close}>Contact</Link>
              {user && (user.role === 'INSTRUCTOR' || user.role === 'ADMIN') && <Link to="/instructor" onClick={close}>Instructor Portal</Link>}
              {user?.role === 'ADMIN' && <Link to="/admin" onClick={close}>Admin Dashboard</Link>}
            </div>
          </div>
          <select className="cur" value={cur} onChange={e => setCur(e.target.value as 'INR' | 'USD')} aria-label="Currency">
            <option value="INR">₹ INR</option>
            <option value="USD">$ USD</option>
          </select>
          {user ? (
            <>
              <Link to="/dashboard" onClick={close}>My Courses</Link>
              <Link to="/dashboard?tab=notifications" onClick={close} title="Notifications">
                🔔{unread > 0 && <span className="badge-dot">{unread}</span>}
              </Link>
              <a href="/" onClick={async e => { e.preventDefault(); close(); await logout(); toast('Logged out'); navigate('/'); }}>Log Out</a>
            </>
          ) : (
            <>
              <Link to="/login" onClick={close}>Login</Link>
              <Link to="/signup" onClick={close}>Sign Up</Link>
            </>
          )}
        </nav>
      </div>
    </header>
  );
}

export function Footer() {
  const [email, setEmail] = useState('');
  const subscribe = async (e: FormEvent) => {
    e.preventDefault();
    try {
      const r = await api<{ message: string }>('/api/newsletter', { body: { email } });
      toast(r.message); setEmail('');
    } catch (err) { toast((err as Error).message, 'error'); }
  };
  return (
    <footer className="footer">
      <div className="footer-inner">
        <div>
          <div>© CloudNest Academy {new Date().getFullYear()}</div>
          <form className="newsletter" onSubmit={subscribe}>
            <input type="email" required placeholder="Your email for new courses & offers" value={email} onChange={e => setEmail(e.target.value)} />
            <button>Subscribe</button>
          </form>
        </div>
        <div className="footer-links">
          <Link to="/terms">Terms of Use</Link>
          <Link to="/privacy">Privacy Policy</Link>
          <Link to="/contact">Support</Link>
          <span className="footer-small">Trainer: {SITE.trainer}</span>
        </div>
      </div>
    </footer>
  );
}

export function Toaster() {
  const [t, setT] = useState<{ message: string; kind: string; n: number } | null>(null);
  useEffect(() => {
    let timer: number | undefined;
    const f = (e: Event) => {
      const d = (e as CustomEvent).detail;
      setT({ ...d, n: Date.now() });
      clearTimeout(timer);
      timer = window.setTimeout(() => setT(null), 4200);
    };
    window.addEventListener('app:toast', f);
    return () => window.removeEventListener('app:toast', f);
  }, []);
  return <div className={'toast' + (t ? ' show' : '') + (t?.kind === 'error' ? ' err' : '')} role="status">{t?.message}</div>;
}

export function Spinner() { return <div className="spinner" aria-label="Loading" />; }

export function ErrorBox({ message, retry }: { message: string; retry?: () => void }) {
  return (
    <div className="empty">
      <p>{message}</p>
      {retry && <button className="btn sq" onClick={retry}>Try again</button>}
    </div>
  );
}

export function Modal({ title, onClose, children, wide }: { title: string; onClose: () => void; children: ReactNode; wide?: boolean }) {
  useEffect(() => {
    const k = (e: KeyboardEvent) => e.key === 'Escape' && onClose();
    window.addEventListener('keydown', k);
    return () => window.removeEventListener('keydown', k);
  }, [onClose]);
  return (
    <div className="modal-bg" onClick={onClose}>
      <div className={'modal' + (wide ? ' wide' : '')} onClick={e => e.stopPropagation()} role="dialog" aria-label={title}>
        <button className="x" onClick={onClose} aria-label="Close">×</button>
        <h3>{title}</h3>
        {children}
      </div>
    </div>
  );
}
