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

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    //회원가입
    @PostMapping("/sign-up")
    public ResponseEntity<?> signUp(@RequestBody @Valid UserDTO.SignUpReq req) {
        var res = userService.signUp(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(res));
    }

    //내 정보 조회
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

    //공통 유틸: HttpServletRequest에서 accessToken 쿠키 추출
    private String extractTokenFromCookie(HttpServletRequest request) {
        if(request.getCookies() == null) return null;
        for(Cookie c : request.getCookies()) {
            if ("accessToken".equals(c.getName())) {
                return c.getValue();
            }
        }
        return null;
    }

    //비밀번호 변경
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

    //닉네임 변경
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

    //회원 탈퇴
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
