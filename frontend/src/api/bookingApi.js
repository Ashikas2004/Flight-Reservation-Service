import api from './axiosInstance'

export const bookingApi = {
  create: (data) => api.post('/bookings', data),
  getMyBookings: (page = 0, size = 10) => api.get('/bookings/my', { params: { page, size } }),
  getByReference: (ref) => api.get(`/bookings/${ref}`),
  cancel: (ref, data) => api.put(`/bookings/${ref}/cancel`, data),
}
