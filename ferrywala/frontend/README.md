# FerryWala web

React 19 + Vite. See the root README for running and deploying.

```bash
npm install
npm run dev      # proxies /api and /ws to DEV_API_TARGET (default http://localhost:8080)
npm run lint
npm run build
```

Config (optional, build time): `VITE_API_BASE_URL` (API on another origin), `VITE_WS_URL`. By default the app calls `/api` on its own origin.
