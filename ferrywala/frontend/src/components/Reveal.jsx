import { useInView } from '../lib/motion'

/**
 * Fades and lifts its content in when it scrolls into view. `i` staggers siblings (50 ms each, capped).
 * Content is visible immediately when motion is reduced or IntersectionObserver is missing.
 */
export default function Reveal({ as: Tag = 'div', i = 0, className = '', children, ...rest }) {
  const [ref, shown] = useInView()
  return (
    <Tag ref={ref} className={`rv ${shown ? 'in' : ''} ${className}`} style={{ '--i': Math.min(i, 8) }} {...rest}>
      {children}
    </Tag>
  )
}
