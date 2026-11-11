import { useEffect, useState } from 'react';
import { api } from '../api';
import { navigate } from '../router';
import type { CheckoutResponse, Order, Quote } from '../types';
import { errMsg, money, toast, useCurrency } from '../util';
import { Modal, Spinner } from './Layout';

declare global {
  interface Window { Razorpay?: new (opts: Record<string, unknown>) => { open: () => void; on: (ev: string, cb: (r: unknown) => void) => void } }
}

function loadRazorpay(): Promise<void> {
  if (window.Razorpay) return Promise.resolve();
  return new Promise((resolve, reject) => {
    const s = document.createElement('script');
    s.src = 'https://checkout.razorpay.com/v1/checkout.js';
    s.onload = () => resolve();
    s.onerror = () => reject(new Error('Could not load Razorpay. Check your internet connection.'));
    document.body.appendChild(s);
  });
}

/** Checkout for a course/bundle (courseSlug) or a membership plan (planCode). Prices are always calculated by the server. */
export function CheckoutModal({ courseSlug, planCode, onClose }: { courseSlug?: string; planCode?: string; onClose: () => void }) {
  const [cur, setCur] = useCurrency();
  const [coupon, setCoupon] = useState('');
  const [applied, setApplied] = useState('');
  const [quote, setQuote] = useState<Quote | null>(null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const [mock, setMock] = useState<CheckoutResponse | null>(null);

  useEffect(() => {
    setError('');
    api<Quote>('/api/checkout/quote', { body: { courseSlug, planCode, coupon: applied, currency: cur } })
      .then(setQuote).catch(e => setError(errMsg(e)));
  }, [courseSlug, planCode, applied, cur]);

  const done = (redirect: string) => {
    toast('🎉 Payment successful – you are enrolled!');
    window.dispatchEvent(new Event('app:notifications'));
    onClose();
    navigate(redirect);
  };

  const pay = async () => {
    setBusy(true); setError('');
    try {
      const r = await api<CheckoutResponse>('/api/checkout', { body: { courseSlug, planCode, coupon: applied, currency: cur } });
      if (r.completed) return done(r.redirect);
      if (r.provider === 'MOCK') { setMock(r); setBusy(false); return; }
      if (r.provider === 'STRIPE' && r.stripeUrl) { window.location.href = r.stripeUrl; return; }
      if (r.provider === 'RAZORPAY') {
        await loadRazorpay();
        const rzp = new window.Razorpay!({
          key: r.razorpayKeyId, amount: r.total * 100, currency: r.currency, order_id: r.razorpayOrderId,
          name: 'CloudNest Academy', description: r.description, prefill: { name: r.userName, email: r.userEmail },
          theme: { color: '#5c1d7f' },
          handler: async (resp: { razorpay_order_id: string; razorpay_payment_id: string; razorpay_signature: string }) => {
            try {
              await api<Order>('/api/checkout/razorpay/verify', {
                body: { orderId: r.orderId, razorpayOrderId: resp.razorpay_order_id, razorpayPaymentId: resp.razorpay_payment_id, razorpaySignature: resp.razorpay_signature }
              });
              done(r.redirect);
            } catch (e) { setError(errMsg(e)); setBusy(false); }
          },
          modal: { ondismiss: () => setBusy(false) }
        });
        rzp.on('payment.failed', () => { setError('Payment failed. No money was taken – please try again.'); setBusy(false); });
        rzp.open();
      }
    } catch (e) { setError(errMsg(e)); setBusy(false); }
  };

  const confirmMock = async () => {
    if (!mock) return;
    setBusy(true);
    try { await api<Order>(`/api/checkout/mock/${mock.orderId}/confirm`, { method: 'POST' }); done(mock.redirect); }
    catch (e) { setError(errMsg(e)); setBusy(false); }
  };

  return (
    <Modal title="Checkout" onClose={onClose}>
      {!quote && !error && <Spinner />}
      {quote && (
        <>
          <p className="muted">{quote.itemName}</p>
          <div className="seg">
            <button className={cur === 'INR' ? 'on' : ''} onClick={() => setCur('INR')}>₹ INR · Razorpay</button>
            <button className={cur === 'USD' ? 'on' : ''} onClick={() => setCur('USD')}>$ USD · Stripe</button>
          </div>
          <div className="sumrow"><span>Price</span><span>{money(quote.listPrice, quote.currency)}</span></div>
          {quote.credit > 0 && <div className="sumrow good"><span>Upgrade credit (courses you own)</span><span>-{money(quote.credit, quote.currency)}</span></div>}
          {quote.discount > 0 && <div className="sumrow good"><span>Coupon</span><span>-{money(quote.discount, quote.currency)}</span></div>}
          <div className="sumrow total"><span>Total</span><span>{money(quote.total, quote.currency)}</span></div>
          {quote.currency === 'INR' && quote.total > 0 && <small className="muted">Inclusive of GST</small>}
          <div className="coupon-row">
            <input placeholder="Coupon code" value={coupon} onChange={e => setCoupon(e.target.value)} />
            <button className="btn ghost sq small" onClick={() => setApplied(coupon.trim())}>Apply</button>
          </div>
          {quote.couponMessage && <p className={quote.discount > 0 ? 'ok-text' : 'error'}>{quote.couponMessage}</p>}
        </>
      )}
      {error && <p className="error">{error}</p>}
      {mock ? (
        <div className="mockbox">
          <b>Development payment mode</b>
          <p>Razorpay/Stripe keys are not configured, so this simulates a successful payment. The server still verifies and creates the enrollment.</p>
          <button className="btn sq block" disabled={busy} onClick={confirmMock}>Simulate successful payment</button>
        </div>
      ) : (
        quote && <button className="btn sq block" disabled={busy} onClick={pay}>{busy ? 'Processing…' : quote.total === 0 ? 'Enroll for free' : `Pay ${money(quote.total, quote.currency)}`}</button>
      )}
      <p className="tiny">Secure payment. 14-day refund policy. Access is granted only after the payment is verified.</p>
    </Modal>
  );
}
