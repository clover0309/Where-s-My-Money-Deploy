package com.byul.wheresmymoney.backend.Config;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
	
	private final JwtTokenProvider jwtTokenProvider;
	
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		
		String path = request.getRequestURI();
		
		if (path.startsWith("/api/auth/") || path.equals("/running")) {
			filterChain.doFilter(request, response);
			return;
		}
		
		String bearerToken = request.getHeader("Authorization");
		String token = jwtTokenProvider.resolveToken(bearerToken);
		
		if (token != null && jwtTokenProvider.validateToken(token)) {
			String userId = jwtTokenProvider.getUserId(token);
			
			UsernamePasswordAuthenticationToken authentication = 
				new UsernamePasswordAuthenticationToken(userId, null, null);
			authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
			
			SecurityContextHolder.getContext().setAuthentication(authentication);
			
			log.info("JWT 인증 성공: userId={}, path={}", userId, path);
		} else {
			System.out.println("  ✗ 토큰 검증 실패 또는 토큰 없음");
		}
		
		filterChain.doFilter(request, response);
	}
}
