# Используем официальный образ OpenJDK 17
FROM openjdk:17-jdk-slim

# Устанавливаем рабочую директорию в контейнере
WORKDIR /usr/app

# Копируем скомпилированный JAR файл из папки target в контейнер
COPY target/sms-activation-service-0.0.1-SNAPSHOT.jar /usr/app/sms-activation-service.jar

# Указываем команду для запуска приложения
CMD ["java", "-jar", "sms-activation-service.jar"]

# Открываем порт для вашего приложения
EXPOSE 8080
