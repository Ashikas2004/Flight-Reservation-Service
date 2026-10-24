import api from './axiosInstance'

export const flightApi = {
  search: (params) => api.get('/flights/search', { params }),
  getById: (id) => api.get(`/flights/${id}`),

  // Airports
  getAirports: () => api.get('/airports'),
  searchAirports: (q) => api.get('/airports/search', { params: { q } }),

  // Airlines
  getAirlines: () => api.get('/airlines'),
}
