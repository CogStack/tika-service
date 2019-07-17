package tika.cogstack.legacy;

import java.util.*;

import org.apache.tika.config.TikaConfig;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import tika.model.TikaProcessingResult;
import tika.processor.AbstractTikaProcessor;


@Component("legacyTikaProcessor")
public class TikaProcessor extends AbstractTikaProcessor {

    private AutoDetectParser defaultParser;
    private ParseContext defaultParseContext;
    private PdfProcessorConfig config;

    private Logger log = LoggerFactory.getLogger(TikaProcessor.class);


    public TikaProcessor() throws Exception {

        config = new PdfProcessorConfig();

        defaultParseContext = new ParseContext();
        defaultParseContext.set(TikaConfig.class, config.getTikaConfig());

        defaultParser = new AutoDetectParser(config.getTikaConfig());
    }

    protected TikaProcessingResult processStream(TikaInputStream stream) {
        TikaProcessingResult result;

        try {
            BodyContentHandler handler = new BodyContentHandler();
            Metadata metadata = new Metadata();

            // firstly try the default parser
            if (stream.markSupported()) {
                stream.mark(Integer.MAX_VALUE);
            }

            defaultParser.parse(stream, handler, metadata, defaultParseContext);

            final Set<String> metaKeys = new HashSet<>(Arrays.asList(metadata.names()));

            Map<String, Object> resultMetadata = new HashMap<>();
            extractOCRMetadata(resultMetadata, metaKeys, metadata);
            extractContentTypeMetadata(resultMetadata, metaKeys, metadata);
            extractPageCountMetadata(resultMetadata, metaKeys, metadata);

            result = TikaProcessingResult.builder()
                    .text(handler.toString())
                    .metadata(resultMetadata)
                    .success(true)
                    .build();
        }
        catch (Exception e) {
            log.error(e.getMessage());

            result = TikaProcessingResult.builder()
                    .error("Exception caught while processing the document: " + e.getMessage())
                    .success(false)
                    .build();
        }

        return result;
    }


    // Helper methods
    //
    private void extractOCRMetadata(Map<String, Object> result, Set<String> metaKeys, Metadata metadata) {
        if (metaKeys.contains("X-PDFPREPROC-OCR-APPLIED")) {
            result.put("X-PDFPREPROC-OCR-APPLIED", metadata.get("X-PDFPREPROC-OCR-APPLIED"));
        }
        if (metaKeys.contains("X-PDFPREPROC-ORIGINAL")) {
            result.put("X-PDFPREPROC-ORIGINAL", metadata.get("X-PDFPREPROC-ORIGINAL"));
        }
    }

    private void extractContentTypeMetadata(Map<String, Object> result, Set<String> metaKeys, Metadata metadata) {
        if (metaKeys.contains("Content-Type")) {
            result.put("X-TL-CONTENT-TYPE", metadata.get("Content-Type"));
        }
        else {
            result.put("X-TL-CONTENT-TYPE", "TL_CONTENT_TYPE_UNKNOWN");
        }
    }

    private void extractPageCountMetadata(Map<String, Object> result, Set<String> metaKeys, Metadata metadata) {
        if (metaKeys.contains("xmpTPg:NPages")) {
            result.put("X-TL-PAGE-COUNT", metadata.get("xmpTPg:NPages"));
        }
        else if (metaKeys.contains("Page-Count")) {
            result.put("X-TL-PAGE-COUNT", metadata.get("Page-Count"));
        }
        else if (metaKeys.contains("meta:page-count")) {
            result.put("X-TL-PAGE-COUNT", metadata.get("meta:page-count"));
        } else {
            result.put("X-TL-PAGE-COUNT", "TL_PAGE_COUNT_UNKNOWN");
        }
    }
}
