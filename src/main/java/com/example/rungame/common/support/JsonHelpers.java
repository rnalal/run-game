package com.example.rungame.common.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/*
* JSON 안전 접 근 유틸리티
*
* - 이벤트 payload, 옵션 설정 등 비정형 JSON 문자열에서
*   특정 필드를 안전하게 추출하기 위한 헬퍼 클래스
* - JSON 파싱 실패 / 필드 누락 시 예외를 던지지 않고
*   기본값을 반환해서 서비스 안정성 유지
* */
public final class JsonHelpers {

    //공통 ObjectMapper
    private static final ObjectMapper M = new ObjectMapper();

    //유틸 클래스이므로 인스턴스 생성 방지
    private JsonHelpers() {}

    //int 필드 안전 추출
    public static int getInt(String json, String field, int def) {
        try {
            JsonNode n = M.readTree(json == null ? "{}" : json);
            return n.has(field) ? n.get(field).asInt(def) : def;
        } catch (Exception e) {
            return def;
        }
    }

    //double 필드 안전 추출
    public static Double getDouble(String json, String field, Double def) {
        try {
            JsonNode n = M.readTree(json == null ? "{}" : json);
            return n.has(field) ? n.get(field).asDouble() : def;
        } catch (Exception e) {
            return def;
        }
    }

    //string 필드 안전 추출
    public static String getString(String json, String field, String def) {
        try {
            JsonNode n = M.readTree(json == null ? "{}" : json);
            return n.has(field) ? n.get(field).asText(def) : def;
        } catch (Exception e) {
            return def;
        }
    }

    //boolean 필드 안전 추출
    public static boolean getBool(String json, String field, boolean def) {
        try {
            JsonNode n = M.readTree(json == null ? "{}" : json);
            return n.has(field) ? n.get(field).asBoolean(def) : def;
        } catch (Exception e) {
            return def;
        }
    }
}
