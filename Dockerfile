FROM maven:3-eclipse-temurin-24 as builder
WORKDIR /app
COPY ./pom.xml ./pom.xml
RUN mvn dependency:go-offline -B
COPY ./src ./src
RUN mvn package && cp target/*.jar application.jar
RUN java -Djarmode=layertools -jar application.jar extract

FROM cockroachdb/cockroach:latest as cockroach

FROM eclipse-temurin:25-jdk
COPY --from=builder /app/dependencies/ ./
COPY --from=builder /app/snapshot-dependencies/ ./
COPY --from=builder /app/spring-boot-loader/ ./
COPY --from=builder /app/application/ ./
COPY --from=cockroach /cockroach/ ./

RUN mkdir -pv ./.cockroach-certs
RUN mkdir -pv ./.cockroach-key

# Create non-root user for security
RUN groupadd -r appuser && useradd -r -g appuser appuser
RUN chown -R appuser:appuser /.cockroach-certs /.cockroach-key

USER appuser

EXPOSE 9999

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
