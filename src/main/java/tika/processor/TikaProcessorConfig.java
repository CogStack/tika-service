package tika.processor;

import lombok.Data;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.parser.ocr.TesseractOCRConfig;
import org.apache.tika.parser.pdf.PDFParserConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;


@Data
@Configuration
@PropertySource("classpath:tika-config.properties")
public class TikaProcessorConfig {

    // Apache Tika configuration and used parsers
    //
    private TikaConfig tikaConfig;

    private TesseractOCRConfig tesseractConfig;

    private PDFParserConfig pdfTextOnlyConfig;
    private PDFParserConfig pdfOcrConfig;


    // external configuration parameter values
    //
    @Value("${tika.parsers.tesseract-ocr.timeout:120}")
    private int ocrTimeout;

    @Value("${tika.parsers.tesseract-ocr.apply-rotation:false}")
    private boolean ocrApplyRotation;

    @Value("${tika.parsers.tesseract-ocr.enable-image-processing:false}")
    private boolean ocrEnableImageProcessing;

    @Value("${tika.parsers.tesseract-ocr.language:eng}")
    private String ocrLanguage;

    // strategy can be: "ocr-only"
    @Value("${tika.parsers.pdf-ocr-parser.ocr-only-strategy:true}")
    private int pdfOcrOnlyStrategy;

    @Value("${tika.parsers.pdf-ocr-parser.min-text-length:100}")
    private int pdfMinTextLength;


    public TikaProcessorConfig() throws Exception {
        tikaConfig = new TikaConfig();
        tesseractConfig = new TesseractOCRConfig();
    }

    public TikaProcessorConfig(TikaConfig tikaConfig, TesseractOCRConfig tesseractConfig) {
        this.tikaConfig = tikaConfig;
        this.tesseractConfig = tesseractConfig;
    }
}
