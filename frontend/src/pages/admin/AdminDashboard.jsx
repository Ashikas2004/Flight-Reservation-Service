import React from 'react'
import { useNavigate } from 'react-router-dom'
import { Plane, MapPin, Building2, FileText, ArrowRight } from 'lucide-react'

const cards = [
  { icon: Building2, label: 'Airlines',  desc: 'Manage airline carriers and fleets',          path: '/admin/airlines',  color: 'bg-blue-100 text-blue-600 dark:bg-blue-950/60 dark:text-blue-300' },
  { icon: MapPin,    label: 'Airports',  desc: 'Manage airport destinations and codes',       path: '/admin/airports',  color: 'bg-emerald-100 text-emerald-600 dark:bg-emerald-950/60 dark:text-emerald-300' },
  { icon: Plane,     label: 'Flights',   desc: 'Create, schedule, and configure flights',     path: '/admin/flights',   color: 'bg-purple-100 text-purple-600 dark:bg-purple-950/60 dark:text-purple-300' },
  { icon: FileText,  label: 'Audit Log', desc: 'Inspect platform administrative events',      path: '/admin/audit',     color: 'bg-amber-100 text-amber-600 dark:bg-amber-950/60 dark:text-amber-300' },
]

export default function AdminDashboard() {
  const navigate = useNavigate()
  return (
    <div className="max-w-5xl mx-auto px-4 py-8">
      <h1 className="text-3xl font-extrabold mb-2 text-slate-900 dark:text-slate-100 tracking-tight">Admin Dashboard</h1>
      <p className="text-slate-500 dark:text-slate-400 mb-8 text-sm">System administration and service management control center.</p>
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
        {cards.map(({ icon: Icon, label, desc, path, color }) => (
          <button
            key={label}
            onClick={() => navigate(path)}
            className="card text-left hover:shadow-lg dark:hover:shadow-slate-900/60 hover:-translate-y-1 transition-all cursor-pointer border-slate-200 dark:border-slate-800 group"
          >
            <div className={`w-12 h-12 rounded-2xl flex items-center justify-center mb-5 ${color} shadow-xs group-hover:scale-105 transition-transform`}>
              <Icon className="w-6 h-6" />
            </div>
            <h2 className="font-bold text-lg text-slate-900 dark:text-slate-100 mb-1">{label}</h2>
            <p className="text-xs text-slate-500 dark:text-slate-400 leading-relaxed mb-4">{desc}</p>
            <span className="inline-flex items-center gap-1 text-xs font-semibold text-blue-600 dark:text-blue-400 group-hover:translate-x-0.5 transition-transform">
              Access portal <ArrowRight className="w-3.5 h-3.5" />
            </span>
          </button>
        ))}
      </div>
    </div>
  )
}
