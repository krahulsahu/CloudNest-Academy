import { useEffect, useState, type FormEvent } from 'react';
import { api } from '../../api';
import { Link, useLocation } from '../../router';
import type { AdminStats, AdminUser, Category, Coupon, CourseCard, Order, Plan, Review, Ticket } from '../../types';
import { errMsg, fmtDate, fmtDateTime, money, setMeta, toast, useLoad } from '../../util';
import { ErrorBox, Modal, Spinner } from '../../components/Layout';

const TABS: [string, string][] = [
  ['overview', 'Overview'], ['courses', 'Courses'], ['users', 'Users'], ['orders', 'Orders & refunds'], ['coupons', 'Coupons'],
  ['plans', 'Memberships'], ['categories', 'Categories'], ['tickets', 'Support tickets'], ['reviews', 'Reviews']
];

export default function AdminDashboard() {
  const { query } = useLocation();
  const tab = query.get('tab') || 'overview';
  useEffect(() => setMeta('Admin Dashboard'), []);
  return (
    <div className="page wide-page">
      <h1>Admin Dashboard</h1>
      <div className="tabs">{TABS.map(([k, l]) => <Link key={k} to={'/admin?tab=' + k} className={tab === k ? 'on' : ''}>{l}</Link>)}</div>
      {tab === 'overview' && <Overview />}
      {tab === 'courses' && <Courses />}
      {tab === 'users' && <Users />}
      {tab === 'orders' && <Orders />}
      {tab === 'coupons' && <Coupons />}
      {tab === 'plans' && <Plans />}
      {tab === 'categories' && <Categories />}
      {tab === 'tickets' && <Tickets />}
      {tab === 'reviews' && <Reviews />}
    </div>
  );
}

function Overview() {
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const q = new URLSearchParams();
  if (from) q.set('from', from);
  if (to) q.set('to', to);
  const { data: s, loading, error, reload } = useLoad(() => api<AdminStats>('/api/admin/stats?' + q.toString()), [from, to]);
  if (loading && !s) return <Spinner />;
  if (error || !s) return <ErrorBox message={error || ''} retry={reload} />;
  const max = Math.max(1, ...s.monthly.map(m => m.revenueInr + m.revenueUsd * 85));
  return (
    <>
      <div className="filters">
        <label className="inline">From <input type="date" value={from} onChange={e => setFrom(e.target.value)} /></label>
        <label className="inline">To <input type="date" value={to} onChange={e => setTo(e.target.value)} /></label>
        {(from || to) && <button className="btn ghost sq small" onClick={() => { setFrom(''); setTo(''); }}>Last 12 months</button>}
      </div>
      <div className="stats">
        <div><b>{s.students}</b><span>Students</span></div>
        <div><b>{s.activeLearners30d}</b><span>Active learners (30d)</span></div>
        <div><b>{s.retentionPercent}%</b><span>Retention (active / total)</span></div>
        <div><b>{s.instructors}</b><span>Instructors</span></div>
        <div><b>{s.publishedCourses}/{s.courses}</b><span>Published courses</span></div>
        <div><b>{s.pendingCourses}</b><span>Awaiting approval</span></div>
        <div><b>{s.enrollments}</b><span>Enrollments</span></div>
        <div><b>{s.certificates}</b><span>Completions (certificates)</span></div>
        <div><b>{s.revenueInr ? money(s.revenueInr, 'INR') : '₹0'}</b><span>Revenue INR</span></div>
        <div><b>{s.revenueUsd ? money(s.revenueUsd, 'USD') : '$0'}</b><span>Revenue USD</span></div>
        <div><b>{s.subscriptionRevenueInr ? money(s.subscriptionRevenueInr, 'INR') : '₹0'}</b><span>Subscription revenue</span></div>
        <div><b>{s.activeSubscriptions}</b><span>Active memberships</span></div>
        <div><b>{s.paidOrders}</b><span>Paid orders</span></div>
        <div><b>{s.refundedOrders}</b><span>Refunds</span></div>
      </div>
      <h3>Monthly revenue</h3>
      <div className="chart">
        {s.monthly.map(m => (
          <div key={m.month} className="bar" title={`${m.month}: ₹${m.revenueInr} + $${m.revenueUsd} · ${m.orders} orders`}>
            <i style={{ height: ((m.revenueInr + m.revenueUsd * 85) / max * 100) + '%' }} />
            <small>{m.month.slice(2)}</small>
          </div>
        ))}
      </div>
      <div className="two-col">
        <div><h3>Most popular courses</h3><ol>{s.popular.map(p => <li key={p.id}>{p.title} – {p.value} enrollments</li>)}</ol></div>
        <div><h3>Most profitable (INR)</h3><ol>{s.profitableInr.map(p => <li key={p.id}>{p.title} – {money(p.value, 'INR')}</li>)}</ol></div>
      </div>
    </>
  );
}

