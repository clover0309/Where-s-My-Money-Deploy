package com.byul.wheresmymoney.backend.User.Service.Login;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.byul.wheresmymoney.backend.User.Entity.UserEntity;
import com.byul.wheresmymoney.backend.User.Entity.UserStatus;
import com.byul.wheresmymoney.backend.User.Repository.Login.LoginRepository;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class LoginService {
    private final LoginRepository loginRepository;
    private final PasswordEncoder passwordEncoder;

    public UserEntity login(String id, String password) {
        UserEntity user = loginRepository.findByUserinfo_ID(id);
        
        if (user != null && passwordEncoder.matches(password, user.getUserinfo_PW())) {
            // BLOCKED 상태인 사용자는 로그인 불가
            if (user.getUserStatus() == UserStatus.BLOCKED) {
                System.out.println("  ✗ 탈퇴한 사용자입니다: " + id);
                return null;
            }
            return user;
        }
        
        return null;
    }

    public UserEntity findById(String id) {
        return loginRepository.findByUserinfo_ID(id);
    }

    public void save(UserEntity user) {
        loginRepository.save(user);
    }
}
