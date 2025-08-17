FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

COPY pom.xml pom.xml
COPY stats/pom.xml stats/pom.xml
COPY stats/stats-dto/pom.xml stats/stats-dto/pom.xml
COPY stats/stats-server/pom.xml stats/stats-server/pom.xml
COPY stats/stats-client/pom.xml stats/stats-client/pom.xml
COPY ewm-main-service/pom.xml ewm-main-service/pom.xml

RUN mvn -q -f pom.xml -DskipTests -am -pl stats/stats-server dependency:go-offline

COPY . .
RUN mvn -q -f pom.xml -DskipTests -am -pl stats/stats-server package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/stats/stats-server/target/*jar app.jar
EXPOSE 9090
ENTRYPOINT ["java","-jar","/app/app.jar"]
