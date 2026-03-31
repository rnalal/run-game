package com.example.rungame.analytics.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/*
 * A/B 테스트 관리 서비스
 *
 * - 사용자별 테스트 그룹(A/B) 할당
 * - 그룹별 사용자 수 통계 제공
 *
 * 메모리 기반의 간단한 구현
 * 기능 실험 및 구조 설계 목적에 초점을 둠
 */
@Service
public class ABTestService {

    /*
     * 사용자별 A/B 테스트 그룹 저장소
     *
     * key   : userId (String)
     * value : 그룹 ("A" 또는 "B")
     *
     * 서버 재시작 시 초기화되는 구조
     */
    private final Map<String, String> userGroups = new HashMap<>();

    /*
     * 사용자 A/B 테스트 그룹 배정
     *
     * - userId 기준 단순 분기 로직
     * - 짝수 → A, 홀수 → B
     *
     * @param userId 사용자 ID
     * @return 할당된 테스트 그룹
     */
    public String assignUser(Long userId) {
        String group = (userId % 2 == 0) ? "A" : "B";
        userGroups.put(String.valueOf(userId), group);
        return group;
    }

    /*
     * A/B 테스트 그룹별 사용자 수 통계
     *
     * @return
     * - A_count : A 그룹 사용자 수
     * - B_count : B 그룹 사용자 수
     */
    public Map<String, Object> getGroupStats() {
        return Map.of("A_count", userGroups.values().stream().filter("A"::equals).count(),
                      "B_count", userGroups.values().stream().filter("B"::equals).count());
    }
}
