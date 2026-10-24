import api from './axiosInstance'

export const paymentApi = {
  processPayment: (data) => api.post('/payments', data),
  getByBooking: (bookingId) => api.get(`/payments/booking/${bookingId}`),
}
