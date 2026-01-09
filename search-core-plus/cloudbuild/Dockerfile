FROM azul/zulu-openjdk:17
WORKDIR /app
ARG PROVIDER_NAME
ENV PROVIDER_NAME core-plus
ARG PORT
ENV PORT $PORT
ENV LOADER_PATH $LOADER_PATH

# Copy plugins
# APD is an optional plugin, required only if Partition does not accept requests without authorization
COPY tmp/apd-openid-*.jar optional/apd-openid.jar

# Copy the jar to the production image from the builder stage.
COPY search-${PROVIDER_NAME}/target/search-${PROVIDER_NAME}-*-spring-boot.jar search-${PROVIDER_NAME}.jar
# Add a non-root user
RUN groupadd -g 10001 -r nonroot \
  && useradd -g 10001 -r -u 10001 nonroot
# Run as non-root user
USER 10001:10001

# Expose debug port
EXPOSE 5005
# Run the web service on container startup.
CMD java --add-opens java.base/java.lang=ALL-UNNAMED \
         --add-opens java.base/java.lang.reflect=ALL-UNNAMED \
         -Djava.security.egd=file:/dev/./urandom -Dserver.port=${PORT} \
         -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 \
         -Dlog4j.formatMsgNoLookups=true \
         -Dloader.path=${LOADER_PATH} \
         -Dloader.main=org.opengroup.osdu.search.SearchCorePlusApplication \
         -jar /app/search-${PROVIDER_NAME}.jar
