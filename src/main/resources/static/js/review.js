/* /js/review.js */
(() => {
  // ── 유틸 ─────────────────────────────────
  const $  = (s, r=document) => r.querySelector(s);
  const $$ = (s, r=document) => r.querySelectorAll(s);
  const A  = (i) => String.fromCharCode(65+i); // 0->A
  const circ = ['①','②','③','④','⑤'];

  // 공통 헤더
  function authHeaders(){
    const h = { Accept:'application/json' };
    const t = localStorage.getItem('token');
    if (t) h.Authorization = 'Bearer ' + t;
    return h;
  }

  // 첫 번째 2xx를 돌려주는 유틸
  async function fetchFirstOk(urls, init){
    for (const u of urls){
      try {
        const r = await fetch(u, init);
        if (r.ok) return r;
        console.debug('[review] fallback try', u, '→', r.status);
      } catch(e) {
        console.debug('[review] fetch error on', u, e);
      }
    }
    throw new Error('모든 리뷰 엔드포인트가 실패했습니다.');
  }
  // 1) 관리자 여부 체크 추가
  // 관리자 여부
  function isAdmin(){
    try {
      const t = localStorage.getItem('token') || '';
      const p = JSON.parse(atob(t.split('.')[1] || '')) || {};
      const roles = p.authorities || p.roles || p.role || p.scope || p.scopes || [];
      const str   = Array.isArray(roles) ? roles.join(',') : String(roles || '');
      return /ROLE_ADMIN|ADMIN/i.test(str);
    } catch { return false; }
  }

  // 단 하나만 남길 load()
  async function load(){
    const id = getExamId();
    const rootTitle = $('#hdrTitle');
    if (!Number.isFinite(id) || id<=0){
      rootTitle.textContent = '잘못된 시험 ID';
      $('#hdrMeta').textContent = '';
      wireBack();
      return;
    }
    try{
      const admin = isAdmin();

      // ✅ 관리자면 admin 엔드포인트를 먼저 시도
      const candidates = admin
        ? [
            `/api/admin/exams/${id}/review`,
            `/api/admin/review/${id}`,
            `/api/admin/exam/${id}/review`,
            `/api/exams/${id}/review?as=admin`,
            // 사용자용 폴백
            `/api/exams/${id}/review`,
            `/api/review/${id}`,
            `/api/reviews/${id}`,
            `/api/exam/${id}/review`
          ]
        : [
            `/api/exams/${id}/review`,
            `/api/review/${id}`,
            `/api/reviews/${id}`,
            `/api/exam/${id}/review`
          ];

      const res = await fetchFirstOk(candidates, { headers: authHeaders() });
      const dto = await res.json();

      EXAM.meta = {
        examId: dto.examId,
        csatDate: dto.csatDate,
        subclassId: dto.subclassId,
        subclassName: dto.subclassName,
        testDate: dto.testDate,
        result: dto.result ?? dto.score
      };
      EXAM.items = Array.isArray(dto.items) ? dto.items : [];

      renderHeader();
      renderOMR();
      renderQuestions();
    } catch(err){
      console.error(err);
      $('#hdrMeta').textContent = '풀이 데이터를 불러오지 못했습니다.';
    } finally {
      wireBack();
    }
  }

  // 바인딩도 이 하나만
  document.addEventListener('DOMContentLoaded', load);
  function getExamId(){
    const v = document.body?.dataset?.examId;
    if (v && !isNaN(v)) return Number(v);
    const m = location.pathname.match(/\/review\/(\d+)(?:\/)?$/);
    return m ? Number(m[1]) : NaN;
  }

  function sanitizeHTML(html){
    let s = String(html ?? '');
    s = s.replace(/<script\b[^>]*>[\s\S]*?<\/script>/gi, '');
    s = s.replace(/\son[a-z]+\s*=\s*(?:"[^"]*"|'[^']*'|[^\s>]+)/gi, '');
    return s;
  }

  function normIndex(val, count){
    if (val == null) return null;
    if (typeof val === 'number') {
      if (val >= 0 && val < count) return val;
      if (val >= 1 && val <= count) return val-1;
    }
    const s = String(val).trim().toUpperCase();
    if (/^\d+$/.test(s)) {
      const n = Number(s); if (n>=1 && n<=count) return n-1;
      if (n>=0 && n<count) return n;
    }
    if (/^[A-Z]$/.test(s)) {
      const i = s.charCodeAt(0)-65; if (i>=0 && i<count) return i;
    }
    return null;
  }

  // ── 상태 ─────────────────────────────────
  let EXAM = { meta:{}, items:[] };
  let currentPage   = 1;
  const perPage     = 6;

  // ── 렌더 헤더 ─────────────────────────────
  function renderHeader(){
    const m = EXAM.meta || {};
    $('#hdrTitle').textContent =
      `${m.subclassName ?? m.subclassId ?? '세부과목'} · ${m.csatDate ?? ''}`;
    $('#hdrMeta').textContent =
      `응시일: ${m.testDate ?? '-'} · 점수: ${m.result ?? m.score ?? '-'}`;
  }

  // ── OMR ──────────────────────────────────
  function renderOMR(){
    const box = $('#omrContainer');
    box.innerHTML = '';
    EXAM.items.forEach((q, idx) => {
      const total = 5;
      const c = normIndex(q.correct, total);
      const u = normIndex(q.myAnswer ?? q.userAnswer, total);
      const ok = (c!=null && u!=null && c===u);

      const line = document.createElement('div');
      line.className = 'omr-line';
      line.innerHTML = `
        <span class="omr-q" title="이 문제로 이동" data-go="${idx}">Q${idx+1}:</span>
        ${[0,1,2,3,4].map(i => {
          const sel = (u===i) ? ' sel' : '';
          const more = (u===i && c!=null)
            ? (ok ? ' ok' : ' bad') : '';
          return `<button type="button" class="omr-btn${sel}${more}" data-idx="${idx}" data-choice="${i}">${circ[i]}</button>`;
        }).join('')}
      `;
      box.appendChild(line);
    });
  }

  // ── 문제(2열) ────────────────────────────
  function renderQuestions(){
    const items = EXAM.items || [];
    const start = (currentPage-1)*perPage;
    const end   = Math.min(start+perPage, items.length);
    $('#pageInfo').textContent = `${currentPage} / ${Math.max(1, Math.ceil(items.length/perPage))}`;

    // 두 컬럼 초기화
    $('#questionContainer').innerHTML = `<div id="colL" class="question-col"></div><div id="colR" class="question-col"></div>`;
    const L = $('#colL'), R = $('#colR');

    const page = items.slice(start, end);
    const leftCount = Math.ceil(page.length/2);

    function makeBox(q, globalIndex){
      const total = 5;
      const c = normIndex(q.correct, total);
      const u = normIndex(q.myAnswer ?? q.userAnswer, total);
      const ok = (c!=null && u!=null && c===u);

      const div = document.createElement('div');
      div.className = 'question-box';
      div.id = `qbox-${globalIndex}`;

      // 본문(지문 이미지가 있다면 표시)
      let ctxHtml = '';
      const ctx = q.contextImage || q.questionContext || q.explanation || '';
      if (typeof ctx === 'string' && /^https?:\/\//i.test(ctx)) {
        ctxHtml = `<div class="article-box"><img src="${ctx}" alt=""></div>`;
      }

      const badge =
        (u==null || c==null) ? `<span class="badge na">무채점</span>` :
        ok ? `<span class="badge ok">정답</span>` :
             `<span class="badge no">오답</span>`;

      const choices = (Array.isArray(q.options) ? q.options.slice(0,5) : []).concat(Array(5).fill('')).slice(0,5);
      const choiceHtml = choices.map((html, i) => {
        const isC = (i===c);
        const isU = (i===u);
        const cls =
          isC ? 'choice correct readonly' :
          (isU && !ok) ? 'choice wrong readonly' :
          'choice readonly';
        return `
          <div class="${cls}">
            <div class="letter">${A(i)}.</div>
            <div class="content">${sanitizeHTML(html)}</div>
          </div>`;
      }).join('');

      div.innerHTML = `
        <div class="d-flex" style="display:flex; justify-content:space-between; align-items:flex-start; gap:8px; margin-bottom:6px;">
          <h3 class="q-title" style="font-size:15px; margin:0;">Q${q.number ?? (globalIndex+1)}</h3>
          ${badge}
        </div>
        ${ctxHtml}
        <div style="margin-bottom:8px;">${sanitizeHTML(q.stem ?? q.question ?? '')}</div>
        <div class="choices">${choiceHtml}</div>
        ${q.explanation && !/^https?:\/\//i.test(q.explanation) ? `
          <div class="article-box" style="margin-top:8px;">
            <div style="font-weight:700; margin-bottom:4px; color:#334155;">해설</div>
            <div>${sanitizeHTML(q.explanation)}</div>
          </div>` : ''}
      `;
      return div;
    }

    // 좌/우 채우기
    for (let i=0;i<leftCount;i++){
      const gi = start+i;
      if (page[i]) L.appendChild(makeBox(page[i], gi));
    }
    for (let j=leftCount;j<page.length;j++){
      const gi = start+j;
      if (page[j]) R.appendChild(makeBox(page[j], gi));
    }

    // 스크롤 상단
    document.querySelector('.question-section')?.scrollTo({top:0, behavior:'instant'});
  }

  // ── 페이지 전환 & 이동 ───────────────────
  function totalPages(){ return Math.max(1, Math.ceil((EXAM.items?.length || 0)/perPage)); }
  function prev(){ if (currentPage>1){ currentPage--; renderQuestions(); } }
  function next(){ if (currentPage<totalPages()){ currentPage++; renderQuestions(); } }
  function goTo(index){
    const tp = Math.floor(index/perPage)+1;
    if (tp !== currentPage){
      currentPage = tp;
      renderQuestions();
      setTimeout(() => {
        const el = document.getElementById(`qbox-${index}`);
        el?.scrollIntoView({behavior:'smooth', block:'start'});
      }, 50);
    } else {
      document.getElementById(`qbox-${index}`)?.scrollIntoView({behavior:'smooth', block:'start'});
    }
  }

  // ── Back 경로 처리 ───────────────────────
  function getBackTarget(){
    const usp = new URLSearchParams(location.search);
    let back = usp.get('back');

    // ① back 파라미터 우선 사용
    if (back) {
      try { back = decodeURIComponent(back); } catch {}
      try {
        // 절대경로면 그대로, 아니면 같은 오리진 기준으로 경로/쿼리만 사용
        const u = back.startsWith('/')
          ? new URL(back, location.origin)
          : new URL(back, location.origin);
        return canonicalizeStatsPath(u.pathname + u.search + u.hash);
      } catch {
        // 파싱 실패 시 원본 경로를 정규화 시도
        return canonicalizeStatsPath(back);
      }
    }

    // ② referrer 기반 (여기도 정규화해서 /stats/user|/stats/admin으로)
    try {
      if (document.referrer) {
        const ru = new URL(document.referrer, location.origin);
        if (ru.origin === location.origin) {
          return canonicalizeStatsPath(ru.pathname + ru.search + ru.hash);
        }
      }
    } catch {}

    // ③ body data-mode 힌트
    const mode = (document.body?.dataset?.mode || '').toLowerCase();
    if (mode === 'one') return '/stats/user';
    if (mode === 'all') return '/stats/admin';

    // ④ 최종 기본
    return '/stats';
  }

  // /stats?mode=one → /stats/user, /stats?mode=all → /stats/admin
  function canonicalizeStatsPath(p){
    try {
      const u = new URL(p, location.origin);
      if (u.pathname === '/stats') {
        const mode = (u.searchParams.get('mode') || '').toLowerCase();
        if (mode === 'one') u.pathname = '/stats/user';
        if (mode === 'all') u.pathname = '/stats/admin';
      }
      return u.pathname + u.search + u.hash;
    } catch {
      return p;
    }
  }

  // ── 이벤트 바인딩 ────────────────────────
  document.addEventListener('click', (e)=>{
    // OMR 번호 클릭 → 해당 문제로
    const nav = e.target.closest('[data-go]');
    if (nav){ goTo(Number(nav.dataset.go)); return; }
  });
  $('#btnPrev')?.addEventListener('click', prev);
  $('#btnNext')?.addEventListener('click', next);

  // 돌아가기 버튼
  function wireBack(){
    const backUrl = getBackTarget();
    const btn = $('#btnBackStats');
    if (!btn) return;
    btn.setAttribute('href', backUrl);
    btn.addEventListener('click', (e)=>{
      e.preventDefault();
      location.href = backUrl;
    });
  }

  // ── 로드 ─────────────────────────────────
  async function load(){
    const id = getExamId();
    const rootTitle = $('#hdrTitle');
    if (!Number.isFinite(id) || id<=0){
      rootTitle.textContent = '잘못된 시험 ID';
      $('#hdrMeta').textContent = '';
      wireBack(); // ← 아래 2번에서 설명하는 것처럼 초기에도 호출
      return;
    }
    try{
      const candidates = [
        `/api/admin/exams/${id}/review`,
        `/api/admin/review/${id}`,
        `/api/admin/exam/${id}/review`,
        `/api/exams/${id}/review`,   // 사용자용 후보들
        `/api/review/${id}`,
        `/api/reviews/${id}`,
        `/api/exam/${id}/review`
      ];
      const res = await fetchFirstOk(candidates, { headers: authHeaders() });
      const dto = await res.json();

      EXAM.meta = {
        examId: dto.examId,
        csatDate: dto.csatDate,
        subclassId: dto.subclassId,
        subclassName: dto.subclassName,
        testDate: dto.testDate,
        result: dto.result ?? dto.score
      };
      EXAM.items = Array.isArray(dto.items) ? dto.items : [];

      renderHeader();
      renderOMR();
      renderQuestions();
    }catch(err){
      console.error(err);
      $('#hdrMeta').textContent = '풀이 데이터를 불러오지 못했습니다.';
    } finally {
      wireBack(); // 안전하게 한 번 더 보정
    }
  }

  document.addEventListener('DOMContentLoaded', load);
})();
