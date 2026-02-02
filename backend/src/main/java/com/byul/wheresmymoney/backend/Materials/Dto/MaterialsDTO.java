package com.byul.wheresmymoney.backend.Materials.Dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class MaterialsDTO {
	
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class AddMaterialsRequest {
		private String userId;
		private String materialType;
		private String materialName;
		private BigDecimal quantity;
		private BigDecimal averagePrice;
		private LocalDate purchaseDate;
	}
	
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class UserMaterialsDTO {
		private Integer usermaterialsIdx;
		private String usermaterialsFidx;
		private String usermaterialsType;
		private String usermaterialsName;
		private BigDecimal usermaterialsQuantity;
		private BigDecimal usermaterialsAvgprice;
		private LocalDate usermaterialsPurchasedate;
	}
	
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class MaterialsDetailDTO {
		private String materialType;
		private String materialName;
		private BigDecimal quantity;
		private String purchaseDate;
		private BigDecimal averagePrice;
		private BigDecimal currentPrice;
		private BigDecimal evaluationAmount;
		private BigDecimal profitLoss;
		private Double profitRate;
	}
}
