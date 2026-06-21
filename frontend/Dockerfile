# Stage 1: Build
FROM oven/bun:alpine AS build
WORKDIR /app

ARG API_URL
ARG UI_URL

ENV API_URL=$API_URL
ENV UI_URL=$UI_URL

# 1. Copy only the dependency files first to take advantage of the Docker cache
COPY package.json bun.lock ./

# 2. Use frozen-lockfile to ensure that CI/CD builds exactly the same as your local environment
RUN bun install --frozen-lockfile

# 3. Copy the rest of the code and build
COPY . .

# 1. Copy the rest of the code and build
COPY . . 
# Ensure it's built with the production configuration
RUN bun run build --configuration=production

# Stage 2: Production Server (Secure)
# We use the official unprivileged Nginx image to avoid running as root
FROM nginxinc/nginx-unprivileged:alpine AS runtime

# We temporarily switch to root to install the package
USER root
RUN apk add --no-cache tzdata
# We return to the default unprivileged user of this image
USER nginx

# Copy the compiled Angular artifacts (ensuring the correct path for Angular 17+)
COPY --from=build /app/dist/auth-alpaca-ui/browser /usr/share/nginx/html

# Copy our optimized Nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Unprivileged Nginx uses port 8080 by default (ports < 1024 require root)
EXPOSE 8080
CMD ["nginx", "-g", "daemon off;"]