import api from './axiosInstance'

export const adminApi = {
  // Airlines
  getAirlines: () => api.get('/admin/airlines'),
  createAirline: (data) => api.post('/admin/airlines', data),
  updateAirline: (id, data) => api.put(`/admin/airlines/${id}`, data),
  toggleAirline: (id, active) => api.patch(`/admin/airlines/${id}/active`, { active }),

  // Airports
  getAirports: () => api.get('/admin/airports'),
  createAirport: (data) => api.post('/admin/airports', data),
  updateAirport: (id, data) => api.put(`/admin/airports/${id}`, data),
  toggleAirport: (id, active) => api.patch(`/admin/airports/${id}/active`, { active }),

  // Flights
  getAllFlights: (page = 0, size = 50) => api.get('/admin/flights', { params: { page, size } }),
  createFlight: (data) => api.post('/admin/flights', data),
  updateFlight: (id, data) => api.put(`/admin/flights/${id}`, data),
  updateFlightStatus: (id, status) => api.patch(`/admin/flights/${id}/status`, { status }),

  // Seats
  getSeatsByFlight: (flightId) => api.get(`/admin/seats/flight/${flightId}`),
  createSeat: (data) => api.post('/admin/seats', data),
  updateSeatStatus: (id, status) => api.patch(`/admin/seats/${id}/status`, { status }),

  // Audit
  getAuditLogs: (params) => api.get('/audit', { params }),
  getAuditByEntity: (entityType, entityId) => api.get(`/audit/entity/${entityType}/${entityId}`),

  // Bookings
  getBooking: (reference) => api.get(`/admin/bookings/${reference}`),
}
