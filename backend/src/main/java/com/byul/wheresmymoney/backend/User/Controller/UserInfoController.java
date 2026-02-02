package com.byul.wheresmymoney.backend.User.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.byul.wheresmymoney.backend.User.Dto.ApiResponse;
import com.byul.wheresmymoney.backend.User.Dto.ChangePasswordRequest;
import com.byul.wheresmymoney.backend.User.Dto.CheckPasswordRequest;
import com.byul.wheresmymoney.backend.User.Dto.DeleteAccountRequest;
import com.byul.wheresmymoney.backend.User.Entity.UserEntity;
import com.byul.wheresmymoney.backend.User.Entity.UserStatus;
import com.byul.wheresmymoney.backend.User.Service.Login.LoginService;
import com.byul.wheresmymoney.backend.User.Service.UserActivityLogService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Slf4j
public class UserInfoController {

    private final LoginService loginService;
    private final PasswordEncoder passwordEncoder;
    private final UserActivityLogService userActivityLogService;


    @GetMapping("/info")
    public ResponseEntity<ApiResponse> getUserInfo(@RequestParam String userId) {
        try {
            UserEntity user = loginService.findById(userId);
            if (user != null) {
                return ResponseEntity.ok(new ApiResponse(true, "사용자 정보 조회 성공", user));
            } else {
                return ResponseEntity.ok(new ApiResponse(false, "사용자를 찾을 수 없습니다."));
            }
        } catch (Exception e) {
            log.error("사용자 정보 조회 실패: ", e);
            return ResponseEntity.ok(new ApiResponse(false, "사용자 정보 조회 실패: " + e.getMessage()));
        }
    }

    @PostMapping("/check-password")
    public ResponseEntity<ApiResponse> checkPassword(@RequestBody CheckPasswordRequest request) {
        try {
            UserEntity user = loginService.findById(request.getUserId());
            
            if (user != null && passwordEncoder.matches(request.getPassword(), user.getUserinfo_PW())) {
                return ResponseEntity.ok(new ApiResponse(true, "비밀번호가 일치합니다."));
            } else {
                return ResponseEntity.ok(new ApiResponse(false, "비밀번호가 일치하지 않습니다."));
            }
        } catch (Exception e) {
            log.error("비밀번호 확인 실패: ", e);
            return ResponseEntity.ok(new ApiResponse(false, "비밀번호 확인 실패: " + e.getMessage()));
        }
    }


    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse> changePassword(@RequestBody ChangePasswordRequest request) {
        try {
            UserEntity user = loginService.findById(request.getUserId());
            
            if (user == null) {
                return ResponseEntity.ok(new ApiResponse(false, "사용자를 찾을 수 없습니다."));
            }
            
            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getUserinfo_PW())) {
                return ResponseEntity.ok(new ApiResponse(false, "현재 비밀번호가 일치하지 않습니다."));
            }
            
            String encodedNewPassword = passwordEncoder.encode(request.getNewPassword());
            user.setUserinfo_PW(encodedNewPassword);
            
            loginService.save(user);
            
            userActivityLogService.logPasswordChange(request.getUserId());
            
            return ResponseEntity.ok(new ApiResponse(true, "비밀번호가 성공적으로 변경되었습니다."));
            
        } catch (Exception e) {
            log.error("비밀번호 변경 실패: ", e);
            return ResponseEntity.ok(new ApiResponse(false, "비밀번호 변경 실패: " + e.getMessage()));
        }
    }


    @PostMapping("/delete-account")
    public ResponseEntity<ApiResponse> deleteAccount(@RequestBody DeleteAccountRequest request) {
        try {
            UserEntity user = loginService.findById(request.getUserId());
            
            if (user == null) {
                return ResponseEntity.ok(new ApiResponse(false, "사용자를 찾을 수 없습니다."));
            }
            
            if (!passwordEncoder.matches(request.getPassword(), user.getUserinfo_PW())) {
                return ResponseEntity.ok(new ApiResponse(false, "비밀번호가 일치하지 않습니다."));
            }
            
            user.setUserStatus(UserStatus.BLOCKED);
            loginService.save(user);
            
            userActivityLogService.logAccountDeletion(request.getUserId());
            
            return ResponseEntity.ok(new ApiResponse(true, "회원탈퇴가 완료되었습니다."));
            
        } catch (Exception e) {

            log.error("회원탈퇴 실패: ", e);
            return ResponseEntity.ok(new ApiResponse(false, "회원탈퇴 실패: " + e.getMessage()));
        }
    }
}
