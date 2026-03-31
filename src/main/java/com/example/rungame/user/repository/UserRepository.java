package com.example.rungame.user.repository;

import com.example.rungame.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

/*
* User 엔티티 저장소
* - 회원가입,로그인,내 정보 조회,통계에 필요한 유저 조회 기능을 모아둔 레포지토리
* - Spring Data JPA의 메서드 이름 규칙 + @Query를 같이 사용해서 자주 쓰던 패턴 쿼리를
*   명시적으로 분리함
* */
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    //회원가입 중복 체크용
    boolean existsByEmail(String email);
    boolean existsByNickname(String nickname);

    //로그인에서 사용(이메일로 유저 찾기)
    Optional<User> findByEmail(String email);

    //내 정보 조회, 토큰 기반 인증시
    //토큰이 들어 있는 userId + tokenVersion 이 실제 DB 값과 일치하는지 확인
    Optional<User> findByIdAndTokenVersion(Long id, Integer tokenVersion);

    //운영 통계용: 특정 시점 이후에 가입한 유저 수
    long countByCreatedAtAfter(LocalDateTime after);

    //닉네임만 필요한 화면에서 전체 User를 다 가져오지 않고
    //닉네임만 가볍게 조회하기 위한 쿼리
    @Query("SELECT u.nickname FROM User u WHERE u.id = :id")
    Optional<String> findNicknameById(@Param("id") Long id);
}
