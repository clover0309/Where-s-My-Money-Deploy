package com.byul.wheresmymoney.backend.User.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.byul.wheresmymoney.backend.User.Entity.UserActivityLog;

@Repository
public interface UserActivityLogRepository extends JpaRepository<UserActivityLog, Integer> {
}
