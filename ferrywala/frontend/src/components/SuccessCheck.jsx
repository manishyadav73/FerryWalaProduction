/** Drawn tick. Only render it after the operation has really succeeded. */
export default function SuccessCheck({ size = 56 }) {
  return (
    <svg className="success-check" width={size} height={size} viewBox="0 0 56 56" aria-hidden="true">
      <circle cx="28" cy="28" r="25" fill="none" strokeWidth="3" />
      <path d="M16 29.5l8 8 16-18" fill="none" strokeWidth="4" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  )
}
