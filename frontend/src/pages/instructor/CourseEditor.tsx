import { useEffect, useRef, useState, type DragEvent } from 'react';
import { api } from '../../api';
import { useAuth } from '../../auth';
import { Link, navigate } from '../../router';
import type { Category, CourseEdit, LessonEdit, QuestionEdit, SectionEdit, Student } from '../../types';
import { errMsg, fmtDate, setMeta, toast, useLoad } from '../../util';
import { ErrorBox, Spinner } from '../../components/Layout';

let keySeq = 0;
const key = () => 'k' + ++keySeq;
const withKeys = (c: CourseEdit): CourseEdit => ({
  ...c, sections: c.sections.map(s => ({ ...s, _key: key(), lessons: s.lessons.map(l => ({ ...l, _key: key() })) }))
});
const newLesson = (): LessonEdit => ({ _key: key(), title: 'New lesson', type: 'VIDEO', durationSeconds: 0, preview: false, randomize: false, questions: [], passPercent: 70, videoUrl: '', content: '', resources: '' });
const newQuestion = (): QuestionEdit => ({ prompt: '', type: 'SINGLE', options: 'Option A\nOption B\nOption C\nOption D', correct: '0', explanation: '' });

function Upload({ onDone, accept, label }: { onDone: (ref: string) => void; accept: string; label: string }) {
  const [busy, setBusy] = useState(false);
  const input = useRef<HTMLInputElement>(null);
  const pick = async (f: File) => {
    const fd = new FormData();
    fd.append('file', f);
    setBusy(true);
    try { const r = await api<{ ref: string }>('/api/instructor/uploads', { body: fd }); onDone(r.ref); toast('Uploaded ' + f.name); }
    catch (e) { toast(errMsg(e), 'error'); } finally { setBusy(false); }
  };
  return (
    <>
      <input ref={input} type="file" accept={accept} hidden onChange={e => e.target.files?.[0] && pick(e.target.files[0])} />
      <button type="button" className="btn ghost sq small" disabled={busy} onClick={() => input.current?.click()}>{busy ? 'Uploading…' : label}</button>
    </>
  );
}

