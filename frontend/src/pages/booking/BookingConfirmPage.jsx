import React from 'react'
import { useNavigate, useLocation, useParams } from 'react-router-dom'
import { CheckCircle, Plane, CreditCard, Users, Luggage } from 'lucide-react'
import { format } from 'date-fns'
import StatusBadge from '../../components/common/StatusBadge'
import { SpecificClassFareBenefits } from '../../components/booking/ClassFareBenefits'
import CommonTravelInfo from '../../components/booking/CommonTravelInfo'

export default function BookingConfirmPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const { bookingId } = useParams()
  const booking = location.state?.booking

  if (!booking) {
    navigate('/bookings')
    return null
  }

  return (
    <div className="max-w-2xl mx-auto px-4 py-8">
      {/* Success header */}
      <div className="text-center mb-8">
        <div className="flex justify-center mb-4">
          <div className="w-16 h-16 bg-emerald-100 dark:bg-emerald-950/60 rounded-2xl flex items-center justify-center shadow-md shadow-emerald-500/10">
            <CheckCircle className="w-9 h-9 text-emerald-600 dark:text-emerald-400" />
          </div>
        </div>
        <h1 className="text-3xl font-extrabold text-slate-900 dark:text-slate-100 tracking-tight">Booking Created!</h1>
        <p className="text-slate-500 dark:text-slate-400 mt-1.5 text-sm">Complete your payment to finalize your flight reservation.</p>
        <div className="mt-4 inline-block bg-blue-50 dark:bg-blue-950/50 border border-blue-200/80 dark:border-blue-900/60 px-6 py-3 rounded-2xl shadow-xs">
          <p className="text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider">Booking Reference</p>
          <p className="text-2xl font-black text-blue-600 dark:text-blue-400 tracking-widest mt-0.5">{booking.bookingReference}</p>
        </div>
      </div>

      {/* Flight summary */}
      <div className="card mb-4 border-slate-200 dark:border-slate-800">
        <div className="flex items-center gap-2 mb-4 pb-3 border-b border-slate-100 dark:border-slate-800 text-slate-900 dark:text-slate-100 font-bold text-base">
          <Plane className="w-5 h-5 text-blue-600 dark:text-blue-400" />
          Flight Details
        </div>
        <div className="grid grid-cols-2 sm:grid-cols-5 gap-4 text-sm">
          <div>
            <p className="text-xs text-slate-400 dark:text-slate-500 font-medium">Flight</p>
            <p className="font-bold text-slate-800 dark:text-slate-200 mt-0.5">{booking.flight.flightNumber}</p>
          </div>
          <div>
            <p className="text-xs text-slate-400 dark:text-slate-500 font-medium">Route</p>
            <p className="font-bold text-slate-800 dark:text-slate-200 mt-0.5">
              {booking.flight.originCity || booking.flight.originIata} → {booking.flight.destinationCity || booking.flight.destinationIata}
            </p>
          </div>
          <div>
            <p className="text-xs text-slate-400 dark:text-slate-500 font-medium">Departure</p>
            <p className="font-bold text-slate-800 dark:text-slate-200 mt-0.5">{format(new Date(booking.flight.departureTime), 'dd MMM HH:mm')}</p>
          </div>
          <div>
            <p className="text-xs text-slate-400 dark:text-slate-500 font-medium">Booking Status</p>
            <div className="mt-0.5">
              <StatusBadge status={booking.status} />
            </div>
          </div>
          <div>
            <p className="text-xs text-slate-400 dark:text-slate-500 font-medium">Flight Status</p>
            <div className="mt-0.5">
              <StatusBadge status={booking.flight.status} />
            </div>
          </div>
        </div>
      </div>

      {/* Passengers */}
      <div className="card mb-4 border-slate-200 dark:border-slate-800">
        <div className="flex items-center gap-2 mb-4 pb-3 border-b border-slate-100 dark:border-slate-800 text-slate-900 dark:text-slate-100 font-bold text-base">
          <Users className="w-5 h-5 text-blue-600 dark:text-blue-400" />
          Passengers ({booking.passengers.length})
        </div>
        <div className="space-y-2.5">
          {booking.passengers.map((p) => (
            <div key={p.id} className="flex justify-between items-center text-sm py-2 px-3 rounded-xl bg-slate-50 dark:bg-slate-800/60 border border-slate-100 dark:border-slate-800">
              <span className="font-semibold text-slate-800 dark:text-slate-200">{p.firstName} {p.lastName}</span>
              <span className="text-xs font-semibold text-blue-600 dark:text-blue-400 bg-blue-50 dark:bg-blue-950/60 px-2.5 py-1 rounded-md">
                Seat {p.seatNumber} · {p.seatClass}
              </span>
            </div>
          ))}
        </div>
      </div>

      {/* Specific Class Inclusions & Instructions */}
      {booking.passengers?.length > 0 && (
        <div className="card mb-4 border-slate-200 dark:border-slate-800">
          <div className="flex items-center gap-2 mb-3 pb-3 border-b border-slate-100 dark:border-slate-800 text-slate-900 dark:text-slate-100 font-bold text-base">
            <Luggage className="w-5 h-5 text-blue-600 dark:text-blue-400" />
            Baggage & Cabin Class Inclusions
          </div>
          <SpecificClassFareBenefits
            seatClasses={booking.passengers.map(p => p.seatClass)}
          />
        </div>
      )}

      {/* Common Travel Information (Applies to all passengers) */}
      <CommonTravelInfo className="mb-4" />

      {/* Total */}
      <div className="card mb-6 border-slate-200 dark:border-slate-800 bg-gradient-to-r from-blue-50/50 to-indigo-50/50 dark:from-slate-900 dark:to-slate-900">
        <div className="flex justify-between items-center">
          <span className="font-bold text-slate-700 dark:text-slate-300">Total Amount Due</span>
          <span className="text-3xl font-black text-blue-600 dark:text-blue-400">₹{booking.totalAmount}</span>
        </div>
      </div>

      {/* Actions */}
      <div>
        <button
          onClick={() => navigate(`/payment/${booking.id}`, { state: { booking } })}
          className="btn-primary w-full py-3.5 flex items-center justify-center gap-2 text-base font-semibold shadow-md"
        >
          <CreditCard className="w-5 h-5" />
          Proceed to Payment
        </button>
      </div>
    </div>
  )
}
