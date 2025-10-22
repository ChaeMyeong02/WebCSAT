/* /js/stats.js */
(() => {
  // === 만점(퍼센트 계산) 관련 전역 설정 ===
  const DEFAULT_MAX_SCORE = 100;
  // null: 모름, false: 지원 안 함 -> 네트워크 호출 스킵
//  let PAPER_SCORE_FETCH_AVAILABLE = null;

const PAPER_SCORE_CACHE = new Map();
let PAPER_SCORE_FETCH_AVAILABLE = null; // null=모름, true/false=결과 기억
let QUESTIONS_ENDPOINT_AVAILABLE = null; // ← 추가: /api/questions 존재 여부 기억

function resolveSubjectIdForRow(r) {
  // ① row → ② SUBCLASS_DIR(세부과목→과목) → ③ 선택된 UI 값
  return (
    r.subjectId ?? r.subject_id ??
    SUBCLASS_DIR.get(String(r.subclassId ?? r.subclass_id ?? ''))?.subjectId ??
    (document.getElementById('subjectId')?.value || null)
  );
}


  // ✅ 과목/세부과목/응시월이 전부 기본값이면 true
  function isAllDefaultSelection(){
    const sid = document.getElementById('subjectId')?.value || '';
    const sc  = document.getElementById('subclassId')?.value || '';
    const cs  = document.getElementById('csatDate')?.value || '';
    const sidAll = !sid || sid === '*';
    return sidAll && !sc && !cs;
  }

  // ✅ 전부 숨김(차트 파괴 + 카드/섹션 숨김 + 안내문)
  function hideAllStatsUI(msg=''){
    try { if (distChart)  { distChart.destroy();  distChart  = null; } } catch {}
    try { if (gradeChart) { gradeChart.destroy(); gradeChart = null; } } catch {}
    try { if (trendChart) { trendChart.destroy(); trendChart = null; } } catch {}

    const trendWrap = document.getElementById('trendCard')
      || document.getElementById('trendScroll')
      || document.getElementById('trendChart')?.closest('.card')
      || document.getElementById('trendChart')?.parentElement;

    document.getElementById('distCard')     && (document.getElementById('distCard').style.display = 'none');
    document.getElementById('gradeBlock')   && (document.getElementById('gradeBlock').style.display = 'none');
    document.getElementById('gradeDivider') && (document.getElementById('gradeDivider').style.display = 'none');
    if (trendWrap) trendWrap.style.display = 'none';

    const adminSec = document.getElementById('adminSection');
    if (adminSec) adminSec.style.display = 'none';
    const kpiRow = document.getElementById('kpiRow');
    if (kpiRow) kpiRow.style.display = 'none';

    const set = id => { const el=document.getElementById(id); if (el) el.textContent = msg; };
    set('distInfo'); set('gradeInfo'); set('trendInfo');

    const cut = document.getElementById('kpiCut1');
    if (cut) { cut.textContent='-'; cut.style.textDecoration='none'; cut.style.cursor='default'; cut.onclick=null; }
  }

  function showTrendContainer() {
    const trendWrap =
      document.getElementById('trendCard') ||
      document.getElementById('trendScroll') ||
      document.getElementById('trendChart')?.closest('.card') ||
      document.getElementById('trendChart')?.parentElement;
    if (trendWrap) trendWrap.style.display = '';
  }

  // ==== 월 필터(연/월) 상태 ====
  let FILTER_YEAR  = null; // 숫자(예: 2025) | null(전체)
  let FILTER_MONTH = null; // 1..12 | null(전체)

  // 안전한 Year/Month 파서(타임존 영향 최소화)
  function _ymFromTestDate(v){
    if (!v) return null;
    const s = String(v);
    const m = s.match(/^(\d{4})-(\d{2})/); // "YYYY-MM-..." 형태면 바로 사용
    if (m)   return { y: Number(m[1]), m: Number(m[2]) };
    const d = new Date(s);
    if (isNaN(d)) return null;
    return { y: d.getFullYear(), m: d.getMonth()+1 };
  }

  // rows -> 월필터 적용
  function applyMonthFilter(rows){
    if (!FILTER_YEAR && !FILTER_MONTH) return rows;
    return (rows || []).filter(r => {
      const ym = _ymFromTestDate(r.testDate);
      if (!ym) return false; // testDate 파싱 실패 시 제외
      if (FILTER_YEAR && ym.y !== Number(FILTER_YEAR)) return false;
      if (FILTER_MONTH && ym.m !== Number(FILTER_MONTH)) return false;
      return true;
    });
  }

  // UI가 없으면 자동 생성(제목 아래)
  function ensureMonthFilterUI(){
    let row = document.getElementById('monthFilterRow');
    if (!row){
      row = document.createElement('div');
      row.id = 'monthFilterRow';
      row.className = 'd-flex align-items-center gap-2 flex-wrap mb-3';
      row.style.display = 'none';
      row.innerHTML = `
        <label class="form-label m-0">기간</label>
        <select id="yearSelect" class="form-select form-select-sm" style="width:auto; min-width:120px"></select>
        <div id="monthStrip" class="d-flex gap-2 flex-wrap"></div>
      `;
      const titleWrap = document.querySelector('.container .d-flex.align-items-center.mb-3');
      (titleWrap || document.querySelector('.container') || document.body).insertAdjacentElement('afterend', row);
    }

    // 이벤트 바인딩(한 번만)
    const sel = document.getElementById('yearSelect');
    const strip = document.getElementById('monthStrip');
    if (sel && !sel._bound){
      sel.addEventListener('change', () => {
        FILTER_YEAR  = sel.value ? Number(sel.value) : null;
        renderMonthStrip(LAST_ROWS_RAW || []); // 이용가능 월 on/off 다시 계산
        refreshAll();
      });
      sel._bound = true;
    }
    if (strip && !strip._bound){
      strip.addEventListener('click', (e) => {
        const btn = e.target.closest('[data-m]');
        if (!btn) return;
        const clicked = btn.dataset.m ? Number(btn.dataset.m) : null;
        FILTER_MONTH = (FILTER_MONTH === clicked ? null : clicked); // 같은 월 재클릭시 해제
        [...strip.querySelectorAll('[data-m]')].forEach(b => {
          const m = b.dataset.m ? Number(b.dataset.m) : null;
          b.classList.toggle('btn-primary', m === FILTER_MONTH);
          b.classList.toggle('btn-outline-primary', m !== FILTER_MONTH);
        });
        refreshAll();
      });
      strip._bound = true;
    }
    row.style.display = '';
  }

  // 연도/월 UI 그림
  function renderMonthStrip(baseRows){
    ensureMonthFilterUI();
    const sel   = document.getElementById('yearSelect');
    const strip = document.getElementById('monthStrip');
    if (!sel || !strip) return;

    const yms = (baseRows || [])
      .map(r => _ymFromTestDate(r.testDate))
      .filter(Boolean);

    const years = [...new Set(yms.map(o => o.y))].sort((a,b) => b - a);
    sel.innerHTML = ['<option value="">전체 연도</option>']
      .concat(years.map(y => `<option value="${y}" ${Number(FILTER_YEAR)===y?'selected':''}>${y}년</option>`))
      .join('');

    const available = new Set(
      yms.filter(o => !FILTER_YEAR || o.y === Number(FILTER_YEAR)).map(o => o.m)
    );

    // 전체(모든 연도) 모드에서 월별 유무 판단용
    const monthsAll = new Set(yms.map(o => o.m));

    // (옵션) 월별 건수 뱃지
    const counts = new Map();
    for (const {y,m} of yms){ if (y && m){
      const k = `${y}-${m}`;
      counts.set(k, (counts.get(k) || 0) + 1);
    }}

    function btn(m, label){
      const isActive = (m==null && FILTER_MONTH==null) || (m!=null && Number(FILTER_MONTH)===m);

      // ✅ 데이터 유무 → 색상 결정: 있음=흰색(btn-light), 없음=회색(btn-secondary)
      const hasData = FILTER_YEAR
        ? (m==null ? available.size>0 : available.has(m))       // 선택된 연도 내
        : (m==null ? yms.length>0 : monthsAll.has(m));          // 전체 연도 기준

      const cls = isActive
        ? 'btn btn-sm btn-primary text-white'                   // 선택됨 = 파란색
        : (hasData ? 'btn btn-sm btn-light border fw-semibold'  // 데이터 있음 = 흰색(테두리)
                   : 'btn btn-sm btn-secondary text-white');    // 데이터 없음 = 회색

      const key   = (m!=null && FILTER_YEAR) ? `${Number(FILTER_YEAR)}-${m}` : null;
      const count = (m!=null && key && counts.get(key)) || 0;
      const badge = (m!=null && count>0)
        ? ` <span class="badge bg-primary-subtle text-primary-emphasis">${count}</span>` : '';

      return `<button type="button" class="${cls}" data-m="${m??''}">${label}${badge}</button>`;
    }

    strip.innerHTML = [ btn(null,'전체보기') ]
      .concat(Array.from({length:12}, (_,i)=>btn(i+1, `${i+1}월`)))
      .join('');
  }


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

  async function buildScorePercents(rows) {
    // rows → [{ raw, csatDate, subclassId, subjectId, max }]
    const arr = (rows || []).map(r => {
      const raw = Number(r.result ?? r.score ?? 0);
      const max = readMaxScore(r); // row에 들어있다면 사용
      const cs  = r.csatDate ?? '';
      const sc  = r.subclassId ?? r.subclass_id ?? null;
      const sid = resolveSubjectIdForRow(r);
      return { raw, max, csatDate: cs, subclassId: sc, subjectId: sid };
    });

    // raw > max 같은 이상치 → max 무효화(재하이드레이션 유도)
    for (const o of arr) {
      if (Number.isFinite(o.raw) && Number.isFinite(o.max) && o.raw > o.max) o.max = null;
    }

    // (csatDate, subclassId, subjectId) 묶음으로 한 번씩만 만점 조회
    const need = new Map();
    for (const o of arr) {
      if (!o.max && o.csatDate && o.subclassId) {
        const key = `${o.csatDate}::${o.subclassId}::${o.subjectId ?? ''}`;
        if (!need.has(key)) need.set(key, { csatDate: o.csatDate, subclassId: o.subclassId, subjectId: o.subjectId });
      }
    }
    for (const meta of need.values()) {
      // subjectId 필수 API면 누락 시 스킵(폴백으로 100 사용)
      if (!meta.subjectId) continue;
      const ps = await fetchPaperScore(meta.csatDate, meta.subclassId, meta.subjectId);
      if (Number.isFinite(ps) && ps > 0) {
        for (const o of arr) {
          if (!o.max && o.csatDate === meta.csatDate && String(o.subclassId) === String(meta.subclassId)) {
            o.max = ps;
          }
        }
      }
    }

    // 퍼센트 변환
    return arr.map(o => {
      const denom = (Number.isFinite(o.max) && o.max > 0) ? o.max : DEFAULT_MAX_SCORE; // 최후 폴백 100
      const p = (denom > 0) ? (o.raw / denom) * 100 : 0;
      return Math.max(0, Math.min(100, p));
    });
  }

  async function calcPercentStats(rows) {
    const attempts = rows.length;
    const participants = new Set(rows.map(r => r.userId)).size;
    const percents = await buildScorePercents(rows);

    const BIN = 10;
    const labels = Array.from({ length: BIN }, (_, i) => `${i * 10}-${(i + 1) * 10}%`);
    const counts = Array(BIN).fill(0);
    for (const p of percents) {
      const idx = Math.max(0, Math.min(9, Math.floor(p / 10)));
      counts[idx]++;
    }
    const avg = percents.length
      ? Math.round((percents.reduce((a, b) => a + b, 0) / percents.length) * 10) / 10
      : 0;

    return { attempts, participants, labels, counts, avg, percents };
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

  async function fetchJSON(url, opts = {}) {
    const res = await fetch(url, opts);
    const status = res.status;
    const ok = res.ok;
    const ct = (res.headers.get('content-type') || '').toLowerCase();
    const text = await res.text();

    let json = null;
    if (ct.includes('json')) {
       try { json = JSON.parse(text); } catch {}
     } else {
       // 헤더가 틀려도 바디가 JSON처럼 보이면 파싱 시도
       const t = text.trim();
       if (t.startsWith('{') || t.startsWith('[')) {
         try { json = JSON.parse(t); } catch {}
       }
     }
     // 디버그: JSON이 아니면 처음 120자 찍어 확인
     if (ok && !json) {
       console.warn('[fetchJSON] Non-JSON body', { url, status, ct, first: (text || '').slice(0,120) });
     }
    return { ok, status, json, ct, text };
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

  // ==== (상단 전역 부근) 과목/세부과목/만점 캐시 ====
  let SUBJECT_DIR = new Map();   // subjectId -> { name }
  let SUBCLASS_DIR = new Map();  // subclassId -> { name, count, subjectId }
  // 파일 상단 전역 근처에 한 번만 선언
//  const PAPER_SCORE_CACHE = new Map();
//  let PAPER_SCORE_FETCH_AVAILABLE = null; // null=모름, true/false=결과 기억

  async function fetchPaperScore(csatDate, subclassId, subjectId) {
    const key = `${csatDate ?? ''}::${subclassId ?? ''}::${subjectId ?? ''}`;
    if (PAPER_SCORE_CACHE.has(key)) return PAPER_SCORE_CACHE.get(key);

    const fb0 = getFallbackPaperScore(subjectId, subclassId);
    if (Number.isFinite(fb0)) { PAPER_SCORE_CACHE.set(key, fb0); return fb0; }

    if (PAPER_SCORE_FETCH_AVAILABLE === false) { PAPER_SCORE_CACHE.set(key, null); return null; }

    const qs = new URLSearchParams();
    if (subclassId) qs.set('subclassId', subclassId);
    if (subjectId)  qs.set('subjectId', subjectId);
    if (csatDate)   qs.set('csatDate', csatDate);

    // 1) 서버가 주는 값은 받아만 둔다(즉시 return 금지)
    let direct = null;
    try {
      const r3 = await fetchJSON(`/api/lookup/paper-score?` + qs.toString(), { headers: authHeaders() });
      if (r3.status === 404) { PAPER_SCORE_FETCH_AVAILABLE = false; }
      if (r3.ok && r3.json != null) {
        const n = Number(r3.json?.paperScore ?? r3.json?.totalScore ?? r3.json);
        if (Number.isFinite(n) && n > 0) {
          PAPER_SCORE_FETCH_AVAILABLE = true;
          direct = n; // ← 바로 return 하지 말고 보류
        }
      }
    } catch (e) {
      console.warn('paper-score fetch failed', e);
    }

    // 2) 폴백: /api/questions에서 공통(0)+선택(1) 합산
    let total = null;

    // /api/questions가 404로 확인된 적 있으면 더 이상 시도하지 않음
    if (QUESTIONS_ENDPOINT_AVAILABLE !== false) {
      try {
        const q = new URLSearchParams();
        if (subjectId) q.set('subjectId', subjectId);   // 공통(0) 매칭용
        if (csatDate)  q.set('csatDate', csatDate);
        q.set('size', '10000');

        const resQ = await fetchJSON('/api/questions?' + q.toString(), { headers: authHeaders() });

        // 한번 404면 기억해두고 다음부터는 조용히 스킵
        if (resQ.status === 404) {
          QUESTIONS_ENDPOINT_AVAILABLE = false;
        }

        if (resQ.ok && resQ.json) {
          const list = Array.isArray(resQ.json) ? resQ.json
            : Array.isArray(resQ.json?.content) ? resQ.json.content
            : Array.isArray(resQ.json?.data)    ? resQ.json.data
            : Array.isArray(resQ.json?.items)   ? resQ.json.items
            : Array.isArray(resQ.json?.results) ? resQ.json.results : [];

          const sum = sumPaperFromQuestions(list, { csatDate, subjectId, subclassId });
          if (Number.isFinite(sum) && sum > 0) total = sum; // 합산이 있으면 우선 사용
        }
      } catch (e) {
        console.warn('paper-score question-sum failed', e);
      }
    }

    // 3) 최종 선택: 합산 > direct > 폴백
    if (total == null && Number.isFinite(direct) && direct > 0) total = direct;

    if (total != null) { PAPER_SCORE_CACHE.set(key, total); return total; }

    const fb = getFallbackPaperScore(subjectId, subclassId);
    if (Number.isFinite(fb)) { PAPER_SCORE_CACHE.set(key, fb); return fb; }
    PAPER_SCORE_CACHE.set(key, null);
    return null;

    // ↓ 기존 그대로
    function sumPaperFromQuestions(list, { csatDate, subjectId, subclassId }) {
      const get = (o,k) => o?.[k] ?? o?.[k.replace(/[A-Z]/g, m => '_'+m.toLowerCase())];
      let sum = 0;
      for (const q of (Array.isArray(list) ? list : [])) {
        if (csatDate && String(get(q,'csatDate')) !== String(csatDate)) continue;
        const opt = Number(get(q,'optional') ?? 0);
        const sid = String(get(q,'subjectId')  ?? '');
        const sc  = String(get(q,'subclassId') ?? '');
        const pt  = Number(get(q,'point') ?? get(q,'score') ?? get(q,'maxPoint') ?? 0);
        if (!Number.isFinite(pt) || pt <= 0) continue;

        // 공통(0)은 subjectId, 선택(1)은 subclassId로 매칭해서 합산
        if (opt === 0 && subjectId && sid === String(subjectId)) sum += pt;
        else if (opt === 1 && subclassId && sc === String(subclassId)) sum += pt;
      }
      return sum > 0 ? sum : null;
    }
  }

const PAPER_SCORE_FALLBACKS = (typeof window !== 'undefined' && window.PAPER_SCORE_FALLBACKS)
  ? window.PAPER_SCORE_FALLBACKS
  : { bySubclass: {}, bySubject: {} };

  function getFallbackPaperScore(subjectId, subclassId){
    const F = (typeof PAPER_SCORE_FALLBACKS === 'object' && PAPER_SCORE_FALLBACKS)
      ? PAPER_SCORE_FALLBACKS : { bySubclass:{}, bySubject:{} };
    const a = F.bySubclass?.[String(subclassId ?? '')];
    if (Number.isFinite(a)) return a;
    const b = F.bySubject?.[String(subjectId ?? '')];
    if (Number.isFinite(b)) return b;
    return null;
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

  // ==== 값 라벨 플러그인 (기본: 전부 표시, 클릭 시 해당 포인트만 표시) ====
  const ValueLabelPlugin = {
    id: 'valueLabel',
    afterDatasetsDraw(chart, _args, opts) {
      const { ctx, chartArea } = chart;

      const dsIndex = opts?.datasetIndex ?? 0;
      const ds = chart.data.datasets?.[dsIndex];
      const meta = chart.getDatasetMeta(dsIndex);
      if (!ds || !meta || meta.hidden) return;

      // ★ 클릭 포커스: null이면 전체 표시, 숫자면 해당 index만
      const respectClickFocus = !!opts?.respectClickFocus;
      const focusIdx = respectClickFocus ? chart.$clickedIndex : null;

      // 너무 많을 때 생략 (포커스일 땐 무시)
      const maxPoints = opts?.maxPoints ?? 60;
      if (!Number.isInteger(focusIdx) && (ds.data?.length || 0) > maxPoints) return;

      // 스타일
      const fmt    = opts?.formatter || ((v)=> String(Math.round(v)));
      const offset = opts?.offset ?? 8;
      const color  = opts?.color  ?? '#0b1220';
      const font   = opts?.font   || { size: 11, weight: '600' };

      ctx.save();
      ctx.font = `${font.weight || ''} ${font.size || 11}px ${font.family || 'system-ui, -apple-system, Segoe UI, Roboto, sans-serif'}`;
      ctx.textAlign = 'center';
      ctx.textBaseline = 'bottom';
      ctx.fillStyle = color;

      const drawOne = (i) => {
        const val = ds.data?.[i];
        const elem = meta.data?.[i];
        if (val == null || isNaN(val) || !elem) return;

        const { x, y } = elem.getProps(['x','y'], true);
        let yy = y - offset;
        if (yy < chartArea.top + 6) yy = chartArea.top + 6;

        const text = fmt(val, i, ds, chart);

        // 하얀 외곽선 + 채움
        ctx.lineWidth = 3;
        ctx.strokeStyle = 'rgba(255,255,255,.9)';
        ctx.strokeText(text, x, yy);
        ctx.fillText(text, x, yy);
      };

      if (Number.isInteger(focusIdx)) {
        drawOne(focusIdx);               // 클릭된 한 점만
      } else {
        for (let i = 0; i < meta.data.length; i++) drawOne(i); // 전체
      }
      ctx.restore();
    }
  };


  function pairKey(csatDate, subclassId){ return `${csatDate}::${subclassId}`; }


  function readMaxScore(o){
    const cand = [
      o?.paperScore, o?.maxScore, o?.totalScore, o?.fullScore, o?.perfectScore,
      o?.max_point, o?.total_point, o?.scoreMax
    ];
    for (const v of cand) { const n = Number(v); if (Number.isFinite(n) && n > 0) return n; }
    return null; // 기본값은 퍼센트 계산부에서 최후에만 100 사용
  }
  function readSubject(o){
    return (
      o?.subclassName ?? o?.subjectName ?? o?.subclassId ?? o?.subjectId ?? ''
    );
  }
  function subjectNameById(id){
    const rec = SUBJECT_DIR.get(String(id));
    return rec?.name || '';
  }
  function subclassNameById(id){
    const rec = SUBCLASS_DIR.get(String(id));
    return rec?.name || '';
  }

  // === 클릭 라벨 플러그인(라운드 말풍선 + 화살표 + 경계 클램프) ===
  const ClickCalloutPlugin = {
    id: 'clickCallout',
    afterDatasetsDraw(chart, args, opts){
      const idx = (typeof chart.$clickedIndex === 'number') ? chart.$clickedIndex : null;
      if (idx == null) return;

      const { ctx, chartArea } = chart;
      const meta = chart.getDatasetMeta(0);
      const elem = meta?.data?.[idx];
      if (!elem) return;

      // 라벨 텍스트 만들기
      const rawText = (opts?.getLabel?.(idx, chart) || '').toString();
      if (!rawText) return;

      // 라벨 스타일
      const pad = 8;
      const lineH = 18;
      const radius = 8;
      const arrow = 8;            // 화살표 높이
      const gap = 12;             // 점과 박스 사이
      const maxW = 240;           // 라벨 최대 폭(자동 줄바꿈)

      ctx.save();
      ctx.font = '600 12px system-ui, -apple-system, Segoe UI, Roboto, sans-serif';
      ctx.textBaseline = 'top';
      ctx.textAlign = 'left';

      // 간단한 워드랩
      function wrapLines(text){
        const out = [];
        const lines = text.split('\n');
        for (const ln of lines){
          const words = ln.split(' ');
          let cur = '';
          for (const w of words){
            const test = cur ? (cur + ' ' + w) : w;
            if (ctx.measureText(test).width <= maxW - pad*2) cur = test;
            else {
              if (cur) out.push(cur);
              // 단어 하나가 너무 길면 강제 쪼개기
              if (ctx.measureText(w).width > maxW - pad*2){
                let acc = '';
                for (const ch of w){
                  const t = acc + ch;
                  if (ctx.measureText(t).width <= maxW - pad*2) acc = t;
                  else { out.push(acc); acc = ch; }
                }
                if (acc) out.push(acc);
                cur = '';
              } else {
                cur = w;
              }
            }
          }
          if (cur) out.push(cur);
        }
        return out.length ? out : [''];
      }

      const lines = wrapLines(rawText);
      const textW = Math.min(
        maxW - pad*2,
        Math.max(...lines.map(t => ctx.measureText(t).width))
      );
      const boxW = textW + pad*2;
      const boxH = lineH * lines.length + pad*2;

      const { x: px, y: py } = elem.getProps(['x','y'], true);

      // 기본: 위쪽 배치 (공간 없으면 아래쪽)
      let bx = px - boxW/2;
      let by = py - (boxH + gap + arrow);
      let placeTop = true;

      // 위가 부족하면 아래
      if (by < chartArea.top + 6) {
        by = py + gap + arrow;
        placeTop = false;
      }

      // 좌우/상하 경계 클램프
      bx = Math.max(chartArea.left + 6, Math.min(bx, chartArea.right - boxW - 6));
      if (placeTop) {
        if (by < chartArea.top + 6) by = chartArea.top + 6;
      } else {
        if (by + boxH > chartArea.bottom - 6) by = chartArea.bottom - 6 - boxH;
      }

      // 라운드 박스 경로
      function roundRectPath(x,y,w,h,r){
        const rr = Math.min(r, w/2, h/2);
        ctx.beginPath();
        ctx.moveTo(x+rr, y);
        ctx.lineTo(x+w-rr, y);
        ctx.quadraticCurveTo(x+w, y, x+w, y+rr);
        ctx.lineTo(x+w, y+h-rr);
        ctx.quadraticCurveTo(x+w, y+h, x+w-rr, y+h);
        ctx.lineTo(x+rr, y+h);
        ctx.quadraticCurveTo(x, y+h, x, y+h-rr);
        ctx.lineTo(x, y+rr);
        ctx.quadraticCurveTo(x, y, x+rr, y);
        ctx.closePath();
      }

      // 배경 + 그림자
      ctx.shadowColor = 'rgba(0,0,0,.25)';
      ctx.shadowBlur = 6;
      ctx.fillStyle = 'rgba(0,0,0,.80)';
      roundRectPath(bx, by, boxW, boxH, radius);
      ctx.fill();

      // 화살표(삼각형)
      ctx.shadowBlur = 0;
      ctx.beginPath();
      if (placeTop) {
        ctx.moveTo(px, by + boxH);
        ctx.lineTo(px - 7, by + boxH + arrow);
        ctx.lineTo(px + 7, by + boxH + arrow);
      } else {
        ctx.moveTo(px, by);
        ctx.lineTo(px - 7, by - arrow);
        ctx.lineTo(px + 7, by - arrow);
      }
      ctx.closePath();
      ctx.fill();

      // 텍스트
      ctx.fillStyle = '#fff';
      let ty = by + pad;
      const tx = bx + pad;
      for (const t of lines){
        ctx.fillText(t, tx, ty);
        ty += lineH;
      }

      // 점과 연결 리더선(얇은 선)
      ctx.strokeStyle = 'rgba(0,0,0,.8)';
      ctx.lineWidth = 1.5;
      ctx.beginPath();
      if (placeTop) {
        ctx.moveTo(px, by + boxH + arrow);
      } else {
        ctx.moveTo(px, by - arrow);
      }
      ctx.lineTo(px, py);
      ctx.stroke();

      ctx.restore();
    }
  };
  Chart.register(ClickCalloutPlugin);


  // 플러그인 등록
  Chart.register(ValueLabelPlugin);

  // 색상
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
    return readMode() === 'one' ? '응시수' : '응시수';
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

  // ========== 룩업 ==========
  async function loadSubjects() {
    let url = '/api/lookup/subjects';
    let r = await fetchJSON(url, { headers: authHeaders() });
    if (!r.ok && r.status === 404) r = await fetchJSON('/api/subjects', { headers: authHeaders() });
    if (!r.ok) throw new Error('subjects HTTP ' + (r.status || '??'));
    const j = r.json;
    const list =
      Array.isArray(j)          ? j :
      (j && Array.isArray(j.content)) ? j.content :
      (j && Array.isArray(j.data))    ? j.data :
      (j && Array.isArray(j.items))   ? j.items :
      (j && Array.isArray(j.results)) ? j.results : [];
    if (!Array.isArray(list)) throw new Error('subjects HTTP ' + (r.status || '??') + ' (bad shape)');


    // SUBJECT_DIR 메모
    SUBJECT_DIR.clear();
    (Array.isArray(list) ? list : []).forEach(s => {
      const id = s.id ?? s.subjectId ?? s.subject_id;
      const name = s.name ?? s.subjectName ?? s.subject_name ?? '(이름없음)';
      if (id != null) SUBJECT_DIR.set(String(id), { name: String(name) });
    });

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

  function fillCsatDates(list, keep){
    const cs = document.getElementById('csatDate');
    if (!cs) return;

    const wanted = keep ?? cs.dataset.keep ?? cs.value ?? '';
    const opts = ['<option value="">전체 응시월</option>']
      .concat((list||[]).map(v => `<option value="${v}">${v}</option>`));

    cs.innerHTML = opts.join('');

    const canKeep = wanted && Array.from(cs.options).some(o => o.value === wanted);
    cs.value = canKeep ? wanted : '';
    cs.disabled = !(list && list.length);
    cs.dataset.keep = canKeep ? wanted : ''; // 다음 호출까지 기억
  }

  async function loadCsatDatesForCurrentSelection(){
    const subjectId  = $('#subjectId').value || '';
    const subclassId = $('#subclassId').value || '';

    const cs   = document.getElementById('csatDate');
    const keep = cs?.value || cs?.dataset.keep || '';

    // 일단 비우되, keep 값을 기억
    fillCsatDates([], keep);

    let list = [];
    if (subclassId) {
      try {
        const r = await fetchJSON(`/api/lookup/csat-dates?subclassId=${subclassId}`, { headers: authHeaders() });
        if (r.ok && Array.isArray(r.json)) list = r.json;
      } catch {}
      if (!list.length) {
        const rows = await fetchAllRows({ ignoreCsat:true });
        list = uniqueSorted(rows.map(r => r.csatDate));
      }
    } else if (subjectId && subjectId !== '*') {
      const rows = await fetchAllRows({ ignoreCsat:true });
      list = uniqueSorted(rows.map(r => r.csatDate));
    } else {
      try {
        const r1 = await fetchJSON('/api/lookup/csat-dates-global', { headers: authHeaders() });
        if (r1.ok && Array.isArray(r1.json)) list = r1.json;
      } catch {}
      if (!list.length) {
        const rows = await fetchAllRows({ ignoreCsat:true });
        list = uniqueSorted(rows.map(r => r.csatDate));
      }
    }

    fillCsatDates(list, keep);   // ← 최종 채울 때도 keep 적용
  }

  // ========== 데이터 ==========
  async function fetchAllRows(opts = {}) {
    const { ignoreCsat = false } = opts;
    const subjectId  = $('#subjectId').value || '';
    const subclassId = $('#subclassId').value || '';
    const csatDate   = $('#csatDate').value || '';
    const selUserId  = $('#userSelect')?.value || '';

    const qs = new URLSearchParams();

    // YEAR/MONTH → from/to  (한 번만!)
    const y = window.FILTER_YEAR;
    const m = window.FILTER_MONTH;
    if (y && m) {
      const mm = String(m).padStart(2,'0');
      const from = `${y}-${mm}-01`;
      const nextY = (m===12) ? y+1 : y;
      const nextM = (m===12) ? 1 : m+1;
      const to   = `${nextY}-${String(nextM).padStart(2,'0')}-01`;
      qs.set('from', from); qs.set('to', to);
    } else if (y && !m) {
      qs.set('from', `${y}-01-01`);
      qs.set('to',   `${y+1}-01-01`);
    }

    if (subjectId && subjectId !== '*') qs.set('subjectId', subjectId);
    if (subclassId) qs.set('subclassId', subclassId);
    if (csatDate && !ignoreCsat) qs.set('csatDate', csatDate);
    if (MODE === 'one' && selUserId) qs.set('userId', selUserId);

    const r = await fetchJSON('/api/exams?' + qs.toString(), { headers: authHeaders() });
    if (!r.ok || !Array.isArray(r.json)) return [];
    let data = r.json;

    if (MODE === 'mine') {
      const uid = getUidForApi();
      if (uid) data = data.filter(r => String(r.userId) === String(uid));
    }
    if (MODE === 'one' && selUserId) {
      data = data.filter(r => String(r.userId) === String(selUserId));
    }
    if (MODE === 'all' || MODE === 'one') {
      await ensureUsersLoaded();
      if (USER_DIR.onlyUserIds.size) {
        data = data.filter(r => USER_DIR.onlyUserIds.has(String(r.userId)));
      }
    }
    return data;
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
    const r2 = await fetchJSON(`/api/lookup/grade-cuts?` + qs.toString(), { headers: authHeaders() });
    if (!r2.ok || !r2.json) return null;  // HTML이나 404면 null 처리
    return r2.json;
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

  async function drawScoreDistribution(rows){
    if (!distCtx) return;
    const { attempts, participants, labels, counts } = await calcPercentStats(rows);

    if (distChart) distChart.destroy();
    distChart = new Chart(distCtx, {
      type:'bar',
      data:{ labels, datasets:[{ label: distributionCountLabel(), data: counts }] },
      options:{
        responsive:true, maintainAspectRatio:false,
plugins:{
  legend:{display:true},
  title:{ display:true, text:`점수 분포(만점 대비 % 기준) · 응시 ${attempts}건 · 참여자 ${participants}명` }
},
scales: {
  x: { title: { display: true, text: '점수(만점 대비 %) 구간' } },
  y: { beginAtZero: true, precision: 0 }
}      }
    });

    const infoEl = document.getElementById('distInfo');
    if (infoEl) infoEl.textContent = `점수 분포(만점 대비 %) · 응시 ${attempts}건 · 참여자 ${participants}명`;
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

  // ========== 점수%(만점대비) / 등급 추이 ==========
  async function drawTrend(){
    try{
      showTrendContainer();
      const subjectId  = $('#subjectId').value || '';
      const subclassId = $('#subclassId').value || '';
      const csatDate   = $('#csatDate').value || '';

      // ✅ 모드와 상관없이 월 필터가 반영된 LAST_ROWS만 사용
      const src = Array.isArray(LAST_ROWS) ? LAST_ROWS : await fetchAllRows();
      const arr = src.slice()
        .sort((a,b)=> new Date(a.testDate) - new Date(b.testDate))
        .map(r => ({
          raw: Number(r.result ?? r.score ?? 0),
          max: readMaxScore(r),
          subclassId: r.subclassId ?? r.subclass_id ?? ($('#subclassId')?.value || null),
          subclassName: r.subclassName ?? r.subclass_name ?? null,
          subjectId: r.subjectId ?? r.subject_id ?? ($('#subjectId')?.value || null),
          csatDate: r.csatDate ?? '',
          testDate: r.testDate ?? ''
        }));
      
      // ✅ 여기로 이동: raw > max면 max 무효화하여 재하이드레이션 유도
      if (Array.isArray(arr)) {
        for (const o of arr) {
          const raw = Number(o.raw);
          const max = Number(o.max);
          if (Number.isFinite(raw) && Number.isFinite(max) && raw > max) {
            o.max = null;
          }
        }
      }

      // 데이터 없으면 깔끔하게 종료
      if (!Array.isArray(arr) || arr.length === 0) {
      setText('trendInfo','표시할 데이터가 없습니다.');
      if (trendChart) { trendChart.destroy(); trendChart = null; }
      return;
      }

      // (csatDate, subclassId) 별 만점 하이드레이션
      const pairMeta = new Map();
      for (const o of arr) {
        if (!o.max && o.csatDate && o.subclassId) {
          pairMeta.set(`${o.csatDate}::${o.subclassId}`, {
            csatDate: o.csatDate, subclassId: o.subclassId, subjectId: o.subjectId
          });
        }
      }
      for (const meta of pairMeta.values()) {
        const ps = await fetchPaperScore(meta.csatDate, meta.subclassId, meta.subjectId ?? ($('#subjectId')?.value || null));
        if (ps != null) {
          arr.forEach(o => {
            if (!o.max && o.csatDate === meta.csatDate && String(o.subclassId) === String(meta.subclassId)) o.max = ps;
          });
        }
      }

      // 원점수/만점 → % 및 표시용 메타
      const rawScores = arr.map(o => Number(o.raw) || 0);
      const maxScores = arr.map(o => Number.isFinite(o.max) && o.max > 0 ? Number(o.max) : 100); // 최후 폴백 100
      const values    = rawScores.map((raw, i) => Math.max(0, Math.min(100, (raw / (maxScores[i] || 100)) * 100)));
      const subjects  = arr.map(o => subjectNameById(o.subjectId) || $('#subjectId')?.selectedOptions?.[0]?.textContent?.trim() || '');
      const subclasses= arr.map(o => o.subclassName || subclassNameById(o.subclassId) || '');
      const csatDates = arr.map(o => o.csatDate || '');
      const testDates = arr.map(o => o.testDate || '');
      const labels    = arr.map((_, i) => `${i+1}회차`);

      // 등급 데이터(오른쪽 Y): 컷이 있을 때만, 원점수 기반
      let gradeData = null;
      if (subjectId && subclassId && csatDate) {
        try {
          const cuts = await fetchGradeCuts(csatDate, subclassId);
          if (cuts) gradeData = rawScores.map(v => gradeFromScore(v, cuts));
        } catch(e){ console.warn('grade cuts for trend failed', e); }
      }

      // 제목
      const titleEl = document.getElementById('trendTitle');
      if (titleEl) {
        if (readMode() === 'all') {
          titleEl.textContent = gradeData ? '전체 점수%(만점대비)·등급 추이(개별 시도)' : '전체 점수%(만점대비) 추이(개별 시도)';
        } else if (readMode() === 'one') {
          titleEl.textContent = gradeData ? '선택 사용자 점수%(만점대비)·등급 추이' : '선택 사용자 점수%(만점대비) 추이';
        } else {
          titleEl.textContent = gradeData ? '내 점수%(만점대비)·등급 추이' : '내 점수%(만점대비) 추이';
        }
      }

      // 캔버스 폭 자동
      const canvas = trendCtx.canvas;
      let wrap = document.getElementById('trendScroll');
      if (wrap) wrap.style.display = '';
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

      // 데이터셋(좌: 퍼센트, 우: 등급[옵션])
      const datasets = [{
        label: '점수%(만점 대비)',
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
          tension: 0,
          stepped: true,
          pointRadius: 3,
          borderColor: COLOR_GRADE,
          backgroundColor: COLOR_GRADE,
          yAxisID: 'y2'
        });
      }

      // 축
      const scales = {
        x: { title: { display: true, text: '회차 (응시 순서)' } },
        y: {
          beginAtZero: true,
          max: 100,
          title: { display: true, text: '점수(%)', color: COLOR_SCORE },
          ticks: { color: COLOR_SCORE, callback: v => `${v}%` }
        }
      };
      if (gradeData) {
        scales.y2 = {
          position: 'right',
          min: 1,
          max: 9,
          reverse: true,
          ticks: { stepSize: 1, color: COLOR_GRADE, callback: v => `${v}등급` },
          title: { display: true, text: '등급', color: COLOR_GRADE },
          grid: { drawOnChartArea: false }
        };
      }

      // 기존 차트 제거 후 생성
      if (trendChart) trendChart.destroy();
      trendChart = new Chart(trendCtx, {
        type: 'line',
        data: { labels, datasets },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          layout: { padding: { top: 28, right: 28, left: 12, bottom: 14 } }, // 여유 더 줌
          interaction: { mode: 'nearest', intersect: true },
          elements: { point: { hitRadius: 14, hoverRadius: 5 } },             // 클릭 잘 되게
          scales,
          plugins: {
            legend: { display: !!gradeData },
            tooltip: {
              // 클릭 중에도 기본 툴팁을 보이게 둘지 말지는 취향대로
              enabled: true,
              callbacks: { /* 기존 콜백 그대로 */ }
            },
            valueLabel: {
              datasetIndex: 0,
              respectClickFocus: true,   // ★ 클릭 포커스 반영: null이면 전체, 숫자면 해당 index만
              offset: 22,                 // 라벨을 점 위에서 더 띄워서 혼동 줄임
              maxPoints: 60,
              font: { size: 11, weight: '700' },
              color: COLOR_SCORE,
              formatter: v => `${Math.round(v)}%`
            },
            clickCallout: {
                getLabel: (i, chart) => {
                  const subj = subjects[i] || '';
                  const subc = subclasses[i] || '';
                  const cd   = csatDates[i] || '';

                  const raw  = chart.$rawScores?.[i];
                  const max  = chart.$maxScores?.[i];
                  const scoreLine = Number.isFinite(raw)
                   ? (Number.isFinite(max) ? `점수: ${raw}/${max}점` : `점수: ${raw}점`)
                  : '';
                  return [ subj && `과목: ${subj}`, subc && `세부과목: ${subc}`, cd && `응시월: ${cd}`, scoreLine ]
                   .filter(Boolean).join('\n');
                }
              }
            },
          onClick: (evt) => {
            const items = trendChart.getElementsAtEventForMode(evt, 'nearest', { intersect: true }, true);
            if (!items.length) {
              trendChart.$clickedIndex = null;     // 빈 곳 클릭 → 전체 라벨
              trendChart.update();
              return;
            }
            const idx = items[0].index;
            trendChart.$clickedIndex = (trendChart.$clickedIndex === idx) ? null : idx;
            trendChart.update();
          }

        }
      });

      // 클릭 라벨에서 만점 표시용
      trendChart.$maxScores = maxScores;
      trendChart.$rawScores = rawScores;

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

  // ========== 등급표(모달) ==========
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
    // 🔒 모두 기본값이면 전부 숨기고 종료
    if (isAllDefaultSelection()){
      hideAllStatsUI('과목/세부과목/응시월 중 하나 이상을 선택해 주세요.');
      return;
    }
    // 기본값이 아니면 KPI 행 다시 보이게(이전에 숨겼었다면)
    const _kpiRow = document.getElementById('kpiRow');
    if (_kpiRow) _kpiRow.style.display = '';
    const ADMIN = isAdmin();

    const rowsAll = await fetchAllRows();     // 서버/기존 필터 적용된 “원본”
    window.LAST_ROWS_RAW = rowsAll;           // (월 바 보강용)
    renderMonthStrip(rowsAll);                // 연/월 UI 그리기/갱신
    const rows = applyMonthFilter(rowsAll);   // ← 월필터
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
      if (distCtx) await drawScoreDistribution(rows);
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

    window.examData = rows;
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
      const backParam = encodeURIComponent(currentStatsBack());

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

  // ================= AI 학습 조언 기능 =================
  let aiRequested = false;

  async function requestAiAdvice(exams) {
    if (aiRequested) return;
    aiRequested = true;
    const box = document.getElementById('aiAdviceBox');
    if (box) box.textContent = "AI가 학습 조언을 생성 중입니다. ⏳";

    try {
      const res = await fetch('/api/stats/advice', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer ' + localStorage.getItem('token')
        },
        body: JSON.stringify(exams)
      });

      console.log('AI 요청 응답 status:', res.status, 'ok:', res.ok);
      const text = await res.text();
      console.log('AI 요청 응답 body (first 1000 chars):', text.slice(0,1000));
      // HTML 로그인 페이지가 왔는지 간단 체크
      if (res.status === 401 || /<form[^>]+login|<title>로그인|<body[^>]*>/.test(text.slice(0,200).toLowerCase())) {
        box.textContent = "인증 실패: 로그인 화면이 반환되었습니다. (토큰 확인 필요)";
      } else {
        box.textContent = text || "AI 조언을 생성하지 못했습니다.";
      }

    } catch (err) {
      console.error("AI 조언 요청 실패:", err);
      if (box) box.textContent = "AI 조언을 불러오지 못했습니다. (요청 실패)";
    } finally {
      aiRequested = false;
    }
  }

  // ✅ 버튼 연결 (DOMContentLoaded 이후)
  document.addEventListener("DOMContentLoaded", () => {
    const btn = document.getElementById('aiAdviceBtn');
    if (btn) {
      btn.addEventListener("click", () => {
        if (!window.examData || window.examData.length === 0) {
          alert("먼저 시험 데이터를 불러와주세요.");
          return;
        }
        requestAiAdvice(window.examData);
      });
    }
  });


  // ========== 부트 ==========
  function param(name){ return new URLSearchParams(location.search).get(name) || ''; }

  document.addEventListener('DOMContentLoaded', async () => {
   ensureMonthFilterUI(); // 연/월 UI가 없으면 만들어 둠(처음엔 비활성)
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
      // 자동 조회 X (검색 버튼으로)
      updateRemindLabel();
    });

    document.getElementById('subjectId')?.addEventListener('change', async (e)=>{
      const v = e.target.value;
      const sc = document.getElementById('subclassId');
      if (v && v !== '*') {
        const r = await fetchJSON(`/api/lookup/subclasses?subjectId=${v}`, { headers: authHeaders() });
        const j = r.ok ? r.json : null;
        const list = Array.isArray(j) ? j
        : (j && Array.isArray(j.content)) ? j.content
        : (j && Array.isArray(j.data))    ? j.data
        : (j && Array.isArray(j.items))   ? j.items
        : (j && Array.isArray(j.results)) ? j.results : [];
        sc.innerHTML = '<option value="">세부과목 선택</option>' + list.map(s=>`<option value="${s.id}">${s.name}</option>`).join('');
        sc.disabled = false;

        // SUBCLASS_DIR 메모
        SUBCLASS_DIR.clear();
        list.forEach(s => {
          const id = s.id ?? s.subclassId ?? s.subclass_id;
          const name = s.name ?? s.subclassName ?? s.subclass_name;
          const count = s.count ?? s.questionCount ?? null;
          if (id != null) SUBCLASS_DIR.set(String(id), { name, count, subjectId: $('#subjectId').value || null });
        });
      } else {
        sc.innerHTML = '<option value="">세부과목 선택</option>';
        sc.disabled = false;
        SUBCLASS_DIR.clear();
      }
      await loadCsatDatesForCurrentSelection();
      // 자동 조회 X (검색 버튼으로)
    });
    document.getElementById('subclassId')?.addEventListener('change', async ()=> {
      await loadCsatDatesForCurrentSelection();
      // 자동 조회 X (검색 버튼으로)
    });
    document.getElementById('csatDate')?.addEventListener('change', refreshAll);

    try { await loadSubjects(); }
    catch (e) { console.error(e); alert('과목 목록을 불러오지 못했습니다. /api/lookup/subjects 확인 필요'); }

    // 초기 파라미터 반영
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
      // 세부과목 로드 후 값 반영 + SUBCLASS_DIR 메모
      if (sel && pSubject !== '*') {
        const r = await fetchJSON(`/api/lookup/subclasses?subjectId=${pSubject}`, { headers: authHeaders() });
        const j = r.ok ? r.json : null;
        const list = Array.isArray(j) ? j
         : (j && Array.isArray(j.content)) ? j.content
         : (j && Array.isArray(j.data))    ? j.data
         : (j && Array.isArray(j.items))   ? j.items
         : (j && Array.isArray(j.results)) ? j.results : [];
        const sc = document.getElementById('subclassId');
        sc.innerHTML = '<option value="">세부과목 선택</option>' + list.map(s=>`<option value="${s.id}">${s.name}</option>`).join('');
        sc.disabled = false;

        SUBCLASS_DIR.clear();
        list.forEach(s => {
          const id = s.id ?? s.subclassId ?? s.subclass_id;
          const name = s.name ?? s.subclassName ?? s.subclass_name;
          const count = s.count ?? s.questionCount ?? null;
          if (id != null) SUBCLASS_DIR.set(String(id), { name, count, subjectId: $('#subjectId').value || null });
        });

        if (pSubclass) sc.value = pSubclass;
      }
    }

    await loadCsatDatesForCurrentSelection();
    if (pCsat) {
      const cs = document.getElementById('csatDate');
      if (cs) cs.value = pCsat;
    }

    // --- 초기 표시 제어 (검색 전엔 완전 숨김) ---
    if (isAllDefaultSelection()) {
      // 안내문도 없이 카드/차트/구분선 전부 숨김
      hideAllStatsUI('');
    } else {
      // 기본값이 아닌 경우에만 기존 모드별 표시 로직 수행
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
    }
  });
})();
