import React, { useState, useEffect } from 'react'
import { Search } from 'lucide-react'
import { adminApi } from '../../api/adminApi'
import { FullPageSpinner } from '../../components/common/Spinner'
import Alert from '../../components/common/Alert'
import { format } from 'date-fns'

const ALLOWED_ACTIONS = [
  { value: '', label: 'All Audited Actions' },
  { value: 'BOOKING_CONFIRMED', label: 'Booking Confirmed' },
  { value: 'BOOKING_CANCELLED', label: 'Booking Cancelled' },
  { value: 'PAYMENT_SUCCESS', label: 'Payment Success' },
  { value: 'REFUND_PROCESSED', label: 'Refund Processed' },
  { value: 'REFUND_FAILED', label: 'Refund Failed' },
  { value: 'FLIGHT_CREATED', label: 'Flight Created' },
  { value: 'FLIGHT_UPDATED', label: 'Flight Updated' },
  { value: 'FLIGHT_CANCELLED', label: 'Flight Cancelled' },
  { value: 'USER_REGISTERED', label: 'User Registered' },
]

const ALLOWED_ACTION_SET = new Set(ALLOWED_ACTIONS.map(a => a.value).filter(Boolean))

const ENTITY_TYPES = ['', 'Booking', 'Payment', 'Refund', 'Flight', 'User']

const actionLabels = {
  BOOKING_CONFIRMED: 'Booking Confirmed',
  BOOKING_CANCELLED: 'Booking Cancelled',
  PAYMENT_SUCCESS: 'Payment Success',
  REFUND_PROCESSED: 'Refund Processed',
  REFUND_FAILED: 'Refund Failed',
  FLIGHT_CREATED: 'Flight Created',
  FLIGHT_UPDATED: 'Flight Updated',
  FLIGHT_CANCELLED: 'Flight Cancelled',
  USER_REGISTERED: 'User Registered',
}

const actionColors = {
  BOOKING_CONFIRMED: 'text-emerald-700 bg-emerald-100 border border-emerald-200 dark:bg-emerald-950/60 dark:text-emerald-300 dark:border-emerald-800',
  BOOKING_CANCELLED: 'text-rose-700 bg-rose-100 border border-rose-200 dark:bg-rose-950/60 dark:text-rose-300 dark:border-rose-800',
  PAYMENT_SUCCESS: 'text-emerald-700 bg-emerald-100 border border-emerald-200 dark:bg-emerald-950/60 dark:text-emerald-300 dark:border-emerald-800',
  REFUND_PROCESSED: 'text-purple-700 bg-purple-100 border border-purple-200 dark:bg-purple-950/60 dark:text-purple-300 dark:border-purple-800',
  REFUND_FAILED: 'text-rose-700 bg-rose-100 border border-rose-200 dark:bg-rose-950/60 dark:text-rose-300 dark:border-rose-800',
  FLIGHT_CREATED: 'text-blue-700 bg-blue-100 border border-blue-200 dark:bg-blue-950/60 dark:text-blue-300 dark:border-blue-800',
  FLIGHT_UPDATED: 'text-amber-700 bg-amber-100 border border-amber-200 dark:bg-amber-950/60 dark:text-amber-300 dark:border-amber-800',
  FLIGHT_CANCELLED: 'text-rose-700 bg-rose-100 border border-rose-200 dark:bg-rose-950/60 dark:text-rose-300 dark:border-rose-800',
  USER_REGISTERED: 'text-teal-700 bg-teal-100 border border-teal-200 dark:bg-teal-950/60 dark:text-teal-300 dark:border-teal-800',
}

