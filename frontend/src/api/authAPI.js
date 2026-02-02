import axios from 'axios';

const API_BASE_URL = (process.env.REACT_APP_API_BASE_URL || '/api') + '/auth';

const api = axios.create({
    baseURL: API_BASE_URL,
    headers: {
        'Content-Type': 'application/json',
    },
    withCredentials: true,
});


api.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('token');
        if (token) {
            config.headers['Authorization'] = `Bearer ${token}`;

        }
        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);


api.interceptors.response.use(
    (response) => response,
    (error) => {
        if (error.response && error.response.status === 401) {
            console.log('인증 만료 - 로그아웃 처리');
            localStorage.removeItem('token');
            localStorage.removeItem('user');
            window.location.href = '/';
        }
        return Promise.reject(error);
    }
);

export const authAPI = {

    register: async (userData) => {
        try {
            const response = await api.post('/register', userData);
            return response.data;
        } catch (error) {
            throw error.response?.data || { success: false, message: '서버 오류가 발생했습니다.' };
        }
    },

    login: async (credentials) => {
        try {
            const response = await api.post('/login', credentials);
            return response.data;
        } catch (error) {
            throw error.response?.data || { success: false, message: '서버 오류가 발생했습니다.' };
        }
    },

    logout: async (userId) => {
        try {
            const response = await api.post('/logout', null, {
                params: { userId }
            });
            return response.data;
        } catch (error) {
            throw error.response?.data || { success: false, message: '서버 오류가 발생했습니다.' };
        } finally {
            console.log("로그아웃 API 호출 완료.");
        }
    },

    verifyToken: async (token) => {
        try {
            const response = await axios.get('/api/auth/verify', {
                params: { token }
            });
            return response.data;
        } catch (error) {
            throw error.response?.data || { success: false, message: '토큰 검증 실패' };
        }
    },
};

export default api;
