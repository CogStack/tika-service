#!/usr/bin/env bash

java -Dspring.config.location=/app/config/ -XX:+UseContainerSupport -jar /app/tika-service-*.jar
