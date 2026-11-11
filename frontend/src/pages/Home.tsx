import { useEffect, type ReactNode } from 'react';
import { api } from '../api';
import { Link } from '../router';
import type { CourseCard as Card, Page } from '../types';
import { setMeta, useLoad } from '../util';
import { CourseCard } from '../components/CourseCard';
import { ErrorBox, Spinner } from '../components/Layout';
import { BannerSlider } from '../components/BannerSlider';

export function Hero({ title, subtitle, children, compact }: { title: string; subtitle?: string; children?: ReactNode; compact?: boolean }) {
  return (
    <section className={'hero' + (compact ? ' course-hero' : '')}>
      <div className="rain">{Array.from({ length: 9 }).map((_, i) => <span key={i} />)}</div>
      <h1>{title}</h1>
      {subtitle && <p>{subtitle}</p>}
      {children}
    </section>
  );
}

export default function Home() {
  useEffect(() => setMeta('Cloud, AI and Technical Training', 'High quality cloud, AI, identity and engineering training. Always up to date.'), []);
  const { data, error, loading, reload } = useLoad(() => api<Page<Card>>('/api/courses?featured=true&size=60'), []);
  return (
    <>
      <BannerSlider />
      <h2 className="section-title">Featured Courses</h2>
      {loading && <Spinner />}
      {error && <ErrorBox message={error} retry={reload} />}
      <div className="grid">{data?.items.map(c => <CourseCard key={c.id} c={c} />)}</div>
      <div className="center view-all"><Link to="/courses" className="btn">View All Products</Link></div>
      <section className="why">
        <div><b>🎬 Learn by doing</b><p>Visual explanations followed by DEMO lessons you repeat in your own account.</p></div>
        <div><b>♾️ Lifetime access</b><p>Buy once, keep forever – including every future update.</p></div>
        <div><b>🎓 Verifiable certificates</b><p>Finish the course and pass the quizzes to earn a certificate anyone can verify.</p></div>
      </section>
    </>
  );
}
