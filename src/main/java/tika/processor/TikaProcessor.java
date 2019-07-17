package tika.processor;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.*;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.ocr.TesseractOCRConfig;
import org.apache.tika.parser.pdf.PDFParser;
import org.apache.tika.parser.pdf.PDFParserConfig;
import org.apache.tika.sax.BodyContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tika.model.TikaProcessingResult;
import javax.annotation.PostConstruct;


@Component("standardTikaProcessor")
public class TikaProcessor extends AbstractTikaProcessor {

    @Autowired
    private TikaProcessorConfig tikaProcessorConfig;

    /*
     In order to properly handle PDF documents and OCR we need three separate parsers:
     - a generic parser
     - one that will extract text only from PDFs
     - one that will apply OCR on PDFs (when stored only images)

     In the default configuration of PDFParser the OCR is disabled when extracting text from PDFs. However, OCR is
     enabled when extracting text from documents of image type. When using default parser with OCR enabled (strategy:
     extract both text and OCR), it will actually always apply OCR on the PDFs even when there is text-only provided.

     We would also like to know when OCR was applied as it will affect the accuracy of the extracted text that will be
     passed to the downstream analysis applications.
     */

    // common tika and parsers configuration
    private TikaConfig tikaConfig;
    private TesseractOCRConfig tessConfig;

    // the default, generic parser for handling all document types (expect PDF)
    private AutoDetectParser defaultParser;
    private ParseContext defaultParseContext;

    // the default parser for PDFs (no OCR)
    private PDFParser pdfTextParser;
    private ParseContext pdfTextParseContext;

    // the parser to extract text from PDFs using OCR
    private PDFParser pdfOcrParser;
    private ParseContext pdfOcrParseContext;


    private Logger log = LoggerFactory.getLogger(TikaProcessor.class);


    @PostConstruct
    public void init() throws Exception {

        tikaConfig = new TikaConfig();

        initializeTesseractConfig();

        initializeDefaultParser();

        initializePdfTextOnlyParser();

        initializePdfOcrParser();
    }


    private boolean isDocumentOfPdfType(InputStream stream) throws Exception {
        Metadata metadata = new Metadata();
        MediaType mediaType = defaultParser.getDetector().detect(stream, metadata);

        return mediaType.equals(MediaType.application("pdf"));
    }


    protected TikaProcessingResult processStream(TikaInputStream stream) {
        final int MIN_TEXT_BUFFER_SIZE = 1024;

        TikaProcessingResult result;
        try {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream(MIN_TEXT_BUFFER_SIZE);
            BodyContentHandler handler = new BodyContentHandler(outStream);

            Metadata metadata = new Metadata();

            // firstly try the default parser
            if (stream.markSupported()) {
                stream.mark(Integer.MAX_VALUE);
            }

            // try to detect whether the document is PDF
            // TODO: general document type -- to update whether OCR was used (also applied on images)
            // TODO: Q: shall we use manual conversion of the images to OCR?
            if (isDocumentOfPdfType(stream)) {
                // run default parser
                pdfTextParser.parse(stream, handler, metadata, pdfTextParseContext);

                // check if
                if (outStream.size() < tikaProcessorConfig.getPdfMinDocTextLength()
                        && stream.getPosition() > tikaProcessorConfig.getPdfMinDocByteSize()) {

                    stream.reset();

                    outStream.reset();
                    handler = new BodyContentHandler(outStream);
                    metadata = new Metadata();

                    // shall we use a clean metadata or re-use some of the previously parsed fields???
                    pdfOcrParser.parse(stream, handler, metadata, pdfOcrParseContext);
                }

                // update the metadata with the name of the parser class used
                //
                metadata.add("X-Parsed-By", PDFParser.class.toString());
            }
            else {
                // run default documents parser
                defaultParser.parse(stream, handler, metadata, defaultParseContext);
            }

            // parse the metadata
            //
            Map<String, Object> resultMeta = extractMetadata(metadata);

            result = TikaProcessingResult.builder()
                    .text(outStream.toString())
                    .metadata(resultMeta)
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


    private void initializeTesseractConfig() {
        tessConfig = new TesseractOCRConfig();

        tessConfig.setTimeout(tikaProcessorConfig.getOcrTimeout());
        tessConfig.setApplyRotation(tikaProcessorConfig.isOcrApplyRotation());
        if (tikaProcessorConfig.isOcrEnableImageProcessing()) {
            tessConfig.setEnableImageProcessing(1);
        }
        else {
            tessConfig.setEnableImageProcessing(0);
        }
        tessConfig.setLanguage(tikaProcessorConfig.getOcrLanguage());
    }


    private void initializeDefaultParser() {
        defaultParser = new AutoDetectParser(tikaConfig);

        defaultParseContext = new ParseContext();
        defaultParseContext.set(TikaConfig.class, tikaConfig);
        defaultParseContext.set(TesseractOCRConfig.class, tessConfig);
        defaultParseContext.set(Parser.class, defaultParser); //need to add this to make sure recursive parsing happens!
    }


    private void initializePdfTextOnlyParser() {
        PDFParserConfig pdfTextOnlyConfig = new PDFParserConfig();
        pdfTextOnlyConfig.setExtractInlineImages(false);
        pdfTextOnlyConfig.setExtractUniqueInlineImagesOnly(false); // do not extract multiple inline images
        pdfTextOnlyConfig.setOcrStrategy(PDFParserConfig.OCR_STRATEGY.NO_OCR);

        pdfTextParser = new PDFParser();
        pdfTextParseContext = new ParseContext();
        pdfTextParseContext.set(TikaConfig.class, tikaConfig);
        pdfTextParseContext.set(PDFParserConfig.class, pdfTextOnlyConfig);
        //pdfTextParseContext.set(Parser.class, defaultParser); //need to add this to make sure recursive parsing happens!
    }


    private void initializePdfOcrParser() {
        PDFParserConfig pdfOcrConfig = new PDFParserConfig();
        pdfOcrConfig.setExtractUniqueInlineImagesOnly(false); // do not extract multiple inline images
        if (tikaProcessorConfig.isPdfOcrOnlyStrategy()) {
            pdfOcrConfig.setExtractInlineImages(false);
            pdfOcrConfig.setOcrStrategy(PDFParserConfig.OCR_STRATEGY.OCR_ONLY);
        }
        else {
            pdfOcrConfig.setExtractInlineImages(true);
            // warn: note that applying 'OCR_AND_TEXT_EXTRACTION' the content can be duplicated
            pdfOcrConfig.setOcrStrategy(PDFParserConfig.OCR_STRATEGY.OCR_AND_TEXT_EXTRACTION);
        }

        pdfOcrParser = new PDFParser();
        pdfOcrParseContext = new ParseContext();
        pdfOcrParseContext.set(TikaConfig.class, tikaConfig);
        pdfOcrParseContext.set(PDFParserConfig.class, pdfOcrConfig);
        pdfOcrParseContext.set(TesseractOCRConfig.class, tessConfig);
        //pdfOcrParseContext.set(Parser.class, defaultParser); //need to add this to make sure recursive parsing happens!
    }
}