function Courses() {
  const { data, loading, reload } = useLoad(() => api<CourseCard[]>('/api/admin/courses'), []);
  const [filter, setFilter] = useState('');
  const act = async (id: number, action: string) => {
    try { await api(`/api/admin/courses/${id}/${action}`, { method: 'POST' }); toast('Updated'); reload(); } catch (e) { toast(errMsg(e), 'error'); }
  };
  if (loading) return <Spinner />;
  const list = (data || []).filter(c => !filter || c.status === filter);
  return (
    <>
      <div className="filters">
        <select value={filter} onChange={e => setFilter(e.target.value)}>
          <option value="">All statuses</option><option value="PENDING_REVIEW">Pending review</option><option value="PUBLISHED">Published</option>
          <option value="DRAFT">Draft</option><option value="SCHEDULED">Scheduled</option><option value="UNPUBLISHED">Unpublished</option>
        </select>
        <Link to="/instructor" className="btn ghost sq small">+ Create course / bundle</Link>
      </div>
      <table className="table">
        <thead><tr><th>Course</th><th>Instructor</th><th>Price</th><th>Status</th><th>Featured</th><th>Actions</th></tr></thead>
        <tbody>{list.map(c => (
          <tr key={c.id}>
            <td><b>{c.title}</b><br /><small className="muted">{c.kind} · {c.category} · {c.lessonCount} lessons</small></td>
            <td>{c.instructorName || '—'}</td>
            <td>{money(c.priceInr, 'INR')} / {money(c.priceUsd, 'USD')}</td>
            <td><span className={'pill ' + c.status.toLowerCase()}>{c.status.replace('_', ' ')}</span></td>
            <td><input type="checkbox" checked={c.featured} onChange={() => act(c.id, c.featured ? 'unfeature' : 'feature')} /></td>
            <td className="nowrap">
              {c.status !== 'PUBLISHED' && <button className="link" onClick={() => act(c.id, 'approve')}>Approve</button>}
              {c.status === 'PENDING_REVIEW' && <> · <button className="link" onClick={() => act(c.id, 'reject')}>Return to draft</button></>}
              {c.status === 'PUBLISHED' && <button className="link" onClick={() => act(c.id, 'unpublish')}>Unpublish</button>}
              {' · '}<Link to={'/instructor/courses/' + c.id}>Edit</Link> · <Link to={'/p/' + c.slug}>View</Link>
            </td>
          </tr>
        ))}</tbody>
      </table>
    </>
  );
}

function Users() {
  const [q, setQ] = useState('');
  const [dq, setDq] = useState('');
  const [enroll, setEnroll] = useState(false);
  useEffect(() => { const t = setTimeout(() => setDq(q), 300); return () => clearTimeout(t); }, [q]);
  const { data, loading, reload } = useLoad(() => api<AdminUser[]>('/api/admin/users?q=' + encodeURIComponent(dq)), [dq]);
  const run = async (fn: () => Promise<unknown>) => { try { await fn(); toast('Updated'); reload(); } catch (e) { toast(errMsg(e), 'error'); } };
  return (
    <>
      <div className="filters">
        <input placeholder="Search name or email" value={q} onChange={e => setQ(e.target.value)} />
        <button className="btn ghost sq small" onClick={() => setEnroll(true)}>Enroll a user manually</button>
      </div>
      {loading && <Spinner />}
      <table className="table">
        <thead><tr><th>User</th><th>Role</th><th>Verified</th><th>Joined</th><th>Status</th></tr></thead>
        <tbody>{data?.map(u => (
          <tr key={u.id}>
            <td>{u.name}<br /><small className="muted">{u.email}</small></td>
            <td><select value={u.role} onChange={e => run(() => api(`/api/admin/users/${u.id}/role`, { method: 'PUT', body: { role: e.target.value } }))}>
              <option>STUDENT</option><option>INSTRUCTOR</option><option>ADMIN</option></select></td>
            <td>{u.emailVerified ? '✔' : '—'}</td><td>{fmtDate(u.createdAt)}</td>
            <td><button className={'link' + (u.enabled ? ' danger' : '')} onClick={() => run(() => api(`/api/admin/users/${u.id}/enabled`, { method: 'PUT', body: { enabled: !u.enabled } }))}>{u.enabled ? 'Disable' : 'Enable'}</button></td>
          </tr>
        ))}</tbody>
      </table>
      {enroll && <EnrollModal onClose={() => setEnroll(false)} />}
    </>
  );
}

