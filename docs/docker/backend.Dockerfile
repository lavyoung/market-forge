FROM maven:3.9.9-eclipse-temurin-17 AS builder

WORKDIR /workspace

# LavShard 是尚未发布到 Maven 仓库的本地组件。
# 先编译并安装到当前构建容器的 Maven 本地仓库，
# 后续 Market Forge 构建即可解析 0.1.0 版本依赖。
COPY vendor/lavshard /workspace/lavshard

RUN mvn -B \
    -f /workspace/lavshard/pom.xml \
    -pl lavshard-spring-boot-starter \
    -am \
    -Dmaven.test.skip=true \
    install

COPY pom.xml ./
COPY market-forge-types/pom.xml market-forge-types/pom.xml
COPY market-forge-api/pom.xml market-forge-api/pom.xml
COPY market-forge-domain/pom.xml market-forge-domain/pom.xml
COPY market-forge-application/pom.xml market-forge-application/pom.xml
COPY market-forge-infrastructure/pom.xml market-forge-infrastructure/pom.xml
COPY market-forge-trigger/pom.xml market-forge-trigger/pom.xml
COPY market-forge-app/pom.xml market-forge-app/pom.xml

COPY market-forge-types market-forge-types
COPY market-forge-api market-forge-api
COPY market-forge-domain market-forge-domain
COPY market-forge-application market-forge-application
COPY market-forge-infrastructure market-forge-infrastructure
COPY market-forge-trigger market-forge-trigger
COPY market-forge-app market-forge-app

RUN mvn -B \
    -pl market-forge-app \
    -am \
    -DskipTests \
    clean package

RUN set -eux; \
    JAR_FILE="$(find market-forge-app/target \
        -type f \
        -name 'market-forge-app-*.jar' \
        ! -name '*.jar.original' \
        -print \
        -quit)"; \
    test -n "${JAR_FILE}"; \
    cp "${JAR_FILE}" /workspace/market-forge.jar


FROM eclipse-temurin:17-jre-jammy AS runtime

RUN apt-get update \
    && apt-get install --yes --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system marketforge \
    && useradd \
        --system \
        --gid marketforge \
        --create-home \
        marketforge

WORKDIR /app

COPY --from=builder /workspace/market-forge.jar /app/market-forge.jar

RUN chown marketforge:marketforge /app/market-forge.jar

USER marketforge

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/market-forge.jar"]