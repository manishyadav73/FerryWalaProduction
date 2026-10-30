# Build: docker build -t ferrywala-web frontend
FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
# Leave unset to call the API on the same origin (nginx proxies /api and /ws).
ARG VITE_API_BASE_URL
ARG VITE_WS_URL
RUN npm run build

FROM nginx:1.27-alpine
COPY deploy/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
