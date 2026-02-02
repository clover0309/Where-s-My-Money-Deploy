package com.byul.wheresmymoney.backend.Stock.Service;

import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockPriceService {
	
	private final KiwoomApiService kiwoomApiService;

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

	public BigDecimal getStockPriceByDate(String stockCode, LocalDate purchaseDate) {
		try {
			log.info("주가 조회 시작: stockCode={}, purchaseDate={}", stockCode, purchaseDate);

		String shortCode = extractShortCode(stockCode);
		log.info("단축코드 변환 완료: {} -> {}", stockCode, shortCode);
		
		String token = kiwoomApiService.getAccessToken();

		if (token == null || token.isEmpty()) {
			log.error("토큰 발급 실패: token이 null입니다.");
			return null;
		}
		
		log.info("액세스 토큰 발급 완료");
		String host = "https://api.kiwoom.com";
		String endpoint = "/api/dostk/mrkcond";
		
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
		String dateStr = purchaseDate.format(formatter);
		log.info("날짜 포맷 변환: {} -> {}", purchaseDate, dateStr);
		
		String jsonBody = String.format(
			"{\"stk_cd\":\"%s\",\"qry_dt\":\"%s\",\"indc_tp\":\"0\"}",
			shortCode,
			dateStr
		);
		
		log.info("API 호출 URL: {}, Body: {}", host + endpoint, jsonBody);
		
		URL url = new URL(host + endpoint);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		
		connection.setRequestMethod("POST");
		connection.setDoOutput(true);
		connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
		connection.setRequestProperty("Authorization", "Bearer " + token.trim());
		connection.setRequestProperty("cont-yn", "N");
		connection.setRequestProperty("next-key", "");
		connection.setRequestProperty("api-id", "ka10086");
		connection.setConnectTimeout(10000);
		connection.setReadTimeout(10000);
		
		try (OutputStream os = connection.getOutputStream()) {
			byte[] input = jsonBody.getBytes("utf-8");
			os.write(input, 0, input.length);
		}			int responseCode = connection.getResponseCode();
			log.info("API 응답 코드: {}", responseCode);
			
			if (responseCode == 200) {
				try (Scanner scanner = new Scanner(connection.getInputStream(), "utf-8")) {
					String responseBody = scanner.useDelimiter("\\A").next();
					
					log.info("주가 조회 응답: {}", responseBody);
					
					ObjectMapper objectMapper = new ObjectMapper();
					@SuppressWarnings("unchecked")
					Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
					
					log.info("API 응답 파싱 완료: {}", responseMap);
					
					@SuppressWarnings("unchecked")
					List<Map<String, Object>> dalyStkpc = (List<Map<String, Object>>) responseMap.get("daly_stkpc");
					
				if (dalyStkpc != null && !dalyStkpc.isEmpty()) {
					Map<String, Object> priceData = dalyStkpc.get(0);
					String closePriceStr = (String) priceData.get("close_pric");
					
					if (closePriceStr != null && !closePriceStr.isEmpty()) {
						closePriceStr = closePriceStr.replace("+", "").replace("-", "");
						BigDecimal closePrice = new BigDecimal(closePriceStr);
						log.info("종가 조회 성공: 종목={}, 날짜={}, 종가={}", shortCode, dateStr, closePrice);
						return closePrice;
					} else {
						log.warn("종가 데이터가 비어있음: 종목={}, 날짜={}", shortCode, dateStr);
					}
				} else {
					log.warn("daly_stkpc 배열이 비어있음: 종목={}, 날짜={}", shortCode, dateStr);
				}					log.warn("주가 데이터를 찾을 수 없습니다. 종목코드: {}, 날짜: {}, 응답: {}", stockCode, dateStr, responseBody);
					return null;
				}
			} else {
				String errorBody = "";
				try (Scanner scanner = new Scanner(connection.getErrorStream(), "utf-8")) {
					errorBody = scanner.useDelimiter("\\A").next();
				} catch (Exception e) {
					log.debug("에러 스트림 읽기 실패", e);
				}
				log.error("주가 조회 API 호출 실패: HTTP {}, 종목코드: {}, 날짜: {}, 에러: {}", responseCode, stockCode, dateStr, errorBody);
				return null;
			}
			
		} catch (Exception e) {
			log.error("주가 조회 중 오류 발생: 종목코드={}, 날짜={}, 오류={}", stockCode, purchaseDate, e.getMessage(), e);
			return null;
		}
	}

	public BigDecimal getCurrentStockPrice(String stockCode) {
		try {
			log.info("===== 현재가 조회 시작 =====");
			log.info("요청 종목코드: {}", stockCode);

		String shortCode = extractShortCode(stockCode);
		log.info("단축코드 변환: {} -> {}", stockCode, shortCode);
		
		String token = kiwoomApiService.getAccessToken();
		
		if (token == null || token.isEmpty()) {
			log.error("토큰 발급 실패: token이 null입니다.");
			return null;
		}
		
		log.info("토큰 발급 완료");
		String host = "https://api.kiwoom.com";
		String endpoint = "/api/dostk/stkinfo";

		String jsonBody = String.format(
			"{\"stk_cd\":\"%s\"}",
			shortCode
		);
			
			log.info("API 호출 URL: {}, Body: {}", host + endpoint, jsonBody);
			
			URL url = new URL(host + endpoint);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			
			connection.setRequestMethod("POST");
			connection.setDoOutput(true);
			connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
		connection.setRequestProperty("Authorization", "Bearer " + token.trim());
		connection.setRequestProperty("cont-yn", "N");
		connection.setRequestProperty("next-key", "");
		connection.setRequestProperty("api-id", "ka10100");
		connection.setConnectTimeout(10000);
		connection.setReadTimeout(10000);

			try (OutputStream os = connection.getOutputStream()) {
				byte[] input = jsonBody.getBytes("utf-8");
				os.write(input, 0, input.length);
			}
			
			int responseCode = connection.getResponseCode();
			log.info("API 응답 코드: {}", responseCode);
			
			if (responseCode == 200) {
				try (Scanner scanner = new Scanner(connection.getInputStream(), "utf-8")) {
					String responseBody = scanner.useDelimiter("\\A").next();
					
					log.info("현재가 조회 응답: {}", responseBody);
					
					ObjectMapper objectMapper = new ObjectMapper();
					@SuppressWarnings("unchecked")
					Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
				
				if (responseMap.containsKey("return_code")) {
					System.out.println("return_code: " + responseMap.get("return_code"));
					System.out.println("return_msg: " + responseMap.get("return_msg"));
				}

				if (responseMap.containsKey("lastPrice")) {
					String lastPriceStr = (String) responseMap.get("lastPrice");

					lastPriceStr = lastPriceStr.replace("+", "").replace("-", "");
					BigDecimal currentPrice = new BigDecimal(lastPriceStr);

					log.info("현재가 조회 완료: 종목코드={}, 현재가={}", stockCode, currentPrice);
					
					return currentPrice;
				}
				
				// 만약 output 필드 안에 있을 경우를 대비
				@SuppressWarnings("unchecked")
				Map<String, Object> output = (Map<String, Object>) responseMap.get("output");
				
				if (output != null) {
					if (output.containsKey("lastPrice")) {
						String lastPriceStr = (String) output.get("lastPrice");
						lastPriceStr = lastPriceStr.replace("+", "").replace("-", "");
						BigDecimal currentPrice = new BigDecimal(lastPriceStr);
						return currentPrice;
					}
				} else {
					System.out.println("output 필드가 null입니다.");
				}
					
					log.warn("현재 주가 데이터를 찾을 수 없습니다. 종목코드: {}, 응답: {}", stockCode, responseBody);
					return null;
				}
			} else {
				String errorBody = "";
				try (Scanner scanner = new Scanner(connection.getErrorStream(), "utf-8")) {
					errorBody = scanner.useDelimiter("\\A").next();
				} catch (Exception e) {
					System.out.println("에러 스트림 읽기 실패: " + e.getMessage());
				}
				
				log.error("현재가 조회 API 호출 실패: HTTP {}, 종목코드: {}, 에러: {}", responseCode, stockCode, errorBody);
				return null;
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			log.error("현재가 조회 중 오류 발생: 종목코드={}, 오류={}", stockCode, e.getMessage(), e);
			return null;
		}
	}
}
