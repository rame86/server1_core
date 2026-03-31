#core_service/ Dockerfile
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
#plain.jar 복사를 방지하기 위해 실행 가능한 jar 패턴만 명시
COPY build/libs/*-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]