import React, { useEffect, useState, useCallback } from 'react'
import { useParams, useNavigate, useLocation } from 'react-router-dom'
import { flightApi } from '../../api/flightApi'
import { seatApi } from '../../api/seatApi'
import { FullPageSpinner } from '../../components/common/Spinner'
import Alert from '../../components/common/Alert'
import { clsx } from 'clsx'
import { format } from 'date-fns'
import { Plane, Users, ArrowLeft } from 'lucide-react'
import { AllClassFareBenefits } from '../../components/booking/ClassFareBenefits'
import CommonTravelInfo from '../../components/booking/CommonTravelInfo'

// ─── Styling helpers ────────────────────────────────────────────────────────

/**
 * Returns Tailwind classes for a seat button.
 * isSelected: Currently selected by this user -> blue.
 * isUnavailable: Booked or locked by ANOTHER user -> disabled gray.
 * Otherwise: Available cabin class color (clickable).
 */
const seatStyle = (seatClass, isSelected, isUnavailable) => {
  if (isSelected) {
    return 'bg-blue-600 border-blue-700 text-white shadow-md scale-105 cursor-pointer ring-2 ring-blue-400 dark:ring-blue-500 font-bold'
  }
  if (isUnavailable) {
    return 'bg-slate-200 dark:bg-slate-800 border-slate-300 dark:border-slate-700 text-slate-400 dark:text-slate-600 cursor-not-allowed'
  }
  const base = 'cursor-pointer hover:scale-105 transition-transform font-medium '
  if (seatClass === 'FIRST')    return base + 'bg-amber-100 dark:bg-amber-950/70 border-amber-300 dark:border-amber-700 text-amber-900 dark:text-amber-200 hover:bg-amber-200 dark:hover:bg-amber-900'
  if (seatClass === 'BUSINESS') return base + 'bg-blue-100 dark:bg-blue-950/70 border-blue-300 dark:border-blue-700 text-blue-900 dark:text-blue-200 hover:bg-blue-200 dark:hover:bg-blue-900'
  return base + 'bg-emerald-100 dark:bg-emerald-950/70 border-emerald-300 dark:border-emerald-700 text-emerald-900 dark:text-emerald-200 hover:bg-emerald-200 dark:hover:bg-emerald-900'
}

const CLASS_INFO = {
  FIRST:    { label: 'First Class', color: 'bg-amber-50 dark:bg-amber-950/40 border-amber-200 dark:border-amber-800 text-amber-900 dark:text-amber-200', extra: '+₹6,000' },
  BUSINESS: { label: 'Business',    color: 'bg-blue-50 dark:bg-blue-950/40 border-blue-200 dark:border-blue-800 text-blue-900 dark:text-blue-200',   extra: '+₹3,000' },
  ECONOMY:  { label: 'Economy',     color: 'bg-emerald-50 dark:bg-emerald-950/40 border-emerald-200 dark:border-emerald-800 text-emerald-900 dark:text-emerald-200', extra: 'Included' },
}

// ─── SeatGrid component ──────────────────────────────────────────────────────

