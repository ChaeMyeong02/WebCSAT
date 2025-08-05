// /js/exam.js

(() => {
  // ── 전역 데이터 주입 받기 ─────────────────────────────────────────────
  const EX = window.__EXAM__ || {};
  const questions = Array.isArray(EX.questions) ? EX.questions : [];
  const articleRangeMap = EX.articleRangeMap || {};
  let remainingSeconds = Number(EX.examTimeSeconds || 3000);

  // ── 상태 ──────────────────────────────────────────────────────────────
  let currentPage = 1;
  const questionsPerPage = 6;
  const totalPages = Math.ceil(questions.length / questionsPerPage);
  let isSubmitting = false;

  // ── 유틸 ──────────────────────────────────────────────────────────────
  const $ = (sel, root = document) => root.querySelector(sel);
  const $$ = (sel, root = document) => root.querySelectorAll(sel);

  function plainText(html) {
    const tmp = document.createElement('div');
    tmp.innerHTML = html ?? '';
    return (tmp.textContent || '').replace(/\s+/g, ' ').trim();
  }

  // ── 렌더링 ────────────────────────────────────────────────────────────
  function renderQuestions() {
    document.documentElement.scrollTop = 0;
    document.body.scrollTop = 0;

    const container = $('#questionContainer');
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
            </div>
          `;
          renderedArticleUrls.add(url);
          break;
        }
      }

      const contextHtml = q.questionContext
        ? `<div style="margin-top:0.5rem;"><img src="${q.questionContext}" alt=""></div>`
        : '';

      const titleText = plainText(q.questionTitle);

      box.innerHTML = `
        ${articleHtml}
        <p class="q-title">${globalIndex + 1}. ${titleText}</p>
        ${contextHtml}
        ${[1,2,3,4,5].map(n => {
          const optText = plainText(q['option' + n]);
          return `
            <div>
              <input type="radio" id="q${globalIndex}_${n}" name="answers[${globalIndex}]"
                     value="${n}" onchange="syncOMR(${globalIndex}, ${n})">
              <label for="q${globalIndex}_${n}" class="choice-label">${'①②③④⑤'[n-1]} ${optText}</label>
            </div>
          `;
        }).join('')}
        <input type="hidden" name="questionIds[${globalIndex}]" value="${q.questionId}">
      `;
      return box;
    };

    for (let i = 0; i < leftCount; i++) {
      const globalIndex = start + i;
      colL.appendChild(makeBox(pageQuestions[i], globalIndex));
    }
    for (let j = leftCount; j < pageQuestions.length; j++) {
      const globalIndex = start + j;
      colR.appendChild(makeBox(pageQuestions[j], globalIndex));
    }

    $('#pageInfo').textContent = `${currentPage} / ${totalPages}`;
  }

  function renderOMR() {
    const omr = $('#omrContainer');
    omr.innerHTML = '';
    for (let i = 0; i < questions.length; i++) {
      const omrLine = document.createElement('div');
      omrLine.className = 'omr-line';
      omrLine.innerHTML = `
        <span style="cursor:pointer; font-weight:bold;" onclick="goToQuestion(${i})">Q${i + 1}:</span>
        ${[1,2,3,4,5].map(n =>
          `<button type="button" data-index="${i}" data-value="${n}" onclick="selectAnswer(this)">${'①②③④⑤'[n - 1]}</button>`
        ).join('')}
      `;
      omr.appendChild(omrLine);
    }
  }

  // ── 네비게이션 ───────────────────────────────────────────────────────
  window.prevPage = function() {
    if (currentPage > 1) {
      currentPage--;
      renderQuestions();
    }
  };
  window.nextPage = function() {
    if (currentPage < totalPages) {
      currentPage++;
      renderQuestions();
    }
  };
  window.goToQuestion = function(index) {
    const targetPage = Math.floor(index / questionsPerPage) + 1;
    const isSamePage = currentPage === targetPage;
    if (!isSamePage) {
      currentPage = targetPage;
      renderQuestions();
      setTimeout(() => {
        const el = document.getElementById(`qbox-${index}`);
        if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' });
      }, 100);
    }
  };

  // ── OMR 동기화 ──────────────────────────────────────────────────────
  window.selectAnswer = function(btn) {
    const index = parseInt(btn.getAttribute('data-index'), 10);
    const value = parseInt(btn.getAttribute('data-value'), 10);
    const targetInput = document.getElementById(`q${index}_${value}`);
    if (targetInput) {
      targetInput.checked = true;
      $$(`.omr-line button[data-index='${index}']`).forEach(b => b.classList.remove('selected'));
      btn.classList.add('selected');
    } else {
      window.goToQuestion(index);
    }
  };
  window.syncOMR = function(index, value) {
    $$(`.omr-line button[data-index='${index}']`).forEach(b => b.classList.remove('selected'));
    const btn = $(`.omr-line button[data-index='${index}'][data-value='${value}']`);
    if (btn) btn.classList.add('selected');
  };

  // ── 제출/오버레이 ───────────────────────────────────────────────────
  function showLoading(msg = '답안을 제출하고 있어요…') {
    const ov  = $('#loadingOverlay');
    const txt = $('#loadingText');
    if (txt) txt.textContent = msg;
    ov.classList.add('show');
    document.body.style.overflow = 'hidden';
    $$('button, select, input:not([type="hidden"]), textarea').forEach(el => el.disabled = true);
  }
  function hideLoading() {
      document.getElementById('loadingOverlay').classList.remove('show');
      document.body.style.overflow = '';
  }

  window.checkBeforeSubmit = function(skipConfirm = false) {
    if (isSubmitting) return;

    const total = questions.length;
    const unanswered = [];
    for (let i = 0; i < total; i++) {
      const selected = document.querySelector(`input[name="answers[${i}]"]:checked`);
      if (!selected) unanswered.push(i + 1);
    }
    if (!skipConfirm && unanswered.length > 0) {
      const ok = confirm(`풀지 않은 문제가 ${unanswered.length}개 있습니다.\n(문제 번호: ${unanswered.join(', ')})\n제출하시겠습니까? 미응답은 오답 처리됩니다.`);
      if (!ok) return;
    }

    isSubmitting = true;

    // FormData는 잠그기 전에 먼저 수집
    const form = $('#examForm');
    const formData = new FormData(form);
    const token = localStorage.getItem('token');

    showLoading('답안을 제출하고 있어요…');

    const controller = new AbortController();
    const timeoutId  = setTimeout(() => controller.abort(), 30000);

    fetch('/submitExam', {
      method: 'POST',
      headers: token ? { 'Authorization': 'Bearer ' + token } : {},
      body: formData,
      signal: controller.signal
    })
    .then(res => {
      clearTimeout(timeoutId);
      if (res.redirected) { window.location.href = res.url; return; }
      return res.text().then(html => { document.body.innerHTML = html; });
    })
    .catch(err => {
      console.error(err);
      alert('제출 중 오류가 발생했습니다. 네트워크 상태를 확인하고 다시 시도해 주세요.');
      hideLoading();
      isSubmitting = false;
    });
  };

  // ── 타이머 ──────────────────────────────────────────────────────────
  function startTimer() {
    const warningEl = $('#time-warning');
    const elapsedHidden = $('#elapsedTime');
    const timerId = setInterval(() => {
      const m = Math.floor(remainingSeconds / 60);
      const s = remainingSeconds % 60;
      $('#timer').textContent = `${String(m).padStart(2,'0')}:${String(s).padStart(2,'0')}`;

      const prev = Number(elapsedHidden.value || 0);
      elapsedHidden.value = prev + 1;

      if (remainingSeconds <= 300) warningEl.style.display = 'block';
      if (remainingSeconds <= 0) {
        clearInterval(timerId);
        alert('시험 시간이 종료되었습니다. 자동 제출합니다.');
        window.checkBeforeSubmit(true);
        return;
      }
      remainingSeconds--;
    }, 1000);
  }

  // ── 초기화 ──────────────────────────────────────────────────────────
  document.addEventListener('DOMContentLoaded', () => {
    renderOMR();
    renderQuestions();
    startTimer();
  });
})();