export default function CourseEditor({ id }: { id: string }) {
  const { user } = useAuth();
  const [c, setC] = useState<CourseEdit | null>(null);
  const [tab, setTab] = useState('details');
  const [dirty, setDirty] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [openLesson, setOpenLesson] = useState<string | null>(null);
  const cats = useLoad(() => api<Category[]>('/api/categories'), []);
  const options = useLoad(() => api<{ id: number; title: string; kind: string }[]>('/api/instructor/course-options'), []);
  const drag = useRef<{ type: 'section' | 'lesson'; si: number; li?: number } | null>(null);

  useEffect(() => { api<CourseEdit>('/api/instructor/courses/' + id).then(x => setC(withKeys(x))).catch(e => setError(errMsg(e))); }, [id]);
  useEffect(() => { if (c) setMeta('Edit: ' + c.title); }, [c?.title]); // eslint-disable-line react-hooks/exhaustive-deps
  useEffect(() => {
    const warn = (e: BeforeUnloadEvent) => { if (dirty) { e.preventDefault(); e.returnValue = ''; } };
    window.addEventListener('beforeunload', warn);
    return () => window.removeEventListener('beforeunload', warn);
  }, [dirty]);

  if (error) return <div className="page"><ErrorBox message={error} /></div>;
  if (!c) return <Spinner />;
  const isAdmin = user?.role === 'ADMIN';
  const set = (patch: Partial<CourseEdit>) => { setC({ ...c, ...patch }); setDirty(true); };
  const setSections = (sections: SectionEdit[]) => set({ sections });
  const updSection = (si: number, patch: Partial<SectionEdit>) => setSections(c.sections.map((s, i) => (i === si ? { ...s, ...patch } : s)));
  const updLesson = (si: number, li: number, patch: Partial<LessonEdit>) =>
    updSection(si, { lessons: c.sections[si].lessons.map((l, i) => (i === li ? { ...l, ...patch } : l)) });

  const save = async () => {
    setBusy(true);
    try {
      const saved = await api<CourseEdit>('/api/instructor/courses/' + c.id, { method: 'PUT', body: c });
      setC(withKeys(saved)); setDirty(false); toast('Saved');
      return true;
    } catch (e) { toast(errMsg(e), 'error'); return false; } finally { setBusy(false); }
  };
  const submit = async () => {
    if (dirty && !(await save())) return;
    try { const r = await api<CourseEdit>(`/api/instructor/courses/${c.id}/submit`, { method: 'POST' }); setC(withKeys(r)); toast(r.status === 'PUBLISHED' ? 'Published!' : r.status === 'SCHEDULED' ? 'Scheduled' : 'Submitted for admin review'); }
    catch (e) { toast(errMsg(e), 'error'); }
  };
  const remove = async () => {
    if (!confirm('Delete this course permanently?')) return;
    try { await api('/api/instructor/courses/' + c.id, { method: 'DELETE' }); toast('Deleted'); navigate('/instructor'); } catch (e) { toast(errMsg(e), 'error'); }
  };

  // ---- drag and drop reordering ----
  const onDrop = (e: DragEvent, si: number, li?: number) => {
    e.preventDefault(); e.stopPropagation();
    const d = drag.current; drag.current = null;
    if (!d) return;
    const secs = c.sections.map(s => ({ ...s, lessons: [...s.lessons] }));
    if (d.type === 'section') {
      const [moved] = secs.splice(d.si, 1);
      secs.splice(si, 0, moved);
    } else if (d.li !== undefined) {
      const [moved] = secs[d.si].lessons.splice(d.li, 1);
      secs[si].lessons.splice(li ?? secs[si].lessons.length, 0, moved);
    }
    setSections(secs);
  };
  const allow = (e: DragEvent) => e.preventDefault();

  const tabs: [string, string][] = c.kind === 'BUNDLE'
    ? [['details', 'Details'], ['bundle', 'Bundle courses'], ['pricing', 'Pricing & publishing'], ['seo', 'SEO']]
    : [['details', 'Details'], ['curriculum', 'Curriculum'], ['pricing', 'Pricing & publishing'], ['seo', 'SEO'], ['students', 'Students'], ['announce', 'Announcement']];

  return (
    <div className="page wide-page editor">
      <div className="dash-head">
        <div>
          <Link to="/instructor">← Instructor Portal</Link>
          <h1>{c.title}</h1>
          <span className={'pill ' + c.status.toLowerCase()}>{c.status.replace('_', ' ')}</span> {c.instructorName && <small className="muted">by {c.instructorName}</small>}
        </div>
        <div className="actions">
          <Link to={'/p/' + c.slug} className="btn ghost sq small">Preview</Link>
          <button className="btn ghost sq small" disabled={busy || !dirty} onClick={save}>{dirty ? 'Save changes' : 'Saved'}</button>
          {c.status !== 'PUBLISHED' && <button className="btn sq small" onClick={submit}>{isAdmin ? 'Publish' : 'Submit for review'}</button>}
        </div>
      </div>
      <div className="tabs">{tabs.map(([k, l]) => <button key={k} className={tab === k ? 'on' : ''} onClick={() => setTab(k)}>{l}</button>)}</div>

      {tab === 'details' && (
        <div className="form-grid">
          <div className="field"><label>Title</label><input value={c.title} onChange={e => set({ title: e.target.value })} /></div>
          <div className="field"><label>URL slug (/p/…)</label><input value={c.slug} onChange={e => set({ slug: e.target.value })} /></div>
          <div className="field full"><label>Subtitle / short description</label><input value={c.subtitle || ''} onChange={e => set({ subtitle: e.target.value })} /></div>
          <div className="field"><label>Category</label>
            <select value={c.category || ''} onChange={e => set({ category: e.target.value })}>{cats.data?.map(x => <option key={x.id}>{x.name}</option>)}</select>
          </div>
          <div className="field"><label>Level</label>
            <select value={c.level || 'Beginner'} onChange={e => set({ level: e.target.value })}><option>Beginner</option><option>Intermediate</option><option>Advanced</option></select>
          </div>
          <div className="field full"><label>Description (blank line = new paragraph)</label><textarea rows={8} value={c.description || ''} onChange={e => set({ description: e.target.value })} /></div>
          <div className="field"><label>Learning objectives (one per line)</label><textarea rows={5} value={c.objectives || ''} onChange={e => set({ objectives: e.target.value })} /></div>
          <div className="field"><label>Prerequisites (one per line)</label><textarea rows={5} value={c.prerequisites || ''} onChange={e => set({ prerequisites: e.target.value })} /></div>
          <div className="field full"><label>Target audience</label><input value={c.audience || ''} onChange={e => set({ audience: e.target.value })} /></div>
          <div className="field"><label>Thumbnail image URL (optional – otherwise badges are drawn)</label><input value={c.thumbnailUrl || ''} onChange={e => set({ thumbnailUrl: e.target.value })} /></div>
          <div className="field"><label>Thumbnail badges: line1|line2|LEVEL|color; …</label><input value={c.badges || ''} onChange={e => set({ badges: e.target.value })} /></div>
          <div className="field"><label>Thumbnail theme</label><select value={c.theme || ''} onChange={e => set({ theme: e.target.value || null })}><option value="">Purple</option><option value="dark">Dark</option></select></div>
          <div className="field"><label>Promo video URL (shown on course page)</label><input value={c.previewVideoUrl || ''} onChange={e => set({ previewVideoUrl: e.target.value })} /></div>
          <label className="check"><input type="checkbox" checked={c.isNew} onChange={e => set({ isNew: e.target.checked })} /> Show "NEW UPDATE" ribbon</label>
          {isAdmin && <label className="check"><input type="checkbox" checked={c.featured} onChange={e => set({ featured: e.target.checked })} /> Featured on home page</label>}
        </div>
      )}

      {tab === 'curriculum' && (
        <div className="curr-editor">
          <p className="muted">Drag ☰ handles to reorder sections and lessons (you can drag lessons between sections). Click a lesson to edit it.</p>
          {c.sections.map((s, si) => (
            <div key={s._key} className="ed-section" draggable onDragStart={() => { drag.current = { type: 'section', si }; }} onDragOver={allow} onDrop={e => onDrop(e, si)}>
              <div className="ed-section-head">
                <span className="handle">☰</span>
                <input value={s.title} onChange={e => updSection(si, { title: e.target.value })} />
                <button className="link danger" onClick={() => confirm('Delete this section and its lessons?') && setSections(c.sections.filter((_, i) => i !== si))}>Delete</button>
              </div>
              {s.lessons.map((l, li) => (
                <div key={l._key} className="ed-lesson" draggable
                  onDragStart={e => { e.stopPropagation(); drag.current = { type: 'lesson', si, li }; }}
                  onDragOver={allow} onDrop={e => onDrop(e, si, li)}>
                  <div className="ed-lesson-row" onClick={() => setOpenLesson(openLesson === l._key ? null : l._key!)}>
                    <span className="handle">☰</span>
                    <span className="lt">{l.type === 'VIDEO' ? '▶' : l.type === 'QUIZ' ? '✎' : '≡'}</span>
                    <span className="grow">{l.title}</span>
                    {l.preview && <span className="pill published">PREVIEW</span>}
                    <span>{openLesson === l._key ? '▴' : '▾'}</span>
                  </div>
                  {openLesson === l._key && <LessonForm l={l} onChange={p => updLesson(si, li, p)} onDelete={() => updSection(si, { lessons: s.lessons.filter((_, i) => i !== li) })} />}
                </div>
              ))}
              <div className="ed-drop" onDragOver={allow} onDrop={e => onDrop(e, si, s.lessons.length)}>
                <button className="link" onClick={() => { const nl = newLesson(); updSection(si, { lessons: [...s.lessons, nl] }); setOpenLesson(nl._key!); }}>+ Add lesson</button>
              </div>
            </div>
          ))}
          <button className="btn ghost sq small" onClick={() => setSections([...c.sections, { _key: key(), title: 'New section', lessons: [] }])}>+ Add section</button>
        </div>
      )}

      {tab === 'bundle' && (
        <div>
          <p className="muted">Select the courses included in this bundle. Students who already bought some of them get credit automatically.</p>
          <div className="checklist">
            {options.data?.filter(o => o.kind === 'COURSE').map(o => (
              <label className="check" key={o.id}>
                <input type="checkbox" checked={c.bundleItemIds.includes(o.id)}
                  onChange={e => set({ bundleItemIds: e.target.checked ? [...c.bundleItemIds, o.id] : c.bundleItemIds.filter(x => x !== o.id) })} />
                {o.title}
              </label>
            ))}
          </div>
        </div>
      )}

      {tab === 'pricing' && (
        <div className="form-grid">
          <div className="field"><label>Price in INR (₹, GST inclusive; 0 = free)</label><input type="number" min={0} value={c.priceInr} onChange={e => set({ priceInr: Number(e.target.value) })} /></div>
          <div className="field"><label>Price in USD ($)</label><input type="number" min={0} value={c.priceUsd} onChange={e => set({ priceUsd: Number(e.target.value) })} /></div>
          <div className="field"><label>Original INR price (shows strike-through discount)</label><input type="number" min={0} value={c.compareAtInr ?? ''} onChange={e => set({ compareAtInr: e.target.value ? Number(e.target.value) : null })} /></div>
          <div className="field"><label>Schedule publication (optional)</label>
            <input type="datetime-local" value={c.scheduledAt ? c.scheduledAt.slice(0, 16) : ''} onChange={e => set({ scheduledAt: e.target.value ? new Date(e.target.value).toISOString() : null })} />
          </div>
          <div className="field"><label>Status</label>
            <select value={c.status} onChange={e => set({ status: e.target.value })}>
              <option value="DRAFT">Draft</option><option value="UNPUBLISHED">Unpublished</option>
              {isAdmin && <><option value="PENDING_REVIEW">Pending review</option><option value="SCHEDULED">Scheduled</option><option value="PUBLISHED">Published</option></>}
              {!isAdmin && c.status !== 'DRAFT' && c.status !== 'UNPUBLISHED' && <option value={c.status}>{c.status.replace('_', ' ')}</option>}
            </select>
          </div>
          <div className="full muted">Instructors submit courses for review; an administrator approves them before they go live. Scheduled courses publish automatically at the chosen time.</div>
          <div className="full"><button className="link danger" onClick={remove}>Delete this course</button></div>
        </div>
      )}

      {tab === 'seo' && (
        <div className="form-grid">
          <div className="field full"><label>SEO title</label><input maxLength={70} value={c.seoTitle || ''} onChange={e => set({ seoTitle: e.target.value })} /></div>
          <div className="field full"><label>Meta description (max 160 characters)</label><textarea maxLength={160} value={c.seoDescription || ''} onChange={e => set({ seoDescription: e.target.value })} /></div>
          <div className="full muted">Public URL: {location.origin}/p/{c.slug}</div>
        </div>
      )}

      {tab === 'students' && <Students id={c.id} />}
      {tab === 'announce' && <Announce id={c.id} />}
    </div>
  );
}

