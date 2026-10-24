import React, { useState, useEffect } from 'react'
import { useParams, useNavigate, useLocation } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { CreditCard, Lock, CheckCircle, XCircle, ArrowLeft, ShieldCheck } from 'lucide-react'
import { paymentApi } from '../../api/paymentApi'
import { bookingApi } from '../../api/bookingApi'
import Alert from '../../components/common/Alert'
import { FullPageSpinner } from '../../components/common/Spinner'

const PAYMENT_METHODS = [
  { value: 'CREDIT_CARD', label: 'Credit Card' },
  { value: 'DEBIT_CARD', label: 'Debit Card' },
  { value: 'PAYPAL', label: 'UPI' },
]

export default function PaymentPage() {
  const { bookingId } = useParams()
  const navigate = useNavigate()
  const location = useLocation()
  const [booking, setBooking] = useState(location.state?.booking ?? null)
  const [loading, setLoading] = useState(!booking)
  const [result, setResult] = useState(null) // 'success' | 'failed'
  const [resultData, setResultData] = useState(null)
  const [error, setError] = useState('')

  const { register, handleSubmit, watch, formState: { errors, isSubmitting } } = useForm({
    defaultValues: { paymentMethod: 'CREDIT_CARD' }
  })

  const method = watch('paymentMethod')

  useEffect(() => {
    if (!booking) {
      bookingApi.getByReference(bookingId)
        .catch(() => bookingApi.getMyBookings(0, 50)
          .then(r => r.data.content.find(b => b.id === Number(bookingId))))
        .then(data => {
          if (data?.data) setBooking(data.data)
          else if (data) setBooking(data)
        })
        .catch(() => setError('Booking not found'))
        .finally(() => setLoading(false))
    }
  }, [bookingId])

  const onSubmit = async (data) => {
    setError('')
    try {
      const res = await paymentApi.processPayment({
        bookingId: Number(bookingId),
        paymentMethod: data.paymentMethod,
        cardNumber: data.cardNumber,
        cardHolderName: data.cardHolderName,
      })
      setResultData(res.data)
      setResult(res.data.status === 'SUCCESS' ? 'success' : 'failed')
    } catch (err) {
      setError(err.response?.data?.message ?? 'Payment failed. Please try again.')
    }
  }

  if (loading) return <FullPageSpinner />

  if (result === 'success') {
    return (
      <div className="max-w-md mx-auto px-4 py-16 text-center">
        <div className="card border-slate-200 dark:border-slate-800 shadow-lg p-8">
          <div className="w-20 h-20 bg-emerald-100 dark:bg-emerald-950/60 rounded-3xl flex items-center justify-center mx-auto mb-6 shadow-md shadow-emerald-500/10">
            <CheckCircle className="w-12 h-12 text-emerald-600 dark:text-emerald-400" />
          </div>
          <h1 className="text-2xl font-extrabold text-slate-900 dark:text-slate-100 mb-2">Payment Successful!</h1>
          <p className="text-slate-500 dark:text-slate-400 text-sm mb-3">Your flight reservation has been fully confirmed.</p>
          <div className="bg-slate-50 dark:bg-slate-800/60 rounded-xl p-3 mb-6 border border-slate-100 dark:border-slate-800">
            <p className="text-xs text-slate-400 dark:text-slate-500 font-medium">Transaction ID</p>
            <p className="text-sm font-bold text-slate-800 dark:text-slate-200 font-mono mt-0.5">{resultData?.transactionId}</p>
            <p className="text-2xl font-black text-emerald-600 dark:text-emerald-400 mt-2">₹{resultData?.amount}</p>
          </div>
          <div className="flex flex-col sm:flex-row gap-3 justify-center">
            <button onClick={() => navigate(`/bookings/${booking?.bookingReference ?? ''}`)} className="btn-primary py-2.5">
              View Ticket Details
            </button>
            <button onClick={() => navigate('/bookings')} className="btn-secondary py-2.5">
              My Bookings
            </button>
          </div>
        </div>
      </div>
    )
  }

  if (result === 'failed') {
    return (
      <div className="max-w-md mx-auto px-4 py-16 text-center">
        <div className="card border-slate-200 dark:border-slate-800 shadow-lg p-8">
          <div className="w-20 h-20 bg-rose-100 dark:bg-rose-950/60 rounded-3xl flex items-center justify-center mx-auto mb-6 shadow-md shadow-rose-500/10">
            <XCircle className="w-12 h-12 text-rose-600 dark:text-rose-400" />
          </div>
          <h1 className="text-2xl font-extrabold text-slate-900 dark:text-slate-100 mb-2">Payment Declined</h1>
          <p className="text-slate-500 dark:text-slate-400 text-sm mb-6">{resultData?.failedReason ?? 'Your transaction could not be processed.'}</p>
          <div className="flex flex-col sm:flex-row gap-3 justify-center">
            <button onClick={() => setResult(null)} className="btn-primary py-2.5">Try Again</button>
            <button onClick={() => navigate('/bookings')} className="btn-secondary py-2.5">My Bookings</button>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="max-w-2xl mx-auto px-4 py-8">
      <button
        onClick={() => navigate(-1)}
        className="inline-flex items-center gap-1.5 text-blue-600 dark:text-blue-400 text-sm mb-4 hover:underline font-medium cursor-pointer"
      >
        <ArrowLeft className="w-4 h-4" /> Back
      </button>

      <h1 className="text-3xl font-extrabold mb-3 text-slate-900 dark:text-slate-100 tracking-tight">Complete Payment</h1>

      {booking && (
        <div className="card mb-6 bg-blue-50/70 dark:bg-blue-950/40 border-blue-200/80 dark:border-blue-900/60">
          <div className="flex flex-wrap items-center justify-between gap-4 text-sm">
            <div>
              <p className="text-xs text-slate-500 dark:text-slate-400 font-medium">Booking Reference</p>
              <p className="font-extrabold text-blue-700 dark:text-blue-400 text-lg">{booking.bookingReference}</p>
            </div>
            <div>
              <p className="text-xs text-slate-500 dark:text-slate-400 font-medium">Route</p>
              <p className="font-bold text-slate-800 dark:text-slate-200">
                {booking.flight?.originCity || booking.flight?.originIata} → {booking.flight?.destinationCity || booking.flight?.destinationIata}
              </p>
            </div>
            <div>
              <p className="text-xs text-slate-500 dark:text-slate-400 font-medium">Amount Due</p>
              <p className="font-black text-blue-700 dark:text-blue-400 text-xl">₹{booking.totalAmount}</p>
            </div>
          </div>
        </div>
      )}

      {error && <Alert type="error" message={error} onClose={() => setError('')} className="mb-4" />}

      <div className="card border-slate-200 dark:border-slate-800 shadow-md">
        <div className="flex items-center gap-2 mb-6 pb-4 border-b border-slate-100 dark:border-slate-800 text-slate-900 dark:text-slate-100 font-bold text-base">
          <ShieldCheck className="w-5 h-5 text-emerald-600 dark:text-emerald-400" />
          Secure 256-Bit Encrypted Payment
        </div>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          {/* Payment Method Selector */}
          <div>
            <label className="label">Select Payment Method</label>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
              {PAYMENT_METHODS.map(pm => {
                const isSelected = method === pm.value
                return (
                  <label
                    key={pm.value}
                    className={`flex items-center gap-2.5 p-3.5 rounded-xl border cursor-pointer transition-all ${
                      isSelected
                        ? 'border-blue-500 bg-blue-50 dark:bg-blue-950/60 dark:border-blue-400 ring-2 ring-blue-500/20 text-blue-900 dark:text-blue-100 font-semibold'
                        : 'border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 hover:bg-slate-50 dark:hover:bg-slate-700/60 text-slate-700 dark:text-slate-300 font-medium'
                    }`}
                  >
                    <input
                      type="radio"
                      value={pm.value}
                      className="text-blue-600 focus:ring-blue-500"
                      {...register('paymentMethod')}
                    />
                    <CreditCard className={`w-4 h-4 ${isSelected ? 'text-blue-600 dark:text-blue-400' : 'text-slate-400'}`} />
                    <span className="text-sm">{pm.label}</span>
                  </label>
                )
              })}
            </div>
          </div>

          {/* Card fields */}
          {(method === 'CREDIT_CARD' || method === 'DEBIT_CARD') && (
            <div className="space-y-4 pt-2">
              <div>
                <label className="label">Card Number *</label>
                <input
                  className="input font-mono tracking-wider"
                  placeholder="4242 4242 4242 4242"
                  maxLength={19}
                  {...register('cardNumber', { required: 'Card number is required' })}
                />
                {errors.cardNumber && <p className="text-red-500 text-xs mt-1">{errors.cardNumber.message}</p>}
              </div>
              <div>
                <label className="label">Cardholder Name *</label>
                <input
                  className="input"
                  placeholder="Cardholder Name"
                  {...register('cardHolderName', { required: 'Cardholder name is required' })}
                />
                {errors.cardHolderName && <p className="text-red-500 text-xs mt-1">{errors.cardHolderName.message}</p>}
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="label">Expiry Date (MM/YY) *</label>
                  <input
                    className="input font-mono"
                    placeholder="12/28"
                    maxLength={5}
                    {...register('expiry', { required: 'Expiry date is required' })}
                  />
                  {errors.expiry && <p className="text-red-500 text-xs mt-1">{errors.expiry.message}</p>}
                </div>
                <div>
                  <label className="label">Security CVV *</label>
                  <input
                    className="input font-mono"
                    placeholder="123"
                    maxLength={4}
                    type="password"
                    {...register('cvv', { required: 'CVV is required' })}
                  />
                  {errors.cvv && <p className="text-red-500 text-xs mt-1">{errors.cvv.message}</p>}
                </div>
              </div>
            </div>
          )}

          <div className="bg-amber-50 dark:bg-amber-950/40 border border-amber-200 dark:border-amber-800/60 rounded-xl p-3.5 text-xs text-amber-800 dark:text-amber-200 font-medium">
            🔒 Test Environment: This is a simulation gateway. Any sample card details will process safely.
          </div>

          <button
            type="submit"
            disabled={isSubmitting}
            className="btn-primary w-full py-3.5 flex items-center justify-center gap-2 text-base font-semibold shadow-md mt-2"
          >
            <Lock className="w-4 h-4" />
            {isSubmitting ? 'Authorizing Payment…' : `Pay ₹${booking?.totalAmount ?? '—'}`}
          </button>
        </form>
      </div>
    </div>
  )
}
