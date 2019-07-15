package tika.processor;

import java.io.InputStream;
import java.util.*;

import org.apache.tika.config.TikaConfig;
import org.apache.tika.detect.Detector;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.ocr.TesseractOCRConfig;
import org.apache.tika.parser.ocr.TesseractOCRParser;
import org.apache.tika.parser.pdf.PDFParser;
import org.apache.tika.parser.pdf.PDFParserConfig;
import org.apache.tika.sax.BodyContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tika.model.TikaBinaryDocument;
import tika.model.TikaProcessingResult;
import tika.processor.TikaProcessorConfig;


public class CompositeTikaProcessor extends AbstractTikaProcessor {

    private TikaProcessorConfig config;

    /*
     In order to properly handle PDF documents and OCR we need three separate parsers:
     - a generic parser
     - one that will extract text only from PDFs
     - one that will apply OCR on PDFs (when stored only images)

     In the default configuration of PDFParser the OCR is disabled when extracting text from PDFs. However, OCR is
     enabled when extracting text from documents of image type.

     We would also like to know when OCR was applied as it will affect the accuracy of the extracted text that will be
     passed to the downstream analysis applications.
     */

    // the default, generic parser for handling all document types (expect PDF)
    private AutoDetectParser defaultParser;
    private ParseContext defaultParseContext;

    // the default parser for PDFs (no OCR)
    private PDFParser pdfTextParser;
    private ParseContext pdfTextParseContext;

    // the parser to extract text from PDFs using OCR
    private PDFParser pdfOcrParser;
    private ParseContext pdfOcrParseContext;


    private Logger log = LoggerFactory.getLogger(tika.processor.CompositeTikaProcessor.class);


    public CompositeTikaProcessor() throws Exception {

        config = new TikaProcessorConfig();

        // general OCR config
        //
        TesseractOCRConfig tessConfig = new TesseractOCRConfig();
        tessConfig.setApplyRotation(true);
        tessConfig.setEnableImageProcessing(1);


        // initialize default parser
        //
        defaultParser = new AutoDetectParser(config.getTikaConfig());

        defaultParseContext = new ParseContext();
        defaultParseContext.set(TikaConfig.class, config.getTikaConfig());
        defaultParseContext.set(TesseractOCRConfig.class, tessConfig);
        //defaultParseContext.set(Parser.class, defaultParser);


        // initialize default pdf parser
        //
        PDFParserConfig pdfTextOnlyConfig = new PDFParserConfig();
        pdfTextOnlyConfig.setExtractInlineImages(false);
        pdfTextOnlyConfig.setExtractUniqueInlineImagesOnly(false); // do not extract multiple inline images
        pdfTextOnlyConfig.setOcrStrategy(PDFParserConfig.OCR_STRATEGY.NO_OCR);

        pdfTextParser = new PDFParser();
        pdfTextParseContext = new ParseContext();
        pdfTextParseContext.set(PDFParserConfig.class, pdfTextOnlyConfig);
        //pdfTextParseContext.set(Parser.class, pdfTextParser);


        // initialize ocr pdf parser
        //
        PDFParserConfig pdfOcrConfig = new PDFParserConfig();
        pdfOcrConfig.setExtractInlineImages(true);
        pdfOcrConfig.setExtractUniqueInlineImagesOnly(false); // do not extract multiple inline images
        pdfOcrConfig.setOcrStrategy(PDFParserConfig.OCR_STRATEGY.OCR_AND_TEXT_EXTRACTION);

        pdfOcrParser = new PDFParser();
        pdfOcrParseContext = new ParseContext();

        pdfOcrParseContext.set(PDFParserConfig.class, pdfOcrConfig);
        pdfOcrParseContext.set(TesseractOCRConfig.class, tessConfig);
        //pdfOcrParseContext.set(Parser.class, pdfOcrParser);
    }


    private boolean isDocumentPdfType(InputStream stream) throws Exception {
        //final String CONTENT_TYPE = "Content-Type";
        //final String PDF_TYPE = "application/pdf";
        Metadata metadata = new Metadata();
        MediaType mediaType = defaultParser.getDetector().detect(stream, metadata);

        return mediaType.equals(MediaType.application("pdf"));
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

            // try to detect whether the document is PDF
            // TODO: general document type -- to update whether OCR was used (also applied on images)
            // TODO: Q: shall we use manual conversion of the images to OCR?
            if (isDocumentPdfType(stream)) {
                // run default parser
                pdfTextParser.parse(stream, handler, metadata, pdfTextParseContext);

                // check if
                if (handler.toString().length() < 100 && stream.getPosition() > 10000) {

                    handler = new BodyContentHandler();
                    metadata = new Metadata();

                    stream.reset();
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


            // check whether OCR has been applied
            //
            if (metadata.get("X-Parsed-By") != null
                    && Arrays.asList(metadata.getValues("X-Parsed-By")).contains(TesseractOCRParser.class.toString())) {
                metadata.add("X-OCR-Applied", "true");
            }
            else {
                metadata.add("X-OCR-Applied", "false");
            }


            // parse the metadata
            //
            final String[] metaKeysSingleValue = {"Content-Type", "Creation-Date", "Last-Modified", "X-OCR-Applied"};
            final String[] metaKeysMultiValue = {"X-Parsed-By"};
            final Metadata md = metadata;

            Map<String, Object> resultMeta = new HashMap<>();

            Arrays.stream(metaKeysSingleValue).forEach(name -> {
                if (md.get(name) != null)
                    resultMeta.put(name, md.get(name));
            });

            Arrays.stream(metaKeysMultiValue).forEach(name -> {
                if (md.getValues(name) != null)
                    resultMeta.put(name, md.getValues(name));
            });

            result = TikaProcessingResult.builder()
                    .text(handler.toString())
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
}