function LessonForm({ l, onChange, onDelete }: { l: LessonEdit; onChange: (p: Partial<LessonEdit>) => void; onDelete: () => void }) {
  const updQ = (qi: number, p: Partial<QuestionEdit>) => onChange({ questions: l.questions.map((q, i) => (i === qi ? { ...q, ...p } : q)) });
  return (
    <div className="lesson-form">
      <div className="form-grid">
        <div className="field"><label>Title</label><input value={l.title} onChange={e => onChange({ title: e.target.value })} /></div>
        <div className="field"><label>Type</label>
          <select value={l.type} onChange={e => onChange({ type: e.target.value })}><option value="VIDEO">Video</option><option value="TEXT">Text / lab</option><option value="QUIZ">Quiz</option></select>
        </div>
        {l.type === 'VIDEO' && <>
          <div className="field full"><label>Video (MP4/WebM upload, or YouTube/Vimeo/MP4 URL)</label>
            <div className="row"><input value={l.videoUrl || ''} onChange={e => onChange({ videoUrl: e.target.value })} placeholder="https://… or upload" />
              <Upload accept="video/*" label="Upload video" onDone={ref => onChange({ videoUrl: ref })} /></div>
          </div>
          <div className="field"><label>Duration (seconds)</label><input type="number" min={0} value={l.durationSeconds} onChange={e => onChange({ durationSeconds: Number(e.target.value) })} /></div>
          <div className="field"><label>Captions (.vtt)</label>
            <div className="row"><input value={l.captionsUrl || ''} onChange={e => onChange({ captionsUrl: e.target.value })} />
              <Upload accept=".vtt" label="Upload" onDone={ref => onChange({ captionsUrl: ref })} /></div>
          </div>
        </>}
        <div className="field full"><label>Lesson text: description, step-by-step instructions, ``` code blocks ```, expected output, troubleshooting</label>
          <textarea rows={7} value={l.content || ''} onChange={e => onChange({ content: e.target.value })} /></div>
        <div className="field full"><label>Resources, one per line: Label|URL (slides, GitHub repo, configs, scripts, lab ZIPs)</label>
          <div className="row"><textarea rows={3} value={l.resources || ''} onChange={e => onChange({ resources: e.target.value })} />
            <Upload accept="*/*" label="Upload file" onDone={ref => onChange({ resources: ((l.resources || '').trim() + '\nDownload|' + ref).trim() })} /></div>
        </div>
        <label className="check"><input type="checkbox" checked={l.preview} onChange={e => onChange({ preview: e.target.checked })} /> Free preview (visible without enrolling)</label>
      </div>
      {l.type === 'QUIZ' && (
        <div className="quiz-editor">
          <div className="form-grid">
            <div className="field"><label>Pass mark %</label><input type="number" min={0} max={100} value={l.passPercent ?? 70} onChange={e => onChange({ passPercent: Number(e.target.value) })} /></div>
            <div className="field"><label>Max attempts (blank = unlimited)</label><input type="number" min={1} value={l.maxAttempts ?? ''} onChange={e => onChange({ maxAttempts: e.target.value ? Number(e.target.value) : null })} /></div>
            <div className="field"><label>Time limit minutes (blank = none)</label><input type="number" min={1} value={l.timeLimitMinutes ?? ''} onChange={e => onChange({ timeLimitMinutes: e.target.value ? Number(e.target.value) : null })} /></div>
            <label className="check"><input type="checkbox" checked={l.randomize} onChange={e => onChange({ randomize: e.target.checked })} /> Randomise question order</label>
          </div>
          {l.questions.map((q, qi) => (
            <div className="q-edit" key={qi}>
              <div className="form-grid">
                <div className="field full"><label>Question {qi + 1}</label><textarea rows={2} value={q.prompt} onChange={e => updQ(qi, { prompt: e.target.value })} /></div>
                <div className="field"><label>Type</label>
                  <select value={q.type} onChange={e => updQ(qi, { type: e.target.value })}>
                    <option value="SINGLE">Multiple choice</option><option value="MULTI">Multiple select</option><option value="TRUE_FALSE">True / false</option><option value="FILL">Fill in the blank</option>
                  </select>
                </div>
                <div className="field"><label>{q.type === 'FILL' ? 'Accepted answers (separate with |)' : q.type === 'MULTI' ? 'Correct option numbers, from 0 (e.g. 0,2)' : q.type === 'TRUE_FALSE' ? '0 = True, 1 = False' : 'Correct option number, from 0'}</label>
                  <input value={q.correct} onChange={e => updQ(qi, { correct: e.target.value })} /></div>
                {(q.type === 'SINGLE' || q.type === 'MULTI') && <div className="field full"><label>Options (one per line)</label><textarea rows={4} value={q.options} onChange={e => updQ(qi, { options: e.target.value })} /></div>}
                <div className="field full"><label>Explanation (shown after submitting)</label><input value={q.explanation} onChange={e => updQ(qi, { explanation: e.target.value })} /></div>
              </div>
              <button className="link danger" onClick={() => onChange({ questions: l.questions.filter((_, i) => i !== qi) })}>Remove question</button>
            </div>
          ))}
          <button className="btn ghost sq small" onClick={() => onChange({ questions: [...l.questions, newQuestion()] })}>+ Add question</button>
        </div>
      )}
      <button className="link danger" onClick={onDelete}>Delete lesson</button>
    </div>
  );
}

