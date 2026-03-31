package com.example.rungame.user.controller;

import com.example.rungame.common.dto.ApiResponse;
import com.example.rungame.common.jwt.JwtProvider;
import com.example.rungame.user.dto.UserDTO;
import com.example.rungame.user.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/*
* 사용자 계정,프로필 API 컨트롤러
* - 회원가입,내 정보 조회, 비밀번호/닉네임 변경, 회원탈퇴까지
*   계정 관리 기능을 담당하는 REST API
*
* - 회원가입
*   : 유효성 검증(@Valid) 후 UserService에 위임해 계정을 생성
* - 내 정보 조회
*   : HttpOnly 쿠키에서 JWT를 꺼낸 뒤 토큰 기준으로 현재 로그인한 사용자 정보를 조회
* - 비밀번호 변경
*   : accessToken 쿠키에서 사용자 식별 -> 현재 비밀번호,새 비밀번호 검증 및 변경 위임
* - 닉네임 변경
*   : 로그인 사용자 기준으로 닉네임을 변경
* - 회원 탈퇴
*   : 토큰 기반으로 본인 계정을 삭제한 뒤 accessToken 쿠키도 함께 만료시키는 흐름
* - 인증 방식
*   - HttpOnly쿠키(accessToken)에 담긴 JWT를 기준으로 인증함
*   - 토큰이 없거나 검증에 실패하면 401 상태코드 + ApiResponse.error(..)를 반환
* */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    //사용자 가입,로그인,프로필 수정 등 계정 관련 비즈니스 로직을 담당하는 서비스
    private final UserService userService;

    /*
    * 회원가입
    *
    * -@Valid로 기본 필드 검증을 먼저 통과한 뒤
    * -userService.signUp(...)에 회원가입 처리를 위임함
    * -생성 성공 시 201 CREATED + ApiResponse.ok를 반환
    * */
    @PostMapping("/sign-up")
    public ResponseEntity<?> signUp(@RequestBody @Valid UserDTO.SignUpReq req) {
        var res = userService.signUp(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(res));
    }

/*

    - 실제 인증 흐름은 JwtProvider + HttpOnly 쿠키(accessToken)을 기반으로 설계됨
    //로그인
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid UserDTO.LoginReq req){

        try {
            var res = userService.login(req);
            String token = res.getAccessToken();

            ResponseCookie cookie = ResponseCookie.from("accessToken", token)
                    .httpOnly(true)
                    .secure(false)
                    .sameSite("Strict")
                    .path("/")
                    .maxAge(60 * 60) //1시간
                    .build();

            return ResponseEntity.ok()
                    .header("Set-Cookie", cookie.toString())
                    .body(ApiResponse.ok("login_success"));
        } catch (Exception e) {
            return ResponseEntity
                    .status(400)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    //로그인*/

