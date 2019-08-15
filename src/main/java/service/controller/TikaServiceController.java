package service.controller;

import com.fasterxml.jackson.annotation.JsonView;
import common.JsonPropertyAccessView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;


/**
 * Main Tika Service REST controller
 */
@RestController
@ComponentScan({"tika.legacy", "tika.processor"})
public class TikaServiceController {

    private final String apiPathPrefix = "/**/api";
    //private final String apiVersion = "v1";
    private final String apiFullPath = apiPathPrefix;

    private Logger log = LoggerFactory.getLogger(TikaServiceController.class);

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


    /**
     * The endpoint used for processing documents (e.g. sent as [ocet] stream)
     */
    @PostMapping(value = apiFullPath + "/process", produces = "application/json")
    public ResponseEntity<ServiceResponseContent> process(HttpServletRequest request) {
        try {
            byte[] streamContent = request.getInputStream().readAllBytes();
            if (streamContent.length == 0) {
                final String message = "Empty content";
                log.info(message);

                return createEmptyDocumentResponseEntity(message);
            }

            // we are buffering the stream using ByteArrayInputStream in order to enable
            // re-reading the binary document content
            ByteArrayInputStream bufs = new ByteArrayInputStream(streamContent);
            TikaProcessingResult result = processStream(bufs);

            return createProcessedDocumentResponseEntiy(result);
        }
        catch (Exception e) {
            final String message = "Error processing the query: " + e.getMessage();
            log.error(message);

            return new ResponseEntity<>(createErrorResponse(message), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    /**
     * The endpoint used for processing documents sent as multi-part files
     */
    @PostMapping(value = apiFullPath + "/process_file", consumes = { "multipart/form-data" }, produces = "application/json")
    public ResponseEntity<ServiceResponseContent> process(@RequestParam("file") MultipartFile file) {
        // check whether we need to perform any processing
        if (file.isEmpty()) {
            final String message = "Empty content";
            log.info(message);

            return createEmptyDocumentResponseEntity(message);
        }

        // process the content
        try {
            // we are buffering the stream using ByteArrayInputStream in order to enable
            // re-reading the binary document content
            ByteArrayInputStream bufs = new ByteArrayInputStream(file.getBytes());

            TikaProcessingResult result = processStream(bufs);
            return createProcessedDocumentResponseEntiy(result);
        }
        catch (Exception e) {
            final String message = "Error processing the query: " + e.getMessage();
            log.error(message);

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


    private TikaProcessingResult processStream(ByteArrayInputStream stream) {
        log.info("Running processor: " + tikaProcessor.getClass().toString());
        return tikaProcessor.process(stream);
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

    private ResponseEntity<ServiceResponseContent> createProcessedDocumentResponseEntiy(TikaProcessingResult result) {
        // remember to actually check the processing status
        HttpStatus status;
        if (result.getSuccess()) {
            if (serviceInfo.getServiceConfig().isFailOnNonDocumentTypes()
                    & !AbstractTikaProcessor.isValidDocumentType(result.getMetadata())) {
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
}
