/** Small shared helpers for the Concierge, kept out of component files. */
export const CASE_CATEGORIES = ['MEETING_POINT', 'RUNNING_LATE', 'NO_RESPONSE', 'ORDER_MISMATCH', 'PAYMENT',
  'CANCELLATION_REFUND', 'SERVICE_DELAY', 'ACCESSIBILITY', 'APP_PROBLEM', 'OTHER']

/** Other screens ask the launcher to open the Concierge for a given request. */
export function openConcierge(orderId) {
  window.dispatchEvent(new CustomEvent('fw:open-concierge', { detail: { orderId } }))
}
