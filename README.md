# Introduction
Apache Tika running as a web service


# Building

To build run:
`./gradlew bootJar`


# Tests

To run available tests:
`./gradlew test`


# Running

To run the server:
`java -jar build/jar/service-*.jar`


# Configuration

All the configuration are stored in `src/main/resources/application.yaml` file.


# API

Tika Service, by default listening at port `8090` exposes such endpoints:
- GET `/api/info` -- general information about the service with configuration.
- POST `/api/process_file` -- processes a multi-part file.
- POST `/api/process` -- processes a stream with file content.


# Mising
- documentation 
- API specs
