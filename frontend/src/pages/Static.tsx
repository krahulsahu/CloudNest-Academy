import { useEffect, useState, type FormEvent } from 'react';
import { api } from '../api';
import { useAuth } from '../auth';
import { Link, navigate, useLocation } from '../router';
import type { Certificate, Order, Plan } from '../types';
import { errMsg, fmtDate, money, setMeta, toast, useCurrency, useLoad } from '../util';
import { CheckoutModal } from '../components/CheckoutModal';
import { ErrorBox, Spinner } from '../components/Layout';

export function Subscribe() {
  const { user } = useAuth();
  const [cur] = useCurrency();
  const [plan, setPlan] = useState<string | null>(null);
  const { data, loading, error } = useLoad(() => api<Plan[]>('/api/plans'), []);
  useEffect(() => setMeta('Subscribe', 'Membership plans with monthly and yearly billing.'), []);
  const choose = (code: string) => {
    if (!user) { toast('Please sign up or log in first'); return navigate('/signup?next=/subscribe'); }
    setPlan(code);
  };
  return (
    <div className="page">
      <h1>Subscribe</h1>
      <p className="lead">One price, many courses. Cancel any time – you keep access until the end of the period you paid for.</p>
      {loading && <Spinner />}
      {error && <ErrorBox message={error} />}
      <div className="plans">
        {data?.map((p, i) => (
          <div key={p.code} className={'plan' + (i === 1 ? ' hot' : '')}>
            <h3>{p.name}</h3>
            <div className="amt">{money(cur === 'USD' ? p.priceUsd : p.priceInr, cur)}<small>/{p.periodDays >= 365 ? 'year' : p.periodDays === 30 ? 'month' : p.periodDays + ' days'}</small></div>
            <p>{p.description}</p>
            <ul>
              <li>{p.category ? `All ${p.category} courses` : 'Every category'}</li>
              <li>{p.level ? `${p.level} level` : 'All levels'}</li>
              <li>Quizzes, practice exams &amp; certificates</li>
              <li>Renewal reminders before expiry</li>
            </ul>
            <button className="btn sq" onClick={() => choose(p.code)}>Choose {p.name}</button>
          </div>
        ))}
      </div>
      <p className="lead small center" style={{ marginTop: 30 }}>Prefer to own a course forever? <Link to="/courses">Buy individual courses or bundles</Link> with lifetime access.</p>
      {plan && <CheckoutModal planCode={plan} onClose={() => setPlan(null)} />}
    </div>
  );
}

export function Roadmap() {
  useEffect(() => setMeta('Roadmap', 'A suggested learning path from fundamentals to expert.'), []);
  const steps: [string, string, [string, string][]][] = [
    ['Tech Fundamentals', 'Networking, DNS, Linux and encryption basics.', [['tech-fundamentals', 'Tech Fundamentals (Free)']]],
    ['Associate level', 'Pick the Associate course matching your role.', [['aws-solutions-architect-associate', 'Solutions Architect – Associate'], ['aws-developer-associate', 'Developer – Associate'], ['aws-cloudops-associate', 'CloudOps – Associate'], ['azure-administrator-az104', 'Azure AZ-104']]],
    ['Identity & Security', 'Learn IAM, IGA and SailPoint Identity Security Cloud.', [['iam-iga-fundamentals', 'IAM & IGA Fundamentals'], ['sailpoint-isc-complete', 'SailPoint ISC Complete']]],
    ['Engineering & DevOps', 'Microservices, containers, Kubernetes and CI/CD.', [['java-spring-microservices', 'Spring Boot Microservices'], ['kubernetes-devops', 'Kubernetes & DevOps']]],
    ['Professional & AI', 'Professional certifications and AI engineering.', [['aws-solutions-architect-professional', 'Solutions Architect – Professional'], ['azure-devops-az400', 'Azure AZ-400'], ['ai-engineering', 'AI Engineering']]]
  ];
  return (
    <div className="page">
      <h1>Roadmap</h1>
      <p className="lead">A suggested learning path from fundamentals to expert.</p>
      <div className="roadmap">
        {steps.map(([t, d, links], i) => (
          <div className="step" key={t}>
            <div className="num">{i + 1}</div>
            <div><h3>{t}</h3><p>{d}</p><div className="links">{links.map(([s, l]) => <Link key={s} to={'/p/' + s}>{l}</Link>)}</div></div>
          </div>
        ))}
      </div>
    </div>
  );
}

