package com.example.rungame.admin.dto;

import java.util.List;

public record AdminDashboardSessionChartResponse(

    List<String> labels,
    List<Long> data
) { }
