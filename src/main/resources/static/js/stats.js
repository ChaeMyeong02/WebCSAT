/* /js/stats.js */
(() => {
  // ===== 모드 처리 =====
  function readMode() {
    const path = location.pathname.replace(/\/+$/, '');
    if (path === '/stats/admin') return 'all';
    if (path === '/stats/user')  return 'one';

    const q = new URLSearchParams(location.search).get('mode');
    if (q) return q.toLowerCase();

    const dm = document.body?.dataset?.mode;
    // 템플릿 원문/가짜 값이면 무시
    if (dm && !/^\[\[/.test(dm) && !/^(null|undefined|0)$/i.test(dm)) {
      return String(dm).toLowerCase();
    }

    return 'mine';
  }
  const MODE = readMode();
  console.debug('[stats] MODE=', MODE, 'path=', location.pathname, 'data-mode(raw)=', document.body?.dataset?.mode);


// ===== 유저 디렉터리(roles 포함) 캐시 =====
let USER_DIR = { map:new Map(), onlyUserIds:new Set(), loaded:false };

function _rolesToString(r){
  return Array.isArray(r) ? r.join(',') : String(r || '');
}
function _isRegularUser(roleStr){
  // USER 포함 && ADMIN 없음
  return /(ROLE_)?USER/i.test(roleStr) && !/ADMIN/i.test(roleStr);
}
function _displayName(u, id){
  return (u.nickname || u.name || u.username || u.email || ('user#'+id)).trim();
}

function _isPlaceholderName(name, id) {
  return !name || new RegExp(`^user#${String(id)}$`, 'i').test(String(name).trim());
}

async function _resolveUserById(uid) {
  const headers = authHeaders();
  const endpoints = [
    `/api/admin/users/${uid}`,
    `/api/admin/user/${uid}`,
    `/api/users/${uid}`,
    `/api/user/${uid}`,
    `/api/user?id=${encodeURIComponent(uid)}`
  ];
  for (const url of endpoints) {
    try {
      const r = await fetch(url, { headers });
      if (!r.ok) continue;
      const u = await r.json();
      const name =
        u.nickname ?? u.nickName ?? u.name ?? u.fullName ??
        u.username ?? u.email ?? u.user?.nickname ?? u.user?.name ??
        `user#${uid}`;
      const roleStr = _rolesToString(u.authorities ?? u.roles ?? u.role ?? u.scope ?? u.scopes ?? u.user?.role);
      USER_DIR.map.set(String(uid), { name: String(name).trim(), roleStr, isUser: true, raw: u });
      return;
    } catch {}
  }
  // 그래도 실패하면 placeholder 유지
  if (!USER_DIR.map.has(String(uid))) {
    USER_DIR.map.set(String(uid), { name: `user#${uid}`, roleStr: '', isUser: true, raw: null });
  }
}

async function _hydrateUserNamesFromRows(rows) {
  await ensureUsersLoaded();
  const uids = [...new Set(rows.map(r => String(r.userId)).filter(Boolean))];
  const need = uids.filter(uid => {
    const rec = USER_DIR.map.get(uid);
    return !rec || _isPlaceholderName(rec.name, uid);
  });
  await Promise.all(need.map(_resolveUserById));
}


// 모든 사용자를 한 번만 불러와 캐시
async function ensureUsersLoaded(){
  if (USER_DIR.loaded) return USER_DIR;

  const urls = ['/api/admin/users','/api/users','/api/user/all'];
  let raw = null;

  for (const u of urls){
    try {
      const r = await fetch(u, { headers: authHeaders() });
      if (!r.ok) continue;
      raw = await r.json();
      break;
    } catch {}
  }

  // 여러 응답 형태 지원: [], {content:[]}, {data:[]}, {items:[]}, {results:[]}
  let list = [];
  if (Array.isArray(raw)) list = raw;
  else if (raw && Array.isArray(raw.content)) list = raw.content;
  else if (raw && Array.isArray(raw.data))    list = raw.data;
  else if (raw && Array.isArray(raw.items))   list = raw.items;
  else if (raw && Array.isArray(raw.results)) list = raw.results;

  const map = new Map();
  const only = new Set();

  const toStr = (v)=> v == null ? '' : String(v);

  for (const u of list){
    const id =
      toStr(u.userId ?? u.id ?? u.user_id ?? u.uid ?? u.user?.id ?? '');
    if (!id) continue;

    const roleStr = _rolesToString(u.authorities ?? u.roles ?? u.role ?? u.scope ?? u.scopes ?? u.user?.role);
    const name =
      (u.nickname ?? u.nickName ?? u.user?.nickname) ??
      (u.name ?? u.fullName ?? u.user?.name) ??
      (u.username ?? u.user?.username) ??
      (u.email ?? u.user?.email) ??
      (`user#${id}`);

    const isUser = _isRegularUser(roleStr) || !roleStr; // 역할 못 읽어도 일반 유저로 간주
    map.set(id, { name: String(name).trim(), roleStr, isUser, raw: u });
    if (isUser) only.add(id);
  }

  USER_DIR.map = map;
  USER_DIR.onlyUserIds = only;
  USER_DIR.loaded = true;
  return USER_DIR;
}


  // 맨 위 MODE 아래에 추가
  let SELECTED_USER_ID = null;
  let SELECTED_USER_NAME = '';

  // CSS가 바로 적용되도록 body data-mode 보정
  (function ensureModeAttr() {
    const set = () => {
      const cur = document.body?.dataset?.mode || '';
      if (!cur || /^\[\[/.test(cur) || /^(null|undefined|0)$/i.test(cur)) {
        document.body.dataset.mode = MODE;  // 'mine' | 'all' | 'one'
        console.debug('[stats] data-mode fixed to', MODE);
      }
    };
    if (document.readyState === 'loading') {
      document.addEventListener('DOMContentLoaded', set);
    } else {
      set();
    }
  })();


  // 선택 유저 헬퍼
  function getSelectedUserId(){
    // MODE=one일 때만 콤보박스에서 읽음
    if (MODE !== 'one') return null;
    const v = document.getElementById('userSelect')?.value || '';
    return v || null;
  }
  function getSelectedUserName(){
    if (MODE !== 'one') return null;
    const sel = document.getElementById('userSelect');
    if (!sel) return null;
    const opt = sel.options[sel.selectedIndex];
    return opt ? opt.textContent.trim() : null;
  }

  const $  = (s) => document.querySelector(s);
  const $$ = (s) => Array.from(document.querySelectorAll(s));
  let distChart, trendChart, gradeChart, distCtx, trendCtx, gradeCtx;
  let LAST_ROWS = [];


// ==== 값 라벨 플러그인 (Chart.js v4) ====
const ValueLabelPlugin = {
  id: 'valueLabel',
  afterDatasetsDraw(chart, args, opts) {
    const { ctx, data, chartArea } = chart;
    const dsIndex = opts?.datasetIndex ?? 0;
    const ds = data.datasets?.[dsIndex];
    const meta = chart.getDatasetMeta(dsIndex);
    if (!ds || !meta || meta.hidden) return;

    // 너무 많을 땐 생략
    const maxPoints = opts?.maxPoints ?? 60;
    if ((ds.data?.length || 0) > maxPoints) return;

    const fmt = opts?.formatter || ((v)=> String(Math.round(v)));
    const offset = opts?.offset ?? 8;
    const color  = opts?.color  ?? '#0b1220';
    const font   = opts?.font   || { size: 11, weight: '600' };

    ctx.save();
    ctx.font = `${font.weight || ''} ${font.size || 11}px ${font.family || 'system-ui, -apple-system, Segoe UI, Roboto, sans-serif'}`;
    ctx.textAlign = 'center';
    ctx.textBaseline = 'bottom';
    ctx.fillStyle = color;

    meta.data.forEach((elem, i) => {
      const val = ds.data?.[i];
      if (val == null || isNaN(val)) return;

      const { x, y } = elem.getProps(['x','y'], true);
      let yy = y - offset;
      if (yy < chartArea.top + 6) yy = chartArea.top + 6; // 화면 밖 방지

      const text = fmt(val, i, ds, chart);

      // 가독성: 하얀 외곽선 + 채움
      ctx.lineWidth = 3;
      ctx.strokeStyle = 'rgba(255,255,255,.9)';
      ctx.strokeText(text, x, yy);
      ctx.fillText(text, x, yy);
    });

    ctx.restore();
  }
};
Chart.register(ValueLabelPlugin);

// 점수(좌측 Y) / 등급(우측 Y) 색
const COLOR_SCORE = '#1976d2';  // 파랑
const COLOR_GRADE = '#2e7d32';  // 초록


  // ========== 공통 유틸 ==========
  const hide    = (sel) => $$(sel).forEach(el => el && (el.style.display = 'none'));
  const show    = (sel, disp='') => $$(sel).forEach(el => el && (el.style.display = disp));
  const setText = (id, v='-') => { const el = document.getElementById(id); if (el) el.textContent = (v ?? '-') };
  const toFixed = (n, d=1) => (Number.isFinite(n) ? Number(n).toFixed(d) : '-');

  function authHeaders(){
    const h = { Accept:'application/json' };
    const t = localStorage.getItem('token'); if (t) h.Authorization = 'Bearer ' + t;
    return h;
  }
  function parseToken(){
    const t = localStorage.getItem('token');
    if (!t) return {};
    try { return JSON.parse(atob(t.split('.')[1])) || {}; }
    catch { return {}; }
  }
  function isAdmin(){
    const p = parseToken();
    const roles = p.authorities || p.roles || p.role || p.scope || p.scopes || [];
    const str   = Array.isArray(roles) ? roles.join(',') : String(roles || '');
    return /ROLE_ADMIN|ADMIN/i.test(str);
  }
  function getUidFromToken(){
    const p = parseToken();
    return p.userId || p.id || p.sub || null;
  }
  function getUidForApi(){
    try {
      const p = JSON.parse(atob((localStorage.getItem('token') || '').split('.')[1] || '')) || {};
      const candidates = [p.userId, p.id, p.sub, document.body?.dataset?.userId];
      for (const v of candidates) {
        if (v == null) continue;
        const s = String(v);
        if (s && s !== '0' && !s.startsWith('[[')) return s;
      }
    } catch {}
    return null;
  }

  function uniqueSorted(list){
    return [...new Set(list)].filter(Boolean).sort();
  }
  function formatDateTimeExact(v){
    if (v == null) return '';
    if (typeof v === 'number') {
      const d = new Date(v);
      const y  = d.getFullYear();
      const m  = String(d.getMonth()+1).padStart(2,'0');
      const dd = String(d.getDate()).padStart(2,'0');
      const hh = String(d.getHours()).padStart(2,'0');
      const mm = String(d.getMinutes()).padStart(2,'0');
      const ss = String(d.getSeconds()).padStart(2,'0');
      return `${y}-${m}-${dd} ${hh}:${mm}:${ss}`;
    }
    const s = String(v);
    const m = s.match(/^(\d{4}-\d{2}-\d{2})[ T](\d{2}:\d{2}:\d{2})/);
    if (m) return `${m[1]} ${m[2]}`;
    return s.replace('T',' ');
  }

  function ensureUserSelectorExists() {
    let row = document.getElementById('userSelectorRow');
    if (row) return row;

    // 혹시 템플릿에 없을 때만 새로 생성
    row = document.createElement('div');
    row.id = 'userSelectorRow';
    row.className = 'row g-3 align-items-end mb-2';
    row.style.display = 'none';
    row.innerHTML = `
      <div class="col-12 col-lg-4">
        <label class="form-label">사용자</label>
        <select id="userSelect" class="form-select">
          <option value="">사용자 선택</option>
        </select>
      </div>
    `;

    // ✅ 제목 아래에 삽입 (템플릿 구조와 동일한 위치)
    const titleWrap = document.querySelector('.container .d-flex.align-items-center.mb-3');
    if (titleWrap) {
      titleWrap.insertAdjacentElement('afterend', row);
    } else {
      document.querySelector('.container')?.prepend(row);
    }
    return row;
  }

  // ---- 한명 모드일 때만 보이도록 하고, 위치를 보정한다 ----
  function ensureUserSelectorPlacement() {
    const row = ensureUserSelectorExists();
    if (!row) return;

    // 모드별 표시만 제어
    row.style.display = (MODE === 'one') ? '' : 'none';

    // ✅ 혹시라도 다른 곳으로 이동했으면, 제목 아래로 되돌리기
    const titleWrap = document.querySelector('.container .d-flex.align-items-center.mb-3');
    if (titleWrap && titleWrap.nextElementSibling !== row) {
      titleWrap.insertAdjacentElement('afterend', row);
    }
  }
  function distributionCountLabel(){
    // 관리자 한 명 보기면 '응시수', 그 외엔 '인원수'
    return readMode() === 'one' ? '응시수' : '인원수';
  }

  function updateRemindLabel() {

  const title = computeRemindTitle();

    // 모달 제목만 갱신
    const modalTitle = document.getElementById('attemptsModalTitle');
    if (modalTitle) modalTitle.textContent = title;

    let label;
    if (MODE === 'all') {
      label = '리마인드 - 전체 유저 응시 목록';
    } else if (MODE === 'one') {
      const name = getSelectedUserName();
      label = `리마인드 - ${(name || '선택 사용자')} 응시 목록`;
    } else {
      label = '리마인드 - 내 응시 목록';
    }

    // 제목 후보: #remindTitle > [data-role="remind-title"] > #btnReview(버튼)
    const titleEl =
      document.getElementById('remindTitle') ||
      document.querySelector('[data-role="remind-title"]') ||
      document.getElementById('btnReview');

    if (!titleEl) return;

    // 아이콘 보존: .label 스팬이 있으면 거기만 교체
    const labelSpan = titleEl.querySelector && titleEl.querySelector('.label');
    if (labelSpan) {
      labelSpan.textContent = label;
    } else {
      titleEl.textContent = label;
    }
  }



  // ====== (수정) 사용자 목록 로드: MODE === 'one' 에서만, USER만 ======
  async function loadUsersIfNeeded(){
    if (MODE !== 'one') return;
    const row = $('#userSelectorRow');
    if (row) row.style.display = '';

    await ensureUsersLoaded();

    const sel = $('#userSelect');
    if (!sel) return;

    const options = ['<option value="">사용자 선택</option>'];
    const entries = Array.from(USER_DIR.map.entries())
      .filter(([id,info]) => info.isUser)
      .sort((a,b)=> a[1].name.localeCompare(b[1].name, 'ko'));

    for (const [id, info] of entries){
      options.push(`<option value="${id}">${info.name}</option>`);
    }
    sel.innerHTML = options.join('');

    // 로드 직후 배치/라벨 갱신
    ensureUserSelectorPlacement();
    updateRemindLabel();
  }
//  if (MODE === 'one' && pUserId) {
//    const sel = document.getElementById('userSelect');
//    if (sel) sel.value = pUserId;
//  }

  // ========== 룩업 ==========
  async function loadSubjects() {
    let url = '/api/lookup/subjects';
    let res = await fetch(url, { headers: authHeaders() });
    if (!res.ok && res.status === 404) { url = '/api/subjects'; res = await fetch(url, { headers: authHeaders() }); }
    if (!res.ok) throw new Error('subjects HTTP ' + res.status);

    const list = await res.json();
    const html =
      '<option value="">과목 선택</option>' +
      (Array.isArray(list) ? list.map(s=>{
        const id   = s.id ?? s.subjectId ?? s.subject_id;
        const name = s.name ?? s.subjectName ?? s.subject_name ?? '(이름없음)';
        return `<option value="${id}">${name}</option>`;
      }).join('') : '');
    const sel = $('#subjectId');
    sel.innerHTML = html;
    sel.disabled = false;
  }

  function fillCsatDates(list){
    const cs = document.getElementById('csatDate');
    const prev = cs?.value || ''; // 현재 선택값 보관
    const opts = ['<option value="">전체 응시월</option>']
      .concat((list||[]).map(v => `<option value="${v}">${v}</option>`));
    cs.innerHTML = opts.join('');
    // 목록에 이전 값이 있으면 그대로 유지
    if (prev && Array.from(cs.options).some(o => o.value === prev)) {
      cs.value = prev;
    }
    cs.disabled = !(list && list.length);
  }

  async function loadCsatDatesForCurrentSelection(){
    const subjectId  = $('#subjectId').value || '';
    const subclassId = $('#subclassId').value || '';

    fillCsatDates([]);

    if (subclassId) {
      try {
        const res = await fetch(`/api/lookup/csat-dates?subclassId=${subclassId}`, { headers: authHeaders() });
        if (res.ok) { fillCsatDates(await res.json()); return; }
      } catch(_) {}
      const rows = await fetchAllRows({ ignoreCsat:true });
      fillCsatDates(uniqueSorted(rows.map(r => r.csatDate)));
      return;
    }

    if (subjectId && subjectId !== '*') {
      const rows = await fetchAllRows({ ignoreCsat:true });
      fillCsatDates(uniqueSorted(rows.map(r => r.csatDate)));
      return;
    }

    try {
      const res = await fetch('/api/lookup/csat-dates-global', { headers: authHeaders() });
      if (res.ok) { fillCsatDates(await res.json()); return; }
    } catch(_) {}
    const rows = await fetchAllRows({ ignoreCsat:true });
    fillCsatDates(uniqueSorted(rows.map(r => r.csatDate)));
  }

  // ========== 데이터 ==========
  async function fetchAllRows(opts = {}){
    const { ignoreCsat = false } = opts;
    const subjectId  = $('#subjectId').value || '';
    const subclassId = $('#subclassId').value || '';
    const csatDate   = $('#csatDate').value || '';
    const selUserId  = $('#userSelect')?.value || '';

    const qs = new URLSearchParams();
    if (subjectId && subjectId !== '*') qs.set('subjectId', subjectId);
    if (subclassId) qs.set('subclassId', subclassId);
    if (csatDate && !ignoreCsat) qs.set('csatDate', csatDate);

    // MODE 별 사용자 스코프
    if (MODE === 'one' && selUserId) {
      qs.set('userId', selUserId); // 백엔드에서 지원하면 서버 필터, 아니면 클라에서 다시 필터링
    }

    const res = await fetch(`/api/exams?` + qs.toString(), { headers: authHeaders() });
    if (!res.ok) throw new Error('HTTP ' + res.status);
    let data = await res.json();

    // mine: 내 것만 남기기(서버에서 전부 내려줄 경우 대비)
    if (MODE === 'mine') {
      const uid = getUidForApi();
      if (uid) data = data.filter(r => String(r.userId) === String(uid));
    }
    // one: 선택 유저만 남기기(서버 필터 없을 때 대비)
    if (MODE === 'one' && selUserId) {
      data = data.filter(r => String(r.userId) === String(selUserId));
    }

    // (추가) 전체/한명 모드에서는 USER만 남긴다
    if (MODE === 'all' || MODE === 'one') {
      await ensureUsersLoaded();
      if (USER_DIR.onlyUserIds.size) {
        data = data.filter(r => USER_DIR.onlyUserIds.has(String(r.userId)));
      }
    }
    return data; // [{examId,userId,subclassId,csatDate,result,testDate,subclassName}]
  }

  // ========== 기본 통계 ==========
  function calcStats(rows){
    const attempts = rows.length;
    const participants = new Set(rows.map(r => r.userId)).size;

    const scores = rows
      .map(r => Number(r.result ?? r.score ?? 0))
      .filter(Number.isFinite)
      .sort((a,b)=>a-b);

    const BIN = 10;
    const labels = Array.from({length:BIN}, (_,i)=>`${i*10}-${(i+1)*10}`);
    const counts = Array(BIN).fill(0);
    for (const s of scores){
      const idx = Math.max(0, Math.min(9, Math.floor(s/10)));
      counts[idx]++;
    }

    const avg = scores.length ? Math.round((scores.reduce((a,b)=>a+b,0)/scores.length)*10)/10 : 0;
    return { attempts, participants, labels, counts, avg, scores };
  }

  // ========== 등급컷 분포 ==========
  async function fetchGradeCuts(csatDate, subclassId){
    const qs = new URLSearchParams({ csatDate, subclassId });
    const res = await fetch(`/api/lookup/grade-cuts?` + qs.toString(), { headers: authHeaders() });
    if (res.status === 204) return null;
    if (!res.ok) throw new Error('grade-cuts HTTP ' + res.status);
    return await res.json();
  }
  function gradeFromScore(score, cuts){
    if (score == null || !cuts) return null;
    for (let g = 1; g <= 9; g++){
      const cut = cuts['cut'+g];
      if (cut == null) continue;
      if (score >= Number(cut)) return g;
    }
    return 9;
  }

  function drawScoreDistribution(rows){
    if (!distCtx) return;
    const { attempts, participants, labels, counts } = calcStats(rows);

    if (distChart) distChart.destroy();
    distChart = new Chart(distCtx, {
      type:'bar',
      data:{ labels, datasets:[{ label: distributionCountLabel(), data: counts }] },
      options:{
        responsive:true, maintainAspectRatio:false,
        plugins:{ legend:{display:true}, title:{ display:true, text:`점수 분포 · 응시 ${attempts}건 · 참여자 ${participants}명` } }
      }
    });

    const infoEl = document.getElementById('distInfo');
    if (infoEl) infoEl.textContent = `점수 분포 · 응시 ${attempts}건 · 참여자 ${participants}명`;
  }

  async function drawGradeCutDistribution(rows){
    const block   = document.getElementById('gradeBlock');
    const divider = document.getElementById('gradeDivider');
    const infoEl  = document.getElementById('gradeInfo');

    const subjectId  = (document.getElementById('subjectId')?.value || '');
    const subclassId = (document.getElementById('subclassId')?.value || '');
    const csatDate   = (document.getElementById('csatDate')?.value || '');

    const ready = !!(subjectId && subclassId && csatDate);
    if (!ready){
      if (gradeChart) { gradeChart.destroy(); gradeChart = null; }
      if (block)   block.style.display = 'none';
      if (divider) divider.style.display = 'none';
      if (infoEl)  infoEl.textContent = '';
      return;
    }

    try{
      const cuts = await fetchGradeCuts(csatDate, subclassId);
      if (!cuts) {
        if (gradeChart) { gradeChart.destroy(); gradeChart = null; }
        if (block)   block.style.display = 'none';
        if (divider) divider.style.display = 'none';
        if (infoEl)  infoEl.textContent = '등급컷 정보가 없습니다.';
        return;
      }

      const buckets = Array(9).fill(0);
      for (const r of rows){
        const score = Number(r.result ?? r.score ?? 0);
        let g = 9;
        for (let k=1; k<=9; k++){
          const cut = cuts['cut'+k];
          if (cut != null && score >= Number(cut)) { g = k; break; }
        }
        buckets[g-1]++;
      }
      const labels = Array.from({length:9}, (_,i)=>`${i+1}등급`);

      if (!gradeCtx) gradeCtx = document.getElementById('gradeChart')?.getContext('2d');
      if (!gradeCtx) return;

      if (gradeChart) gradeChart.destroy();
      gradeChart = new Chart(gradeCtx, {
        type:'bar',
        data:{ labels, datasets:[{ label: distributionCountLabel(), data: buckets }] },
        options:{
          responsive:true, maintainAspectRatio:false,
          plugins:{ legend:{display:true}, title:{ display:true, text:`등급 분포(컷 기준) · 회차 ${csatDate}` } }
        }
      });

      if (infoEl)  infoEl.textContent = `회차 ${csatDate} · 세부과목 기준`;
      if (divider) divider.style.display = '';
      if (block)   block.style.display   = '';
    } catch(e){
      console.warn('grade-cut draw failed', e);
      if (gradeChart) { gradeChart.destroy(); gradeChart = null; }
      if (block)   block.style.display = 'none';
      if (divider) divider.style.display = 'none';
      if (infoEl)  infoEl.textContent = '등급컷 정보를 불러오지 못했습니다.';
    }
  }
  function computeRemindTitle() {
    if (MODE === 'all') return '리마인드 — 전체 유저 응시 목록';
    if (MODE === 'one') {
      const name = getSelectedUserName();
      return `리마인드 — ${(name || '선택 사용자')} 응시 목록`;
    }
    return '리마인드 — 내 응시 목록';
  }

  // ========== 점수/백분위 추이 ==========
  async function drawTrend(){
    try{
      const admin      = isAdmin();
      const subjectId  = $('#subjectId').value || '';
      const subclassId = $('#subclassId').value || '';
      const csatDate   = $('#csatDate').value || '';
      const selUserId  = $('#userSelect')?.value || '';

      const qs = new URLSearchParams();
      if (subjectId && subjectId !== '*') qs.set('subjectId', subjectId);
      if (subclassId) qs.set('subclassId', subclassId);
      if (csatDate)   qs.set('csatDate', csatDate);
      const qsStr = qs.toString();

      // ==== 데이터 불러오기 (기존 로직 유지) ====
      let arr;
      if (readMode() === 'all') {
        const rows = (Array.isArray(LAST_ROWS) && LAST_ROWS.length)
          ? LAST_ROWS
          : await fetchAllRows();
        arr = rows
          .slice()
          .sort((a,b)=> new Date(a.testDate) - new Date(b.testDate))
          .map(r => ({
            value: Number(r.result ?? r.score ?? 0),
            csatDate: r.csatDate ?? '',
            testDate: r.testDate ?? ''
          }));
      } else {
        let uid = (readMode() === 'one') ? (selUserId || null) : (getUidForApi() || null);
        if (!uid) { setText('trendInfo','사용자 선택 필요'); if (trendChart) trendChart.destroy(); return; }
        const url = `/api/stats/user/${encodeURIComponent(uid)}/attempt-trend${qsStr ? `?${qsStr}` : ''}`;
        const res = await fetch(url, { headers: authHeaders() });
        if (!res.ok) throw new Error('trend ' + res.status);
        arr = await res.json();
      }

      const normalize = (x) => {
        if (x == null) return 0;
        if (typeof x === 'number') return x;
        if (typeof x === 'string' && !isNaN(x)) return Number(x);
        if (typeof x === 'object') {
          const v = x.value ?? x.result ?? x.score ?? x.avgScore ?? x.avg_score ?? x.average ?? x.percentile ?? 0;
          return Number(v) || 0;
        }
        return 0;
      };

      const values    = arr.map(normalize);
      const csatDates = arr.map(x => (x && typeof x === 'object') ? (x.csatDate ?? '') : '');
      const testDates = arr.map(x => (x && typeof x === 'object') ? (x.testDate ?? '') : '');
      const labels    = arr.map((_, i) => `${i+1}회차`);

      // 점수/백분위 판단 (기존)
      const usingPercentile =
        arr.some(o => o && typeof o === 'object' && o.percentile != null) &&
        !arr.some(o => o && typeof o === 'object' && (o.result != null || o.score != null || o.avgScore != null || o.avg_score != null));

      const maxVal = Math.max(0, ...values);
      const yMax = usingPercentile ? 100 : (maxVal <= 1 ? 1 : maxVal <= 10 ? 10 : Math.ceil((maxVal + 5) / 10) * 10);

      // ==== 등급 오버레이: 과목+세부과목+응시월 모두 고르면 표시 ====
      let gradeData = null;
      if (!usingPercentile && subjectId && subclassId && csatDate) {
        try {
          const cuts = await fetchGradeCuts(csatDate, subclassId);
          if (cuts) {
            // 각 점수 → 등급(1~9)
            gradeData = values.map(v => gradeFromScore(v, cuts));
          }
        } catch (e) {
          console.warn('grade cuts for trend failed', e);
        }
      }

      // 제목
      const titleEl = document.getElementById('trendTitle');
      if (titleEl) {
        if (readMode() === 'all') {
          titleEl.textContent = usingPercentile ? '전체 백분위 추이(개별 시도)' : (gradeData ? '전체 점수·등급 추이(개별 시도)' : '전체 점수 추이(개별 시도)');
        } else if (readMode() === 'one') {
          titleEl.textContent = usingPercentile ? '선택 사용자 백분위 추이' : (gradeData ? '선택 사용자 점수·등급 추이' : '선택 사용자 점수 추이');
        } else {
          titleEl.textContent = usingPercentile ? '내 백분위 추이' : (gradeData ? '내 점수·등급 추이' : '내 점수 추이');
        }
      }

      // 캔버스 폭 자동
      const canvas = trendCtx.canvas;
      let wrap = document.getElementById('trendScroll');
      if (!wrap) {
        wrap = document.createElement('div');
        wrap.id = 'trendScroll';
        wrap.style.overflowX = 'auto';
        wrap.style.width = '100%';
        wrap.style.paddingBottom = '4px';
        canvas.parentElement.insertBefore(wrap, canvas);
        wrap.appendChild(canvas);
      }
      const perPoint = 110;
      const minWidth = Math.max(wrap.clientWidth, perPoint * Math.max(1, labels.length));
      canvas.style.width  = `${minWidth}px`;
      canvas.style.height = `320px`;

      // ==== 데이터셋 구성 (점수 + [옵션] 등급) ====
      const datasets = [{
        label: usingPercentile ? '백분위(%)' : '점수',
        data: values,
        tension: .25,
        pointRadius: 3,
        borderColor: COLOR_SCORE,
        backgroundColor: COLOR_SCORE,
        yAxisID: 'y'
      }];

      if (gradeData) {
        datasets.push({
          label: '등급',
          data: gradeData,
          tension: 0,               // 계단 느낌으로 보이게 하고 싶으면 0
          stepped: true,            // 등급은 구간 값이라 계단선이 보기 좋음
          pointRadius: 3,
          borderColor: COLOR_GRADE,
          backgroundColor: COLOR_GRADE,
          yAxisID: 'y2'
        });
      }

      // ==== 축 설정: 좌(점수=파랑) / 우(등급=초록) ====
      const scales = {
        x: { title: { display: true, text: '회차 (응시 순서)' } },
        y: {
          beginAtZero: true,
          suggestedMax: yMax,
          title: { display: true, text: usingPercentile ? '백분위(%)' : '점수', color: COLOR_SCORE },
          ticks: { color: COLOR_SCORE }
        }
      };

      if (gradeData) {
        scales.y2 = {
          position: 'right',
          min: 1,
          max: 9,
          reverse: true,                 // ✅ 1등급이 위, 9등급이 아래
          ticks: {
            stepSize: 1,
            color: COLOR_GRADE,
            callback: v => `${v}등급`    // 보기 좋게 라벨
          },
          title: { display: true, text: '등급', color: COLOR_GRADE },
          grid: { drawOnChartArea: false }
        };
      }

      if (trendChart) trendChart.destroy();
      trendChart = new Chart(trendCtx, {
        type: 'line',
        data: { labels, datasets },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          scales,
          plugins: {
            legend: { display: !!gradeData }, // 등급 있을 때만 범례 표시
            tooltip: {
              callbacks: {
                title: (items)=>{ const i=items[0].dataIndex;
                  const t=[`${i+1}회차`];
                  if (testDates[i]) t.push(`${formatDateTimeExact(testDates[i])} 응시`);
                  if (csatDates[i]) t.push(`모의고사 ${csatDates[i]}`);
                  return t;
                },
                label: (it)=> {
                  const v = it.parsed.y;
                  if (it.dataset.yAxisID === 'y2') return `등급 ${Math.round(v)}등급`;
                  return usingPercentile ? `점수 ${Math.round(v)}%` : `점수 ${Math.round(v)}`;
                }
              }
            },
            // 점 위 숫자 라벨: 점수(좌측 라인)에만, 파란색으로
            valueLabel: {
              datasetIndex: 0,
              offset: 8,
              maxPoints: 60,
              font: { size: 11, weight: '700' },
              color: COLOR_SCORE,
              formatter: v => usingPercentile ? `${Math.round(v)}%` : `${Math.round(v)}`
            }
          }
        }
      });

      setText('trendInfo', readMode()==='all' ? `집계 포인트 ${arr.length}개` : `총 ${arr.length}회 시행`);
      requestAnimationFrame(()=>{ wrap.scrollLeft = wrap.scrollWidth - wrap.clientWidth; });
    } catch(e){
      console.warn(e);
      setText('trendInfo','트렌드 불러오기 실패');
      if (trendChart) trendChart.destroy();
    }
  }


  // ========== 관리자 KPI ==========
  function computeAdminKpis(rows){
    const totalUsers = new Set(rows.map(r=> String(r.userId))).size;
    const attemptsByUser = rows.reduce((m,r)=>{
      const k = String(r.userId);
      m.set(k, (m.get(k)||0) + 1);
      return m;
    }, new Map());
    const multiTakers = Array.from(attemptsByUser.values()).filter(n => n >= 2).length;
    const totalAttempts = rows.length;

    const avgAttemptsPerUser = totalUsers ? (totalAttempts / totalUsers) : 0;
    const retakeRate = totalUsers ? (multiTakers / totalUsers) * 100 : 0;

    setText('adminRetakeRate', `${toFixed(retakeRate, 1)}%`);
    setText('adminParticipants', totalUsers);
    setText('adminReusers', multiTakers);
    setText('adminAvgAttempts', toFixed(avgAttemptsPerUser, 2));
  }



  // ========== 등급표(모달) — ★ 잘못된 행 템플릿 수정 ==========
  function getMyLatestScore(rows){
    const uid = getUidFromToken();
    if (!uid) return null;
    const mine = rows.filter(r => String(r.userId) === String(uid));
    if (!mine.length) return null;
    mine.sort((a,b)=> new Date(b.testDate) - new Date(a.testDate));
    return Number(mine[0].result || 0);
  }
  function openGradeModal(cuts, myGrade, myScore){
    let modalEl = document.getElementById('gradeModal');
    if (!modalEl){
      const html = `
      <div class="modal fade" id="gradeModal" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog">
          <div class="modal-content">
            <div class="modal-header">
              <h5 class="modal-title">등급표</h5>
              <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
            </div>
            <div class="modal-body">
              <div id="gradeSummary" class="mb-2"></div>
              <table class="table table-sm">
                <thead><tr><th>등급</th><th>컷 점수</th></tr></thead>
                <tbody id="gradeTbody"></tbody>
              </table>
            </div>
          </div>
        </div>
      </div>`;
      document.body.insertAdjacentHTML('beforeend', html);
      modalEl = document.getElementById('gradeModal');
    }

    const tb = modalEl.querySelector('#gradeTbody');
    tb.innerHTML = '';

    // 관리자라면 '최근 점수', 아니면 '내 점수'
    const scoreLabel = isAdmin() ? '최근 점수' : '내 점수';
    for (let g=1; g<=9; g++){
      const cut = cuts['cut'+g];
      if (cut == null) continue;
      const mark = (myGrade === g) ? ' class="table-primary"' : '';
      tb.insertAdjacentHTML('beforeend', `<tr${mark}><td>${g}등급</td><td>${cut}</td></tr>`);
    }
    const summary = modalEl.querySelector('#gradeSummary');
    summary.textContent = (myScore!=null && myGrade!=null)
      ? `${scoreLabel} ${myScore}점 → ${myGrade}등급`
      : `등급표`;

    const modal = new bootstrap.Modal(modalEl);
    modal.show();
  }
  async function updateGradeUI(rows){
    const subjectId  = $('#subjectId').value || '';
    const subclassId = $('#subclassId').value || '';
    const csatDate   = $('#csatDate').value || '';
    const kpiCut = $('#kpiCut1');
    if (!kpiCut) return;

    kpiCut.textContent = '-';
    kpiCut.style.textDecoration = 'none';
    kpiCut.style.cursor = 'default';
    kpiCut.onclick = null;

    if (!subjectId || !subclassId || !csatDate) return;

    try{
      const cuts = await fetchGradeCuts(csatDate, subclassId);
      const myScore = getMyLatestScore(rows);
      const myGrade = gradeFromScore(myScore, cuts);

      if (myGrade != null){
        kpiCut.textContent = `${myGrade}등급`;
        kpiCut.style.textDecoration = 'underline';
        kpiCut.style.cursor = 'pointer';
        kpiCut.onclick = () => openGradeModal(cuts, myGrade, myScore);
      } else if (cuts && cuts.cut1 != null){
        kpiCut.textContent = `등급표 보기`;
        kpiCut.style.textDecoration = 'underline';
        kpiCut.style.cursor = 'pointer';
        kpiCut.onclick = () => openGradeModal(cuts, null, null);
      }
    }catch(e){
      console.warn('grade cuts load failed', e);
      kpiCut.textContent = '-';
    }
  }

  // ========== 전체 리프레시 ==========
  async function refreshAll(){
    const ADMIN = isAdmin();

    const rows = await fetchAllRows();
    LAST_ROWS = rows;

    const s = calcStats(rows);

    if (MODE === 'all') {
      show('#distCard');
      show('#adminSection');
      setText('kpiParticipants', s.participants ?? '-');
    } else if (MODE === 'one') {
      show('#distCard');        // 분포는 보여줌
      hide('#adminSection');    // 🔒 관리자 지표 숨김
    } else {
      // mine
      hide('#distCard,[data-role="admin-only"]');
      const kpiRow = document.getElementById('kpiRow');
      if (kpiRow) {
        kpiRow.style.display = 'flex';
        kpiRow.style.flexWrap = 'wrap';
        kpiRow.style.justifyContent = 'center';
        kpiRow.style.gap = '16px';
      }
    }

    setText('kpiAttempts', s.attempts ?? '-');
    setText('kpiAvg', Number.isFinite(s.avg) ? s.avg.toFixed(1) : '-');

    if (MODE === 'all' || MODE === 'one') {
      if (distCtx) drawScoreDistribution(rows);
      await drawGradeCutDistribution(rows);
      computeAdminKpis(rows);
    } else {
      // mine
      if (distChart) { distChart.destroy(); distChart = null; }
      if (gradeChart) { gradeChart.destroy(); gradeChart = null; }
      setText('distInfo', '');
      setText('gradeInfo', '');
      document.getElementById('gradeBlock')?.style && (document.getElementById('gradeBlock').style.display = 'none');
      document.getElementById('gradeDivider')?.style && (document.getElementById('gradeDivider').style.display = 'none');
    }

    await drawTrend();

    await updateGradeUI(rows);
  }

  // ================== 리마인드(응시 목록) ==================
  let REVIEW_CACHE = new Map();

  function fmtDateShort(v){
    const s = formatDateTimeExact(v);
    return s ? s.slice(0, 16) : '';
  }

  async function openAttemptsModal(){
    try{
      updateRemindLabel();

      // 유저 디렉터리 보장
      if (typeof ensureUsersLoaded === 'function') {
        await ensureUsersLoaded();
      } else if (typeof loadUsersIfNeeded === 'function') {
        await loadUsersIfNeeded();
      }

      let rows = Array.isArray(LAST_ROWS) && LAST_ROWS.length ? LAST_ROWS : await fetchAllRows();

      await _hydrateUserNamesFromRows(rows);


      // role=USER만 남기기
      const onlyIds = (window.USER_DIR && USER_DIR.onlyUserIds) ? USER_DIR.onlyUserIds : null;
      if (onlyIds && onlyIds.size) {
        rows = rows.filter(r => onlyIds.has(String(r.userId)));
      }

      const tb = document.getElementById('attemptTbody');
      if (!tb) { alert('attemptTbody 엘리먼트를 찾을 수 없습니다.'); return; }

      // 헤더에 '응시자' 열 없으면 주입
      const theadRow = document.querySelector('#attemptsModal thead tr');
      if (theadRow && !theadRow.dataset.withName) {
        theadRow.innerHTML = '<th>응시자</th>' + theadRow.innerHTML;
        theadRow.dataset.withName = '1';
      }
      const colspan = (theadRow && theadRow.dataset.withName) ? 6 : 5;

      // ✅ back 파라미터: 현재 화면 경로를 /stats/user 또는 /stats/admin으로 정규화
      function currentStatsBack(){
        const path = location.pathname.replace(/\/+$/, '');
        const qs   = location.search || '';
        const mode = (document.body?.dataset?.mode || '').toLowerCase();
        if (path === '/stats') {
          if (mode === 'one') return '/stats/user'  + qs;
          if (mode === 'all') return '/stats/admin' + qs;
        }
        return path + qs;
      }
      const backParam = encodeURIComponent(currentStatsBack()); // ← 중복 선언 방지

      tb.innerHTML = rows.length ? '' :
        `<tr><td colspan="${colspan}" class="text-center text-muted">표시할 응시가 없습니다.</td></tr>`;

      rows.sort((a,b)=> new Date(b.testDate) - new Date(a.testDate));

      for (const r of rows){
        const examId = r.examId ?? r.id;
        const when   = fmtDateShort(r.testDate);
        const cd     = r.csatDate ?? '-';
        const sub    = r.subclassName ?? r.subclassId ?? '-';
        const score  = (r.result ?? r.score ?? '-') + '';
        const uid    = String(r.userId ?? '');
        const uname =
          (USER_DIR.map.get(uid)?.name) ||
          r.userName || r.name || r.displayName ||
          r.nickname || r.nickName || r.username || r.email ||
          r.user?.name || r.user?.nickname ||
          `user#${uid}`;

        tb.insertAdjacentHTML('beforeend', `
          <tr>
            <td>${uname}</td>
            <td>${when}</td>
            <td>${cd}</td>
            <td>${sub}</td>
            <td class="text-end">${score}</td>
            <td class="text-end">
              <a class="btn btn-sm btn-primary" href="/review/${examId}?back=${backParam}">보기</a>
            </td>
          </tr>
        `);
      }

      const modalEl = document.getElementById('attemptsModal');
      const modal = new bootstrap.Modal(modalEl);
      modal.show();
    }catch(e){
      console.error(e);
      alert('응시 목록을 불러오지 못했습니다.');
    }
  }


  // “보기” 버튼(표 안) → 새 페이지 이동(개별 행에 a링크도 이미 추가)
  document.addEventListener('click', (e)=>{
    const btn = e.target.closest('.btn-view-attempt');
    if (!btn) return;
    const examId = btn.getAttribute('data-examid');
    if (!examId) return;
    window.location.href = `/review/${encodeURIComponent(examId)}`;
  });

  // ====== 리뷰 모달(예비) 이하 생략 — 기존 그대로 =====
  // (openReviewModal, ensureReviewModal 등 기존 함수들 유지)

  // ========== 부트 ==========
  function param(name){ return new URLSearchParams(location.search).get(name) || ''; }

  document.addEventListener('DOMContentLoaded', async () => {
  if (!document.body.dataset.mode) {
    document.body.dataset.mode = MODE; // 'mine' | 'all' | 'one'
  }
    // 0) 모드 맞게 잡혔는지 디버그
    console.debug('[stats] MODE=', MODE);

    // 1) 콤보 DOM 확보/배치 먼저
    ensureUserSelectorPlacement();

    // 2) 차트 컨텍스트 준비/이벤트 바인드
    distCtx  = document.getElementById('distChart')?.getContext('2d');
    trendCtx = document.getElementById('trendChart')?.getContext('2d');
    gradeCtx = document.getElementById('gradeChart')?.getContext('2d');
    document.getElementById('refreshAll')?.addEventListener('click', refreshAll);
    document.getElementById('btnReview')?.addEventListener('click', openAttemptsModal);

    // 3) (one일 때만) 사용자 목록 로드
    await loadUsersIfNeeded();

    // 4) 라벨 갱신
    updateRemindLabel();

    document.getElementById('userSelect')?.addEventListener('change', async ()=>{
      // 선택 사용자 바뀌면 회차도 달라질 수 있음
      await loadCsatDatesForCurrentSelection();
      await refreshAll();
      updateRemindLabel(); // (NEW) 라벨 즉시 갱신
    });

    document.getElementById('subjectId')?.addEventListener('change', async (e)=>{
      const v = e.target.value;
      const sc = document.getElementById('subclassId');
      if (v && v !== '*') {
        const res = await fetch(`/api/lookup/subclasses?subjectId=${v}`, { headers: authHeaders() });
        const list = res.ok ? await res.json() : [];
        sc.innerHTML = '<option value="">세부과목 선택</option>' + list.map(s=>`<option value="${s.id}">${s.name}</option>`).join('');
        sc.disabled = false;
      } else {
        sc.innerHTML = '<option value="">세부과목 선택</option>';
        sc.disabled = false;
      }
      await loadCsatDatesForCurrentSelection();
    });
    document.getElementById('subclassId')?.addEventListener('change', async ()=> {
      await loadCsatDatesForCurrentSelection();
    });

    try { await loadSubjects(); }
    catch (e) { console.error(e); alert('과목 목록을 불러오지 못했습니다. /api/lookup/subjects 확인 필요'); }

      const pSubject = param('subjectId');
      const pSubclass = param('subclassId');
      const pCsat = param('csatDate');
      const pUserId = param('userId');
      if (MODE === 'one' && pUserId) {
        const sel = document.getElementById('userSelect');
        if (sel) sel.value = pUserId;
      }



       if (pSubject) {
          const sel = document.getElementById('subjectId');
          if (sel) sel.value = pSubject;
          // 세부과목 로드 후 값 반영
          if (sel && pSubject !== '*') {
            const res = await fetch(`/api/lookup/subclasses?subjectId=${pSubject}`, { headers: authHeaders() });
            const list = res.ok ? await res.json() : [];
            const sc = document.getElementById('subclassId');
            sc.innerHTML = '<option value="">세부과목 선택</option>' + list.map(s=>`<option value="${s.id}">${s.name}</option>`).join('');
            sc.disabled = false;
            if (pSubclass) sc.value = pSubclass;
          }
        }

        await loadCsatDatesForCurrentSelection();
        if (pCsat) {
          const cs = document.getElementById('csatDate');
          if (cs) cs.value = pCsat;
        }

        await refreshAll();

    if (MODE === 'mine') {
      hide('#adminSection,[data-role="admin-only"]');
      hide('#distCard');
    } else if (MODE === 'one') {
      hide('#adminSection');  // 🔒 관리자 지표 숨김
      show('#distCard');
    } else { // all
      show('#adminSection');
      show('#distCard');
    }

    // 응시자 수 카드는 all 이외 숨김
    const participantsCol = document.getElementById('kpiParticipants')?.closest('[data-role="admin-only"]');
    if (participantsCol) participantsCol.style.display = (MODE === 'all') ? '' : 'none';
  });
})();
