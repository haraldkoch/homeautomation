FROM java:8-alpine
MAINTAINER Your Name <you@example.com>

ADD target/uberjar/homeautomation.jar /homeautomation/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/homeautomation/app.jar"]
