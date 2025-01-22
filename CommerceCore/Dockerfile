
FROM openjdk:17-jdk-slim
WORKDIR /usr/app
COPY target/sms-activation-service-0.0.1-SNAPSHOT.jar /usr/app/sms-activation-service.jar
CMD ["java", "-jar", "sms-activation-service.jar"]
EXPOSE 8080
