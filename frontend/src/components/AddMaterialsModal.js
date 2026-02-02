import axios from "axios";
import { useState } from "react";
import { useAuth } from "../contexts/AuthContext";

function AddMaterialsModal({ isOpen, onClose, onSuccess }) {
	const { user } = useAuth();
	const [materialType] = useState('gold');
	const [materialName] = useState('금 (Gold)');
	const [quantity, setQuantity] = useState('');
	const [averagePrice, setAveragePrice] = useState('');
	const [purchaseDate, setPurchaseDate] = useState('');
	const [useAutoPrice, setUseAutoPrice] = useState(false);
	const [isSubmitting, setIsSubmitting] = useState(false);

	const handleSubmit = async function() {
		if (!quantity || parseFloat(quantity) <= 0) {
			console.error('검증 실패: 수량이 없거나 0 이하');
			alert('보유 그램을 입력해주세요.');
			return;
		}


		if (!useAutoPrice) {
			if (!averagePrice || parseFloat(averagePrice) <= 0) {
				console.error('검증 실패: 평균 매수가가 없거나 0 이하');
				alert('1g당 평균 매수가를 입력해주세요.');
				return;
			}
		}

		console.log('검증 통과 - 요청 데이터 생성 시작');
		setIsSubmitting(true);
		
		try {
			const requestData = {
				userId: user,
				materialType: materialType,
				materialName: materialName,
				quantity: parseFloat(quantity),
				purchaseDate: purchaseDate || null
			};

			// 자동 가격 조회 사용 시 null, 아니면 수동 입력 가격 전송
			if (useAutoPrice) {
				requestData.averagePrice = null;
			} else {
				requestData.averagePrice = parseFloat(averagePrice);
			}

			const response = await axios.post('/api/materials/add', requestData, {
				headers: {
					'Content-Type': 'application/json'
				}
			});


			if (response.data.success) {
				alert('금이 추가되었습니다.');
				resetForm();
				onSuccess();
				onClose();
			} else {
				const errorMsg = response.data.message || '금 추가에 실패했습니다.';
				
				if (useAutoPrice) {
					alert(errorMsg + '\n\n자동 가격 조회에 실패했습니다.\n체크박스를 해제하고 1g당 평균 매수가를 직접 입력해주세요.');
				} else {
					alert(errorMsg);
				}
			}
		} catch (error) {
			
			let errorMessage = '금 추가에 실패했습니다.';
			
			if (error.response?.data?.message) {
				errorMessage = error.response.data.message;
			} else if (error.response?.status === 400) {
				errorMessage = '잘못된 요청입니다. 입력 값을 확인해주세요.';
			} else if (error.response?.status === 500) {
				errorMessage = '서버 오류가 발생했습니다.';
			} else if (error.message === 'Network Error') {
				errorMessage = '서버에 연결할 수 없습니다. 백엔드가 실행 중인지 확인해주세요.';
			}
			
			if (useAutoPrice) {
				errorMessage += '\n\n자동 가격 조회가 실패했습니다.\n\n체크박스를 해제하고 1g당 평균 매수가를 직접 입력해주세요.';
			}
			alert(errorMessage);
		} finally {
			setIsSubmitting(false);
		}
	};

	const resetForm = function() {
		setQuantity('');
		setAveragePrice('');
		setPurchaseDate('');
		setUseAutoPrice(false);
	};

	const handleClose = function() {
		resetForm();
		onClose();
	};

	if (!isOpen) return null;

	return (
		<div style={{
			position: 'fixed',
			top: 0,
			left: 0,
			right: 0,
			bottom: 0,
			backgroundColor: 'rgba(0, 0, 0, 0.5)',
			display: 'flex',
			justifyContent: 'center',
			alignItems: 'center',
			zIndex: 1000
		}}>
			<div style={{
				backgroundColor: 'white',
				padding: '30px',
				borderRadius: '8px',
				width: '500px',
				maxHeight: '80vh',
				overflow: 'auto'
			}}>
				<h2>금 추가</h2>

				{/* 종류 표시 */}
				<div style={{
					marginBottom: '20px',
					padding: '10px',
					backgroundColor: '#fff3cd',
					borderRadius: '4px',
					border: '1px solid #ffc107'
				}}>
					<strong>종류:</strong> {materialName}
				</div>

				{/* 보유 그램 입력 */}
				<div style={{ marginBottom: '20px' }}>
					<label style={{ display: 'block', marginBottom: '5px', fontWeight: 'bold' }}>
						보유 그램 (g)
					</label>
					<input
						type="number"
						value={quantity}
						onChange={(e) => setQuantity(e.target.value)}
						placeholder="보유 그램 입력 (예: 37.5)"
						min="0"
						step="0.0001"
						style={{ width: '100%', padding: '8px', border: '1px solid #ccc', borderRadius: '4px' }}
					/>
				</div>

				{/* 평균 매수가 입력 */}
				<div style={{ marginBottom: '20px' }}>
					<div style={{ marginBottom: '10px' }}>
						<label style={{ display: 'flex', alignItems: 'center', cursor: 'pointer' }}>
							<input
								type="checkbox"
								checked={useAutoPrice}
								onChange={(e) => {
									setUseAutoPrice(e.target.checked);
									if (e.target.checked) {
										setAveragePrice('');
									}
								}}
								style={{ marginRight: '8px' }}
							/>
							<span style={{ fontWeight: 'bold' }}>현재 시세로 자동 입력</span>
						</label>
					</div>
					
					{useAutoPrice ? (
						<div>
							<div style={{
								padding: '15px',
								backgroundColor: '#e7f3ff',
								borderRadius: '4px',
								border: '1px solid #007bff'
							}}>
								<p style={{ margin: '0 0 10px 0', fontWeight: 'bold', color: '#007bff' }}>
									✓ 현재 금 시세를 자동으로 적용합니다
								</p>
								<small style={{ color: '#666', display: 'block' }}>
									* 매수 날짜는 오늘 날짜로 자동 저장됩니다
								</small>
							</div>
						</div>
					) : (
						<div>
							<label style={{ display: 'block', marginBottom: '5px', fontWeight: 'bold' }}>
								1g당 평균 매수가 (원)
							</label>
							<input
								type="number"
								value={averagePrice}
								onChange={(e) => setAveragePrice(e.target.value)}
								placeholder="1g당 평균 매수가 입력 (예: 85000)"
								min="0"
								step="0.01"
								style={{ width: '100%', padding: '8px', border: '1px solid #ccc', borderRadius: '4px' }}
							/>
							<small style={{ color: '#666', display: 'block', marginTop: '5px' }}>
								* 매수 날짜는 오늘 날짜로 자동 저장됩니다
							</small>
						</div>
					)}
				</div>

				{/* 버튼 */}
				<div style={{ display: 'flex', gap: '10px', justifyContent: 'flex-end' }}>
					<button
						onClick={handleClose}
						style={{
							padding: '10px 20px',
							backgroundColor: '#6c757d',
							color: 'white',
							border: 'none',
							borderRadius: '4px',
							cursor: 'pointer'
						}}
					>
						취소
					</button>
					<button
						onClick={handleSubmit}
						disabled={isSubmitting}
						style={{
							padding: '10px 20px',
							backgroundColor: isSubmitting ? '#6c757d' : '#28a745',
							color: 'white',
							border: 'none',
							borderRadius: '4px',
							cursor: isSubmitting ? 'not-allowed' : 'pointer',
							opacity: isSubmitting ? 0.7 : 1
						}}
					>
						{isSubmitting ? '추가 중...' : '추가'}
					</button>
				</div>
			</div>
		</div>
	);
}

export default AddMaterialsModal;
