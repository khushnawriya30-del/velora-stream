FROM node:18-slim
WORKDIR /app
COPY package*.json ./
ENV PUPPETEER_SKIP_CHROMIUM_DOWNLOAD=true
ENV PUPPETEER_SKIP_DOWNLOAD=true
RUN npm install --omit=dev --ignore-scripts --no-audit --no-fund
COPY . .
EXPOSE 3000
CMD ["node", "dist/src/main"]
