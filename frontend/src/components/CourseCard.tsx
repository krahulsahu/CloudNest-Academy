import { Link } from '../router';
import type { CourseCard as Card } from '../types';
import { money, priceOf, useCurrency } from '../util';

const STRIPES = ['#38bdf8', '#a3e635', '#fb923c', '#f87171', '#e11d48', '#475569'];

export function Thumb({ c }: { c: Card }) {
  if (c.thumbnailUrl) return <div className="thumb img"><img src={c.thumbnailUrl} alt="" loading="lazy" /></div>;
  const dark = c.theme === 'dark';
  const badges = (c.badges || '').split(';').filter(Boolean).map(b => b.split('|'));
  const small = badges.length > 2;
  const sale = c.compareAtInr && c.compareAtInr > c.priceInr ? Math.round((1 - c.priceInr / c.compareAtInr) * 100) : 0;
  return (
    <div className={'thumb' + (dark ? ' dark' : '')}>
      {dark && <div className="stripes">{STRIPES.map(s => <i key={s} style={{ background: s }} />)}</div>}
      {sale > 0 ? <div className="saletag">-{sale}%</div> : c.isNew ? <div className="newtag"><b>NEW</b><span>UPDATE</span></div> : null}
      <div className="badges">
        {badges.map(([l1, l2, lvl, color], i) => (
          <div key={i} className={`hex ${color || 'blue'} ${small ? 'sm' : ''}`}>
            <span className="top">certified</span>
            <span className="l1">{l1}</span>
            {l2 && <span className="l2">{l2}</span>}
            <span className="lvl">{lvl}</span>
          </div>
        ))}
      </div>
      {!dark && <div className="logo-row"><i /><i /><i /><i /></div>}
    </div>
  );
}

export function CourseCard({ c }: { c: Card }) {
  const [cur] = useCurrency();
  const price = priceOf(c, cur);
  const initials = (c.instructorName || '').split(' ').map(s => s[0]).join('').slice(0, 2);
  return (
    <Link to={'/p/' + c.slug} className="card">
      <Thumb c={c} />
      <div className="card-body">
        <h3>{c.title}</h3>
        <p>{c.subtitle}</p>
        {c.reviewCount > 0 && <div className="stars">★ {c.rating.toFixed(1)} <span>({c.reviewCount})</span></div>}
        <div className="card-foot">
          {c.kind === 'BUNDLE'
            ? <span>{c.itemCount} Course Bundle</span>
            : <span className="author"><span className="avatar">{initials}</span>{c.instructorName}</span>}
          <span className="price">
            {cur === 'INR' && c.compareAtInr && c.compareAtInr > c.priceInr && <s>{money(c.compareAtInr, 'INR')}</s>}
            {money(price, cur)}
          </span>
        </div>
      </div>
    </Link>
  );
}
