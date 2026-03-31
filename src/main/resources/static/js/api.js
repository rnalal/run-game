/*
공통 API 호출 유틸
- 모든 fetch 요청에서 사용
- 401 나오면 /api/auth/refresh 호출 후 한 번만 원요청 재시도
*/
let refreshing = null;  //현재 refresh 요청 Pramise (중복 호출 방지)

export async function apiFetch(url, options = {}) {
    //기본 옵션 + 쿠키 포함 설정
    const opts = {
        ...options,
        credentials: "include",
        headers: {
            ...(options.headers || {})
        }
    };

    const res = await fetch(url, opts);

    //정상 응답이거나 401 이외의 상태면 그대로 반환
    if (res.status !== 401) {
        return res;
    }

    //refresh 요청 자체에서 401 나면 재시도 안하고 그대로 반환
    if (url === "/api/auth/refresh") {
        return res;
    }

    console.warn("AccessToken 만료, refresh 시도");

    try {
        //이미 refresh 중이면 그 Pramise 재사용
        if (!refreshing) {
            refreshing = fetch("/api/auth/refresh", {
                method: "POST",
                credentials: "include"
            }).finally(() => {
                refreshing = null;  //완료되면 항상 초기화
            });
        }

        const refreshRes = await refreshing;

        //refresh 실패 -> 그대로 에러 처리
        if (!refreshRes.ok) {
            const msg = await refreshRes.text();
            throw new Error(msg || "refresh failed");
        }

        console.log("Refresh 성공, 원래 요청 재시도");

        //토큰 재발급 후, 같은 옵션으로 한 번만 재요청
        const retryOpts = {
            ...opts,
            body: opts.body
        };
        return fetch(url, retryOpts);

    } catch (e) {
        //refresh 자체가 실패하면 로그인 페이지로 이동
        alert("로그인이 만료되었습니다. 다시 로그인해주세요.");
        location.href = "/login";
        throw e;
    }
}
