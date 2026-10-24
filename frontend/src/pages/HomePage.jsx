import React from 'react'
import { Link } from 'react-router-dom'
import { Plane, Shield, CreditCard, Clock, Sparkles, CheckCircle2 } from 'lucide-react'
import { useAuth } from '../context/AuthContext'

export default function HomePage() {
  const { isAuthenticated } = useAuth()

  return (
    <div className="space-y-16 pb-16">
      {/* Hero Section */}
      <div className="relative overflow-hidden bg-gradient-to-br from-blue-600 via-indigo-600 to-blue-800 dark:from-slate-900 dark:via-blue-950 dark:to-slate-900 text-white py-24 px-4 border-b border-blue-500/20 dark:border-slate-800">
        {/* Subtle background glow effect */}
        <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[600px] h-[350px] bg-blue-400/20 dark:bg-blue-500/10 rounded-full blur-3xl pointer-events-none" />

        <div className="relative max-w-4xl mx-auto text-center">
          <div className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-white/15 dark:bg-white/10 backdrop-blur-md text-xs font-medium text-blue-100 mb-6 border border-white/20">
            <Sparkles className="w-3.5 h-3.5 text-amber-300" />
            <span>Smart & Seamless Flight Reservations</span>
          </div>

          <h1 className="text-4xl md:text-6xl font-extrabold tracking-tight mb-6">
            Book Your Next Journey{' '}
            <span className="bg-gradient-to-r from-blue-200 via-sky-100 to-indigo-200 bg-clip-text text-transparent">
              With Ease
            </span>
          </h1>

          <p className="text-blue-100 dark:text-slate-300 text-lg md:text-xl mb-10 max-w-2xl mx-auto font-normal leading-relaxed">
            Search hundreds of direct flights, choose your preferred seat in real-time, and fly with absolute confidence.
          </p>

          <div className="flex flex-col sm:flex-row items-center justify-center gap-4">
            <Link
              to="/flights"
              className="w-full sm:w-auto inline-flex items-center justify-center gap-2.5 bg-white text-blue-700 font-semibold px-8 py-3.5 rounded-xl hover:bg-blue-50 transition-all shadow-lg shadow-black/10 hover:shadow-xl hover:-translate-y-0.5 active:translate-y-0"
            >
              <Plane className="w-5 h-5 -rotate-45" />
              Search Flights Now
            </Link>
            {!isAuthenticated ? (
              <Link
                to="/register"
                className="w-full sm:w-auto inline-flex items-center justify-center gap-2 bg-white/10 hover:bg-white/20 text-white font-medium px-6 py-3.5 rounded-xl backdrop-blur-md border border-white/20 transition-all hover:-translate-y-0.5"
              >
                Create Free Account
              </Link>
            ) : (
              <Link
                to="/bookings"
                className="w-full sm:w-auto inline-flex items-center justify-center gap-2 bg-white/10 hover:bg-white/20 text-white font-medium px-6 py-3.5 rounded-xl backdrop-blur-md border border-white/20 transition-all hover:-translate-y-0.5"
              >
                My Bookings
              </Link>
            )}
          </div>
        </div>
      </div>

      {/* Feature Highlights */}
      <div className="max-w-6xl mx-auto px-4">
        <div className="text-center mb-12">
          <h2 className="text-2xl md:text-3xl font-bold tracking-tight text-slate-900 dark:text-slate-100 mb-3">
            Why Fly With SkyBook?
          </h2>
          <p className="text-slate-500 dark:text-slate-400 max-w-xl mx-auto text-sm">
            Everything you need for a frictionless travel experience from takeoff to landing.
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
          {[
            {
              icon: Shield,
              title: 'Secure & Reliable',
              desc: 'State-of-the-art bank-grade encryption protects every payment and personal detail.',
              gradient: 'from-blue-500 to-cyan-500',
            },
            {
              icon: CreditCard,
              title: 'Flexible Payments',
              desc: 'Seamless checkout with Credit, Debit, and instant UPI payment processing.',
              gradient: 'from-indigo-500 to-purple-500',
            },
            {
              icon: Clock,
              title: 'Instant Seat Locking',
              desc: 'Interactive cabin selection with real-time seat status and immediate booking confirmations.',
              gradient: 'from-amber-500 to-orange-500',
            },
          ].map(({ icon: Icon, title, desc, gradient }) => (
            <div
              key={title}
              className="card text-center hover:shadow-lg dark:hover:shadow-slate-900/50 hover:-translate-y-1 transition-all group"
            >
              <div className="flex justify-center mb-5">
                <div className={`w-14 h-14 rounded-2xl bg-gradient-to-tr ${gradient} flex items-center justify-center text-white shadow-md shadow-blue-500/10 group-hover:scale-110 transition-transform`}>
                  <Icon className="w-7 h-7" />
                </div>
              </div>
              <h3 className="font-bold text-lg text-slate-900 dark:text-slate-100 mb-2">{title}</h3>
              <p className="text-sm text-slate-500 dark:text-slate-400 leading-relaxed">{desc}</p>
            </div>
          ))}
        </div>
      </div>

      {/* Trust & Guarantee Banner */}
      <div className="max-w-6xl mx-auto px-4">
        <div className="card bg-gradient-to-r from-blue-50 via-indigo-50 to-slate-50 dark:from-slate-900 dark:via-blue-950/40 dark:to-slate-900 border-blue-200/60 dark:border-slate-800 p-8 sm:p-10">
          <div className="flex flex-col md:flex-row items-center justify-between gap-6">
            <div className="space-y-2 text-center md:text-left">
              <h3 className="text-xl font-bold text-slate-900 dark:text-slate-100">
                Ready to plan your next trip?
              </h3>
              <p className="text-sm text-slate-600 dark:text-slate-400">
                Join thousands of travelers enjoying fast, hassle-free flight bookings today.
              </p>
              <div className="flex flex-wrap gap-4 pt-2 text-xs text-slate-500 dark:text-slate-400">
                <span className="flex items-center gap-1.5"><CheckCircle2 className="w-4 h-4 text-emerald-500" /> Instant e-tickets</span>
                <span className="flex items-center gap-1.5"><CheckCircle2 className="w-4 h-4 text-emerald-500" /> 24/7 Support</span>
                <span className="flex items-center gap-1.5"><CheckCircle2 className="w-4 h-4 text-emerald-500" /> Transparent pricing</span>
              </div>
            </div>
            <div className="flex items-center gap-3">
              <Link to="/flights" className="btn-primary text-sm px-6 py-3">
                Book a Flight
              </Link>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
