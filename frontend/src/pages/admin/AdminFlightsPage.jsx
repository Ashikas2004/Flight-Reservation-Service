import React, { useState, useEffect, useRef } from 'react'
import { Plus, Pencil, Settings, Armchair } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import { adminApi } from '../../api/adminApi'
import { FullPageSpinner } from '../../components/common/Spinner'
import Alert from '../../components/common/Alert'
import StatusBadge from '../../components/common/StatusBadge'
import { useForm } from 'react-hook-form'
import { addDays, format, getISODay, parseISO } from 'date-fns'

const STATUSES = ['SCHEDULED', 'DELAYED', 'CANCELLED', 'COMPLETED']
const ROUTINES = [
  { value: 'DAILY', label: 'Daily' },
  { value: 'WEEKLY', label: 'Weekly' },
  { value: 'ALTERNATE_ODD', label: 'Alternate odd dates' },
  { value: 'ALTERNATE_EVEN', label: 'Alternate even dates' },
  { value: 'MONTHLY', label: 'Monthly' },
  { value: 'SPECIAL_ADDITIONAL', label: 'Special' },
]
const WEEKDAYS = [
  { value: 1, label: 'Monday' },
  { value: 2, label: 'Tuesday' },
  { value: 3, label: 'Wednesday' },
  { value: 4, label: 'Thursday' },
  { value: 5, label: 'Friday' },
  { value: 6, label: 'Saturday' },
  { value: 7, label: 'Sunday' },
]
const routineLabel = (routine) => ROUTINES.find(option => option.value === routine)?.label ?? ROUTINES[5].label

