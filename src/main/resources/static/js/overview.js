/* /js/overview.js */
(() => {

// 공통 유틸 부근 최상단에 추가
let USER_NAME_MAP = new Map();

 // --- 관리자면 '최근 점수'로 헤더 라벨 교체 ---
 function applyScoreHeaderLabel() {
   // 오버뷰 모달 thead 의 '점수' 헤더(th)를 찾는다
   const ths = Array.from(document.querySelectorAll('#overviewModal thead th'));
   // data-col="score"가 있으면 그걸, 없으면 텍스트로 추정, 그래도 없으면 3번째(th[2])
   let scoreTh =
     document.querySelector('#overviewModal thead th[data-col="score"]') ||
     ths.find(th => /^(내\s*점수|최근\s*점수|점수)$/.test(th.textContent.trim())) ||
    (ths.length >= 3 ? ths[2] : null);
   if (!scoreTh) return;

   const label = '평균 점수';
   const badge = scoreTh.querySelector('.badge, .label'); // 배지 안에 글자가 있을 수 있음
   if (badge) badge.textContent = label;
   else scoreTh.textContent = label;
 }

function rolesToString(r){
  if (!r) return '';
  if (Array.isArray(r)) {
    return r.map(x => typeof x === 'string'
      ? x
      : (x?.authority || x?.role || x?.name || '')
    ).join(',');
  }
  if (typeof r === 'object') {
    // {authority:'ROLE_ADMIN'} 같은 단일 객체
    return r.authority || r.role || Object.values(r).join(',');
  }
  return String(r || '');
}

function isAdminRole(str){
  // ROLE_ADMIN, ADMIN, SUPERADMIN 등 ADMIN 포함 역할 잡기
  return /(^|[\s,])(?:ROLE_)?ADMIN([\s,]|$)/i.test(String(str));
}


  // ---------- 공통 ----------
  const $  = s => document.querySelector(s);
  const $$ = s => Array.from(document.querySelectorAll(s));

  let OV_SEL = { subjectId: '', subclassId: '' };

  function authHeaders() {
    const h = { Accept: 'application/json' };
    const t = localStorage.getItem('token');
    if (t) h.Authorization = 'Bearer ' + t;
    return h;
  }
  function parseToken() {
    const t = localStorage.getItem('token');
    if (!t) return {};
    try { return JSON.parse(atob(t.split('.')[1])) || {}; } catch { return {}; }
  }
  function isAdmin() {
    const p = parseToken();
    const roles = p.authorities || p.roles || p.role || p.scope || p.scopes || [];
    const str = Array.isArray(roles) ? roles.join(',') : String(roles || '');
    return /ROLE_ADMIN|ADMIN/i.test(str);
  }
  function setOverviewScoreColVisible(visible) {
    const tb = document.getElementById('ovTbody');
    if (!tb) return;
    const table = tb.closest('table');
    const idx = 2; // 열 인덱스: 0=과목, 1=세부과목, 2=점수
    const disp = visible ? '' : 'none';

    const headRow = table?.querySelector('thead tr');
    if (headRow && headRow.children[idx]) headRow.children[idx].style.display = disp;

    tb.querySelectorAll('tr').forEach(tr => {
      const cell = tr.children[idx];
      if (cell) cell.style.display = disp;
    });
  }

  function updateScoreColByUser() {
    setOverviewScoreColVisible(true);
  }
  function getUidForApi() {
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
  const toNumOrNull = v => (v === '' || v == null || isNaN(v)) ? null : Number(v);
  const uniqueSorted = list => [...new Set(list)].filter(Boolean).sort();
  async function fetchJSON(url) {
    const r = await fetch(url, { headers: authHeaders() });
    if (!r.ok) throw new Error(`${url} HTTP ${r.status}`);
    return r.json();
  }

  function pick(o, keys) {
    for (const k of keys) {
      const v = k.includes('.') ? k.split('.').reduce((a,c)=>a?.[c], o) : o?.[k];
      if (v !== undefined && v !== null && v !== '') return v;
    }
    return null;
  }

  // 값 표시 헬퍼(빈값이면 '-' 대신 빈칸 원하시면 ''로 바꾸세요)
  const show = v => (v === '' || v == null ? '-' : v);

  // ---------- 회차/유저 ----------
  async function fetchMonths() {
    try {
      const list = await fetchJSON('/api/lookup/csat-dates-global');
      if (Array.isArray(list) && list.length) return list;
    } catch {}
    try {
      const rows = await fetchJSON('/api/exams');
      return uniqueSorted(rows.map(r => r.csatDate));
    } catch { return []; }
  }
  async function fetchUsers() {
    const urls = ['/api/admin/users','/api/users','/api/user/all'];
    for (const u of urls) {
      try {
        const r = await fetch(u, { headers: authHeaders() });
        if (!r.ok) continue;
        const raw = await r.json();

        let list = [];
        if (Array.isArray(raw)) list = raw;
        else if (raw?.content)  list = raw.content;
        else if (raw?.data)     list = raw.data;
        else if (raw?.items)    list = raw.items;
        else if (raw?.results)  list = raw.results;

        const out = [];
        for (const x of list) {
          const id =
            x.userId ?? x.id ?? x.user_id ?? x.uid ?? x.user?.id;
          if (!id) continue;

          // 역할 문자열 만들기 (여러 API 케이스 대응)
          const roleStr = rolesToString(
            x.authorities ?? x.roles ?? x.role ?? x.scope ?? x.scopes ??
            x.user?.authorities ?? x.user?.roles ?? x.user?.role ?? x.user?.scope ?? x.user?.scopes
          );

          // ✅ 관리자면 제외
          if (isAdminRole(roleStr)) continue;

          const name =
            (x.nickname ?? x.nickName ?? x.user?.nickname) ??
            (x.name ?? x.fullName ?? x.user?.name) ??
            (x.username ?? x.user?.username) ??
            (x.email ?? x.user?.email) ??
            `user#${id}`;

          out.push({ id, name });
        }
        return out;
      } catch {}
    }
    return [];
  }


  async function fetchRowsByMonth(csatDate, userId) {
    const qs = new URLSearchParams();
    if (csatDate) qs.set('csatDate', csatDate);
    if (userId)   qs.set('userId', userId);
    const rows = await fetchJSON('/api/exams?' + qs.toString());
    // 관리자인데 ovUser 선택이 없으면 전체 그대로 반환
    const isAllAdmin = isAdmin() && !(userId && String(userId).trim());
    const uid = userId || (isAllAdmin ? null : getUidForApi());
    return uid ? rows.filter(r => String(r.userId) === String(uid)) : rows;
  }

  // ---------- 과목/세부과목 전체 목록 (캐시) ----------
  let SUBJECTS = null;        // [{id,name}, ...]
  let SUBCLASSES = null;      // [{id,name,subjectId,subjectName?}, ...]

  async function fetchSubjectsAll() {
    if (SUBJECTS) return SUBJECTS;
    let url = '/api/lookup/subjects';
    let res = await fetch(url, { headers: authHeaders() });
    if (!res.ok && res.status === 404) { url = '/api/subjects'; res = await fetch(url, { headers: authHeaders() }); }
    if (!res.ok) throw new Error('subjects HTTP ' + res.status);
    const list = await res.json();
    SUBJECTS = (Array.isArray(list) ? list : []).map(s => ({
      id:  s.id ?? s.subjectId ?? s.subject_id,
      name:s.name ?? s.subjectName ?? s.subject_name ?? '(이름없음)'
    })).filter(x => x.id != null);
    return SUBJECTS;
  }

  // ---- add to /js/overview.js ----

  // 관리자 여부 재사용
  function _isAdmin() {
    try {
      const t = localStorage.getItem('token');
      const p = t ? JSON.parse(atob(t.split('.')[1])) : {};
      const roles = p.authorities || p.roles || p.role || p.scope || p.scopes || [];
      const str = Array.isArray(roles) ? roles.join(',') : String(roles || '');
      return /ROLE_ADMIN|ADMIN/i.test(str);
    } catch { return false; }
  }

  // 상세통계 경로 계산: 응시자(기본) / 관리자 전체 / 관리자 한명
  function computeStatsHref() {
    const admin  = _isAdmin();
    const csat   = document.querySelector('#ovMonth')?.value || '';

    const rawId  = admin ? (document.querySelector('#ovUser')?.value ?? '') : '';
    const userId = (/^\s*$/.test(rawId) || /^(0|null|undefined)$/i.test(rawId)) ? '' : rawId;

    // ✅ 오버뷰 표에서 사용자가 고른 과목/세부과목
    const subjectId  = OV_SEL.subjectId  || '';
    const subclassId = OV_SEL.subclassId || '';

    let path = '/stats';                    // 기본: 응시자
    if (admin && userId)      path = '/stats/user';   // 관리자: 한명
    else if (admin && !userId) path = '/stats/admin'; // 관리자: 전체

    const qs = new URLSearchParams();
    if (csat)      qs.set('csatDate', csat);
    if (subjectId) qs.set('subjectId', subjectId);
    if (subclassId)qs.set('subclassId', subclassId);
    if (userId)    qs.set('userId', userId); // 관리자 한명 보기

    return path + (qs.toString() ? `?${qs}` : '');
  }

  function refreshStatsLinks() {
    const href = computeStatsHref();
    document.getElementById('ovToStatsTop')?.setAttribute('href', href);
    document.getElementById('ovToStats')?.setAttribute('href', href);
  }

  function buildStatsHrefFor(subjectId, subclassId){
    const admin  = _isAdmin();
    const csat   = document.querySelector('#ovMonth')?.value || '';

    const rawId  = admin ? (document.querySelector('#ovUser')?.value ?? '') : '';
    const userId = (/^\s*$/.test(rawId) || /^(0|null|undefined)$/i.test(rawId)) ? '' : rawId;

    let path = '/stats';
    if (admin && userId)      path = '/stats/user';
    else if (admin && !userId) path = '/stats/admin';

    const qs = new URLSearchParams();
    if (csat)       qs.set('csatDate', csat);
    if (subjectId)  qs.set('subjectId', subjectId);
    if (subclassId) qs.set('subclassId', subclassId);
    if (userId)     qs.set('userId', userId);
    return path + (qs.toString() ? `?${qs}` : '');
  }

  document.addEventListener('DOMContentLoaded', () => {

  document.body.addEventListener('click', (e) => {
    const tr = e.target.closest('#ovTbody tr.ov-row');
    if (!tr) return;

    // 선택 비주얼 (파란 배경 같은 Bootstrap 스타일)
    const prev = document.querySelector('#ovTbody tr.table-active');
    if (prev) prev.classList.remove('table-active');
    tr.classList.add('table-active');

    // 선택 저장
    OV_SEL.subjectId  = tr.dataset.subjectId || '';
    OV_SEL.subclassId = tr.dataset.subclassId || '';

    // 상단 "상세 통계로" 링크 재계산
    refreshStatsLinks();
  });
    refreshStatsLinks();
    document.getElementById('ovMonth')?.addEventListener('change', refreshStatsLinks);
    document.getElementById('ovUser')?.addEventListener('change', refreshStatsLinks);
  });


  async function fetchSubclassesAll() {
    const h = { Accept: 'application/json' };
    const t = localStorage.getItem('token'); if (t) h.Authorization = 'Bearer ' + t;

    // 1) 먼저 /api/lookup/subclasses-all 시도
    try {
      const r = await fetch('/api/lookup/subclasses-all', { headers: h });
      if (r.ok) return await r.json();
      if (r.status !== 404) throw new Error('HTTP ' + r.status);
    } catch (_) {}

    // 2) 없으면 subjects를 받아서 각 과목별로 /api/lookup/subclasses?subjectId= 호출 후 합치기
    const subjRes = await fetch('/api/lookup/subjects', { headers: h });
    if (!subjRes.ok) throw new Error('subjects HTTP ' + subjRes.status);
    const subjects = await subjRes.json();

      const subjNameMap = new Map(
        (subjects || []).map(s => [
          String(s.id ?? s.subjectId ?? s.subject_id),
          String(s.name ?? s.subjectName ?? s.subject_name ?? '')
        ])
      );

    const all = [];
    for (const s of (subjects || [])) {
      const sid = s.id ?? s.subjectId ?? s.subject_id;
      if (!sid) continue;
      const r = await fetch(`/api/lookup/subclasses?subjectId=${encodeURIComponent(sid)}`, { headers: h });
      if (!r.ok) continue;
      const list = await r.json();
      for (const sc of (list || [])) {
        all.push({
          id: sc.id ?? sc.subclassId ?? sc.subclass_id,
          name: sc.name ?? sc.subclassName ?? sc.subclass_name,
          subjectId: sid,
          subjectName: subjNameMap.get(String(sid)) || (s.name ?? s.subjectName ?? s.subject_name ?? '-')
        });
      }
    }
    return all;
  }

  // ---------- 등급컷 ----------
  async function fetchCuts(csatDate, subclassId) {
    const qs = new URLSearchParams({ csatDate, subclassId });
    try {
      const r = await fetch('/api/lookup/grade-cuts?' + qs.toString(), { headers: authHeaders() });
      if (r.status === 204) return null;
      if (!r.ok) throw new Error('grade-cuts HTTP ' + r.status);
      return await r.json();
    } catch { return null; }
  }

  // ---------- 렌더: 오버뷰 표 (응시한 세부과목만 + 평균 + 점수 이력 버튼) ----------
  function renderOverviewTable(tbody, allSubclasses, rows, csatDate) {
    // 관리자인데 사용자 미선택이면 "전체 모드"
    const isAllAdmin = isAdmin() && !((document.getElementById('ovUser')?.value || '').trim());

    // subclassId -> attempts[]
    const attemptsBySub = new Map();
    const numOrNull = v => {
      const n = Number(v); return Number.isFinite(n) ? n : null;
    };
    const getScore = at => {
      const v = pick(at, ['result','score','rawScore','raw_score','value']);
      return numOrNull(v);
    };
    rows.forEach(r => {
      const sid = String(r.subclassId ?? r.subclass_id ?? '');
      if (!sid) return;
      (attemptsBySub.get(sid) || attemptsBySub.set(sid, []).get(sid)).push(r);
    });

    const attemptedSubclasses = (allSubclasses || []).filter(sc => {
      const sid = String(sc.id ?? sc.subclass_id ?? '');
      return sid && attemptsBySub.has(sid);
    });
    if (!attemptedSubclasses.length) {
      tbody.innerHTML = `<tr><td colspan="6" class="text-center text-muted py-4">선택 회차에서 응시한 세부과목이 없습니다.</td></tr>`;
      return;
    }

    const cmpIdAsc = (a,b)=> {
      const an=Number(a), bn=Number(b);
      return (!isNaN(an)&&!isNaN(bn)) ? (an-bn) : String(a).localeCompare(String(b),'ko');
    };

    const bySubjectId = new Map();
    for (const sc of attemptedSubclasses) {
      const sid = String(sc.id ?? sc.subclass_id ?? '');
      const subj= String(sc.subjectId ?? sc.subject_id ?? '');
      if (!sid || !subj) continue;
      const subjectName = sc.subjectName || '-';
      if (!bySubjectId.has(subj)) bySubjectId.set(subj, { id:subj, name:subjectName, list:[] });
      bySubjectId.get(subj).list.push(sc);
    }

    const groups = [...bySubjectId.values()].sort((a,b)=> cmpIdAsc(a.id,b.id));
    tbody.innerHTML = '';

    // 유저 이름 보정용 (행에 name 필드 없을 수도 있어, 가능한 값 우선 사용)
    const nameOf = (r) => {
     const id = r.userId ?? r.user?.id;
     const key = id != null ? String(id) : '';
     if (key && USER_NAME_MAP.has(key)) return USER_NAME_MAP.get(key); // ✅ 콤보와 동일 로직
    // 혹시 맵에 없으면 행 안의 필드로 폴백
    return (
     r.nickname || r.nickName || r.userName || r.name || r.displayName ||
     r.username || r.email || (key ? `user#${key}` : 'user')
     );
    };

    for (const g of groups) {
      g.list.sort((a,b)=> cmpIdAsc((a.id ?? a.subclass_id),(b.id ?? b.subclass_id)));
      for (const sc of g.list) {
        const sid = String(sc.id ?? sc.subclass_id);
        const ats = (attemptsBySub.get(sid) || []).slice()
                      .sort((a,b)=> new Date(b.testDate) - new Date(a.testDate));

        const scores = ats.map(getScore).filter(v => v != null);
        const avg = scores.length ? Math.round((scores.reduce((s,v)=>s+v,0)/scores.length)*10)/10 : null;

        // 최신 시도 보조 지표(개별 모드에서만 의미 있음)
        const latest = ats[ats.length-1] || {};
        const pct = isAllAdmin ? null : pick(latest, [
          'percentile','pct','perc','percent','percentRank','rankPct',
          'stats.percentile','metrics.percentile','summary.percentile'
        ]);
        const stdScore = isAllAdmin ? null : pick(latest, [
          'standardScore','stdScore','std','standard',
          'standard_score','std_score','scoreStd',
          'stats.standardScore','metrics.standardScore','summary.standardScore'
        ]);

        // 모달 payload
        const itemsForMine = ats.map(a => ({ date:a.testDate ?? '', score:getScore(a) }));
        const itemsForAll  = ats.map(a => ({
          userName: nameOf(a),
          date: a.testDate ?? '',
          score: getScore(a)
        }));

        const payload = encodeURIComponent(JSON.stringify(isAllAdmin ? itemsForAll : itemsForMine));
        const scoreBtnClass = isAllAdmin ? 'btn-score-list-all' : 'btn-score-list';

        tbody.insertAdjacentHTML('beforeend', `
          <tr class="ov-row"
              data-subject-id="${g.id}"
              data-subject-name="${g.name || '-'}"
              data-subclass-id="${sid}"
              data-subclass-name="${sc.name || '-'}">
            <td>${g.name || '-'}</td>
            <td>${sc.name || '-'}</td>
            <td class="text-end">
              ${avg != null
                ? `<button type="button"
                           class="btn btn-link p-0 ${scoreBtnClass}"
                           data-subname="${encodeURIComponent(sc.name || '-')}"
                           data-csat="${encodeURIComponent(csatDate)}"
                           data-scores="${payload}">
                     ${avg}
                   </button>`
                : '-'}
            </td>
            <td class="text-end">${show(pct)}</td>
            <td class="text-end">${show(stdScore)}</td>
            <td class="text-end">
              <a class="btn btn-sm btn-outline-secondary" href="${buildStatsHrefFor(g.id, sid)}">상세</a>
              <button class="btn btn-sm btn-primary btn-show-cut"
                      data-csat="${encodeURIComponent(csatDate)}"
                      data-subid="${encodeURIComponent(sid)}"
                      data-subname="${encodeURIComponent(sc.name || '-')}"
                      data-myscore="${avg != null ? avg : ''}">
                등급표
              </button>
            </td>
          </tr>
        `);
      }
    }
  }

 // ---------- 점수 이력 모달 (개인 모드: 응시일/점수) ----------
 function ensureScoresModal() {
   if (document.getElementById('scoresModal')) return;
   document.body.insertAdjacentHTML('beforeend', `
     <div class="modal fade" id="scoresModal" tabindex="-1" aria-hidden="true">
       <div class="modal-dialog modal-md modal-dialog-scrollable">
         <div class="modal-content">
           <div class="modal-header">
             <h5 class="modal-title" id="scoresModalTitle"
                 style="white-space:nowrap;overflow:hidden;text-overflow:ellipsis;max-width:calc(100% - 2rem);">
               점수 목록
             </h5>
             <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
           </div>
           <div class="modal-body">
             <table class="table table-sm align-middle mb-0">
               <thead><tr><th>응시일</th><th class="text-end">점수</th></tr></thead>
               <tbody id="scoresTbody"></tbody>
             </table>
           </div>
         </div>
       </div>
     </div>`);
 }
 function openScoresModal({ subclassName, csatDate, items }) {
   ensureScoresModal();
   const title = document.getElementById('scoresModalTitle');
   const tb = document.getElementById('scoresTbody');
   title.textContent = `${subclassName} · ${csatDate} — 점수 목록`;
   items.sort((a,b)=> new Date(b.date) - new Date(a.date)); // 최신순
   tb.innerHTML = items.length
     ? items.map(it => `
         <tr>
           <td>${it.date ? new Date(it.date).toLocaleString() : '-'}</td>
           <td class="text-end">${it.score ?? '-'}</td>
         </tr>`).join('')
     : `<tr><td colspan="2" class="text-center text-muted">응시 이력이 없습니다.</td></tr>`;
   new bootstrap.Modal(document.getElementById('scoresModal')).show();
 }

 // ---------- 점수 이력 모달 (전체 모드: 사용자/응시일/점수) ----------
 function ensureScoresModalAll() {
   if (document.getElementById('scoresModalAll')) return; // ✅ 올바른 ID 체크
   document.body.insertAdjacentHTML('beforeend', `
     <div class="modal fade" id="scoresModalAll" tabindex="-1" aria-hidden="true">
       <div class="modal-dialog modal-lg modal-dialog-scrollable">
         <div class="modal-content">
           <div class="modal-header">
             <h5 class="modal-title" id="scoresModalAllTitle"
                 style="white-space:nowrap;overflow:hidden;text-overflow:ellipsis;max-width:calc(100% - 2rem);">
               점수 목록
             </h5>
             <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
           </div>
           <div class="modal-body">
             <table class="table table-sm align-middle mb-0">
               <thead><tr><th>사용자</th><th>응시일</th><th class="text-end">점수</th></tr></thead>
               <tbody id="scoresAllTbody"></tbody>
             </table>
           </div>
         </div>
       </div>
     </div>`);
 }
 function openScoresModalAll({ subclassName, csatDate, items }) {
   ensureScoresModalAll();
   const title = document.getElementById('scoresModalAllTitle');
   const tb = document.getElementById('scoresAllTbody');
   title.textContent = `${subclassName} · ${csatDate} — 점수 목록`;
   items.sort((a,b)=> new Date(b.date) - new Date(a.date)); // 최신순
   tb.innerHTML = items.length
     ? items.map(it => `
         <tr>
           <td>${it.userName || '-'}</td>
           <td>${it.date ? new Date(it.date).toLocaleString() : '-'}</td>
           <td class="text-end">${it.score ?? '-'}</td>
         </tr>`).join('')
     : `<tr><td colspan="3" class="text-center text-muted">표시할 점수가 없습니다.</td></tr>`;
   new bootstrap.Modal(document.getElementById('scoresModalAll')).show();
 }


  // ---------- 등급표 모달 ----------
  function openCutModal({ csatDate, subclassName, myScore }, cuts) {
    const title   = $('#cutModalTitle');
    const tb      = $('#cutTbody');
    const stdHead = $('#stdHead');
    const summary = $('#cutSummary');

    title.textContent = `${subclassName} · ${csatDate}`;
    tb.innerHTML = '';

    // 관리자라면 '최근 점수', 아니면 '내 점수'
    const scoreLabel = isAdmin() ? '최근 점수' : '내 점수';

    if (!cuts) {
      stdHead.style.display = 'none';
      summary.textContent = '등급컷 정보를 불러오지 못했습니다.';
      return;
    }

    const hasStd = Object.keys(cuts).some(k => /^std(cut)?[1-9]$/i.test(k)) ||
                   Object.keys(cuts).some(k => /^cutStd[1-9]$/i.test(k));
    stdHead.style.display = hasStd ? '' : 'none';

    for (let g=1; g<=9; g++) {
      const raw = cuts['cut'+g] ?? cuts['raw'+g] ?? cuts['scoreCut'+g] ?? null;
      const std = cuts['std'+g] ?? cuts['stdCut'+g] ?? cuts['cutStd'+g] ?? null;
      tb.insertAdjacentHTML('beforeend', `
        <tr><td>${g}등급</td><td>${raw ?? '-'}</td><td ${hasStd ? '' : 'style="display:none;"'}>${std ?? '-'}</td></tr>
      `);
    }

    if (myScore !== '' && myScore != null) {
      let myGrade = null;
      for (let g=1; g<=9; g++) {
        const cut = cuts['cut'+g];
        if (cut != null && Number(myScore) >= Number(cut)) { myGrade = g; break; }
      }
      if (myGrade == null) myGrade = 9;
      summary.textContent = `${scoreLabel} ${myScore}점 → ${myGrade}등급`;
    } else {
      summary.textContent = '등급표';
    }

    new bootstrap.Modal($('#cutModal')).show();
  }

  // ---------- 초기화/이벤트 ----------
  async function initOverviewModal() {
    // 회차
    try {
      const months = await fetchMonths();
      $('#ovMonth').innerHTML =
        '<option value="">회차 선택</option>' +
        months.map(m => `<option value="${m}">${m}</option>`).join('');
    } catch {
      $('#ovMonth').innerHTML = '<option value="">회차 선택</option>';
    }

    // 관리자 유저 콤보
    if (isAdmin()) {
      $('#ovUserWrap').style.display = '';
      try {
        const users = await fetchUsers();

        // ⬇️ 여기서 전역 맵도 한번에 채움 (id → 표시이름)
        USER_NAME_MAP = new Map(
          (users || []).map(u => [
          String(u.userId ?? u.id),
          String(u.nickname ?? u.name ?? u.username ?? u.email ?? `user#${u.userId ?? u.id}`)
        ])
        );
        const opts = ['<option value="">사용자 선택</option>'];
        for (const u of users) {
          const id = u.userId ?? u.id;
          const name = (u.nickname || u.name || u.username || u.email || ('user#'+id));
          if (!id) continue;
          opts.push(`<option value="${id}">${name}</option>`);
        }
        $('#ovUser').innerHTML = opts.join('');
      } catch {}
    } else {
      $('#ovUserWrap').style.display = 'none';
    }

    // 모든 세부과목 캐시
    try { await fetchSubjectsAll(); await fetchSubclassesAll(); } catch {}
  }

  async function openOverview() {
    await initOverviewModal();
    applyScoreHeaderLabel();
    new bootstrap.Modal($('#overviewModal')).show();
  }

  document.addEventListener('DOMContentLoaded', () => {
  applyScoreHeaderLabel();
    // 열기 버튼
    $('#btnOverview')?.addEventListener('click', openOverview);

    // 불러오기
    $('#ovFetch')?.addEventListener('click', async () => {
      const csat = $('#ovMonth').value || '';
      const uid  = $('#ovUserWrap').style.display !== 'none' ? ($('#ovUser').value || '') : null;
      const tb   = $('#ovTbody');

      if (!csat) {
        tb.innerHTML = `<tr><td colspan="6" class="text-center text-muted py-4">회차를 선택해 주세요.</td></tr>`;
        return;
      }
      tb.innerHTML = `<tr><td colspan="6" class="text-center text-muted py-4">불러오는 중…</td></tr>`;

      try {
        const allSub = await fetchSubclassesAll();
        const rows   = await fetchRowsByMonth(csat, uid);
        renderOverviewTable(tb, allSub, rows, csat);
        updateScoreColByUser(); // 표 렌더 후 열 표시 상태 반영
      } catch (e) {
        console.warn(e);
        tb.innerHTML = `<tr><td colspan="6" class="text-center text-danger py-4">데이터를 불러오지 못했습니다.</td></tr>`;
      }
    });
    document.getElementById('ovUser')?.addEventListener('change', updateScoreColByUser);

    // 등급표 버튼(위임)
    document.body.addEventListener('click', async (e) => {
      const btn = e.target.closest('.btn-show-cut');
      if (!btn) return;
      const csatDate   = decodeURIComponent(btn.dataset.csat || '');
      const subclassId = decodeURIComponent(btn.dataset.subid || '');
      const subclassNm = decodeURIComponent(btn.dataset.subname || '');
      const myScore    = btn.dataset.myscore ?? '';

      let cuts = null;
      try { cuts = await fetchCuts(csatDate, subclassId); } catch {}
      openCutModal({ csatDate, subclassName: subclassNm, myScore }, cuts);
    });

    // 위임 클릭 리스너(기존 등급표 리스너 옆에 추가)
    document.body.addEventListener('click', (e) => {
      const btn = e.target.closest('.btn-score-list');
      if (!btn) return;
      const subclassNm = decodeURIComponent(btn.dataset.subname || '');
      const csatDate = decodeURIComponent(btn.dataset.csat || '');
      let items = [];
      try { items = JSON.parse(decodeURIComponent(btn.dataset.scores || '[]')) || []; } catch {}
      openScoresModal({ subclassName: subclassNm, csatDate, items });
    });

    // (NEW) 전체 모드: 사용자/날짜/점수 3열
    document.body.addEventListener('click', (e) => {
      const btn = e.target.closest('.btn-score-list-all');
      if (!btn) return;
      const subclassNm = decodeURIComponent(btn.dataset.subname || '');
      const csatDate   = decodeURIComponent(btn.dataset.csat || '');
      let items = [];
      try { items = JSON.parse(decodeURIComponent(btn.dataset.scores || '[]')) || []; } catch {}
      openScoresModalAll({ subclassName: subclassNm, csatDate, items });
    });
  });
})();
