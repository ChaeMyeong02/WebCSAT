/* /js/overview.js */
(() => {
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

   const label = isAdmin() ? '최근 점수' : '내 점수';
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
    const sel = document.getElementById('ovUser');
    const hasUser = !!(sel && sel.value && sel.value.trim() !== '');
    const visible = !isAdmin() || hasUser; // 일반 유저는 항상 보임, 관리자+미선택이면 숨김
    setOverviewScoreColVisible(visible);
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
    const uid = userId || getUidForApi();
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

  // ---------- 렌더: 오버뷰 표 (모든 세부과목 + 내 최신 응시 매칭) ----------
  function renderOverviewTable(tbody, allSubclasses, rows, csatDate) {
    const latestBySub = new Map(); // subId -> row
    rows.slice().sort((a,b)=> new Date(b.testDate) - new Date(a.testDate))
      .forEach(r => {
        const key = String(r.subclassId ?? r.subclass_id ?? '');
        if (!key) return;
        if (!latestBySub.has(key)) latestBySub.set(key, r);
      });

    if (!allSubclasses.length) {
      tbody.innerHTML = `<tr><td colspan="6" class="text-center text-muted py-4">세부과목 목록이 없습니다.</td></tr>`;
      return;
    }

    const cmpIdAsc = (a, b) => {
      const an = Number(a), bn = Number(b);
      const aNum = !isNaN(an), bNum = !isNaN(bn);
      if (aNum && bNum) return an - bn;
      return String(a).localeCompare(String(b), 'ko');
    };

    // 과목별 그룹 (subject_id)
    const bySubjectId = new Map(); // subjectId -> { id, name, list: [] }
    for (const sc of allSubclasses) {
      const sid = String(sc.subjectId ?? sc.subject_id ?? '');
      if (!sid) continue;
      if (!bySubjectId.has(sid)) {
        bySubjectId.set(sid, { id: sid, name: sc.subjectName || '-', list: [] });
      }
      bySubjectId.get(sid).list.push(sc);
    }

    // ✅ 과목 정렬: subject_id 오름차순
    const groups = [...bySubjectId.values()].sort((a,b)=> cmpIdAsc(a.id, b.id));

    tbody.innerHTML = '';
    for (const g of groups) {
      // ✅ 세부과목 정렬: subclass_id(= sc.id) 오름차순
      g.list.sort((a,b)=> cmpIdAsc((a.id ?? a.subclass_id), (b.id ?? b.subclass_id)));

      for (const sc of g.list) {
        const at = latestBySub.get(String(sc.id ?? sc.subclass_id));

        const myScore = pick(at, [
          'result','score','rawScore','raw_score','value'
        ]);
        const pct = pick(at, [
          'percentile','pct','perc','percent','percentRank','rankPct',
          'stats.percentile','metrics.percentile','summary.percentile'
        ]);
        const stdScore = pick(at, [
          'standardScore','stdScore','std','standard',
          'standard_score','std_score','scoreStd',
          'stats.standardScore','metrics.standardScore','summary.standardScore'
        ]);

        tbody.insertAdjacentHTML('beforeend', `
          <tr class="ov-row"
              data-subject-id="${g.id}"
              data-subject-name="${g.name || '-'}"
              data-subclass-id="${sc.id ?? sc.subclass_id}"
              data-subclass-name="${sc.name || '-'}">
            <td>${g.name || '-'}</td>
            <td>${sc.name || '-'}</td>
            <td class="text-end">${show(myScore)}</td>
            <td class="text-end">${show(pct)}</td>
            <td class="text-end">${show(stdScore)}</td>
            <td class="text-end">
              <a class="btn btn-sm btn-outline-secondary"
                 href="${buildStatsHrefFor(g.id, (sc.id ?? sc.subclass_id))}">
                상세
              </a>
              <button class="btn btn-sm btn-primary btn-show-cut"
                      data-csat="${encodeURIComponent(csatDate)}"
                      data-subid="${encodeURIComponent(sc.id ?? sc.subclass_id)}"
                      data-subname="${encodeURIComponent(sc.name || '-')}"
                      data-myscore="${show(myScore)}">
                등급표
              </button>
            </td>
          </tr>
        `);
      }
    }
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
  });
})();