function FlightModal({ flight, airlines, airports, onClose, onSave }) {
  const { register, handleSubmit, watch, setValue, formState: { errors, isSubmitting } } = useForm({
    defaultValues: flight ? {
      flightNumber: flight.flightNumber,
      airlineId: flight.airline?.id,
      originAirportId: flight.originAirport?.id,
      destinationAirportId: flight.destinationAirport?.id,
      departureTime: flight.departureTime?.slice(0, 16),
      arrivalTime: flight.arrivalTime?.slice(0, 16),
      scheduleRoutine: flight.scheduleRoutine ?? 'SPECIAL_ADDITIONAL',
      recurrenceEndDate: flight.recurrenceEndDate ?? '',
      weeklyDay: flight.weeklyDay ?? (flight.departureTime ? getISODay(parseISO(flight.departureTime)) : ''),
      monthlyDay: flight.monthlyDay ?? (flight.departureTime ? Number(flight.departureTime.slice(8, 10)) : ''),
      basePrice: flight.basePrice,
      firstClassSeats: 8,
      businessSeats: 36,
      economySeats: 136,
    } : {
      firstClassSeats: 8,
      businessSeats: 36,
      economySeats: 136,
      scheduleRoutine: 'SPECIAL_ADDITIONAL',
      weeklyDay: '',
      monthlyDay: '',
    }
  })

  const departureTime = watch('departureTime')
  const scheduleRoutine = watch('scheduleRoutine')
  const autoEndDate = useRef('')
  const autoWeeklyDay = useRef(null)
  const autoMonthlyDay = useRef(null)
  useEffect(() => {
    if (departureTime && scheduleRoutine !== 'SPECIAL_ADDITIONAL') {
      const defaultEndDate = format(addDays(new Date(departureTime), 59), 'yyyy-MM-dd')
      const currentEndDate = watch('recurrenceEndDate')
      if (!currentEndDate || currentEndDate === autoEndDate.current) {
        setValue('recurrenceEndDate', defaultEndDate)
        autoEndDate.current = defaultEndDate
      }
    }
  }, [departureTime, scheduleRoutine, flight, setValue, watch])
  useEffect(() => {
    if (departureTime && scheduleRoutine === 'WEEKLY') {
      const currentWeeklyDay = watch('weeklyDay')
      const departureWeekday = getISODay(parseISO(departureTime))
      if (!currentWeeklyDay || Number(currentWeeklyDay) === autoWeeklyDay.current) {
        setValue('weeklyDay', departureWeekday)
        autoWeeklyDay.current = departureWeekday
      }
    }
  }, [departureTime, scheduleRoutine, setValue, watch])
  useEffect(() => {
    if (departureTime && scheduleRoutine === 'MONTHLY') {
      const currentMonthlyDay = watch('monthlyDay')
      const departureMonthDay = Number(departureTime.slice(8, 10))
      if (!currentMonthlyDay || Number(currentMonthlyDay) === autoMonthlyDay.current) {
        setValue('monthlyDay', departureMonthDay)
        autoMonthlyDay.current = departureMonthDay
      }
    }
  }, [departureTime, scheduleRoutine, setValue, watch])

  const first    = parseInt(watch('firstClassSeats') || 0)
  const business = parseInt(watch('businessSeats')   || 0)
  const economy  = parseInt(watch('economySeats')    || 0)
  const total    = (Math.ceil(first / 4) * 4) + (Math.ceil(business / 6) * 6) + (Math.ceil(economy / 6) * 6)

  return (
    <div className="fixed inset-0 bg-black/60 backdrop-blur-xs flex items-center justify-center z-50 p-4 overflow-y-auto">
      <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl shadow-2xl w-full max-w-lg p-6 my-4">
        <h2 className="text-xl font-bold mb-4 text-slate-900 dark:text-slate-100">{flight ? 'Edit Flight' : 'Create Flight'}</h2>
        <form onSubmit={handleSubmit(d => onSave(d).then(onClose))} className="space-y-3.5">

          {/* Flight number — only on create */}
          {!flight && (
            <div>
              <label className="label">Flight Number</label>
              <input className="input" placeholder="AA101"
                {...register('flightNumber', { required: 'Required' })} />
              {errors.flightNumber && <p className="text-rose-500 text-xs mt-1">{errors.flightNumber.message}</p>}
            </div>
          )}

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="label">Airline</label>
              <select className="input" {...register('airlineId', { required: 'Required' })}>
                <option value="">Select airline</option>
                {airlines.map(a => <option key={a.id} value={a.id}>{a.code} – {a.name}</option>)}
              </select>
            </div>
            <div>
              <label className="label">Base Economy Price (₹)</label>
              <input type="number" step="0.01" min="0.01" className="input"
                placeholder="299.00"
                {...register('basePrice', { required: 'Required' })} />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="label">Origin Airport</label>
              <select className="input" {...register('originAirportId', { required: 'Required' })}>
                <option value="">Select origin</option>
                {airports.map(a => <option key={a.id} value={a.id}>{a.iataCode} – {a.city}</option>)}
              </select>
            </div>
            <div>
              <label className="label">Destination Airport</label>
              <select className="input" {...register('destinationAirportId', { required: 'Required' })}>
                <option value="">Select destination</option>
                {airports.map(a => <option key={a.id} value={a.id}>{a.iataCode} – {a.city}</option>)}
              </select>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="label">Departure Time</label>
              <input type="datetime-local" className="input"
                {...register('departureTime', { required: 'Required' })} />
            </div>
            <div>
              <label className="label">Arrival Time</label>
              <input type="datetime-local" className="input"
                {...register('arrivalTime', { required: 'Required' })} />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="label" htmlFor="scheduleRoutine">Flight Routine</label>
              <select id="scheduleRoutine" className="input" {...register('scheduleRoutine', { required: 'Select a flight routine' })}>
                {ROUTINES.map(routine => <option key={routine.value} value={routine.value}>{routine.label}</option>)}
              </select>
            </div>
            {scheduleRoutine !== 'SPECIAL_ADDITIONAL' && (
              <div>
                <label className="label" htmlFor="recurrenceEndDate">Schedule Through</label>
                <input
                  id="recurrenceEndDate"
                  type="date"
                  className="input"
                  min={departureTime?.slice(0, 10)}
                  {...register('recurrenceEndDate', { required: 'Choose the last schedule date' })}
                />
              </div>
            )}
            {scheduleRoutine === 'WEEKLY' && (
              <div>
                <label className="label" htmlFor="weeklyDay">Repeat on</label>
                <select
                  id="weeklyDay"
                  className="input"
                  {...register('weeklyDay', { required: 'Choose a weekday', valueAsNumber: true })}
                >
                  {WEEKDAYS.map(day => <option key={day.value} value={day.value}>{day.label}</option>)}
                </select>
                {errors.weeklyDay && <p className="text-rose-500 text-xs mt-1">{errors.weeklyDay.message}</p>}
              </div>
            )}
            {scheduleRoutine === 'MONTHLY' && (
              <div>
                <label className="label" htmlFor="monthlyDay">Repeat on date</label>
                <input
                  id="monthlyDay"
                  type="number"
                  min="1"
                  max="31"
                  className="input"
                  {...register('monthlyDay', {
                    required: 'Choose a date from 1 to 31',
                    valueAsNumber: true,
                    min: { value: 1, message: 'Date must be from 1 to 31' },
                    max: { value: 31, message: 'Date must be from 1 to 31' },
                  })}
                />
                {errors.monthlyDay && <p className="text-rose-500 text-xs mt-1">{errors.monthlyDay.message}</p>}
              </div>
            )}
          </div>
          <p className="text-xs text-slate-500 dark:text-slate-400">
            {scheduleRoutine === 'SPECIAL_ADDITIONAL'
              ? 'Creates only this additional flight.'
              : 'The entered departure is always created first; later flights follow this routine through the selected end date (60 days by default).'}
          </p>

          {/* Seat class breakdown — only on create */}
          {!flight && (
            <>
              <div className="border-t border-slate-150 dark:border-slate-800 pt-3">
                <p className="text-sm font-bold text-slate-800 dark:text-slate-200 mb-1">Seat Configuration</p>
                <p className="text-xs text-slate-400 dark:text-slate-500 mb-3">
                  First uses 4 seats/row (A-D) · Business & Economy use 6 seats/row (A-F)
                </p>
                <div className="grid grid-cols-3 gap-3">
                  <div>
                    <label className="label">
                      <span className="inline-block w-2.5 h-2.5 rounded-sm bg-amber-400 mr-1" />
                      First Class
                    </label>
                    <input type="number" min="0" step="4" className="input"
                      {...register('firstClassSeats', { required: 'Required', min: 0 })} />
                    <p className="text-xs text-slate-400 dark:text-slate-500 mt-1">+₹6,000/seat</p>
                  </div>
                  <div>
                    <label className="label">
                      <span className="inline-block w-2.5 h-2.5 rounded-sm bg-blue-400 mr-1" />
                      Business
                    </label>
                    <input type="number" min="0" step="6" className="input"
                      {...register('businessSeats', { required: 'Required', min: 0 })} />
                    <p className="text-xs text-slate-400 dark:text-slate-500 mt-1">+₹3,000/seat</p>
                  </div>
                  <div>
                    <label className="label">
                      <span className="inline-block w-2.5 h-2.5 rounded-sm bg-emerald-400 mr-1" />
                      Economy
                    </label>
                    <input type="number" min="6" step="6" className="input"
                      {...register('economySeats', { required: 'Required', min: 1 })} />
                    <p className="text-xs text-slate-400 dark:text-slate-500 mt-1">Included</p>
                  </div>
                </div>

                {/* Live total */}
                <div className="mt-3 bg-slate-50 dark:bg-slate-800/80 border border-slate-100 dark:border-slate-800 rounded-xl px-3.5 py-2 flex justify-between text-sm">
                  <span className="text-slate-500 dark:text-slate-400">Total seats generated:</span>
                  <span className="font-bold text-slate-800 dark:text-slate-200">{total || 0}</span>
                </div>
              </div>
            </>
          )}

          <div className="flex gap-2 pt-3">
            <button type="submit" disabled={isSubmitting} className="btn-primary flex-1 py-2.5">
              {isSubmitting ? 'Saving…' : 'Save Flight'}
            </button>
            <button type="button" onClick={onClose} className="btn-secondary py-2.5">Cancel</button>
          </div>
        </form>
      </div>
    </div>
  )
}

