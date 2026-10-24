import React from 'react'
import { Luggage, ShieldAlert, Sparkles, Check, X, Minus } from 'lucide-react'

export const CLASS_FARE_BENEFITS = {
  FIRST: {
    key: 'FIRST',
    title: 'FIRST CLASS',
    subtitle: 'Premium Experience & Maximum Flexibility',
    accentColor: 'border-amber-300 dark:border-amber-700/80 bg-gradient-to-b from-amber-50/80 to-white dark:from-amber-950/25 dark:to-slate-900',
    badgeColor: 'bg-amber-100 dark:bg-amber-900/60 text-amber-900 dark:text-amber-200 border-amber-300 dark:border-amber-700',
    sections: [
      {
        title: 'Baggage',
        icon: Luggage,
        items: [
          { type: 'check', text: '7 Kgs Cabin Baggage' },
          { type: 'check', text: '30 Kgs Check-in Baggage' },
        ],
      },
      {
        title: 'Flexibility',
        icon: ShieldAlert,
        items: [
          { type: 'cross', text: 'Free Cancellation' },
          { type: 'check', text: 'Free Date Change' },
        ],
      },
      {
        title: 'Seats, Meals & More',
        icon: Sparkles,
        items: [
          { type: 'check', text: 'Complimentary Seat Selection' },
          { type: 'check', text: 'Complimentary Meals' },
          { type: 'check', text: 'Priority Boarding' },
        ],
      },
    ],
  },
  BUSINESS: {
    key: 'BUSINESS',
    title: 'BUSINESS CLASS',
    subtitle: 'Enhanced Comfort & Priority Services',
    accentColor: 'border-blue-300 dark:border-blue-700/80 bg-gradient-to-b from-blue-50/80 to-white dark:from-blue-950/25 dark:to-slate-900',
    badgeColor: 'bg-blue-100 dark:bg-blue-900/60 text-blue-900 dark:text-blue-200 border-blue-300 dark:border-blue-700',
    sections: [
      {
        title: 'Baggage',
        icon: Luggage,
        items: [
          { type: 'check', text: '7 Kgs Cabin Baggage' },
          { type: 'check', text: '25 Kgs Check-in Baggage' },
        ],
      },
      {
        title: 'Flexibility',
        icon: ShieldAlert,
        items: [
          { type: 'minus', text: 'Cancellation fee applies' },
          { type: 'minus', text: 'Date Change fee starts at ₹2,500' },
        ],
      },
      {
        title: 'Seats, Meals & More',
        icon: Sparkles,
        items: [
          { type: 'check', text: 'Complimentary Seat Selection' },
          { type: 'check', text: 'Complimentary Meals' },
          { type: 'check', text: 'Priority Boarding' },
        ],
      },
    ],
  },
  ECONOMY: {
    key: 'ECONOMY',
    title: 'ECONOMY CLASS',
    subtitle: 'Essential Travel & Great Value',
    accentColor: 'border-emerald-300 dark:border-emerald-700/80 bg-gradient-to-b from-emerald-50/80 to-white dark:from-emerald-950/25 dark:to-slate-900',
    badgeColor: 'bg-emerald-100 dark:bg-emerald-900/60 text-emerald-900 dark:text-emerald-200 border-emerald-300 dark:border-emerald-700',
    sections: [
      {
        title: 'Baggage',
        icon: Luggage,
        items: [
          { type: 'check', text: '7 Kgs Cabin Baggage' },
          { type: 'check', text: '20 Kgs Check-in Baggage' },
        ],
      },
      {
        title: 'Flexibility',
        icon: ShieldAlert,
        items: [
          { type: 'cross', text: 'No refund on Cancellation' },
          { type: 'minus', text: 'Date Change fee starts at ₹4,750' },
        ],
      },
      {
        title: 'Seats, Meals & More',
        icon: Sparkles,
        items: [
          { type: 'check', text: 'Complimentary Seat Selection' },
          { type: 'check', text: 'Complimentary Meals' },
        ],
      },
    ],
  },
}

