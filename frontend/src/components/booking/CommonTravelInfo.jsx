import React from 'react'
import {
  Info,
  Utensils,
  Sparkles,
  Ban,
  CigaretteOff,
  ShieldCheck,
  Smartphone,
  Luggage,
  AlertTriangle,
  UserCheck
} from 'lucide-react'

export const COMMON_TRAVEL_GUIDELINES = [
  {
    id: 1,
    title: 'Meals & Beverages',
    text: 'Complimentary meals and beverages are provided.',
    icon: Utensils,
    badgeColor: 'text-emerald-600 dark:text-emerald-400 bg-emerald-50 dark:bg-emerald-950/60 border-emerald-200 dark:border-emerald-800',
  },
  {
    id: 2,
    title: 'Cleanliness & Sanitation',
    text: 'Cabin and seating areas are regularly cleaned and sanitized.',
    icon: Sparkles,
    badgeColor: 'text-blue-600 dark:text-blue-400 bg-blue-50 dark:bg-blue-950/60 border-blue-200 dark:border-blue-800',
  },
  {
    id: 3,
    title: 'Pet Policy',
    text: 'Pets are not permitted in the passenger cabin.',
    icon: Ban,
    badgeColor: 'text-amber-600 dark:text-amber-400 bg-amber-50 dark:bg-amber-950/60 border-amber-200 dark:border-amber-800',
  },
  {
    id: 4,
    title: 'No Smoking / Vaping',
    text: 'Smoking and vaping are strictly prohibited onboard.',
    icon: CigaretteOff,
    badgeColor: 'text-rose-600 dark:text-rose-400 bg-rose-50 dark:bg-rose-950/60 border-rose-200 dark:border-rose-800',
  },
  {
    id: 5,
    title: 'Seat Belt Protocol',
    text: 'Keep your seat belt fastened whenever instructed by the crew.',
    icon: ShieldCheck,
    badgeColor: 'text-indigo-600 dark:text-indigo-400 bg-indigo-50 dark:bg-indigo-950/60 border-indigo-200 dark:border-indigo-800',
  },
  {
    id: 6,
    title: 'Crew Operational Guidelines',
    text: 'Follow all safety and operational instructions from the cabin crew.',
    icon: UserCheck,
    badgeColor: 'text-blue-600 dark:text-blue-400 bg-blue-50 dark:bg-blue-950/60 border-blue-200 dark:border-blue-800',
  },
  {
    id: 7,
    title: 'Electronic Devices',
    text: 'Mobile phones, laptops, and other devices must be used in airplane mode when required.',
    icon: Smartphone,
    badgeColor: 'text-purple-600 dark:text-purple-400 bg-purple-50 dark:bg-purple-950/60 border-purple-200 dark:border-purple-800',
  },
  {
    id: 8,
    title: 'Baggage Permissibility',
    text: 'Only permitted items within the specified baggage limits can be carried onboard.',
    icon: Luggage,
    badgeColor: 'text-teal-600 dark:text-teal-400 bg-teal-50 dark:bg-teal-950/60 border-teal-200 dark:border-teal-800',
  },
  {
    id: 9,
    title: 'Emergency Readiness',
    text: 'Follow the safety briefing and crew instructions during an emergency.',
    icon: AlertTriangle,
    badgeColor: 'text-amber-600 dark:text-amber-400 bg-amber-50 dark:bg-amber-950/60 border-amber-200 dark:border-amber-800',
  },
]

export default function CommonTravelInfo({ className = '' }) {
  return (
    <div className={`card border-slate-200/90 dark:border-slate-800 p-6 ${className}`}>
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between pb-4 mb-4 border-b border-slate-100 dark:border-slate-800 gap-2">
        <div className="flex items-center gap-2.5">
          <div className="w-8 h-8 rounded-xl bg-blue-100 dark:bg-blue-950/80 text-blue-600 dark:text-blue-400 flex items-center justify-center shrink-0 shadow-xs">
            <Info className="w-4 h-4" />
          </div>
          <div>
            <h4 className="font-bold text-base text-slate-900 dark:text-slate-100">
              Common Travel Information
            </h4>
            <p className="text-xs text-slate-500 dark:text-slate-400">
              Applies to First Class, Business Class, and Economy Class
            </p>
          </div>
        </div>
        <span className="self-start sm:self-center text-[11px] font-semibold tracking-wide uppercase px-2.5 py-1 rounded-full bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-400 border border-slate-200 dark:border-slate-700">
          All Cabin Classes
        </span>
      </div>

      {/* Guidelines - Strictly Line by Line */}
      <div className="flex flex-col divide-y divide-slate-100 dark:divide-slate-800/80">
        {COMMON_TRAVEL_GUIDELINES.map((item) => {
          const Icon = item.icon
          return (
            <div
              key={item.id}
              className="flex items-start sm:items-center gap-3.5 py-3 first:pt-1 last:pb-1"
            >
              <div
                className={`w-7 h-7 rounded-lg flex items-center justify-center shrink-0 border shadow-2xs mt-0.5 sm:mt-0 ${item.badgeColor}`}
              >
                <Icon className="w-3.5 h-3.5" />
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-xs sm:text-sm text-slate-700 dark:text-slate-300 font-medium leading-relaxed">
                  <span className="font-bold text-slate-900 dark:text-slate-100 mr-1.5">
                    {item.id}.
                  </span>
                  {item.text}
                </p>
              </div>
            </div>
          )
        })}
      </div>
    </div>
  )
}