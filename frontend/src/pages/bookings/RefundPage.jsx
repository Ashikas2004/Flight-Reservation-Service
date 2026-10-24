import React, { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { RefreshCw, CheckCircle, ArrowLeft } from 'lucide-react'
import { bookingApi } from '../../api/bookingApi'
import { refundApi } from '../../api/refundApi'
import { FullPageSpinner } from '../../components/common/Spinner'
import Alert from '../../components/common/Alert'

export default function RefundPage() {
  const { bookingId } = useParams()
  const navigate = useNavigate()
  const [booking, setBooking] = useState(null)
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [reason, setReason] = useState('')
  const [success, setSuccess] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    bookingApi.getMyBookings(0, 50)
      .then(r => {
        const found = r.data.content.find(b => b.id === Number(bookingId))
        if (found) setBooking(found)
        else setError('Booking not found')
      })
      .catch(() => setError('Failed to load booking'))
      .finally(() => setLoading(false))
  }, [bookingId])

  const handleSubmit = async () => {
    setSubmitting(true)
    setError('')
    try {
      const res = await refundApi.requestRefund({ bookingId: Number(bookingId), reason })
      setSuccess(res.data)
    } catch (err) {
      setError(err.response?.data?.message ?? 'Refund request failed')
    } finally {
      setSubmitting(false)
    }
  }

  if (loading) return <FullPageSpinner />

  if (success) {
    return (
      <div className="max-w-md mx-auto px-4 py-16 text-center">
        <div className="card border-slate-200 dark:border-slate-800 shadow-lg p-8">
          <div className="w-16 h-16 bg-emerald-100 dark:bg-emerald-950/60 rounded-2xl flex items-center justify-center mx-auto mb-4 shadow-md shadow-emerald-500/10">
            <CheckCircle className="w-9 h-9 text-emerald-600 dark:text-emerald-400" />
          </div>
          <h1 className="text-2xl font-extrabold text-slate-900 dark:text-slate-100 mb-2">Refund {success.status}</h1>
          <p className="text-slate-500 dark:text-slate-400 text-sm mb-3">Refund of <strong className="text-slate-900 dark:text-slate-100">₹{success.refundAmount}</strong> has been {success.status.toLowerCase()}.</p>
          {success.refundTransactionId && (
            <div className="bg-slate-50 dark:bg-slate-800/60 rounded-xl p-2.5 mb-6 border border-slate-100 dark:border-slate-800">
              <p className="text-xs text-slate-400 dark:text-slate-500 font-mono">ID: {success.refundTransactionId}</p>
            </div>
          )}
          <button onClick={() => navigate('/bookings')} className="btn-primary w-full py-2.5">Back to Bookings</button>
        </div>
      </div>
    )
  }

  return (
    <div className="max-w-lg mx-auto px-4 py-8">
      <button
        onClick={() => navigate(-1)}
        className="inline-flex items-center gap-1.5 text-blue-600 dark:text-blue-400 text-sm mb-4 hover:underline font-medium cursor-pointer"
      >
        <ArrowLeft className="w-4 h-4" /> Back
      </button>
      <h1 className="text-2xl font-extrabold mb-6 text-slate-900 dark:text-slate-100 tracking-tight">Request Refund</h1>

      {error && <Alert type="error" message={error} onClose={() => setError('')} className="mb-4" />}

      {booking && (
        <div className="card mb-6 border-slate-200 dark:border-slate-800 bg-blue-50/70 dark:bg-blue-950/40">
          <div className="flex justify-between items-center text-sm">
            <div>
              <p className="text-xs text-slate-400 dark:text-slate-500 font-medium">Booking Ref</p>
              <p className="font-extrabold text-blue-600 dark:text-blue-400 text-lg">{booking.bookingReference}</p>
            </div>
            <div>
              <p className="text-xs text-slate-400 dark:text-slate-500 font-medium">Route</p>
              <p className="font-bold text-slate-800 dark:text-slate-200">{booking.flight?.originCity || booking.flight?.originIata} → {booking.flight?.destinationCity || booking.flight?.destinationIata}</p>
            </div>
            <div>
              <p className="text-xs text-slate-400 dark:text-slate-500 font-medium">Refund Amount</p>
              <p className="font-black text-emerald-600 dark:text-emerald-400 text-lg">₹{booking.totalAmount}</p>
            </div>
          </div>
        </div>
      )}

      <div className="card border-slate-200 dark:border-slate-800 shadow-md">
        <div className="flex items-center gap-2 mb-4 pb-3 border-b border-slate-100 dark:border-slate-800 font-bold text-slate-900 dark:text-slate-100">
          <RefreshCw className="w-5 h-5 text-blue-600 dark:text-blue-400" />
          Refund Policy
        </div>
        <p className="text-sm text-slate-500 dark:text-slate-400 mb-4 leading-relaxed">
          The full eligible booking amount will be reversed to your original payment method. Instant UPI refunds reflect immediately, while card transactions typically settle within 3–5 business days.
        </p>
        <div className="mb-5">
          <label className="label">Reason for Refund (optional)</label>
          <textarea
            className="input"
            rows={3}
            placeholder="Tell us why you'd like a refund…"
            value={reason}
            onChange={e => setReason(e.target.value)}
          />
        </div>
        <div className="flex gap-3">
          <button
            onClick={handleSubmit}
            disabled={submitting}
            className="btn-primary flex items-center gap-2 flex-1 justify-center py-2.5 font-semibold"
          >
            <RefreshCw className={`w-4 h-4 ${submitting ? 'animate-spin' : ''}`} />
            {submitting ? 'Processing…' : 'Submit Refund Request'}
          </button>
          <button onClick={() => navigate(-1)} className="btn-secondary py-2.5">Cancel</button>
        </div>
      </div>
    </div>
  )
}
