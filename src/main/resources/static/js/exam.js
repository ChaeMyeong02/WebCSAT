(() => {
  // ── 전역 ────────────────────────────────────────────────────────────
  const EX = window.__EXAM__ || {};
  const questions = Array.isArray(EX.questions) ? EX.questions : [];
  const articleRangeMap = EX.articleRangeMap || {};
  let remainingSeconds = Number(EX.examTimeSeconds || 3000);

  let currentPage = 1;
  const questionsPerPage = 6;
  const totalPages = Math.ceil(questions.length / questionsPerPage);
  let isSubmitting = false;
  let timerHandle = null;

  // ── MathJax (필요 시 자동 주입) ─────────────────────────────────────
  (function ensureMathJax(){
     if (!window.MathJax) {
       window.MathJax = {
         tex: { inlineMath: [['\\(', '\\)'], ['$', '$']] },
         svg: { fontCache: 'global' },
         startup: { typeset: false }            // ← 자동 typeset 끔
       };
       const s = document.createElement('script');
       s.src = 'https://cdn.jsdelivr.net/npm/mathjax@3/es5/tex-svg.js';
       s.async = true;
       s.onload = () => {                       // ← 로드되면 한 번 전체 typeset
         if (document.body) window.MathJax.typesetPromise?.([document.body]);
       };
       document.head.appendChild(s);
     }
  })();

  // ── 유틸 ─────────────────────────────────────────────────────────────
  const $  = (sel, root = document) => root.querySelector(sel);
  const $$ = (sel, root = document) => root.querySelectorAll(sel);

  // √ / ∛ / ∜ 를 LaTeX로, sup/계수/괄호 케이스까지 처리 + '고아 √' 제거
  function normalizeRootsToLaTeX(html) {
    if (!html) return '';
    let s = String(html);

    // 이미 LaTeX가 있으면 건너뜀
    if (/\\sqrt|\\\(|\\\[|\\begin\{/.test(s)) return s;

    s = s.replace(/&nbsp;/g, ' ');

    const atom = String.raw`(?:\([^)]*\)|[A-Za-z0-9]+(?:\^\d+)?)`;

    // sup이 √ 앞/뒤 어디 있어도 근지수로 먹기
    s = s.replace(
      new RegExp(
        String.raw`(?:\^(\d+)|<sup[^>]*>(\d+)<\/sup>)?\s*√\s*(?:<sup[^>]*>(\d+)<\/sup>)?\s*` + atom,
        'gi'
      ),
      (_m, a, b, c, rest) => {
        const idx = a || b || c;                         // 근지수
        const operand = rest.startsWith('(') ? rest.slice(1, -1) : rest;
        return `\\(${ idx ? `\\sqrt[${idx}]` : `\\sqrt` }{${operand}}\\)`;
      }
    );

    // ∛, ∜
    s = s.replace(new RegExp(String.raw`∛\s*` + atom, 'g'), (_m, rest) => {
      const op = rest.startsWith('(') ? rest.slice(1, -1) : rest;
      return `\\(\\sqrt[3]{${op}}\\)`;
    });
    s = s.replace(new RegExp(String.raw`∜\s*` + atom, 'g'), (_m, rest) => {
      const op = rest.startsWith('(') ? rest.slice(1, -1) : rest;
      return `\\(\\sqrt[4]{${op}}\\)`;
    });

    // 괄호형 √( ... )
    s = s.replace(/√\s*\(([^)]+)\)/g, (_m, a) => `\\(\\sqrt{${a}}\\)`);
    // 토큰형 √x
    s = s.replace(/√\s*([A-Za-z0-9]+(?:\^\d+)?)/g, (_m, a) => `\\(\\sqrt{${a}}\\)`);

    // 계수 결합: 3 \( \sqrt{...} \) → \( 3\sqrt{...} \)
    s = s.replace(
      /(\d+)\s*\\\(\s*\\sqrt(\[[^\]]+\])?\{([^}]+)\}\s*\\\)/g,
      (_m, c, idx, rad) => `\\(${c}\\sqrt${idx || ''}{${rad}}\\)`
    );

    // 🔥 고아 √ 제거:
    //  - 줄 시작/공백 뒤 등장하고
    //  - 다음이 피연산자( (, 영숫자, \, <sup )가 아닌 경우
    //  - 또는 곧바로 기호(×,+,−,*,/,=,.,,,:)나 줄바꿈/끝이 오는 경우
    s = s
      .replace(/(^|[\s>])√(?=\s*(?!\(|[A-Za-z0-9]|\\|<sup)[^])/g, '$1') // 피연산자 아닌 패턴
      .replace(/√\s*(?=($|[×+\-*/=,:;)|\]]))/g, '');                   // 기호/끝 앞의 √

    return s;
  }

  function typesetMath(root){
    const run = () => window.MathJax?.typesetPromise?.(root ? [root] : undefined);
    setTimeout(run, 0);
  }

  function sanitizeHTML(html) {
    if (html == null) return '';
    let s = String(html);

    // 스크립트/이벤트 제거(기존 그대로)
    s = s.replace(/<script\b[^>]*>[\s\S]*?<\/script>/gi, '');
    s = s.replace(/\son[a-z]+\s*=\s*(?:"[^"]*"|'[^']*'|[^\s>]+)/gi, '');

    // DOM으로 파싱
    const tmp = document.createElement('div');
    tmp.innerHTML = s;

    // 보이지 않는 문자 제거 함수
    const stripZW = t => String(t || '').replace(/[\uFEFF\u200B\u200C\u200D]/g, '');

    // 1) ql-formula 전체를 data-value(또는 MathML annotation)로 교체
    tmp.querySelectorAll('span.ql-formula').forEach(el => {
      let tex =
        el.getAttribute('data-value') ||
        el.querySelector('annotation[encoding="application/x-tex"]')?.textContent ||
        '';
      tex = stripZW(tex).trim();
      el.outerHTML = tex ? `\\(${tex}\\)` : '';   // MathJax 인라인 수식으로 치환
    });

    // 2) 혹시 남은 KaTeX 렌더 트리 통째 제거(안전망)
    tmp.querySelectorAll('span.katex, .katex-mathml, .katex-html').forEach(el => el.remove());

    // 3) 일반적인 마크업 정리
    s = tmp.innerHTML;
    s = s.replace(/^(?:\s*<br\s*\/?>\s*)+/i, '');                       // 앞쪽 연속 <br> 제거
    s = s.replace(/^\s*<p[^>]*>([\s\S]*?)<\/p>\s*$/i, '$1');            // 단일 <p> 벗겨내기
    s = s.replace(/<\/p>\s*<p[^>]*>/gi, '<br>');                        // 연속 <p>는 줄바꿈으로

    // 4) 텍스트에 남은 제로폭 문자 제거
    s = stripZW(s);

    return s;
  }

  // 보기 존재 여부 판단(텍스트가 없더라도 <img>, 수식, 숫자 등 포함하면 "존재")
  function hasOptionContent(html) {
    if (html == null) return false;
    const s = String(html).trim();

    // 'null', 빈태그, 공백만 → 없음
    const onlySpaces = s.replace(/&nbsp;|\s|<br\s*\/?>/gi, '').length === 0;
    if (!s || /^(null|null\.|undefined)$/i.test(s) || onlySpaces) return false;

    // 이미지/수식/숫자/문자 존재 여부
    if (/<img\b|ql-formula|katex|annotation/i.test(s)) return true;
    if (/[0-9A-Za-z가-힣√∑∫∞π±×÷^]/.test(s)) return true;

    // 태그 벗겨서 텍스트 남는지
    const tmp = document.createElement('div');
    tmp.innerHTML = s;
    const text = (tmp.textContent || '').replace(/\s+/g,'').trim();
    return text.length > 0;
  }

  // ✅ DB 플래그 우선: question_type 0=주관식, 1=객관식
  function readQType(q) {
    // DB 필드 우선 순위
    const raw =
      q?.question_type ??
      q?.questionType ??
      q?.type ??
      q?.question_type_flag;

    if (raw !== undefined && raw !== null) {
      // 숫자/문자 모두 대응
      const v = (typeof raw === 'string') ? raw.trim().toLowerCase() : raw;

      // 정확 규칙: 0 -> 주관식, 1 -> 객관식
      if (v === 0 || v === '0') return 'S'; // Subjective
      if (v === 1 || v === '1') return 'O'; // Objective

      // 혹시 문자로 들어오는 다른 값들까지 안전 처리
      if (typeof v === 'string') {
        if (v.includes('주관') || v === 's' || v === 'true') return 'S';
        if (v.includes('객관') || v === 'o' || v === 'false') return 'O';
      }
    }

    // ⬇ 플래그가 아예 없을 때만 보기 유무로 판정 (보기가 하나라도 있으면 객관식)
    const anyOpt = [q?.option1, q?.option2, q?.option3, q?.option4, q?.option5]
      .some(hasOptionContent);
    return anyOpt ? 'O' : 'S';
  }

  // 이걸로 통일해서 사용
  const isSubjective = (q) => readQType(q) === 'S';

  // ── hidden store ─────────────────────────────────────────────────────
  function initHiddenStore() {
    const store = $('#hiddenStore');
    if (!store) return;
    store.innerHTML = '';
    for (let i = 0; i < questions.length; i++) {
      const q = questions[i];

      const qid = document.createElement('input');
      qid.type  = 'hidden';
      qid.name  = `questionIds[${i}]`;
      qid.value = (q && q.questionId != null) ? q.questionId : '';
      store.appendChild(qid);

      const ans = document.createElement('input');
      ans.type  = 'hidden';
      ans.name  = `answers[${i}]`;
      ans.value = '';
      store.appendChild(ans);
    }
  }
  function getHiddenAnswer(i){ return document.querySelector(`input[name="answers[${i}]"]`)?.value || ''; }
  function setHiddenAnswer(i, val){ const el = document.querySelector(`input[name="answers[${i}]"]`); if (el) el.value = String(val ?? ''); }
  window.__setHidden = setHiddenAnswer;

  // ── 렌더링 ───────────────────────────────────────────────────────────
  function renderQuestions() {
    document.documentElement.scrollTop = 0;
    document.body.scrollTop = 0;

    const container = $('#questionContainer');
    if (!container) return;

    container.innerHTML = '<div id="colL" class="question-col"></div><div id="colR" class="question-col"></div>';
    const colL = $('#colL');
    const colR = $('#colR');

    const sorted = [...questions].sort((a, b) => a.num - b.num);
    const start = (currentPage - 1) * questionsPerPage;
    const end   = Math.min(start + questionsPerPage, sorted.length);
    const pageQuestions = sorted.slice(start, end);
    const leftCount = Math.ceil(pageQuestions.length / 2);

    const renderedArticleUrls = new Set();

    const makeBox = (q, globalIndex) => {
      const box = document.createElement('div');
      box.className = 'question-box';
      box.id = `qbox-${globalIndex}`;

      // 공통지문
      let articleHtml = '';
      for (const [url, [startIdx, endIdx]] of Object.entries(articleRangeMap)) {
        if (globalIndex === startIdx && !renderedArticleUrls.has(url)) {
          const rangeLabel = (startIdx === endIdx) ? '' :
            `<div style="font-weight:bold; margin-bottom:0.3rem;">[Q${startIdx + 1}~Q${endIdx + 1}] 공통 지문</div>`;
          articleHtml = `
            <div class="article-box">
              ${rangeLabel}
              <img src="${url}" alt="">
            </div>`;
          renderedArticleUrls.add(url);
          break;
        }
      }

      const contextHtml = q.questionContext
        ? `<div style="margin-top:0.5rem;"><img src="${q.questionContext}" alt=""></div>`
        : '';

      const titleHtml = normalizeRootsToLaTeX(sanitizeHTML(q.questionTitle || ''));
      const subjective = isSubjective(q);
      const saved = getHiddenAnswer(globalIndex);

      let bodyHtml = '';
      if (subjective) {
        // 주관식
        bodyHtml = `
          <div class="short-answer">
            <input type="text" class="form-control" placeholder="정답 입력"
                   value="${(saved||'').replace(/"/g,'&quot;')}"
                   oninput="window.__setHidden(${globalIndex}, this.value)">
          </div>`;
      } else {
        // 객관식
        const choiceBlock = [1,2,3,4,5].map(n => {
          const raw = q['option' + n];
          if (!hasOptionContent(raw)) return '';
          const optHTML = normalizeRootsToLaTeX(sanitizeHTML(raw));
          const checked = (String(saved) === String(n)) ? 'checked' : '';
          const labelHTML = `${'①②③④⑤'[n-1]} <span class="choice-content">${optHTML}</span>`;
          return `
            <div class="choice-row">
              <input type="radio" id="q${globalIndex}_${n}" name="visible_${globalIndex}"
                    value="${n}" ${checked}
                    onchange="syncOMR(${globalIndex}, ${n}); window.__setHidden(${globalIndex}, ${n});">
              <label for="q${globalIndex}_${n}" class="choice-label">${labelHTML}</label>
            </div>`;
        }).join('');
        bodyHtml = choiceBlock || `<div class="text-muted small">보기 없음</div>`;
      }

      box.innerHTML = `
        ${articleHtml}
        <div class="q-title">${globalIndex + 1}. ${titleHtml}</div>
        ${contextHtml}
        ${bodyHtml}
      `;
      typesetMath(box);
      return box;
    };

    for (let i = 0; i < leftCount; i++) {
      const gi = start + i;
      if (pageQuestions[i]) colL.appendChild(makeBox(pageQuestions[i], gi));
    }
    for (let j = leftCount; j < pageQuestions.length; j++) {
      const gi = start + j;
      if (pageQuestions[j]) colR.appendChild(makeBox(pageQuestions[j], gi));
    }

    // OMR 하이라이트 복원(객관식만)
    for (let i = start; i < end; i++) {
      const q = sorted[i];
      if (!isSubjective(q)) {
        const saved = getHiddenAnswer(i);
        if (saved) syncOMR(i, Number(saved));
      }
    }

    const pageInfo = $('#pageInfo');
    if (pageInfo) pageInfo.textContent = `${currentPage} / ${totalPages}`;
  }

  function renderOMR() {
    const omr = $('#omrContainer');
    if (!omr) return;
    omr.innerHTML = '';

    const list = [...questions].sort((a,b)=>a.num-b.num);
    for (let i = 0; i < list.length; i++) {
      const q = list[i];
      const subjective = isSubjective(q);
      const saved = getHiddenAnswer(i);

      const line = document.createElement('div');
      line.className = 'omr-line';

      if (subjective) {
        line.innerHTML = `
          <span style="cursor:pointer; font-weight:bold;" onclick="goToQuestion(${i})">Q${i + 1}:</span>
          <input type="text" class="form-control form-control-sm"
                 style="width: 140px; display:inline-block; margin-left:6px;"
                 placeholder="정답"
                 value="${(saved||'').replace(/"/g,'&quot;')}"
                 oninput="window.__setHidden(${i}, this.value)">

        `;
      } else {
        line.innerHTML = `
          <span style="cursor:pointer; font-weight:bold;" onclick="goToQuestion(${i})">Q${i + 1}:</span>
          ${[1,2,3,4,5].map(n => {
            const opt = q['option' + n];
            if (!hasOptionContent(opt)) return '';
            const selected = (String(saved) === String(n)) ? 'selected' : '';
            return `<button type="button" data-index="${i}" data-value="${n}" class="${selected}" onclick="selectAnswer(this)">${'①②③④⑤'[n - 1]}</button>`;
          }).join('')}
        `;
      }
      omr.appendChild(line);
    }
  }

  // ── 네비게이션 ───────────────────────────────────────────────────────
  window.prevPage = function(){ if (currentPage > 1) { currentPage--; renderQuestions(); } };
  window.nextPage = function(){ if (currentPage < totalPages) { currentPage++; renderQuestions(); } };
  window.goToQuestion = function(index){
    const targetPage = Math.floor(index / questionsPerPage) + 1;
    if (currentPage !== targetPage) {
      currentPage = targetPage;
      renderQuestions();
      setTimeout(() => document.getElementById(`qbox-${index}`)?.scrollIntoView({ behavior: 'smooth', block: 'start' }), 60);
    }
  };

  // ── OMR/라디오 동기화(객관식용) ───────────────────────────────────────
  window.selectAnswer = function(btn){
    const index = parseInt(btn.getAttribute('data-index'), 10);
    const value = parseInt(btn.getAttribute('data-value'), 10);
    setHiddenAnswer(index, value);
    $$(`.omr-line button[data-index='${index}']`).forEach(b => b.classList.remove('selected'));
    btn.classList.add('selected');
    const radio = document.getElementById(`q${index}_${value}`);
    if (radio) radio.checked = true; else window.goToQuestion(index);
  };
  window.syncOMR = function(index, value){
    $$(`.omr-line button[data-index='${index}']`).forEach(b => b.classList.remove('selected'));
    const btn = $(`.omr-line button[data-index='${index}'][data-value='${value}']`);
    if (btn) btn.classList.add('selected');
  };

  // ── 제출 전 hiddenStore 정리 ─────────────────────────────────────────
  function normalizeStoreForSubmit() {
    const store = document.getElementById('hiddenStore');
    if (!store) return;

    // 1) questionId 비어 있으면 페어 제거
    store.querySelectorAll('input[name^="questionIds["]').forEach(qid => {
      const val = (qid.value ?? '').trim();
      if (!val || val.toLowerCase() === 'null') {
        const m = qid.name.match(/\[(\d+)\]/);
        const idx = m ? m[1] : null;
        const ans = idx ? store.querySelector(`input[name="answers[${idx}]"]`) : null;
        qid.remove();
        if (ans) ans.remove();
      }
    });

    // 2) 객관식 미응답만 -1, 주관식은 '' 유지
    const list = [...questions].sort((a,b)=>a.num-b.num);
    for (let i = 0; i < list.length; i++){
      const ans = store.querySelector(`input[name="answers[${i}]"]`);
      if (!ans) continue;
      if ((ans.value ?? '') === '' && !isSubjective(list[i])) ans.value = '-1';
    }
  }

  // ── 제출/오버레이 ───────────────────────────────────────────────────
  function showLoading(msg = '답안을 제출하고 있어요…'){
    const ov  = $('#loadingOverlay');
    const txt = $('#loadingText');
    if (txt) txt.textContent = msg;
    if (ov) {
      ov.classList.add('show');
      document.body.style.overflow = 'hidden';
      $$('button, select, input:not([type="hidden"]), textarea').forEach(el => el.disabled = true);
    }
  }
  function hideLoading(){
    const ov = $('#loadingOverlay');
    if (ov) {
      ov.classList.remove('show');
      document.body.style.overflow = '';
    }
  }

  window.checkBeforeSubmit = function(skipConfirm = false){
    if (isSubmitting) return;
    if (timerHandle) { clearInterval(timerHandle); timerHandle = null; }

    // 안내(선택)
    const total = questions.length;
    const unanswered = [];
    for (let i = 0; i < total; i++) {
      const a = getHiddenAnswer(i);
      if (!a) unanswered.push(i + 1);
    }
    if (!skipConfirm && unanswered.length > 0) {
      const ok = confirm(`풀지 않은 문제가 ${unanswered.length}개 있습니다.\n(문제 번호: ${unanswered.join(', ')})\n제출하시겠습니까? 미응답은 오답 처리됩니다.`);
      if (!ok) return;
    }

    isSubmitting = true;
    showLoading('답안을 제출하고 있어요…');

    normalizeStoreForSubmit();

    const form = $('#examForm');
    const formData = new FormData(form);
    const token = localStorage.getItem('token');

    const controller = new AbortController();
    const timeoutId  = setTimeout(() => controller.abort(), 30000);

    fetch('/submitExam', {
      method: 'POST',
      headers: token ? { 'Authorization': 'Bearer ' + token } : {},
      body: formData,
      signal: controller.signal
    })
    .then(async res => {
      clearTimeout(timeoutId);
      if (!res.ok) {
        hideLoading();
        isSubmitting = false;
        const txt = await res.text().catch(()=> '');
        console.error('submit failed:', res.status, txt);
        alert('제출 중 서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.');
        return;
      }
      if (res.redirected) { window.location.href = res.url; return; }
      const html = await res.text();
      document.body.innerHTML = html;
    })
    .catch(err => {
      console.error(err);
      alert('제출 중 오류가 발생했습니다. 네트워크 상태를 확인하고 다시 시도해 주세요.');
      hideLoading();
      isSubmitting = false;
    });
  };

  // ── 타이머 ──────────────────────────────────────────────────────────
  function startTimer(){
    const warningEl = $('#time-warning');
    const elapsedHidden = $('#elapsedTime');

    if (timerHandle) { clearInterval(timerHandle); timerHandle = null; }

    timerHandle = setInterval(() => {
      const timerEl = document.getElementById('timer');
      if (!timerEl) { clearInterval(timerHandle); timerHandle = null; return; }

      const m = Math.floor(remainingSeconds / 60);
      const s = remainingSeconds % 60;
      timerEl.textContent = `${String(m).padStart(2,'0')}:${String(s).padStart(2,'0')}`;

      if (elapsedHidden) elapsedHidden.value = (Number(elapsedHidden.value || 0) + 1);

      if (remainingSeconds <= 300 && warningEl) warningEl.style.display = 'block';
      if (remainingSeconds <= 0) {
        clearInterval(timerHandle); timerHandle = null;
        alert('시험 시간이 종료되었습니다. 자동 제출합니다.');
        window.checkBeforeSubmit(true);
        return;
      }
      remainingSeconds--;
    }, 1000);
  }

  // ── 초기화 ───────────────────────────────────────────────────────────
  document.addEventListener('DOMContentLoaded', () => {
    initHiddenStore();
    renderOMR();
    renderQuestions();
    startTimer();
  });
})();

