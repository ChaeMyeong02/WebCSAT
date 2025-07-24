document.addEventListener("DOMContentLoaded", function () {
  const token = localStorage.getItem('token');
  const subjectSelect = document.getElementById('subject-select');
  const subclassSelect = document.getElementById('subclass-select');
  const csatDateSelect = document.getElementById('csat-date-select');

  subjectSelect.addEventListener('change', function () {
    const subjectId = subjectSelect.value;

    subclassSelect.innerHTML = "<option value=''>부과목 선택</option>";
    csatDateSelect.innerHTML = "<option value=''>응시 월 선택</option>";

    fetch(`/api/subclasses?subjectId=${subjectId}`, {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      }
    })
      .then(res => {
        if (!res.ok) throw new Error(`Fetch 실패: ${res.status}`);
        return res.json();
      })
      .then(data => {
        data.forEach(sub => {
          const option = document.createElement('option');
          option.value = sub.subclassId;
          option.text = sub.subclassName;
          subclassSelect.appendChild(option);
        });
      })
      .catch(err => {
        console.error('부과목 가져오기 실패:', err);
      });
  });

  subclassSelect.addEventListener('change', function () {
    const subclassId = subclassSelect.value;

    csatDateSelect.innerHTML = "<option value=''>응시 월 선택</option>";

    fetch(`/api/csatDates?subclassId=${subclassId}`, {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      }
    })
      .then(res => {
        if (!res.ok) throw new Error(`Fetch 실패: ${res.status}`);
        return res.json();
      })
      .then(data => {
        data.forEach(date => {
          const option = document.createElement('option');
          option.value = date;
          option.text = date;
          csatDateSelect.appendChild(option);
        });
      })
      .catch(err => {
        console.error('응시 월 가져오기 실패:', err);
      });
  });
});
