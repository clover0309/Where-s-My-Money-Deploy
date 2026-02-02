package com.byul.wheresmymoney.backend.Materials.Entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "usermaterials")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserMaterialsEntity {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "usermaterials_idx")
	private Integer usermaterialsIdx;
	
	@Column(name = "usermaterials_fidx", length = 50, nullable = false)
	private String usermaterialsFidx;
	
	@Column(name = "usermaterials_type", length = 20, nullable = false)
	private String usermaterialsType;
	
	@Column(name = "usermaterials_name", length = 100, nullable = false)
	private String usermaterialsName;
	
	@Column(name = "usermaterials_quantity", nullable = false, columnDefinition = "DECIMAL(15, 4) DEFAULT 0.0000")
	private BigDecimal usermaterialsQuantity = BigDecimal.ZERO;
	
	@Column(name = "usermaterials_avgprice", nullable = false, columnDefinition = "DECIMAL(15, 2) DEFAULT 0.00")
	private BigDecimal usermaterialsAvgprice = BigDecimal.ZERO;
	
	@Column(name = "usermaterials_stk")
	private String usermaterialsStk;
	
	@Column(name = "usermaterials_purchasedate", nullable = false)
	private LocalDate usermaterialsPurchasedate;
	
	@Column(name = "usermaterials_createdat", nullable = false, columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
	private LocalDateTime usermaterialsCreatedat = LocalDateTime.now();
	
	@Column(name = "usermaterials_updatedat", nullable = false, columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
	private LocalDateTime usermaterialsUpdatedat = LocalDateTime.now();
}
