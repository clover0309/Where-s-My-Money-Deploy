import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { authAPI } from "../api/authAPI";
import { useAuth } from "../contexts/AuthContext";

function LoginPage() {
    const { login, isLoggedIn } = useAuth();
    const navigate = useNavigate();

    const [id, setId] = useState('');
    const [password, setPassword] = useState('');
    const [isSubmitting, setIsSubmitting] = useState(false);

    useEffect(
        () => {
            if (isLoggedIn)  {
                navigate("/UserStockPage", {replace: true} );
            }
        }, [isLoggedIn, navigate]);
  
    const handleLogin = async (e) => {
        e.preventDefault();

        if(id === '' || password === ''){
            alert("아이디나 비밀번호를 모두 입력해주세요.");
            return;
        }

        setIsSubmitting(true);

        try {
            const credentials = {
                id: id,
                password: password
            };

            const response = await authAPI.login(credentials);


            if (response.success) {
                const { userId, token } = response.data;
                
                alert('로그인 성공!');
                login(userId, token);
            } else {
                alert(response.message || '로그인에 실패했습니다.');
            }
        } catch (error) {
            console.error('로그인 오류:', error);
            alert(error.message || '로그인 중 오류가 발생했습니다.');
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <div>
            <h1>로그인 페이지</h1>
                <form onSubmit={handleLogin}>
                <input type="text" value={id} onChange={(e) => setId(e.target.value)} placeholder="아이디" />
                <br />
                <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} placeholder="비밀번호" />
                <br />
                <button type="submit" disabled={isSubmitting}>
                    {isSubmitting ? '로그인 중...' : '로그인'}
                </button>
                </form>
                <button type="button" onClick={() => navigate("/RegisterPage")}>회원가입</button>
        </div>
    );
}

export default LoginPage;