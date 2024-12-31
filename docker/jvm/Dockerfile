###############################################################################
#  Licensed to the Apache Software Foundation (ASF) under one
#  or more contributor license agreements.  See the NOTICE file
#  distributed with this work for additional information
#  regarding copyright ownership.  The ASF licenses this file
#  to you under the Apache License, Version 2.0 (the
#  "License"); you may not use this file except in compliance
#  with the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
# limitations under the License.
###############################################################################

FROM eclipse-temurin:23-jre-alpine AS build-jsa

USER root

ENV KAFKA_HOME=/opt/kafka

# Get kafka from local directory and pass file name through build arguments
ARG kafka_url

COPY jsa_launch /etc/kafka/docker/jsa_launch
COPY $kafka_url kafka.tgz

RUN set -eux ; \
    apk update ; \
    apk upgrade ; \
    apk add --no-cache gcompat procps bash; \
    mkdir $KAFKA_HOME; \
    tar xfz kafka.tgz -C $KAFKA_HOME --strip-components 1; \
	rm kafka.tgz ; \
    chmod +x $KAFKA_HOME/bin/*.sh;

# Generate jsa files using dynamic CDS for kafka server start command and kafka storage format command
RUN /etc/kafka/docker/jsa_launch


FROM eclipse-temurin:23-jre-alpine

# exposed ports
EXPOSE 9092

USER root

ENV KAFKA_HOME=/opt/kafka
ENV STRIMZI_VERSION=0.46.0-kvmorozov

# Get kafka from local directory and pass file name through build arguments
ARG kafka_url
ARG build_date

LABEL org.label-schema.name="kafka" \
      org.label-schema.description="Apache Kafka by kmorozov" \
      org.label-schema.build-date="${build_date}" \
      org.label-schema.vcs-url="https://github.com/apache/kafka" \
      maintainer="kmorozov"

COPY $kafka_url kafka.tgz

RUN set -eux ; \
    apk update ; \
    apk upgrade ; \
    apk add --no-cache curl gcompat procps bash; \
    mkdir $KAFKA_HOME; \
    tar xfz kafka.tgz -C $KAFKA_HOME --strip-components 1; \
    mkdir -p /var/lib/kafka/data /etc/kafka/secrets; \
    mkdir -p /etc/kafka/docker /usr/logs /mnt/shared/config; \
    adduser -h /home/appuser -D --shell /bin/bash appuser; \
    chown appuser:appuser -R /usr/logs $KAFKA_HOME /mnt/shared/config; \
    chown appuser:root -R /var/lib/kafka /etc/kafka/secrets /etc/kafka; \
    chmod -R ug+w /etc/kafka /var/lib/kafka /etc/kafka/secrets; \
    cp $KAFKA_HOME/config/log4j2.yaml /etc/kafka/docker/log4j2.yaml; \
    cp $KAFKA_HOME/config/tools-log4j.properties /etc/kafka/docker/tools-log4j.properties; \
    cp $KAFKA_HOME/config/kraft/reconfig-server.properties /etc/kafka/docker/server.properties; \
    apk cache clean;
	
#####
# Add Tini
#####
ENV TINI_VERSION=v0.19.0
ENV TINI_SHA256_AMD64=93dcc18adc78c65a028a84799ecf8ad40c936fdfc5f2a57b1acda5a8117fa82c

RUN set -ex; \
	curl -s -L https://github.com/krallin/tini/releases/download/${TINI_VERSION}/tini -o /usr/bin/tini; \
	echo "${TINI_SHA256_AMD64} */usr/bin/tini" | sha256sum -c; \
	chmod +x /usr/bin/tini;
	
#####
# Add Kafka Exporter
#####
ENV KAFKA_EXPORTER_HOME=/opt/kafka-exporter
ENV KAFKA_EXPORTER_VERSION=1.8.0
ENV KAFKA_EXPORTER_CHECKSUM_AMD64="bb38dc2c0ce0dcbcf208beab0d75f323e05595486b43dd7d0d478c858621fec828d6070099b3972ac56b5adeaab34947ecf0947b8b3c4f6f8085af1b28043268  kafka_exporter-${KAFKA_EXPORTER_VERSION}.linux-amd64.tar.gz"

RUN set -ex; \
        curl -LO https://github.com/danielqsj/kafka_exporter/releases/download/v${KAFKA_EXPORTER_VERSION}/kafka_exporter-${KAFKA_EXPORTER_VERSION}.linux-amd64.tar.gz; \
        echo $KAFKA_EXPORTER_CHECKSUM_AMD64 > kafka_exporter-${KAFKA_EXPORTER_VERSION}.linux-amd64.tar.gz.sha512; \
        sha512sum --check kafka_exporter-${KAFKA_EXPORTER_VERSION}.linux-amd64.tar.gz.sha512; \
        mkdir $KAFKA_EXPORTER_HOME; \
        tar xvfz kafka_exporter-${KAFKA_EXPORTER_VERSION}.linux-amd64.tar.gz -C $KAFKA_EXPORTER_HOME --strip-components=1; \
        rm -f kafka_exporter-${KAFKA_EXPORTER_VERSION}.linux-amd64.tar.gz*;
		
COPY ./exporter-scripts $KAFKA_EXPORTER_HOME

#####
# Add Prometheus JMX Exporter
#####
ENV JMX_EXPORTER_HOME=/opt/prometheus-jmx-exporter
ENV JMX_EXPORTER_VERSION=1.1.0
ENV JMX_EXPORTER_CHECKSUM="63f9ff5297c6add9ecd19633a54300ffcec0df2e09afca7212e80506b801ea20aa009efc4b582b6ed8a4616b1ed68c98e30cc5b9dbf2cf6fe68b7cf2e3c5db3c  jmx_prometheus_javaagent-${JMX_EXPORTER_VERSION}.jar"

RUN set -ex; \
    curl -LO https://github.com/prometheus/jmx_exporter/releases/download/${JMX_EXPORTER_VERSION}/jmx_prometheus_javaagent-${JMX_EXPORTER_VERSION}.jar; \
    echo $JMX_EXPORTER_CHECKSUM > jmx_prometheus_javaagent-${JMX_EXPORTER_VERSION}.jar.sha512; \
    sha512sum --check jmx_prometheus_javaagent-${JMX_EXPORTER_VERSION}.jar.sha512; \
    mkdir $JMX_EXPORTER_HOME; \
    mv jmx_prometheus_javaagent-${JMX_EXPORTER_VERSION}.jar $JMX_EXPORTER_HOME/; \
    rm -f jmx_prometheus_javaagent-${JMX_EXPORTER_VERSION}.jar.sha512;




COPY ./strimzi_scripts/ $KAFKA_HOME
RUN chmod +x $KAFKA_HOME/*.sh;
RUN chmod +x $KAFKA_HOME/bin/*.sh;
RUN chmod +x $KAFKA_EXPORTER_HOME/*.sh;

COPY --from=build-jsa kafka.jsa $KAFKA_HOME/kafka.jsa
COPY --from=build-jsa storage.jsa $KAFKA_HOME/storage.jsa
COPY --chown=appuser:appuser resources/common-scripts /etc/kafka/docker

#####
# Add Strimzi agent
#####
COPY ./agent/kafka-agent-${STRIMZI_VERSION}.jar ${KAFKA_HOME}/libs/

USER appuser

VOLUME ["/etc/kafka/secrets", "/var/lib/kafka/data", "/mnt/shared/config"]

WORKDIR $KAFKA_HOME
