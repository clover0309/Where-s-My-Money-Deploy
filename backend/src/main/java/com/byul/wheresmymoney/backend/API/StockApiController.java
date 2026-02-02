package com.byul.wheresmymoney.backend.API;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.byul.wheresmymoney.backend.Stock.Dto.StockDTO.AddStockRequest;
import com.byul.wheresmymoney.backend.Stock.Dto.StockDTO.StockSearchResponse;
import com.byul.wheresmymoney.backend.Stock.Dto.StockDetailDTO;
import com.byul.wheresmymoney.backend.Stock.Dto.UserStockDTO;
import com.byul.wheresmymoney.backend.Stock.Service.KiwoomApiService;
import com.byul.wheresmymoney.backend.Stock.Service.StockSearchService;
import com.byul.wheresmymoney.backend.Stock.Service.UserStockService;
import com.byul.wheresmymoney.backend.User.Dto.ApiResponse;
import com.byul.wheresmymoney.backend.User.Service.UserActivityLogService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
@Slf4j
public class StockApiController {

    private final StockSearchService stockSearchService;
    private final UserStockService userStockService;
    private final UserActivityLogService userActivityLogService;
    private final KiwoomApiService kiwoomApiService;

    @PostMapping("/test-connection")
    public ResponseEntity<ApiResponse> testKiwoomConnection() {
        try {
            String token = kiwoomApiService.getAccessToken();
            if (token != null && !token.isEmpty()) {
                Map<String, Object> data = new HashMap<>();
                return ResponseEntity.ok(new ApiResponse(true, "연결 성공 (토큰 발급 완료)", data));
            } else {
                return ResponseEntity.ok(new ApiResponse(false, "연결 실패: 토큰을 받아오지 못했습니다."));
            }
        } catch (Exception e) {
            return ResponseEntity.ok(new ApiResponse(false, "연결 실패: " + e.getMessage()));
        }
    }
    
    @GetMapping("/search")
    public ResponseEntity<List<StockSearchResponse>> searchStock(@RequestParam String keyword) {
        if (keyword == null || keyword.trim().length() < 1) {
            return ResponseEntity.badRequest().build();
        }
        
        List<StockSearchResponse> results = stockSearchService.searchStocks(keyword);
        return ResponseEntity.ok(results);
    }
    
