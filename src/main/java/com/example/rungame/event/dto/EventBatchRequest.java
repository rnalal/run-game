package com.example.rungame.event.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/*
* 세션 이벤트를 한 번에 여러 개 전송하기 위한 배치 요청 DTO
* - 클라이언트가 일정 주기마다 모아둔 이벤트들을 묶어서 보내는 용도
* - 네트워크 트래픽/요청 수를 줄이기 위해 단건이 아닌 배치 전송 구조
* */
@Getter
@Setter
public class EventBatchRequest {

    /*
    * 전송된 이벤트들의 리스트
    * - 각 요소는 개별 이벤트를 표현하는 EventDTO
    * - null 또는 빈 리스트에 대한 처리/검증은 컨트롤러/서비스 단에서 수행
    * */
    private List<EventDTO> events;
}
