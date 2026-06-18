package com.example.rungame.leaderboard.service;

import com.example.rungame.leaderboard.dto.LeaderboardEntryDTO;
import com.example.rungame.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RedisLeaderboardService {

    private final StringRedisTemplate redisTemplate;
    private final UserRepository userRepository;

    private static final String PREFIX = "leaderboard:";
    private static final Duration DAILY_TTL = Duration.ofDays(14);
    private static final Duration WEEKLY_TTL = Duration.ofDays(90);
    private static final Duration MONTHLY_TTL = Duration.ofDays(400);

    //게임 세션 종료 시 리더보드 갱신
    public void updateLeaderboards(Long userId, int score, LocalDateTime endedAt){
        ZoneId kst = ZoneId.of("Asia/Seoul");
        LocalDate day = endedAt == null
                ? LocalDate.now(kst)
                : endedAt.atZone(kst).toLocalDate();

        String member = String.valueOf(userId);

        String allKey = keyAll();
        String dailyKey = keyDaily(day);
        String weeklyKey = keyWeekly(day);
        String monthlyKey = keyMonthly(day);

        updateIfHigher(allKey, member, score);
        updateIfHigher(dailyKey, member, score);
        updateIfHigher(weeklyKey, member, score);
        updateIfHigher(monthlyKey, member, score);

        applyPeriodLeaderboardTtl(dailyKey, weeklyKey, monthlyKey);
    }

    //현재 저장된 점수보다 높은 경우에만 Redis Sorted Set 점수 갱신
    private void updateIfHigher(String key, String member, int score) {
        Double current = redisTemplate.opsForZSet().score(key, member);

        if(current == null || score > current) {
            redisTemplate.opsForZSet().add(key, member, score);
        }
    }

    //기간별 리더보드에 TTL 적용
    private void applyPeriodLeaderboardTtl(String dailyKey, String weeklyKey, String monthlyKey){
        redisTemplate.expire(dailyKey, DAILY_TTL);
        redisTemplate.expire(weeklyKey, WEEKLY_TTL);
        redisTemplate.expire(monthlyKey, MONTHLY_TTL);
    }

    //Redis Sorted Set 기반 리더보드 조회
    public Page<LeaderboardEntryDTO> getLeaderboard(String range, int page, int size){
        String key = resolveKey(range);

        long start = (long) page * size;
        long end = start + size - 1;

        //Redis ZSet에서 현재 페이지 범위 조회
        Set<ZSetOperations.TypedTuple<String>> rows =
                redisTemplate.opsForZSet().reverseRangeWithScores(key, start, end);

        Long total = redisTemplate.opsForZSet().zCard(key);
        if (total == null) total = 0L;

        List<LeaderboardEntryDTO> content = new ArrayList<>();

        if (rows == null || rows.isEmpty()) {
            return new PageImpl<>(
                    content,
                    PageRequest.of(page, size),
                    total
            );
        }

        //Redis Sorted Set에서 가져온 member 값을 userId 목록으로 변환
        List<Long> userIds = rows.stream()
                .map(row -> Long.valueOf(row.getValue()))
                .toList();

        //사용자 정보를 한 번에 조회
        Map<Long, String> nicknameMap = userRepository.findAllById(userIds)
                .stream()
                .collect(Collectors.toMap(
                        user -> user.getId(),
                        user -> user.getNickname()
                ));

        int index = 0;

        //Redis에서 가져온 랭킹 순서를 유지하면서 DTO 생성
        for (ZSetOperations.TypedTuple<String> row : rows) {
            Long userId = Long.valueOf(row.getValue());
            int score = row.getScore() == null ? 0 : row.getScore().intValue();

            String nickname = nicknameMap.getOrDefault(userId, "Unknown");

            long rank = start + index + 1;

            content.add(new LeaderboardEntryDTO(
                    userId,
                    nickname,
                    score,
                    0,
                    0,
                    rank
            ));
            index++;
        }
        return new PageImpl<>(
                content,
                PageRequest.of(page, size),
                total
        );
    }

    //특정 사용자의 현재 순위 조회
    public Long getUserRank(Long userId, String range){
        String key = resolveKey(range);

        Long zeroBasedRank = redisTemplate.opsForZSet()
                .reverseRank(key, String.valueOf(userId));

        if (zeroBasedRank == null) return null;

        return zeroBasedRank + 1;
    }

    //요청된 범위에 맞는 Redis 리더보드 키 생성
    private String resolveKey(String range){
        ZoneId kst = ZoneId.of("Asia/Seoul");
        LocalDate today = LocalDate.now(kst);

        return switch(range) {
            case "daily" -> keyDaily(today);
            case "weekly" -> keyWeekly(today);
            case "monthly" -> keyMonthly(today);
            default -> keyAll();
        };
    }

    //전체 리더보드 Key
    private String keyAll() {
        return PREFIX + "all:score";
    }

    //일간 리더보드 Key 생성
    private String keyDaily(LocalDate day){
        return PREFIX + "daily:score:" + day;
    }

    //주간 리더보드 Key 생성
    private String keyWeekly(LocalDate day) {
        WeekFields wf = WeekFields.ISO;
        int year = day.get(wf.weekBasedYear());
        int week = day.get(wf.weekOfWeekBasedYear());
        return PREFIX + "weekly:score:" + year + "-W" + week;
    }

    //월단 리더보드 Key 생성
    private String keyMonthly(LocalDate day) {
        return PREFIX + "monthly:score:" + day.getYear() + "-" + day.getMonthValue();
    }
}
