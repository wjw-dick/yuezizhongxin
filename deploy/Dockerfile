# syntax=docker/dockerfile:1
# 后端镜像：多阶段构建，运行镜像只保留 JRE 与 jar
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /src
# 先拷 pom 再拷源码，利用层缓存加速依赖下载
COPY pom.xml .
COPY yzz-common/pom.xml yzz-common/
COPY yzz-tenant/pom.xml yzz-tenant/
COPY yzz-system/pom.xml yzz-system/
COPY yzz-platform/pom.xml yzz-platform/
COPY yzz-room/pom.xml yzz-room/
COPY yzz-product/pom.xml yzz-product/
COPY yzz-crm/pom.xml yzz-crm/
COPY yzz-order/pom.xml yzz-order/
COPY yzz-stay/pom.xml yzz-stay/
COPY yzz-care/pom.xml yzz-care/
COPY yzz-rehab/pom.xml yzz-rehab/
COPY yzz-meal/pom.xml yzz-meal/
COPY yzz-staff/pom.xml yzz-staff/
COPY yzz-member/pom.xml yzz-member/
COPY yzz-content/pom.xml yzz-content/
COPY yzz-report/pom.xml yzz-report/
COPY yzz-mp/pom.xml yzz-mp/
COPY yzz-api/pom.xml yzz-api/
RUN mvn -q -B -DskipTests dependency:go-offline

COPY . .
RUN mvn -q -B -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
ENV TZ=Asia/Shanghai \
    JAVA_OPTS="-Xms512m -Xmx1536m -XX:+UseG1GC -Dfile.encoding=UTF-8"
COPY --from=build /src/yzz-api/target/yzz-api.jar /app/yzz-api.jar
RUN mkdir -p /app/logs /app/data/storage
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s \
  CMD wget -qO- http://127.0.0.1:8080/actuator/health | grep -q UP || exit 1
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/yzz-api.jar"]
