import React, { useState } from 'react'
import { Link, useNavigate, useLocation } from 'react-router-dom'
import { Plane, Menu, X, LogOut, Sun, Moon } from 'lucide-react'
import { useAuth } from '../../context/AuthContext'
import { useTheme } from '../../context/ThemeContext'

export default function Navbar() {
  const { user, logout, isAdmin, isAuthenticated } = useAuth()
  const { theme, toggleTheme, isDark } = useTheme()
  const navigate = useNavigate()
  const location = useLocation()
  const [menuOpen, setMenuOpen] = useState(false)

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  const navLink = (to, label) => {
    const isActive = location.pathname.startsWith(to)
    return (
      <Link
        to={to}
        className={`text-sm font-medium px-3 py-1.5 rounded-lg transition-all ${
          isActive
            ? 'text-blue-600 dark:text-blue-400 bg-blue-50 dark:bg-blue-950/60 font-semibold shadow-xs'
            : 'text-slate-600 dark:text-slate-300 hover:text-slate-900 dark:hover:text-white hover:bg-slate-100 dark:hover:bg-slate-800/60'
        }`}
      >
        {label}
      </Link>
    )
  }

  return (
    <nav className="bg-white/90 dark:bg-slate-900/90 backdrop-blur-md border-b border-slate-200/80 dark:border-slate-800/90 sticky top-0 z-40 transition-colors duration-200">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          {/* Logo */}
          <Link to="/" className="flex items-center gap-2.5 font-bold text-xl group">
            <div className="w-9 h-9 rounded-xl bg-gradient-to-tr from-blue-600 to-indigo-600 flex items-center justify-center text-white shadow-md shadow-blue-500/20 group-hover:scale-105 transition-transform">
              <Plane className="w-5 h-5 -rotate-45" />
            </div>
            <span className="bg-gradient-to-r from-blue-600 to-indigo-600 bg-clip-text text-transparent dark:from-blue-400 dark:to-indigo-300 font-extrabold tracking-tight">
              SkyBook
            </span>
          </Link>

          {/* Desktop nav — different links for admin vs passenger */}
          <div className="hidden md:flex items-center gap-2">
            {isAdmin ? (
              navLink('/admin', 'Admin Dashboard')
            ) : (
              <>
                {navLink('/flights', 'Flights')}
                {isAuthenticated && navLink('/bookings', 'My Bookings')}
              </>
            )}
          </div>

          {/* Desktop right controls: theme toggle + user info */}
          <div className="hidden md:flex items-center gap-3">
            {/* Theme Toggle Button */}
            <button
              onClick={toggleTheme}
              className="p-2 rounded-xl text-slate-500 hover:text-slate-900 dark:text-slate-400 dark:hover:text-slate-100 hover:bg-slate-100 dark:hover:bg-slate-800 border border-transparent hover:border-slate-200 dark:hover:border-slate-700 transition-all cursor-pointer"
              aria-label="Toggle theme"
              title={isDark ? 'Switch to Light Mode' : 'Switch to Dark Mode'}
            >
              {isDark ? (
                <Sun className="w-5 h-5 text-amber-400 hover:rotate-45 transition-transform" />
              ) : (
                <Moon className="w-5 h-5 text-slate-600 hover:-rotate-12 transition-transform" />
              )}
            </button>

            {isAuthenticated ? (
              <div className="flex items-center gap-3 pl-2 border-l border-slate-200 dark:border-slate-800">
                <div className="flex items-center gap-2">
                  <div className="w-8 h-8 rounded-full bg-gradient-to-br from-blue-500 to-indigo-600 text-white font-semibold text-xs flex items-center justify-center shadow-xs">
                    {user?.firstName?.[0] || 'U'}
                  </div>
                  <span className="text-sm font-medium text-slate-700 dark:text-slate-200">
                    {user?.firstName} {user?.lastName}
                  </span>
                </div>

                {isAdmin && (
                  <span className="badge bg-purple-100 text-purple-700 dark:bg-purple-950/60 dark:text-purple-300 border border-purple-200/50 dark:border-purple-800/50">
                    Admin
                  </span>
                )}
                <button
                  onClick={handleLogout}
                  className="flex items-center gap-1.5 text-xs font-medium text-slate-500 hover:text-rose-600 dark:text-slate-400 dark:hover:text-rose-400 px-2.5 py-1.5 rounded-lg hover:bg-rose-50 dark:hover:bg-rose-950/40 transition-colors cursor-pointer"
                >
                  <LogOut className="w-3.5 h-3.5" />
                  Logout
                </button>
              </div>
            ) : (
              <div className="flex items-center gap-2 pl-2 border-l border-slate-200 dark:border-slate-800">
                <Link to="/login" className="btn-secondary text-xs py-2 px-3.5">Log in</Link>
                <Link to="/register" className="btn-primary text-xs py-2 px-3.5">Sign up</Link>
              </div>
            )}
          </div>

          {/* Mobile right items: theme toggle + hamburger */}
          <div className="flex items-center gap-1 md:hidden">
            <button
              onClick={toggleTheme}
              className="p-2 rounded-xl text-slate-500 hover:text-slate-900 dark:text-slate-400 dark:hover:text-slate-100"
              aria-label="Toggle theme"
            >
              {isDark ? (
                <Sun className="w-5 h-5 text-amber-400" />
              ) : (
                <Moon className="w-5 h-5 text-slate-600" />
              )}
            </button>
            <button
              className="p-2 rounded-xl hover:bg-slate-100 dark:hover:bg-slate-800 text-slate-600 dark:text-slate-300"
              onClick={() => setMenuOpen(!menuOpen)}
              aria-label="Open menu"
            >
              {menuOpen ? <X className="w-5 h-5" /> : <Menu className="w-5 h-5" />}
            </button>
          </div>
        </div>
      </div>

      {/* Mobile menu */}
      {menuOpen && (
        <div className="md:hidden border-t border-slate-200 dark:border-slate-800 bg-white/95 dark:bg-slate-900/95 backdrop-blur-md px-4 py-4 space-y-3">
          {isAdmin ? (
            <Link
              to="/admin"
              className="block text-sm font-medium text-slate-700 dark:text-slate-200 hover:text-blue-600"
              onClick={() => setMenuOpen(false)}
            >
              Admin Dashboard
            </Link>
          ) : (
            <>
              <Link
                to="/flights"
                className="block text-sm font-medium text-slate-700 dark:text-slate-200 hover:text-blue-600"
                onClick={() => setMenuOpen(false)}
              >
                Flights
              </Link>
              {isAuthenticated && (
                <Link
                  to="/bookings"
                  className="block text-sm font-medium text-slate-700 dark:text-slate-200 hover:text-blue-600"
                  onClick={() => setMenuOpen(false)}
                >
                  My Bookings
                </Link>
              )}
            </>
          )}

          {isAuthenticated ? (
            <div className="pt-3 border-t border-slate-200 dark:border-slate-800">
              <div className="text-xs text-slate-500 dark:text-slate-400 mb-2">
                Signed in as <span className="font-semibold text-slate-800 dark:text-slate-200">{user?.firstName} {user?.lastName}</span>
              </div>
              <button
                onClick={handleLogout}
                className="flex items-center gap-2 text-sm text-rose-600 dark:text-rose-400 w-full font-medium"
              >
                <LogOut className="w-4 h-4" />
                Logout
              </button>
            </div>
          ) : (
            <div className="flex gap-2 pt-2">
              <Link to="/login" className="btn-secondary text-sm py-2 flex-1 text-center" onClick={() => setMenuOpen(false)}>Log in</Link>
              <Link to="/register" className="btn-primary text-sm py-2 flex-1 text-center" onClick={() => setMenuOpen(false)}>Sign up</Link>
            </div>
          )}
        </div>
      )}
    </nav>
  )
}
