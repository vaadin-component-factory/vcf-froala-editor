# DOCKERFILE TO BUILD THE DEMO

FROM ghcr.io/jqlang/jq:latest AS jq-stage

FROM eclipse-temurin:21-jdk AS build
COPY --from=jq-stage /jq /usr/bin/jq

ENV HOME=/app
RUN mkdir -p $HOME
WORKDIR $HOME

# The demo's parent is the root pom.
COPY pom.xml $HOME/
COPY component/ $HOME/component/
COPY demo/ $HOME/demo/

# If the build needs a Vaadin license key, pass a Pro key as a secret with id "proKey", or an
# offline key with id "offlineKey":
#
#   $ docker build --secret id=proKey,src=$HOME/.vaadin/proKey .
#   $ docker build --secret id=offlineKey,src=$HOME/.vaadin/offlineKey .

RUN --mount=type=cache,target=/root/.m2 \
    --mount=type=cache,target=/root/.vaadin \
    --mount=type=secret,id=proKey \
    --mount=type=secret,id=offlineKey \
    sh -c 'PRO_KEY=$(jq -r ".proKey // empty" /run/secrets/proKey 2>/dev/null || echo "") && \
    OFFLINE_KEY=$(cat /run/secrets/offlineKey 2>/dev/null || echo "") && \
    demo/mvnw -f component/pom.xml clean install -DskipTests -Dspotless.check.skip=true -Dcheckstyle.skip=true && \
    demo/mvnw -f demo/pom.xml clean package -Pproduction -DskipTests -Dvaadin.proKey=${PRO_KEY} -Dvaadin.offlineKey=${OFFLINE_KEY}'

FROM eclipse-temurin:21-jre-alpine
COPY --from=build /app/demo/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
