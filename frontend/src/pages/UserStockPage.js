import axios from "axios";
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { authAPI } from "../api/authAPI";
import AddMaterialsModal from "../components/AddMaterialsModal";
import AddStockModal from "../components/AddStockModal";
import MaterialsDetailModal from "../components/MaterialsDetailModal";
import StockDetailModal from "../components/StockDetailModal";
import LossComparisonModal from "../components/LossComparisonModal";
import { useAuth } from "../contexts/AuthContext";

function UserStockPage() {
  const navigate = useNavigate();
  const { logout, user } = useAuth();
  const [apiStatus, setApiStatus] = useState('loading');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isDetailModalOpen, setIsDetailModalOpen] = useState(false);
  const [selectedStock, setSelectedStock] = useState(null);
  const [stocks, setStocks] = useState([]);
  const [isLoadingStocks, setIsLoadingStocks] = useState(false);
  
  const [isMaterialsModalOpen, setIsMaterialsModalOpen] = useState(false);
  const [isMaterialsDetailModalOpen, setIsMaterialsDetailModalOpen] = useState(false);
  const [selectedMaterialType, setSelectedMaterialType] = useState(null);
  const [materials, setMaterials] = useState([]);
  const [isLoadingMaterials, setIsLoadingMaterials] = useState(false);
  
  const [isLossComparisonModalOpen, setIsLossComparisonModalOpen] = useState(false);

    useEffect(() => {
        const testConnection = async () => {
            try {
                const response = await axios.post('/api/stock/test-connection', {}, {
                    headers: {
                        'Content-Type': 'application/json'
                    }
                });

                if (response.data.success) {
                    setApiStatus('success');
                } else {
                    setApiStatus('error');
                }
            } catch (error) {
                setApiStatus('error');
            }
        };

        testConnection();
    }, []);

    useEffect(() => {
        if (user) {
            loadStocks();
            loadMaterials();
        }
    }, [user]);

    const loadStocks = async () => {
        setIsLoadingStocks(true);
        try {
            const response = await axios.get(`/api/stock/list?userId=${user}`);
            

            const groupedStocks = {};
            
            response.data.forEach(function(stock) {
                const stockCode = stock.userstockStk;
                
                if (groupedStocks[stockCode]) {
                    // 이미 존재하는 종목이면 수량과 평균가 합산
                    const existing = groupedStocks[stockCode];
                    const existingValue = Number(existing.userstockAvgprice) * existing.userstockQuantity;
                    const newValue = Number(stock.userstockAvgprice) * stock.userstockQuantity;
                    const totalQuantity = existing.userstockQuantity + stock.userstockQuantity;
                    const newAvgPrice = (existingValue + newValue) / totalQuantity;
                    
                    groupedStocks[stockCode] = {
                        ...existing,
                        userstockQuantity: totalQuantity,
                        userstockAvgprice: newAvgPrice
                    };
                } else {
                    // 새로운 종목이면 추가
                    groupedStocks[stockCode] = { ...stock };
                }
            });
            
            // 객체를 배열로 변환
            const mergedStocks = Object.values(groupedStocks);
            setStocks(mergedStocks);
        } catch (error) {
            console.error('주식 목록 조회 오류:', error);
        } finally {
            setIsLoadingStocks(false);
        }
    };

    const handleLogout = async () => {
        try {
            localStorage.removeItem('token');
            localStorage.removeItem('user');

            logout();

            try {
                if (user) {
                    await authAPI.logout(user);
                } else {
                }
            } catch (error) {
            }
            
            alert('로그아웃되었습니다.');
            navigate("/");
            
        } catch (error) {
            localStorage.removeItem('token');
            localStorage.removeItem('user');
            logout();
            navigate("/");
        }
    };

    const getApiStatusText = () => {
        switch(apiStatus) {
            case 'loading':
                return <span style={{ color: 'gray' }}>키움증권 API 호출중...</span>;
            case 'success':
                return <span style={{ color: 'green' }}>키움증권 API 연결 성공!</span>;
            case 'error':
                return <span style={{ color: 'red' }}>키움증권 API 연결 실패.</span>;
            default:
                return null;
        }
    };

    const handleStockAdded = () => {
        loadStocks();
    };

    const handleDeleteStock = async (stockIdx, stockName) => {
        if (!window.confirm(`'${stockName}' 종목을 삭제하시겠습니까?`)) {
            return;
        }

        try {
            const response = await axios.delete(
                `/api/stock/delete?stockIdx=${stockIdx}&userId=${user}`
            );

            if (response.data.success) {
                alert('주식이 삭제되었습니다.');
                loadStocks();
            } else {
                alert(response.data.message || '주식 삭제에 실패했습니다.');
            }
        } catch (error) {
            alert('주식 삭제 중 오류가 발생했습니다.');
        }
    };

    const handleShowDetail = function(stock) {
        setSelectedStock(stock);
        setIsDetailModalOpen(true);
    };
    
    // 금 목록 조회
    const loadMaterials = async () => {
        setIsLoadingMaterials(true);
        try {
            const response = await axios.get(`/api/materials/list?userId=${user}`);
            
            // 같은 종류끼리 그룹화하여 합산
            const groupedMaterials = {};
            
            response.data.forEach(function(material) {
                const materialType = material.usermaterialsType;
                
                if (groupedMaterials[materialType]) {
                    // 이미 존재하는 종류면 수량과 평균가 합산
                    const existing = groupedMaterials[materialType];
                    const existingValue = Number(existing.usermaterialsAvgprice) * Number(existing.usermaterialsQuantity);
                    const newValue = Number(material.usermaterialsAvgprice) * Number(material.usermaterialsQuantity);
                    const totalQuantity = Number(existing.usermaterialsQuantity) + Number(material.usermaterialsQuantity);
                    const newAvgPrice = (existingValue + newValue) / totalQuantity;
                    
                    groupedMaterials[materialType] = {
                        ...existing,
                        usermaterialsQuantity: totalQuantity,
                        usermaterialsAvgprice: newAvgPrice
                    };
                } else {
                    // 새로운 종류면 추가
                    groupedMaterials[materialType] = { ...material };
                }
            });
            
            // 객체를 배열로 변환
            const mergedMaterials = Object.values(groupedMaterials);
            setMaterials(mergedMaterials);
        } catch (error) {
            console.error('금 목록 조회 오류:', error);
        } finally {
            setIsLoadingMaterials(false);
        }
    };
    
    const handleMaterialsAdded = () => {
        // 금 추가 후 목록 새로고침
        loadMaterials();
    };
    
    const handleDeleteMaterials = async (materialsIdx, materialsName) => {
        if (!window.confirm(`'${materialsName}'을(를) 삭제하시겠습니까?`)) {
            return;
        }

        try {
            const response = await axios.delete(
                `/api/materials/delete?materialsIdx=${materialsIdx}&userId=${user}`
            );

            if (response.data.success) {
                alert('금이 삭제되었습니다.');
                loadMaterials();
            } else {
                alert(response.data.message || '금 삭제에 실패했습니다.');
            }
        } catch (error) {
            console.error('금 삭제 오류:', error);
            alert('금 삭제 중 오류가 발생했습니다.');
        }
    };
    
    const handleShowMaterialsDetail = function(materialType) {
        setSelectedMaterialType(materialType);
        setIsMaterialsDetailModalOpen(true);
    };


    return (
        <div>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
                <div>
                    <h1>로그인 성공</h1>
                    {user && <p>환영합니다, {user}님!</p>}
                </div>
                <div style={{ display: 'flex', gap: '10px' }}>
                    <button 
                        onClick={() => navigate('/UserProfilePage')}
                        style={{
                            padding: '10px 20px',
                            backgroundColor: '#6c757d',
                            color: 'white',
                            border: 'none',
                            borderRadius: '4px',
                            cursor: 'pointer',
                            fontSize: '14px'
                        }}
                    >
                        내 정보 관리
                    </button>
                    <button 
                        onClick={handleLogout}
                        style={{
                            padding: '10px 20px',
                            backgroundColor: '#dc3545',
                            color: 'white',
                            border: 'none',
                            borderRadius: '4px',
                            cursor: 'pointer',
                            fontSize: '14px'
                        }}
                    >
                        로그아웃
                    </button>
                </div>
            </div>

            <hr />

            <div style={{ marginTop: '20px', display: 'flex', gap: '10px', alignItems: 'center' }}>
                <button 
                    onClick={() => setIsModalOpen(true)}
                    style={{
                        padding: '10px 20px',
                        backgroundColor: '#007bff',
                        color: 'white',
                        border: 'none',
                        borderRadius: '4px',
                        cursor: 'pointer',
                        fontSize: '16px',
                        fontWeight: 'bold'
                    }}
                >
                    주식 추가
                </button>
                
                <button 
                    onClick={() => setIsLossComparisonModalOpen(true)}
                    style={{
                        padding: '10px 20px',
                        backgroundColor: '#ffc107',
                        color: 'white',
                        border: 'none',
                        borderRadius: '4px',
                        cursor: 'pointer',
                        fontSize: '16px',
                        fontWeight: 'bold'
                    }}
                    onMouseOver={(e) => e.target.style.backgroundColor = '#e0a800'}
                    onMouseOut={(e) => e.target.style.backgroundColor = '#ffc107'}
                >
                    💸 이 돈으로 뭘 할 수 있었을까?
                </button>
            </div>

            <hr />

            {/* 주식 목록 */}
            <div style={{ marginTop: '30px' }}>
                <h2>보유 주식 목록</h2>
                {isLoadingStocks ? (
                    <p>로딩 중...</p>
                ) : stocks.length === 0 ? (
                    <p style={{ color: '#666' }}>보유 중인 주식이 없습니다.</p>
                ) : (
                    <table style={{ 
                        width: '100%', 
                        borderCollapse: 'collapse',
                        marginTop: '20px'
                    }}>
                        <thead>
                            <tr style={{ backgroundColor: '#f8f9fa' }}>
                                <th style={{ padding: '12px', border: '1px solid #dee2e6', textAlign: 'left' }}>종목코드</th>
                                <th style={{ padding: '12px', border: '1px solid #dee2e6', textAlign: 'left' }}>종목명</th>
                                <th style={{ padding: '12px', border: '1px solid #dee2e6', textAlign: 'right' }}>보유수량</th>
                                <th style={{ padding: '12px', border: '1px solid #dee2e6', textAlign: 'right' }}>평균매수가</th>
                                <th style={{ padding: '12px', border: '1px solid #dee2e6', textAlign: 'right' }}>현재가</th>
                                <th style={{ padding: '12px', border: '1px solid #dee2e6', textAlign: 'right' }}>수익률</th>
                                <th style={{ padding: '12px', border: '1px solid #dee2e6', textAlign: 'right' }}>평가금액</th>
                                <th style={{ padding: '12px', border: '1px solid #dee2e6', textAlign: 'center' }}>관리</th>
                            </tr>
                        </thead>
                        <tbody>
                            {stocks.map((stock) => (
                                <tr key={stock.userstockIdx}>
                                    <td style={{ padding: '12px', border: '1px solid #dee2e6' }}>{stock.userstockStk}</td>
                                    <td style={{ padding: '12px', border: '1px solid #dee2e6' }}>{stock.userstockName}</td>
                                    <td style={{ padding: '12px', border: '1px solid #dee2e6', textAlign: 'right' }}>
                                        {stock.userstockQuantity.toLocaleString()}주
                                    </td>
                                    <td style={{ padding: '12px', border: '1px solid #dee2e6', textAlign: 'right' }}>
                                        {Math.floor(Number(stock.userstockAvgprice)).toLocaleString()}원
                                    </td>
                                    <td style={{ padding: '12px', border: '1px solid #dee2e6', textAlign: 'right' }}>
                                        {stock.currentPrice ? Number(stock.currentPrice).toLocaleString() + '원' : '-'}
                                    </td>
                                    <td style={{ 
                                        padding: '12px', 
                                        border: '1px solid #dee2e6', 
                                        textAlign: 'right',
                                        color: stock.returnRate > 0 ? 'red' : stock.returnRate < 0 ? 'blue' : 'black',
                                        fontWeight: 'bold'
                                    }}>
                                        {stock.returnRate ? stock.returnRate.toFixed(2) + '%' : '-'}
                                    </td>
                                    <td style={{ padding: '12px', border: '1px solid #dee2e6', textAlign: 'right' }}>
                                        {(Number(stock.currentPrice || stock.userstockAvgprice) * stock.userstockQuantity).toLocaleString()}원
                                    </td>
                                    <td style={{ padding: '12px', border: '1px solid #dee2e6', textAlign: 'center' }}>
                                        <button
                                            onClick={() => handleShowDetail(stock)}
                                            style={{
                                                padding: '6px 12px',
                                                backgroundColor: '#007bff',
                                                color: 'white',
                                                border: 'none',
                                                borderRadius: '4px',
                                                cursor: 'pointer',
                                                fontSize: '14px',
                                                marginRight: '8px'
                                            }}
                                            onMouseOver={(e) => e.target.style.backgroundColor = '#0056b3'}
                                            onMouseOut={(e) => e.target.style.backgroundColor = '#007bff'}
                                        >
                                            상세보기
                                        </button>
                                        <button
                                            onClick={() => handleDeleteStock(stock.userstockIdx, stock.userstockName)}
                                            style={{
                                                padding: '6px 12px',
                                                backgroundColor: '#dc3545',
                                                color: 'white',
                                                border: 'none',
                                                borderRadius: '4px',
                                                cursor: 'pointer',
                                                fontSize: '14px'
                                            }}
                                            onMouseOver={(e) => e.target.style.backgroundColor = '#c82333'}
                                            onMouseOut={(e) => e.target.style.backgroundColor = '#dc3545'}
                                        >
                                            삭제
                                        </button>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                )}
            </div>

            <hr />

            {/* 금 추가 버튼 */}
            <div style={{ marginTop: '20px' }}>
                <button 
                    onClick={() => setIsMaterialsModalOpen(true)}
                    style={{
                        padding: '10px 20px',
                        backgroundColor: '#ffc107',
                        color: 'white',
                        border: 'none',
                        borderRadius: '4px',
                        cursor: 'pointer',
                        fontSize: '16px',
                        fontWeight: 'bold'
                    }}
                >
                    금 추가
                </button>
            </div>

            <hr />

            {/* 금 목록 */}
            <div style={{ marginTop: '30px' }}>
                <h2>보유 금 목록</h2>
                {isLoadingMaterials ? (
                    <p>로딩 중...</p>
                ) : materials.length === 0 ? (
                    <p style={{ color: '#666' }}>보유 중인 금이 없습니다.</p>
                ) : (
                    <table style={{ 
                        width: '100%', 
                        borderCollapse: 'collapse',
                        marginTop: '20px'
                    }}>
                        <thead>
                            <tr style={{ backgroundColor: '#fff3cd' }}>
                                <th style={{ padding: '12px', border: '1px solid #dee2e6', textAlign: 'left' }}>종류</th>
                                <th style={{ padding: '12px', border: '1px solid #dee2e6', textAlign: 'left' }}>이름</th>
                                <th style={{ padding: '12px', border: '1px solid #dee2e6', textAlign: 'right' }}>보유그램</th>
                                <th style={{ padding: '12px', border: '1px solid #dee2e6', textAlign: 'right' }}>평균단가(1g)</th>
                                <th style={{ padding: '12px', border: '1px solid #dee2e6', textAlign: 'right' }}>평가금액</th>
                                <th style={{ padding: '12px', border: '1px solid #dee2e6', textAlign: 'center' }}>관리</th>
                            </tr>
                        </thead>
                        <tbody>
                            {materials.map((material) => (
                                <tr key={material.usermaterialsIdx}>
                                    <td style={{ padding: '12px', border: '1px solid #dee2e6' }}>{material.usermaterialsType}</td>
                                    <td style={{ padding: '12px', border: '1px solid #dee2e6' }}>{material.usermaterialsName}</td>
                                    <td style={{ padding: '12px', border: '1px solid #dee2e6', textAlign: 'right' }}>
                                        {Number(material.usermaterialsQuantity).toFixed(4)}g
                                    </td>
                                    <td style={{ padding: '12px', border: '1px solid #dee2e6', textAlign: 'right' }}>
                                        {Math.floor(Number(material.usermaterialsAvgprice)).toLocaleString()}원
                                    </td>
                                    <td style={{ padding: '12px', border: '1px solid #dee2e6', textAlign: 'right' }}>
                                        {Math.floor(Number(material.usermaterialsAvgprice) * Number(material.usermaterialsQuantity)).toLocaleString()}원
                                    </td>
                                    <td style={{ padding: '12px', border: '1px solid #dee2e6', textAlign: 'center' }}>
                                        <button
                                            onClick={() => handleShowMaterialsDetail(material.usermaterialsType)}
                                            style={{
                                                padding: '6px 12px',
                                                backgroundColor: '#ffc107',
                                                color: 'white',
                                                border: 'none',
                                                borderRadius: '4px',
                                                cursor: 'pointer',
                                                fontSize: '14px',
                                                marginRight: '8px'
                                            }}
                                            onMouseOver={(e) => e.target.style.backgroundColor = '#e0a800'}
                                            onMouseOut={(e) => e.target.style.backgroundColor = '#ffc107'}
                                        >
                                            상세보기
                                        </button>
                                        <button
                                            onClick={() => handleDeleteMaterials(material.usermaterialsIdx, material.usermaterialsName)}
                                            style={{
                                                padding: '6px 12px',
                                                backgroundColor: '#dc3545',
                                                color: 'white',
                                                border: 'none',
                                                borderRadius: '4px',
                                                cursor: 'pointer',
                                                fontSize: '14px'
                                            }}
                                            onMouseOver={(e) => e.target.style.backgroundColor = '#c82333'}
                                            onMouseOut={(e) => e.target.style.backgroundColor = '#dc3545'}
                                        >
                                            삭제
                                        </button>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                )}
            </div>

            <AddStockModal 
                isOpen={isModalOpen} 
                onClose={() => setIsModalOpen(false)}
                onSuccess={handleStockAdded}
            />

            <StockDetailModal
                isOpen={isDetailModalOpen}
                onClose={() => setIsDetailModalOpen(false)}
                stock={selectedStock}
            />
            
            <AddMaterialsModal
                isOpen={isMaterialsModalOpen}
                onClose={() => setIsMaterialsModalOpen(false)}
                onSuccess={handleMaterialsAdded}
            />
            
            <MaterialsDetailModal
                isOpen={isMaterialsDetailModalOpen}
                onClose={() => setIsMaterialsDetailModalOpen(false)}
                materialType={selectedMaterialType}
            />
            
            <LossComparisonModal
                isOpen={isLossComparisonModalOpen}
                onClose={() => setIsLossComparisonModalOpen(false)}
                stocks={stocks}
                materials={materials}
            />

            <div style={{ 
                position: 'fixed', 
                bottom: '20px', 
                left: '20px', 
                fontSize: '14px',
                fontWeight: 'bold'
            }}>
                {getApiStatusText()}
            </div>
        </div>
    );
}

export default UserStockPage;