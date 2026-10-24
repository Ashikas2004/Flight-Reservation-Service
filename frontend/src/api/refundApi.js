import api from './axiosInstance'

export const refundApi = {
  requestRefund: (data) => api.post('/refunds', data),
  getMyRefunds: (page = 0, size = 10) => api.get('/refunds/my', { params: { page, size } }),
  getByBooking: (bookingId) => api.get(`/refunds/booking/${bookingId}`),
}
