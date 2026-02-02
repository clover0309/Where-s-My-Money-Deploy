package com.byul.wheresmymoney.backend.Config;

import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class JwtTokenProvider {
	
	@Value("${jwt.secret}")
	private String secretKey;
	
	@Value("${jwt.expiration}")
	private long validityInMilliseconds;
	
	/**
	 * JWT 토큰 생성
	 * @param userId 사용자 ID
	 * @return JWT 토큰
	 */
	public String createToken(String userId) {
		Date now = new Date();
		Date validity = new Date(now.getTime() + validityInMilliseconds);
		
		SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes());
		
		String token = Jwts.builder()
			.subject(userId)
			.issuedAt(now)
			.expiration(validity)
			.signWith(key, SignatureAlgorithm.HS256)
			.compact();
		
		log.info("JWT 토큰 생성 완료: userId={}", userId);
		
		return token;
	}
	
	/**
	 * JWT 토큰에서 사용자 ID 추출
	 * @param token JWT 토큰
	 * @return 사용자 ID
	 */
	public String getUserId(String token) {
		SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes());
		
		Claims claims = Jwts.parser()
			.verifyWith(key)
			.build()
			.parseSignedClaims(token)
			.getPayload();
		
		return claims.getSubject();
	}
	
	/**
	 * JWT 토큰 유효성 검증
	 * @param token JWT 토큰
	 * @return 유효 여부
	 */
	public boolean validateToken(String token) {
		try {
			SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes());
			
			Jwts.parser()
				.verifyWith(key)
				.build()
				.parseSignedClaims(token);
			
			return true;
		} catch (Exception e) {
			log.error("JWT 토큰 검증 실패: {}", e.getMessage());
			return false;
		}
	}
	
	/**
	 * HTTP 헤더에서 토큰 추출
	 * @param bearerToken "Bearer {token}" 형식
	 * @return JWT 토큰
	 */
	public String resolveToken(String bearerToken) {
		if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
			return bearerToken.substring(7);
		}
		return null;
	}
}
