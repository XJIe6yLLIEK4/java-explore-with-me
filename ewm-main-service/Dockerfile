FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

COPY pom.xml pom.xml
COPY stats/pom.xml stats/pom.xml
COPY stats/stats-dto/pom.xml stats/stats-dto/pom.xml
COPY stats/stats-client/pom.xml stats/stats-client/pom.xml
COPY stats/stats-server/pom.xml stats/stats-server/pom.xml
COPY ewm-main-service/pom.xml ewm-main-service/pom.xml

RUN mvn -q -f pom.xml -DskipTests -am -pl ewm-main-service dependency:go-offline

COPY . .
RUN mvn -q -f pom.xml -DskipTests -am -pl ewm-main-service package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /build/ewm-main-service/target/*jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
