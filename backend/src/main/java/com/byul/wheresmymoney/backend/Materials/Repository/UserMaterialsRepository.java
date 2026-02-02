package com.byul.wheresmymoney.backend.Materials.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.byul.wheresmymoney.backend.Materials.Entity.UserMaterialsEntity;

@Repository
public interface UserMaterialsRepository extends JpaRepository<UserMaterialsEntity, Integer> {
	
	@Query("SELECT u FROM UserMaterialsEntity u WHERE u.usermaterialsFidx = :userId")
	List<UserMaterialsEntity> findByUserId(@Param("userId") String userId);
	
	@Query("SELECT u FROM UserMaterialsEntity u WHERE u.usermaterialsFidx = :userId AND u.usermaterialsType = :materialType")
	List<UserMaterialsEntity> findAllByUserIdAndMaterialType(@Param("userId") String userId, @Param("materialType") String materialType);
}