export function ContactForm({ type, title, lead }: { type: 'CONTACT' | 'TEAM'; title: string; lead: string }) {
  const { user } = useAuth();
  const [f, setF] = useState({ name: user?.name || '', email: user?.email || '', subject: '', message: '', company: '', seats: '10' });
  const [busy, setBusy] = useState(false);
  useEffect(() => setMeta(title), [title]);
  const submit = async (e: FormEvent) => {
    e.preventDefault(); setBusy(true);
    const body = type === 'TEAM'
      ? { type, name: f.name, email: f.email, subject: `Team training: ${f.company} (${f.seats} learners)`, message: f.message || 'Please send a quote.' }
      : { type, name: f.name, email: f.email, subject: f.subject, message: f.message };
    try { const r = await api<{ message: string }>('/api/contact', { body }); toast(r.message); setF({ ...f, subject: '', message: '' }); }
    catch (err) { toast(errMsg(err), 'error'); } finally { setBusy(false); }
  };
  return (
    <div className="page">
      <h1>{title}</h1>
      <p className="lead">{lead}</p>
      {type === 'TEAM' && (
        <div className="why left">
          <div><b>👥 Bulk licences</b><p>Volume pricing for 5+ learners with corporate GST invoices.</p></div>
          <div><b>📊 Progress reports</b><p>Track completion and quiz scores for every employee.</p></div>
          <div><b>🎯 Assigned paths</b><p>Assign courses and roadmaps to teams.</p></div>
        </div>
      )}
      <form className="form-card" onSubmit={submit}>
        <div className="field"><label>Name</label><input required value={f.name} onChange={e => setF({ ...f, name: e.target.value })} /></div>
        <div className="field"><label>{type === 'TEAM' ? 'Work email' : 'Email'}</label><input type="email" required value={f.email} onChange={e => setF({ ...f, email: e.target.value })} /></div>
        {type === 'TEAM' ? <>
          <div className="field"><label>Company</label><input required value={f.company} onChange={e => setF({ ...f, company: e.target.value })} /></div>
          <div className="field"><label>Number of learners</label><input type="number" min={2} required value={f.seats} onChange={e => setF({ ...f, seats: e.target.value })} /></div>
        </> : <div className="field"><label>Subject</label><input value={f.subject} onChange={e => setF({ ...f, subject: e.target.value })} /></div>}
        <div className="field"><label>Message</label><textarea required={type === 'CONTACT'} value={f.message} onChange={e => setF({ ...f, message: e.target.value })} /></div>
        <button className="btn sq" disabled={busy}>{type === 'TEAM' ? 'Request a quote' : 'Send message'}</button>
      </form>
    </div>
  );
}

export function Faq() {
  useEffect(() => setMeta('FAQ'), []);
  const items: [string, string][] = [
    ['How do I log in?', 'Enter your email and we send you a one-time code. You can also set a password from your account page.'],
    ['How long do I have access?', 'Individual courses and bundles include lifetime access and future updates. Memberships give access while active.'],
    ['Which payment methods do you accept?', 'UPI, cards, net banking and wallets in India through Razorpay, and international cards through Stripe.'],
    ['Do you provide invoices?', 'Yes. Every paid order has a downloadable tax invoice in My Courses → Orders & invoices.'],
    ['What is your refund policy?', 'Request a refund within 14 days of purchase. Refunds go back to the original payment method.'],
    ['How do bundle upgrades work?', 'If you already own courses in a bundle, their price is credited automatically when you buy the bundle.'],
    ['Are certificates verifiable?', 'Yes. Every certificate has a unique ID that anyone can check on our Verify Certificate page.'],
    ['Can my company buy for a team?', 'Yes – see Team Training for bulk licences and progress reports.']
  ];
  return <div className="page"><div className="faq wide"><h2>Frequently Asked Questions</h2>{items.map(([q, a]) => <div key={q}><h4>{q}</h4><p>{a}</p></div>)}</div></div>;
}

