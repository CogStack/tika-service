# Introduction
This project implements Apache Tika running as a web service using Spring Boot. It exposes a REST API so that a client can send a document in binary format and receive back the extracted text. The supported document formats are the ones as in Tika.


# Building
To build the application, run in the main directory:

`./gradlew build`

The build artifacts will be placed in `./build` directory.


During the build, the tests will be run, where the failed tests can also signify missing third-party dependencies (see below). However, to skip running the tests and just build the application, one can run:

`./gradlew bootJar`.


## Tests
To run available tests, run:

`./gradlew test`

Please note that failed tests may signify missing third-party dependencies.


## Dependencies
In the minimal setup, Apache Tika requires the following applications to be present on the system:
- Tesseract OCR,
- ImageMagick,
- Ghostscript (required by ImageMagick).

ImageMagick also requires its configuration file `policy.xml` to be overriden by the provided `extras/ImageMagick/policy.xml`.

Moreover, in order to enable additional image processing capabilities of Tesseract OCR, few other dependencies need to be present in the system. Please see the provided `Dockerfile` for the full list.


# Running the application
The application can be either run as a standalone Java application or inside the Docker container.

Please note that when running the application locally, a number of dependencies need to be satisfied, hence the recommended way is to use the provided Docker image.


## Running as a standalone Java application
Assuming that the build went correctly, to run the Tika service on a local machine:

`java -jar build/jar/service-*.jar`

the running service will be listening on port `8090` on the host machine. The port can be changed in the `application.yaml` configuration file.


## Using the Docker image
The latest stable Docker image is available in DockerHub under: `cogstacksystems/tika-service:latest`. Alternatively, the latest development version is `cogstacksystems/tika-service:dev-latest`. 

The image can be also build locally, using the provided `Dockerfile` in the main directory.


To run Tika service container, run:

`docker run -p 8090:8090 cogstacksystems/tika-service:latest`

the service will be listening on port `8090` on the host machine.


# API

## API specification
Tika Service, by default, will be listening on port `8090` and the returned content extraction result will be represented in JSON format. 

The service exposes such endpoints:
- *GET* `/api/info` - returns information about the service with its configuration,
- *POST* `/api/process` - processes a binary data stream with the binary document content,
- *POST* `/api/process_file` - processes a document file (multi-part request).

## Document extraction result
The extraction results are represented in JSON format where the available main fields are:
- `result` -- holds the content extraction result,
- `timestamp` -- the content processing timestamp,
- `success` -- specifies whether the extraction accomplished successfully,
- `error` -- the message in case of processing error (assumes `success : false`).

The content extraction result can contain such fields:
- `text` -- contains the extracted text,
- `metadata` -- contains metadata associated with the document.

The provided metadata associated with the document and processing can include such fields:
- `X-Parsed-By` -- array of names of the Tika parsers used during the content extraction,
- `X-OCR-Applied` -- a flag specifying whether OCR was applied,
- `Content-Type` -- the format type of the document, as identified by Tika,
- `Page-Count` -- document page count (extracted from the document metadata),
- `Creation-Date` -- document creation date (extracted from the document metadata).


## Example use
Using `curl` to send the document to Tika server instance running on localhost:

`curl -F file=@test.pdf http://localhost:8090/api/process_file | jq`

Returned result:
```
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
    "success": true,
    "timestamp": "2019-08-13T15:14:58.022+01:00"
  }
}
```

# Configuration

## Configuration file
All the service and document processors configurations are stored in a single `src/main/resources/application.yaml` file. 

Although the initial configuration file is bundled with the application, a modified one can be provided as a parameter when running the Java application. For example, when running the Tika service in the Docker container, the script `scripts/run.sh` runs the Tika service with custom configuration file `application.yaml` located in `/app/config/` directory: 
`java -Dspring.config.location=/app/config/ -jar /app/service-*.jar`


## Available properties
The configuration file is stored in yaml format with such available properties:

### General application properties
- `application.version` -- specifies the application version,
- `server.port` -- the port number on which the service will be run (default: `8090`),
- `spring.servlet.multipart.max-file-size` and `spring.servlet.multipart.max-request-size` -- specifies max file size when processing file requests.


### Tika service configuration
The following keys reside under `tika.processing` node:
- `use-legacy-tika-processor-as-default` -- whether to use the legacy Tika parser (as used in CogStack Pipeline) for backward compatibility,
- `fail-on-empty-files` -- whether to fail the request and report an error when client provided an empty document,
- `fail-on-non-document-types` -- whether to fail the request and report an erorr when client provided a not supported and/or non-document content,


### Tika parsers configuration
The following keys reside under `tika.parsers` node.

The keys under `tesseract-ocr` define the default behavior of the Tika Tesseract OCR parser:
- `language` -- the language dictionary used by Tesseract (default: `eng`),
- `timeout` -- the max time (ms) to process documents before reporting error (default: `300`),
- `enable-image-processing` -- whether to use additional pre-processing of the images using ImageMagick (default: `false`),
- `apply-rotation` -- whether to apply de-rotating of the images (default: `false` ; note that this runs an external Python application and being computationally expensive can significantly slower the extraction process).

The keys under `pdf-ocr-parser` define the default behavior of the PDF parser that uses OCR to extract the text:
- `ocr-only-strategy` -- whether to use only OCR or to apply additional text extraction from the content (default: `true`),
- `min-doc-text-length` -- the minimum expected length of the text in the document to be extracted (default: `100`),
- `min-doc-byte-size` -- the minimum expected size of the document (in bytes) that should have content to be extracted (default: `10000`),
- `use-legacy-ocr-parser-for-single-page-doc` -- in case of single-page PDF documents, whether to use the legacy parser (default: `false`).

The keys under `legacy-pdf-parser` define the behavior of the Tika PDF parser used in CogStack Pipeline (legacy), that is used for backward compatibility:
- `image-magick.timeout` -- the max timeout value (in ms) when performing document conversion (default: `300`),
- `tesseract-ocr.timeout` -- the max timeout value (in ms) when performin OCR (default: `300`),
- `min-doc-text-length` --  the minimum expected length of the text in the document to be extracted (default: `100`).

