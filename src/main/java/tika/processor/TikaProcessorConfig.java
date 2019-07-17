package tika.processor;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;


@Data
@Configuration
@PropertySource("classpath:tika-config/std-processor-config.properties")
public class TikaProcessorConfig {

    // the timeout value (s) when performing OCR over documents
    @Value("${tika.parsers.tesseract-ocr.timeout:120}")
    private int ocrTimeout;

    // apply de-rotation of documents before processing
    // can be quite computationally expensive (runs as an external python script)
    @Value("${tika.parsers.tesseract-ocr.apply-rotation:false}")
    private boolean ocrApplyRotation;

    // apply image processing techniques during documents conversion (using ImageMagick)
    @Value("${tika.parsers.tesseract-ocr.enable-image-processing:false}")
    private boolean ocrEnableImageProcessing;

    // the language used in the OCR for corrections
    @Value("${tika.parsers.tesseract-ocr.language:eng}")
    private String ocrLanguage;

    // wherher to apply OCR only on the documents or also extract the embeded text (if present)
    // warn: note that applying 'OCR_AND_TEXT_EXTRACTION' the content can be duplicated
    @Value("${tika.parsers.pdf-ocr-parser.ocr-only-strategy:true}")
    private boolean pdfOcrOnlyStrategy;

    // apply OCR only when trying to extract text from previously parsed document (w/o OCR)
    // that extracted characters were less than N
    @Value("${tika.parsers.pdf-ocr-parser.min-doc-text-length:100}")
    private int pdfMinDocTextLength;

    // apply OCR only when trying to extract text from previously parsed document (w/o OCR)
    // that the read bytes were at least N
    @Value("${tika.parsers.pdf-ocr-parser.min-doc-byte-size:10000}")
    private int pdfMinDocByteSize;
}
