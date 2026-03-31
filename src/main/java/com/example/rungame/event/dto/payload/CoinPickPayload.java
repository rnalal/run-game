package com.example.rungame.event.dto.payload;

import lombok.*;

/*
* coin_pick 이벤트 전송/저장을 위한 Payload DTO
*
* - 플레이어가 코인을 획득할 때 서버로 전달되는 정보 구조
* - 이벤트 수집 컨트롤러 <-> 내부 도메인/엔티티 사이에서 사용하는 전용 DTO
*
* - 코인 가치를 명시적으로 전달
* - 코인을 먹은 위치(x,y)를 함께 전달해서 비정상 좌표 감지 및 분석에 활용
* */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor @Builder
public class CoinPickPayload {

    /*
    * 코인 가치
    * - 기본값 : 1코인
    * - 빌더 사용 시 별도 세팅이 없으면 1로 초기화
    * */
    @Builder.Default
    private int value = 1;

    /*
    * 코인 획득 시 X 좌표
    * - 비정상 위치 탐지용
    * - null 허용
    * */
    private Double x;

    /*
    * 코인 획득 시 Y 좌표
    * - 점프 상태/지면 상태 등과 연계해 분석할 수 있는 보조 정보
    * - 클라이언트에서 전달되는 경우에만 저장
    * */
    private Double y;
}
