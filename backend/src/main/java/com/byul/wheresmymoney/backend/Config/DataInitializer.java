package com.byul.wheresmymoney.backend.Config;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

	private final JdbcTemplate jdbcTemplate;
	private static final int EXPECTED_STOCK_COUNT = 2886;

	@Override
	public void run(String... args) throws Exception {
		checkAndInitializeStockMaster();
	}

	private void checkAndInitializeStockMaster() {
		try {
			// stockmaster 테이블의 데이터 개수 확인
			Long count = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM stockmaster", 
				Long.class
			);

			log.info("=== StockMaster 데이터 확인 ===");
			log.info("현재 데이터 개수: {}", count);

			if (count == null || count < EXPECTED_STOCK_COUNT) {
				log.warn("StockMaster 데이터가 부족합니다. (현재: {}, 필요: {})", count, EXPECTED_STOCK_COUNT);
				log.info("StockMaster 데이터 초기화를 시작합니다...");
				
				initializeStockMasterData();
				
				// 초기화 후 다시 확인
				Long newCount = jdbcTemplate.queryForObject(
					"SELECT COUNT(*) FROM stockmaster", 
					Long.class
				);
				log.info("✓ StockMaster 데이터 초기화 완료! (총 {}개)", newCount);
			} else {
				log.info("✓ StockMaster 데이터가 정상입니다. ({}개)", count);
			}
		} catch (Exception e) {
			log.error("✗ StockMaster 데이터 확인 중 오류 발생", e);
		}
	}

	private void initializeStockMasterData() {
		try {
			log.info("SQL 덤프 파일 검색 중...");
			
			ClassPathResource resource = new ClassPathResource("data/wheresmymoney_stockmaster.sql");
			
			log.info("리소스 존재 여부: {}", resource.exists());
			log.info("리소스 경로: {}", resource.getPath());
			
			if (!resource.exists()) {
				log.error("✗ SQL 덤프 파일을 찾을 수 없습니다: data/wheresmymoney_stockmaster.sql");
				return;
			}

			log.info("SQL 덤프 파일 읽기 시작...");
			
			InputStream inputStream = resource.getInputStream();
			BufferedReader reader = new BufferedReader(
				new InputStreamReader(inputStream, StandardCharsets.UTF_8)
			);

			StringBuilder sqlScript = new StringBuilder();
			String line;
			int lineCount = 0;
			int executedCount = 0;
			int errorCount = 0;

			while ((line = reader.readLine()) != null) {
				lineCount++;
				
				String trimmedLine = line.trim();
				
				if (trimmedLine.isEmpty() || trimmedLine.startsWith("--")) {
					continue;
				}

				if (trimmedLine.startsWith("/*!")) {
					try {
						String cleanedLine = trimmedLine.replaceAll("/\\*![0-9]+ ", "").replace("*/", "");
						if (!cleanedLine.trim().isEmpty()) {
							jdbcTemplate.execute(cleanedLine);
						}
					} catch (Exception e) {
						log.debug("MySQL 특수 명령어 건너뜀: {}", trimmedLine);
					}
					continue;
				}
				
				sqlScript.append(line).append("\n");

				if (trimmedLine.endsWith(";")) {
					String sql = sqlScript.toString().trim();
					
					try {
						if (!sql.isEmpty()) {
							jdbcTemplate.execute(sql);
							executedCount++;

							if (sql.toUpperCase().startsWith("DROP") || 
								sql.toUpperCase().startsWith("CREATE") ||
								sql.toUpperCase().startsWith("LOCK") ||
								sql.toUpperCase().startsWith("UNLOCK") ||
								sql.toUpperCase().startsWith("INSERT")) {
								
								if (executedCount % 500 == 0 || 
									!sql.toUpperCase().startsWith("INSERT")) {
									log.info("  ... SQL 실행: {} ({}번째)", 
										sql.substring(0, Math.min(50, sql.length())), executedCount);
								}
							}
						}
					} catch (Exception e) {
						errorCount++;
						if (errorCount <= 5) {
							log.warn("SQL 실행 중 오류 (라인 {}): {} - {}", 
								lineCount, e.getMessage(), sql.substring(0, Math.min(100, sql.length())));
						}
					}
					
					sqlScript.setLength(0);
				}
			}

			reader.close();
			log.info("✓ 총 {}줄 읽음, {}개의 SQL 문 실행 완료 (오류: {}개)", lineCount, executedCount, errorCount);

		} catch (Exception e) {
			log.error("✗ StockMaster 데이터 초기화 실패: {}", e.getMessage(), e);
		}
	}
}
