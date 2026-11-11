import { useEffect, useState, type FormEvent } from 'react';
import { api } from '../api';
import { useAuth } from '../auth';
import { Link, navigate } from '../router';
import type { CourseDetail as Detail, Review } from '../types';
import { duration, errMsg, fmtDate, money, priceOf, setMeta, toast, useCurrency, useLoad } from '../util';
import { CourseCard } from '../components/CourseCard';
import { CheckoutModal } from '../components/CheckoutModal';
import { ErrorBox, Spinner } from '../components/Layout';
import { Hero } from './Home';

const FAQ: [string, string][] = [
  ['When does the course start and finish?', 'The course starts now and never ends! It is a completely self-paced online course – you decide when you start and when you finish.'],
  ['How long do I have access to the course?', 'After enrolling, you have unlimited access for as long as you like, across any and all devices you own.'],
  ['What if I am unhappy with the course?', 'Contact us within the first 14 days and we will give you a full refund. We only ask for an honest reason so we can improve.'],
  ['Is there a subscription?', 'Courses and bundles are one-time purchases that include all updates. An optional membership is also available if you prefer monthly or yearly access.'],
  ['Do the courses have labs?', 'Yes – we call them DEMO lessons. You build real environments in your own accounts, mostly within free tiers, with step-by-step instructions, code and troubleshooting notes.'],
  ['Are there any time restrictions?', 'No. It is self-paced training, so you can study at your own speed and rewatch lessons as many times as you need.'],
  ['What do the courses include?', 'Video lessons, architecture diagrams, DEMO lessons with lab files, quizzes, a practice exam and a verifiable certificate of completion.']
];

const icon = (t: string) => (t === 'VIDEO' ? '▶' : t === 'QUIZ' ? '✎' : '≡');

