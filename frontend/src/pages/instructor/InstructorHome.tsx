import { useEffect, useState, type FormEvent } from 'react';
import { api } from '../../api';
import { Link, navigate } from '../../router';
import type { CourseEdit, InstructorCourse } from '../../types';
import { errMsg, money, setMeta, toast, useLoad } from '../../util';
import { ErrorBox, Modal, Spinner } from '../../components/Layout';

export default function InstructorHome() {
  useEffect(() => setMeta('Instructor Portal'), []);
  const { data, loading, error, reload } = useLoad(() => api<InstructorCourse[]>('/api/instructor/courses'), []);
  const [creating, setCreating] = useState(false);
  const [title, setTitle] = useState('');
  const [kind, setKind] = useState('COURSE');
  const create = async (e: FormEvent) => {
    e.preventDefault();
    try { const c = await api<CourseEdit>('/api/instructor/courses', { body: { title, kind } }); toast('Draft created'); navigate('/instructor/courses/' + c.id); }
    catch (err) { toast(errMsg(err), 'error'); }
  };
  const totals = (data || []).reduce((t, c) => ({ students: t.students + c.students, inr: t.inr + c.revenueInr, usd: t.usd + c.revenueUsd }), { students: 0, inr: 0, usd: 0 });
  return (
    <div className="page wide-page">
      <div className="dash-head">
        <div><h1>Instructor Portal</h1><p className="lead">Create courses, manage curriculum, track students and revenue.</p></div>
        <button className="btn sq small" onClick={() => setCreating(true)}>+ New course or bundle</button>
      </div>
      <div className="stats">
        <div><b>{data?.length ?? '–'}</b><span>Courses</span></div>
        <div><b>{totals.students}</b><span>Enrolled students</span></div>
        <div><b>{money(totals.inr, 'INR') === 'Free' ? '₹0' : money(totals.inr, 'INR')}</b><span>Revenue (INR)</span></div>
        <div><b>{totals.usd ? money(totals.usd, 'USD') : '$0'}</b><span>Revenue (USD)</span></div>
      </div>
      {loading && <Spinner />}
      {error && <ErrorBox message={error} retry={reload} />}
      <table className="table">
        <thead><tr><th>Course</th><th>Status</th><th>Students</th><th>Rating</th><th>Revenue</th><th>Certificates</th><th /></tr></thead>
        <tbody>
          {data?.map(({ card: c, students, revenueInr, revenueUsd, certificates }) => (
            <tr key={c.id}>
              <td><b>{c.title}</b><br /><small className="muted">{c.kind === 'BUNDLE' ? `Bundle · ${c.itemCount} courses` : `${c.lessonCount} lessons`} · {c.category}{c.instructorName ? ' · ' + c.instructorName : ''}</small></td>
              <td><span className={'pill ' + c.status.toLowerCase()}>{c.status.replace('_', ' ')}</span></td>
              <td>{students}</td>
              <td>{c.reviewCount ? `★ ${c.rating.toFixed(1)} (${c.reviewCount})` : '—'}</td>
              <td>{revenueInr ? money(revenueInr, 'INR') : '₹0'}{revenueUsd > 0 && <><br />{money(revenueUsd, 'USD')}</>}</td>
              <td>{certificates}</td>
              <td className="nowrap"><Link to={'/instructor/courses/' + c.id}>Edit</Link> · <Link to={'/p/' + c.slug}>View</Link></td>
            </tr>
          ))}
        </tbody>
      </table>
      {creating && (
        <Modal title="New course" onClose={() => setCreating(false)}>
          <form onSubmit={create}>
            <div className="field"><label>Title</label><input required autoFocus value={title} onChange={e => setTitle(e.target.value)} placeholder="e.g. Terraform on AWS – Complete Guide" /></div>
            <div className="field"><label>Type</label>
              <select value={kind} onChange={e => setKind(e.target.value)}><option value="COURSE">Course</option><option value="BUNDLE">Bundle of courses</option></select>
            </div>
            <button className="btn sq block">Create draft</button>
          </form>
        </Modal>
      )}
    </div>
  );
}
