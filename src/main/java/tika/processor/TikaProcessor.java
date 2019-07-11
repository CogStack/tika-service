package tika.processor;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.tika.config.TikaConfig;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.ocr.TesseractOCRConfig;
import org.apache.tika.parser.pdf.PDFParserConfig;
import org.apache.tika.sax.BodyContentHandler;
import org.apache.tika.utils.ParserUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tika.model.TikaBinaryDocument;
import tika.model.TikaProcessingResult;


public class TikaProcessor {

    private AutoDetectParser defaultParser = new AutoDetectParser();
    private ParseContext defaultParseContext = new ParseContext();

    private AutoDetectParser ocrParser = new AutoDetectParser();
    private ParseContext ocrParseContext = new ParseContext();

    private Logger log = LoggerFactory.getLogger(TikaProcessor.class);


    public TikaProcessor() {

        // set up default parse context -- no ocr
        TesseractOCRConfig tessConfig = new TesseractOCRConfig();
        PDFParserConfig pdfConfig = new PDFParserConfig();
        pdfConfig.setExtractInlineImages(false);
        pdfConfig.setExtractUniqueInlineImagesOnly(false); // do not extract multiple inline images

        defaultParseContext.set(TesseractOCRConfig.class, tessConfig);
        defaultParseContext.set(PDFParserConfig.class, pdfConfig);
        defaultParseContext.set(Parser.class, defaultParser);


        // set up default parse context -- use ocr
        tessConfig = new TesseractOCRConfig();
        tessConfig.setApplyRotation(true);
        tessConfig.setEnableImageProcessing(1);

        pdfConfig = new PDFParserConfig();
        pdfConfig.setExtractInlineImages(true);
        pdfConfig.setExtractUniqueInlineImagesOnly(false); // do not extract multiple inline images
        pdfConfig.setOcrStrategy(PDFParserConfig.OCR_STRATEGY.OCR_ONLY);

        ocrParseContext.set(TesseractOCRConfig.class, tessConfig);
        ocrParseContext.set(PDFParserConfig.class, pdfConfig);
        ocrParseContext.set(Parser.class, ocrParser);
    }


    public TikaProcessingResult process(final TikaBinaryDocument binaryDoc) {
        return processTikaStream(TikaInputStream.get(binaryDoc.getContent()));
    }

    public TikaProcessingResult process(InputStream binaryStream) {
        return processTikaStream(TikaInputStream.get(binaryStream));
    }

    private TikaProcessingResult processTikaStream(TikaInputStream stream) {
        TikaProcessingResult result;

        try {
            BodyContentHandler handler = new BodyContentHandler();
            Metadata metadata = new Metadata();

            // firstly try the default parser
            if (stream.markSupported()) {
                stream.mark(Integer.MAX_VALUE);
            }

            defaultParser.parse(stream, handler, metadata, defaultParseContext);

            // check whether we have content to parse and try ocr parser
            if (metadata.get("Content-Type").equals("application/pdf")) {
                if (handler.toString().length() < 100 && stream.getPosition() > 10000) {

                    handler = new BodyContentHandler();
                    metadata = new Metadata();

                    stream.reset();
                    ocrParser.parse(stream, handler, metadata, ocrParseContext);
                }
            }

            /*
            // set up default parse context -- use ocr
            TesseractOCRConfig tessConfig = new TesseractOCRConfig();
            tessConfig.setApplyRotation(true);
            tessConfig.setEnableImageProcessing(1);

            PDFParserConfig pdfConfig = new PDFParserConfig();
            pdfConfig.setExtractInlineImages(true);
            pdfConfig.setExtractUniqueInlineImagesOnly(false); // do not extract multiple inline images
            pdfConfig.setOcrStrategy(PDFParserConfig.OCR_STRATEGY.OCR_AND_TEXT_EXTRACTION);

            AutoDetectParser parser = new AutoDetectParser();
            ParseContext context = new ParseContext();

            context.set(TesseractOCRConfig.class, tessConfig);
            context.set(PDFParserConfig.class, pdfConfig);
            context.set(Parser.class, parser);

            parser.parse(stream, handler, metadata, context);
            */

            final Metadata md = metadata;

            Map<String, String[]> tm = new HashMap<>();
            Arrays.stream(metadata.names()).forEach(name -> tm.put(name, md.getValues(name)));

            result = TikaProcessingResult.builder()
                    .documentContent(handler.toString())
                    .metadata(tm)
                    .success(true)
                    .build();
        }
        catch (Exception e) {
            log.error(e.getMessage());

            result = TikaProcessingResult.builder()
                    .errorMessage("Exception caught while processing the document: " + e.getMessage())
                    .success(false)
                    .build();
        }

        return result;
    }
}