function EnrollModal({ onClose }: { onClose: () => void }) {
  const [email, setEmail] = useState('');
  const [slug, setSlug] = useState('');
  const courses = useLoad(() => api<CourseCard[]>('/api/admin/courses'), []);
  const submit = async (e: FormEvent) => {
    e.preventDefault();
    try { await api('/api/admin/enrollments', { body: { email, courseSlug: slug } }); toast('User enrolled'); onClose(); } catch (err) { toast(errMsg(err), 'error'); }
  };
  return (
    <Modal title="Enroll a user" onClose={onClose}>
      <form onSubmit={submit}>
        <div className="field"><label>User email</label><input type="email" required value={email} onChange={e => setEmail(e.target.value)} /></div>
        <div className="field"><label>Course or bundle</label>
          <select required value={slug} onChange={e => setSlug(e.target.value)}><option value="">Choose…</option>{courses.data?.map(c => <option key={c.id} value={c.slug}>{c.title}</option>)}</select>
        </div>
        <button className="btn sq block">Enroll</button>
      </form>
    </Modal>
  );
}

function Orders() {
  const { data, loading, reload } = useLoad(() => api<Order[]>('/api/admin/orders'), []);
  const [status, setStatus] = useState('');
  const refund = async (o: Order) => {
    if (!confirm(`Refund ${money(o.total, o.currency)} to ${o.userEmail}? Access will be removed.`)) return;
    try { await api(`/api/admin/orders/${o.id}/refund?force=true`, { method: 'POST' }); toast('Refunded'); reload(); } catch (e) { toast(errMsg(e), 'error'); }
  };
  const csv = () => {
    const rows = [['Order', 'Invoice', 'Email', 'Item', 'Currency', 'Total', 'Tax', 'Status', 'Provider', 'Paid at'],
      ...(data || []).map(o => [o.id, o.invoiceNumber ?? '', o.userEmail, o.item, o.currency, o.total, o.tax, o.status, o.provider ?? '', o.paidAt ?? ''])];
    const blob = new Blob([rows.map(r => r.map(x => `"${String(x).replace(/"/g, '""')}"`).join(',')).join('\n')], { type: 'text/csv' });
    const a = document.createElement('a'); a.href = URL.createObjectURL(blob); a.download = 'orders.csv'; a.click();
  };
  if (loading) return <Spinner />;
  const list = (data || []).filter(o => !status || o.status === status);
  return (
    <>
      <div className="filters">
        <select value={status} onChange={e => setStatus(e.target.value)}><option value="">All orders</option><option>PAID</option><option>CREATED</option><option>FAILED</option><option>REFUNDED</option></select>
        <button className="btn ghost sq small" onClick={csv}>Export CSV</button>
      </div>
      <table className="table">
        <thead><tr><th>#</th><th>Customer</th><th>Item</th><th>Total</th><th>Provider</th><th>Status</th><th>Date</th><th /></tr></thead>
        <tbody>{list.map(o => (
          <tr key={o.id}>
            <td>{o.id}<br /><small className="muted">{o.invoiceNumber}</small></td><td>{o.userEmail}</td><td>{o.item}{o.couponCode && <><br /><small className="muted">coupon {o.couponCode}</small></>}</td>
            <td>{money(o.total, o.currency)}</td><td>{o.provider}</td><td><span className={'pill ' + o.status.toLowerCase()}>{o.status}</span></td>
            <td>{fmtDateTime(o.paidAt || o.createdAt)}</td>
            <td>{o.status === 'PAID' && <button className="link danger" onClick={() => refund(o)}>Refund</button>}</td>
          </tr>
        ))}</tbody>
      </table>
    </>
  );
}

