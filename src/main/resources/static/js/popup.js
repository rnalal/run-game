//popup.js
//팝업 공지 로딩,표시
async function loadPopupNotice() {
  try {
    //오늘 하루 보지 않기 체크(같은 날짜면 바로 종료)
    if (localStorage.getItem("popupHideToday") === new Date().toDateString()) {
      return;
    }

    //서버에서 팝업 공지 리스트 조회
    const res = await fetch("/public/notices/popup");
    if (!res.ok) return;

    const popups = await res.json();
    if (!popups || popups.length === 0) return;

    let index = 0;  //현재 보고 있는 팝업 인덱스

    //전체 팝업 배경 + 팝업 박스 생성
    const backdrop = document.createElement("div");
    backdrop.className = "popup-backdrop";

    backdrop.innerHTML = `
      <div class="popup-window">

        <button class="slide-btn prev-btn">◀</button>

        <div class="popup-slide-content">
          <div class="popup-title"></div>
          <div class="popup-content"></div>
        </div>

        <button class="slide-btn next-btn">▶</button>

        <button class="popup-close-btn">닫기</button>

        <label>
          <input type="checkbox" id="popupHideTodayChk"> 오늘 하루 보지 않기
        </label>
      </div>
    `;

    document.body.appendChild(backdrop);
    backdrop.style.display = "flex";

    //실제 내용이 들어갈 영역
    const titleBox = backdrop.querySelector(".popup-title");
    const contentBox = backdrop.querySelector(".popup-content");

    //현재 index에 해당하는 팝업 렌더링
    function render() {
      const n = popups[index];
      titleBox.innerHTML = n.title;
      contentBox.innerHTML = n.content;
    }

    render();

    //이전,다음 버튼으로 팝업 슬라이드
    backdrop.querySelector(".prev-btn").onclick = () => {
      index = (index - 1 + popups.length) % popups.length;
      render();
    };

    backdrop.querySelector(".next-btn").onclick = () => {
      index = (index + 1) % popups.length;
      render();
    };

    //닫기 버튼 + 오늘 하루 보지 않기 처리
    backdrop.querySelector(".popup-close-btn").onclick = () => {
      if (document.getElementById("popupHideTodayChk").checked) {
        localStorage.setItem("popupHideToday", new Date().toDateString());
      }
      backdrop.remove();
    };

  } catch (e) {
    console.error("팝업 로딩 실패:", e);
  }
}

//페이지 로딩 완료 시 한 번만 실행
document.addEventListener("DOMContentLoaded", loadPopupNotice);

