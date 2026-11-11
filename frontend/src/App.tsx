import { useEffect } from 'react';
import { AuthProvider, useAuth } from './auth';
import { match, navigate, useLocation } from './router';
import { Footer, Header, Spinner, Toaster } from './components/Layout';
import { WhatsAppWidget } from './components/WhatsAppWidget';
import Home from './pages/Home';
import Catalog from './pages/Catalog';
import CourseDetail from './pages/CourseDetail';
import Learn from './pages/Learn';
import Dashboard from './pages/Dashboard';
import { ForgotPassword, Login, Signup } from './pages/Auth';
import { CheckoutSuccess, ContactForm, Faq, Legal, NotFound, Roadmap, Subscribe, Verify } from './pages/Static';
import InstructorHome from './pages/instructor/InstructorHome';
import CourseEditor from './pages/instructor/CourseEditor';
import AdminDashboard from './pages/admin/AdminDashboard';

type Role = 'STUDENT' | 'INSTRUCTOR' | 'ADMIN';

function Guard({ roles, children }: { roles?: Role[]; children: JSX.Element }) {
  const { user, ready } = useAuth();
  const { path, query } = useLocation();
  useEffect(() => {
    if (ready && !user) navigate('/login?next=' + encodeURIComponent(path + (query.toString() ? '?' + query : '')), true);
  }, [ready, user, path, query]);
  if (!ready) return <Spinner />;
  if (!user) return null;
  if (roles && !roles.includes(user.role)) return <div className="page center"><h1>Access denied</h1><p className="lead">You don't have permission to view this page.</p></div>;
  return children;
}

function Routes() {
  const { path } = useLocation();
  const { ready } = useAuth();
  let m: Record<string, string> | null;
  if (path === '/') return <Home />;
  if (path === '/courses') return <Catalog />;
  if (path === '/offers') return <Catalog preset={{ kind: 'BUNDLE', title: 'Launch Offer – 50% off bundles', lead: 'Use code LAUNCH50 at checkout for 50% off any bundle or course.' }} />;
  if ((m = match('/p/:slug', path))) return ready ? <CourseDetail slug={m.slug} /> : <Spinner />;
  if ((m = match('/learn/:slug', path))) return ready ? <Learn slug={m.slug} /> : <Spinner />;
  if ((m = match('/learn/:slug/:lessonId', path))) return ready ? <Learn slug={m.slug} lessonId={m.lessonId} /> : <Spinner />;
  if (path === '/login') return <Login />;
  if (path === '/signup') return <Signup />;
  if (path === '/forgot-password') return <ForgotPassword />;
  if (path === '/dashboard') return <Guard><Dashboard /></Guard>;
  if (path === '/checkout/success') return <Guard><CheckoutSuccess /></Guard>;
  if (path === '/subscribe') return <Subscribe />;
  if (path === '/roadmap') return <Roadmap />;
  if (path === '/teams') return <ContactForm type="TEAM" title="Team Training" lead="Train your whole team with bulk licences, corporate invoices and progress reports." />;
  if (path === '/contact') return <ContactForm type="CONTACT" title="Contact us" lead="Questions about courses, billing or your account? Send us a message or use the WhatsApp button." />;
  if (path === '/faq') return <Faq />;
  if (path === '/terms') return <Legal kind="terms" />;
  if (path === '/privacy') return <Legal kind="privacy" />;
  if (path === '/verify') return <Verify />;
  if ((m = match('/verify/:code', path))) return <Verify key={m.code} code={m.code} />;
  if (path === '/instructor') return <Guard roles={['INSTRUCTOR', 'ADMIN']}><InstructorHome /></Guard>;
  if ((m = match('/instructor/courses/:id', path))) return <Guard roles={['INSTRUCTOR', 'ADMIN']}><CourseEditor key={m.id} id={m.id} /></Guard>;
  if (path === '/admin') return <Guard roles={['ADMIN']}><AdminDashboard /></Guard>;
  return <NotFound />;
}

export default function App() {
  const { path } = useLocation();
  const isPlayer = path.startsWith('/learn/');
  return (
    <AuthProvider>
      <Header />
      <main>
        <Routes />
      </main>
      {!isPlayer && <Footer />}
      <WhatsAppWidget />
      <Toaster />
    </AuthProvider>
  );
}
