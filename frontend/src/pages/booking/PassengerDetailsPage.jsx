import React, { useState } from 'react'
import { useParams, useNavigate, useLocation } from 'react-router-dom'
import { useForm, useFieldArray } from 'react-hook-form'
import { User, Plane } from 'lucide-react'
import { bookingApi } from '../../api/bookingApi'
import Alert from '../../components/common/Alert'

export default function PassengerDetailsPage() {
  const { flightId } = useParams()
  const navigate = useNavigate()
  const location = useLocation()
  const { selectedSeats = [] } = location.state ?? {}
  const [error, setError] = useState('')

  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm({
    defaultValues: {
      passengers: selectedSeats.map(seat => ({
        seatId: seat.id,
        seatNumber: seat.seatNumber,
        seatClass: seat.seatClass,
        firstName: '',
        lastName: '',
        passportNumber: '',
        dateOfBirth: '',
        nationality: '',
      }))
    }
  })

  if (selectedSeats.length === 0) {
    navigate(`/booking/${flightId}/seats`)
    return null
  }

  const onSubmit = async (data) => {
    setError('')
    try {
      const payload = {
        flightId: Number(flightId),
        passengers: data.passengers.map(p => ({
          seatId: p.seatId,
          firstName: p.firstName,
          lastName: p.lastName,
          passportNumber: p.passportNumber,
          dateOfBirth: p.dateOfBirth,
          nationality: p.nationality,
        }))
      }
      const res = await bookingApi.create(payload)
      navigate(`/booking/${res.data.id}/confirm`, { state: { booking: res.data } })
    } catch (err) {
      setError(err.response?.data?.message ?? 'Booking failed. Please try again.')
    }
  }

  return (
    <div className="max-w-3xl mx-auto px-4 py-8">
      <button
        onClick={() => navigate(`/booking/${flightId}/seats`, { state: { selectedSeats } })}
        className="text-blue-600 dark:text-blue-400 text-sm mb-4 hover:underline flex items-center gap-1.5 font-medium cursor-pointer"
      >
        ← Back to seat selection
      </button>

      <h1 className="text-2xl font-extrabold mb-6 text-slate-900 dark:text-slate-100 tracking-tight">Passenger Details</h1>

      {error && <Alert type="error" message={error} onClose={() => setError('')} className="mb-6" />}

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
        {selectedSeats.map((seat, idx) => (
          <div key={seat.id} className="card">
            <div className="flex items-center gap-3.5 mb-5 pb-4 border-b border-slate-100 dark:border-slate-800">
              <div className="w-10 h-10 bg-blue-50 dark:bg-blue-950/60 rounded-xl flex items-center justify-center">
                <User className="w-5 h-5 text-blue-600 dark:text-blue-400" />
              </div>
              <div>
                <p className="font-bold text-slate-900 dark:text-slate-100">Passenger {idx + 1}</p>
                <p className="text-xs text-slate-500 dark:text-slate-400 font-medium">
                  Seat <span className="text-blue-600 dark:text-blue-400 font-bold">{seat.seatNumber}</span> · {seat.seatClass}
                  {parseFloat(seat.extraPrice) > 0 && ` (+₹${seat.extraPrice})`}
                </p>
              </div>
            </div>

            <input type="hidden" {...register(`passengers.${idx}.seatId`)} />

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="label">First Name *</label>
                <input
                  className="input"
                  placeholder="Enter First Name"
                  {...register(`passengers.${idx}.firstName`, { required: 'Required' })}
                />
                {errors.passengers?.[idx]?.firstName && (
                  <p className="text-red-500 text-xs mt-1">{errors.passengers[idx].firstName.message}</p>
                )}
              </div>

              <div>
                <label className="label">Last Name *</label>
                <input
                  className="input"
                  placeholder="Enter Last Name"
                  {...register(`passengers.${idx}.lastName`, { required: 'Required' })}
                />
                {errors.passengers?.[idx]?.lastName && (
                  <p className="text-red-500 text-xs mt-1">{errors.passengers[idx].lastName.message}</p>
                )}
              </div>

              <div>
                <label className="label">Passport Number *</label>
                <input
                  className="input"
                  placeholder="AB1234567"
                  {...register(`passengers.${idx}.passportNumber`, { required: 'Required' })}
                />
                {errors.passengers?.[idx]?.passportNumber && (
                  <p className="text-red-500 text-xs mt-1">{errors.passengers[idx].passportNumber.message}</p>
                )}
              </div>

              <div>
                <label className="label">Date of Birth *</label>
                <input
                  type="date"
                  className="input"
                  {...register(`passengers.${idx}.dateOfBirth`, { required: 'Required' })}
                />
                {errors.passengers?.[idx]?.dateOfBirth && (
                  <p className="text-red-500 text-xs mt-1">{errors.passengers[idx].dateOfBirth.message}</p>
                )}
              </div>

              <div>
                <label className="label">Nationality *</label>
                <input
                  className="input"
                  placeholder="United States"
                  {...register(`passengers.${idx}.nationality`, { required: 'Required' })}
                />
                {errors.passengers?.[idx]?.nationality && (
                  <p className="text-red-500 text-xs mt-1">{errors.passengers[idx].nationality.message}</p>
                )}
              </div>
            </div>
          </div>
        ))}

        <div className="flex justify-end">
          <button type="submit" disabled={isSubmitting} className="btn-primary px-8">
            {isSubmitting ? 'Creating booking…' : 'Confirm Passengers & Book'}
          </button>
        </div>
      </form>
    </div>
  )
}
