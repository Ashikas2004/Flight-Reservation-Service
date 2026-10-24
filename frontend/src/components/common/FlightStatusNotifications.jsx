import React, { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { AlertTriangle, Clock, X } from 'lucide-react'
import { useAuth } from '../../context/AuthContext'
import { bookingApi } from '../../api/bookingApi'

export default function FlightStatusNotifications() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const [notifications, setNotifications] = useState([])
  const queuedNotifications = useRef(new Set())

  useEffect(() => {
    setNotifications([])
    queuedNotifications.current.clear()
    if (!user || user.role === 'ADMIN') return undefined

    let active = true
    let requestPending = false
    const storagePrefix = `flightStatusNotifications:${user.id ?? user.email}:`

    const checkBookings = async () => {
      if (requestPending) return
      requestPending = true
      try {
        const bookings = []
        let page = 0
        let totalPages = 1
        while (page < totalPages) {
          const { data } = await bookingApi.getMyBookings(page, 100)
          if (!active) return
          bookings.push(...(data.content ?? []))
          totalPages = data.totalPages
          page += 1
        }
        const newNotifications = []
        const checkedFlights = new Set()

        for (const booking of bookings) {
          const flight = booking.flight
          if (!flight) continue

          if (
            flight.status === 'CANCELLED' &&
            booking.cancellationReason?.startsWith('Flight cancelled by airline')
          ) {
            const cancellationId = `cancelled-${booking.id}`
            if (
              localStorage.getItem(`${storagePrefix}${cancellationId}`) !== 'true' &&
              !queuedNotifications.current.has(cancellationId)
            ) {
              queuedNotifications.current.add(cancellationId)
              newNotifications.push({
                id: cancellationId,
                type: 'CANCELLED',
                flightNumber: flight.flightNumber,
                bookingReference: booking.bookingReference,
                refund: booking.refund,
                bookingStatus: booking.status,
              })
            }
          }

          const flightId = String(flight.id)
          if (checkedFlights.has(flightId)) continue
          checkedFlights.add(flightId)

          const delayId = `delayed-${flightId}`
          if (
            flight.status === 'DELAYED' &&
            localStorage.getItem(`${storagePrefix}${delayId}`) !== 'true' &&
            !queuedNotifications.current.has(delayId)
          ) {
            queuedNotifications.current.add(delayId)
            newNotifications.push({
              id: delayId,
              type: 'DELAYED',
              flightNumber: flight.flightNumber,
              bookingReference: booking.bookingReference,
            })
          } else if (flight.status !== 'DELAYED') {
            localStorage.removeItem(`${storagePrefix}${delayId}`)
          }
        }

        if (newNotifications.length) {
          setNotifications(current => [...current, ...newNotifications])
        }
      } catch (error) {
        console.error('Failed to check booked flight status updates', error)
      } finally {
        requestPending = false
      }
    }

    checkBookings()
    const interval = setInterval(checkBookings, 30_000)
    return () => {
      active = false
      clearInterval(interval)
    }
  }, [user])

  const notification = notifications[0]
  if (!notification) return null

  const dismiss = () => {
    queuedNotifications.current.delete(notification.id)
    const storagePrefix = `flightStatusNotifications:${user.id ?? user.email}:`
    localStorage.setItem(`${storagePrefix}${notification.id}`, 'true')
    setNotifications(current => current.slice(1))
  }
  const isCancelled = notification.type === 'CANCELLED'
  const refundProcessed = notification.refund?.status === 'PROCESSED'

  return (
    <div className="fixed inset-0 z-[100] flex items-center justify-center bg-slate-950/60 backdrop-blur-sm p-4">
      <section
        role="dialog"
        aria-modal="true"
        aria-labelledby="flight-status-notification-title"
        className="w-full max-w-md rounded-2xl border border-slate-200 bg-white p-6 shadow-2xl dark:border-slate-700 dark:bg-slate-900"
      >
        <div className="flex items-start gap-4">
          <div className={`flex h-11 w-11 shrink-0 items-center justify-center rounded-xl ${
            isCancelled
              ? 'bg-rose-100 text-rose-600 dark:bg-rose-950/60 dark:text-rose-300'
              : 'bg-amber-100 text-amber-600 dark:bg-amber-950/60 dark:text-amber-300'
          }`}>
            {isCancelled ? <AlertTriangle className="h-5 w-5" /> : <Clock className="h-5 w-5" />}
          </div>
          <div className="min-w-0 flex-1">
            <h2 id="flight-status-notification-title" className="text-lg font-bold text-slate-900 dark:text-slate-100">
              {isCancelled ? 'Flight cancelled' : 'Flight delayed'}
            </h2>
            <p className="mt-1 text-sm leading-relaxed text-slate-600 dark:text-slate-300">
              {isCancelled
                ? `Flight ${notification.flightNumber} for booking ${notification.bookingReference} has been cancelled.`
                : `Flight ${notification.flightNumber} on booking ${notification.bookingReference} has been delayed.`}
            </p>
            {isCancelled && (
              <p className="mt-2 text-sm font-medium text-slate-700 dark:text-slate-200">
                {refundProcessed
                  ? `An automatic refund of ${notification.refund.refundAmount} ${notification.refund.currency} has been processed.`
                  : notification.refund
                    ? `Automatic refund status: ${notification.refund.status}.`
                    : 'Your booking was cancelled. No successful payment was found to refund.'}
              </p>
            )}
          </div>
          <button
            type="button"
            onClick={dismiss}
            aria-label="Dismiss flight status notification"
            className="rounded-lg p-1 text-slate-400 hover:bg-slate-100 hover:text-slate-700 dark:hover:bg-slate-800 dark:hover:text-slate-200"
          >
            <X className="h-5 w-5" />
          </button>
        </div>
        <div className="mt-6 flex justify-end gap-3">
          <button type="button" onClick={dismiss} className="btn-secondary">
            Dismiss
          </button>
          <button
            type="button"
            onClick={() => {
              dismiss()
              navigate(`/bookings/${notification.bookingReference}`)
            }}
            className="btn-primary"
          >
            View ticket
          </button>
        </div>
      </section>
    </div>
  )
}
