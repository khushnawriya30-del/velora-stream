FROM node:18-slim AS builder
WORKDIR /app
COPY package*.json ./
ENV PUPPETEER_SKIP_CHROMIUM_DOWNLOAD=true
ENV PUPPETEER_SKIP_DOWNLOAD=true
ENV NODE_OPTIONS=--max-old-space-size=400
RUN npm install --ignore-scripts --no-audit --no-fund --prefer-offline || npm install --ignore-scripts --no-audit --no-fund
COPY . .
RUN node --max-old-space-size=400 node_modules/.bin/nest build

FROM node:18-slim
RUN apt-get update && apt-get install -y ffmpeg && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY package*.json ./
ENV PUPPETEER_SKIP_CHROMIUM_DOWNLOAD=true
ENV PUPPETEER_SKIP_DOWNLOAD=true
ENV NODE_OPTIONS=--max-old-space-size=400
RUN npm install --omit=dev --ignore-scripts --no-audit --no-fund
COPY --from=builder /app/dist ./dist
EXPOSE 3000
CMD ["node", "--max-old-space-size=400", "dist/main"]
