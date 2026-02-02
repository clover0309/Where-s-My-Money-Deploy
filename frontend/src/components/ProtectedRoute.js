import { Navigate } from "react-router-dom";
import { useAuth } from "../contexts/AuthContext";

function ProviderdRoute({ children }) {
  const { isLoggedIn } = useAuth();

  if (!isLoggedIn) {
    return <Navigate to="/" replace />;
  }

  return children;
}

export default ProviderdRoute;