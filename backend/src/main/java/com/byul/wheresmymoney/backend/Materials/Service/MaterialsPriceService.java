package com.byul.wheresmymoney.backend.Materials.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MaterialsPriceService {
	
	private final ObjectMapper objectMapper = new ObjectMapper();
	
	@Value("${kiwoom.appkey}")
	private String appKey;
	
	@Value("${kiwoom.secretkey}")
	private String secretKey;
	
	private static final String KIWOOM_TOKEN_URL = "https://api.kiwoom.com/oauth2/token";
	private static final String KIWOOM_GOLD_PRICE_URL = "https://api.kiwoom.com/api/dostk/mrkcond";
	private static final String GOLD_STOCK_CODE = "M04020000"; // 금 1kg 종목코드
	
	private String cachedAccessToken = null;
	private long tokenExpireTime = 0;
	

	private String getAccessToken() {
		
		if (cachedAccessToken != null && System.currentTimeMillis() < tokenExpireTime) {
			return cachedAccessToken;
		}
		
		try {
			URL url = new URL(KIWOOM_TOKEN_URL);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
			conn.setDoOutput(true);

			String requestBody = "grant_type=client_credentials"
				+ "&appkey=" + appKey
				+ "&secretkey=" + secretKey;
			
			try (OutputStream os = conn.getOutputStream()) {
				byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
				os.write(input, 0, input.length);
			}
			
			int responseCode = conn.getResponseCode();
			
			if (responseCode == 200) {
				try (BufferedReader br = new BufferedReader(
					new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
					
					StringBuilder response = new StringBuilder();
					String line;
					while ((line = br.readLine()) != null) {
						response.append(line);
					}

					JsonNode jsonNode = objectMapper.readTree(response.toString());
					cachedAccessToken = jsonNode.get("access_token").asText();
					int expiresIn = jsonNode.get("expires_in").asInt();

					tokenExpireTime = System.currentTimeMillis() + ((expiresIn - 600) * 1000L);
					;
					log.info("키움증권 접근 토큰 발급 성공");
					
					return cachedAccessToken;
				}
			} else {
				log.error("키움증권 토큰 발급 실패: {}", responseCode);
			}
			
		} catch (Exception e) {
			log.error("키움증권 토큰 발급 예외", e);
		}
		
		return null;
	}

	public BigDecimal getGoldPrice() {
		try {
			String accessToken = getAccessToken();
			if (accessToken == null) {
				return new BigDecimal("85000");
			}
			
			URL url = new URL(KIWOOM_GOLD_PRICE_URL);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
			conn.setRequestProperty("authorization", "Bearer " + accessToken);
			conn.setRequestProperty("api-id", "ka50100");
			conn.setDoOutput(true);
			
			String jsonInputString = "{\"stk_cd\":\"" + GOLD_STOCK_CODE + "\"}";
			
			try (OutputStream os = conn.getOutputStream()) {
				byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
				os.write(input, 0, input.length);
			}
			
			int responseCode = conn.getResponseCode();
			
			if (responseCode == 200) {
				try (BufferedReader br = new BufferedReader(
					new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
					
					StringBuilder response = new StringBuilder();
					String line;
					while ((line = br.readLine()) != null) {
						response.append(line.trim());
					}
					
					JsonNode jsonNode = objectMapper.readTree(response.toString());

					if (jsonNode.has("output") && jsonNode.get("output").has("stck_prpr")) {
						String priceStr = jsonNode.get("output").get("stck_prpr").asText();
						BigDecimal price1kg = new BigDecimal(priceStr);
						BigDecimal price1g = price1kg.divide(new BigDecimal("1000"), 2, RoundingMode.HALF_UP);
						log.info("금 시세 조회 성공: 1g = {}원", price1g);
						return price1g;
					} else {
						log.warn("금 시세 응답 파싱 실패 - 임시 가격 반환");
					}
				}
			} else {
				log.error("금 시세 조회 실패: {}", responseCode);
			}
			
		} catch (Exception e) {
			log.error("금 시세 조회 중 예외 발생", e);
			e.printStackTrace();
		}

		log.warn("금 시세 조회 실패 - 임시 가격 사용");
		return new BigDecimal("85000");
	}

	public BigDecimal getGoldPriceByDate(String date) {
		log.warn("날짜별 금 시세 조회 현재 시세 반환");
		return getGoldPrice();
	}
}
