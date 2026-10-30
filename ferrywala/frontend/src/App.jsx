import { lazy, Suspense } from 'react'
import { Route, Routes } from 'react-router-dom'
import { ToastProvider } from './context/Toast'
import { AuthProvider } from './context/Auth'
import { LocationProvider } from './context/Location'
import { CartProvider } from './context/Cart'
import { LanguageProvider } from './i18n'
import Shell from './components/Shell'
import ProtectedRoute from './components/ProtectedRoute'
import ErrorBoundary from './components/ErrorBoundary'
import RouteLoading from './components/RouteLoading'
import Landing from './pages/Landing'

// Everything except the landing page is split out so the first paint stays small.
const Login = lazy(() => import('./pages/Login'))
const Register = lazy(() => import('./pages/Register'))
const VerifyEmail = lazy(() => import('./pages/VerifyEmail'))
const ForgotPassword = lazy(() => import('./pages/ForgotPassword'))
const ResetPassword = lazy(() => import('./pages/ResetPassword'))
const Privacy = lazy(() => import('./pages/Privacy'))
const Account = lazy(() => import('./pages/Account'))
const Vendors = lazy(() => import('./pages/Vendors'))
const VendorProfile = lazy(() => import('./pages/VendorProfile'))
const Cart = lazy(() => import('./pages/Cart'))
const Orders = lazy(() => import('./pages/Orders'))
const VendorDashboard = lazy(() => import('./pages/vendor/Dashboard'))
const VendorRequests = lazy(() => import('./pages/vendor/Requests'))
const VendorProducts = lazy(() => import('./pages/vendor/Products'))
const VendorShop = lazy(() => import('./pages/vendor/Shop'))
const Meet = lazy(() => import('./pages/Meet'))
const Support = lazy(() => import('./pages/Support'))
const Admin = lazy(() => import('./pages/Admin'))
const NotFound = lazy(() => import('./pages/NotFound'))


export default function App() {
  return (
    <LanguageProvider>
    <ErrorBoundary>
      <ToastProvider>
        <AuthProvider>
          <LocationProvider>
            <CartProvider>
              <Suspense fallback={<RouteLoading />}>
                <Routes>
                  <Route element={<Shell />}>
                    <Route index element={<Landing />} />
                    <Route path="login" element={<Login />} />
                    <Route path="register" element={<Register />} />
                    <Route path="verify-email" element={<VerifyEmail />} />
                    <Route path="forgot-password" element={<ForgotPassword />} />
                    <Route path="reset-password" element={<ResetPassword />} />
                    <Route path="privacy" element={<Privacy />} />
                    <Route path="vendors" element={<Vendors />} />
                    <Route path="vendors/:id" element={<VendorProfile />} />
                    <Route path="cart" element={<ProtectedRoute role="CUSTOMER"><Cart /></ProtectedRoute>} />
                    <Route path="orders" element={<ProtectedRoute role="CUSTOMER"><Orders /></ProtectedRoute>} />
                    <Route path="meet/:id" element={<ProtectedRoute><Meet /></ProtectedRoute>} />
                    <Route path="support" element={<ProtectedRoute><Support /></ProtectedRoute>} />
                    <Route path="account" element={<ProtectedRoute><Account /></ProtectedRoute>} />
                    <Route path="vendor" element={<ProtectedRoute role="VENDOR"><VendorDashboard /></ProtectedRoute>} />
                    <Route path="vendor/requests" element={<ProtectedRoute role="VENDOR"><VendorRequests /></ProtectedRoute>} />
                    <Route path="vendor/products" element={<ProtectedRoute role="VENDOR"><VendorProducts /></ProtectedRoute>} />
                    <Route path="vendor/profile" element={<ProtectedRoute role="VENDOR"><VendorShop /></ProtectedRoute>} />
                    <Route path="admin" element={<ProtectedRoute role="ADMIN"><Admin /></ProtectedRoute>} />
                    <Route path="*" element={<NotFound />} />
                  </Route>
                </Routes>
              </Suspense>
            </CartProvider>
          </LocationProvider>
        </AuthProvider>
      </ToastProvider>
    </ErrorBoundary>
    </LanguageProvider>
  )
}
