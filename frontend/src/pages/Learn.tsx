import { useEffect, useRef, useState } from 'react';
import { api, download } from '../api';
import { useAuth } from '../auth';
import { Link, navigate } from '../router';
import type { Certificate, LearnCourse, Lesson, QuizResult } from '../types';
import { duration, errMsg, setMeta, toast, useLoad } from '../util';
import { ErrorBox, Spinner } from '../components/Layout';

/** Minimal safe renderer: paragraphs, "- " lists, numbered lists and ``` code blocks. No raw HTML. */
export function RichText({ text }: { text?: string | null }) {
  if (!text) return null;
  const blocks = text.split(/```/);
  return (
    <div className="rich">
      {blocks.map((b, i) => {
        if (i % 2 === 1) {
          const code = b.replace(/^[a-z]*\n/, '');
          return (
            <div className="code" key={i}>
              <button onClick={() => { navigator.clipboard?.writeText(code); toast('Copied'); }}>Copy</button>
              <pre><code>{code}</code></pre>
            </div>
          );
        }
        return b.split(/\n\n+/).filter(p => p.trim()).map((p, j) => {
          const lines = p.split('\n');
          if (lines.every(l => /^\s*[-*] /.test(l))) return <ul key={i + '-' + j}>{lines.map((l, k) => <li key={k}>{l.replace(/^\s*[-*] /, '')}</li>)}</ul>;
          if (lines.every(l => /^\s*\d+\. /.test(l))) return <ol key={i + '-' + j}>{lines.map((l, k) => <li key={k}>{l.replace(/^\s*\d+\. /, '')}</li>)}</ol>;
          return <p key={i + '-' + j}>{lines.map((l, k) => <span key={k}>{l}{k < lines.length - 1 && <br />}</span>)}</p>;
        });
      })}
    </div>
  );
}

function youTubeId(url: string) {
  const m = url.match(/(?:youtube\.com\/watch\?v=|youtu\.be\/|youtube\.com\/embed\/)([\w-]{11})/);
  return m ? m[1] : null;
}
function vimeoId(url: string) {
  const m = url.match(/vimeo\.com\/(?:video\/)?(\d+)/);
  return m ? m[1] : null;
}

function VideoPlayer({ lesson, canSave }: { lesson: Lesson; canSave: boolean }) {
  const ref = useRef<HTMLVideoElement>(null);
  const lastSaved = useRef(0);
  const [speed, setSpeed] = useState(() => Number(localStorage.getItem('cn_speed') || 1));
  const url = lesson.videoUrl || '';
  const yt = youTubeId(url), vm = vimeoId(url);

  useEffect(() => { if (ref.current) ref.current.playbackRate = speed; localStorage.setItem('cn_speed', String(speed)); }, [speed]);

  const save = (force = false) => {
    const v = ref.current;
    if (!v || !canSave) return;
    const pos = Math.floor(v.currentTime);
    if (!force && Math.abs(pos - lastSaved.current) < 10) return;
    lastSaved.current = pos;
    api(`/api/learn/lessons/${lesson.id}/progress`, { method: 'PUT', body: { positionSeconds: pos } }).catch(() => undefined);
  };
  useEffect(() => () => save(true), // save position when leaving the lesson
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [lesson.id]);

  if (!url) return <div className="video empty-video">Video coming soon</div>;
  if (yt) return <div className="video"><iframe src={`https://www.youtube-nocookie.com/embed/${yt}?rel=0`} title={lesson.title} allow="accelerometer; autoplay; encrypted-media; picture-in-picture; fullscreen" allowFullScreen /></div>;
  if (vm) return <div className="video"><iframe src={`https://player.vimeo.com/video/${vm}`} title={lesson.title} allow="autoplay; fullscreen; picture-in-picture" allowFullScreen /></div>;
  return (
    <div>
      <div className="video">
        <video
          ref={ref}
          key={lesson.id}
          src={url}
          controls
          controlsList="nodownload"
          playsInline
          preload="metadata"
          onContextMenu={e => e.preventDefault()}
          onLoadedMetadata={e => {
            const v = e.currentTarget;
            v.playbackRate = speed;
            if (lesson.positionSeconds > 5 && lesson.positionSeconds < v.duration - 5) {
              v.currentTime = lesson.positionSeconds;
              toast('Resumed from ' + duration(lesson.positionSeconds));
            }
          }}
          onTimeUpdate={() => save()}
          onPause={() => save(true)}
        >
          {lesson.captionsUrl && <track kind="captions" src={lesson.captionsUrl} srcLang="en" label="English" default />}
        </video>
      </div>
      <div className="speed">
        Speed:
        {[0.75, 1, 1.25, 1.5, 1.75, 2].map(s => <button key={s} className={s === speed ? 'on' : ''} onClick={() => setSpeed(s)}>{s}x</button>)}
      </div>
    </div>
  );
}

function Quiz({ lesson, onPassed }: { lesson: Lesson; onPassed: () => void }) {
  const [answers, setAnswers] = useState<Record<number, string>>({});
  const [result, setResult] = useState<QuizResult | null>(null);
  const [busy, setBusy] = useState(false);
  const [started, setStarted] = useState(!lesson.timeLimitMinutes);
  const [left, setLeft] = useState((lesson.timeLimitMinutes || 0) * 60);
  const submitted = useRef(false);

  const submit = async () => {
    if (submitted.current) return;
    submitted.current = true;
    setBusy(true);
    try {
      const r = await api<QuizResult>(`/api/learn/lessons/${lesson.id}/quiz`, { body: { answers } });
      setResult(r);
      if (r.passed) { toast(`✅ Passed with ${r.percent}%`); onPassed(); } else toast(`Score ${r.percent}% – you need ${lesson.passPercent}% to pass`, 'error');
    } catch (e) { toast(errMsg(e), 'error'); submitted.current = false; } finally { setBusy(false); }
  };

  useEffect(() => {
    if (!started || !lesson.timeLimitMinutes || result) return;
    if (left <= 0) { toast('Time is up – submitting your answers'); submit(); return; }
    const t = setTimeout(() => setLeft(l => l - 1), 1000);
    return () => clearTimeout(t);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [started, left, result]);

  const set = (qid: number, v: string) => setAnswers(a => ({ ...a, [qid]: v }));
  const toggleMulti = (qid: number, idx: number) => {
    const cur = new Set((answers[qid] || '').split(',').filter(Boolean));
    if (cur.has(String(idx))) cur.delete(String(idx)); else cur.add(String(idx));
    set(qid, [...cur].sort().join(','));
  };
  const retry = () => { setResult(null); setAnswers({}); submitted.current = false; setLeft((lesson.timeLimitMinutes || 0) * 60); setStarted(!lesson.timeLimitMinutes); };
  const attemptsLeft = lesson.maxAttempts ? lesson.maxAttempts - lesson.attemptsUsed - (result ? 1 : 0) : null;

  if (!started) return (
    <div className="form-card wide">
      <h3>Timed quiz</h3>
      <p>{lesson.questions.length} questions · {lesson.timeLimitMinutes} minutes · pass mark {lesson.passPercent}%{lesson.maxAttempts ? ` · ${lesson.maxAttempts - lesson.attemptsUsed} attempts left` : ''}</p>
      {lesson.bestPercent != null && <p>Your best score so far: <b>{lesson.bestPercent}%</b></p>}
      <button className="btn sq" onClick={() => setStarted(true)} disabled={attemptsLeft !== null && attemptsLeft <= 0}>Start quiz</button>
    </div>
  );

  return (
    <div className="form-card wide quiz">
      <div className="quiz-head">
        <span>Pass mark: {lesson.passPercent}%{attemptsLeft !== null && ` · Attempts left: ${Math.max(0, attemptsLeft)}`}</span>
        {lesson.timeLimitMinutes && !result && <span className={'timer' + (left < 60 ? ' low' : '')}>⏱ {Math.floor(left / 60)}:{String(left % 60).padStart(2, '0')}</span>}
      </div>
      {lesson.questions.map((q, i) => {
        const r = result?.results.find(x => x.id === q.id);
        return (
          <div key={q.id} className={'question' + (r ? (r.correct ? ' right' : ' wrong') : '')}>
            <p><b>{i + 1}.</b> {q.prompt}</p>
            {q.type === 'FILL' ? (
              <input value={answers[q.id] || ''} onChange={e => set(q.id, e.target.value)} disabled={!!result} placeholder="Your answer" />
            ) : q.options.map((o, idx) => (
              <label className="check" key={idx}>
                {q.type === 'MULTI'
                  ? <input type="checkbox" checked={(answers[q.id] || '').split(',').includes(String(idx))} onChange={() => toggleMulti(q.id, idx)} disabled={!!result} />
                  : <input type="radio" name={'q' + q.id} checked={answers[q.id] === String(idx)} onChange={() => set(q.id, String(idx))} disabled={!!result} />}
                {o}
              </label>
            ))}
            {r && <div className="explain">{r.correct ? '✅ Correct.' : `❌ Correct answer: ${r.correctAnswer}.`} {r.explanation}</div>}
          </div>
        );
      })}
      {result ? (
        <div className="quiz-result">
          <b>{result.passed ? '🎉 Passed!' : 'Not passed yet'}</b> You scored {result.score}/{result.total} ({result.percent}%).
          {!result.passed && (attemptsLeft === null || attemptsLeft > 0) && <button className="btn ghost sq small" onClick={retry}>Try again</button>}
        </div>
      ) : (
        <button className="btn sq" disabled={busy} onClick={submit}>Submit answers</button>
      )}
    </div>
  );
}

export default function Learn({ slug, lessonId }: { slug: string; lessonId?: string }) {
  const { user } = useAuth();
  const course = useLoad(() => api<LearnCourse>('/api/learn/' + encodeURIComponent(slug)), [slug, user?.id]);
  const lid = lessonId ? Number(lessonId) : course.data?.resumeLessonId;
  const lesson = useLoad(() => (lid ? api<Lesson>(`/api/learn/${encodeURIComponent(slug)}/lessons/${lid}`) : Promise.resolve(null)), [slug, lid, user?.id]);
  const [note, setNote] = useState('');
  const [sideOpen, setSideOpen] = useState(false);
  const noteTimer = useRef<number>();
  const L = lesson.data;

  useEffect(() => { setNote(L?.note || ''); if (L) setMeta(L.title + ' – ' + (course.data?.card.title || '')); }, [L?.id]); // eslint-disable-line react-hooks/exhaustive-deps

  if (course.loading && !course.data) return <Spinner />;
  if (course.error || !course.data) return <div className="page"><ErrorBox message={course.error || 'Not found'} retry={course.reload} /></div>;
  const c = course.data;
  const canSave = c.hasAccess && !!user;
  const pct = c.total ? Math.round((c.completed / c.total) * 100) : 0;

  const complete = async () => {
    if (!L) return;
    try {
      if (!L.completed) await api(`/api/learn/lessons/${L.id}/progress`, { method: 'PUT', body: { completed: true } });
      course.reload();
      if (L.nextId) navigate(`/learn/${slug}/${L.nextId}`);
      else { lesson.reload(); toast('🎉 You reached the end of the course!'); }
    } catch (e) { toast(errMsg(e), 'error'); }
  };
  const bookmark = async () => {
    if (!L) return;
    await api(`/api/learn/lessons/${L.id}/progress`, { method: 'PUT', body: { bookmarked: !L.bookmarked } }).catch(e => toast(errMsg(e), 'error'));
    lesson.reload();
    toast(L.bookmarked ? 'Bookmark removed' : 'Lesson bookmarked');
  };
  const onNote = (v: string) => {
    setNote(v);
    clearTimeout(noteTimer.current);
    noteTimer.current = window.setTimeout(() => {
      if (L) api(`/api/learn/lessons/${L.id}/note`, { method: 'PUT', body: { text: v } }).catch(() => undefined);
    }, 800);
  };
  const claim = async () => {
    try {
      const cert = await api<Certificate>(`/api/learn/${slug}/certificate`, { method: 'POST' });
      toast('🎓 Certificate issued: ' + cert.code);
      course.reload();
      window.dispatchEvent(new Event('app:notifications'));
    } catch (e) { toast(errMsg(e), 'error'); }
  };

  return (
    <div className="player">
      <aside className={'side' + (sideOpen ? ' open' : '')}>
        <div className="side-head">
          <Link to={c.hasAccess ? '/dashboard' : '/p/' + slug}>← {c.hasAccess ? 'My Courses' : 'Course page'}</Link>
          <h3>{c.card.title}</h3>
          <div className="progress"><i style={{ width: pct + '%' }} /></div>
          <small>{pct}% complete · {c.completed}/{c.total} lessons</small>
          {c.hasAccess && (c.certificateCode ? (
            <button className="btn sq small block" onClick={() => download(`/api/certificates/${c.certificateCode}/pdf`, `certificate-${c.certificateCode}.pdf`)}>⬇ Download certificate</button>
          ) : pct === 100 && <button className="btn sq small block" onClick={claim}>🎓 Get your certificate</button>)}
        </div>
        {c.sections.map(s => (
          <div key={s.id}>
            <div className="sec-t">{s.title}</div>
            {s.lessons.map(l => {
              const locked = !c.hasAccess && !l.preview;
              return (
                <Link key={l.id} to={locked ? '/p/' + slug : `/learn/${slug}/${l.id}`} className={l.id === lid ? 'active' : ''} onClick={() => setSideOpen(false)}>
                  <span className={l.completed ? 'done' : ''}>{l.completed ? '✔' : locked ? '🔒' : l.type === 'VIDEO' ? '▶' : l.type === 'QUIZ' ? '✎' : '≡'}</span>
                  <span>{l.title}{l.durationSeconds > 0 && ` (${duration(l.durationSeconds)})`}</span>
                </Link>
              );
            })}
          </div>
        ))}
      </aside>
      <section className="stage">
        <button className="btn ghost sq small only-mobile" onClick={() => setSideOpen(!sideOpen)}>☰ Curriculum</button>
        {lesson.loading && !L && <Spinner />}
        {lesson.error && <ErrorBox message={lesson.error} />}
        {!lid && !course.loading && <div className="empty">{c.hasAccess ? 'This course has no lessons yet.' : <>Enroll to start this course. <Link to={'/p/' + slug}>View course</Link></>}</div>}
        {L && (
          <>
            <small className="muted">{L.sectionTitle}</small>
            <div className="lesson-title">
              <h2>{L.title}</h2>
              {canSave && <button className={'bm' + (L.bookmarked ? ' on' : '')} onClick={bookmark} title="Bookmark">{L.bookmarked ? '★ Bookmarked' : '☆ Bookmark'}</button>}
            </div>
            {L.type === 'VIDEO' && <VideoPlayer lesson={L} canSave={canSave} />}
            {L.type === 'QUIZ' && (canSave
              ? <Quiz key={L.id} lesson={L} onPassed={() => { course.reload(); }} />
              : <div className="info-box"><span className="q">i</span><span>Enroll to take the quiz.</span></div>)}
            {!L.hasAccess && <div className="info-box"><span className="q">i</span><span>You're watching a free preview. <Link to={'/p/' + slug}>Enroll</Link> to unlock every lesson.</span></div>}
            <div className="stage-nav">
              {L.prevId ? <Link className="btn ghost sq" to={`/learn/${slug}/${L.prevId}`}>← Previous</Link> : <span />}
              {canSave
                ? (L.type !== 'QUIZ' || L.completed) && <button className="btn sq" onClick={complete}>{L.completed ? (L.nextId ? 'Next →' : '✔ Completed') : 'Complete & Continue →'}</button>
                : L.nextId && <Link className="btn sq" to={`/learn/${slug}/${L.nextId}`}>Next →</Link>}
            </div>
            <RichText text={L.content} />
            {L.resources.length > 0 && (
              <div className="resources">
                <h4>Lesson resources</h4>
                {L.resources.map((r, i) => <a key={i} href={r.url} target="_blank" rel="noopener noreferrer">⬇ {r.label}</a>)}
              </div>
            )}
            {canSave && (
              <div className="notes">
                <h4>My private notes</h4>
                <textarea value={note} onChange={e => onNote(e.target.value)} placeholder="Notes are saved automatically and only visible to you…" />
              </div>
            )}
          </>
        )}
      </section>
    </div>
  );
}
