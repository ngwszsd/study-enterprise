import { Navigate, Route, Routes } from 'react-router-dom'
import ProtectedRoute from './components/ProtectedRoute'
import ArticleDetailPage from './pages/ArticleDetailPage'
import ArticleEditPage from './pages/ArticleEditPage'
import ArticleListPage from './pages/ArticleListPage'
import LoginPage from './pages/LoginPage'
import RealtimePage from './pages/RealtimePage'
import RegisterPage from './pages/RegisterPage'

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route element={<ProtectedRoute />}>
        <Route path="/articles" element={<ArticleListPage />} />
        <Route path="/articles/new" element={<ArticleEditPage />} />
        <Route path="/articles/:id" element={<ArticleDetailPage />} />
        <Route path="/articles/:id/edit" element={<ArticleEditPage />} />
        <Route path="/realtime" element={<RealtimePage />} />
      </Route>
      <Route path="*" element={<Navigate to="/articles" replace />} />
    </Routes>
  )
}
