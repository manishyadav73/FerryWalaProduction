// Single place that talks to the backend. No hard-coded hosts: the base is
// "/api" (same origin / dev proxy) unless VITE_API_BASE_URL is set at build.
const API_BASE = (import.meta.env.VITE_API_BASE_URL ?? '/api').replace(/\/+$/, '')
const API_ORIGIN = API_BASE.startsWith('http') ? new URL(API_BASE).origin : ''
const TOKEN_KEY = 'fw_token'
const TIMEOUT_MS = 15000

export class ApiError extends Error {
  constructor(message, { status = 0, code = 'ERROR', details = {} } = {}) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
    this.details = details
  }
}

let onUnauthorized = null
export function setUnauthorizedHandler(fn) {
  onUnauthorized = fn
}

export const tokenStore = {
  get() {
    try { return localStorage.getItem(TOKEN_KEY) } catch { return null }
  },
  set(token) {
    try { token ? localStorage.setItem(TOKEN_KEY, token) : localStorage.removeItem(TOKEN_KEY) } catch { /* private mode */ }
  },
}

/** Uploaded photos come back as "/api/media/..."; make them absolute if the API is on another origin. */
export function mediaUrl(path) {
  if (!path) return null
  if (/^https?:\/\//.test(path)) return path
  return API_ORIGIN + path
}

async function request(path, { method = 'GET', body, signal, form } = {}) {
  const controller = new AbortController()
  const timer = setTimeout(() => controller.abort(), TIMEOUT_MS)
  if (signal) signal.addEventListener('abort', () => controller.abort(), { once: true })

  const token = tokenStore.get()
  const headers = { Accept: 'application/json' }
  if (token) headers.Authorization = `Bearer ${token}`
  let payload
  if (form) payload = form
  else if (body !== undefined) {
    headers['Content-Type'] = 'application/json'
    payload = JSON.stringify(body)
  }

  let res
  try {
    res = await fetch(`${API_BASE}${path}`, { method, headers, body: payload, signal: controller.signal })
  } catch (e) {
    if (signal?.aborted) throw new ApiError('Cancelled', { code: 'ABORTED' })
    if (e?.name === 'AbortError') throw new ApiError('This is taking too long. Check your connection and try again.', { code: 'TIMEOUT' })
    throw new ApiError("Can't reach FerryWala right now. Check your connection and try again.", { code: 'NETWORK' })
  } finally {
    clearTimeout(timer)
  }

  let data = null
  if (res.status !== 204) {
    const type = res.headers.get('content-type') || ''
    if (type.includes('application/json')) data = await res.json().catch(() => null)
  }
  if (!res.ok) {
    if (res.status === 401 && token && onUnauthorized) onUnauthorized()
    throw new ApiError(data?.message || 'Something went wrong. Please try again.', {
      status: res.status,
      code: data?.code || 'ERROR',
      details: data?.details || {},
    })
  }
  return data
}

const qs = (params) => {
  const p = new URLSearchParams()
  Object.entries(params).forEach(([k, v]) => {
    if (v !== undefined && v !== null && v !== '') p.set(k, String(v))
  })
  const s = p.toString()
  return s ? `?${s}` : ''
}

export const api = {
  // auth
  register: (b) => request('/auth/register', { method: 'POST', body: b }),
  login: (b) => request('/auth/login', { method: 'POST', body: b }),
  me: (signal) => request('/auth/me', { signal }),
  verifyEmail: (token) => request(`/auth/verify-email${qs({ token })}`),
  forgotPassword: (email) => request('/auth/forgot-password', { method: 'POST', body: { email } }),
  resetPassword: (token, password) => request('/auth/reset-password', { method: 'POST', body: { token, password } }),
  resendVerification: () => request('/auth/resend-verification', { method: 'POST' }),
  // public browsing
  nearby: (p, signal) => request(`/vendors/nearby${qs(p)}`, { signal }),
  vendor: (id, signal) => request(`/vendors/${id}`, { signal }),
  reviewsOf: (id, signal) => request(`/vendors/${id}/reviews`, { signal }),
  // customer
  placeOrder: (b) => request('/orders', { method: 'POST', body: b }),
  myOrders: (signal) => request('/orders/mine', { signal }),
  cancelOrder: (id) => request(`/orders/${id}/cancel`, { method: 'POST' }),
  reviewOrder: (id, b) => request(`/orders/${id}/review`, { method: 'POST', body: b }),
  sendSignal: (b) => request('/demand/signals', { method: 'POST', body: b }),
  // notifications
  notifications: (signal) => request('/notifications', { signal }),
  readAllNotifications: () => request('/notifications/read-all', { method: 'POST' }),
  // vendor
  vendorMe: (signal) => request('/vendor/me', { signal }),
  vendorUpdate: (b) => request('/vendor/me', { method: 'PUT', body: b }),
  vendorStatus: (online) => request('/vendor/me/status', { method: 'PUT', body: { online } }),
  vendorLocation: (b) => request('/vendor/me/location', { method: 'PUT', body: b }),
  vendorUploadImage: (file) => {
    const form = new FormData()
    form.append('file', file)
    return request('/vendor/me/image', { method: 'POST', form })
  },
  vendorDeleteImage: () => request('/vendor/me/image', { method: 'DELETE' }),
  vendorStats: (signal) => request('/vendor/stats', { signal }),
  vendorOpportunities: (p, signal) => request(`/vendor/opportunities${qs(p)}`, { signal }),
  vendorOrders: (status, signal) => request(`/vendor/orders${qs({ status })}`, { signal }),
  vendorSetOrderStatus: (id, status) => request(`/vendor/orders/${id}/status`, { method: 'PUT', body: { status } }),
  vendorProducts: (signal) => request('/vendor/products', { signal }),
  vendorCreateProduct: (b) => request('/vendor/products', { method: 'POST', body: b }),
  vendorUpdateProduct: (id, b) => request(`/vendor/products/${id}`, { method: 'PUT', body: b }),
  vendorSetAvailability: (id, available) => request(`/vendor/products/${id}/availability`, { method: 'PATCH', body: { available } }),
  vendorDeleteProduct: (id) => request(`/vendor/products/${id}`, { method: 'DELETE' }),
  vendorReviews: (signal) => request('/vendor/reviews', { signal }),
  // AI Concierge, meeting coordination, support
  conciergeStatus: (signal) => request('/concierge/status', { signal }),
  conciergeChat: (b) => request('/concierge/chat', { method: 'POST', body: b }),
  conciergeHistory: (signal) => request('/concierge/history', { signal }),
  conciergeClear: () => request('/concierge/history', { method: 'DELETE' }),
  meeting: (id, signal) => request(`/orders/${id}/meeting`, { signal }),
  meetingUpdate: (id, b) => request(`/orders/${id}/meeting/updates`, { method: 'POST', body: b }),
  sendMessage: (id, body) => request(`/orders/${id}/messages`, { method: 'POST', body: { body } }),
  startShare: (id, b) => request(`/orders/${id}/location-share`, { method: 'PUT', body: b }),
  stopShare: (id) => request(`/orders/${id}/location-share`, { method: 'DELETE' }),
  createCase: (b) => request('/support/cases', { method: 'POST', body: b }),
  myCases: (signal) => request('/support/cases/mine', { signal }),
  adminCases: (status, signal) => request(`/admin/support/cases${qs({ status })}`, { signal }),
  adminUpdateCase: (id, b) => request(`/admin/support/cases/${id}`, { method: 'PUT', body: b }),
  // admin
  adminStats: (signal) => request('/admin/stats', { signal }),
  adminUsers: (p, signal) => request(`/admin/users${qs(p)}`, { signal }),
  adminSetUserEnabled: (id, enabled) => request(`/admin/users/${id}/enabled`, { method: 'PUT', body: { enabled } }),
  adminVendors: (p, signal) => request(`/admin/vendors${qs(p)}`, { signal }),
  adminSetVendorVerified: (id, verified) => request(`/admin/vendors/${id}/verified`, { method: 'PUT', body: { verified } }),
  adminProducts: (p, signal) => request(`/admin/products${qs(p)}`, { signal }),
  adminDeleteProduct: (id) => request(`/admin/products/${id}`, { method: 'DELETE' }),
  adminOrders: (status, signal) => request(`/admin/orders${qs({ status })}`, { signal }),
  adminDemandAreas: (signal) => request('/admin/demand/areas', { signal }),
  adminSuspicious: (signal) => request('/admin/suspicious', { signal }),
}
