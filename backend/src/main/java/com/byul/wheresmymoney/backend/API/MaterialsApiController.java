package com.byul.wheresmymoney.backend.API;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.byul.wheresmymoney.backend.Materials.Dto.MaterialsDTO.AddMaterialsRequest;
import com.byul.wheresmymoney.backend.Materials.Dto.MaterialsDTO.MaterialsDetailDTO;
import com.byul.wheresmymoney.backend.Materials.Dto.MaterialsDTO.UserMaterialsDTO;
import com.byul.wheresmymoney.backend.Materials.Service.UserMaterialsService;
import com.byul.wheresmymoney.backend.User.Dto.ApiResponse;
import com.byul.wheresmymoney.backend.User.Service.UserActivityLogService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/materials")
@RequiredArgsConstructor
@Slf4j
public class MaterialsApiController {
	
	private final UserMaterialsService userMaterialsService;
	private final UserActivityLogService userActivityLogService;
	

	@PostMapping("/add")
	public ResponseEntity<ApiResponse> addMaterials(@RequestBody AddMaterialsRequest request) {
		try {

			if (request == null) {
				System.out.println("!!! 요청 객체가 null입니다 !!!");
				return ResponseEntity.ok(new ApiResponse(false, "요청 데이터가 없습니다."));
			}
			
			log.info("금 추가 요청: userId={}, type={}, quantity={}", 
				request.getUserId(), request.getMaterialType(), request.getQuantity());

			
			boolean success = userMaterialsService.addMaterials(request);
			
			
			if (success) {
				userActivityLogService.logMaterialAdd(request.getUserId());
				
				return ResponseEntity.ok(new ApiResponse(true, "금이 추가되었습니다."));
			} else {
				log.error("금 추가 실패: userId={}, type={}", request.getUserId(), request.getMaterialType());
				return ResponseEntity.ok(new ApiResponse(false, "금 추가에 실패했습니다."));
			}
		} catch (Exception e) {
			log.error("금 추가 중 예외 발생: userId={}, error={}", request.getUserId(), e.getMessage(), e);
			e.printStackTrace();
			return ResponseEntity.ok(new ApiResponse(false, "서버 오류가 발생했습니다: " + e.getMessage()));
		}
	}
	

	@GetMapping("/list")
	public ResponseEntity<List<UserMaterialsDTO>> getUserMaterials(@RequestParam String userId) {
		if (userId == null || userId.trim().isEmpty()) {
			return ResponseEntity.badRequest().build();
		}
		
		List<UserMaterialsDTO> materials = userMaterialsService.getUserMaterials(userId);
		return ResponseEntity.ok(materials);
	}
	

	@DeleteMapping("/delete")
	public ResponseEntity<ApiResponse> deleteMaterials(
		@RequestParam Integer materialsIdx, 
		@RequestParam String userId
	) {
		if (materialsIdx == null || userId == null || userId.trim().isEmpty()) {
			return ResponseEntity.ok(new ApiResponse(false, "잘못된 요청입니다."));
		}
		
		boolean success = userMaterialsService.deleteMaterials(materialsIdx, userId);
		
		if (success) {
			return ResponseEntity.ok(new ApiResponse(true, "금이 삭제되었습니다."));
		} else {
			return ResponseEntity.ok(new ApiResponse(false, "금 삭제에 실패했습니다."));
		}
	}
	

	@GetMapping("/detail")
	public ResponseEntity<ApiResponse> getMaterialsDetail(
		@RequestParam String userId, 
		@RequestParam String materialType
	) {
		log.info("금 상세 정보 요청: userId={}, materialType={}", userId, materialType);
		
		if (userId == null || userId.trim().isEmpty() || materialType == null || materialType.trim().isEmpty()) {
			return ResponseEntity.ok(new ApiResponse(false, "잘못된 요청입니다."));
		}
		
		try {
			List<MaterialsDetailDTO> detailList = userMaterialsService.getMaterialsDetailList(userId, materialType);
			
			if (detailList == null || detailList.isEmpty()) {
				return ResponseEntity.ok(new ApiResponse(false, "금 상세 정보를 찾을 수 없습니다."));
			}
			
			return ResponseEntity.ok(new ApiResponse(true, "조회 성공", detailList));
		} catch (Exception e) {
			log.error("금 상세 정보 조회 실패: userId={}, materialType={}", userId, materialType, e);
			return ResponseEntity.ok(new ApiResponse(false, "상세 정보 조회 중 오류가 발생했습니다."));
		}
	}
}
