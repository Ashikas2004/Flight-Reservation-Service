import React, { useState, useEffect } from 'react'
import { Plus, Pencil, ToggleLeft, ToggleRight } from 'lucide-react'
import { adminApi } from '../../api/adminApi'
import { FullPageSpinner } from '../../components/common/Spinner'
import Alert from '../../components/common/Alert'
import { useForm } from 'react-hook-form'

function AirportModal({ airport, onClose, onSave }) {
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm({
    defaultValues: airport ?? { iataCode: '', name: '', city: '', country: '', timezone: '' }
  })

  const onSubmit = async (data) => { await onSave(data); onClose() }

  return (
    <div className="fixed inset-0 bg-black/60 backdrop-blur-xs flex items-center justify-center z-50 p-4">
      <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl shadow-2xl w-full max-w-md p-6">
        <h2 className="text-xl font-bold mb-4 text-slate-900 dark:text-slate-100">{airport ? 'Edit Airport' : 'Add Airport'}</h2>
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          {!airport && (
            <div>
              <label className="label">IATA Code (3 letters)</label>
              <input className="input font-mono uppercase" placeholder="JFK" maxLength={3} {...register('iataCode', { required: 'Required', minLength: 3, maxLength: 3 })} />
              {errors.iataCode && <p className="text-rose-500 text-xs mt-1">{errors.iataCode.message}</p>}
            </div>
          )}
          <div>
            <label className="label">Airport Name</label>
            <input className="input" placeholder="John F. Kennedy International" {...register('name', { required: 'Required' })} />
            {errors.name && <p className="text-rose-500 text-xs mt-1">{errors.name.message}</p>}
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="label">City</label>
              <input className="input" placeholder="New York" {...register('city', { required: 'Required' })} />
            </div>
            <div>
              <label className="label">Country</label>
              <input className="input" placeholder="United States" {...register('country', { required: 'Required' })} />
            </div>
          </div>
          <div>
            <label className="label">Timezone</label>
            <input className="input" placeholder="America/New_York" {...register('timezone')} />
          </div>
          <div className="flex gap-2 pt-2">
            <button type="submit" disabled={isSubmitting} className="btn-primary flex-1 py-2.5">
              {isSubmitting ? 'Saving…' : 'Save Airport'}
            </button>
            <button type="button" onClick={onClose} className="btn-secondary py-2.5">Cancel</button>
          </div>
        </form>
      </div>
    </div>
  )
}

export default function AdminAirportsPage() {
  const [airports, setAirports] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [modal, setModal] = useState(null)

  const load = () => {
    setLoading(true)
    adminApi.getAirports()
      .then(r => setAirports(r.data))
      .catch(() => setError('Failed to load airports'))
      .finally(() => setLoading(false))
  }

  useEffect(() => { load() }, [])

  const handleSave = async (data) => {
    try {
      if (modal === 'create') await adminApi.createAirport(data)
      else await adminApi.updateAirport(modal.id, data)
      load()
    } catch (err) {
      setError(err.response?.data?.message ?? 'Save failed')
    }
  }

  const toggleActive = async (airport) => {
    try {
      await adminApi.toggleAirport(airport.id, !airport.active)
      load()
    } catch { setError('Failed to update') }
  }

  if (loading && !airports.length) return <FullPageSpinner />

  return (
    <div className="max-w-5xl mx-auto px-4 py-8">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-3xl font-extrabold text-slate-900 dark:text-slate-100 tracking-tight">Airports</h1>
          <p className="text-slate-500 dark:text-slate-400 text-sm mt-0.5">Manage airport destinations and coordinates</p>
        </div>
        <button onClick={() => setModal('create')} className="btn-primary flex items-center gap-2 text-sm shadow-sm">
          <Plus className="w-4 h-4" /> Add Airport
        </button>
      </div>

      {error && <Alert type="error" message={error} onClose={() => setError('')} className="mb-4" />}

      <div className="card overflow-hidden p-0 border-slate-200 dark:border-slate-800 shadow-sm">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead className="bg-slate-50 dark:bg-slate-800/80 border-b border-slate-200 dark:border-slate-700/80">
              <tr>
                {['IATA', 'Name', 'City', 'Country', 'Timezone', 'Status', 'Actions'].map(h => (
                  <th key={h} className="px-4 py-3.5 text-left text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
              {airports.map(a => (
                <tr key={a.id} className="hover:bg-slate-50/80 dark:hover:bg-slate-800/40 transition-colors">
                  <td className="px-4 py-3.5 font-mono font-bold text-blue-600 dark:text-blue-400">{a.iataCode}</td>
                  <td className="px-4 py-3.5 font-semibold text-slate-800 dark:text-slate-200 max-w-[200px] truncate">{a.name}</td>
                  <td className="px-4 py-3.5 text-slate-700 dark:text-slate-300 font-medium">{a.city}</td>
                  <td className="px-4 py-3.5 text-slate-500 dark:text-slate-400 font-medium">{a.country}</td>
                  <td className="px-4 py-3.5 text-slate-400 dark:text-slate-500 text-xs font-mono">{a.timezone}</td>
                  <td className="px-4 py-3.5">
                    <span className={`badge border font-medium ${
                      a.active
                        ? 'bg-emerald-100 text-emerald-800 border-emerald-200 dark:bg-emerald-950/60 dark:text-emerald-300 dark:border-emerald-800'
                        : 'bg-slate-100 text-slate-600 border-slate-200 dark:bg-slate-800 dark:text-slate-400 dark:border-slate-700'
                    }`}>
                      {a.active ? 'Active' : 'Inactive'}
                    </span>
                  </td>
                  <td className="px-4 py-3.5">
                    <div className="flex items-center gap-2">
                      <button onClick={() => setModal(a)} className="p-1.5 rounded-lg text-slate-400 hover:text-blue-600 hover:bg-blue-50 dark:hover:bg-blue-950/60 transition-colors cursor-pointer" title="Edit Airport">
                        <Pencil className="w-4 h-4" />
                      </button>
                      <button onClick={() => toggleActive(a)} className="p-1 rounded-lg text-slate-400 hover:text-blue-600 transition-colors cursor-pointer" title="Toggle active">
                        {a.active
                          ? <ToggleRight className="w-6 h-6 text-emerald-500" />
                          : <ToggleLeft className="w-6 h-6 text-slate-400" />}
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
              {airports.length === 0 && (
                <tr>
                  <td colSpan={7} className="text-center py-12 text-slate-400 dark:text-slate-500">No airports configured</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {modal && (
        <AirportModal airport={modal === 'create' ? null : modal} onClose={() => setModal(null)} onSave={handleSave} />
      )}
    </div>
  )
}