function Coupons() {
  const { data, loading, reload } = useLoad(() => api<Coupon[]>('/api/admin/coupons'), []);
  const blank: Coupon = { code: '', percentOff: 20, active: true, uses: 0, maxUses: null, expiresAt: null, courseSlug: '' };
  const [f, setF] = useState<Coupon>(blank);
  const save = async (e: FormEvent) => {
    e.preventDefault();
    try { await api('/api/admin/coupons', { body: f }); toast('Coupon saved'); setF(blank); reload(); } catch (err) { toast(errMsg(err), 'error'); }
  };
  const del = async (id: number) => { if (confirm('Delete coupon?')) { await api('/api/admin/coupons/' + id, { method: 'DELETE' }); reload(); } };
  return (
    <div className="two-col">
      <form className="form-card" onSubmit={save}>
        <h3>{f.id ? 'Edit coupon' : 'New coupon'}</h3>
        <div className="field"><label>Code</label><input required value={f.code} onChange={e => setF({ ...f, code: e.target.value.toUpperCase() })} /></div>
        <div className="field"><label>Percent off</label><input type="number" min={1} max={100} required value={f.percentOff} onChange={e => setF({ ...f, percentOff: Number(e.target.value) })} /></div>
        <div className="field"><label>Max uses (blank = unlimited)</label><input type="number" min={1} value={f.maxUses ?? ''} onChange={e => setF({ ...f, maxUses: e.target.value ? Number(e.target.value) : null })} /></div>
        <div className="field"><label>Expires</label><input type="date" value={f.expiresAt ? f.expiresAt.slice(0, 10) : ''} onChange={e => setF({ ...f, expiresAt: e.target.value ? new Date(e.target.value + 'T23:59:59').toISOString() : null })} /></div>
        <div className="field"><label>Only for course slug (blank = everything)</label><input value={f.courseSlug || ''} onChange={e => setF({ ...f, courseSlug: e.target.value })} /></div>
        <label className="check"><input type="checkbox" checked={f.active} onChange={e => setF({ ...f, active: e.target.checked })} /> Active</label>
        <button className="btn sq">Save coupon</button>
      </form>
      <div>
        {loading && <Spinner />}
        <table className="table">
          <thead><tr><th>Code</th><th>Off</th><th>Used</th><th>Expires</th><th /></tr></thead>
          <tbody>{data?.map(c => (
            <tr key={c.id}><td><b>{c.code}</b>{!c.active && ' (inactive)'}<br /><small className="muted">{c.courseSlug || 'all products'}</small></td><td>{c.percentOff}%</td>
              <td>{c.uses}{c.maxUses ? '/' + c.maxUses : ''}</td><td>{fmtDate(c.expiresAt)}</td>
              <td className="nowrap"><button className="link" onClick={() => setF(c)}>Edit</button> · <button className="link danger" onClick={() => del(c.id!)}>Delete</button></td></tr>
          ))}</tbody>
        </table>
      </div>
    </div>
  );
}

function Plans() {
  const { data, loading, reload } = useLoad(() => api<Plan[]>('/api/admin/plans'), []);
  const cats = useLoad(() => api<Category[]>('/api/categories'), []);
  const blank: Plan = { code: '', name: '', description: '', priceInr: 999, priceUsd: 15, periodDays: 30, category: '', level: '', active: true };
  const [f, setF] = useState<Plan>(blank);
  const save = async (e: FormEvent) => {
    e.preventDefault();
    try { await api('/api/admin/plans', { body: f }); toast('Plan saved'); setF(blank); reload(); } catch (err) { toast(errMsg(err), 'error'); }
  };
  return (
    <div className="two-col">
      <form className="form-card" onSubmit={save}>
        <h3>{f.id ? 'Edit plan' : 'New membership plan'}</h3>
        <div className="field"><label>Code</label><input required value={f.code} onChange={e => setF({ ...f, code: e.target.value })} disabled={!!f.id} /></div>
        <div className="field"><label>Name</label><input required value={f.name} onChange={e => setF({ ...f, name: e.target.value })} /></div>
        <div className="field"><label>Description</label><input value={f.description || ''} onChange={e => setF({ ...f, description: e.target.value })} /></div>
        <div className="field"><label>Price INR</label><input type="number" min={0} value={f.priceInr} onChange={e => setF({ ...f, priceInr: Number(e.target.value) })} /></div>
        <div className="field"><label>Price USD</label><input type="number" min={0} value={f.priceUsd} onChange={e => setF({ ...f, priceUsd: Number(e.target.value) })} /></div>
        <div className="field"><label>Billing period</label>
          <select value={f.periodDays} onChange={e => setF({ ...f, periodDays: Number(e.target.value) })}><option value={30}>Monthly (30 days)</option><option value={90}>Quarterly</option><option value={365}>Yearly</option></select>
        </div>
        <div className="field"><label>Included category</label>
          <select value={f.category || ''} onChange={e => setF({ ...f, category: e.target.value })}><option value="">All categories</option>{cats.data?.map(c => <option key={c.id}>{c.name}</option>)}</select>
        </div>
        <div className="field"><label>Included level</label>
          <select value={f.level || ''} onChange={e => setF({ ...f, level: e.target.value })}><option value="">All levels</option><option>Beginner</option><option>Intermediate</option><option>Advanced</option></select>
        </div>
        <label className="check"><input type="checkbox" checked={f.active} onChange={e => setF({ ...f, active: e.target.checked })} /> Active (shown on Subscribe page)</label>
        <button className="btn sq">Save plan</button>
      </form>
      <div>
        {loading && <Spinner />}
        <table className="table">
          <thead><tr><th>Plan</th><th>Price</th><th>Covers</th><th /></tr></thead>
          <tbody>{data?.map(p => (
            <tr key={p.code}><td><b>{p.name}</b>{!p.active && ' (inactive)'}<br /><small className="muted">{p.code} · {p.periodDays} days</small></td>
              <td>{money(p.priceInr, 'INR')} / {money(p.priceUsd, 'USD')}</td><td>{p.category || 'All'} · {p.level || 'all levels'}</td>
              <td><button className="link" onClick={() => setF({ ...p, category: p.category || '', level: p.level || '' })}>Edit</button></td></tr>
          ))}</tbody>
        </table>
      </div>
    </div>
  );
}

