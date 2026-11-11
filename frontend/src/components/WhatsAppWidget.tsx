import { useEffect, useRef, useState, type FormEvent, type MouseEvent } from 'react';
import { SITE } from '../config';
import { navigate } from '../router';

interface Msg { me: boolean; html: string; time: string }

const WA_ICON = (
  <svg viewBox="0 0 24 24" aria-hidden="true">
    <path d="M12 2.2a9.8 9.8 0 0 0-8.4 14.8L2.3 21.7l4.8-1.3A9.8 9.8 0 1 0 12 2.2z" fill="none" stroke="#fff" strokeWidth="1.9" />
    <path fill="#fff" d="M8.6 7.3c.3-.4.6-.4.9-.4h.5c.2 0 .4.1.5.4l.8 1.9c.1.2 0 .5-.1.7l-.6.7c-.1.2-.1.4 0 .5.6 1.1 1.5 2 2.6 2.6.2.1.4.1.5 0l.7-.8c.2-.2.4-.2.6-.1l1.9.9c.2.1.4.3.3.6v.5c-.1.6-.6 1.1-1.2 1.3-.6.2-1.5.2-3.2-.6-1.8-.9-3.3-2.4-4.1-4.2-.7-1.5-.6-2.7-.1-3.5z" />
  </svg>
);

/** Rules are checked in order; the first match wins. Links use data-nav so they open inside the app. */
const KB: { k: string[]; a: string }[] = [
  { k: ['sailpoint', 'isc', 'iga', 'identity'], a: "Our <b>SailPoint ISC Complete Training</b> covers identity profiles, sources, VA, correlation, lifecycle states, transforms and rules with a real-world joiner scenario. <a data-nav='/p/sailpoint-isc-complete'>View course</a>" },
  { k: ['aws', 'solutions architect', 'saa'], a: "For AWS, start with <b>Solutions Architect – Associate</b> or save with the <a data-nav='/p/aws-core-associate-bundle'>Core Associate Bundle</a>." },
  { k: ['azure', 'az-104', 'az104', 'az-400'], a: "We have AZ-104 and AZ-400 courses plus a DevOps Expert bundle. <a data-nav='/courses?category=Azure'>See Azure courses</a>" },
  { k: ['java', 'spring', 'microservice'], a: "See <a data-nav='/p/java-spring-microservices'>Java Spring Boot Microservices</a>." },
  { k: ['kubernetes', 'docker', 'devops'], a: "See <a data-nav='/p/kubernetes-devops'>Kubernetes & DevOps Engineer</a> and the AWS DevOps Bundle." },
  { k: ['price', 'cost', 'fee', 'fees', 'how much', 'discount', 'coupon'], a: "Courses start at ₹1,999 and bundles save up to 40%. Use code <b>LAUNCH50</b> for 50% off. <a data-nav='/courses'>All courses</a>" },
  { k: ['subscription', 'membership', 'monthly', 'yearly'], a: "Memberships give access to many courses for one price. <a data-nav='/subscribe'>See plans</a>" },
  { k: ['refund', 'money back'], a: 'Not happy? Request a refund within 14 days of purchase from My Courses → Orders.' },
  { k: ['access', 'lifetime', 'how long'], a: 'One-time purchases include lifetime access and all future updates, on any device.' },
  { k: ['demo', 'class', 'batch', 'live', 'schedule', 'timing'], a: 'We run free live demo classes every weekend. Tap <b>WhatsApp Us</b> below and the trainer will share the next slot.' },
  { k: ['certificate', 'certification', 'verify'], a: "Finish all lessons and pass the quizzes to get a verifiable PDF certificate. <a data-nav='/verify'>Verify a certificate</a>" },
  { k: ['login', 'otp', 'sign in', 'password', 'code'], a: "Use <a data-nav='/login'>Log In</a> – we email you a one-time code, or you can use your password." },
  { k: ['team', 'company', 'corporate', 'bulk', 'employees'], a: "We offer team licences with progress reports. <a data-nav='/teams'>Team Training</a>" },
  { k: ['hi', 'hello', 'hey', 'namaste'], a: 'Hello! 👋 Ask me about courses, prices, demo classes, certificates or refunds.' }
];
const QUICK = ['Course prices', 'SailPoint ISC course', 'Free demo class', 'Refund policy'];

const now = () => new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
const esc = (s: string) => s.replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c] as string));

