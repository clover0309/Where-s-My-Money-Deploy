import { BrowserRouter, Route, Routes } from 'react-router-dom';
import ProtectedRoute from './components/ProtectedRoute';
import { AuthProvider } from './contexts/AuthContext';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import UserProfilePage from './pages/UserProfilePage';
import UserStockPage from './pages/UserStockPage';


function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<LoginPage />} />
          <Route path="/RegisterPage" element={<RegisterPage />} />

          <Route 
            path="/UserStockPage" 
            element={
              <ProtectedRoute>
                <UserStockPage />
              </ProtectedRoute>
            } 
          />

          <Route 
            path="/UserProfilePage" 
            element={
              <ProtectedRoute>
                <UserProfilePage />
              </ProtectedRoute>
            } 
          />

        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}

export default App;
