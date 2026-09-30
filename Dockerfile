# DOCKERFILE TO BUILD THE DEMO

FROM eclipse-temurin:21-jdk AS build

ENV HOME=/app
RUN mkdir -p $HOME
WORKDIR $HOME

# The demo's parent is the root pom, and the code checks read their config from the repo root.
COPY pom.xml .prettierrc $HOME/
COPY checkstyle/ $HOME/checkstyle/
COPY eclipse/ $HOME/eclipse/
COPY component/ $HOME/component/
COPY demo/ $HOME/demo/

RUN --mount=type=cache,target=/root/.m2 \
    --mount=type=cache,target=/root/.vaadin \
    demo/mvnw -f component/pom.xml clean install -DskipTests && \
    demo/mvnw -f demo/pom.xml clean package -Pproduction -DskipTests

FROM eclipse-temurin:21-jre-alpine
COPY --from=build /app/demo/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
