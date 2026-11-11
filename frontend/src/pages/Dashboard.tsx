import { useEffect, useState, type FormEvent } from 'react';
import { api, download } from '../api';
import { useAuth } from '../auth';
import { Link, navigate, useLocation } from '../router';
import type { Bookmark, Certificate, MyCourse, Notification, Order, QuizHistory, Subscription, Ticket, User } from '../types';
import { errMsg, fmtDate, fmtDateTime, money, setMeta, toast, useLoad } from '../util';
import { Thumb } from '../components/CourseCard';
import { ErrorBox, Spinner } from '../components/Layout';

const TABS: [string, string][] = [
  ['courses', 'My Courses'], ['certificates', 'Certificates'], ['bookmarks', 'Bookmarks'], ['quizzes', 'Quiz history'],
  ['orders', 'Orders & invoices'], ['subscriptions', 'Subscriptions'], ['notifications', 'Notifications'], ['support', 'Support'], ['profile', 'Account']
];

export default function Dashboard() {
  const { user } = useAuth();
  const { query } = useLocation();
  const tab = query.get('tab') || 'courses';
  useEffect(() => setMeta('My Dashboard'), []);
  if (!user) return null;
  return (
    <div className="page dash">
      <div className="dash-head">
        <div><h1>Welcome back, {user.name.split(' ')[0]}</h1><p className="lead">Pick up where you left off.</p></div>
        <Link to="/courses" className="btn ghost sq small">Browse more courses</Link>
      </div>
      <div className="tabs">
        {TABS.map(([k, label]) => <Link key={k} to={'/dashboard?tab=' + k} className={tab === k ? 'on' : ''}>{label}</Link>)}
      </div>
      {tab === 'courses' && <Courses />}
      {tab === 'certificates' && <Certificates />}
      {tab === 'bookmarks' && <Bookmarks />}
      {tab === 'quizzes' && <Quizzes />}
      {tab === 'orders' && <Orders />}
      {tab === 'subscriptions' && <Subscriptions />}
      {tab === 'notifications' && <Notifications />}
      {tab === 'support' && <Support />}
      {tab === 'profile' && <Profile user={user} />}
    </div>
  );
}

function Courses() {
  const { data, error, loading, reload } = useLoad(() => api<MyCourse[]>('/api/me/courses'), []);
  if (loading) return <Spinner />;
  if (error) return <ErrorBox message={error} retry={reload} />;
  if (!data?.length) return <div className="empty">You haven't enrolled in any courses yet.<br /><br /><Link to="/courses" className="btn">Explore courses</Link></div>;
  const last = data.filter(c => c.completed > 0 && c.completed < c.total)[0];
  return (
    <>
      {last && (
        <div className="continue">
          <div><small className="muted">CONTINUE LEARNING</small><h3>{last.card.title}</h3>
            <div className="progress"><i style={{ width: Math.round(last.completed / last.total * 100) + '%' }} /></div></div>
          <Link to={`/learn/${last.card.slug}/${last.resumeLessonId ?? ''}`} className="btn sq">Resume →</Link>
        </div>
      )}
      <div className="grid flush">
        {data.map(m => {
          const pct = m.total ? Math.round(m.completed / m.total * 100) : 0;
          return (
            <div className="card static" key={m.card.id}>
              <Thumb c={m.card} />
              <div className="card-body left">
                <h3>{m.card.title}</h3>
                <div className="progress"><i style={{ width: pct + '%' }} /></div>
                <small>{pct}% complete · {m.completed}/{m.total} lessons</small>
                <small className="muted">{m.access === 'MEMBERSHIP' ? `Membership access until ${fmtDate(m.expiresAt)}` : 'Lifetime access'}</small>
                {m.certificateCode && <small>🎓 Certificate {m.certificateCode}</small>}
                <Link to={`/learn/${m.card.slug}/${m.resumeLessonId ?? ''}`} className="btn sq small block">{pct ? 'Continue' : 'Start'} course</Link>
              </div>
            </div>
          );
        })}
      </div>
    </>
  );
}

function Certificates() {
  const { data, loading } = useLoad(() => api<Certificate[]>('/api/me/certificates'), []);
  if (loading) return <Spinner />;
  if (!data?.length) return <div className="empty">Complete a course and pass its quizzes to earn a certificate.</div>;
  return (
    <table className="table">
      <thead><tr><th>Course</th><th>Certificate ID</th><th>Issued</th><th /></tr></thead>
      <tbody>{data.map(c => (
        <tr key={c.code}>
          <td>{c.courseTitle}</td><td><Link to={'/verify/' + c.code}>{c.code}</Link></td><td>{fmtDate(c.issuedAt)}</td>
          <td><button className="link" onClick={() => download(`/api/certificates/${c.code}/pdf`, `certificate-${c.code}.pdf`)}>Download PDF</button></td>
        </tr>
      ))}</tbody>
    </table>
  );
}

