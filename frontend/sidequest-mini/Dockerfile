# Stage 1: Build
FROM node:18-slim AS build
WORKDIR /app
COPY package*.json ./
RUN npm install --include=optional
# Ensure rollup native optional dep is present in Linux builds
RUN npm install --no-save @rollup/rollup-linux-x64-gnu
COPY . .
RUN npm run build:h5

# Stage 2: Run
FROM nginx:alpine
# 复制构建产物到 nginx 目录
COPY --from=build /app/dist/build/h5 /usr/share/nginx/html/sidequest-mini
# 容器内部使用的配置文件会在 docker-compose 中挂载覆盖
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