function reply(text: string) {
  const lower = text.toLowerCase();
  const hit = KB.find(e => e.k.some(k => new RegExp('\\b' + k.replace(/[-]/g, '\\-') + '\\b').test(lower)));
  return hit ? hit.a : 'Thanks for your message! Our trainer will reply personally – tap <b>WhatsApp Us</b> below to continue on WhatsApp.';
}

export function WhatsAppWidget() {
  const cfg = SITE.whatsapp;
  const [open, setOpen] = useState(false);
  const [msgs, setMsgs] = useState<Msg[]>([]);
  const [typing, setTyping] = useState(false);
  const [showQuick, setShowQuick] = useState(true);
  const [text, setText] = useState('');
  const chat = useRef<HTMLDivElement>(null);

  const start = () => { if (msgs.length === 0) setMsgs([{ me: false, html: esc(cfg.greeting), time: now() }]); };
  const toggle = () => { if (!open) start(); setOpen(!open); };

  useEffect(() => {
    if (cfg.autoOpenSeconds <= 0 || sessionStorage.getItem('waShown')) return;
    const t = setTimeout(() => { sessionStorage.setItem('waShown', '1'); start(); setOpen(true); }, cfg.autoOpenSeconds * 1000);
    return () => clearTimeout(t);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);
  useEffect(() => { chat.current?.scrollTo(0, chat.current.scrollHeight); }, [msgs, typing]);

  const ask = (q: string) => {
    setShowQuick(false);
    setMsgs(m => [...m, { me: true, html: esc(q), time: now() }]);
    setTyping(true);
    setTimeout(() => { setTyping(false); setMsgs(m => [...m, { me: false, html: reply(q), time: now() }]); }, 700);
  };
  const submit = (e: FormEvent) => { e.preventDefault(); if (text.trim()) { ask(text.trim()); setText(''); } };

  const asked = msgs.filter(m => m.me).slice(-3).map(m => m.html.replace(/&[a-z#0-9]+;/g, ' ')).join(' | ');
  const waText = asked ? `Hi ${cfg.agentName}, I have a question: ${asked}` : `Hi ${cfg.agentName}, I would like to know more about your courses.`;
  const waLink = `https://wa.me/${cfg.phone}?text=${encodeURIComponent(waText)}`;

  const onChatClick = (e: MouseEvent) => {
    const a = (e.target as HTMLElement).closest('a[data-nav]');
    if (a) { e.preventDefault(); setOpen(false); navigate(a.getAttribute('data-nav') || '/'); }
  };

  return (
    <>
      <div className={'wa-pop' + (open ? ' open' : '')} role="dialog" aria-label="WhatsApp chat">
        <div className="wa-top">
          <div className="av">{cfg.agentName.split(' ').map(s => s[0]).join('')}</div>
          <div><h4>{cfg.agentName}</h4><small>{cfg.status}</small></div>
          <button className="close" onClick={() => setOpen(false)} aria-label="Close chat">×</button>
        </div>
        <div className="wa-chat" ref={chat} onClick={onChatClick}>
          {msgs.map((m, i) => (
            <div key={i} className={'msg ' + (m.me ? 'me' : 'bot')}>
              <span dangerouslySetInnerHTML={{ __html: m.html }} />
              <span className="time">{m.time}</span>
            </div>
          ))}
          {showQuick && msgs.length > 0 && (
            <div className="quick">{QUICK.map(q => <button key={q} type="button" onClick={() => ask(q)}>{q}</button>)}</div>
          )}
          {typing && <div className="msg bot typing"><i /><i /><i /></div>}
        </div>
        <form className="wa-input" onSubmit={submit}>
          <input value={text} onChange={e => setText(e.target.value)} placeholder="Type a message…" aria-label="Message" />
          <button aria-label="Send">➤</button>
        </form>
        <div className="wa-cta"><a href={waLink} target="_blank" rel="noopener noreferrer">{WA_ICON} WhatsApp Us</a></div>
        <div className="wa-foot">Online | <a href="/privacy" onClick={e => { e.preventDefault(); setOpen(false); navigate('/privacy'); }}>Privacy policy</a></div>
      </div>
      <button className="wa-fab" onClick={toggle} aria-label="Chat on WhatsApp">
        {WA_ICON}
        {!open && msgs.length === 0 && <span className="dot">1</span>}
      </button>
    </>
  );
}
