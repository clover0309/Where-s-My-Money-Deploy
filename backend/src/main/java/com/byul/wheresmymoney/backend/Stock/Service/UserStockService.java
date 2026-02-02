package com.byul.wheresmymoney.backend.Stock.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.byul.wheresmymoney.backend.Stock.Dto.StockDTO.AddStockRequest;
import com.byul.wheresmymoney.backend.Stock.Dto.StockDetailDTO;
import com.byul.wheresmymoney.backend.Stock.Dto.UserStockDTO;
import com.byul.wheresmymoney.backend.Stock.Entity.UserStockEntity;
import com.byul.wheresmymoney.backend.Stock.Repository.UserStockRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserStockService {
	
	private final UserStockRepository userStockRepository;
	private final StockPriceService stockPriceService;

	private String extractShortCode(String stockCode) {
		if (stockCode == null) {
			return stockCode;
		}
		

		if (stockCode.length() == 12 && stockCode.startsWith("KR7")) {
			String shortCode = stockCode.substring(3, 9);
			log.info("표준코드를 단축코드로 변환: {} -> {}", stockCode, shortCode);
			return shortCode;
		}

		if (stockCode.length() == 6) {
			return stockCode;
		}
		
		log.warn("알 수 없는 종목코드 형식: {}", stockCode);
		return stockCode;
	}
	
	public boolean addStock(String userId, AddStockRequest request) {
		try {
			log.info("주식 추가 시작: userId={}, stockCode={}, quantity={}, averagePrice={}, purchaseDate={}", 
				userId, request.getStockCode(), request.getQuantity(), request.getAveragePrice(), request.getPurchaseDate());
			
			String shortCode = extractShortCode(request.getStockCode());
			log.info("사용할 종목코드: {}", shortCode);
			
			Double averagePrice = request.getAveragePrice();
			if ((averagePrice == null || averagePrice == 0) && request.getPurchaseDate() != null) {
				log.info("매수 날짜로 주가 조회 시작: 종목={}, 날짜={}", shortCode, request.getPurchaseDate());
				
				try {
					BigDecimal fetchedPrice = stockPriceService.getStockPriceByDate(shortCode, request.getPurchaseDate());
					
					if (fetchedPrice != null) {
						averagePrice = fetchedPrice.doubleValue();
						log.info("주가 조회 성공: 종목={}, 가격={}", shortCode, averagePrice);
					} else {
						log.warn("주가 조회 실패: 종목={}, 날짜={} - API 응답 없음", shortCode, request.getPurchaseDate());
						return false;
					}
				} catch (Exception e) {
					log.error("주가 조회 API 호출 실패: 종목={}, 날짜={}, 오류={}", shortCode, request.getPurchaseDate(), e.getMessage(), e);
					return false;
				}
			}

			if (averagePrice == null || averagePrice <= 0) {
				log.error("매수가를 확인할 수 없습니다: 종목={}, averagePrice={}", shortCode, averagePrice);
				return false;
			}

			LocalDate purchaseDate = request.getPurchaseDate();
			if (purchaseDate == null) {
				purchaseDate = LocalDate.now();
			}

			log.info("신규 매수 내역 추가 시작: 종목={}, 날짜={}", shortCode, purchaseDate);
			
			UserStockEntity newStock = new UserStockEntity();
			newStock.setUserstockFidx(userId);
			newStock.setUserstockStk(shortCode);
			newStock.setUserstockName(request.getStockName());
			newStock.setUserstockQuantity(request.getQuantity());
			newStock.setUserstockAvgprice(BigDecimal.valueOf(averagePrice));
			newStock.setUserstockPurchasedate(purchaseDate);
			
			userStockRepository.save(newStock);
			
			log.info("신규 종목 추가 완료: stockCode={}, name={}, quantity={}, avgPrice={}, purchaseDate={}", 
				shortCode, request.getStockName(), request.getQuantity(), averagePrice, purchaseDate);
			return true;
		} catch (Exception e) {
			log.error("주식 추가 중 오류 발생: userId={}, stockCode={}, error={}", userId, request.getStockCode(), e.getMessage(), e);
			return false;
		}
	}
	
	public BigDecimal getStockPrice(String code, String date) {
		try {
			if (date != null && !date.isEmpty()) {
				LocalDate localDate = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
				return stockPriceService.getStockPriceByDate(code, localDate);
			} else {
				return stockPriceService.getCurrentStockPrice(code);
			}
		} catch (Exception e) {
			log.error("주가 조회 실패: code={}, date={}, error={}", code, date, e.getMessage());
			return null;
		}
	}
	
	public List<UserStockDTO> getUserStocks(String userId) {
		List<UserStockEntity> stocks = userStockRepository.findByUserId(userId);
		
		return stocks.stream()
			.map(stock -> {
				BigDecimal currentPrice = getStockPrice(stock.getUserstockStk(), null);
				if (currentPrice == null) {
					log.warn("현재가 조회 실패, 기본값 사용: stockCode={}", stock.getUserstockStk());
					currentPrice = BigDecimal.ZERO;
				}
				
				BigDecimal avgPrice = stock.getUserstockAvgprice();
				Double returnRate = 0.0;
				
				if (avgPrice.compareTo(BigDecimal.ZERO) > 0 && currentPrice.compareTo(BigDecimal.ZERO) > 0) {
					BigDecimal diff = currentPrice.subtract(avgPrice);
					returnRate = diff.divide(avgPrice, 4, RoundingMode.HALF_UP)
							.multiply(BigDecimal.valueOf(100))
							.doubleValue();
				}
				
				return new UserStockDTO(
					stock.getUserstockIdx(),
					stock.getUserstockStk(),
					stock.getUserstockName(),
					stock.getUserstockQuantity(),
					stock.getUserstockAvgprice(),
					stock.getUserstockCreatedat(),
					stock.getUserstockUpdatedat(),
					currentPrice,
					returnRate
				);
			})
			.collect(Collectors.toList());
	}
	
	public boolean deleteStock(Integer stockIdx, String userId) {
		try {
			if (stockIdx == null) {
				return false;
			}

			UserStockEntity stock = userStockRepository.findById(stockIdx).orElse(null);
			
			if (stock == null) {
				return false;
			}

			if (!stock.getUserstockFidx().equals(userId)) {
				return false;
			}
			
			userStockRepository.deleteById(stockIdx);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public StockDetailDTO getStockDetail(String userId, String stockCode) {
		try {
			log.info("주식 상세 정보 조회: userId={}, stockCode={}", userId, stockCode);

			String shortCode = extractShortCode(stockCode);

			UserStockEntity stock = userStockRepository.findByUserstockFidxAndUserstockStk(userId, shortCode);
			
			if (stock == null) {
				log.warn("보유 주식을 찾을 수 없음: userId={}, stockCode={}", userId, shortCode);
				return null;
			}

			BigDecimal currentPrice = stockPriceService.getCurrentStockPrice(shortCode);
			
			if (currentPrice == null) {
				log.warn("현재가 조회 실패: stockCode={}", shortCode);
				return null;
			}
			
			log.info("현재가 조회 성공: stockCode={}, currentPrice={}", shortCode, currentPrice);

			BigDecimal averagePrice = stock.getUserstockAvgprice();
			Integer quantity = stock.getUserstockQuantity();

			BigDecimal evaluationAmount = currentPrice.multiply(BigDecimal.valueOf(quantity));

			BigDecimal profitLoss = currentPrice.subtract(averagePrice).multiply(BigDecimal.valueOf(quantity));

			Double profitRate = 0.0;
			if (averagePrice.compareTo(BigDecimal.ZERO) > 0) {
				profitRate = currentPrice.subtract(averagePrice)
					.divide(averagePrice, 4, RoundingMode.HALF_UP)
					.multiply(BigDecimal.valueOf(100))
					.doubleValue();
			}

		String purchaseDate = null;
		if (stock.getUserstockPurchasedate() != null) {
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
			purchaseDate = stock.getUserstockPurchasedate().format(formatter);
		} else {
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
			purchaseDate = stock.getUserstockCreatedat().format(formatter);
		}			StockDetailDTO detail = new StockDetailDTO();
			detail.setStockCode(shortCode);
			detail.setStockName(stock.getUserstockName());
			detail.setQuantity(quantity);
			detail.setPurchaseDate(purchaseDate);
			detail.setAveragePrice(averagePrice);
			detail.setCurrentPrice(currentPrice);
			detail.setEvaluationAmount(evaluationAmount);
			detail.setProfitLoss(profitLoss);
			detail.setProfitRate(profitRate);
			
			log.info("주식 상세 정보 조회 완료: 수익률={}%", profitRate);
			
			return detail;
		} catch (Exception e) {
			log.error("주식 상세 정보 조회 중 오류: userId={}, stockCode={}, 오류={}", userId, stockCode, e.getMessage(), e);
			return null;
		}
	}
	
	public List<StockDetailDTO> getStockDetailList(String userId, String stockCode) {
		try {
			String shortCode = stockCode.length() > 6 ? stockCode.substring(1) : stockCode;

			List<UserStockEntity> stocks = userStockRepository.findAllByUserIdAndStockCode(userId, shortCode);
			
			if (stocks == null || stocks.isEmpty()) {
				log.warn("보유 주식을 찾을 수 없음: userId={}, stockCode={}", userId, shortCode);
				return null;
			}

			BigDecimal currentPrice = stockPriceService.getCurrentStockPrice(shortCode);
			
			if (currentPrice == null) {
				log.warn("현재가 조회 실패: stockCode={}", shortCode);
				return null;
			}
			
			log.info("현재가 조회 성공: stockCode={}, currentPrice={}", shortCode, currentPrice);

			List<StockDetailDTO> detailList = new ArrayList<>();
			
			for (UserStockEntity stock : stocks) {
				BigDecimal averagePrice = stock.getUserstockAvgprice();
				Integer quantity = stock.getUserstockQuantity();
				
				BigDecimal evaluationAmount = currentPrice.multiply(BigDecimal.valueOf(quantity));

				BigDecimal profitLoss = currentPrice.subtract(averagePrice).multiply(BigDecimal.valueOf(quantity));

				Double profitRate = 0.0;
				if (averagePrice.compareTo(BigDecimal.ZERO) > 0) {
					profitRate = currentPrice.subtract(averagePrice)
						.divide(averagePrice, 4, RoundingMode.HALF_UP)
						.multiply(BigDecimal.valueOf(100))
						.doubleValue();
				}

				String purchaseDate = null;
				if (stock.getUserstockPurchasedate() != null) {
					DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
					purchaseDate = stock.getUserstockPurchasedate().format(formatter);
				} else {
					DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
					purchaseDate = stock.getUserstockCreatedat().format(formatter);
				}
				
				StockDetailDTO detail = new StockDetailDTO();
				detail.setStockCode(shortCode);
				detail.setStockName(stock.getUserstockName());
				detail.setQuantity(quantity);
				detail.setPurchaseDate(purchaseDate);
				detail.setAveragePrice(averagePrice);
				detail.setCurrentPrice(currentPrice);
				detail.setEvaluationAmount(evaluationAmount);
				detail.setProfitLoss(profitLoss);
				detail.setProfitRate(profitRate);
				
				detailList.add(detail);
			}
			
			log.info("주식 상세 정보 리스트 조회 완료: 총 {}건", detailList.size());
			
			return detailList;
		} catch (Exception e) {
			log.error("주식 상세 정보 리스트 조회 중 오류: userId={}, stockCode={}, 오류={}", userId, stockCode, e.getMessage(), e);
			return null;
		}
	}
}
