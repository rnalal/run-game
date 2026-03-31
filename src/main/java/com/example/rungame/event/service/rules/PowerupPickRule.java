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
import java.util.Objects;
import java.util.Optional;

/*
* 파워업 획득 이벤트를 검증하는 규칙 클래스
* - 같은 파워업을 말도 안 되게 오래 유지하거나
*   아직 끝나지도 않았는데 또 줍은 상황을 막는 역할
*
* - payload 안에 name/durationMs 값이 정상인지
* - game_over 이후에는 더 이상 powerup_pick이 들어오지 않는지
* - 같은 이름의 파워업이 아직 지속 중인데 또 먹은 건 아닌지
* - 최근에 먹었던 같은 파워업을 너무 빨리 다시 줍는 건 아닌지
* - 배치 단위로 powerup_pick이 너무 몰려서 들어오는 건 아닌지
* */
@Slf4j
@Component
@RequiredArgsConstructor
public class PowerupPickRule implements EventRule {

    private final SessionEventRepository repo;

    //튜닝용 기본값
    /*
    * 동일 파워업 최소 재지급 간격
    * - 파워업이 끝나자마자 바로 또 줍는 상황을 조금 막기 위한 값
    * */
    private static final int MIN_COOLDOWN_MS = 400;

    /*
    * 한 파워업이 가질 수 있는 최대 지속시간 상한
    * - durationMs가 너무 비정상적으로 큰 값으로 들어오는 걸 방어
    * */
    private static final int MAX_STACK_WINDOWN_MS = 20_000;

    //어떤 타입의 이벤트에 이 룰을 적용할 지 표시
    //powerup_pick 이벤트에만 적용
    @Override
    public boolean supports(EventType type){
        return type == EventType.powerup_pick;
    }

    /*
    * powerup_pick 이벤트 한 건에 대한 검증 로직
    * - payload 기본값 체크
    * - game_over 이후인지 확인
    * - 같은 파워업이 아직 끝나지 않았는지 확인
    * - 최소 쿨다운/배치 단위 스팸 여부 확인
    *
    * @param ev : 현재 들어온 powerup_pick 이벤트
    * @param lastTms : 배치 내에서 타입별 마지막 tMs를 관리하는 맵
    * */
    @Override
    public void validate(SessionEvent ev, Map<EventType, Integer> lastTms){

        final long sid = ev.getSessionId();
        final int tMs = ev.getTMs();
        final String payload = ev.getPayload();

        //1) payload 파싱 & 기본 검증
        final String name = JsonHelpers.getString(payload, "name", null);
        if(name == null || name.isBlank()) {
            log.debug("[drop:powerup] invalid name sid={} payload={}", sid, payload);
            return;
        }

        final int durationMsRaw = JsonHelpers.getInt(payload, "durationMs", -1);
        //durationMs가 안 들어왔으면 기본 5000ms로 간주
        final int durationMs = (durationMsRaw == -1) ? 5000 : durationMsRaw;

        //너무 짧거나 너무 긴 지속 시간은 비정상으로 처리
        if(durationMs < 1000 || durationMs > 20_000) {
            log.debug("[drop:powerup] duration out of range sid={} duration={}", sid, durationMs);
            return;
        }

        //2) game_over 이후 이벤트 금지
        Optional<SessionEvent> lastGameOver = repo.findLastBySessionIdAndType(sid, EventType.game_over);

        //마지막 game_over 시점이 현재 powerup_pick보다 과거라면
        //즉 게임이 이미 끝난 뒤라면 drop
        if (lastGameOver.isPresent() && lastGameOver.get().getTMs() <= tMs){
            log.debug("[drop:powerup] after game_over sid={} tMs={}", sid, tMs);
            return;
        }

        //3) 동일 파워업 중복 활성 방지
        //- 최근 powerup_pick들 중에서 같은 name을 가진 마지막 이벤트를 확인
        Optional<SessionEvent> lastPowerupAny = repo.findLastBySessionIdAndType(sid, EventType.powerup_pick);
        if (lastPowerupAny.isPresent()) {
            //세션 내 powerup_pick들을 최근 순으로 조회해서 같은 name을 찾음
            var candidates = repo.findAllBySessionIdAndTypeOrderByTmsDescIdDesc(sid, EventType.powerup_pick);
            SessionEvent lastSameName = null;

            for (SessionEvent e : candidates){

                String n = JsonHelpers.getString(e.getPayload(), "name", null);

                if(Objects.equals(n, name)){
                    lastSameName = e;
                    break;
                }
            }
            if(lastSameName != null){

                int lastStart = lastSameName.getTMs();
                int lastDuration = JsonHelpers.getInt(lastSameName.getPayload(), "durationMs", 5000);

                //비정상적으로 저장된 duration 방어
                if(lastDuration < 1000 || lastDuration > MAX_STACK_WINDOWN_MS){
                    lastDuration = Math.max(1000, Math.min(lastDuration, MAX_STACK_WINDOWN_MS));
                }

                boolean stillActive = tMs < (lastStart + lastDuration);
                //아직 이전 파워업이 끝나지도 않았는데 같은 이름을 또 줍는 경우
                if(stillActive){
                    log.debug("[drop:powerup] same powerup active sid={} name={}", sid, name);
                    return;
                }
                //최소 쿨다운 : 끝난 직후 너무 빨리 다시 줍는 것도 제한
                int sinceLast = tMs - lastStart;
                if (sinceLast < MIN_COOLDOWN_MS) {
                    log.debug("[drop:powerup] cooldown violated sid={} name={} diff={}", sid, name, sinceLast);
                    return;
                }
            }
        }

        //4) 배치 컨텍스트(LastTms)를 활용한 느슨한 쿨다운
        //- 같은 배치 안에서 powerup_pick이 너무 촘촘하게 몰려오면 drop
        Integer lastTypeTms = lastTms.get(EventType.powerup_pick);
        if (lastTypeTms != null && (tMs - lastTypeTms) < 150) {
            log.debug("[drop:powerup] batch spam sid={} diff={}", sid, tMs - lastTypeTms);
            return;
        }
    }
}
