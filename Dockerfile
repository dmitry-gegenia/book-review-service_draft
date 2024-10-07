# build stage
FROM gradle:7.5.1-jdk17-alpine AS builder
WORKDIR /usr/test/src/book-reviews-service
COPY . .
RUN gradle clean build -x test

#app package stage
FROM openjdk:17-alpine3.14
WORKDIR /app
COPY --from=builder /usr/test/src/book-reviews-service/build/libs/*.jar /app/book-reviews-service.jar
EXPOSE 8081
CMD ["java", "-jar", "-Dspring.profiles.active=default", "/app/book_reviews_service.jar"]