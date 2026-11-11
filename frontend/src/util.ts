import { useEffect, useState } from 'react';
import { SITE } from './config';

export type Currency = 'INR' | 'USD';

export function money(amount: number, currency: string) {
  if (amount === 0) return 'Free';
  return currency === 'USD' ? '$' + amount.toLocaleString('en-US') : '₹' + amount.toLocaleString('en-IN');
}

export function duration(seconds: number) {
  if (!seconds) return '';
  const h = Math.floor(seconds / 3600), m = Math.floor((seconds % 3600) / 60), s = seconds % 60;
  return h > 0 ? `${h}h ${m}m` : `${m}:${String(s).padStart(2, '0')}`;
}

export const fmtDate = (s?: string | null) => (s ? new Date(s).toLocaleDateString(undefined, { day: 'numeric', month: 'short', year: 'numeric' }) : '—');
export const fmtDateTime = (s?: string | null) => (s ? new Date(s).toLocaleString() : '—');

export function setMeta(title: string, description?: string) {
  document.title = title ? `${title} | ${SITE.name}` : SITE.name;
  if (description) {
    let m = document.querySelector('meta[name="description"]');
    if (!m) { m = document.createElement('meta'); m.setAttribute('name', 'description'); document.head.appendChild(m); }
    m.setAttribute('content', description);
  }
}

export function toast(message: string, kind: 'ok' | 'error' = 'ok') {
  window.dispatchEvent(new CustomEvent('app:toast', { detail: { message, kind } }));
}

export function errMsg(e: unknown) { return e instanceof Error ? e.message : String(e); }

/** Loads data and exposes {data, error, loading, reload}. */
export function useLoad<T>(loader: () => Promise<T>, deps: unknown[]) {
  const [state, setState] = useState<{ data: T | null; error: string | null; loading: boolean }>({ data: null, error: null, loading: true });
  const [n, setN] = useState(0);
  useEffect(() => {
    let alive = true;
    setState(s => ({ ...s, loading: true, error: null }));
    loader().then(d => alive && setState({ data: d, error: null, loading: false }))
      .catch(e => alive && setState({ data: null, error: errMsg(e), loading: false }));
    return () => { alive = false; };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...deps, n]);
  return { ...state, reload: () => setN(x => x + 1) };
}

export function useCurrency(): [Currency, (c: Currency) => void] {
  const [c, setC] = useState<Currency>(() => (localStorage.getItem('cn_currency') as Currency) || 'INR');
  useEffect(() => {
    const f = () => setC((localStorage.getItem('cn_currency') as Currency) || 'INR');
    window.addEventListener('app:currency', f);
    return () => window.removeEventListener('app:currency', f);
  }, []);
  return [c, (v: Currency) => { localStorage.setItem('cn_currency', v); window.dispatchEvent(new Event('app:currency')); }];
}

export const priceOf = (c: { priceInr: number; priceUsd: number }, cur: Currency) => (cur === 'USD' ? c.priceUsd : c.priceInr);
