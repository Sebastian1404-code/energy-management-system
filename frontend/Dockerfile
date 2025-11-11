# --- build stage ---
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
# pass API URLs at build time (or use relative /api paths)
ARG VITE_API_URL
ARG VITE_DEVICE_API_URL
ARG VITE_AUTH_API_URL
ENV VITE_AUTH_API_URL=$VITE_AUTH_API_URL
ENV VITE_API_URL=$VITE_API_URL
ENV VITE_DEVICE_API_URL=$VITE_DEVICE_API_URL
RUN npm run build

# --- runtime stage ---
FROM nginx:alpine
# SPA history fallback
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/ /usr/share/nginx/html/
EXPOSE 80
