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


@RestController
@ComponentScan({"tika.legacy", "tika.processor"})
public class TikaServiceController {

    private final String apiPathPrefix = "/**/api";
    //private final String apiVersion = "v1";
    private final String apiFullPath = apiPathPrefix;


    private Logger log = LoggerFactory.getLogger(TikaServiceController.class);

    @Autowired
    @Qualifier("legacyTikaProcessor")
    private LegacyTikaProcessor legacyTikaProcessor;

    @Autowired
    @Qualifier("compositeTikaProcessor")
    private CompositeTikaProcessor compositeTikaProcessor;

    @Autowired
    ServiceInformation serviceInfo;

    private AbstractTikaProcessor tikaProcessor;


    @PostConstruct
    void init() {
        if (serviceInfo.getServiceConfig().isUseLegacyTikaProcessor()) {
            tikaProcessor = legacyTikaProcessor;
        }
        else {
            tikaProcessor = compositeTikaProcessor;
        }
    }


    @GetMapping(value = apiFullPath + "/info", produces = "application/json")
    @JsonView(JsonPropertyAccessView.Public.class)
    public @ResponseBody
    ServiceInformation info() {
        return serviceInfo;
    }


    @PostMapping(value = apiFullPath + "/process", produces = "application/json")
    public ResponseEntity<ServiceResponseContent> process(HttpServletRequest request) {

        //log.info("Processing request...");

        // process the content
        //
        try {
            byte[] streamContent = request.getInputStream().readAllBytes();

            if (streamContent.length == 0) {
                final String message = "Empty content";
                log.info(message);

                return new ResponseEntity<>(createErrorResponse(message), HttpStatus.BAD_REQUEST);
            }

            // process
            //
            ByteArrayInputStream bufs = new ByteArrayInputStream(streamContent);
            ServiceResponseContent response = processStream(bufs);


            // remember to actually check the processing status
            if (response.getResult().getSuccess())
                return new ResponseEntity<>(response, HttpStatus.OK);

            // an error occurred during processing -- assume it's a faulty document
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
        catch (Exception e) {
            final String message = "Error processing the query: " + e.getMessage();
            log.error(message);

            return new ResponseEntity<>(createErrorResponse(message), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    

    @PostMapping(value = apiFullPath + "/process_file", consumes = { "multipart/form-data" }, produces = "application/json")
    public ResponseEntity<ServiceResponseContent> process(@RequestParam("file") MultipartFile file) {

        //log.info("Received a new document -- processing...");

        // check whether we need to perform any processing
        //
        if (file.isEmpty()) {
            final String message = "Empty content";
            log.info(message);

            return new ResponseEntity<>(createErrorResponse(message), HttpStatus.BAD_REQUEST);
        }


        // process the content
        //
        try {
            ByteArrayInputStream bufs = new ByteArrayInputStream(file.getBytes());

            ServiceResponseContent response = processStream(bufs);

            // remember to actually check the processing status
            if (response.getResult().getSuccess())
                return new ResponseEntity<>(response, HttpStatus.OK);

            // an error occurred during processing -- assume it's a faulty document
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
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


    private ServiceResponseContent processStream(ByteArrayInputStream stream) {
        log.info("Running processor: " + tikaProcessor.getClass().toString());

        TikaProcessingResult result = tikaProcessor.process(stream);
        ServiceResponseContent response = new ServiceResponseContent();
        response.setResult(result);

        return response;
    }
}