function SeatGrid({ title, classKey, seats, selected, onToggle }) {
  if (!seats.length) return null

  const rows = {}
  seats.forEach(seat => {
    const row = seat.seatNumber.replace(/[A-Z]/g, '')
    if (!rows[row]) rows[row] = []
    rows[row].push(seat)
  })

  const info = CLASS_INFO[classKey]
  const cols = classKey === 'FIRST' ? ['A','B','C','D'] : ['A','B','C','D','E','F']

  // Available count includes seats currently AVAILABLE plus seats locked by THIS user
  const availCount = seats.filter(s =>
    s.status === 'AVAILABLE' || s.isLockedByCurrentUser
  ).length

  return (
    <div className="mb-6">
      <div className={clsx('flex items-center justify-between px-4 py-2 rounded-lg border mb-3', info.color)}>
        <div className="flex items-center gap-2">
          <span className="font-bold text-sm">{title}</span>
          <span className="text-xs opacity-70">({availCount} available)</span>
        </div>
        <span className="text-xs font-semibold">{info.extra}</span>
      </div>

      {/* Column headers */}
      <div className="flex justify-center mb-1">
        <div className="w-8" />
        {cols.map((col, i) => (
          <React.Fragment key={col}>
            {i === 3 && cols.length === 6 && <div className="w-4" />}
            <div className="w-9 text-center text-xs font-medium text-gray-400">{col}</div>
          </React.Fragment>
        ))}
      </div>

      {/* Seat rows */}
      {Object.entries(rows)
        .sort(([a],[b]) => Number(a) - Number(b))
        .map(([row, rowSeats]) => {
          const sorted = [...rowSeats].sort((a,b) => a.seatNumber.localeCompare(b.seatNumber))
          return (
            <div key={row} className="flex items-center justify-center mb-1">
              <div className="w-8 text-center text-xs text-gray-400 font-medium">{row}</div>
              {cols.map((col, i) => {
                const seat = sorted.find(s => s.seatNumber.endsWith(col))
                if (!seat) return (
                  <React.Fragment key={col}>
                    {i === 3 && cols.length === 6 && <div className="w-4" />}
                    <div className="w-9 h-8 mx-0.5" />
                  </React.Fragment>
                )

                const isSelected = !!selected.find(s => s.id === seat.id)
                // A seat is locked by another user only if status is LOCKED, not locked by current user, and not currently selected
                const isLockedByOther = seat.status === 'LOCKED' && !seat.isLockedByCurrentUser && !isSelected
                const isUnavailable = seat.status === 'BOOKED' || isLockedByOther
                // If a seat is selected, the user MUST be able to click it to unselect it!
                const clickable = isSelected || !isUnavailable

                return (
                  <React.Fragment key={col}>
                    {i === 3 && cols.length === 6 && <div className="w-4" />}
                    <button
                      onClick={() => clickable && onToggle(seat)}
                      disabled={!clickable}
                      title={`${seat.seatNumber} · ${seat.seatClass}${parseFloat(seat.extraPrice) > 0 ? ` · +₹${seat.extraPrice}` : ''}${isSelected ? ' (Selected - click to unselect)' : ''}`}
                      className={clsx(
                        'w-9 h-8 rounded border text-xs font-bold mx-0.5 transition-all',
                        seatStyle(seat.seatClass, isSelected, isUnavailable)
                      )}
                    >
                      {col}
                    </button>
                  </React.Fragment>
                )
              })}
            </div>
          )
        })}
    </div>
  )
}

// ─── Main page ───────────────────────────────────────────────────────────────

