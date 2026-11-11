import { useEffect, useState, type ReactNode, type MouseEvent } from 'react';

/** Tiny dependency-free router using the History API (SEO-friendly URLs like /p/course-slug). */
export function navigate(to: string, replace = false) {
  if (replace) history.replaceState(null, '', to); else history.pushState(null, '', to);
  window.dispatchEvent(new Event('app:navigate'));
  if (!to.includes('#')) window.scrollTo(0, 0);
}

export function useLocation() {
  const get = () => ({ path: location.pathname, query: new URLSearchParams(location.search) });
  const [loc, setLoc] = useState(get);
  useEffect(() => {
    const update = () => setLoc(get());
    window.addEventListener('popstate', update);
    window.addEventListener('app:navigate', update);
    return () => { window.removeEventListener('popstate', update); window.removeEventListener('app:navigate', update); };
  }, []);
  return loc;
}

/** match('/p/:slug', '/p/aws') -> { slug: 'aws' } */
export function match(pattern: string, path: string): Record<string, string> | null {
  const p = pattern.split('/').filter(Boolean), a = path.split('/').filter(Boolean);
  if (p.length !== a.length) return null;
  const params: Record<string, string> = {};
  for (let i = 0; i < p.length; i++) {
    if (p[i].startsWith(':')) params[p[i].slice(1)] = decodeURIComponent(a[i]);
    else if (p[i] !== a[i]) return null;
  }
  return params;
}

export function Link({ to, children, className, onClick, title }: { to: string; children: ReactNode; className?: string; onClick?: () => void; title?: string }) {
  const handle = (e: MouseEvent<HTMLAnchorElement>) => {
    if (e.metaKey || e.ctrlKey || e.shiftKey || e.button !== 0) return;
    e.preventDefault();
    onClick?.();
    navigate(to);
  };
  return <a href={to} className={className} onClick={handle} title={title}>{children}</a>;
}
