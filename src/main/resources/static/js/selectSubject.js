document.addEventListener("DOMContentLoaded", function () {
  const token = localStorage.getItem('token');
  const questionType = document.querySelector('input[name="questionType"]')?.value || '';

  const subjectSelect  = document.getElementById('subject-select');
  const subclassSelect = document.getElementById('subclass-select');
  const csatDateSelect = document.getElementById('csat-date-select');
  const csatDateWrapper = document.getElementById('csat-date-wrapper');

  // 문제은행이면 응시월 숨김
  if (questionType === '문제은행' && csatDateWrapper) {
    csatDateWrapper.style.display = 'none';
    csatDateSelect?.removeAttribute('required');
  }

  // 유틸
  const fetchJSON = (url) =>
    fetch(url, { headers: token ? { 'Authorization': `Bearer ${token}` } : {} })
      .then(r => { if (!r.ok) throw new Error(`${url} ${r.status}`); return r.json(); });

  const setOptions = (selectEl, items, placeholder) => {
    selectEl.innerHTML = `<option value="">${placeholder}</option>`;
    items.forEach(({ value, text }) => {
      const opt = document.createElement('option');
      opt.value = value; opt.textContent = text;
      selectEl.appendChild(opt);
    });
  };

  // 초기 상태: 부과목/응시월 비활성화
  subclassSelect.disabled = true;
  csatDateSelect.disabled = true;
  setOptions(subclassSelect, [], '부과목 선택');
  setOptions(csatDateSelect, [], '응시 월 선택');

  // 과목 변경 → 선택과목(optional=1) 부과목만 로딩
  subjectSelect.addEventListener('change', () => {
    const subjectId = subjectSelect.value;

    // 부과목/응시월 초기화
    setOptions(subclassSelect, [], '부과목 선택');
    setOptions(csatDateSelect, [], '응시 월 선택');
    csatDateSelect.disabled = true;

    if (!subjectId) {
      subclassSelect.disabled = true;
      return;
    }
    subclassSelect.disabled = false;

    const q = new URLSearchParams({ subjectId, optional: '1' });
    fetchJSON(`/api/subclasses?${q.toString()}`)
      .then(list => {
        // 혹시 optional 필터가 서버에서 무시되면 프런트에서 한 번 더 필터
        const filtered = (Array.isArray(list) ? list : []).filter(sc => {
          const v = sc.optional ?? sc.is_optional ?? sc.optionalFlag;
          return v === true || v === 1 || v === '1';
        });
        const items = filtered.map(sc => ({
          value: sc.subclassId ?? sc.subclass_id,
          text : sc.subclassName ?? sc.subclass_name
        }));
        setOptions(subclassSelect, items, '부과목 선택');
      })
      .catch(err => {
        console.error('부과목 로드 실패:', err);
        subclassSelect.disabled = true;
      });
  });

  // 부과목 변경 → 해당 부과목의 응시월 로딩
  subclassSelect.addEventListener('change', () => {
    const subclassId = subclassSelect.value;

    setOptions(csatDateSelect, [], '응시 월 선택');

    // 문제은행 모드면 응시월 자체를 안 보이므로 스킵
    if (questionType === '문제은행') return;

    if (!subclassId) {
      csatDateSelect.disabled = true;
      return;
    }

    csatDateSelect.disabled = false;

    fetchJSON(`/api/csatDates?subclassId=${encodeURIComponent(subclassId)}`)
      .then(dates => {
        const items = (Array.isArray(dates) ? dates : []).map(d => ({ value: d, text: d }));
        setOptions(csatDateSelect, items, '응시 월 선택');
      })
      .catch(err => {
        console.error('응시월 로드 실패:', err);
        csatDateSelect.disabled = true;
      });
  });
});