function Categories() {
  const { data, reload } = useLoad(() => api<Category[]>('/api/categories'), []);
  const [name, setName] = useState('');
  const add = async (e: FormEvent) => {
    e.preventDefault();
    try { await api('/api/admin/categories', { body: { name } }); setName(''); toast('Category added'); reload(); } catch (err) { toast(errMsg(err), 'error'); }
  };
  const del = async (c: Category) => { if (confirm(`Delete ${c.name}? Courses keep their category text.`)) { await api('/api/admin/categories/' + c.id, { method: 'DELETE' }); reload(); } };
  return (
    <div className="two-col">
      <form className="form-card" onSubmit={add}>
        <h3>Add category</h3>
        <div className="field"><input required value={name} onChange={e => setName(e.target.value)} placeholder="e.g. Google Cloud" /></div>
        <button className="btn sq">Add</button>
      </form>
      <div className="includes">{data?.map(c => <a key={c.id}><span>{c.name}</span><button className="link danger" onClick={() => del(c)}>Delete</button></a>)}</div>
    </div>
  );
}

function Tickets() {
  const { data, loading, reload } = useLoad(() => api<Ticket[]>('/api/admin/tickets'), []);
  const [reply, setReply] = useState<Record<number, string>>({});
  const update = async (t: Ticket, status: string) => {
    try { await api('/api/admin/tickets/' + t.id, { method: 'PUT', body: { status, reply: reply[t.id] ?? t.reply } }); toast('Ticket updated'); reload(); } catch (e) { toast(errMsg(e), 'error'); }
  };
  if (loading) return <Spinner />;
  if (!data?.length) return <div className="empty">No tickets.</div>;
  return (
    <div className="notes-list">
      {data.map(t => (
        <div key={t.id} className={'note-item' + (t.status === 'OPEN' ? ' unread' : '')}>
          <b>[{t.type}] {t.subject || '(no subject)'}</b> <span className={'pill ' + t.status.toLowerCase()}>{t.status}</span>
          <p><small className="muted">{t.name} &lt;{t.email}&gt; · {fmtDateTime(t.createdAt)}</small></p>
          <p>{t.message}</p>
          <textarea placeholder="Reply (emailed to the customer)" value={reply[t.id] ?? t.reply ?? ''} onChange={e => setReply({ ...reply, [t.id]: e.target.value })} />
          <div className="row">
            <button className="btn sq small" onClick={() => update(t, 'CLOSED')}>Send reply &amp; close</button>
            {t.status === 'CLOSED' && <button className="btn ghost sq small" onClick={() => update(t, 'OPEN')}>Reopen</button>}
          </div>
        </div>
      ))}
    </div>
  );
}

function Reviews() {
  const { data, loading, reload } = useLoad(() => api<Review[]>('/api/admin/reviews'), []);
  const toggle = async (r: Review) => { await api(`/api/admin/reviews/${r.id}/visible`, { method: 'PUT', body: { enabled: !r.visible } }); reload(); };
  if (loading) return <Spinner />;
  if (!data?.length) return <div className="empty">No reviews yet.</div>;
  return (
    <table className="table">
      <thead><tr><th>Course</th><th>Student</th><th>Rating</th><th>Comment</th><th /></tr></thead>
      <tbody>{data.map(r => (
        <tr key={r.id} className={r.visible ? '' : 'dim'}><td>{r.courseTitle}</td><td>{r.userName}</td><td>{'★'.repeat(r.rating)}</td><td>{r.comment}</td>
          <td><button className="link" onClick={() => toggle(r)}>{r.visible ? 'Hide' : 'Show'}</button></td></tr>
      ))}</tbody>
    </table>
  );
}
