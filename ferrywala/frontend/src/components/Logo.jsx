export default function Logo({ size = 30, withText = true }) {
  return (
    <span className="logo">
      <svg width={size} height={size} viewBox="0 0 64 64" aria-hidden="true">
        <rect width="64" height="64" rx="16" fill="#2F6B3A" />
        <path d="M14 40h30l-3 10H19z" fill="#F2A81D" />
        <circle cx="22" cy="54" r="4" fill="#FBF6EC" />
        <circle cx="40" cy="54" r="4" fill="#FBF6EC" />
        <path d="M16 24h32l-3-8H19z" fill="#FBF6EC" />
        <circle cx="26" cy="32" r="4" fill="#D8572A" />
        <circle cx="36" cy="32" r="4" fill="#FBF6EC" />
      </svg>
      {withText && <span className="logo-text">Ferry<b>Wala</b></span>}
    </span>
  )
}