export function Legal({ kind }: { kind: 'terms' | 'privacy' }) {
  useEffect(() => setMeta(kind === 'terms' ? 'Terms of Use' : 'Privacy Policy'), [kind]);
  return kind === 'terms' ? (
    <div className="page prose-page">
      <h1>Terms of Use</h1>
      <p className="warn">Template – have these terms reviewed by a legal professional before launch.</p>
      <p>Courses are licensed for individual personal use. Sharing accounts, downloading or redistributing videos and materials is not permitted.</p>
      <p>One-time purchases include access for the lifetime of the course. Memberships give access while active, plus a short grace period after expiry.</p>
      <p>Refunds can be requested within 14 days of purchase. Refunded purchases lose access to the related courses.</p>
      <p>Certificates are issued when all lessons are completed and all quizzes are passed.</p>
    </div>
  ) : (
    <div className="page prose-page">
      <h1>Privacy Policy</h1>
      <p className="warn">Template – review for India's DPDP Act, GDPR and your payment providers before launch.</p>
      <p>We collect your name, email, country, purchases and learning progress to provide our courses. Payments are processed by Razorpay and Stripe; we never store your card details.</p>
      <p>We send transactional emails (login codes, receipts, course updates). Marketing emails are only sent if you opt in, and you can unsubscribe at any time.</p>
      <p>You can request a copy or deletion of your data by contacting support.</p>
    </div>
  );
}

export function Verify({ code: initial }: { code?: string }) {
  const [code, setCode] = useState(initial || '');
  const [result, setResult] = useState<Certificate | null>(null);
  const [error, setError] = useState('');
  useEffect(() => setMeta('Verify Certificate'), []);
  const check = async (c: string) => {
    setError(''); setResult(null);
    try { setResult(await api<Certificate>('/api/certificates/verify/' + encodeURIComponent(c.trim()))); } catch (e) { setError(errMsg(e)); }
  };
  useEffect(() => { if (initial) check(initial); }, [initial]);
  return (
    <div className="page">
      <h1>Verify a certificate</h1>
      <p className="lead">Enter the certificate ID printed on the certificate.</p>
      <form className="filters" onSubmit={e => { e.preventDefault(); navigate('/verify/' + encodeURIComponent(code.trim())); }}>
        <input required value={code} onChange={e => setCode(e.target.value)} placeholder="CN-XXXXXXXXXX" />
        <button className="btn sq small">Verify</button>
      </form>
      {error && <div className="empty">❌ {error}</div>}
      {result && (
        <div className="verified">
          <div className="big">✅ Valid certificate</div>
          <p><b>{result.studentName}</b> completed <Link to={'/p/' + result.courseSlug}>{result.courseTitle}</Link> on {fmtDate(result.issuedAt)}.</p>
          <p className="muted">Certificate ID: {result.code}</p>
          <a className="btn sq small" href={'/api/certificates/' + result.code + '/pdf'} target="_blank" rel="noopener noreferrer">View PDF</a>
        </div>
      )}
    </div>
  );
}

/** Stripe redirects here; the webhook marks the order paid, so we poll the order status. */
export function CheckoutSuccess() {
  const { query } = useLocation();
  const id = query.get('order');
  const [order, setOrder] = useState<Order | null>(null);
  const [tries, setTries] = useState(0);
  useEffect(() => {
    if (!id) return;
    api<Order>('/api/me/orders/' + id).then(o => {
      setOrder(o);
      if (o.status !== 'PAID' && tries < 15) setTimeout(() => setTries(t => t + 1), 2000);
    }).catch(() => undefined);
  }, [id, tries]);
  return (
    <div className="page center">
      {!order || order.status === 'CREATED' ? <><Spinner /><p>Confirming your payment…</p></>
        : order.status === 'PAID' ? <><h1>🎉 Payment successful</h1><p className="lead">You're enrolled in {order.item}.</p><Link to="/dashboard" className="btn">Go to My Courses</Link></>
          : <><h1>Payment not completed</h1><p className="lead">Status: {order.status}. No money was taken.</p><Link to="/courses" className="btn">Back to courses</Link></>}
    </div>
  );
}

export function NotFound() {
  useEffect(() => setMeta('Page not found'), []);
  return <div className="page center"><h1>Page not found</h1><p className="lead">The page you're looking for doesn't exist.</p><Link to="/" className="btn">Go home</Link></div>;
}
