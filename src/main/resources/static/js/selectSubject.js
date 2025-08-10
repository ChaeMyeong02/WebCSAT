document.addEventListener("DOMContentLoaded", function () {
  const token = localStorage.getItem('token');
  const questionType = document.querySelector('input[name="questionType"]')?.value || '';

  const subjectSelect     = document.getElementById('subject-select');
  const subclassSelect    = document.getElementById('subclass-select');
  const csatDateSelect    = document.getElementById('csat-date-select');   // 모의고사에서만 존재
  const csatDateWrapper   = document.getElementById('csat-date-wrapper');  // 모의고사에서만 존재

  // ---- 유틸 ----
  // ───── 401 알림 + 로그인 페이지 이동 로직 추가 ─────
  const fetchJSON = (url) =>
    fetch(url, { headers: token ? { 'Authorization': `Bearer ${token}` } : {} })
      .then(r => {
        if (r.status === 401) {               // ➟ 인증 만료·비로그인
          alert('로그인이 필요합니다!');       // ✅ 알림
          window.location.href = '/login';    // ✅ 로그인 페이지로
          throw new Error('Unauthorized');    // 이후 로직 중단
        }
        if (!r.ok) throw new Error(`${url} ${r.status}`);
        return r.json();
      });

  const setOptions = (selectEl, items, placeholder) => {
    if (!selectEl) return; // ✅ null 가드
    selectEl.innerHTML = `<option value="">${placeholder}</option>`;
    (items || []).forEach(({ value, text }) => {
      const opt = document.createElement('option');
      opt.value = value; opt.textContent = text;
      selectEl.appendChild(opt);
    });
  };

  const setDisabled = (el, v) => { if (el) el.disabled = !!v; }; // ✅ null 가드

  // ---- 모드별 UI 처리 ----
  // 문제은행이면 응시월 숨김(있을 때만)
  if (questionType === '문제은행' && csatDateWrapper) {
    csatDateWrapper.style.display = 'none';
    csatDateSelect?.removeAttribute('required');
  }

  // ---- 초기 상태 ----
  setDisabled(subclassSelect, true);
  setOptions(subclassSelect, [], '부과목 선택');

  // 모의고사일 때만 응시월 초기화/비활성
  if (questionType !== '문제은행') {
    setDisabled(csatDateSelect, true);
    setOptions(csatDateSelect, [], '응시 월 선택');
  }

  // ---- 과목 변경 ----
  subjectSelect.addEventListener('change', () => {
    const subjectId = subjectSelect.value;

    // 부과목/응시월 초기화
    setOptions(subclassSelect, [], '부과목 선택');
    if (questionType !== '문제은행') {
      setOptions(csatDateSelect, [], '응시 월 선택');
      setDisabled(csatDateSelect, true);
    }

    if (!subjectId) {
      setDisabled(subclassSelect, true);
      return;
    }
    setDisabled(subclassSelect, false);

    // 선택과목(optional=1)만 요청
    const q = new URLSearchParams({ subjectId, optional: '1' });
    fetchJSON(`/api/subclasses?${q.toString()}`)
      .then(list => {
        // 서버가 optional 필터를 무시하는 경우를 대비해 프런트에서 한 번 더 필터
        const filtered = (Array.isArray(list) ? list : []).filter(sc => {
          const v = sc.optional ?? sc.is_optional ?? sc.optionalFlag;
          return v === true || v === 1 || v === '1';
        });
        const items = filtered.map(sc => ({
          value: sc.subclassId ?? sc.subclass_id,
          text : sc.subclassName ?? sc.subclass_name
        }));
        setOptions(subclassSelect, items, '부과목 선택');

        // 옵션이 없으면 다시 비활성
        if (!items.length) setDisabled(subclassSelect, true);
      })
      .catch(err => {
        console.error('부과목 로드 실패:', err);
        setDisabled(subclassSelect, true);
      });
  });

  // ---- 부과목 변경 ----
  subclassSelect.addEventListener('change', () => {
    const subclassId = subclassSelect.value;

    // 문제은행 모드면 응시월 자체를 다루지 않음
    if (questionType === '문제은행') return;

    // 모의고사: 응시월 select가 존재할 때만 처리
    if (!csatDateSelect) return;

    setOptions(csatDateSelect, [], '응시 월 선택');

    if (!subclassId) {
      setDisabled(csatDateSelect, true);
      return;
    }
    setDisabled(csatDateSelect, false);

    fetchJSON(`/api/csatDates?subclassId=${encodeURIComponent(subclassId)}`)
      .then(dates => {
        const items = (Array.isArray(dates) ? dates : []).map(d => ({ value: d, text: d }));
        setOptions(csatDateSelect, items, '응시 월 선택');
        if (!items.length) setDisabled(csatDateSelect, true);
      })
      .catch(err => {
        console.error('응시월 로드 실패:', err);
        setDisabled(csatDateSelect, true);
      });
  });
});
