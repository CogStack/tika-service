package tika.legacy;

import lombok.Data;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.exception.TikaException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.xml.sax.SAXException;
import javax.annotation.PostConstruct;
import java.io.IOException;


@Data
@Configuration
@PropertySource("classpath:tika-config/tika-processor-config.properties")
public class LegacyPdfProcessorConfig {

    private TikaConfig tikaConfig;

    // the timeout value (s) when performing PDF->TIFF conversion of the documents
    // the default value in Tika is 120s, but this may be too short for multi-page documents
    @Value("${tika.parsers.legacy-pdf-parser.image-magick.timeout:300}")
    private int conversionTimeout;

    // the timeout value (s) when performing OCR over the documents
    // the default value in Tika is 120s, but this may be too short for multi-page documents
    @Value("${tika.parsers.legacy-pdf-parser.tesseract-ocr.timeout:300}")
    private int ocrTimeout;

    // apply OCR when trying to extract text ...
    @Value("${tika.parsers.legacy-pdf-parser.min-doc-text-length:100}")
    private int pdfMinDocTextLength;

    @PostConstruct
    void init() throws IOException, SAXException, TikaException  {
        tikaConfig = new TikaConfig(this.getClass().getClassLoader()
                .getResourceAsStream("tika-config/legacy-parser-config.xml"));
    }
}