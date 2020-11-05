FROM adoptopenjdk/openjdk11:alpine-jre
ADD build/bootScripts/* /app/bin/
ADD build/libs/* /app/lib/
WORKDIR /app/files
CMD ["/app/bin/marlin-console-configurator"]