function Students({ id }: { id: number }) {
  const { data, loading, error } = useLoad(() => api<Student[]>(`/api/instructor/courses/${id}/students`), [id]);
  if (loading) return <Spinner />;
  if (error) return <ErrorBox message={error} />;
  if (!data?.length) return <div className="empty">No students enrolled yet.</div>;
  const csv = () => {
    const rows = [['Name', 'Email', 'Enrolled', 'Source', 'Completed', 'Total', 'Last quiz %'], ...data.map(s => [s.name, s.email, s.enrolledAt, s.source, s.completed, s.total, s.lastQuizPercent ?? ''])];
    const blob = new Blob([rows.map(r => r.map(x => `"${String(x).replace(/"/g, '""')}"`).join(',')).join('\n')], { type: 'text/csv' });
    const a = document.createElement('a'); a.href = URL.createObjectURL(blob); a.download = 'students.csv'; a.click();
  };
  return (
    <>
      <button className="btn ghost sq small" onClick={csv}>Export CSV</button>
      <table className="table">
        <thead><tr><th>Student</th><th>Enrolled</th><th>Access</th><th>Progress</th><th>Last quiz</th></tr></thead>
        <tbody>{data.map(s => (
          <tr key={s.userId}><td>{s.name}<br /><small className="muted">{s.email}</small></td><td>{fmtDate(s.enrolledAt)}</td><td>{s.source}</td>
            <td><div className="progress mini"><i style={{ width: (s.total ? s.completed / s.total * 100 : 0) + '%' }} /></div>{s.completed}/{s.total}</td>
            <td>{s.lastQuizPercent != null ? s.lastQuizPercent + '%' : '—'}</td></tr>
        ))}</tbody>
      </table>
    </>
  );
}

function Announce({ id }: { id: number }) {
  const [title, setTitle] = useState('');
  const [body, setBody] = useState('');
  const send = async () => {
    if (!title || !body) return toast('Add a title and message', 'error');
    try { const r = await api<{ sent: number }>(`/api/instructor/courses/${id}/announce`, { body: { title, body } }); toast(`Sent to ${r.sent} students`); setTitle(''); setBody(''); }
    catch (e) { toast(errMsg(e), 'error'); }
  };
  return (
    <div className="form-card">
      <p className="muted">Sends an in-app notification and an email to every enrolled student (e.g. new lessons added).</p>
      <div className="field"><label>Title</label><input value={title} onChange={e => setTitle(e.target.value)} placeholder="New lessons added: EKS deep dive" /></div>
      <div className="field"><label>Message</label><textarea rows={5} value={body} onChange={e => setBody(e.target.value)} /></div>
      <button className="btn sq" onClick={send}>Send announcement</button>
    </div>
  );
}
