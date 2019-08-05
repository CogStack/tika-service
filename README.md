# Introduction
This project implements Apache Tika running as a web service using Spring Boot. It exposes a REST API to provide extraction of the text from provided binary documents. The document formats supported are the same as in Tika.


# Building

To build the application, run in the main directory:

`./gradlew build`

The build artifacts will be placed in `./build` directory.


During the build, the tests will be run, where the failed tests can signify missing third-party dependencies (see below). However, to skip running the tests and just build the application, one can run:

`./gradlew bootJar`.


## Tests

To run available tests, run:

`./gradlew test`

Please note that failed tests may signify missing third-party dependencies.


## Dependencies

In the minimal setup, Apache Tika requires the following applications to be present on the system:
- Tesseract OCR
- ImageMagick
- Ghostscript (required by ImageMagick).

In order to enable additional image processing capabilities of Tesseract OCR, few other dependencies need to be present in the system. Please see the provided `Dockerfile` for the full list.


# Running the application

The application can be either run as a standalone Java application or using a provided Docker image.

Please note that when running the application locally, a number of dependencies need to be satisfied, hence the recommended way is to use the provided Docker image.


## Running as a standalone Java application

Assuming that the build went correctly, to run the Tika service on a local machine:

`java -jar build/jar/service-*.jar`

the service will be listening on port `8090` on the host machine.


## Using the Docker image

The Docker image is available in DockerHub under: `cogstacksystems/tika-service:latest`. However, it can be also build locally, using the `Dockerfile` in the main directory.

To run Tika service container:

`docker run -p 8090:8090 cogstacksystems/tika-service:latest`

the service will be listening on port `8090` on the host machine.


# API

## API specification

Tika Service, by default, will be listening on port `8090` and the returned result will be in JSON format. It exposes such endpoints:
- GET `/api/info` - returns information about the service with its configuration,
- POST `/api/process_file` - processes a multi-part file,
- POST `/api/process` - processes a binary data stream with the file content.

## Example use

Using curl to query the Tika server instance running on localhost:

`curl -F file=@test.pdf http://localhost:8090/api/process_file | jq`

Returned result:
`
{
  "result": {
    "text": "Sample Type / Medical Specialty: Lab Medicine - Pathology",
    "metadata": {
      "X-Parsed-By": [
        "org.apache.tika.parser.CompositeParser",
        "org.apache.tika.parser.DefaultParser",
        "org.apache.tika.parser.microsoft.ooxml.OOXMLParser"
      ],
      "X-OCR-Applied": "false",
      "Content-Type": "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    },
    "success": true
  }
}
`

# Configuration

All the service and document processors configurations are stored in a single `src/main/resources/application.yaml` file.



# Mising
- configuration file specs 
- full API specs
