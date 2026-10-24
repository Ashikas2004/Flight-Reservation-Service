import React, { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { Plane, ArrowRight, Calendar } from 'lucide-react'
import { bookingApi } from '../../api/bookingApi'
import { FullPageSpinner } from '../../components/common/Spinner'
import Alert from '../../components/common/Alert'
import StatusBadge from '../../components/common/StatusBadge'
import { format } from 'date-fns'

export default function MyBookingsPage() {
  const navigate = useNavigate()
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [page, setPage] = useState(0)

  const fetchBookings = (p = 0) => {
    setLoading(true)
    bookingApi.getMyBookings(p, 10)
      .then(r => { setData(r.data); setPage(p) })
      .catch(() => setError('Failed to load bookings'))
      .finally(() => setLoading(false))
  }

  useEffect(() => { fetchBookings() }, [])

  useEffect(() => {
    const refreshBookings = () => {
      bookingApi.getMyBookings(page, 10)
        .then(r => setData(r.data))
        .catch(() => setError('Failed to load bookings'))
    }
    const interval = setInterval(refreshBookings, 60_000)
    window.addEventListener('focus', refreshBookings)
    return () => {
      clearInterval(interval)
      window.removeEventListener('focus', refreshBookings)
    }
  }, [page])

  if (loading && !data) return <FullPageSpinner />

  return (
    <div className="max-w-4xl mx-auto px-4 py-8">
      <h1 className="text-3xl font-extrabold mb-6 text-slate-900 dark:text-slate-100 tracking-tight">My Bookings</h1>

      {error && <Alert type="error" message={error} className="mb-4" />}

      {(!data?.content || data.content.filter(b => b.status !== 'PENDING').length === 0) ? (
        <div className="card text-center py-16 border-slate-200 dark:border-slate-800">
          <Plane className="w-12 h-12 text-slate-300 dark:text-slate-600 mx-auto mb-4" />
          <h2 className="text-xl font-bold text-slate-800 dark:text-slate-200 mb-2">No bookings yet</h2>
          <p className="text-slate-500 dark:text-slate-400 mb-6 text-sm">Book your first flight and your confirmed itinerary will appear right here.</p>
          <button onClick={() => navigate('/flights')} className="btn-primary">Search Flights</button>
        </div>
      ) : (
        <>
          <div className="space-y-4">
            {data.content.filter(b => b.status !== 'PENDING').map(booking => {
              const flightCompleted = booking.flight.status === 'COMPLETED' ||
                new Date(booking.flight.arrivalTime) <= new Date()
              return (
                <div
                  key={booking.id}
                  className="card hover:shadow-lg dark:hover:shadow-slate-900/60 hover:border-blue-400 dark:hover:border-blue-700/50 transition-all cursor-pointer border-slate-200 dark:border-slate-800"
                  onClick={() => navigate(`/bookings/${booking.bookingReference}`)}
                >
                <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
                  {/* Ref & status */}
                  <div>
                    <p className="text-xs text-slate-400 dark:text-slate-500 font-semibold uppercase tracking-wider">Booking Reference</p>
                    <p className="text-xl font-black text-blue-600 dark:text-blue-400 tracking-wider my-0.5">{booking.bookingReference}</p>
                    <div className="mt-1">
                      <StatusBadge status={flightCompleted ? 'COMPLETED' : booking.status} />
                    </div>
                  </div>

                  {/* Route */}
                  <div className="flex items-center gap-3">
                    <div className="text-center">
                      <p className="text-lg font-bold text-slate-900 dark:text-slate-100">{booking.flight.originCity || booking.flight.originIata}</p>
                      {booking.flight.originCity && <p className="text-xs text-slate-400 dark:text-slate-500 font-medium">({booking.flight.originIata})</p>}
                    </div>
                    <ArrowRight className="w-4 h-4 text-slate-400 dark:text-slate-500" />
                    <div className="text-center">
                      <p className="text-lg font-bold text-slate-900 dark:text-slate-100">{booking.flight.destinationCity || booking.flight.destinationIata}</p>
                      {booking.flight.destinationCity && <p className="text-xs text-slate-400 dark:text-slate-500 font-medium">({booking.flight.destinationIata})</p>}
                    </div>
                  </div>

                  {/* Date */}
                  <div className="flex items-center gap-2 text-slate-500 dark:text-slate-400 text-sm font-medium">
                    <Calendar className="w-4 h-4 text-blue-500" />
                    {format(new Date(booking.flight.departureTime), 'dd MMM yyyy · HH:mm')}
                  </div>

                  {/* Amount */}
                  <div className="text-left md:text-right">
                    <p className="text-xs text-slate-400 dark:text-slate-500 font-medium">Total Amount</p>
                    <p className="text-xl font-black text-slate-900 dark:text-slate-100 mt-0.5">₹{booking.totalAmount}</p>
                    <p className="text-xs text-slate-400 dark:text-slate-500 font-medium">{booking.passengers?.length} passenger(s)</p>
                  </div>
                </div>

                {/* Action buttons */}
                <div className="flex gap-2 mt-4 pt-4 border-t border-slate-100 dark:border-slate-800">
                  {booking.status === 'CONFIRMED' && !flightCompleted && (
                    <button
                      className="btn-danger text-xs py-2 px-4"
                      onClick={e => { e.stopPropagation(); navigate(`/bookings/${booking.bookingReference}`) }}
                    >
                      Manage & Cancel
                    </button>
                  )}
                  {booking.status === 'REFUNDED' && (
                    <span className="text-xs font-semibold text-purple-700 dark:text-purple-400 py-1.5 flex items-center">
                      Refunded to original payment method
                    </span>
                  )}
                </div>
                </div>
              )
            })}
          </div>

          {/* Pagination */}
          {data?.totalPages > 1 && (
            <div className="flex justify-center gap-3 mt-6">
              <button
                className="btn-secondary text-sm"
                disabled={page === 0}
                onClick={() => fetchBookings(page - 1)}
              >
                Previous
              </button>
              <span className="text-sm font-medium text-slate-500 dark:text-slate-400 self-center">
                Page {page + 1} of {data.totalPages}
              </span>
              <button
                className="btn-secondary text-sm"
                disabled={data.last}
                onClick={() => fetchBookings(page + 1)}
              >
                Next
              </button>
            </div>
          )}
        </>
      )}
    </div>
  )
}
