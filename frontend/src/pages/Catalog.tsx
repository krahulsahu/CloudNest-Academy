import { useEffect, useState } from 'react';
import { api } from '../api';
import { Link, navigate, useLocation } from '../router';
import type { Category, CourseCard as Card, Page } from '../types';
import { setMeta, useLoad } from '../util';
import { CourseCard } from '../components/CourseCard';
import { ErrorBox, Spinner } from '../components/Layout';

const PAGE_SIZE = 12;

export default function Catalog({ preset }: { preset?: { kind?: string; title?: string; lead?: string } }) {
  const { query } = useLocation();
  const category = query.get('category') || '';
  const page = Number(query.get('page') || 0);
  const [q, setQ] = useState(query.get('q') || '');
  const [kind, setKind] = useState(preset?.kind || query.get('kind') || '');
  const [level, setLevel] = useState(query.get('level') || '');
  const [price, setPrice] = useState(query.get('price') || '');
  const [sort, setSort] = useState(query.get('sort') || '');
  const [debounced, setDebounced] = useState(q);

  useEffect(() => { const t = setTimeout(() => setDebounced(q), 300); return () => clearTimeout(t); }, [q]);
  useEffect(() => setMeta(preset?.title || (category ? category + ' Courses' : 'All Courses'), 'Browse every course and bundle.'), [category, preset]);

  const cats = useLoad(() => api<Category[]>('/api/categories'), []);
  const params = new URLSearchParams({ q: debounced, category, kind, level, price, sort, page: String(page), size: String(PAGE_SIZE) });
  const list = useLoad(() => api<Page<Card>>('/api/courses?' + params.toString()), [params.toString()]);
  const pages = list.data ? Math.ceil(list.data.total / PAGE_SIZE) : 0;
  const go = (p: number) => { const u = new URLSearchParams(query); u.set('page', String(p)); navigate(location.pathname + '?' + u.toString()); };

  return (
    <div className="page">
      <h1>{preset?.title || 'All Courses'}</h1>
      <p className="lead">{preset?.lead || 'Browse every course and bundle. Lifetime access, self paced, always up to date.'}</p>
      <div className="chips">
        <Link to="/courses" className={'chip' + (!category ? ' active' : '')}>All</Link>
        {cats.data?.map(c => (
          <Link key={c.id} to={'/courses?category=' + encodeURIComponent(c.name)} className={'chip' + (category === c.name ? ' active' : '')}>{c.name}</Link>
        ))}
      </div>
      <div className="filters">
        <input value={q} onChange={e => setQ(e.target.value)} placeholder="Search courses, technologies, keywords…" aria-label="Search" />
        {!preset?.kind && (
          <select value={kind} onChange={e => setKind(e.target.value)} aria-label="Type">
            <option value="">Courses &amp; bundles</option><option value="COURSE">Courses</option><option value="BUNDLE">Bundles</option>
          </select>
        )}
        <select value={level} onChange={e => setLevel(e.target.value)} aria-label="Level">
          <option value="">All levels</option><option>Beginner</option><option>Intermediate</option><option>Advanced</option>
        </select>
        <select value={price} onChange={e => setPrice(e.target.value)} aria-label="Price">
          <option value="">Free &amp; paid</option><option value="free">Free</option><option value="paid">Paid</option>
        </select>
        <select value={sort} onChange={e => setSort(e.target.value)} aria-label="Sort">
          <option value="">Sort: Featured</option><option value="popular">Most popular</option><option value="rating">Highest rated</option>
          <option value="newest">Newest</option><option value="price_asc">Price: low to high</option><option value="price_desc">Price: high to low</option>
        </select>
      </div>
      {list.data && <p className="lead small">{list.data.total} products</p>}
      {list.loading && <Spinner />}
      {list.error && <ErrorBox message={list.error} retry={list.reload} />}
      {list.data && list.data.items.length === 0 && <div className="empty">No courses match your filters.</div>}
      <div className="grid flush">{list.data?.items.map(c => <CourseCard key={c.id} c={c} />)}</div>
      {pages > 1 && (
        <div className="pager">
          <button disabled={page === 0} onClick={() => go(page - 1)}>← Prev</button>
          <span>Page {page + 1} of {pages}</span>
          <button disabled={page >= pages - 1} onClick={() => go(page + 1)}>Next →</button>
        </div>
      )}
    </div>
  );
}
