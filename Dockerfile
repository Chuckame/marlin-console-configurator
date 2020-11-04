FROM adoptopenjdk/openjdk11:alpine-jre
RUN mkdir /opt/app
COPY build/distributions/marlin-console-configurator.zip /opt/app
RUN tar xvf /opt/app/marlin-console-configurator.zip -C /opt/app
RUN chmod +x /opt/app/bin/marlin-console-configurator
CMD ["/opt/app/bin/marlin-console-configurator"]
