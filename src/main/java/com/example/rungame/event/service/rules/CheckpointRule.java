package com.example.rungame.event.service.rules;

import com.example.rungame.common.support.JsonHelpers;
import com.example.rungame.event.domain.EventType;
import com.example.rungame.event.domain.SessionEvent;
import com.example.rungame.event.repository.SessionEventRepository;
import com.example.rungame.event.service.EventRule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

//checkpoint 이벤트 검증을 담당하는 규칙 클래스
@Slf4j
@Component
@RequiredArgsConstructor
public class CheckpointRule implements EventRule {

    private final SessionEventRepository repo;

    //튜닝 파라미터(기본값)
    private static final int MIN_GAP_MS = 300;
    //이전 index에서 한 번에 점프할 수 있는 최대 범위
    private static final int MAX_INDEX_JUMP = 5;

    //이 규칙이 어떤 이벤트 타입을 대상으로 하는지 표시
    @Override
    public boolean supports(EventType type){
        return type == EventType.checkpoint;
    }

    //checkpoint 이벤트 한 건에 대해 유효성 검증 수행
    @Override
    public void validate(SessionEvent ev, Map<EventType, Integer> lastTms) {
        final long sid = ev.getSessionId();
        final int tMs = ev.getTMs();
        final String payload = ev.getPayload();

        //payload 파싱 & 기본 검증
        Integer index = JsonHelpers.getInt(payload, "index", -1);
        if (index == null || index < 1){
            log.debug("[drop:checkpoint] invalid index sid={} payload={}", sid, payload);
            return;
        }
        Integer distance = JsonHelpers.getInt(payload, "distance", -1);
        if(distance != null && distance < 0){
            log.debug("[drop:checkpoint] negative distance sid={} distance={}", sid, distance);
            return;
        }

        //같은 세션의 마지막 checkpoint와 비교
        Optional<SessionEvent> last = repo.findLastBySessionIdAndType(sid, EventType.checkpoint);
        if (last.isPresent()) {
            int lastIndex = JsonHelpers.getInt(last.get().getPayload(), "index", -1);
            int lastT = last.get().getTMs();
            Integer lastDist = JsonHelpers.getInt(last.get().getPayload(), "distance", -1);

            //인덱스는 반드시 앞으로만 진행
            if(lastIndex >= 1 && index <= lastIndex){
                log.debug("[drop:checkpoint] index not increasing sid={} last={} curr={}", sid, lastIndex, index);
                return;
            }

            //인덱스가 한 번에 너무 많이 뛰는 경우 방지
            if(lastIndex >= 1 && index - lastIndex > MAX_INDEX_JUMP){
                log.debug("[drop:checkpoint] index jump too large sid={} last={} curr={}", sid, lastIndex, index);
                return;
            }

            //체크포인트 사이 시간 간격 검증
            int diff = tMs - lastT;
            if(diff < MIN_GAP_MS){
                log.debug("[drop:checkpoint] gap too short sid={} diff={}ms", sid, diff);
                return;
            }

            //distance 값이 있다면, 이전 값과의 관계 확인
            if(distance != null && distance >= 0 && lastDist != null && lastDist >= 0){
                log.debug("[drop:checkpoint] distance decreased sid={} last={} curr={}", sid, lastDist, distance);
                return;
            }
        } else {
            //첫 체크포인트인데 index가 1이 아니라면 비정상으로 판단
            if(index != 1) {
                log.debug("[drop:checkpoint] first index not 1 sid={} index={}", sid, index);
                return;
            }
        }

        //같은 배치 안에서의 밀도 체크
        //직전에 처리한 checkpoint와의 간격이 100ms 미만이면 비정상 패턴으로 간주
        Integer lastTypeTms = lastTms.get(EventType.checkpoint);
        if (lastTypeTms != null && (tMs - lastTypeTms) < 100) {
            log.debug("[drop:checkpoint] too dense in batch sid={} diff={}ms", sid, tMs - lastTypeTms);
            return;
        }
    }
}
