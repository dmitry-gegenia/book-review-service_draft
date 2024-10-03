# build stage
FROM gradle:7.5.1-jdk17-alpine AS builder
WORKDIR /usr/test/src/book_review_service
COPY . .
RUN gradle clean build -x test

#app package stage
FROM openjdk:17-alpine3.14
WORKDIR /app
COPY --from=builder /usr/test/src/book_review_service/build/libs/*.jar /app/book_review_service.jar
CMD ["java", "-jar", "-Dspring.profiles.active=default", "/app/book_review_service.jar"]