    @GetMapping("/price")
    public ResponseEntity<ApiResponse> getStockPrice(@RequestParam String code, @RequestParam(required = false) String date) {
        if (code == null || code.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "종목 코드가 필요합니다."));
        }
        
        try {
            BigDecimal price = userStockService.getStockPrice(code, date);
            Map<String, Object> data = new HashMap<>();
            data.put("price", price);
            return ResponseEntity.ok(new ApiResponse(true, "가격 조회 성공", data));
        } catch (Exception e) {
            System.err.println("Error retrieving price: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.ok(new ApiResponse(false, "가격 조회 실패: " + e.getMessage()));
        }
    }

    @PostMapping("/add")
    public ResponseEntity<ApiResponse> addStock(@RequestBody AddStockRequest request) {
        log.info("주식 추가 요청 수신: userId={}, stockCode={}, stockName={}, quantity={}, averagePrice={}, purchaseDate={}", 
            request.getUserId(), request.getStockCode(), request.getStockName(), 
            request.getQuantity(), request.getAveragePrice(), request.getPurchaseDate());
        
        try {
            if (request.getUserId() == null || request.getUserId().trim().isEmpty()) {
                log.warn("로그인 정보가 없습니다.");
                return ResponseEntity.ok(new ApiResponse(false, "로그인이 필요합니다."));
            }
            
            if (request.getStockCode() == null || request.getStockCode().trim().isEmpty()) {
                log.warn("종목 코드가 없습니다.");
                return ResponseEntity.ok(new ApiResponse(false, "종목을 선택해주세요."));
            }
            
            if (request.getQuantity() == null || request.getQuantity() <= 0) {
                log.warn("수량이 유효하지 않습니다: {}", request.getQuantity());
                return ResponseEntity.ok(new ApiResponse(false, "유효한 수량을 입력해주세요."));
            }
            
            // 평균 매수가와 매수 날짜 둘 다 없는 경우
            if ((request.getAveragePrice() == null || request.getAveragePrice() <= 0) 
                && request.getPurchaseDate() == null) {
                log.warn("평균 매수가와 매수 날짜가 모두 없습니다.");
                return ResponseEntity.ok(new ApiResponse(false, "평균 매수가 또는 매수 날짜를 입력해주세요."));
            }
            
            String userId = request.getUserId();
            boolean success = userStockService.addStock(userId, request);
            
            if (success) {
                log.info("주식 추가 성공: userId={}, stockCode={}", userId, request.getStockCode());
                
                // 주식 추가 활동 기록
                userActivityLogService.logStockAdd(userId);
                
                return ResponseEntity.ok(new ApiResponse(true, "주식이 추가되었습니다."));
            } else {
                log.error("주식 추가 실패: userId={}, stockCode={}", userId, request.getStockCode());
                
                // 매수 날짜로 조회했는지 확인
                if (request.getPurchaseDate() != null) {
                    return ResponseEntity.ok(new ApiResponse(false, "해당 날짜의 주가 정보를 가져올 수 없습니다. 평균 매수가를 직접 입력해주세요."));
                } else {
                    return ResponseEntity.ok(new ApiResponse(false, "주식 추가에 실패했습니다."));
                }
            }
        } catch (Exception e) {
            log.error("주식 추가 중 예외 발생: userId={}, error={}", request.getUserId(), e.getMessage(), e);
            return ResponseEntity.ok(new ApiResponse(false, "서버 오류가 발생했습니다: " + e.getMessage()));
        }
    }
    
    @GetMapping("/list")
    public ResponseEntity<List<UserStockDTO>> getUserStocks(@RequestParam String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        List<UserStockDTO> stocks = userStockService.getUserStocks(userId);
        return ResponseEntity.ok(stocks);
    }
    
    @DeleteMapping("/delete")
    public ResponseEntity<ApiResponse> deleteStock(@RequestParam Integer stockIdx, @RequestParam String userId) {
        if (stockIdx == null || userId == null || userId.trim().isEmpty()) {
            return ResponseEntity.ok(new ApiResponse(false, "잘못된 요청입니다."));
        }
        
        boolean success = userStockService.deleteStock(stockIdx, userId);
        
        if (success) {
            return ResponseEntity.ok(new ApiResponse(true, "주식이 삭제되었습니다."));
        } else {
            return ResponseEntity.ok(new ApiResponse(false, "주식 삭제에 실패했습니다."));
        }
    }
    
    @GetMapping("/detail")
    public ResponseEntity<ApiResponse> getStockDetail(@RequestParam String userId, @RequestParam String stockCode) {
    	log.info("주식 상세 정보 요청: userId={}, stockCode={}", userId, stockCode);
    	
    	if (userId == null || userId.trim().isEmpty() || stockCode == null || stockCode.trim().isEmpty()) {
    		return ResponseEntity.ok(new ApiResponse(false, "잘못된 요청입니다."));
    	}
    	
    	try {
    		// 날짜별로 구분된 매수내역 리스트 조회
    		List<StockDetailDTO> detailList = userStockService.getStockDetailList(userId, stockCode);
    		
    		if (detailList == null || detailList.isEmpty()) {
    			return ResponseEntity.ok(new ApiResponse(false, "주식 상세 정보를 찾을 수 없습니다."));
    		}
    		
    		return ResponseEntity.ok(new ApiResponse(true, "조회 성공", detailList));
    	} catch (Exception e) {
    		log.error("주식 상세 정보 조회 실패: userId={}, stockCode={}", userId, stockCode, e);
    		return ResponseEntity.ok(new ApiResponse(false, "상세 정보 조회 중 오류가 발생했습니다."));
    	}
    }
}
