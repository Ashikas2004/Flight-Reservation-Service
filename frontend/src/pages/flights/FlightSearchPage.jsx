import React, { useState, useEffect } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { Search, Plane, ArrowRight, Clock } from 'lucide-react'
import { flightApi } from '../../api/flightApi'
import { FullPageSpinner } from '../../components/common/Spinner'
import Alert from '../../components/common/Alert'
import StatusBadge from '../../components/common/StatusBadge'
import { format } from 'date-fns'

export default function FlightSearchPage() {

  const navigate = useNavigate()
  const [searchParams, setSearchParams] = useSearchParams()

  const [flights, setFlights] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [airports, setAirports] = useState([])
  const [pagination, setPagination] = useState(null)

  // Text displayed in From and To fields
  const [originText, setOriginText] = useState('')
  const [destinationText, setDestinationText] = useState('')

  // Show/hide autocomplete suggestions
  const [showOriginSuggestions, setShowOriginSuggestions] = useState(false)
  const [showDestinationSuggestions, setShowDestinationSuggestions] = useState(false)

  const {
    register,
    handleSubmit,
    setValue,
    formState: { errors }
  } = useForm({
    defaultValues: {
      origin: searchParams.get('origin') ?? '',
      destination: searchParams.get('destination') ?? '',
      departureDate: searchParams.get('departureDate') ?? '',
      passengers: searchParams.get('passengers') ?? 1,
    }
  })


  // --------------------------------------------------
  // LOAD AIRPORTS
  // --------------------------------------------------

  useEffect(() => {

    flightApi
      .getAirports()
      .then(response => {
        const airportData = response.data || []

        setAirports(airportData)

        // If URL already contains airport codes,
        // display the corresponding city names.
        const originCode = searchParams.get('origin')
        const destinationCode = searchParams.get('destination')

        if (originCode) {
          const originAirport = airportData.find(
            airport =>
              airport.iataCode.toLowerCase() === originCode.toLowerCase()
          )

          if (originAirport) {
            setOriginText(originAirport.city)
          }
        }

        if (destinationCode) {
          const destinationAirport = airportData.find(
            airport =>
              airport.iataCode.toLowerCase() === destinationCode.toLowerCase()
          )

          if (destinationAirport) {
            setDestinationText(destinationAirport.city)
          }
        }
      })
      .catch(() => {
        setAirports([])
      })


    // Auto-search if URL parameters are present
    if (
      searchParams.get('origin') &&
      searchParams.get('departureDate')
    ) {

      doSearch({
        origin: searchParams.get('origin'),
        destination: searchParams.get('destination'),
        departureDate: searchParams.get('departureDate'),
        passengers: searchParams.get('passengers') ?? 1,
      })
    }

  }, [])


  // --------------------------------------------------
  // SEARCH FLIGHTS
  // --------------------------------------------------

  const doSearch = async (data, page = 0) => {

    setLoading(true)
    setError('')

    try {

      const res = await flightApi.search({
        ...data,
        page,
        size: 10
      })

      setFlights(res.data.content)
      setPagination(res.data)

    } catch (err) {

      setError(
        err.response?.data?.message ??
        'Search failed. Please try again.'
      )

    } finally {

      setLoading(false)

    }
  }


  // --------------------------------------------------
  // FORM SUBMIT
  // --------------------------------------------------

  const onSubmit = (data) => {

    // Make sure From and To are actual airport codes
    if (!data.origin) {
      setError('Please select a departure airport.')
      return
    }

    if (!data.destination) {
      setError('Please select an arrival airport.')
      return
    }

    // Prevent same airport
    if (data.origin === data.destination) {
      setError('From and To airports cannot be the same.')
      return
    }

    setError('')

    setSearchParams(data)

    doSearch(data)
  }


  // --------------------------------------------------
  // FORMAT FLIGHT DURATION
  // --------------------------------------------------

  const formatDuration = (mins) => {

    const h = Math.floor(mins / 60)
    const m = mins % 60

    return `${h}h ${m}m`
  }


  // --------------------------------------------------
  // FILTER AIRPORTS
  // --------------------------------------------------

  const getFilteredAirports = (searchText) => {

    if (!searchText) {
      return []
    }

    const search = searchText.toLowerCase().trim()

    return airports.filter(airport => {

      const iataCode = airport.iataCode?.toLowerCase() || ''
      const city = airport.city?.toLowerCase() || ''
      const airportName = airport.airportName?.toLowerCase() || ''

      return (
        iataCode.includes(search) ||
        city.includes(search) ||
        airportName.includes(search)
      )
    })
  }


  // --------------------------------------------------
  // SELECT FROM AIRPORT
  // --------------------------------------------------

  const selectOriginAirport = (airport) => {

    // Display city name to user
    setOriginText(airport.city)

    // Store IATA code in react-hook-form
    setValue('origin', airport.iataCode, {
      shouldValidate: true
    })

    setShowOriginSuggestions(false)
  }


  // --------------------------------------------------
  // SELECT TO AIRPORT
  // --------------------------------------------------

  const selectDestinationAirport = (airport) => {

    // Display city name to user
    setDestinationText(airport.city)

    // Store IATA code in react-hook-form
    setValue('destination', airport.iataCode, {
      shouldValidate: true
    })

    setShowDestinationSuggestions(false)
  }


  // --------------------------------------------------
  // RENDER
  // --------------------------------------------------

  return (

    <div className="max-w-5xl mx-auto px-4 py-8">

      <h1 className="text-3xl font-extrabold text-slate-900 dark:text-slate-100 mb-6 tracking-tight">
        Search Flights
      </h1>


      {/* ==================================================
          SEARCH FORM
          ================================================== */}

      <div className="card mb-8 max-w-xl mx-auto border border-slate-200/90 dark:border-slate-800 shadow-md">

        <form
          onSubmit={handleSubmit(onSubmit)}
          className="space-y-4"
        >


          {/* ==================================================
              FROM
              ================================================== */}

          <div className="relative">

            <label className="label">
              From
            </label>

            <input
              type="text"
              placeholder="Enter City"
              className="input"
              value={originText}
              autoComplete="off"

              onFocus={() => {
                if (originText) {
                  setShowOriginSuggestions(true)
                }
              }}

              onChange={(e) => {

                const value = e.target.value

                setOriginText(value)

                // Clear previously selected airport
                setValue('origin', '')

                setShowOriginSuggestions(true)
              }}
            />


            {/* FROM SUGGESTIONS */}

            {showOriginSuggestions && originText && (

              <div className="absolute z-50 w-full mt-1.5 bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl shadow-xl overflow-hidden divide-y divide-slate-100 dark:divide-slate-700/50">

                {getFilteredAirports(originText).length > 0 ? (

                  getFilteredAirports(originText)
                    .slice(0, 6)
                    .map(airport => (

                      <div
                        key={airport.iataCode}
                        className="px-4 py-3 cursor-pointer hover:bg-slate-50 dark:hover:bg-slate-700/60 transition-colors"
                        onMouseDown={(e) => {
                          e.preventDefault()
                          selectOriginAirport(airport)
                        }}
                      >

                        {/* ONLY CITY NAME */}

                        <p className="font-semibold text-slate-800 dark:text-slate-100">
                          {airport.city}
                        </p>

                      </div>

                    ))

                ) : (

                  <div className="px-4 py-3 text-sm text-slate-500 dark:text-slate-400">
                    No airports found
                  </div>

                )}

              </div>

            )}


            {errors.origin && (

              <p className="text-red-500 text-xs mt-1">
                Please select a departure airport.
              </p>

            )}

          </div>


          {/* ==================================================
              TO
              ================================================== */}

          <div className="relative">

            <label className="label">
              To
            </label>

            <input
              type="text"
              placeholder="Enter Destination"
              className="input"
              value={destinationText}
              autoComplete="off"

              onFocus={() => {
                if (destinationText) {
                  setShowDestinationSuggestions(true)
                }
              }}

              onChange={(e) => {

                const value = e.target.value

                setDestinationText(value)

                // Clear previously selected airport
                setValue('destination', '')

                setShowDestinationSuggestions(true)
              }}
            />


            {/* TO SUGGESTIONS */}

            {showDestinationSuggestions && destinationText && (

              <div className="absolute z-50 w-full mt-1.5 bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl shadow-xl overflow-hidden divide-y divide-slate-100 dark:divide-slate-700/50">

                {getFilteredAirports(destinationText).length > 0 ? (

                  getFilteredAirports(destinationText)
                    .slice(0, 6)
                    .map(airport => (

                      <div
                        key={airport.iataCode}
                        className="px-4 py-3 cursor-pointer hover:bg-slate-50 dark:hover:bg-slate-700/60 transition-colors"
                        onMouseDown={(e) => {
                          e.preventDefault()
                          selectDestinationAirport(airport)
                        }}
                      >

                        {/* ONLY CITY NAME */}

                        <p className="font-semibold text-slate-800 dark:text-slate-100">
                          {airport.city}
                        </p>

                      </div>

                    ))

                ) : (

                  <div className="px-4 py-3 text-sm text-slate-500 dark:text-slate-400">
                    No airports found
                  </div>

                )}

              </div>

            )}


            {errors.destination && (

              <p className="text-red-500 text-xs mt-1">
                Please select an arrival airport.
              </p>

            )}

          </div>


          {/* ==================================================
              DATE
              ================================================== */}

          <div>

            <label className="label">
              Date
            </label>

            <input
              type="date"
              className="input"
              min={new Date().toISOString().split('T')[0]}
              {...register('departureDate', {
                required: 'Required'
              })}
            />

            {errors.departureDate && (

              <p className="text-red-500 text-xs mt-1">
                {errors.departureDate.message}
              </p>

            )}

          </div>


          {/* ==================================================
              NUMBER OF PERSONS
              ================================================== */}

          <div>

            <label className="label">
              Number of Persons
            </label>

            <input
              type="number"
              min="1"
              max="9"
              className="input"
              {...register('passengers', {
                required: true,
                min: 1,
                max: 9
              })}
            />

          </div>


          {/* ==================================================
              SEARCH BUTTON
              ================================================== */}

          <div className="pt-2">

            <button
              type="submit"
              className="btn-primary w-full py-3 flex items-center justify-center gap-2 font-semibold shadow-md"
            >

              <Search className="w-4 h-4" />

              Search Flights

            </button>

          </div>

        </form>

      </div>


      {/* ==================================================
          LOADING
          ================================================== */}

      {loading && <FullPageSpinner />}


      {/* ==================================================
          ERROR
          ================================================== */}

      {error && (
        <Alert
          type="error"
          message={error}
        />
      )}


      {/* ==================================================
          SEARCH RESULTS
          ================================================== */}

      {flights !== null && !loading && (

        <>

          <p className="text-sm font-medium text-slate-500 dark:text-slate-400 mb-4">

            {pagination?.totalElements ?? 0}

            {' '}

            flight
            {pagination?.totalElements !== 1 ? 's' : ''}

            {' '}found

          </p>


          {/* NO RESULTS */}

          {flights.length === 0 ? (

            <div className="card text-center py-16">

              <Plane className="w-12 h-12 text-slate-300 dark:text-slate-600 mx-auto mb-3" />

              <p className="text-slate-500 dark:text-slate-400 font-medium">
                No flights found for this route and date.
              </p>

            </div>

          ) : (

            <div className="space-y-4">


              {/* ==================================================
                  FLIGHT CARDS
                  ================================================== */}

              {flights.map(flight => (

                <div
                  key={flight.id}
                  className="card hover:shadow-lg dark:hover:shadow-slate-900/60 hover:border-blue-400 dark:hover:border-blue-700/50 transition-all cursor-pointer group"

                  onClick={() =>
                    navigate(`/booking/${flight.id}/seats`)
                  }
                >

                  <div className="flex flex-col md:flex-row md:items-center justify-between gap-5">


                    {/* ==================================================
                        AIRLINE
                        ================================================== */}

                    <div className="flex items-center gap-3.5 min-w-[150px]">

                      <div className="w-11 h-11 bg-blue-50 dark:bg-blue-950/60 rounded-xl flex items-center justify-center group-hover:scale-105 transition-transform">

                        <Plane className="w-5 h-5 text-blue-600 dark:text-blue-400" />

                      </div>

                      <div>

                        <p className="font-bold text-sm text-slate-900 dark:text-slate-100">
                          {flight.airline.name}
                        </p>

                        <p className="text-xs font-medium text-slate-400 dark:text-slate-500">
                          {flight.flightNumber}
                        </p>

                      </div>

                    </div>


                    {/* ==================================================
                        ROUTE & TIMES
                        ================================================== */}

                    <div className="flex items-center gap-4 flex-1">


                      {/* DEPARTURE */}
                      <div className="text-center min-w-[70px]">
                        <p className="text-xl font-extrabold text-slate-900 dark:text-slate-100">
                          {format(
                            new Date(flight.departureTime),
                            'HH:mm'
                          )}
                        </p>
                        <p className="text-sm font-semibold text-slate-700 dark:text-slate-300">
                          {flight.originAirport.city}
                        </p>
                        <p className="text-xs font-medium text-slate-400 dark:text-slate-500">
                          ({flight.originAirport.iataCode})
                        </p>
                      </div>

                      {/* DURATION */}
                      <div className="flex-1 flex flex-col items-center">
                        <div className="flex items-center gap-1 text-xs font-medium text-slate-400 dark:text-slate-400">
                          <Clock className="w-3 h-3" />
                          {formatDuration(
                            flight.durationMinutes
                          )}
                        </div>
                        <div className="flex items-center w-full gap-2 mt-1.5">
                          <div className="h-px flex-1 bg-slate-200 dark:bg-slate-700" />
                          <ArrowRight className="w-4 h-4 text-slate-400 dark:text-slate-500" />
                          <div className="h-px flex-1 bg-slate-200 dark:bg-slate-700" />
                        </div>
                        <p className="text-[11px] font-semibold text-emerald-600 dark:text-emerald-400 mt-1">
                          Direct
                        </p>
                      </div>

                      {/* ARRIVAL */}
                      <div className="text-center min-w-[70px]">
                        <p className="text-xl font-extrabold text-slate-900 dark:text-slate-100">
                          {format(
                            new Date(flight.arrivalTime),
                            'HH:mm'
                          )}
                        </p>
                        <p className="text-sm font-semibold text-slate-700 dark:text-slate-300">
                          {flight.destinationAirport.city}
                        </p>
                        <p className="text-xs font-medium text-slate-400 dark:text-slate-500">
                          ({flight.destinationAirport.iataCode})
                        </p>
                      </div>

                    </div>


                    {/* ==================================================
                        PRICE & CTA
                        ================================================== */}

                    <div className="flex flex-col items-end gap-2 min-w-[120px]">

                      <div className="text-right">

                        <p className="text-2xl font-black text-blue-600 dark:text-blue-400">
                          ₹{flight.basePrice}
                        </p>

                        <p className="text-[11px] text-slate-400 dark:text-slate-500 font-medium">
                          per person
                        </p>

                      </div>


                      <div className="flex items-center gap-2">

                        <StatusBadge
                          status={flight.status}
                        />

                        <span className="text-xs text-slate-500 dark:text-slate-400 font-medium">
                          {flight.availableSeats} left
                        </span>

                      </div>


                      <button
                        className="btn-primary text-xs py-2 px-4 shadow-sm"

                        onClick={(e) => {

                          e.stopPropagation()

                          navigate(
                            `/booking/${flight.id}/seats`
                          )

                        }}
                      >
                        Select Flight
                      </button>

                    </div>

                  </div>

                </div>

              ))}


              {/* ==================================================
                  PAGINATION
                  ================================================== */}

              {pagination &&
                pagination.totalPages > 1 && (

                <div className="flex justify-center gap-2 pt-4">


                  {/* PREVIOUS */}

                  <button
                    className="btn-secondary text-sm"

                    disabled={
                      pagination.pageNumber === 0
                    }

                    onClick={() =>
                      doSearch(
                        Object.fromEntries(searchParams),
                        pagination.pageNumber - 1
                      )
                    }
                  >
                    Previous
                  </button>


                  {/* PAGE NUMBER */}

                  <span className="text-sm text-slate-500 dark:text-slate-400 font-medium self-center">

                    Page {pagination.pageNumber + 1}
                    {' '}of{' '}
                    {pagination.totalPages}

                  </span>


                  {/* NEXT */}

                  <button
                    className="btn-secondary text-sm"

                    disabled={pagination.last}

                    onClick={() =>
                      doSearch(
                        Object.fromEntries(searchParams),
                        pagination.pageNumber + 1
                      )
                    }
                  >
                    Next
                  </button>

                </div>

              )}

            </div>

          )}

        </>

      )}

    </div>

  )
}