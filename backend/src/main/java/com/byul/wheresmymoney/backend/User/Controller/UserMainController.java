package com.byul.wheresmymoney.backend.User.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.byul.wheresmymoney.backend.Config.JwtTokenProvider;
import com.byul.wheresmymoney.backend.User.Dto.ApiResponse;
import com.byul.wheresmymoney.backend.User.Dto.LoginRequest;
import com.byul.wheresmymoney.backend.User.Dto.LoginResponse;
import com.byul.wheresmymoney.backend.User.Dto.RegisterRequest;
import com.byul.wheresmymoney.backend.User.Entity.UserEntity;
import com.byul.wheresmymoney.backend.User.Service.Login.LoginService;
import com.byul.wheresmymoney.backend.User.Service.Register.RegisterService;
import com.byul.wheresmymoney.backend.User.Service.UserActivityLogService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class UserMainController {

    private final RegisterService registerService;
    private final LoginService loginService;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserActivityLogService userActivityLogService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse> register(@RequestBody RegisterRequest request) {
        try {
            UserEntity user = registerService.Register(
                request.getId(),
                request.getPassword(),
                request.getName(),
                request.getEmail()
            );

            userActivityLogService.logRegister(user.getUserinfo_ID());
            return ResponseEntity.ok(new ApiResponse(true, "회원가입이 완료되었습니다.", user.getUserinfo_ID()));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse(false, "회원가입 실패: " + e.getMessage()));
        }
    }

    @GetMapping("/check-id")
    public ResponseEntity<ApiResponse> checkId(@RequestParam String id) {
        try {
            UserEntity existingUser = loginService.findById(id);
            if (existingUser != null) {
                return ResponseEntity.ok(new ApiResponse(false, "이미 사용 중인 아이디입니다."));
            } else {
                return ResponseEntity.ok(new ApiResponse(true, "사용 가능한 아이디입니다."));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse(false, "아이디 확인 실패: " + e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse> login(@RequestBody LoginRequest request) {
        try {
            UserEntity user = loginService.login(request.getId(), request.getPassword());
            
            if (user != null) {
                String token = jwtTokenProvider.createToken(user.getUserinfo_ID());
                
                LoginResponse loginResponse = new LoginResponse(user.getUserinfo_ID(), token);
                
                log.info("로그인 성공: userId={}", user.getUserinfo_ID());
                
                userActivityLogService.logLogin(user.getUserinfo_ID());
                
                return ResponseEntity.ok(new ApiResponse(true, "로그인 성공", loginResponse));
            } else {
                System.out.println("  ✗ 로그인 실패: 인증 실패");
                System.out.println("====================================\n");
                
                return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "아이디 또는 비밀번호가 일치하지 않습니다."));
            }
        } catch (Exception e) {
            System.out.println("  ✗ 로그인 예외: " + e.getMessage());
            System.out.println("====================================\n");
            
            log.error("로그인 실패: ", e);
            return ResponseEntity.badRequest()
                .body(new ApiResponse(false, "로그인 실패: " + e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse> logout(@RequestParam(required = false) String userId) {
        try {
            
            if (userId != null && !userId.trim().isEmpty()) {
                userActivityLogService.logLogout(userId);
            } else {
                System.out.println("  ✗ 사용자 ID 없음 - 활동 기록 생략");
            }
            
            System.out.println("====================================\n");
            return ResponseEntity.ok(new ApiResponse(true, "로그아웃 성공"));
        } catch (Exception e) {
            System.out.println("  ✗ 로그아웃 예외: " + e.getMessage());
            System.out.println("====================================\n");
            return ResponseEntity.badRequest()
                .body(new ApiResponse(false, "로그아웃 실패: " + e.getMessage()));
        }
    }

    @GetMapping("/verify")
    public ResponseEntity<ApiResponse> verifyToken(@RequestParam String token) {
        try {
            
            if (jwtTokenProvider.validateToken(token)) {
                String userId = jwtTokenProvider.getUserId(token);

                LoginResponse loginResponse = new LoginResponse(userId, token);
                return ResponseEntity.ok(new ApiResponse(true, "유효한 토큰입니다.", loginResponse));
            } else {
                System.out.println("  ✗ 토큰 검증 실패: 유효하지 않은 토큰");
                System.out.println("====================================\n");
                return ResponseEntity.ok(new ApiResponse(false, "유효하지 않은 토큰입니다."));
            }
        } catch (Exception e) {
            System.out.println("  ✗ 토큰 검증 예외: " + e.getMessage());
            System.out.println("====================================\n");
            log.error("토큰 검증 실패: ", e);
            return ResponseEntity.ok(new ApiResponse(false, "토큰 검증 실패: " + e.getMessage()));
        }
    }

    @GetMapping("/findpw")
    public String UserFindPW() {
        return "여기에 아이디 비밀번호 찾기 로직 추가.";
    }

    @GetMapping("/running")
    public String test() {
        return "구동중.";
    }
}