export default function CourseDetail({ slug }: { slug: string }) {
  const { user } = useAuth();
  const [cur] = useCurrency();
  const { data: d, error, loading, reload } = useLoad(() => api<Detail>('/api/courses/' + encodeURIComponent(slug)), [slug, user?.id]);
  const [checkout, setCheckout] = useState(false);
  const [openSecs, setOpenSecs] = useState<Record<number, boolean>>({});

  useEffect(() => { if (d) setMeta(d.seoTitle || d.card.title, d.seoDescription || d.card.subtitle); }, [d]);
  if (loading && !d) return <Spinner />;
  if (error || !d) return <div className="page"><ErrorBox message={error || 'Not found'} retry={reload} /></div>;

  const c = d.card;
  const isBundle = c.kind === 'BUNDLE';
  const price = priceOf(c, cur);
  const enroll = () => {
    if (d.hasAccess) return navigate(isBundle ? '/dashboard' : '/learn/' + c.slug);
    if (!user) { toast('Please sign up or log in to enroll'); return navigate('/signup?next=' + encodeURIComponent('/p/' + c.slug)); }
    setCheckout(true);
  };
  const label = d.hasAccess ? (isBundle ? 'Go to My Courses' : 'Go to Course')
    : price === 0 ? 'Enroll for Free' : `Enroll in ${isBundle ? 'Bundle' : 'Course'} for ${money(price, cur)}`;
  const Btn = () => <button className="btn" onClick={enroll}>{label}</button>;
  const openLesson = (id: number, preview: boolean) => {
    if (preview || d.hasAccess) navigate(`/learn/${c.slug}/${id}`); else enroll();
  };
  const sumBundle = d.bundleItems.reduce((t, i) => t + priceOf(i, cur), 0);

  return (
    <>
      <Hero title={c.title} subtitle={c.subtitle} compact><Btn /></Hero>
      <div className="prose">
        {c.status !== 'PUBLISHED' && <p className="warn">Preview – this course is {c.status.replace('_', ' ').toLowerCase()} and not visible to students.</p>}
        {isBundle ? (
          <>
            <h4>THIS BUNDLE INCLUDES {d.bundleItems.length} COURSES:</h4>
            <div className="includes">
              {d.bundleItems.map(i => <Link key={i.id} to={'/p/' + i.slug}><span>{i.title}</span><span>{money(priceOf(i, cur), cur)}</span></Link>)}
            </div>
            {sumBundle > price && <p>Buying separately costs <b>{money(sumBundle, cur)}</b>. With this bundle you pay <b>{money(price, cur)}</b> – you save <b>{money(sumBundle - price, cur)}</b>.</p>}
          </>
        ) : d.inBundles.length > 0 && (
          <>
            <h4>IF YOU ARE LOOKING FOR A BUNDLE WITH THIS COURSE IN :</h4>
            <p>{d.inBundles.map((b, i) => <span key={b.id}>{i > 0 && ' or '}<Link to={'/p/' + b.slug}>{b.title}</Link></span>)}</p>
          </>
        )}
        {(d.description || '').split(/\n\n+/).map((p, i) => <p key={i}>{p}</p>)}
        {d.previewVideoUrl && <video className="preview-video" src={d.previewVideoUrl} controls preload="metadata" />}
        {!isBundle && (
          <div className="facts">
            <span>📚 {c.lessonCount} lessons</span>
            {c.totalSeconds > 0 && <span>⏱ {duration(c.totalSeconds)} of video</span>}
            <span>📈 {c.level}</span>
            {c.reviewCount > 0 && <span>★ {c.rating.toFixed(1)} ({c.reviewCount} reviews)</span>}
            <span>🎓 Certificate of completion</span>
          </div>
        )}
        {d.objectives.length > 0 && <><h3>What you'll learn</h3><ul className="checks">{d.objectives.map(o => <li key={o}>{o}</li>)}</ul></>}
        {d.prerequisites.length > 0 && <><h3>Prerequisites</h3><ul>{d.prerequisites.map(o => <li key={o}>{o}</li>)}</ul></>}
        {d.audience && <><h3>Who this course is for</h3><p>{d.audience}</p></>}
      </div>

      {d.instructor && (
        <div className="grey-band">
          <div className="instructor">
            <div className="big-avatar">{d.instructor.name.split(' ').map(s => s[0]).join('')}</div>
            <div>
              <h2>Your Instructor</h2>
              <h5>{d.instructor.name}{d.instructor.headline ? ' · ' + d.instructor.headline : ''}</h5>
              {(d.instructor.bio || '').split('\n').filter(Boolean).map((p, i) => <p key={i}>{p}</p>)}
            </div>
          </div>
        </div>
      )}

      {!isBundle && d.sections.length > 0 && (
        <div className="curriculum-wrap">
          <h2>{d.hasAccess ? 'Course Curriculum' : 'Example Curriculum'}</h2>
          {d.sections.map((s, si) => {
            const open = openSecs[s.id] ?? si < 2;
            return (
              <div className="csection" key={s.id}>
                <div className="csection-head" onClick={() => setOpenSecs({ ...openSecs, [s.id]: !open })}>
                  <span>{s.title}</span><span>{s.lessons.length} lessons {open ? '▴' : '▾'}</span>
                </div>
                {open && s.lessons.map(l => (
                  <div className="lesson" key={l.id}>
                    <span className="lesson-left">
                      <span className="licon">{l.completed ? '✔' : icon(l.type)}</span>
                      {l.title}{l.durationSeconds > 0 && ` (${duration(l.durationSeconds)})`}
                    </span>
                    <button className={'ltag' + (l.preview && !d.hasAccess ? ' preview' : '')} onClick={() => openLesson(l.id, l.preview)}>
                      {l.preview && !d.hasAccess ? 'PREVIEW' : 'START'}
                    </button>
                  </div>
                ))}
              </div>
            );
          })}
        </div>
      )}

      {!isBundle && <Reviews d={d} onSaved={reload} />}

      <div className="grey-band">
        <div className="faq">
          <h2>❓ Frequently Asked Questions</h2>
          {FAQ.map(([q, a]) => <div key={q}><h4>{q}</h4><p>{a}</p></div>)}
        </div>
      </div>
      <div className="cta-band"><h3>Get started now!</h3><Btn /></div>

      {d.related.length > 0 && (
        <>
          <h2 className="section-title small">Related courses</h2>
          <div className="grid">{d.related.map(r => <CourseCard key={r.id} c={r} />)}</div>
          <div style={{ height: 40 }} />
        </>
      )}
      {checkout && <CheckoutModal courseSlug={c.slug} onClose={() => setCheckout(false)} />}
    </>
  );
}

function Reviews({ d, onSaved }: { d: Detail; onSaved: () => void }) {
  const [rating, setRating] = useState(5);
  const [comment, setComment] = useState('');
  const [busy, setBusy] = useState(false);
  const submit = async (e: FormEvent) => {
    e.preventDefault();
    setBusy(true);
    try {
      await api<Review>(`/api/courses/${d.card.slug}/reviews`, { body: { rating, comment } });
      toast('Thanks for your review!'); setComment(''); onSaved();
    } catch (err) { toast(errMsg(err), 'error'); } finally { setBusy(false); }
  };
  if (d.reviews.length === 0 && !d.canReview) return null;
  return (
    <div className="curriculum-wrap">
      <h2>Student reviews {d.card.reviewCount > 0 && `· ★ ${d.card.rating.toFixed(1)}`}</h2>
      {d.canReview && (
        <form className="form-card review-form" onSubmit={submit}>
          <div className="star-input">
            {[1, 2, 3, 4, 5].map(n => <button type="button" key={n} className={n <= rating ? 'on' : ''} onClick={() => setRating(n)} aria-label={n + ' stars'}>★</button>)}
          </div>
          <textarea placeholder="What did you think of this course?" value={comment} onChange={e => setComment(e.target.value)} maxLength={3000} />
          <button className="btn sq small" disabled={busy}>Submit review</button>
        </form>
      )}
      {d.reviews.map(r => (
        <div className="review" key={r.id}>
          <b>{r.userName}</b> <span className="stars">{'★'.repeat(r.rating)}{'☆'.repeat(5 - r.rating)}</span> <small>{fmtDate(r.createdAt)}</small>
          {r.comment && <p>{r.comment}</p>}
        </div>
      ))}
    </div>
  );
}
