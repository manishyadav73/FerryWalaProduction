# UI/UX and animation audit (FerryWala)

FerryWala is a street-vendor marketplace, not a ferry-booking app. The brief's ferry terms map as: ferry card = vendor card, booking/checkout = the request flow (`/cart`), ferry details = vendor profile, payment = not applicable (customers pay vendors directly; FerryWala never handles money). Verified in headless Chromium with a **mocked API** (Playwright route stubs), so it proves the UI behaviour, not backend integration.

Legend: **Preserved** = already existed and kept; **Improved/Added** = changed in this delivery; **N/A** = does not fit this product.

| Category | Status | Where | Notes / what is still missing |
|---|---|---|---|
| Cinematic hero | Preserved + Improved | Landing | Kept the SVG "living scene" (customers ask -> demand adds up -> vendor heads there). Added drifting gradient mesh, staggered headline/search/CTA entrance, scene entrance. No video/ocean: not relevant to the product. |
| Scroll-triggered animation | Added | Landing (problem, flow, simulator, how-it-works, benefit cards, signal list, trust cards, final CTA), Meet page sections | `Reveal` + IntersectionObserver, stagger 70 ms, shows instantly with reduced motion or no IO. Other pages use mount-time staggers instead of scroll reveals. |
| Micro-interactions | Added | Buttons (hover lift, press, busy spinner), nav underline, bottom-nav icon pop, input focus/label, error shake, chips, cards, quantity stepper bump, cart-badge bump, saved-vendor heart, toasts, FAQ open, dialogs | Date pickers and passenger selectors do not exist in this product (N/A). |
| 3D animation | Partial (by design) | Landing hero scene | CSS perspective tilt toward the pointer (off for touch and reduced motion). **No Three.js/WebGL and no 3D ferry**: nothing in this product needs it and it would add heavy assets. |
| Page transitions | Added | All routes | Route-keyed enter animation, scroll to top, focus moves to main, slim progress bar when a page takes >150 ms to load (also Suspense fallback). Enter-only (no exit animation). Back/forward works. |
| Interactive route animation | Preserved + Added | Meet page, Landing scene, Orders | Animated dashed line between the two people **only when both chose to share location** (never fabricated). Order status timeline current step pulses. No ferry movement (no such data). |
| Loading and state animations | Preserved + Added | Skeletons (preserved), busy buttons, route bar, request steps, success tick (only after the API succeeds), error shake + retry, empty states, offline banner | Payment processing: N/A. The offline banner reflects browser connectivity, not API health. |
| Premium minimalism | Preserved + Improved | Auth, lists, dashboard, account | Existing warm paper/leaf/saffron system kept; darker muted/eyebrow colours for contrast. |
| Cinematic travel UI | Partial | Landing | Hero only. Vendor profile and other pages were not restyled with imagery (vendors upload one photo). |
| Conversion-focused request UX | Improved | Landing hero search, Vendors, Cart | Hero search -> pre-filtered vendors; 3-step progress; clear total; error stays in place; success confirmation on Orders after real success. |
| Glassmorphism | Added (selective) | Hero search panel, bottom nav, assistant header | Falls back to solid colour where `backdrop-filter` is unsupported. |
| Mobile-first responsive | Verified | All routes | 375 / 820 / 1280 px and 360 px: no horizontal overflow; no console errors. |

## Verification run (on the exact ZIP source)
- `npm run lint`: pass. `npm test`: 18 tests pass (i18n parity, dialog, concierge panel, meet consent flow, reveal, steps, favourite). `npm run build`: pass.
- Browser behaviour script (20 checks, mocked API): hero animations running, glass applied, scroll reveal hidden-then-shown, 3D tilt, hero search -> filtered list, favourite persists, page transition + focus, order timeline pulse, back button, offline banner on/off, steps, stepper bump, success banner only after success, success tick draws, failed send stays on cart with shake, route bar during slow load, reduced motion (nothing hidden, no infinite animations, no tilt), 360 px no overflow: **all pass**.
- Axe (WCAG 2 A/AA) on 12 customer routes and 4 vendor routes at 3 widths: **0 violations**, no horizontal scroll, no console errors.
- **Not browser-verified:** Admin pages (my mock data did not cover them), real device touch/Safari, real slow networks, screen readers.
- **Backend tests were not run** (Maven Central unreachable here); see `docs/LAUNCH_REPORT.md`. No backend code changed in this pass.
