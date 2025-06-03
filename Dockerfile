# Stage 1: Build the application
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Run the application
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/management-0.0.1-SNAPSHOT.jar ./travel-nest.jar

# Set default environment variables
ENV APP_NAME=management \
    SERVER_PORT=8085 \
    PORT=8085 \
    DATE_FORMAT=DD-MM-YYYY \
    DB_URL=jdbc:mysql://localhost:3306/travel_agency \
    DB_USERNAME=root \
    DB_PASSWORD=root \
    DB_DRIVER=com.mysql.cj.jdbc.Driver \
    JPA_DDL_AUTO=update \
    JPA_SHOW_SQL=true \
    JPA_DIALECT=org.hibernate.dialect.MySQL8Dialect \
    JPA_FORMAT_SQL=true \
    SECURITY_USER_NAME=admin \
    SECURITY_USER_PASSWORD=admin123 \
    SECURITY_USER_ROLES=ADMIN \
    JWT_EXPIRATION=3600000 \
    JWT_REFRESH_EXPIRATION=86400000 \
    CORS_MAX_AGE=3600 \
    CORS_ALLOW_CREDENTIALS=true \
    MAIL_HOST=smtp.gmail.com \
    MAIL_PORT=587 \
    MAIL_USERNAME=assemomar202@gmail.com \
    MAIL_PASSWORD="ujrh axfq yjnv cwzp" \
    MAIL_SMTP_AUTH=true \
    MAIL_SMTP_STARTTLS=true

EXPOSE 8085
CMD ["java", "-jar", "travel-nest.jar"]