package tika.server.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import tika.model.ServiceRequestContent;
import tika.model.ServiceResponseContent;
import tika.model.TikaProcessingResult;

import tika.cogstack.TikaProcessor;
import tika.processor.CompositeTikaProcessor;


import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.InputStream;


@RestController
public class TikaController {

    private final String apiPathPrefix = "/**/api";
    //private final String apiVersion = "v1";
    private final String apiFullPath = apiPathPrefix;


    private Logger log = LoggerFactory.getLogger(TikaController.class);

    private TikaProcessor cogstackTikaProcessor;
    private CompositeTikaProcessor tikaProcessor;


    @PostConstruct
    void init() throws Exception {
        cogstackTikaProcessor = new TikaProcessor();
        tikaProcessor = new CompositeTikaProcessor();
    }


    /*
    @PostMapping(value = apiFullPath + "/process")
    public ResponseEntity<ServiceResponseContent> process(@RequestBody ServiceRequestContent content) {

        ServiceResponseContent response = new ServiceResponseContent();

        // check whether we need to perform any processing
        //
        if (content.getDocument() == null || content.getDocument().getContent() == null
                || content.getDocument().getContent().length == 0) {
            final String message = "Empty content";
            log.info(message);

            TikaProcessingResult result = TikaProcessingResult.builder()
                    .success(false)
                    .errorMessage(message).build();
            response.setResult(result);

            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        // process the content
        //
        try {
            TikaProcessingResult result = cogstackTikaProcessor.process(content.getDocument());
            response.setResult(result);
        }
        catch (Exception e) {
            final String message = "Error processing the query: " + e.getMessage();
            log.error(message);

            TikaProcessingResult result = TikaProcessingResult.builder()
                    .success(false)
                    .errorMessage(message).build();
            response.setResult(result);

            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // set the original footer to return it back to the client
        //
        //response.getResult().setFooter(content.getContent().getFooter());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    */


    @PostMapping(value = apiFullPath + "/process_file", consumes = { "multipart/form-data" })
    public ResponseEntity<ServiceResponseContent> process(@RequestParam("file") MultipartFile file,
                                                          @RequestParam(name = "processor", required = false) String processorName) {

        ServiceResponseContent response = new ServiceResponseContent();

        final boolean useCogstackProcessor = (processorName != null && processorName.equals("cogstack"));


        // check whether we need to perform any processing
        //
        if (file.isEmpty()) {
            final String message = "Empty content";
            log.info(message);

            TikaProcessingResult result = TikaProcessingResult.builder()
                    .success(false)
                    .errorMessage(message).build();
            response.setResult(result);

            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        // process the content
        //
        try {
            //InputStream bufs = new BufferedInputStream(file.getInputStream(), (int)file.getSize());
            InputStream bufs = new ByteArrayInputStream(file.getBytes());

            TikaProcessingResult result;
            if (useCogstackProcessor) {
                result = cogstackTikaProcessor.process(bufs);
            }
            else {
                result = tikaProcessor.process(bufs);
            }
            response.setResult(result);
        }
        catch (Exception e) {
            final String message = "Error processing the query: " + e.getMessage();
            log.error(message);

            TikaProcessingResult result = TikaProcessingResult.builder()
                    .success(false)
                    .errorMessage(message).build();
            response.setResult(result);

            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // set the original footer to return it back to the client
        //
        //response.getResult().setFooter(content.getContent().getFooter());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

}
