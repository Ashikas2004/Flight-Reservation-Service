import React, { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { Plane, ArrowRight, Clock, Calendar, MapPin, ArrowLeft } from 'lucide-react'
import { flightApi } from '../../api/flightApi'
import { FullPageSpinner } from '../../components/common/Spinner'
import Alert from '../../components/common/Alert'
import StatusBadge from '../../components/common/StatusBadge'
import { format } from 'date-fns'

export default function FlightDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [flight, setFlight] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    flightApi.getById(id)
      .then(r => setFlight(r.data))
      .catch(() => setError('Flight not found'))
      .finally(() => setLoading(false))
  }, [id])

  if (loading) return <FullPageSpinner />
  if (error) return <div className="max-w-4xl mx-auto px-4 py-8"><Alert type="error" message={error} /></div>

  const dh = Math.floor(flight.durationMinutes / 60)
  const dm = flight.durationMinutes % 60

  return (
    <div className="max-w-4xl mx-auto px-4 py-8">
      <button
        onClick={() => navigate(-1)}
        className="inline-flex items-center gap-1.5 text-blue-600 dark:text-blue-400 text-sm mb-6 hover:underline font-medium cursor-pointer"
      >
        <ArrowLeft className="w-4 h-4" /> Back to search
      </button>

      <div className="card mb-6">
        <div className="flex items-center justify-between mb-8 pb-6 border-b border-slate-100 dark:border-slate-800">
          <div className="flex items-center gap-3.5">
            <div className="w-12 h-12 bg-blue-50 dark:bg-blue-950/60 rounded-xl flex items-center justify-center shadow-xs">
              <Plane className="w-6 h-6 text-blue-600 dark:text-blue-400" />
            </div>
            <div>
              <h1 className="text-xl font-bold text-slate-900 dark:text-slate-100">{flight.airline.name}</h1>
              <p className="text-slate-500 dark:text-slate-400 text-sm font-medium">Flight {flight.flightNumber}</p>
            </div>
          </div>
          <StatusBadge status={flight.status} />
        </div>

        <div className="flex flex-col md:flex-row items-center justify-between gap-6 py-4">
          <div className="text-center md:text-left min-w-[140px]">
            <p className="text-3xl font-extrabold text-slate-900 dark:text-slate-100">{format(new Date(flight.departureTime), 'HH:mm')}</p>
            <p className="text-lg font-bold text-slate-800 dark:text-slate-200 mt-1">{flight.originAirport.city}</p>
            <p className="text-sm text-slate-500 dark:text-slate-400">{flight.originAirport.name} ({flight.originAirport.iataCode})</p>
            <p className="text-xs text-slate-400 dark:text-slate-500">{flight.originAirport.country}</p>
            <div className="flex items-center justify-center md:justify-start gap-1.5 mt-2 text-xs text-slate-500 dark:text-slate-400 font-medium">
              <Calendar className="w-3.5 h-3.5 text-blue-500" />
              {format(new Date(flight.departureTime), 'dd MMM yyyy')}
            </div>
          </div>

          <div className="flex flex-col items-center flex-1 max-w-xs w-full px-4">
            <div className="flex items-center gap-1.5 text-xs font-semibold text-slate-500 dark:text-slate-400">
              <Clock className="w-3.5 h-3.5" />
              {dh}h {dm}m
            </div>
            <div className="flex items-center w-full gap-2 my-2.5">
              <div className="h-0.5 flex-1 bg-slate-200 dark:bg-slate-700" />
              <div className="p-1.5 rounded-full bg-blue-50 dark:bg-blue-950/60 text-blue-600 dark:text-blue-400 shadow-xs">
                <Plane className="w-4 h-4 rotate-90" />
              </div>
              <div className="h-0.5 flex-1 bg-slate-200 dark:bg-slate-700" />
            </div>
            <p className="text-xs font-semibold text-emerald-600 dark:text-emerald-400">Direct Flight</p>
          </div>

          <div className="text-center md:text-right min-w-[140px]">
            <p className="text-3xl font-extrabold text-slate-900 dark:text-slate-100">{format(new Date(flight.arrivalTime), 'HH:mm')}</p>
            <p className="text-lg font-bold text-slate-800 dark:text-slate-200 mt-1">{flight.destinationAirport.city}</p>
            <p className="text-sm text-slate-500 dark:text-slate-400">{flight.destinationAirport.name} ({flight.destinationAirport.iataCode})</p>
            <p className="text-xs text-slate-400 dark:text-slate-500">{flight.destinationAirport.country}</p>
            <div className="flex items-center justify-center md:justify-end gap-1.5 mt-2 text-xs text-slate-500 dark:text-slate-400 font-medium">
              <Calendar className="w-3.5 h-3.5 text-blue-500" />
              {format(new Date(flight.arrivalTime), 'dd MMM yyyy')}
            </div>
          </div>
        </div>
      </div>

      {/* Cabin Class Pricing */}
      <div className="card mb-6">
        <h2 className="font-bold text-lg text-slate-900 dark:text-slate-100 mb-4">Cabin Classes & Fares</h2>
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          {[
            { label: 'Economy', price: flight.basePrice, extra: 0, tag: 'Standard' },
            { label: 'Business', price: flight.basePrice, extra: 200, tag: 'Popular' },
            { label: 'First Class', price: flight.basePrice, extra: 500, tag: 'Premium' },
          ].map(({ label, price, extra, tag }) => (
            <div key={label} className="border border-slate-200 dark:border-slate-800 rounded-xl p-5 text-center bg-slate-50/50 dark:bg-slate-800/50 hover:border-blue-400 dark:hover:border-blue-700/60 transition-all">
              <span className="inline-block px-2.5 py-0.5 rounded-full text-[11px] font-semibold bg-blue-50 dark:bg-blue-950/60 text-blue-600 dark:text-blue-300 mb-2">
                {tag}
              </span>
              <p className="font-semibold text-slate-800 dark:text-slate-200 text-sm">{label}</p>
              <p className="text-2xl font-black text-blue-600 dark:text-blue-400 mt-1">₹{(parseFloat(price) + extra).toFixed(2)}</p>
              <p className="text-xs text-slate-400 dark:text-slate-500 font-medium mt-0.5">per person</p>
            </div>
          ))}
        </div>
      </div>

      {/* Action footer */}
      <div className="card flex flex-col sm:flex-row items-center justify-between gap-4">
        <div>
          <p className="text-sm font-semibold text-slate-700 dark:text-slate-300">
            {flight.availableSeats} seats remaining
          </p>
          <p className="text-xs text-slate-500 dark:text-slate-400">
            Instant booking confirmation with electronic ticket
          </p>
        </div>
        <button
          className="btn-primary w-full sm:w-auto px-8 py-3 text-sm"
          disabled={flight.status !== 'SCHEDULED' || flight.availableSeats === 0}
          onClick={() => navigate(`/booking/${flight.id}/seats`)}
        >
          Select Seats & Continue
        </button>
      </div>
    </div>
  )
}
