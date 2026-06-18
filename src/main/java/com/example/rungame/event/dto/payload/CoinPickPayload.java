package com.example.rungame.event.dto.payload;

import lombok.*;

//coin_pick 이벤트 전송/저장을 위한 Payload DTO
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor @Builder
public class CoinPickPayload {

    //코인 가치
    @Builder.Default
    private int value = 1;

    //코인 획득 시 X 좌표
    private Double x;

    //코인 획득 시 Y 좌표
    private Double y;
}
