package com.example.rungame.event.service.rules;

import com.example.rungame.common.support.Payloads;
import com.example.rungame.event.domain.EventType;
import com.example.rungame.event.domain.SessionEvent;
import com.example.rungame.event.repository.SessionEventRepository;
import com.example.rungame.event.service.EventRule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

/*
* 코인 획득 이벤트 검증을 담당하는 규칙 클래스
* - 플레이어가 코인을 너무 비정상적인 템포로 먹거나
*   순간이동하듯이 위치가 튀는 상황을 걸러내는 역할
*
* - 코인 획득 간격이 너무 짧지 않은지
* - 코인 획득 위치가 비정상적으로 크지 않은지
* - 직전에 먹은 코인 위치와 비교했을 때, 한 번에 너무 멀리 이동하지 않았는지
* */
@Component
@RequiredArgsConstructor
@Slf4j
public class CoinPickupRule implements EventRule {

    private final SessionEventRepository repo;

    //코인 사이 최소 허용 간격
    @Value("${rungame.coin.min-interval-ms:120}")
    private int MIN_INTERVAL_MS;

    //한 번에 이동했다고 인정할 수 있는 최대 거리
    //직전 코인 위치와 비교해서 이 값보다 크면 순간이동에 가깝다고 보고 로그 남김
    @Value("${rungame.coin.max-delta-x:500}")
    private double MAX_DELTA_X;

    //메모리 컨텍스트에서 직전 coin_pick의 x 좌표를 저장할 때 사용할 수 있는 키
    private static final String CTX_KEY_LAST_COIN_X = "ctx.coin.lastX";

    //이 규칙이 어떤 이벤트 타입에 적용되는지 알려주는 메서드
    @Override
    public boolean supports(EventType type) {
        return type == EventType.coin_pick;
    }

    /*
    * 코인 획득 이벤트 한 건에 대한 검증 로직
    * - 간격 체크
    * - 위치 값 이상 여부 체크
    * - 직전 코인 위치와의 거리 차이 체크
    *
    * @param e : 현재 들어온 코인 이벤트
    * @param lastTms : 배치 처리 중 타입별 마지막 tMs를 돌고 있는 맵
    * */
    @Override
    public void validate(SessionEvent e, Map<EventType, Integer> lastTms){
        final long sid = e.getSessionId();
        final int tMs = e.getTMs();

        /*
        * 1) 코인 간 최소 간격 체크
        * - 우선 배체 컨택스트에 있는 값 사용
        * - 없으면 DB에서 마지막 코인 이벤트를 찾아와 보정
        * */
        Integer lastCoinT = lastTms.get(EventType.coin_pick);
        if (lastCoinT == null) {
            repo.findLastBySessionIdAndType(sid, EventType.coin_pick)
                    .ifPresent(prev -> lastTms.put(EventType.coin_pick, prev.getTMs()));
            lastCoinT = lastTms.get(EventType.coin_pick);
        }

        if (lastCoinT != null) {
            int diff = tMs - lastCoinT;
            if (diff < MIN_INTERVAL_MS) {
                log.debug("[drop:coin] cooldown sid={} lastT={} currT={} diff={}ms", sid, lastCoinT, tMs, diff);
                return;
            }
        }

        /*
        * 2) 위치값 자체가 이상한지 체크
        * - payload에서 x를 읽어오고 너무 큰 절대값은 바로 의심
        * */
        double x = Payloads.getDouble(e.getPayload(), "x", 0.0);

        //아주 말도 안되는 큰 값 필터링
        if (Math.abs(x) > 1000000) {
            log.debug("[drop:coin] suspicious x sid={} x={}", sid, x);
            return;
        }

        /*
        * 3) 직전 코인 위치와 비교해서 순간이동 수준인지 체크
        * - repo를 이용해 같은 세션의 마지막 coin_pick 조회
        * */
        repo.findLastBySessionIdAndType(e.getSessionId(), EventType.coin_pick)
                .ifPresent(prev -> {
                    double px = Payloads.getDouble(prev.getPayload(), "x", x);
                    double delta = Math.abs(x - px);

                    if(delta > MAX_DELTA_X){
                        log.debug("[drop:coin] position jump sid={} prevX={} currX={} delta={}", sid, px, x, delta);
                    }
                });
    }
}
