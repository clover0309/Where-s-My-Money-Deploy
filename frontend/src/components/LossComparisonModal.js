import { useCallback, useEffect, useState } from "react";

function LossComparisonModal({ isOpen, onClose, stocks, materials }) {
	const [totalLoss, setTotalLoss] = useState(0);
	const [comparisonItems, setComparisonItems] = useState([]);

	const generateComparisons = useCallback((loss) => {
		const items = [
			{
				name: "중고 모닝 가솔린 수동",
				unitPrice: 600000,
				emoji: "🚗",
				description: "중고차 시장의 입문용"
			},
			{
				name: "맥북 에어 M3",
				unitPrice: 1500000,
				emoji: "💻",
				description: "애플의 인기 노트북"
			},
			{
				name: "아이폰 16 Pro Max",
				unitPrice: 2000000,
				emoji: "📱",
				description: "최신 플래그십 스마트폰"
			},
			{
				name: "치킨 세트",
				unitPrice: 25000,
				emoji: "🍗",
				description: "황금 올리브 후라이드+콜라"
			},
			{
				name: "스타벅스 아메리카노 톡",
				unitPrice: 4500,
				emoji: "☕",
				description: "일상의 작은 사치"
			},
			{
				name: "CGV 영화 관람권",
				unitPrice: 15000,
				emoji: "🎬",
				description: "주말의 여유"
			},
			{
				name: "삼겹살 2인분",
				unitPrice: 30000,
				emoji: "🥓",
				description: "소주 한잔의 행복"
			}
		];

		const validItems = items
			.map(item => ({
				...item,
				count: Math.floor(loss / item.unitPrice)
			}))
			.filter(item => item.count >= 1)
			.sort((a, b) => b.unitPrice - a.unitPrice);

		setComparisonItems(validItems);
	}, []);

	const calculateTotalLoss = useCallback(() => {
		let loss = 0;

		stocks.forEach((stock) => {
			const avgPrice = Number(stock.userstockAvgprice);
			const currentPrice = Number(stock.currentPrice || avgPrice);
			const quantity = stock.userstockQuantity;
			const profit = (currentPrice - avgPrice) * quantity;
			
			if (profit < 0) {
				loss += Math.abs(profit);
			}
		});

		materials.forEach((material) => {
			const avgPrice = Number(material.usermaterialsAvgprice);
			const currentPrice = Number(material.currentPrice || avgPrice);
			const quantity = material.usermaterialsQuantity;
			const profit = (currentPrice - avgPrice) * quantity;
			
			if (profit < 0) {
				loss += Math.abs(profit);
			}
		});

		setTotalLoss(loss);
		generateComparisons(loss);
	}, [stocks, materials, generateComparisons]);

	useEffect(() => {
		if (isOpen) {
			calculateTotalLoss();
		}
	}, [isOpen, calculateTotalLoss]);

	if (!isOpen) return null;

	return (
		<div
			style={{
				position: "fixed",
				top: 0,
				left: 0,
				right: 0,
				bottom: 0,
				backgroundColor: "rgba(0, 0, 0, 0.5)",
				display: "flex",
				justifyContent: "center",
				alignItems: "center",
				zIndex: 1000
			}}
			onClick={onClose}
		>
			<div
				style={{
					backgroundColor: "white",
					padding: "30px",
					borderRadius: "12px",
					width: "90%",
					maxWidth: "600px",
					maxHeight: "80vh",
					overflowY: "auto",
					boxShadow: "0 4px 20px rgba(0, 0, 0, 0.3)"
				}}
				onClick={(e) => e.stopPropagation()}
			>
				<h2 style={{ 
					marginBottom: "20px", 
					color: "#dc3545",
					fontSize: "28px",
					textAlign: "center",
					borderBottom: "3px solid #dc3545",
					paddingBottom: "15px"
				}}>
					💸 이 돈으로 뭘 할 수 있었을까요?
				</h2>

				{totalLoss === 0 ? (
					<div style={{ 
						textAlign: "center", 
						padding: "40px 20px",
						backgroundColor: "#d4edda",
						borderRadius: "8px",
						border: "2px solid #28a745"
					}}>
						<h3 style={{ color: "#155724", fontSize: "24px", marginBottom: "10px" }}>
							🎉 축하합니다!
						</h3>
						<p style={{ color: "#155724", fontSize: "18px" }}>
							손실이 없습니다! 훌륭한 투자입니다! 💪
						</p>
					</div>
				) : (
					<div>
						<div style={{ 
							textAlign: "center", 
							padding: "20px",
							backgroundColor: "#f8d7da",
							borderRadius: "8px",
							marginBottom: "25px",
							border: "2px solid #dc3545"
						}}>
							<p style={{ 
								fontSize: "18px", 
								color: "#721c24",
								marginBottom: "10px"
							}}>
								현재 총 손실액
							</p>
							<h3 style={{ 
								fontSize: "36px", 
								color: "#dc3545",
								margin: "0",
								fontWeight: "bold"
							}}>
								-{totalLoss.toLocaleString()}원
							</h3>
						</div>

						{comparisonItems.length === 0 ? (
							<p style={{ 
								textAlign: "center", 
								color: "#666",
								fontSize: "16px"
							}}>
								손실액이 너무 작아서 비교할 항목이 없습니다.
							</p>
						) : (
							<div>
								<h3 style={{ 
									fontSize: "20px", 
									marginBottom: "15px",
									color: "#333",
									textAlign: "center"
								}}>
									이 금액이면...
								</h3>
								<div style={{ 
									display: "flex", 
									flexDirection: "column", 
									gap: "15px" 
								}}>
									{comparisonItems.map((item, index) => (
										<div
											key={index}
											style={{
												padding: "20px",
												backgroundColor: "#fff3cd",
												borderRadius: "8px",
												border: "2px solid #ffc107",
												display: "flex",
												alignItems: "center",
												gap: "15px"
											}}
										>
											<div style={{ fontSize: "48px" }}>
												{item.emoji}
											</div>
											<div style={{ flex: 1 }}>
												<h4 style={{ 
													margin: "0 0 5px 0", 
													fontSize: "18px",
													color: "#333"
												}}>
													{item.name}
												</h4>
												<p style={{ 
													margin: "0 0 5px 0", 
													fontSize: "14px",
													color: "#666"
												}}>
													{item.description}
												</p>
												<p style={{ 
													margin: "0", 
													fontSize: "16px",
													color: "#856404",
													fontWeight: "bold"
												}}>
													({item.unitPrice.toLocaleString()}원/개)
												</p>
											</div>
											<div style={{ 
												fontSize: "24px", 
												fontWeight: "bold",
												color: "#dc3545",
												minWidth: "80px",
												textAlign: "right"
											}}>
												{item.count.toLocaleString()}개
											</div>
										</div>
									))}
								</div>
							</div>
						)}

						<div style={{ 
							marginTop: "25px", 
							padding: "15px",
							backgroundColor: "#e7f3ff",
							borderRadius: "8px",
							border: "1px solid #b3d9ff"
						}}>
							<p style={{ 
								margin: "0", 
								fontSize: "14px",
								color: "#004085",
								lineHeight: "1.6"
							}}>
								💡 <strong>참고:</strong> 투자는 장기적인 관점에서 바라보세요. 
								일시적인 손실은 회복될 수 있습니다. 
								하지만 이런 비교를 통해 리스크 관리의 중요성을 되새겨보세요!
							</p>
						</div>
					</div>
				)}

				<div style={{ marginTop: "25px", textAlign: "center" }}>
					<button
						onClick={onClose}
						style={{
							padding: "12px 40px",
							backgroundColor: "#007bff",
							color: "white",
							border: "none",
							borderRadius: "6px",
							cursor: "pointer",
							fontSize: "16px",
							fontWeight: "bold"
						}}
						onMouseOver={(e) => e.target.style.backgroundColor = "#0056b3"}
						onMouseOut={(e) => e.target.style.backgroundColor = "#007bff"}
					>
						닫기
					</button>
				</div>
			</div>
		</div>
	);
}

export default LossComparisonModal;
