package com.byul.wheresmymoney.backend.User.Entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table (name = "useractivity")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "userActivity_idx")
    private int userActivity_idx;

    @Column(name = "userActivity_userId", nullable = false, length = 50)
    private String userActivity_userId;

    // 보유주식 추가, 보유주식 삭제, 보유현물 추가, 보유현물 삭제, 패스워드변경, 로그인 기록, 로그아웃 기록.
    @Enumerated(EnumType.STRING)
    @Column(name = "userActivity_type", nullable = false)
    private ActivityType userActivity_type;

    @Column(name = "userActivity_timestamp", nullable = false)
    private LocalDateTime userActivity_timestamp;
}