function Bookmarks() {
  const { data, loading } = useLoad(() => api<Bookmark[]>('/api/me/bookmarks'), []);
  if (loading) return <Spinner />;
  if (!data?.length) return <div className="empty">Use ☆ Bookmark inside a lesson to save it here.</div>;
  return <div className="includes">{data.map(b => <Link key={b.lessonId} to={`/learn/${b.courseSlug}/${b.lessonId}`}><span>★ {b.lessonTitle}</span><span className="muted">{b.courseTitle}</span></Link>)}</div>;
}

function Quizzes() {
  const { data, loading } = useLoad(() => api<QuizHistory[]>('/api/me/quiz-history'), []);
  if (loading) return <Spinner />;
  if (!data?.length) return <div className="empty">No quiz attempts yet.</div>;
  return (
    <table className="table">
      <thead><tr><th>Course</th><th>Quiz</th><th>Score</th><th>Result</th><th>Date</th></tr></thead>
      <tbody>{data.map((q, i) => <tr key={i}><td>{q.courseTitle}</td><td>{q.lessonTitle}</td><td>{q.score}/{q.total}</td><td>{q.passed ? '✅ Passed' : '❌ Not passed'}</td><td>{fmtDateTime(q.at)}</td></tr>)}</tbody>
    </table>
  );
}

function Orders() {
  const { data, loading, reload } = useLoad(() => api<Order[]>('/api/me/orders'), []);
  const refund = async (o: Order) => {
    const reason = prompt('Please tell us briefly why you would like a refund:');
    if (reason === null) return;
    try { const r = await api<{ message: string }>(`/api/me/orders/${o.id}/refund-request`, { body: { subject: 'Refund', message: reason || 'No reason given' } }); toast(r.message); reload(); }
    catch (e) { toast(errMsg(e), 'error'); }
  };
  if (loading) return <Spinner />;
  if (!data?.length) return <div className="empty">No orders yet.</div>;
  return (
    <table className="table">
      <thead><tr><th>Order</th><th>Item</th><th>Total</th><th>Status</th><th>Date</th><th /></tr></thead>
      <tbody>{data.map(o => (
        <tr key={o.id}>
          <td>#{o.id}<br /><small className="muted">{o.invoiceNumber}</small></td><td>{o.item}</td>
          <td>{money(o.total, o.currency)}{o.discount + o.credit > 0 && <><br /><small className="muted">saved {money(o.discount + o.credit, o.currency)}</small></>}</td>
          <td><span className={'pill ' + o.status.toLowerCase()}>{o.status}</span></td><td>{fmtDate(o.paidAt || o.createdAt)}</td>
          <td>
            {o.invoiceNumber && <button className="link" onClick={() => download(`/api/me/orders/${o.id}/invoice`, o.invoiceNumber + '.pdf')}>Invoice</button>}
            {o.status === 'PAID' && o.total > 0 && <> · <button className="link" onClick={() => refund(o)}>Request refund</button></>}
          </td>
        </tr>
      ))}</tbody>
    </table>
  );
}

function Subscriptions() {
  const { data, loading, reload } = useLoad(() => api<Subscription[]>('/api/me/subscriptions'), []);
  const cancel = async (s: Subscription) => {
    if (!confirm(`Cancel ${s.planName}? You keep access until ${fmtDate(s.expiresAt)}.`)) return;
    try { await api(`/api/me/subscriptions/${s.id}/cancel`, { method: 'POST' }); toast('Subscription cancelled'); reload(); } catch (e) { toast(errMsg(e), 'error'); }
  };
  if (loading) return <Spinner />;
  if (!data?.length) return <div className="empty">No memberships. <Link to="/subscribe">See plans</Link></div>;
  return (
    <table className="table">
      <thead><tr><th>Plan</th><th>Status</th><th>Started</th><th>Access until</th><th /></tr></thead>
      <tbody>{data.map(s => (
        <tr key={s.id}><td>{s.planName}</td><td><span className={'pill ' + s.status.toLowerCase()}>{s.status}</span></td><td>{fmtDate(s.startsAt)}</td><td>{fmtDate(s.expiresAt)}</td>
          <td>{s.status === 'ACTIVE' && <button className="link" onClick={() => cancel(s)}>Cancel</button>}
            {(s.status !== 'ACTIVE') && <Link to="/subscribe">Renew</Link>}</td></tr>
      ))}</tbody>
    </table>
  );
}

