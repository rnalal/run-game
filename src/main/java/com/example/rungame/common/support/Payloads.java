package com.example.rungame.common.support;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Set;
import java.util.regex.Pattern;

/*
* 런게임 이벤트 payload 공통 지원 클래스
*
* - 게임 세션에서 발생하는 각종 이벤트의 payload 구조를 정의
* - 이벤트 타입별 payload를 statice inner class 형태로 관리
* - jakarta validation 어노테이션을 통해 1차 데이터 검증 수행
* - DB 저장 전 또는 통계 집계 시 필요한 값 파싱 유틸 제공
* - JSON 파싱 라이브러리 없이도 최소한의 값 추출 가능하도록 지원
* */
public final class Payloads {

    //유틸성 클래스이므로 인스턴스 생성 방지
    private Payloads() {}

    public interface EventPayload {}

    //powerup_pick 이벤트 payload
    public static final class PowerupPickPayload implements EventPayload {

        public static final Set<String> ALLOWED = Set.of("MAGNET", "SHIELD", "SCORE_X2", "SPEED_BOOST");

        @NotBlank
        public String name;

        //null 허용 -> null인 경우 default 값 적용
        @Min(1000) @Max(20000)
        public Integer durationMs;

        public Double x;
        public String source;

        public boolean isNameAllowed() {
            return ALLOWED.contains(name);
        }

        public int effectiveDurationMs() {
            return durationMs != null ? durationMs : 5000;
        }
    }

    /*
    * chkeckpoint 이벤트 payload
    * - 플레이 중 특정 체크포인트 도달 시 사용
    * - 진행도 추적 및 통계 집계 용도
    * */
    public static final class CheckpointPayload implements EventPayload {

        //체크포인트 순번(1 이상 필수)
        @NotNull
        @Min(1)
        public Integer index;

        //체크 포인트 도달 시점의 거리 값
        public Integer distance;
    }

    /*
    *  JSON 문자열에서 int 값 추출
    *
    * @param json : 원본 JSON 문자열
    * @param key : 추출할 필드명
    * @param def : 값이 없거나 파싱 실패 시 기본값
    * */
    public static int getInt(String json, String key, int def) {
        if (json == null) return def;
        try {
            var m = Pattern.compile("\"" + key + "\"\\s*:\\s*(-?\\d+)").matcher(json);
            return m.find() ? Integer.parseInt(m.group(1)) : def;
        } catch (Exception ignore) { return def; }
    }

    //JSON 문자열에서 double 값 추출(기본값 반환)
    public static double getDouble(String json, String key, double def) {
        if (json == null) return def;
        try {
            var m = Pattern.compile("\"" + key + "\"\\s*:\\s*(-?[0-9]+\\.?[0-9]*)").matcher(json);
            return m.find() ? Double.parseDouble(m.group(1)) : def;
        } catch (Exception ignore) { return def; }
    }

    //JSON 문자열에서 double 값 추출(없으면 null반환)
    public static Double getDoubleOrNull(String json, String key){
        if (json == null) return null;
        try {
            var m = java.util.regex.Pattern
                    .compile("\"" + key + "\"\\s*:\\s*(-?[0-9]+\\.?[0-9]*)")
                    .matcher(json);
            return m.find() ? Double.valueOf(m.group(1)) : null;
        } catch (Exception ignore) {
            return null;
        }
    }

    //JSON 문자열에서 문자열 값 추출
    public static String getString(String json, String key, String def) {
        if (json == null) return def;
        try {
            var m = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]*)\"").matcher(json);
            return m.find() ? m.group(1) : def;
        } catch (Exception ignore) { return def; }
    }
}
