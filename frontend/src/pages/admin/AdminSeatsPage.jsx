import React, { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { ArrowLeft, Armchair } from 'lucide-react'
import { adminApi } from '../../api/adminApi'
import { flightApi } from '../../api/flightApi'
import { FullPageSpinner } from '../../components/common/Spinner'
import Alert from '../../components/common/Alert'
import StatusBadge from '../../components/common/StatusBadge'
import { clsx } from 'clsx'

const CLASS_COLORS = {
  FIRST: 'text-amber-800 bg-amber-100 dark:bg-amber-950/60 dark:text-amber-300 border border-amber-200 dark:border-amber-800',
  BUSINESS: 'text-blue-800 bg-blue-100 dark:bg-blue-950/60 dark:text-blue-300 border border-blue-200 dark:border-blue-800',
  ECONOMY: 'text-emerald-800 bg-emerald-100 dark:bg-emerald-950/60 dark:text-emerald-300 border border-emerald-200 dark:border-emerald-800',
}

export default function AdminSeatsPage() {
  const { flightId } = useParams()
  const navigate = useNavigate()
  const [seats, setSeats] = useState([])
  const [flight, setFlight] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const load = () => {
    Promise.all([
      flightApi.getById(flightId),
      adminApi.getSeatsByFlight(flightId),
    ])
      .then(([fr, sr]) => { setFlight(fr.data); setSeats(sr.data) })
      .catch(() => setError('Failed to load seats'))
      .finally(() => setLoading(false))
  }

  useEffect(() => { load() }, [flightId])

  const updateStatus = async (seatId, status) => {
    try {
      await adminApi.updateSeatStatus(seatId, status)
      setSeats(prev => prev.map(s => s.id === seatId ? { ...s, status } : s))
    } catch { setError('Failed to update seat status') }
  }

  if (loading) return <FullPageSpinner />

  const stats = {
    total: seats.length,
    available: seats.filter(s => s.status === 'AVAILABLE').length,
    booked: seats.filter(s => s.status === 'BOOKED').length,
    locked: seats.filter(s => s.status === 'LOCKED').length,
  }

  return (
    <div className="max-w-6xl mx-auto px-4 py-8">
      <button
        onClick={() => navigate('/admin/flights')}
        className="inline-flex items-center gap-1.5 text-blue-600 dark:text-blue-400 text-sm mb-4 hover:underline font-medium cursor-pointer"
      >
        <ArrowLeft className="w-4 h-4" /> Back to Flights
      </button>

      {flight && (
        <div className="mb-6">
          <h1 className="text-3xl font-extrabold text-slate-900 dark:text-slate-100 tracking-tight">Seats — {flight.flightNumber}</h1>
          <p className="text-slate-500 dark:text-slate-400 text-sm mt-1 font-medium">
            {flight.originAirport?.city || flight.originAirport?.iataCode} → {flight.destinationAirport?.city || flight.destinationAirport?.iataCode} · {flight.airline?.name}
          </p>
        </div>
      )}

      {error && <Alert type="error" message={error} onClose={() => setError('')} className="mb-4" />}

      {/* Stats */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 mb-6">
        {[
          { label: 'Total Seats', value: stats.total, color: 'text-slate-900 dark:text-slate-100' },
          { label: 'Available', value: stats.available, color: 'text-emerald-600 dark:text-emerald-400' },
          { label: 'Booked', value: stats.booked, color: 'text-rose-600 dark:text-rose-400' },
          { label: 'Locked', value: stats.locked, color: 'text-amber-600 dark:text-amber-400' },
        ].map(({ label, value, color }) => (
          <div key={label} className="card text-center py-4 border-slate-200 dark:border-slate-800 shadow-xs">
            <p className={`text-2xl font-black ${color}`}>{value}</p>
            <p className="text-xs text-slate-500 dark:text-slate-400 font-semibold mt-0.5">{label}</p>
          </div>
        ))}
      </div>

      {/* Seat table */}
      <div className="card overflow-hidden p-0 border-slate-200 dark:border-slate-800 shadow-sm">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead className="bg-slate-50 dark:bg-slate-800/80 border-b border-slate-200 dark:border-slate-700/80">
              <tr>
                {['Seat', 'Class', 'Status', 'Extra Price', 'Actions'].map(h => (
                  <th key={h} className="px-4 py-3.5 text-left text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
              {seats.map(seat => (
                <tr key={seat.id} className="hover:bg-slate-50/80 dark:hover:bg-slate-800/40 transition-colors">
                  <td className="px-4 py-3 font-mono font-bold text-slate-900 dark:text-slate-100">{seat.seatNumber}</td>
                  <td className="px-4 py-3">
                    <span className={clsx('badge text-xs font-semibold', CLASS_COLORS[seat.seatClass])}>
                      {seat.seatClass}
                    </span>
                  </td>
                  <td className="px-4 py-3"><StatusBadge status={seat.status} /></td>
                  <td className="px-4 py-3 font-medium text-slate-600 dark:text-slate-400">₹{seat.extraPrice}</td>
                  <td className="px-4 py-3">
                    <select
                      className="input text-xs py-1.5 w-36"
                      value={seat.status}
                      onChange={e => updateStatus(seat.id, e.target.value)}
                    >
                      <option value="AVAILABLE">AVAILABLE</option>
                      <option value="LOCKED">LOCKED</option>
                      <option value="BOOKED">BOOKED</option>
                    </select>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}