export default function AdminAuditPage() {
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [page, setPage] = useState(0)
  const [filters, setFilters] = useState({
    entityType: '', action: '', performedBy: '',
  })

  const load = (p = 0, f = filters) => {
    setLoading(true)
    const params = { page: p, size: 20, ...f }
    Object.keys(params).forEach(k => { if (!params[k]) delete params[k] })
    adminApi.getAuditLogs(params)
      .then(r => { setData(r.data); setPage(p) })
      .catch(() => setError('Failed to load audit logs'))
      .finally(() => setLoading(false))
  }

  useEffect(() => { load() }, [])

  const handleSearch = (e) => {
    e.preventDefault()
    load(0)
  }

  const logs = data?.content?.filter(log => ALLOWED_ACTION_SET.has(log.action)) ?? []

  return (
    <div className="max-w-7xl mx-auto px-4 py-8">
      <h1 className="text-3xl font-extrabold mb-6 text-slate-900 dark:text-slate-100 tracking-tight">Audit Logs</h1>

      {error && <Alert type="error" message={error} onClose={() => setError('')} className="mb-4" />}

      {/* Filters */}
      <form onSubmit={handleSearch} className="card mb-6 border-slate-200 dark:border-slate-800 shadow-sm">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          <div>
            <label className="label">Entity Type</label>
            <select
              className="input"
              value={filters.entityType}
              onChange={e => setFilters(prev => ({ ...prev, entityType: e.target.value }))}
            >
              {ENTITY_TYPES.map(t => <option key={t} value={t}>{t || 'All types'}</option>)}
            </select>
          </div>
          <div>
            <label className="label">Action</label>
            <select
              className="input"
              value={filters.action}
              onChange={e => setFilters(prev => ({ ...prev, action: e.target.value }))}
            >
              {ALLOWED_ACTIONS.map(a => <option key={a.value} value={a.value}>{a.label}</option>)}
            </select>
          </div>
          <div>
            <label className="label">Performed By</label>
            <input
              className="input"
              placeholder="user@example.com"
              value={filters.performedBy}
              onChange={e => setFilters(prev => ({ ...prev, performedBy: e.target.value }))}
            />
          </div>
          <div className="flex items-end">
            <button type="submit" className="btn-primary w-full flex items-center justify-center gap-2 py-2.5">
              <Search className="w-4 h-4" />
              Search Logs
            </button>
          </div>
        </div>
      </form>

      {/* Results */}
      {loading ? (
        <FullPageSpinner />
      ) : (
        <>
          {data?.totalElements !== undefined && (
            <p className="text-sm font-medium text-slate-500 dark:text-slate-400 mb-3">{data.totalElements} audit log entries found</p>
          )}

          <div className="card overflow-hidden p-0 border-slate-200 dark:border-slate-800 shadow-sm">
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead className="bg-slate-50 dark:bg-slate-800/80 border-b border-slate-200 dark:border-slate-700/80">
                  <tr>
                    {['Time', 'Action', 'Entity', 'Performed By', 'Description'].map(h => (
                      <th key={h} className="px-4 py-3.5 text-left text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider">{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
                  {logs.map(log => (
                    <tr key={log.id} className="hover:bg-slate-50/80 dark:hover:bg-slate-800/40 transition-colors">
                      <td className="px-4 py-3 text-xs text-slate-400 dark:text-slate-500 whitespace-nowrap font-mono">
                        {format(new Date(log.createdAt), 'dd MMM HH:mm:ss')}
                      </td>
                      <td className="px-4 py-3">
                        <span className={`badge text-xs font-semibold ${actionColors[log.action] ?? 'bg-slate-100 text-slate-600 dark:bg-slate-800 dark:text-slate-300'}`}>
                          {actionLabels[log.action] || log.action}
                        </span>
                      </td>
                      <td className="px-4 py-3 font-medium text-slate-700 dark:text-slate-300">
                        {log.entityType}{log.entityId ? ` #${log.entityId}` : ''}
                      </td>
                      <td className="px-4 py-3 text-slate-500 dark:text-slate-400 text-xs font-mono max-w-[160px] truncate">{log.performedBy}</td>
                      <td className="px-4 py-3 text-slate-600 dark:text-slate-400 text-xs max-w-[300px] truncate" title={log.description}>
                        {log.description}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {!logs.length && (
              <div className="text-center py-12 text-slate-400 dark:text-slate-500">No audit logs found</div>
            )}
          </div>

          {/* Pagination */}
          {data?.totalPages > 1 && (
            <div className="flex justify-center gap-3 mt-6">
              <button
                className="btn-secondary text-sm"
                disabled={page === 0}
                onClick={() => load(page - 1)}
              >
                Previous
              </button>
              <span className="text-sm font-medium text-slate-500 dark:text-slate-400 self-center">
                Page {page + 1} of {data.totalPages}
              </span>
              <button
                className="btn-secondary text-sm"
                disabled={data.last}
                onClick={() => load(page + 1)}
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
