package com.byul.wheresmymoney.backend.User.Service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.byul.wheresmymoney.backend.User.Entity.ActivityType;
import com.byul.wheresmymoney.backend.User.Entity.UserActivityLog;
import com.byul.wheresmymoney.backend.User.Repository.UserActivityLogRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserActivityLogService {

    private final UserActivityLogRepository userActivityLogRepository;

    public void logLogin(String userId) {
        UserActivityLog log = UserActivityLog.builder()
                .userActivity_userId(userId)
                .userActivity_type(ActivityType.USERLOGIN)
                .userActivity_timestamp(LocalDateTime.now())
                .build();
        
        userActivityLogRepository.save(log);
    }

    public void logLogout(String userId) {
        UserActivityLog log = UserActivityLog.builder()
                .userActivity_userId(userId)
                .userActivity_type(ActivityType.USERLOGOUT)
                .userActivity_timestamp(LocalDateTime.now())
                .build();
        
        userActivityLogRepository.save(log);
    }

    public void logRegister(String userId) {
        UserActivityLog log = UserActivityLog.builder()
                .userActivity_userId(userId)
                .userActivity_type(ActivityType.USERREGISTER)
                .userActivity_timestamp(LocalDateTime.now())
                .build();
        
        userActivityLogRepository.save(log);
    }

    public void logStockAdd(String userId) {
        UserActivityLog log = UserActivityLog.builder()
                .userActivity_userId(userId)
                .userActivity_type(ActivityType.STOCKADD)
                .userActivity_timestamp(LocalDateTime.now())
                .build();
        
        userActivityLogRepository.save(log);
    }

    public void logMaterialAdd(String userId) {
        UserActivityLog log = UserActivityLog.builder()
                .userActivity_userId(userId)
                .userActivity_type(ActivityType.SPOTADD)
                .userActivity_timestamp(LocalDateTime.now())
                .build();
        
        userActivityLogRepository.save(log);
    }

    public void logPasswordChange(String userId) {
        UserActivityLog log = UserActivityLog.builder()
                .userActivity_userId(userId)
                .userActivity_type(ActivityType.PWCHANGE)
                .userActivity_timestamp(LocalDateTime.now())
                .build();
        
        userActivityLogRepository.save(log);
    }

    public void logAccountDeletion(String userId) {
        UserActivityLog log = UserActivityLog.builder()
                .userActivity_userId(userId)
                .userActivity_type(ActivityType.USERDELETE)
                .userActivity_timestamp(LocalDateTime.now())
                .build();
        
        userActivityLogRepository.save(log);
    }
}