export default function SeatSelectionPage() {
  const { flightId }  = useParams()
  const navigate      = useNavigate()
  const location      = useLocation()

  // When coming back with state (e.g. from passenger details page)
  const prevSelected  = location.state?.selectedSeats ?? []

  const [flight,   setFlight]   = useState(null)
  const [seats,    setSeats]    = useState([])
  const [selected, setSelected] = useState(prevSelected)
  const [loading,  setLoading]  = useState(true)
  const [locking,  setLocking]  = useState(false)
  const [error,    setError]    = useState('')

  useEffect(() => {
    Promise.all([flightApi.getById(flightId), seatApi.getByFlight(flightId)])
      .then(([fr, sr]) => {
        setFlight(fr.data)
        setSeats(sr.data)

        // Find any seats currently locked by the logged-in user in the database
        const myLocked = sr.data.filter(s => s.isLockedByCurrentUser)

        if (prevSelected.length > 0) {
          // Merge state seats with any backend-locked seats for this user
          const map = new Map()
          prevSelected.forEach(s => map.set(s.id, s))
          myLocked.forEach(s => map.set(s.id, s))
          setSelected(Array.from(map.values()))
        } else if (myLocked.length > 0) {
          // Initialize selection from seats already locked by this user
          setSelected(myLocked)
        }
      })
      .catch(() => setError('Failed to load seat map'))
      .finally(() => setLoading(false))
  }, [flightId])

  /**
   * Toggle a seat:
   * - If already selected → deselect it. If it was locked in DB by this user, release it immediately.
   * - If not selected → select it.
   */
  const toggle = useCallback((seat) => {
    setSelected(prev => {
      const isAlreadySelected = !!prev.find(s => s.id === seat.id)
      if (isAlreadySelected) {
        // Deselect seat: always release lock and update local seat to AVAILABLE
        seatApi.releaseSeats([seat.id]).catch(() => {})
        setSeats(curr => curr.map(s =>
          s.id === seat.id ? { ...s, status: 'AVAILABLE', isLockedByCurrentUser: false } : s
        ))
        return prev.filter(s => s.id !== seat.id)
      } else {
        // Select seat
        return [...prev, seat]
      }
    })
  }, [])

  const handleBack = () => {
    navigate(-1)
  }

  /**
   * Proceed to passenger details:
   * Locks the selected seats atomically. The backend locks the requested seats
   * for this user and automatically releases any previously locked seats on this
   * flight that were deselected.
   */
  const handleProceed = async () => {
    if (selected.length === 0) {
      setError('Please select at least one seat')
      return
    }
    setLocking(true)
    setError('')

    try {
      const res = await seatApi.lockSeats({
        flightId: Number(flightId),
        seatIds: selected.map(s => s.id),
      })

      const lockedSeats = res.data && res.data.length > 0 ? res.data : selected

      navigate(`/booking/${flightId}/passengers`, {
        state: { selectedSeats: lockedSeats, flightId: Number(flightId) }
      })
    } catch (err) {
      setError(err.response?.data?.message ?? 'Failed to lock seats. Please try again.')
    } finally {
      setLocking(false)
    }
  }

  if (loading) return <FullPageSpinner />

  const byClass = {
    FIRST:    seats.filter(s => s.seatClass === 'FIRST'),
    BUSINESS: seats.filter(s => s.seatClass === 'BUSINESS'),
    ECONOMY:  seats.filter(s => s.seatClass === 'ECONOMY'),
  }

  // Cabin availability counts (available seats + seats locked by this user)
  const stats = {
    FIRST:    byClass.FIRST.filter(s => s.status === 'AVAILABLE' || s.isLockedByCurrentUser).length,
    BUSINESS: byClass.BUSINESS.filter(s => s.status === 'AVAILABLE' || s.isLockedByCurrentUser).length,
    ECONOMY:  byClass.ECONOMY.filter(s => s.status === 'AVAILABLE' || s.isLockedByCurrentUser).length,
  }

  const totalExtra = selected.reduce((sum, s) => sum + parseFloat(s.extraPrice ?? 0), 0)
  const basePrice  = flight ? parseFloat(flight.basePrice) : 0
  const totalPrice = (basePrice * selected.length + totalExtra).toFixed(2)

  const hasMyLocks = seats.some(s => s.isLockedByCurrentUser) || prevSelected.length > 0

  return (
    <div className="max-w-3xl mx-auto px-4 py-8">
      {/* Back button */}
      <button
        onClick={handleBack}
        className="flex items-center gap-1 text-blue-600 text-sm mb-4 hover:underline"
      >
        <ArrowLeft className="w-4 h-4" /> Back
      </button>

      {/* Notice when user has existing selection */}
      {hasMyLocks && (
        <div className="mb-4 bg-blue-50 dark:bg-blue-950/50 border border-blue-200 dark:border-blue-800 rounded-xl px-4 py-3 text-sm text-blue-800 dark:text-blue-200">
          Your selected seat(s) are highlighted in blue. You can click any seat to select or unselect it until payment is completed. Click <strong>Continue</strong> when ready.
        </div>
      )}

      {/* Flight summary */}
      {flight && (
        <div className="card mb-6 bg-blue-50/70 dark:bg-blue-950/40 border-blue-200/80 dark:border-blue-900/60">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-blue-100 dark:bg-blue-900/60 flex items-center justify-center">
                <Plane className="w-5 h-5 text-blue-600 dark:text-blue-400" />
              </div>
              <div>
                <p className="font-bold text-slate-900 dark:text-slate-100">{flight.flightNumber} · {flight.airline.name}</p>
                <p className="text-sm text-slate-500 dark:text-slate-400">
                  {flight.originAirport.city} → {flight.destinationAirport.city} ·{' '}
                  {format(new Date(flight.departureTime), 'dd MMM yyyy HH:mm')}
                </p>
              </div>
            </div>
            <p className="font-black text-blue-700 dark:text-blue-400 text-lg">₹{flight.basePrice} / person</p>
          </div>
        </div>
      )}

      <h2 className="text-xl font-bold mb-3 text-slate-900 dark:text-slate-100">Select Your Seats</h2>

      {/* Cabin availability summary */}
      <div className="grid grid-cols-3 gap-3 mb-6">
        {Object.entries(CLASS_INFO).map(([key, info]) => (
          <div key={key} className={clsx('rounded-xl border p-3.5 text-center transition-all', info.color)}>
            <p className="text-xs font-semibold">{info.label}</p>
            <p className="text-xl font-black mt-0.5">{stats[key]}</p>
            <p className="text-xs opacity-75">seats available</p>
            <p className="text-xs font-bold mt-1.5">{info.extra}</p>
          </div>
        ))}
      </div>

      {error && <Alert type="error" message={error} onClose={() => setError('')} className="mb-4" />}

      {/* Legend */}
      <div className="flex flex-wrap items-center gap-4 mb-4 text-xs">
        {[
          { label: 'Selected (Yours)', cls: 'bg-blue-600 border-blue-700' },
          { label: 'Unavailable / Taken', cls: 'bg-slate-200 dark:bg-slate-800 border-slate-300 dark:border-slate-700' },
        ].map(({ label, cls }) => (
          <div key={label} className="flex items-center gap-2">
            <div className={clsx('w-5 h-5 rounded-md border shadow-2xs', cls)} />
            <span className="text-slate-600 dark:text-slate-400 font-medium">{label}</span>
          </div>
        ))}
      </div>

      {/* Seat map */}
      <div className="card border-slate-200 dark:border-slate-800 shadow-sm">
        <div className="flex justify-center mb-6">
          <div className="flex items-center gap-2 text-slate-400 dark:text-slate-500 text-sm font-medium">
            <Plane className="w-5 h-5 rotate-[-90deg] text-blue-500" />
            <span>Front of aircraft (Cockpit)</span>
          </div>
        </div>

        <SeatGrid
          title="First Class" classKey="FIRST"
          seats={byClass.FIRST} selected={selected}
          onToggle={toggle}
        />
        <SeatGrid
          title="Business Class" classKey="BUSINESS"
          seats={byClass.BUSINESS} selected={selected}
          onToggle={toggle}
        />
        <SeatGrid
          title="Economy Class" classKey="ECONOMY"
          seats={byClass.ECONOMY} selected={selected}
          onToggle={toggle}
        />
      </div>

      {/* Cabin Class Benefits & Fare Rules */}
      <div className="mt-8 mb-6">
        <h3 className="text-lg font-bold text-slate-900 dark:text-slate-100 mb-3">
          Cabin Class Inclusions & Fare Rules
        </h3>
        <AllClassFareBenefits />
      </div>

      {/* Common Travel Information (Applies to all passengers) */}
      <CommonTravelInfo className="mb-6" />

      {/* Summary + actions */}
      <div className="card mt-6 bg-slate-50/80 dark:bg-slate-900 border-slate-200 dark:border-slate-800">
        <div className="flex flex-col sm:flex-row items-center justify-between gap-4">
          <div className="flex-1 w-full text-center sm:text-left">
            <div className="flex items-center justify-center sm:justify-start gap-2 mb-1.5">
              <Users className="w-4 h-4 text-slate-500 dark:text-slate-400" />
              <span className="font-semibold text-sm text-slate-800 dark:text-slate-200">
                {selected.length} seat{selected.length !== 1 ? 's' : ''} selected
              </span>
            </div>
            {selected.length > 0 && (
              <div className="flex flex-wrap justify-center sm:justify-start gap-1.5 mt-2">
                {selected.map(s => (
                  <span
                    key={s.id}
                    className="inline-flex items-center gap-1.5 bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-lg px-2.5 py-1 text-xs font-semibold text-slate-800 dark:text-slate-200 cursor-pointer hover:bg-rose-50 dark:hover:bg-rose-950/40 hover:border-rose-300 dark:hover:border-rose-800 transition-colors shadow-2xs"
                    onClick={() => toggle(s)}
                    title="Click to deselect"
                  >
                    {s.seatNumber} · {s.seatClass}
                    <span className="text-slate-400 hover:text-rose-500 font-bold">×</span>
                  </span>
                ))}
              </div>
            )}
            {selected.length > 0 && (
              <p className="text-2xl font-black text-blue-600 dark:text-blue-400 mt-3">Total: ₹{totalPrice}</p>
            )}
          </div>

          <div className="flex flex-col gap-2 items-center sm:items-end w-full sm:w-auto">
            <button
              onClick={handleProceed}
              disabled={selected.length === 0 || locking}
              className="btn-primary px-8 py-3 w-full sm:w-auto whitespace-nowrap text-sm"
            >
              {locking ? 'Processing…' : 'Continue to Passenger Details →'}
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}
