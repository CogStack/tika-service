# Introduction
This project implements Apache Tika running as a web service using Spring Boot. It exposes a REST API so that a client can send a document in binary format and receive back the extracted text. The supported document formats are the ones as in Tika.

Some of the key motivation behind developing own wrapper over Tika instead of using the already available [Tika server](https://cwiki.apache.org/confluence/display/tika/TikaJAXRS) is a better control over used document parsers (such as PDFParser, Tesseract OCR and the legacy one taken from [CogStack-Pipeline](https://github.com/CogStack/CogStack-Pipeline)) and control over returned results with HTTP return codes.


# Building
To build the application, run in the main directory:

`./gradlew build --console=plain`

The build artifacts will be placed in `./build` directory.


During the build, the tests will be run, where the failed tests can also signify missing third-party dependencies (see below). However, to skip running the tests and just build the application, one can run:

`./gradlew bootJar --console=plain`.


## Tests
To run the available tests, run:

`./gradlew test --console=plain`.

Please note that failed tests may signify missing third-party dependencies.

## Third-party dependencies
In the minimal setup, for proper text extraction Apache Tika requires the following applications to be present on the system:
- [Tesseract OCR](https://github.com/tesseract-ocr/tesseract),
- [ImageMagick](https://imagemagick.org),
- [Ghostscript](https://www.ghostscript.com/) (required by ImageMagick for documents conversion).

ImageMagick also requires its configuration file `policy.xml` to be overriden by the provided `extras/ImageMagick/policy.xml` (in order to increase the available resources for file processing and to override [security policy](https://stackoverflow.com/questions/52703123/override-default-imagemagick-policy-xml) related with Ghostscript).

Moreover, in order to enable additional image processing capabilities of Tesseract OCR, few other dependencies need to be present in the system, such as Python environment. Please see the provided `Dockerfile` for the full list.

# Running the application
The application can be either run as a standalone Java application or inside a Docker container. The application configuration can be changed in the `application.yaml` file. The default version of configuration file is embeded in the jar file, but can be specified manually (see below).

Please note that the recommended way is to use the provided Docker image since a number of dependencies need to be satisfied on a local machine.

## Running as a standalone Java application
Assuming that the build went correctly, to run the Tika service on a local machine:

`java -jar build/jar/service-*.jar`

The running service will be listening on port `8090` (by default) on the host machine. 

## Using the Docker image
The latest stable Docker image is available in the Docker Hub under `cogstacksystems/tika-service:latest` tag. Alternatively, the latest development version is available under `cogstacksystems/tika-service:dev-latest` tag. The image can be also build locally using the provided `Dockerfile`.


To run Tika service container:

`docker run -p 8090:8090 cogstacksystems/tika-service:latest`

The service will be listening on port `8090` on the host machine.

## Important settings for TESSERACT OCR 
When processing large documents (and in large amounts) it is important to benchmark the performance depending on the use case. 
An important setting which is enabled by default is the Tesseract thread limit option which is suitable for deployments where only small-sized documents are to be processed, this is because threading in such cases can do more harm than good since the program will have to start up and manage threads for small jobs most of the time. 

By default, this setting is found in the '/docker/docker_compose.yml' file, enabled as a global variable: 
```
  environment:
    OMP_THREAD_LIMIT=1
```

For more information: https://github.com/tesseract-ocr/tesseract/blob/7c3ac569f9c320bdc4bacea0ec66c69e2cf06a32/doc/tesseract.1.asc#environment-variables


# API

## API specification
Tika Service, by default, will be listening on port `8090` and the returned content extraction result will be represented in JSON format. 

The service exposes such endpoints:
- *GET* `/api/info` - returns information about the service with its configuration,
- *POST* `/api/process` - processes a binary data stream with the binary document content,
- *POST* `/api/process_file` - processes a document file (multi-part request).

## Document extraction result
The extraction results are represented in JSON format where the available main fields are:
- `result` - the content extraction result with metadata,
- `timestamp` - the content processing timestamp,
- `success` - specifies whether the extraction accomplished successfully,
- `error` - the message in case of processing error (assumes `success : false`).

The content extraction result can contain such fields:
- `text` - the extracted text,
- `metadata` - the metadata associated with the document and the used parsers.

The provided metadata associated with the document and the used parsers can include such fields:
- `X-Parsed-By` - an array of names of the parsers used during the content extraction,
- `X-OCR-Applied` - a flag specifying whether OCR was applied,
- `Content-Type` - the content type of the document, as identified by Tika,
- `Page-Count` - the document page count (extracted from the document metadata by Tika),
- `Creation-Date` - the document creation date (extracted from the document metadata by Tika).


# Example use
Using `curl` to send the document to Tika server instance running on localhost on `8090` port:

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

## Bulk processing

For this feature to work you must set the following `use-legacy-tika-processor-as-default: false` in application.yaml.

`curl -F file=@test1.pdf -F file=@test2.pdf http://localhost:8090/api/process_bulk`

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
All the available service and document processors parameteres are stored in a single `src/main/resources/application.yaml` file. 

Although the initial configuration file is bundled with the application jar file, a modified one can be provided as a parameter when running the Java application. For example, when running the Tika service in the Docker container, the script `scripts/run.sh` runs the Tika service with custom configuration file `application.yaml` located in `/app/config/` directory: 
`java -Dspring.config.location=/app/config/ -jar /app/service-*.jar`

## Available properties
The configuration file is stored in yaml format with the following available properties.

### General application properties
- `application.version` - specifies the application version,
- `server.port` - the port number on which the service will be run (default: `8090`),
- `spring.servlet.multipart.max-file-size` and `spring.servlet.multipart.max-request-size` - specifies the max file size when processing file requests (default: `100MB`).

### Tika service configuration
The following keys reside under `tika.processing` node:
- `use-legacy-tika-processor-as-default` - whether to use the legacy Tika PDF parser (as used in CogStack Pipeline) for backward compatibility (default: `true`),
- `fail-on-empty-files` - whether to fail the request and report an error when client provided an empty document (default: `false`),
- `fail-on-non-document-types` - whether to fail the request and report an erorr when client provided a not supported and/or non-document content (default: `true`).

### Tika parsers configuration
The following keys reside under `tika.parsers` node.

The keys under `tesseract-ocr` define the default behavior of the Tika Tesseract OCR parser:
- `language` - the language dictionary used by Tesseract (default: `eng`),
- `timeout` - the max time (ms) to process documents before reporting error (default: `300`),
- `enable-image-processing` - whether to use additional pre-processing of the images using ImageMagick (default: `false`),
- `apply-rotation` - whether to apply de-rotating of the images (default: `false`),
Please note that enabling `enable-image-processing` and/or `apply-rotation` although might improve the quality of the extracted text can significantly slower the extraction process.

The keys under `pdf-ocr-parser` define the default behavior of the PDF parser that uses Tesseract OCR to extract the text:
- `ocr-only-strategy` - whether to use only OCR or to apply additional text extraction from the content (default: `true`),
- `min-doc-text-length` - if the available text in the document (before applying OCR) is higher than this value then skip OCR (default: `100`),
- `min-doc-byte-size` - the minimum size of the image data (in bytes) that should have the content to be extracted, otherwise is skipped (default: `10000`),
- `use-legacy-ocr-parser-for-single-page-doc` - in case of single-page PDF documents, whether to use the legacy parser (default: `false`).

The keys under `legacy-pdf-parser` define the behavior of the Tika PDF parser used in CogStack Pipeline (the 'legacy' parser), that is used for backward compatibility:
- `image-magick.timeout` - the max timeout value (in ms) when performing document conversion using ImageMagick (default: `300`),
- `tesseract-ocr.timeout` - the max timeout value (in ms) when performing text extraction using Tesseract OCR (default: `300`),
- `min-doc-text-length` - if the available text in the document (before applying OCR) is higher than this value then skip OCR (default: `100`).