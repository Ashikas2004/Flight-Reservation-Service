import React, { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { Plane, Users, CreditCard, ArrowLeft, AlertTriangle, CheckCircle, Luggage } from 'lucide-react'
import { SpecificClassFareBenefits } from '../../components/booking/ClassFareBenefits'
import CommonTravelInfo from '../../components/booking/CommonTravelInfo'
import { bookingApi } from '../../api/bookingApi'
import { FullPageSpinner } from '../../components/common/Spinner'
import Alert from '../../components/common/Alert'
import StatusBadge from '../../components/common/StatusBadge'
import { format } from 'date-fns'

export default function BookingDetailPage() {
  const { reference } = useParams()
  const navigate = useNavigate()
  const [booking, setBooking] = useState(null)
  const [loading, setLoading] = useState(true)
  const [cancelling, setCancelling] = useState(false)
  const [error, setError] = useState('')
  const [showCancelConfirm, setShowCancelConfirm] = useState(false)
  const [cancelReason, setCancelReason] = useState('')

  useEffect(() => {
    let active = true
    const refreshBooking = () => bookingApi.getByReference(reference)
      .then(r => {
        if (!active) return
        if (r.data.status === 'PENDING') {
          // Booking has not been paid yet; redirect user to complete payment
          navigate(`/payment/${r.data.id}`, { replace: true, state: { booking: r.data } })
          return
        }
        setBooking(r.data)
      })
      .catch(() => {
        if (active) setError('Booking not found')
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    refreshBooking()
    const refreshInterval = setInterval(refreshBooking, 60_000)
    return () => {
      active = false
      clearInterval(refreshInterval)
    }
  }, [reference, navigate])

  const handleCancel = async () => {
    setCancelling(true)
    try {
      const res = await bookingApi.cancel(reference, { reason: cancelReason })
      setBooking(res.data)
      setShowCancelConfirm(false)
    } catch (err) {
      setError(err.response?.data?.message ?? 'Cancellation failed')
    } finally {
      setCancelling(false)
    }
  }

  if (loading) return <FullPageSpinner />
  const flightCompleted = booking && (
    booking.flight.status === 'COMPLETED' ||
    new Date(booking.flight.arrivalTime) <= new Date()
  )
  const flightDeparted = booking && new Date(booking.flight.departureTime) <= new Date()

  return (
    <div className="max-w-3xl mx-auto px-4 py-8">
      <button
        onClick={() => navigate('/bookings')}
        className="inline-flex items-center gap-1.5 text-blue-600 dark:text-blue-400 text-sm mb-6 hover:underline font-medium cursor-pointer"
      >
        <ArrowLeft className="w-4 h-4" /> Back to bookings
      </button>

      {error && <Alert type="error" message={error} onClose={() => setError('')} className="mb-4" />}

      {booking && (
        <>
          {/* Header */}
          <div className="card mb-4 border-slate-200 dark:border-slate-800">
            <div className="flex justify-between items-start mb-2">
              <div>
                <p className="text-xs text-slate-400 dark:text-slate-500 font-semibold uppercase tracking-wider mb-1">Booking Reference</p>
                <p className="text-2xl font-black text-blue-600 dark:text-blue-400 tracking-wider">{booking.bookingReference}</p>
              </div>
              <StatusBadge status={flightCompleted ? 'COMPLETED' : booking.status} />
            </div>
            <div className="flex flex-wrap gap-4 text-xs font-medium text-slate-500 dark:text-slate-400 mt-3 pt-3 border-t border-slate-100 dark:border-slate-800">
              {booking.bookedAt && <span>Booked: {format(new Date(booking.bookedAt), 'dd MMM yyyy HH:mm')}</span>}
              {booking.cancelledAt && <span className="text-rose-500">Cancelled: {format(new Date(booking.cancelledAt), 'dd MMM yyyy HH:mm')}</span>}
            </div>
          </div>

          {/* Flight */}
          <div className="card mb-4 border-slate-200 dark:border-slate-800">
            <div className="flex items-center gap-2 mb-4 pb-3 border-b border-slate-100 dark:border-slate-800 font-bold text-slate-900 dark:text-slate-100">
              <Plane className="w-5 h-5 text-blue-600 dark:text-blue-400" />
              Flight Details
            </div>
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
              <div><p className="text-xs text-slate-400 dark:text-slate-500 font-medium">Flight</p><p className="font-bold text-slate-800 dark:text-slate-200 mt-0.5">{booking.flight.flightNumber}</p></div>
              <div><p className="text-xs text-slate-400 dark:text-slate-500 font-medium">Airline</p><p className="font-bold text-slate-800 dark:text-slate-200 mt-0.5">{booking.flight.airlineName}</p></div>
              <div><p className="text-xs text-slate-400 dark:text-slate-500 font-medium">Route</p><p className="font-bold text-slate-800 dark:text-slate-200 mt-0.5">{booking.flight.originCity || booking.flight.originIata} → {booking.flight.destinationCity || booking.flight.destinationIata}</p></div>
              <div><p className="text-xs text-slate-400 dark:text-slate-500 font-medium">Departure</p><p className="font-bold text-slate-800 dark:text-slate-200 mt-0.5">{format(new Date(booking.flight.departureTime), 'dd MMM yyyy HH:mm')}</p></div>
              <div><p className="text-xs text-slate-400 dark:text-slate-500 font-medium">Flight Status</p><div className="mt-0.5"><StatusBadge status={booking.flight.status} /></div></div>
            </div>
          </div>

          {/* Passengers */}
          <div className="card mb-4 border-slate-200 dark:border-slate-800">
            <div className="flex items-center gap-2 mb-4 pb-3 border-b border-slate-100 dark:border-slate-800 font-bold text-slate-900 dark:text-slate-100">
              <Users className="w-5 h-5 text-blue-600 dark:text-blue-400" />
              Passengers ({booking.passengers.length})
            </div>
            <div className="space-y-2">
              {booking.passengers.map((p) => (
                <div key={p.id} className="flex justify-between items-center text-sm py-2 px-3 rounded-xl bg-slate-50 dark:bg-slate-800/60 border border-slate-100 dark:border-slate-800">
                  <span className="font-semibold text-slate-800 dark:text-slate-200">{p.firstName} {p.lastName}</span>
                  <div className="flex items-center gap-2">
                    <span className="text-xs font-semibold text-blue-600 dark:text-blue-400 bg-blue-50 dark:bg-blue-950/60 px-2 py-0.5 rounded-md">
                      Seat {p.seatNumber} · {p.seatClass}
                    </span>
                    {p.passportNumber && <span className="text-xs text-slate-400 dark:text-slate-500 font-mono">({p.passportNumber})</span>}
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Specific Class Inclusions & Instructions */}
          {booking.passengers?.length > 0 && (
            <div className="card mb-4 border-slate-200 dark:border-slate-800">
              <div className="flex items-center gap-2 mb-3 pb-3 border-b border-slate-100 dark:border-slate-800 font-bold text-slate-900 dark:text-slate-100">
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
              <div className="flex items-center gap-2 font-bold text-slate-700 dark:text-slate-300">
                <CreditCard className="w-5 h-5 text-blue-600 dark:text-blue-400" />
                Total Amount
              </div>
              <span className="text-2xl font-black text-blue-600 dark:text-blue-400">₹{booking.totalAmount}</span>
            </div>
          </div>

          {/* Automatic Refund Banner */}
          {(booking.status === 'REFUNDED' || booking.refund) && (
            <div className="card mb-6 bg-emerald-50/80 dark:bg-emerald-950/40 border-emerald-200 dark:border-emerald-800 text-emerald-900 dark:text-emerald-100">
              <div className="flex items-center gap-2 font-bold text-base mb-1 text-emerald-800 dark:text-emerald-300">
                <CheckCircle className="w-5 h-5 text-emerald-600 dark:text-emerald-400 shrink-0" />
                Booking Cancelled & Refund Processed Automatically
              </div>
              <p className="text-sm text-emerald-700 dark:text-emerald-300/90 mb-3">
                A full refund of ₹{booking.refund?.refundAmount ?? booking.totalAmount} has been processed back to your original payment method.
              </p>
              {booking.refund && (
                <div className="grid grid-cols-2 sm:grid-cols-3 gap-3 text-xs bg-white/80 dark:bg-slate-900/70 p-3 rounded-xl border border-emerald-200/60 dark:border-emerald-800/60">
                  <div>
                    <span className="text-slate-500 dark:text-slate-400 block font-medium">Refund Status</span>
                    <span className="font-bold text-emerald-700 dark:text-emerald-400 uppercase">{booking.refund.status}</span>
                  </div>
                  <div>
                    <span className="text-slate-500 dark:text-slate-400 block font-medium">Amount Refunded</span>
                    <span className="font-bold text-slate-800 dark:text-slate-200">₹{booking.refund.refundAmount}</span>
                  </div>
                  {booking.refund.refundTransactionId && (
                    <div className="col-span-2 sm:col-span-1">
                      <span className="text-slate-500 dark:text-slate-400 block font-medium">Transaction ID</span>
                      <span className="font-mono font-bold text-slate-800 dark:text-slate-200 text-[11px] truncate block">{booking.refund.refundTransactionId}</span>
                    </div>
                  )}
                </div>
              )}
            </div>
          )}

          {booking.status === 'CANCELLED' && !booking.refund && (
            <div className="card mb-6 bg-slate-50 dark:bg-slate-900 border-slate-200 dark:border-slate-800 text-slate-700 dark:text-slate-300">
              <p className="text-sm">
                This booking has been cancelled. No payment was charged, so no refund was required.
              </p>
            </div>
          )}

          {/* Action buttons */}
          <div className="flex flex-wrap gap-3">
            {booking.status === 'CONFIRMED' && !flightCompleted && !flightDeparted && !showCancelConfirm && (
              <button
                onClick={() => setShowCancelConfirm(true)}
                className="btn-danger"
              >
                Cancel Booking
              </button>
            )}
          </div>

          {/* Cancel confirmation modal / card */}
          {showCancelConfirm && !flightCompleted && !flightDeparted && (
            <div className="mt-4 card border-rose-200 dark:border-rose-900/60 bg-rose-50/80 dark:bg-rose-950/30">
              <div className="flex items-center gap-2 text-rose-800 dark:text-rose-300 font-bold mb-2">
                <AlertTriangle className="w-5 h-5" />
                Confirm Cancellation
              </div>
              <p className="text-sm text-rose-700 dark:text-rose-300/80 mb-3">
                This action cannot be undone. If payment was completed, your full refund will be processed automatically to your original payment method.
              </p>
              <textarea
                className="input mb-3 bg-white dark:bg-slate-900"
                rows={2}
                placeholder="Reason for cancellation (optional)"
                value={cancelReason}
                onChange={e => setCancelReason(e.target.value)}
              />
              <div className="flex gap-2">
                <button onClick={handleCancel} disabled={cancelling} className="btn-danger text-sm py-2">
                  {cancelling ? 'Cancelling…' : 'Yes, Cancel Booking'}
                </button>
                <button onClick={() => setShowCancelConfirm(false)} className="btn-secondary text-sm py-2">
                  Keep Booking
                </button>
              </div>
            </div>
          )}
        </>
      )}
    </div>
  )
}
