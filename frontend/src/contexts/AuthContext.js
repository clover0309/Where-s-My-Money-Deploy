import axios from 'axios';
import { createContext, useContext, useEffect, useState } from 'react';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [user, setUser] = useState(null);
  const [isLoading, setIsLoading] = useState(true);


  useEffect(() => {
    const verifyToken = async () => {
      const token = localStorage.getItem('token');
      const savedUser = localStorage.getItem('user');
      
      if (token && savedUser) {
        try {
          const response = await axios.get('/api/auth/verify', {
            params: { token }
          });
          
          if (response.data.success) {
            setUser(savedUser);
            setIsLoggedIn(true);
          } else {
            localStorage.removeItem('token');
            localStorage.removeItem('user');
            setIsLoggedIn(false);
            setUser(null);
          }
        } catch (error) {
          localStorage.removeItem('token');
          localStorage.removeItem('user');
          setIsLoggedIn(false);
          setUser(null);
        }
      } else {
        setIsLoggedIn(false);
        setUser(null);
      }
      
      setIsLoading(false);
    };
    
    verifyToken();
  }, []);

  const login = (userData, token) => {
    setIsLoggedIn(true);
    setUser(userData);

    localStorage.setItem('token', token);
    localStorage.setItem('user', userData);
  };

  const logout = () => {
    
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    
    setIsLoggedIn(false);
    setUser(null);
    
  };

  const getToken = () => {
    return localStorage.getItem('token');
  };


  if (isLoading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
        <p>로딩 중...</p>
      </div>
    );
  }

  return (
    <AuthContext.Provider value={{ isLoggedIn, user, login, logout, getToken }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
    return useContext(AuthContext);
}