export default function AdminFlightsPage() {
  const navigate = useNavigate()
  const [flights, setFlights] = useState([])
  const [airlines, setAirlines] = useState([])
  const [airports, setAirports] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [modal, setModal] = useState(null)
  const [statusModal, setStatusModal] = useState(null)

  const load = (showLoader = true) => {
    if (showLoader) setLoading(true)
    Promise.all([
      adminApi.getAllFlights(0, 100),
      adminApi.getAirlines(),
      adminApi.getAirports(),
    ]).then(([fr, ar, pr]) => {
      setFlights(fr.data.content ?? [])
      setAirlines(ar.data)
      setAirports(pr.data)
    }).catch(() => setError('Failed to load data'))
      .finally(() => {
        if (showLoader) setLoading(false)
      })
  }

  useEffect(() => {
    load()
    const refreshInterval = setInterval(() => load(false), 60_000)
    return () => clearInterval(refreshInterval)
  }, [])

  const handleSave = async (data) => {
    const payload = {
      flightNumber:         data.flightNumber,
      airlineId:            Number(data.airlineId),
      originAirportId:      Number(data.originAirportId),
      destinationAirportId: Number(data.destinationAirportId),
      departureTime:        data.departureTime,
      arrivalTime:          data.arrivalTime,
      scheduleRoutine:      data.scheduleRoutine,
      recurrenceEndDate:    data.scheduleRoutine === 'SPECIAL_ADDITIONAL' ? null : data.recurrenceEndDate,
      weeklyDay:            data.scheduleRoutine === 'WEEKLY' ? Number(data.weeklyDay) : null,
      monthlyDay:           data.scheduleRoutine === 'MONTHLY' ? Number(data.monthlyDay) : null,
      basePrice:            parseFloat(data.basePrice),
      firstClassSeats:      parseInt(data.firstClassSeats  || 0),
      businessSeats:        parseInt(data.businessSeats    || 0),
      economySeats:         parseInt(data.economySeats     || 0),
    }
    if (modal === 'create') await adminApi.createFlight(payload)
    else await adminApi.updateFlight(modal.id, payload)
    load()
  }

  const updateStatus = async (id, status) => {
    await adminApi.updateFlightStatus(id, status)
    setStatusModal(null)
    load()
  }

  if (loading && !flights.length) return <FullPageSpinner />

  return (
    <div className="max-w-6xl mx-auto px-4 py-8">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-3xl font-extrabold text-slate-900 dark:text-slate-100 tracking-tight">Flights</h1>
          <p className="text-slate-500 dark:text-slate-400 text-sm mt-0.5">Create and manage scheduled flight routes</p>
        </div>
        <button onClick={() => setModal('create')} className="btn-primary flex items-center gap-2 text-sm shadow-sm">
          <Plus className="w-4 h-4" /> Create Flight
        </button>
      </div>

      {error && <Alert type="error" message={error} onClose={() => setError('')} className="mb-4" />}

      <div className="card overflow-hidden p-0 border-slate-200 dark:border-slate-800 shadow-sm">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead className="bg-slate-50 dark:bg-slate-800/80 border-b border-slate-200 dark:border-slate-700/80">
              <tr>
                {['Flight', 'Airline', 'Route', 'Departure', 'Routine', 'Base Price', 'Seats', 'Status', 'Actions'].map(h => (
                  <th key={h} className="px-4 py-3.5 text-left text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
              {flights.map(f => (
                <tr key={f.id} className="hover:bg-slate-50/80 dark:hover:bg-slate-800/40 transition-colors">
                  <td className="px-4 py-3.5 font-mono font-bold text-blue-600 dark:text-blue-400">{f.flightNumber}</td>
                  <td className="px-4 py-3.5 font-medium text-slate-700 dark:text-slate-300">{f.airline?.name}</td>
                  <td className="px-4 py-3.5">
                    <span className="font-semibold text-slate-800 dark:text-slate-200">{f.originAirport?.city || f.originAirport?.iataCode}</span>
                    <span className="text-slate-400 dark:text-slate-500 mx-1.5 font-mono">→</span>
                    <span className="font-semibold text-slate-800 dark:text-slate-200">{f.destinationAirport?.city || f.destinationAirport?.iataCode}</span>
                  </td>
                  <td className="px-4 py-3.5 text-slate-500 dark:text-slate-400 text-xs whitespace-nowrap font-medium">
                    {f.departureTime ? format(new Date(f.departureTime), 'dd MMM HH:mm') : '—'}
                  </td>
                  <td className="px-4 py-3.5 text-slate-500 dark:text-slate-400 text-xs whitespace-nowrap font-medium">
                    {routineLabel(f.scheduleRoutine)}
                  </td>
                  <td className="px-4 py-3.5 font-bold text-slate-900 dark:text-slate-100">₹{f.basePrice}</td>
                  <td className="px-4 py-3.5 text-slate-500 dark:text-slate-400 font-medium">{f.availableSeats}/{f.totalSeats}</td>
                  <td className="px-4 py-3.5"><StatusBadge status={f.status} /></td>
                  <td className="px-4 py-3.5">
                    <div className="flex items-center gap-1.5">
                      <button onClick={() => setModal(f)} title="Edit Flight" className="p-1.5 rounded-lg text-slate-400 hover:text-blue-600 hover:bg-blue-50 dark:hover:bg-blue-950/60 transition-colors cursor-pointer">
                        <Pencil className="w-4 h-4" />
                      </button>
                      <button onClick={() => setStatusModal(f)} title="Change Status" className="p-1.5 rounded-lg text-slate-400 hover:text-amber-600 hover:bg-amber-50 dark:hover:bg-amber-950/60 transition-colors cursor-pointer">
                        <Settings className="w-4 h-4" />
                      </button>
                      <button onClick={() => navigate(`/admin/seats/${f.id}`)} title="Manage Seats" className="p-1.5 rounded-lg text-slate-400 hover:text-emerald-600 hover:bg-emerald-50 dark:hover:bg-emerald-950/60 transition-colors cursor-pointer">
                        <Armchair className="w-4 h-4" />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
              {flights.length === 0 && (
                <tr>
                  <td colSpan={9} className="text-center py-12 text-slate-400 dark:text-slate-500">No flights found</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {modal && (
        <FlightModal
          flight={modal === 'create' ? null : modal}
          airlines={airlines}
          airports={airports}
          onClose={() => setModal(null)}
          onSave={handleSave}
        />
      )}

      {statusModal && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-xs flex items-center justify-center z-50 p-4">
          <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl shadow-2xl w-80 p-6">
            <h2 className="font-bold text-lg mb-4 text-slate-900 dark:text-slate-100">Update Status — {statusModal.flightNumber}</h2>
            <div className="space-y-2">
              {STATUSES.map(s => (
                <button key={s} onClick={() => updateStatus(statusModal.id, s)}
                  className={`w-full text-left px-3.5 py-2.5 rounded-xl text-sm font-semibold transition-all cursor-pointer ${
                    s === statusModal.status
                      ? 'bg-blue-600 text-white shadow-xs'
                      : 'hover:bg-slate-100 dark:hover:bg-slate-800 text-slate-700 dark:text-slate-200'
                  }`}>
                  {s}
                </button>
              ))}
            </div>
            <button onClick={() => setStatusModal(null)} className="btn-secondary w-full mt-4 text-sm py-2">Cancel</button>
          </div>
        </div>
      )}
    </div>
  )
}
