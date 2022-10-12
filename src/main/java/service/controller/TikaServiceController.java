package service.controller;

import com.fasterxml.jackson.annotation.JsonView;
import common.JsonPropertyAccessView;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tika.io.TemporaryResources;
import org.apache.tika.io.TikaInputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import service.model.ServiceInformation;
import service.model.ServiceResponseContent;
import tika.legacy.LegacyTikaProcessor;
import tika.model.TikaProcessingResult;
import tika.processor.AbstractTikaProcessor;
import tika.processor.CompositeTikaProcessor;
import tika.utils.TikaUtils;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.nio.file.Files;


/**
 * Main Tika Service REST controller
 */
@RestController
@ComponentScan({"tika.legacy", "tika.processor"})
public class TikaServiceController implements ErrorController {

    private final String apiPathPrefix = "/api";
    private final String apiFullPath = apiPathPrefix;

    private final Logger logger = LogManager.getLogger(TikaServiceController.class);

    /**
     * Tika document processors
     */
    @Autowired
    @Qualifier("legacyTikaProcessor")
    private LegacyTikaProcessor legacyTikaProcessor;

    @Autowired
    @Qualifier("compositeTikaProcessor")
    private CompositeTikaProcessor compositeTikaProcessor;

    /**
     * All the necessary information about the service, incl. config
     */
    @Autowired
    ServiceInformation serviceInfo;

    private AbstractTikaProcessor tikaProcessor;

    @PostConstruct
    void init() {
        // select the appropriate document processor depending on the configuration
        if (serviceInfo.getServiceConfig().isUseLegacyTikaProcessor()) {
            tikaProcessor = legacyTikaProcessor;
        }
        else {
            tikaProcessor = compositeTikaProcessor;
        }
    }

    /**
     * The endpoint returning service information with configuration
     */
    @GetMapping(value = apiFullPath + "/info", produces = "application/json")
    @JsonView(JsonPropertyAccessView.Public.class)
    public @ResponseBody
        ServiceInformation info() {
            return serviceInfo;
    }

    @GetMapping(value = "/")
    public String home() {
        return "Tika Service, you can see the current configuration of the service by going to /api/info";
    }

    /**
     * The endpoint used for processing documents (e.g. sent as [octet] stream)
     */
    @PostMapping(value = apiFullPath + "/process", produces = "application/json")
    public ResponseEntity<ServiceResponseContent> process(HttpServletRequest request) {
        try {
            byte[] streamContent = IOUtils.toByteArray(request.getInputStream());

            if (streamContent.length == 0) {
                final String message = "Empty content";
                logger.info(message);
                return createEmptyDocumentResponseEntity(message);
            }

            // we are buffering the stream using ByteArrayInputStream in order to enable
            // re-reading the binary document content
            ByteArrayInputStream byteBuffer = new ByteArrayInputStream(streamContent);

            TikaProcessingResult result = processStream(byteBuffer);

            return createProcessedDocumentResponseEntity(result);
        }
        catch (Exception e) {
            final String message = "Error processing the query: " + e.getMessage();
            logger.error(message);
            e.printStackTrace();
            return new ResponseEntity<>(createErrorResponse(message), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping(name="process_bulk", value = apiFullPath + "/process_bulk", consumes = "multipart/form-data", produces = "application/json")
    public ResponseEntity<ServiceResponseContent> process(@RequestParam("file") MultipartFile[] multipartFiles) {
        if(multipartFiles.length == 0)
        {
            final String message = "Empty content, no files were sent.";
            logger.info(message);
            return createEmptyDocumentResponseEntity(message);
        }

        try {
            logger.info("Bulk processing number of files : " + multipartFiles.length);
            logger.info("Running processor: " + tikaProcessor.getClass().toString());

            var results = tikaProcessor.process(multipartFiles);

            ServiceResponseContent serviceResponseContent = new ServiceResponseContent();

            return new ResponseEntity<ServiceResponseContent>(serviceResponseContent, HttpStatus.OK);
        }
        catch (Exception e) {
            final String message = "Error processing: " + e.getMessage();
            logger.error(message);
            e.printStackTrace();
            return new ResponseEntity<>(createErrorResponse(message), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * The endpoint used for processing documents sent as multipart files
     */
    @PostMapping(value = apiFullPath + "/process_file", consumes = { "multipart/form-data" }, produces = "application/json")
    public ResponseEntity<ServiceResponseContent> process(@RequestParam("file") MultipartFile file) {
        // check whether we need to perform any processing
        if (file.isEmpty()) {
            final String message = "Empty content";
            logger.info(message);
            return createEmptyDocumentResponseEntity(message);
        }

        // process the content
        try {
            // we are buffering the stream using ByteArrayInputStream in order to enable
            // re-reading the binary document content
            ByteArrayInputStream byteBuffer = new ByteArrayInputStream(file.getBytes());

            TikaProcessingResult result = processStream(byteBuffer);
            return createProcessedDocumentResponseEntity(result);
        }
        catch (Exception e) {
            final String message = "Error processing the query: " + e.getMessage();
            logger.error(message);
            e.printStackTrace();
            return new ResponseEntity<>(createErrorResponse(message), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private ServiceResponseContent createErrorResponse(String message) {
        ServiceResponseContent response = new ServiceResponseContent();
        TikaProcessingResult result = TikaProcessingResult.builder()
                .success(false)
                .error(message).build();
        response.setResult(result);
        return response;
    }

    private TikaProcessingResult processStream(ByteArrayInputStream stream) throws IOException {
        TemporaryResources temporaryResources = new TemporaryResources();
        File tmpFilePath = temporaryResources.createTemporaryFile();

        logger.info("Storing tmp file at :" + tmpFilePath.toString());

        TikaInputStream tikaInputStream = TikaInputStream.get(stream, temporaryResources);
        TikaProcessingResult result = tikaProcessor.process(tikaInputStream);

        if (tmpFilePath.exists()) {
            logger.info("Deleting tmp file:" + tmpFilePath.toPath());
            Files.delete(tmpFilePath.toPath());
        }
        tikaInputStream.close();
        logger.info("Running processor: " + tikaProcessor.getClass().toString());

        return result;
    }

    private ResponseEntity<ServiceResponseContent> createEmptyDocumentResponseEntity(String errorMessage) {
        HttpStatus status;
        if (serviceInfo.getServiceConfig().isFailOnEmptyFiles()) {
            status = HttpStatus.BAD_REQUEST;
        }
        else {
            status = HttpStatus.OK;
        }

        return new ResponseEntity<>(createErrorResponse(errorMessage), status);
    }

    private ResponseEntity<ServiceResponseContent> createProcessedDocumentResponseEntity(TikaProcessingResult result) {
        // remember to actually check the processing status
        HttpStatus status;
        if (result.getSuccess()) {
            if (serviceInfo.getServiceConfig().isFailOnNonDocumentTypes()
                    & !TikaUtils.isValidDocumentType(result.getMetadata())) {
                // assume fail on non-document types
                status = HttpStatus.BAD_REQUEST;
            }
            else {
                status = HttpStatus.OK;
            }
        }
        else {
            // an error occurred during processing -- assume it's actually faulty document
            status = HttpStatus.BAD_REQUEST;
        }

        ServiceResponseContent response = new ServiceResponseContent();
        response.setResult(result);
        return new ResponseEntity<>(response, status);
    }

    @RequestMapping("/error")
    public String handleError() {
        return "Error, the page could not be found.";
    }

    public String getErrorPath() {
        return null;
    }
}
