FROM openjdk:8-slim
WORKDIR /app
COPY ../target/search-reference-0.0.5-SNAPSHOT-spring-boot.jar search-reference.jar
# Run the web service on container startup.
CMD java -Djava.security.egd=file:/dev/./urandom -Dserver.port=8080 -jar /app/search-reference.jar