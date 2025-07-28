// subjectManage.js

const subjectListEl = document.getElementById("subjectList");
const subclassListEl = document.getElementById("subclassList");
const subclassSectionEl = document.getElementById("subclassSection");

let selectedSubjectId = null;
let selectedSubjectEl = null;
let selectedSubjectOption = null;

function loadSubjects() {
  authFetch("/api/subject")
    .then(res => res.json())
    .then(subjects => {
      subjectListEl.innerHTML = "";
      subclassListEl.innerHTML = `<li class="list-group-item text-muted">과목을 선택하지 않았습니다</li>`;
      subclassSectionEl.style.display = "block";

      subjects.forEach(subject => {
        const li = document.createElement("li");
        li.className = "list-group-item d-flex justify-content-between align-items-center subject-item";
        li.setAttribute("data-subject-id", subject.subjectId);
        li.innerHTML = `
          <div>
            <strong>${subject.subjectName}</strong>
            <small class="ms-2">(공통과목 여부 : ${subject.option ? 'O' : 'X'}, 시험 시간 ${subject.min}분, ${subject.relative ? '상대평가' : '절대평가'})</small>
          </div>
          <div>
            <button class="btn btn-sm btn-outline-primary me-1">수정</button>
            <button class="btn btn-sm btn-outline-danger">삭제</button>
          </div>`;

        li.addEventListener("click", () => selectSubject(subject.subjectId, li));

        li.querySelector(".btn-outline-primary").addEventListener("click", e => editSubject(e, subject.subjectId));
        li.querySelector(".btn-outline-danger").addEventListener("click", e => deleteSubject(e, subject.subjectId));

        subjectListEl.appendChild(li);
      });
    });
}

function selectSubject(subjectId, element) {
  selectedSubjectId = subjectId;
  authFetch(`/api/subject/${subjectId}`)
    .then(res => res.json())
    .then(subject => {
      selectedSubjectOption = subject.option;
    });
  if (selectedSubjectEl) {
    selectedSubjectEl.classList.remove("selected-subject");
  }
  element.classList.add("selected-subject");
  selectedSubjectEl = element;

  authFetch(`/api/subclasses?subjectId=${subjectId}`)
    .then(res => res.json())
    .then(subclasses => {
      subclassListEl.innerHTML = "";

      if (subclasses.length === 0) {
        subclassListEl.innerHTML = `<li class="list-group-item text-muted">세부 과목이 없습니다</li>`;
      } else {
        subclasses.forEach(sub => {
          const li = document.createElement("li");
          li.className = "list-group-item d-flex justify-content-between align-items-center";
          li.innerHTML = `
            <div>
              <strong>${sub.subclassName}</strong>
              <small class="ms-2">(${sub.optional ? '선택과목' : '공통과목'}, 듣기: ${sub.listening ? '듣기과목' : '일반과목'}, 문제 수: ${sub.count})</small>
            </div>
            <div>
              <button class="btn btn-sm btn-outline-primary me-1" onclick="editSubclass(${sub.subclassId})">수정</button>
              <button class="btn btn-sm btn-outline-danger" onclick="deleteSubclass(${sub.subclassId})">삭제</button>
            </div>`;
          subclassListEl.appendChild(li);
        });
      }
      subclassSectionEl.style.display = "block";
    });
}


function showSubjectModal() {
  document.getElementById("subjectForm").reset();
  document.getElementById("subjectId").value = "";
  document.getElementById("subjectModalTitle").innerText = "새 과목 추가";
  document.getElementById("subjectSubmitBtn").innerText = "저장";
  document.getElementById("option").disabled = false;
  new bootstrap.Modal(document.getElementById("subjectModal")).show();
}

function editSubject(e, subjectId) {
  e.stopPropagation();
  authFetch(`/api/subject/${subjectId}`)
    .then(res => res.json())
    .then(subject => {
      document.getElementById("subjectId").value = subject.subjectId;
      document.getElementById("subjectName").value = subject.subjectName;
      document.getElementById("option").value = subject.option;
      document.getElementById("min").value = subject.min;
      document.getElementById("relative").value = subject.relative;
      document.getElementById("subjectModalTitle").innerText = "과목 수정";
      document.getElementById("subjectSubmitBtn").innerText = "수정";
      document.getElementById("option").disabled = true;
      new bootstrap.Modal(document.getElementById("subjectModal")).show();
    });
}



