FROM node:24-alpine
ENV NODE_ENV=production
WORKDIR /app
RUN npm install --global npm@12.2.1
COPY package*.json ./
RUN npm install --omit=dev && npm cache clean --force
COPY src ./src
RUN mkdir -p /app/data && chown -R node:node /app
USER node
EXPOSE 4100
CMD ["node", "src/server.js"]
