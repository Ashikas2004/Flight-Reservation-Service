import React from 'react'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import { ThemeProvider } from './context/ThemeContext'
import Navbar from './components/common/Navbar'
import FlightStatusNotifications from './components/common/FlightStatusNotifications'
import { ProtectedRoute, AdminRoute } from './components/common/ProtectedRoute'

// Auth pages
import LoginPage from './pages/auth/LoginPage'
import RegisterPage from './pages/auth/RegisterPage'

// Flight pages
import FlightSearchPage from './pages/flights/FlightSearchPage'
import FlightDetailPage from './pages/flights/FlightDetailPage'

// Booking pages
import SeatSelectionPage from './pages/booking/SeatSelectionPage'
import PassengerDetailsPage from './pages/booking/PassengerDetailsPage'
import BookingConfirmPage from './pages/booking/BookingConfirmPage'

// Payment
import PaymentPage from './pages/payment/PaymentPage'

// My Bookings & Refunds
import MyBookingsPage from './pages/bookings/MyBookingsPage'
import BookingDetailPage from './pages/bookings/BookingDetailPage'
import RefundPage from './pages/bookings/RefundPage'

// Admin
import AdminDashboard from './pages/admin/AdminDashboard'
import AdminAirlinesPage from './pages/admin/AdminAirlinesPage'
import AdminAirportsPage from './pages/admin/AdminAirportsPage'
import AdminFlightsPage from './pages/admin/AdminFlightsPage'
import AdminSeatsPage from './pages/admin/AdminSeatsPage'
import AdminAuditPage from './pages/admin/AdminAuditPage'

// Home
import HomePage from './pages/HomePage'

export default function App() {
  return (
    <ThemeProvider>
      <BrowserRouter>
        <AuthProvider>
          <div className="min-h-screen flex flex-col bg-slate-50 dark:bg-slate-950 text-slate-900 dark:text-slate-100 transition-colors duration-200">
            <Navbar />
            <FlightStatusNotifications />
            <main className="flex-1">
              <Routes>
                {/* Public */}
                <Route path="/" element={<HomePage />} />
                <Route path="/login" element={<LoginPage />} />
                <Route path="/register" element={<RegisterPage />} />
                <Route path="/flights" element={<FlightSearchPage />} />
                <Route path="/flights/:id" element={<FlightDetailPage />} />

                {/* Protected passenger */}
                <Route path="/booking/:flightId/seats" element={
                  <ProtectedRoute><SeatSelectionPage /></ProtectedRoute>
                } />
                <Route path="/booking/:flightId/passengers" element={
                  <ProtectedRoute><PassengerDetailsPage /></ProtectedRoute>
                } />
                <Route path="/booking/:bookingId/confirm" element={
                  <ProtectedRoute><BookingConfirmPage /></ProtectedRoute>
                } />
                <Route path="/payment/:bookingId" element={
                  <ProtectedRoute><PaymentPage /></ProtectedRoute>
                } />
                <Route path="/bookings" element={
                  <ProtectedRoute><MyBookingsPage /></ProtectedRoute>
                } />
                <Route path="/bookings/:reference" element={
                  <ProtectedRoute><BookingDetailPage /></ProtectedRoute>
                } />
                <Route path="/bookings/:bookingId/refund" element={
                  <ProtectedRoute><RefundPage /></ProtectedRoute>
                } />

                {/* Admin */}
                <Route path="/admin" element={<AdminRoute><AdminDashboard /></AdminRoute>} />
                <Route path="/admin/airlines" element={<AdminRoute><AdminAirlinesPage /></AdminRoute>} />
                <Route path="/admin/airports" element={<AdminRoute><AdminAirportsPage /></AdminRoute>} />
                <Route path="/admin/flights" element={<AdminRoute><AdminFlightsPage /></AdminRoute>} />
                <Route path="/admin/seats/:flightId" element={<AdminRoute><AdminSeatsPage /></AdminRoute>} />
                <Route path="/admin/audit" element={<AdminRoute><AdminAuditPage /></AdminRoute>} />

                <Route path="*" element={<Navigate to="/" replace />} />
              </Routes>
            </main>

            <footer className="bg-white dark:bg-slate-900 border-t border-slate-200 dark:border-slate-800 py-6 mt-auto transition-colors">
              <div className="max-w-7xl mx-auto px-4 text-center text-sm text-slate-500 dark:text-slate-400">
                © 2026 SkyBook Flight Reservation Service. All rights reserved.
              </div>
            </footer>
          </div>
        </AuthProvider>
      </BrowserRouter>
    </ThemeProvider>
  )
}
