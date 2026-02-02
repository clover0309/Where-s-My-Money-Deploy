import axios from "axios";
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../contexts/AuthContext";

function UserProfilePage() {
	const navigate = useNavigate();
	const { user, getToken, logout } = useAuth();
	const [userInfo, setUserInfo] = useState({
		userId: '',
		userName: '',
		email: ''
	});
	const [passwordForm, setPasswordForm] = useState({
		currentPassword: '',
		newPassword: '',
		confirmPassword: ''
	});
	const [passwordErrors, setPasswordErrors] = useState({
		currentPassword: '',
		newPassword: '',
		confirmPassword: ''
	});
	const [deleteAccountPassword, setDeleteAccountPassword] = useState('');
	const [isChangingPassword, setIsChangingPassword] = useState(false);
	const [isCheckingPassword, setIsCheckingPassword] = useState(false);
	const [isDeletingAccount, setIsDeletingAccount] = useState(false);

	// 사용자 정보 불러오기
	useEffect(() => {
		const fetchUserInfo = async () => {
			if (!user) return;

			try {
				const token = getToken();
				const response = await axios.get(`/api/user/info`, {
					params: { userId: user },
					headers: {
						'Authorization': `Bearer ${token}`
					}
				});

				if (response.data.success) {
					setUserInfo({
						userId: response.data.data.userinfo_ID,
						userName: response.data.data.userinfo_Name,
						email: response.data.data.userinfo_Email
					});
				}
			} catch (error) {
				console.error('사용자 정보 조회 실패:', error);
				alert('사용자 정보를 불러오는데 실패했습니다.');
			}
		};

		fetchUserInfo();
	}, [user, getToken]);

	// 기존 비밀번호 확인 (비동기)
	const checkCurrentPassword = async (password) => {
		if (!password) {
			setPasswordErrors(prev => ({ ...prev, currentPassword: '' }));
			return;
		}

		setIsCheckingPassword(true);
		try {
			const token = getToken();
			const response = await axios.post('/api/user/check-password', {
				userId: user,
				password: password
			}, {
				headers: {
					'Authorization': `Bearer ${token}`
				}
			});

			if (response.data.success) {
				setPasswordErrors(prev => ({ ...prev, currentPassword: '' }));
			} else {
				setPasswordErrors(prev => ({ ...prev, currentPassword: '현재 비밀번호가 일치하지 않습니다.' }));
			}
		} catch (error) {
			console.error('비밀번호 확인 실패:', error);
			setPasswordErrors(prev => ({ ...prev, currentPassword: '비밀번호 확인 중 오류가 발생했습니다.' }));
		} finally {
			setIsCheckingPassword(false);
		}
	};

	// 새 비밀번호 유효성 검사 (비동기)
	const validateNewPassword = async (password) => {
		if (!password) {
			setPasswordErrors(prev => ({ ...prev, newPassword: '' }));
			return;
		}

		// 비밀번호 강도 검사
		if (password.length < 8) {
			setPasswordErrors(prev => ({ ...prev, newPassword: '비밀번호는 최소 8자 이상이어야 합니다.' }));
			return;
		}

		if (!/[A-Za-z]/.test(password) || !/[0-9]/.test(password)) {
			setPasswordErrors(prev => ({ ...prev, newPassword: '비밀번호는 영문자와 숫자를 포함해야 합니다.' }));
			return;
		}

		setPasswordErrors(prev => ({ ...prev, newPassword: '' }));
	};

	// 비밀번호 확인 일치 검사 (비동기)
	const validateConfirmPassword = async (confirmPassword) => {
		if (!confirmPassword) {
			setPasswordErrors(prev => ({ ...prev, confirmPassword: '' }));
			return;
		}

		if (passwordForm.newPassword !== confirmPassword) {
			setPasswordErrors(prev => ({ ...prev, confirmPassword: '새 비밀번호가 일치하지 않습니다.' }));
		} else {
			setPasswordErrors(prev => ({ ...prev, confirmPassword: '' }));
		}
	};

	// 입력 필드 변경 핸들러
	const handlePasswordChange = (field, value) => {
		setPasswordForm(prev => ({ ...prev, [field]: value }));

		// 비동기 검증
		if (field === 'currentPassword') {
			const timer = setTimeout(() => checkCurrentPassword(value), 500);
			return () => clearTimeout(timer);
		} else if (field === 'newPassword') {
			validateNewPassword(value);
			if (passwordForm.confirmPassword) {
				validateConfirmPassword(passwordForm.confirmPassword);
			}
		} else if (field === 'confirmPassword') {
			validateConfirmPassword(value);
		}
	};

	// 비밀번호 변경 제출
	const handleSubmitPasswordChange = async (e) => {
		e.preventDefault();

		// 모든 에러 체크
		if (passwordErrors.currentPassword || passwordErrors.newPassword || passwordErrors.confirmPassword) {
			alert('입력 내용을 확인해주세요.');
			return;
		}

		if (!passwordForm.currentPassword || !passwordForm.newPassword || !passwordForm.confirmPassword) {
			alert('모든 항목을 입력해주세요.');
			return;
		}

		setIsChangingPassword(true);
		try {
			const token = getToken();
			const response = await axios.post('/api/user/change-password', {
				userId: user,
				currentPassword: passwordForm.currentPassword,
				newPassword: passwordForm.newPassword
			}, {
				headers: {
					'Authorization': `Bearer ${token}`
				}
			});

			if (response.data.success) {
				alert('비밀번호가 성공적으로 변경되었습니다.');
				setPasswordForm({
					currentPassword: '',
					newPassword: '',
					confirmPassword: ''
				});
				setPasswordErrors({
					currentPassword: '',
					newPassword: '',
					confirmPassword: ''
				});
			} else {
				alert(response.data.message || '비밀번호 변경에 실패했습니다.');
			}
		} catch (error) {
			console.error('비밀번호 변경 실패:', error);
			alert('비밀번호 변경 중 오류가 발생했습니다.');
		} finally {
			setIsChangingPassword(false);
		}
	};

	// 회원탈퇴 처리
	const handleDeleteAccount = async () => {
		if (!deleteAccountPassword) {
			alert('비밀번호를 입력해주세요.');
			return;
		}

		if (!window.confirm('정말로 회원탈퇴를 진행하시겠습니까?\n탈퇴 후에는 계정을 복구할 수 없습니다.')) {
			return;
		}

		setIsDeletingAccount(true);
		try {
			const token = getToken();
			const response = await axios.post('/api/user/delete-account', {
				userId: user,
				password: deleteAccountPassword
			}, {
				headers: {
					'Authorization': `Bearer ${token}`
				}
			});

			if (response.data.success) {
				alert('회원탈퇴가 완료되었습니다.');
				logout();
				navigate('/');
			} else {
				alert(response.data.message || '회원탈퇴에 실패했습니다.');
			}
		} catch (error) {
			console.error('회원탈퇴 실패:', error);
			alert('회원탈퇴 중 오류가 발생했습니다.');
		} finally {
			setIsDeletingAccount(false);
			setDeleteAccountPassword('');
		}
	};

	return (
		<div style={{ padding: '40px', maxWidth: '800px', margin: '0 auto' }}>
			<div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '30px' }}>
				<h1>내 정보 관리</h1>
				<button
					onClick={() => navigate('/UserStockPage')}
					style={{
						padding: '10px 20px',
						backgroundColor: '#6c757d',
						color: 'white',
						border: 'none',
						borderRadius: '4px',
						cursor: 'pointer'
					}}
				>
					돌아가기
				</button>
			</div>

			{/* 기본 정보 섹션 */}
			<div style={{
				backgroundColor: '#f8f9fa',
				padding: '20px',
				borderRadius: '8px',
				marginBottom: '30px'
			}}>
			<h2 style={{ marginBottom: '20px' }}>기본 정보</h2>
			<div style={{ marginBottom: '15px' }}>
				<label style={{ display: 'block', marginBottom: '5px', fontWeight: 'bold' }}>
					아이디
				</label>
				<input
					type="text"
					value={userInfo.userId}
					disabled
					style={{
						width: '100%',
						padding: '10px',
						border: '1px solid #ced4da',
						borderRadius: '4px',
						backgroundColor: '#e9ecef',
						cursor: 'not-allowed'
					}}
				/>
			</div>
			<div style={{ marginBottom: '15px' }}>
				<label style={{ display: 'block', marginBottom: '5px', fontWeight: 'bold' }}>
					이름
				</label>
				<input
					type="text"
					value={userInfo.userName}
					disabled
					style={{
						width: '100%',
						padding: '10px',
						border: '1px solid #ced4da',
						borderRadius: '4px',
						backgroundColor: '#e9ecef',
						cursor: 'not-allowed'
					}}
				/>
			</div>
			<div>
				<label style={{ display: 'block', marginBottom: '5px', fontWeight: 'bold' }}>
					이메일
				</label>
				<input
					type="email"
					value={userInfo.email}
					disabled
					style={{
						width: '100%',
						padding: '10px',
						border: '1px solid #ced4da',
						borderRadius: '4px',
						backgroundColor: '#e9ecef',
						cursor: 'not-allowed'
					}}
				/>
			</div>
		</div>			{/* 비밀번호 변경 섹션 */}
			<div style={{
				backgroundColor: '#fff',
				padding: '20px',
				border: '1px solid #dee2e6',
				borderRadius: '8px'
			}}>
				<h2 style={{ marginBottom: '20px' }}>비밀번호 변경</h2>
				<form onSubmit={handleSubmitPasswordChange}>
					<div style={{ marginBottom: '20px' }}>
						<label style={{ display: 'block', marginBottom: '5px', fontWeight: 'bold' }}>
							현재 비밀번호 *
						</label>
						<input
							type="password"
							value={passwordForm.currentPassword}
							onChange={(e) => handlePasswordChange('currentPassword', e.target.value)}
							style={{
								width: '100%',
								padding: '10px',
								border: `1px solid ${passwordErrors.currentPassword ? '#dc3545' : '#ced4da'}`,
								borderRadius: '4px'
							}}
						/>
						{isCheckingPassword && (
							<small style={{ color: '#6c757d' }}>확인 중...</small>
						)}
						{passwordErrors.currentPassword && (
							<small style={{ color: '#dc3545', display: 'block', marginTop: '5px' }}>
								{passwordErrors.currentPassword}
							</small>
						)}
					</div>

					<div style={{ marginBottom: '20px' }}>
						<label style={{ display: 'block', marginBottom: '5px', fontWeight: 'bold' }}>
							새 비밀번호 *
						</label>
						<input
							type="password"
							value={passwordForm.newPassword}
							onChange={(e) => handlePasswordChange('newPassword', e.target.value)}
							style={{
								width: '100%',
								padding: '10px',
								border: `1px solid ${passwordErrors.newPassword ? '#dc3545' : '#ced4da'}`,
								borderRadius: '4px'
							}}
						/>
						{passwordErrors.newPassword && (
							<small style={{ color: '#dc3545', display: 'block', marginTop: '5px' }}>
								{passwordErrors.newPassword}
							</small>
						)}
						<small style={{ color: '#6c757d', display: 'block', marginTop: '5px' }}>
							8자 이상, 영문자와 숫자 포함
						</small>
					</div>

					<div style={{ marginBottom: '20px' }}>
						<label style={{ display: 'block', marginBottom: '5px', fontWeight: 'bold' }}>
							새 비밀번호 확인 *
						</label>
						<input
							type="password"
							value={passwordForm.confirmPassword}
							onChange={(e) => handlePasswordChange('confirmPassword', e.target.value)}
							style={{
								width: '100%',
								padding: '10px',
								border: `1px solid ${passwordErrors.confirmPassword ? '#dc3545' : '#ced4da'}`,
								borderRadius: '4px'
							}}
						/>
						{passwordErrors.confirmPassword && (
							<small style={{ color: '#dc3545', display: 'block', marginTop: '5px' }}>
								{passwordErrors.confirmPassword}
							</small>
						)}
					</div>

					<button
						type="submit"
						disabled={isChangingPassword || Object.values(passwordErrors).some(err => err !== '')}
						style={{
							width: '100%',
							padding: '12px',
							backgroundColor: isChangingPassword || Object.values(passwordErrors).some(err => err !== '') ? '#6c757d' : '#007bff',
							color: 'white',
							border: 'none',
							borderRadius: '4px',
							cursor: isChangingPassword || Object.values(passwordErrors).some(err => err !== '') ? 'not-allowed' : 'pointer',
							fontSize: '16px',
							fontWeight: 'bold'
						}}
					>
						{isChangingPassword ? '변경 중...' : '비밀번호 변경'}
					</button>
				</form>
			</div>

			{/* 회원탈퇴 섹션 */}
			<div style={{
				backgroundColor: '#fff3cd',
				padding: '20px',
				borderRadius: '8px',
				border: '1px solid #ffc107'
			}}>
				<h2 style={{ marginBottom: '20px', color: '#856404' }}>회원탈퇴</h2>
				<p style={{ marginBottom: '15px', color: '#856404' }}>
					회원탈퇴 시 모든 정보가 삭제되며, 복구할 수 없습니다.
				</p>
				<div style={{ marginBottom: '15px' }}>
					<label style={{ display: 'block', marginBottom: '5px', fontWeight: 'bold', color: '#856404' }}>
						비밀번호 확인
					</label>
					<input
						type="password"
						value={deleteAccountPassword}
						onChange={(e) => setDeleteAccountPassword(e.target.value)}
						placeholder="비밀번호를 입력하세요"
						disabled={isDeletingAccount}
						style={{
							width: '100%',
							padding: '10px',
							border: '1px solid #ffc107',
							borderRadius: '4px'
						}}
					/>
				</div>
				<button
					onClick={handleDeleteAccount}
					disabled={isDeletingAccount || !deleteAccountPassword}
					style={{
						width: '100%',
						padding: '12px',
						backgroundColor: isDeletingAccount || !deleteAccountPassword ? '#6c757d' : '#dc3545',
						color: 'white',
						border: 'none',
						borderRadius: '4px',
						cursor: isDeletingAccount || !deleteAccountPassword ? 'not-allowed' : 'pointer',
						fontSize: '16px',
						fontWeight: 'bold'
					}}
				>
					{isDeletingAccount ? '처리 중...' : '회원탈퇴'}
				</button>
			</div>
		</div>
	);
}

export default UserProfilePage;