function deleteSubject(e, subjectId) {
  e.stopPropagation();
  if (!confirm("과목을 삭제하시겠습니까? (선택과목과 문제가 없어야 합니다)")) return;
  authFetch(`/api/subject/${subjectId}`, { method: "DELETE" })
    .then(res => {
      if (res.ok) loadSubjects();
      else alert("삭제 실패: 문제나 선택과목이 남아있을 수 있습니다");
    });
}

document.getElementById("subjectForm").addEventListener("submit", e => {
  e.preventDefault();
  const subjectId = document.getElementById("subjectId").value;
  const payload = {
    subjectName: document.getElementById("subjectName").value,
    option: +document.getElementById("option").value,
    min: +document.getElementById("min").value,
    relative: document.getElementById("relative").value
  };
  const method = subjectId ? "PUT" : "POST";
  const url = subjectId ? `/api/subject/${subjectId}` : "/api/subject";

  authFetch(url, {
    method,
    body: JSON.stringify(payload)
  }).then(res => {
    if (res.ok) {
      bootstrap.Modal.getInstance(document.getElementById("subjectModal")).hide();
      loadSubjects();
    } else {
      alert("저장 실패");
    }
  });
});

function showSubclassModal() {
  document.getElementById("subclassForm").reset();
  document.getElementById("subclassId").value = "";
  document.getElementById("subclassModalTitle").innerText = "새 선택과목 추가";
  document.getElementById("subclassSubmitBtn").innerText = "저장";
  document.getElementById("subclassListening").disabled = false;
  const optionalSelect = document.getElementById("subclassOptional");
  if (selectedSubjectOption === 0) {
    optionalSelect.value = "true";
    optionalSelect.disabled = true;
  } else {
    optionalSelect.disabled = false;
  }
  new bootstrap.Modal(document.getElementById("subclassModal")).show();
}

function editSubclass(subclassId) {
  authFetch(`/api/subclass/${subclassId}`)
    .then(res => res.json())
    .then(subclass => {
      document.getElementById("subclassId").value = subclass.subclassId;
      document.getElementById("subclassName").value = subclass.subclassName;
      document.getElementById("count").value = subclass.count;
      document.getElementById("subclassOptional").value = subclass.optional;
      document.getElementById("subclassListening").value = subclass.listening;
      document.getElementById("subclassModalTitle").innerText = "선택과목 수정";
      document.getElementById("subclassSubmitBtn").innerText = "수정";
      document.getElementById("subclassListening").disabled = true;
      const optionalSelect = document.getElementById("subclassOptional");
      if (selectedSubjectOption === 0) {
        optionalSelect.value = "true";
        optionalSelect.disabled = true;
      } else {
        optionalSelect.disabled = false;
      }
      new bootstrap.Modal(document.getElementById("subclassModal")).show();
    });
}

function saveSubclass() {
  const subclassId = document.getElementById("subclassId").value;
  const payload = {
    subclassName: document.getElementById("subclassName").value,
    optional: document.getElementById("subclassOptional").value,
    listening: document.getElementById("subclassListening").value,
    count: document.getElementById("count").value,
    subjectId: selectedSubjectId
  };
  const method = subclassId ? "PUT" : "POST";
  const url = subclassId ? `/api/subclass/${subclassId}` : "/api/subclass";

  authFetch(url, {
    method,
    body: JSON.stringify(payload)
  }).then(res => {
    if (res.ok) {
      bootstrap.Modal.getInstance(document.getElementById("subclassModal")).hide();
      selectSubject(selectedSubjectId, selectedSubjectEl);
    } else {
      alert("저장 실패");
    }
  });
}

function deleteSubclass(subclassId) {
  if (!confirm("선택과목을 삭제하시겠습니까? (문제가 없어야 합니다)")) return;
  authFetch(`/api/subclass/${subclassId}`, { method: "DELETE" })
    .then(res => {
      if (res.ok) selectSubject(selectedSubjectId, selectedSubjectEl);
      else alert("삭제 실패: 문제가 남아있을 수 있습니다");
    });
}

loadSubjects();
window.showSubjectModal = showSubjectModal;
window.editSubject = editSubject;
window.deleteSubject = deleteSubject;
window.showSubclassModal = showSubclassModal;
window.editSubclass = editSubclass;
window.deleteSubclass = deleteSubclass;
window.saveSubclass = saveSubclass;
