package com.byul.wheresmymoney.backend.Materials.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.byul.wheresmymoney.backend.Materials.Dto.MaterialsDTO.AddMaterialsRequest;
import com.byul.wheresmymoney.backend.Materials.Dto.MaterialsDTO.MaterialsDetailDTO;
import com.byul.wheresmymoney.backend.Materials.Dto.MaterialsDTO.UserMaterialsDTO;
import com.byul.wheresmymoney.backend.Materials.Entity.UserMaterialsEntity;
import com.byul.wheresmymoney.backend.Materials.Repository.UserMaterialsRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserMaterialsService {
	
	private final UserMaterialsRepository userMaterialsRepository;
	private final MaterialsPriceService materialsPriceService;
	
	/**
	 * 금 추가
	 */
	public boolean addMaterials(AddMaterialsRequest request) {
		try {
			log.info("금 추가 시작: userId={}, type={}, quantity={}", 
				request.getUserId(), request.getMaterialType(), request.getQuantity());

			BigDecimal averagePrice = request.getAveragePrice();
			
			if (averagePrice == null) {
				if (request.getPurchaseDate() != null) {

					averagePrice = materialsPriceService.getGoldPriceByDate(
						request.getPurchaseDate().toString()
					);
				} else {
					averagePrice = materialsPriceService.getGoldPrice();
				}
			} else {
				System.out.println("averagePrice 수동 입력값 사용: " + averagePrice);
			}
			
			if (averagePrice == null || averagePrice.compareTo(BigDecimal.ZERO) <= 0) {
				log.error("매수가를 확인할 수 없습니다: type={}, averagePrice={}",
					request.getMaterialType(), averagePrice);
				return false;
			}
			
			// 매수일 결정
			LocalDate purchaseDate = request.getPurchaseDate();
			if (purchaseDate == null) {
				purchaseDate = LocalDate.now();
			}
			
			// 신규 금 추가 (같은 종류라도 매수 날짜가 다르면 별도로 저장)
			log.info("신규 금 추가: type={}, date={}", request.getMaterialType(), purchaseDate);
			
			UserMaterialsEntity newMaterial = new UserMaterialsEntity();
			newMaterial.setUsermaterialsFidx(request.getUserId());
			newMaterial.setUsermaterialsType(request.getMaterialType());
			newMaterial.setUsermaterialsName(request.getMaterialName());
			newMaterial.setUsermaterialsQuantity(request.getQuantity());
			newMaterial.setUsermaterialsAvgprice(averagePrice);
			newMaterial.setUsermaterialsPurchasedate(purchaseDate);

			UserMaterialsEntity savedEntity = userMaterialsRepository.save(newMaterial);
			
			log.info("금 추가 완료: type={}, name={}, quantity={}, avgPrice={}, purchaseDate={}", 
				request.getMaterialType(), request.getMaterialName(), 
				request.getQuantity(), averagePrice, purchaseDate);
			return true;
		} catch (Exception e) {
			log.error("금 추가 중 오류 발생: userId={}, error={}", request.getUserId(), e.getMessage(), e);
			e.printStackTrace();
			return false;
		}
	}
	

	public List<UserMaterialsDTO> getUserMaterials(String userId) {
		try {
			List<UserMaterialsEntity> entities = userMaterialsRepository.findByUserId(userId);
			
			return entities.stream()
				.map(entity -> new UserMaterialsDTO(
					entity.getUsermaterialsIdx(),
					entity.getUsermaterialsFidx(),
					entity.getUsermaterialsType(),
					entity.getUsermaterialsName(),
					entity.getUsermaterialsQuantity(),
					entity.getUsermaterialsAvgprice(),
					entity.getUsermaterialsPurchasedate()
				))
				.collect(Collectors.toList());
		} catch (Exception e) {
			log.error("금 목록 조회 중 오류: userId={}", userId, e);
			return new ArrayList<>();
		}
	}
	
	public boolean deleteMaterials(Integer materialsIdx, String userId) {
		try {
			UserMaterialsEntity material = userMaterialsRepository.findById(materialsIdx).orElse(null);
			
			if (material == null) {
				log.warn("금을 찾을 수 없음: idx={}", materialsIdx);
				return false;
			}
			
			if (!material.getUsermaterialsFidx().equals(userId)) {
				log.warn("권한 없음: idx={}, userId={}", materialsIdx, userId);
				return false;
			}
			
			userMaterialsRepository.delete(material);
			log.info("금 삭제 완료: idx={}", materialsIdx);
			
			return true;
		} catch (Exception e) {
			log.error("금 삭제 중 오류: idx={}", materialsIdx, e);
			return false;
		}
	}
	

	public List<MaterialsDetailDTO> getMaterialsDetailList(String userId, String materialType) {
		try {

			List<UserMaterialsEntity> materials = userMaterialsRepository
				.findAllByUserIdAndMaterialType(userId, materialType);
			
			if (materials == null || materials.isEmpty()) {
				log.warn("보유 금을 찾을 수 없음: userId={}, type={}", userId, materialType);
				return null;
			}

			BigDecimal currentPrice = materialsPriceService.getGoldPrice();
			
			if (currentPrice == null) {
				log.warn("현재가 조회 실패: type={}", materialType);
				return null;
			}
			
			log.info("현재가 조회 성공: type={}, currentPrice={}", materialType, currentPrice);
			
			List<MaterialsDetailDTO> detailList = new ArrayList<>();
			
			for (UserMaterialsEntity material : materials) {
				BigDecimal quantity = material.getUsermaterialsQuantity();
				BigDecimal averagePrice = material.getUsermaterialsAvgprice();
				
				BigDecimal evaluationAmount = currentPrice.multiply(quantity);
				
				BigDecimal profitLoss = currentPrice.subtract(averagePrice).multiply(quantity);
				
				Double profitRate = 0.0;
				if (averagePrice.compareTo(BigDecimal.ZERO) > 0) {
					profitRate = currentPrice.subtract(averagePrice)
						.divide(averagePrice, 4, RoundingMode.HALF_UP)
						.multiply(BigDecimal.valueOf(100))
						.doubleValue();
				}

				String purchaseDate = null;
				if (material.getUsermaterialsPurchasedate() != null) {
					DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
					purchaseDate = material.getUsermaterialsPurchasedate().format(formatter);
				} else {
					DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
					purchaseDate = material.getUsermaterialsCreatedat().toLocalDate().format(formatter);
				}
				
				MaterialsDetailDTO detail = new MaterialsDetailDTO();
				detail.setMaterialType(material.getUsermaterialsType());
				detail.setMaterialName(material.getUsermaterialsName());
				detail.setQuantity(quantity);
				detail.setPurchaseDate(purchaseDate);
				detail.setAveragePrice(averagePrice);
				detail.setCurrentPrice(currentPrice);
				detail.setEvaluationAmount(evaluationAmount);
				detail.setProfitLoss(profitLoss);
				detail.setProfitRate(profitRate);
				
				detailList.add(detail);
			}
			
			log.info("금 상세 정보 리스트 조회 완료: 총 {}건", detailList.size());
			
			return detailList;
		} catch (Exception e) {
			log.error("금 상세 정보 리스트 조회 중 오류: userId={}, type={}, 오류={}", 
				userId, materialType, e.getMessage(), e);
			return null;
		}
	}
}
