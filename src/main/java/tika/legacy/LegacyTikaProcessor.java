package tika.legacy;

import org.apache.tika.config.TikaConfig;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.ocr.TesseractOCRConfig;
import org.apache.tika.sax.BodyContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import tika.model.TikaProcessingResult;
import tika.processor.AbstractTikaProcessor;
import tika.utils.TikaUtils;

import javax.annotation.PostConstruct;
import java.io.ByteArrayOutputStream;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static tika.model.MetadataKeys.IMAGE_PROCESSING_ENABLED;


/**
 * The "legacy" Tika processor, using parser from CogStack-Pipeline
 * to provide compatibility with the migration of the pipeline.
 *
 * Processes PDF documents by running manually:
 * - 1x ImageMagick - to create one large temporary TIFF image
 * - 1x Tesseract - to extract the text from the TIFF
 */
@Component("legacyTikaProcessor")
public class LegacyTikaProcessor extends AbstractTikaProcessor {

    @Autowired
    private LegacyPdfProcessorConfig config;

    /**
     * Document-type based automatic detection of the parser to be used by Tika
     */
    private AutoDetectParser defaultParser;
    private ParseContext defaultParseContext;

    private Logger log = LoggerFactory.getLogger(LegacyTikaProcessor.class);

    /**
     * Initializes the processor using provided (autowired) configuration
     */
    @PostConstruct
    @Override
    public void init() throws Exception {
        defaultParseContext = new ParseContext();
        defaultParseContext.set(TikaConfig.class, config.getTikaConfig());
        defaultParseContext.set(LegacyPdfProcessorConfig.class, config);

        TesseractOCRConfig tessConfig = new TesseractOCRConfig();
//        tessConfig.setTimeout(config.getOcrTimeout());
        defaultParseContext.set(TesseractOCRConfig.class, tessConfig);

        ImageMagickConfig imgConfig = new ImageMagickConfig();
        imgConfig.setTimeout(config.getConversionTimeout());
        defaultParseContext.set(ImageMagickConfig.class, imgConfig);

        defaultParser = new AutoDetectParser(config.getTikaConfig());
    }

    /**
     * Resets the component with any intermediate data used
     */
    @Override
    public void reset() throws Exception {
        // actually, we only need to re-initialize all the resources apart from the configuration
        init();
    }

    /**
     * Processes the input stream returning the extracted text
     */
    protected TikaProcessingResult processStream(TikaInputStream stream) {
        TikaProcessingResult result;

        try {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream(64 * 1024);
            BodyContentHandler handler = new BodyContentHandler(outStream);
            Metadata metadata = new Metadata();
            metadata.add(IMAGE_PROCESSING_ENABLED, "true");

            defaultParser.parse(stream, handler, metadata, defaultParseContext);

            // parse the metadata and store the result
            Map<String, Object> resultMetadata = TikaUtils.extractMetadata(metadata);

            result = TikaProcessingResult.builder()
                    .text(outStream.toString())
                    .metadata(resultMetadata)
                    .success(true)
                    .timestamp(OffsetDateTime.now())
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

    @Override
    protected List<TikaProcessingResult> processBatch(MultipartFile[] multipartFiles) {
        List <TikaProcessingResult> tikaProcessingResultList = new ArrayList<>();
        return tikaProcessingResultList;
    }
}
