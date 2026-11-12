FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /workspace

COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src src
RUN mvn -B -q -DskipTests package \
    && java -Djarmode=tools -jar target/chargewise-rag-*.jar extract --layers --launcher --destination target/extracted

FROM eclipse-temurin:25-jre
WORKDIR /app

RUN groupadd --system chargewise && useradd --system --gid chargewise chargewise

COPY --from=build /workspace/target/extracted/dependencies/ ./
COPY --from=build /workspace/target/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/target/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/target/extracted/application/ ./

USER chargewise
EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "org.springframework.boot.loader.launch.JarLauncher"]
