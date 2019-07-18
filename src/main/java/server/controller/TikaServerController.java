package server.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import tika.legacy.LegacyTikaProcessor;
import server.model.ServiceResponseContent;
import tika.model.TikaProcessingResult;
import tika.processor.AbstractTikaProcessor;
import tika.processor.CompositeTikaProcessor;
import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;


@RestController
@ComponentScan({"tika.legacy", "tika.processor"})
public class TikaServerController {

    private final String apiPathPrefix = "/**/api";
    //private final String apiVersion = "v1";
    private final String apiFullPath = apiPathPrefix;


    private Logger log = LoggerFactory.getLogger(TikaServerController.class);

    @Autowired
    @Qualifier("legacyTikaProcessor")
    private LegacyTikaProcessor legacyTikaProcessor;

    @Autowired
    @Qualifier("compositeTikaProcessor")
    private CompositeTikaProcessor compositeTikaProcessor;


    @PostMapping(value = apiFullPath + "/process")
    public ResponseEntity<ServiceResponseContent> process(HttpServletRequest request,
                                                          @RequestParam(name = "processor", required = false) String processorName) {

        final boolean useLegacyProcessor = (processorName != null && processorName.equals("legacy"));

        log.info("Processing request...");

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
            ServiceResponseContent response = processStream(bufs, useLegacyProcessor);


            // remember to actually check the processing status
            if (response.getResult().getSuccess())
                return new ResponseEntity<>(response, HttpStatus.OK);

            // an error occurred during processing
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        catch (Exception e) {
            final String message = "Error processing the query: " + e.getMessage();
            log.error(message);

            return new ResponseEntity<>(createErrorResponse(message), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    

    @PostMapping(value = apiFullPath + "/process_file", consumes = { "multipart/form-data" })
    public ResponseEntity<ServiceResponseContent> process(@RequestParam("file") MultipartFile file,
                                                          @RequestParam(name = "processor", required = false) String processorName) {

        final boolean useLegacyProcessor = (processorName != null && processorName.equals("legacy"));

        log.info("Received a new document -- processing...");

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

            ServiceResponseContent response = processStream(bufs, useLegacyProcessor);

            // remember to actually check the processing status
            if (response.getResult().getSuccess())
                return new ResponseEntity<>(response, HttpStatus.OK);

            // an error occurred during processing
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
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


    private ServiceResponseContent processStream(ByteArrayInputStream stream, boolean useLegacyProcessor) {
        AbstractTikaProcessor processor;
        if (useLegacyProcessor) {
            processor = legacyTikaProcessor;
        }
        else {
            processor = compositeTikaProcessor;
        }
        log.debug("Running processor: " + processor.getClass().toString());

        TikaProcessingResult result = processor.process(stream);
        ServiceResponseContent response = new ServiceResponseContent();
        response.setResult(result);

        return response;
    }
}
