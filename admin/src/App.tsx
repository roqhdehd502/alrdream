import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { AuthProvider } from "./auth/AuthContext";
import { Layout } from "./components/Layout";
import { ProtectedRoute } from "./components/ProtectedRoute";
import { LoginPage } from "./pages/LoginPage";
import { DashboardPage } from "./pages/DashboardPage";
import { SurveyDefinitionsPage } from "./pages/SurveyDefinitionsPage";
import { PromptTemplatesPage } from "./pages/PromptTemplatesPage";
import { UsersPage } from "./pages/UsersPage";
import { UserDetailPage } from "./pages/UserDetailPage";

function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route
            element={
              <ProtectedRoute>
                <Layout />
              </ProtectedRoute>
            }
          >
            <Route path="/dashboard" element={<DashboardPage />} />
            <Route path="/surveys" element={<SurveyDefinitionsPage />} />
            <Route path="/prompt-templates" element={<PromptTemplatesPage />} />
            <Route path="/users" element={<UsersPage />} />
            <Route path="/users/:userId" element={<UserDetailPage />} />
          </Route>
          <Route path="/" element={<Navigate to="/dashboard" replace />} />
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}

export default App;