function ItemIcon({ type }) {
  if (type === 'check') {
    return (
      <span className="w-4 h-4 rounded-full bg-emerald-100 dark:bg-emerald-900/70 text-emerald-600 dark:text-emerald-400 flex items-center justify-center shrink-0 mt-0.5">
        <Check className="w-2.5 h-2.5 stroke-[3]" />
      </span>
    )
  }
  if (type === 'cross') {
    return (
      <span className="w-4 h-4 rounded-full bg-rose-100 dark:bg-rose-900/70 text-rose-600 dark:text-rose-400 flex items-center justify-center shrink-0 mt-0.5">
        <X className="w-2.5 h-2.5 stroke-[3]" />
      </span>
    )
  }
  return (
    <span className="w-4 h-4 rounded-full bg-amber-100 dark:bg-amber-900/70 text-amber-700 dark:text-amber-400 flex items-center justify-center shrink-0 mt-0.5">
      <Minus className="w-2.5 h-2.5 stroke-[3]" />
    </span>
  )
}

export function ClassFareCard({ classKey, compact = false }) {
  const info = CLASS_FARE_BENEFITS[classKey?.toUpperCase()] || CLASS_FARE_BENEFITS.ECONOMY

  return (
    <div className={`card p-5 sm:p-6 border rounded-2xl shadow-sm transition-all ${info.accentColor}`}>
      {/* Header */}
      <div className="pb-3 mb-4">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-1.5">
          <span className={`self-start text-xs font-black tracking-wider uppercase px-2.5 py-1 rounded-md border ${info.badgeColor}`}>
            {info.title}
          </span>
          <span className="text-[11px] text-slate-500 dark:text-slate-400 font-medium">
            {info.subtitle}
          </span>
        </div>
        {/* Dashed line representing ──────────────────────── */}
        <div className="border-b border-dashed border-slate-300 dark:border-slate-700/80 mt-3" />
      </div>

      {/* Sections - Line by line */}
      <div className={`space-y-4 ${compact ? 'text-xs' : 'text-sm'}`}>
        {info.sections.map((sec, idx) => {
          const SectionIcon = sec.icon
          return (
            <div key={idx} className="space-y-2">
              <div className="flex items-center gap-1.5 font-bold text-slate-900 dark:text-slate-100 text-xs">
                {SectionIcon && <SectionIcon className="w-3.5 h-3.5 text-slate-500 dark:text-slate-400" />}
                <span>{sec.title}</span>
              </div>
              <ul className="space-y-1.5 pl-1">
                {sec.items.map((item, itemIdx) => (
                  <li key={itemIdx} className="flex items-center gap-2.5 text-xs sm:text-sm text-slate-700 dark:text-slate-300 font-medium">
                    <ItemIcon type={item.type} />
                    <span>{item.text}</span>
                  </li>
                ))}
              </ul>
            </div>
          )
        })}
      </div>
    </div>
  )
}

export function AllClassFareBenefits() {
  return (
    <div className="flex flex-col space-y-4">
      <ClassFareCard classKey="FIRST" />
      <ClassFareCard classKey="BUSINESS" />
      <ClassFareCard classKey="ECONOMY" />
    </div>
  )
}

export function SpecificClassFareBenefits({ seatClasses = [] }) {
  const normalized = Array.isArray(seatClasses) ? seatClasses : [seatClasses]
  const uniqueClasses = Array.from(
    new Set(normalized.map(c => c?.toString().toUpperCase()).filter(Boolean))
  )

  const classesToRender = uniqueClasses.length > 0 ? uniqueClasses : ['ECONOMY']

  return (
    <div className="space-y-3">
      {classesToRender.map(cls => (
        <ClassFareCard key={cls} classKey={cls} />
      ))}
    </div>
  )
}
