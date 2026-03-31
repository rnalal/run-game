package com.example.rungame.common.util;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Function;

/*
* CSV 내보내기 전용 유틸리티 클래스
*
* - 관리자 기능에서 통계/로그 데이터를 CSV 파일로 변환하는 역할
* - HTTP 응답으로 바로 내려보낼 수 있는 byte[] 생성
* - 제네릭 기반 변환 메서드를 제공해서 다양한 도메인 데이터 지원
*
* - CSV 생성 로직을 컨트롤러/서비스에서 분리
* - RFC4180 규칙에 맞춘 최소한의 escape 처리 제공
* */
public final class CsvExporter {

    //유틸리티 클래스이므로 인스턴스 생성 방지
    private CsvExporter() {}

    /*
    * 이미 문자열로 변환된 CSV row 리스트를 CSV byte 배열로 변환
    *
    * @param header : CSV 헤더
    * @param rows : CSV 한 줄 단위 데이터 목록
    * @return : UTF-8 인코딩된 CSV byte 배열
    * */
    public static byte[] toCsv(String header, List<String> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append(header).append("\n");
        for (String line : rows) sb.append(line).append("\n");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    /*
    * 도메인 객체 리스트를 CSV byte 배열로 변환
    *
    * @param header : CSV 헤더
    * @param data : CSV로 변환할 도메인 데이터 리스트
    * @param toLine : 객체 -> CSV 한 줄 문자열 매핑 함수
    * @return UTF-8 인코딩된 CSV byte 배열
    * */
    public static <T> byte[] toCsv(String header, List<T> data, Function<T, String> toLine) {
        StringBuilder sb = new StringBuilder();
        sb.append(header).append("\n");
        for(T t : data) sb.append(toLine.apply(t)).append("\n");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    /*
    * CSV 필드 값 escape 처리
    * - 쉼표, 따옴표, 개행 문자가 포함된 경우
    *   RFC4180 규칙에 따라 전체를 따옴표로 감쌈
    * - 내부 따옴표는 ""형태로 이스케이프
    *
    * @param s : 원본 문자열
    * @return : CSV 규칙에 맞게 escape 처리된 문자열
    * */
    public static String csvEscape(String s) {
        if (s == null) return "";
        //쉼표, 따옴표, 개행 포함 여부 확인
        boolean needQuote = s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r");
        //따옴표 escape 처리
        String esc = s.replace("\"","\"\"");
        return needQuote ? "\"" + esc + "\"" : esc;
    }
}