function Notifications() {
  const { data, loading } = useLoad(() => api<Notification[]>('/api/me/notifications'), []);
  useEffect(() => {
    api('/api/me/notifications/read-all', { method: 'POST' }).then(() => window.dispatchEvent(new Event('app:notifications'))).catch(() => undefined);
  }, []);
  if (loading) return <Spinner />;
  if (!data?.length) return <div className="empty">No notifications yet.</div>;
  return (
    <div className="notes-list">
      {data.map(n => (
        <div key={n.id} className={'note-item' + (n.seen ? '' : ' unread')} onClick={() => n.link && navigate(n.link)}>
          <b>{n.title}</b><p>{n.body}</p><small className="muted">{fmtDateTime(n.createdAt)}</small>
        </div>
      ))}
    </div>
  );
}

function Support() {
  const { data, loading, reload } = useLoad(() => api<Ticket[]>('/api/me/tickets'), []);
  const [subject, setSubject] = useState('');
  const [message, setMessage] = useState('');
  const send = async (e: FormEvent) => {
    e.preventDefault();
    try { await api('/api/me/tickets', { body: { subject, message } }); toast('Support request sent'); setSubject(''); setMessage(''); reload(); }
    catch (err) { toast(errMsg(err), 'error'); }
  };
  return (
    <div className="two-col">
      <form className="form-card" onSubmit={send}>
        <h3>Open a support request</h3>
        <div className="field"><label>Subject</label><input required maxLength={200} value={subject} onChange={e => setSubject(e.target.value)} /></div>
        <div className="field"><label>Message</label><textarea required value={message} onChange={e => setMessage(e.target.value)} /></div>
        <button className="btn sq">Send</button>
      </form>
      <div>
        <h3>Your requests</h3>
        {loading && <Spinner />}
        {data?.length === 0 && <p className="muted">No requests yet.</p>}
        {data?.map(t => (
          <div className="note-item" key={t.id}>
            <b>{t.subject}</b> <span className={'pill ' + t.status.toLowerCase()}>{t.status}</span>
            <p>{t.message}</p>
            {t.reply && <p className="reply"><b>Reply:</b> {t.reply}</p>}
            <small className="muted">{fmtDateTime(t.createdAt)}</small>
          </div>
        ))}
      </div>
    </div>
  );
}

function Profile({ user }: { user: User }) {
  const { setUser, logout } = useAuth();
  const [p, setP] = useState({ name: user.name, country: user.country || '', headline: user.headline || '', bio: user.bio || '' });
  const [pw, setPw] = useState({ currentPassword: '', newPassword: '' });
  const save = async (e: FormEvent) => {
    e.preventDefault();
    try { setUser(await api<User>('/api/me', { method: 'PUT', body: p })); toast('Profile saved'); } catch (err) { toast(errMsg(err), 'error'); }
  };
  const changePw = async (e: FormEvent) => {
    e.preventDefault();
    try { await api('/api/me/password', { method: 'PUT', body: pw }); toast('Password updated'); setPw({ currentPassword: '', newPassword: '' }); setUser({ ...user, hasPassword: true }); }
    catch (err) { toast(errMsg(err), 'error'); }
  };
  const logoutAll = async () => {
    await api('/api/me/logout-all', { method: 'POST' }).catch(() => undefined);
    await logout(); toast('Logged out on all devices'); navigate('/');
  };
  return (
    <div className="two-col">
      <form className="form-card" onSubmit={save}>
        <h3>Profile</h3>
        <p className="muted">{user.email} · {user.role}</p>
        <div className="field"><label>Name</label><input required value={p.name} onChange={e => setP({ ...p, name: e.target.value })} /></div>
        <div className="field"><label>Country</label><input value={p.country} onChange={e => setP({ ...p, country: e.target.value })} /></div>
        {user.role !== 'STUDENT' && <>
          <div className="field"><label>Headline (shown on course pages)</label><input value={p.headline} onChange={e => setP({ ...p, headline: e.target.value })} /></div>
          <div className="field"><label>Instructor bio</label><textarea value={p.bio} onChange={e => setP({ ...p, bio: e.target.value })} /></div>
        </>}
        <button className="btn sq">Save profile</button>
      </form>
      <div>
        <form className="form-card" onSubmit={changePw}>
          <h3>{user.hasPassword ? 'Change password' : 'Set a password'}</h3>
          {user.hasPassword && <div className="field"><label>Current password</label><input type="password" required value={pw.currentPassword} onChange={e => setPw({ ...pw, currentPassword: e.target.value })} /></div>}
          <div className="field"><label>New password</label><input type="password" required minLength={8} value={pw.newPassword} onChange={e => setPw({ ...pw, newPassword: e.target.value })} /></div>
          <button className="btn sq">Update password</button>
        </form>
        <div className="form-card" style={{ marginTop: 20 }}>
          <h3>Security</h3>
          <p className="muted">Signed in on a shared or lost device?</p>
          <button className="btn ghost sq small" onClick={logoutAll}>Log out on all devices</button>
        </div>
      </div>
    </div>
  );
}
