package com.example.rungame.common.util;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Function;

//내보내기 전용 유틸리티 클래스
public final class CsvExporter {

    //유틸리티 클래스이므로 인스턴스 생성 방지
    private CsvExporter() {}

    //이미 문자열로 변환된 CSV row 리스트를 CSV byte 배열로 변환
    public static byte[] toCsv(String header, List<String> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append(header).append("\n");
        for (String line : rows) sb.append(line).append("\n");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    //도메인 객체 리스트를 CSV byte 배열로 변환
    public static <T> byte[] toCsv(String header, List<T> data, Function<T, String> toLine) {
        StringBuilder sb = new StringBuilder();
        sb.append(header).append("\n");
        for(T t : data) sb.append(toLine.apply(t)).append("\n");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    //CSV 필드 값 escape 처리
    public static String csvEscape(String s) {
        if (s == null) return "";
        //쉼표, 따옴표, 개행 포함 여부 확인
        boolean needQuote = s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r");
        //따옴표 escape 처리
        String esc = s.replace("\"","\"\"");
        return needQuote ? "\"" + esc + "\"" : esc;
    }
}
