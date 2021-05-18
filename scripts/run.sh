#!/usr/bin/env bash

java -Dspring.config.location=/app/config/ -XX:+UseContainerSupport -XshowSettings:vm "$JAVA_OPTIONS" -jar /app/tika-service-*.jar