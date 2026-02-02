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
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class KiwoomApiService {

    @Value("${kiwoom.appkey}")
    private String appkey;

    @Value("${kiwoom.secretkey}")
    private String secretkey;

    private String accessToken;
    private long tokenExpirationTime;

    private final ObjectMapper objectMapper = new ObjectMapper();


    public String getAccessToken() {
        if (accessToken != null && System.currentTimeMillis() < tokenExpirationTime) {
            return accessToken;
        }

        try {
            String host = "https://api.kiwoom.com";
            String endpoint = "/oauth2/token";
            String urlString = host + endpoint;
            
            log.info("Requesting Access Token from: {}", urlString);

            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json"); 
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
            connection.setDoOutput(true);

            String requestBody = "{\"grant_type\":\"client_credentials\",\"appkey\":\"" 
                                + (appkey != null ? appkey.trim() : "") 
                                + "\",\"secretkey\":\"" + (secretkey != null ? secretkey.trim() : "") + "\"}";

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                    
                    String responseBody = response.toString();
                    log.info("토큰 API 응답: {}", responseBody);
                    Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
                    this.accessToken = (String) responseMap.get("access_token");
                    
                    if (this.accessToken == null) {
                        this.accessToken = (String) responseMap.get("token");
                    }
                    
                    if (this.accessToken == null) {
                        log.error("토큰을 찾을 수 없습니다. 응답: {}", responseBody);
                        return null;
                    }
                    
                    Object expiresInObj = responseMap.get("expires_in");
                    int expiresIn = 86400;
                    
                    if (expiresInObj instanceof Integer) {
                        expiresIn = (Integer) expiresInObj;
                    } else if (expiresInObj instanceof String) {
                        try {
                            expiresIn = Integer.parseInt((String) expiresInObj);
                        } catch (NumberFormatException e) {
                            log.warn("Invalid expires_in format: {}", expiresInObj);
                        }
                    }
                    
                    this.tokenExpirationTime = System.currentTimeMillis() + (expiresIn * 1000L) - 60000;
                    
                    log.info("Access Token obtained successfully. Expires in: {} seconds", expiresIn);
                    return this.accessToken;
                }
            } else {
                log.error("Failed to get access token. HTTP Code: {}", responseCode);
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
                    StringBuilder errorResponse = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        errorResponse.append(line);
                    }
                    log.error("Error Response: {}", errorResponse.toString());
                } catch (Exception e) {
                    log.error("Could not read error response");
                }
                throw new RuntimeException("Failed to get access token. HTTP Code: " + responseCode);
            }
        } catch (Exception e) {
            log.error("Error getting access token", e);
            throw new RuntimeException("Error getting access token", e);
        }
    }

    public BigDecimal getCurrentPrice(String stockCode) {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        BigDecimal price = getPriceFromApi(stockCode, today);
        
        if (price == null) {
            log.info("No data for today ({}), trying yesterday...", today);
            String yesterday = LocalDate.now().minusDays(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            price = getPriceFromApi(stockCode, yesterday);
        }
        
        if (price == null) {

             log.info("No data for yesterday, returning Mock price.");
             return getMockPrice();
        }
        
        return price;
    }

    // 특정 날짜 종가 조회
    public BigDecimal getPriceByDate(String stockCode, String date) {
        // date format: YYYY-MM-DD -> YYYYMMDD
        String formattedDate = date.replace("-", "");
        BigDecimal price = getPriceFromApi(stockCode, formattedDate);
        return price != null ? price : getMockPrice();
    }

    private BigDecimal getPriceFromApi(String stockCode, String date) {
        try {
            String token = getAccessToken();
            String host = "https://api.kiwoom.com";
            String endpoint = "/api/dostk/chart";
            String urlString = host + endpoint;
            
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            connection.setRequestProperty("authorization", "Bearer " + token.trim());
            connection.setRequestProperty("cont-yn", "N");
            connection.setRequestProperty("next-key", "");
            connection.setRequestProperty("api-id", "ka10081");
            connection.setDoOutput(true);
            
            String jsonData = String.format(
                "{\"stk_cd\" : \"%s\",\"base_dt\" : \"%s\",\"upd_stkpc_tp\" : \"1\"}",
                stockCode, date
            );
            
            log.info("Calling Kiwoom API (ka10081). URL: {}, Payload: {}", urlString, jsonData);
            
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonData.getBytes("utf-8");
                os.write(input, 0, input.length);
            }
            
            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                try (Scanner scanner = new Scanner(connection.getInputStream(), "utf-8")) {
                    String responseBody = scanner.useDelimiter("\\A").next();
                    log.info("API Response Body: {}", responseBody);
                    
                    Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);

                    Object outputObj = responseMap.get("output");
                    if (outputObj instanceof List) {
                        List<Map<String, Object>> list = (List<Map<String, Object>>) outputObj;
                        if (!list.isEmpty()) {
                            Map<String, Object> firstItem = list.get(0);
                            String priceStr = (String) firstItem.get("stck_clpr");
                            log.info("Price found for {}: {}", stockCode, priceStr);
                            return new BigDecimal(priceStr);
                        }
                    }
                    
                    log.warn("No data found in API response for {} on {}", stockCode, date);
                    return null;
                }
            } else {
                 try (Scanner scanner = new Scanner(connection.getErrorStream(), "utf-8")) {
                    String errorBody = scanner.useDelimiter("\\A").next();
                    log.warn("Failed to get price for {} on {}. HTTP Code: {}. Error Body: {}", stockCode, date, responseCode, errorBody);
                } catch (Exception e) {
                    log.warn("Failed to get price for {} on {}. HTTP Code: {}. Could not read error body.", stockCode, date, responseCode);
                }
                return null;
            }
        } catch (Exception e) {
            log.error("Error calling Kiwoom API", e);
            return null;
        }
    }

    private BigDecimal getMockPrice() {
        double randomValue = 10000 + (Math.random() * 90000);
        return BigDecimal.valueOf(randomValue).setScale(0, java.math.RoundingMode.HALF_UP);
    }
}
