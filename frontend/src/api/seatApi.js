import api from './axiosInstance'

export const seatApi = {
  getByFlight: (flightId) => api.get(`/seats/flight/${flightId}`),
  getAvailable: (flightId) => api.get(`/seats/flight/${flightId}/available`),
  lockSeats: (data) => api.post('/seats/lock', data),
  releaseSeats: (seatIds) => api.post('/seats/release', { seatIds }),
}
