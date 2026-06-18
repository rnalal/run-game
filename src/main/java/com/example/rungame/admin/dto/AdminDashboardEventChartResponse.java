package com.example.rungame.admin.dto;

import java.util.List;

public record AdminDashboardEventChartResponse(

    //차트 X축 라벨
    List<String> labels,

    //차트 데이터 값
    List<Long> data
) { }