/*    //로그아웃
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {

        // 1) 쿠키에서 토큰 추출
        String token = null;
        if (request.getCookies() != null) {
            for (Cookie c : request.getCookies()) {
                if ("accessToken".equals(c.getName())) {
                    token = c.getValue();
                    break;
                }
            }
        }

        if (token == null) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("로그인 정보가 없습니다."));
        }

        // 2) 로그아웃 처리
        var res = userService.logoutByToken(token);

        // 3) accessToken 쿠키 삭제
        ResponseCookie expired = ResponseCookie.from("accessToken", "")
                .path("/")
                .maxAge(0)
                .httpOnly(true)
                .build();

        return ResponseEntity.ok()
                .header("Set-Cookie", expired.toString())
                .body(ApiResponse.ok(res));
    }
    //로그아웃
*/

    /*
    * 내 정보 조회
    * - 요청 쿠키에서 accessToken을 찾아 JWT를 꺼낸 뒤
    *   userService.getMyInfoByToken(token)으로 사용자 정보를 조회함
    *
    * 성공 -> 200 OK + ApiResponse.ok
    * 실패 -> 토큰 없음,만료,검증 실패 -> 401 + ApiResponse.error(message)
    * */
    @GetMapping("/me")
    public ResponseEntity<?> getMyInfo(HttpServletRequest request) {

        //쿠키에서 accessToken 추출
        String token = null;
        if (request.getCookies() != null) {
            for (Cookie c : request.getCookies()) {
                if ("accessToken".equals(c.getName())) {
                    token = c.getValue();
                    break;
                }
            }
        }

        if (token == null) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("로그인이 필요합니다."));
        }

        try {
            var res = userService.getMyInfoByToken(token);
            return ResponseEntity.ok(ApiResponse.ok(res));
        } catch (Exception e) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /*
    * 공통 유틸: HttpServletRequest에서 accessToken 쿠키 추출
    * - 비밀번호 변경, 닉네임 변경, 회원 탈퇴처럼 로그인된 사용자 기준으로 동작하는 API에서 재사용함
    *
    * - 쿠키 배열이 비어있으면 null 반환
    * - accessToken 이름의 쿠키가 있으면 해당 값을 반환
    * - 없으면 null
    * */
    private String extractTokenFromCookie(HttpServletRequest request) {
        if(request.getCookies() == null) return null;
        for(Cookie c : request.getCookies()) {
            if ("accessToken".equals(c.getName())) {
                return c.getValue();
            }
        }
        return null;
    }

    /*
    * 비밀번호 변경
    *
    * - 쿠키에서 accessToken을 꺼내 현재 유저를 식별함
    * - UserDTO.ChangePasswordReq에 담긴 현재 비밀번호, 새 비밀번호를 전달해 실제 변경은 userService에 위임
    *
    * -성공 -> 200 OK + password_changed
    * -로그인 안 된 상태 -> 401 + 로그인이 필요합니다.
    * -비밀번호 검증 실패 등 -> 400 BAD REQUEST + ApiResponse.error(message)
    * */
    @PostMapping("/password")
    public ResponseEntity<?> changePassword(@RequestBody @Valid UserDTO.ChangePasswordReq req,
                                            HttpServletRequest request) {
        String token = extractTokenFromCookie(request);
        if (token == null) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("로그인이 필요합니다."));
        }

        try {
            userService.changePasswordByToken(token, req);
            return ResponseEntity.ok(ApiResponse.ok("password_changed"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /*
    * 닉네임 변경
    *
    * - accessToken 쿠키로 현재 로그인한 유저를 찾고
    * - UserDTO.ChangeNicknameReq의 새 닉네임으로 업데이트를 요청함
    *
    * -성공 -> 200 OK + nickname_changed
    * -미인증 -> 401 + 로그인이 필요합니다.
    * -닉네임 규칙 위반/중복 등 -> 400 BAD REQUEST + ApiResponse.error(message)
    * */
    @PutMapping("/nickname")
    public ResponseEntity<?> changeNickname(@RequestBody @Valid UserDTO.ChangeNicknameReq req,
                                            HttpServletRequest request) {
        String token = extractTokenFromCookie(request);
        if (token == null) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("로그인이 필요합니다."));
        }

        try {
            userService.changeNicknameByToken(token, req);
            return ResponseEntity.ok(ApiResponse.ok("nickname_changed"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /*
    * 회원 탈퇴
    *
    * - accessToken 쿠키 기준으로 본인 계정을 삭제하도록 userService에 위임
    * - 성공 시 서버에서 accessToken 쿠키를 만료시켜 클라이언트 측에서도 로그인 상태가 해제되도록 처리함
    *
    * -성공 -> 200 OK + account_deleted
    * -미인증 -> 401 + 로그인이 필요합니다
    * -비즈니스 예외 -> 401 BAD REQUEST + ApiResponse.error(message)
    * */
    @DeleteMapping("/me")
    public ResponseEntity<?> deleteMyAccount(HttpServletRequest request) {
        String token = extractTokenFromCookie(request);
        if (token == null) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("로그인이 필요합니다."));
        }

        try {
            userService.deleteMyAccount(token);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
        //accessToken 쿠키 삭제(만료)
        ResponseCookie expired = ResponseCookie.from("accessToken", "")
                .path("/")
                .maxAge(0)
                .httpOnly(true)
                .build();
        return ResponseEntity.ok()
                .header("Set-Cookie", expired.toString())
                .body(ApiResponse.ok("account_deleted"));
    }
}
