package tika.processor;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tika.batch.*;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.html.HtmlParser;
import org.apache.tika.parser.ocr.TesseractOCRConfig;
import org.apache.tika.parser.pdf.PDFParser;
import org.apache.tika.parser.pdf.PDFParserConfig;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import service.controller.TikaServiceController;
import tika.legacy.ImageMagickConfig;
import tika.legacy.LegacyPdfProcessorConfig;
import tika.legacy.LegacyPdfProcessorParser;
import tika.model.MetadataKeys;
import tika.model.TikaFileResource;
import tika.model.TikaFileResourceConsumer;
import tika.model.TikaProcessingResult;
import tika.utils.TikaUtils;

import javax.annotation.PostConstruct;
import java.io.*;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;

import static tika.model.MetadataKeys.IMAGE_PROCESSING_ENABLED;

/**
 * A default, composite Tika processor.
 *
 * In contrast to "legacy" processor it uses the default approach implemented in Tika, i.e. when
 * parsing PDF documents, it runs the processing independently per each PDF page,
 * and hence running Tesseract Page-Count times.
 */
@Component("compositeTikaProcessor")
public class CompositeTikaProcessor extends AbstractTikaProcessor {

    @Autowired
    private CompositeTikaProcessorConfig compositeTikaProcessorConfig;

    @Autowired
    private LegacyPdfProcessorConfig legacyPdfProcessorConfig;

    /**
     In order to properly handle PDF documents and OCR we need three separate parsers:
     - a generic parser (for any, non-PDF document type),
     - one that will extract text only from PDFs,
     - one that will apply OCR on PDFs (when stored only images).

     In the default configuration of PDFParser the OCR is disabled when extracting text from PDFs. However, OCR is
     enabled when extracting text from documents of image type. When using default parser with OCR enabled (strategy:
     extract both text and OCR), it will actually always apply OCR on the PDFs even when there is text-only provided.

     We would also like to know when OCR was applied as it will affect the accuracy of the extracted text that will be
     passed to the downstream analysis applications.
     */

    // common tika and parsers configuration
    private TikaConfig tikaConfig;
    private TesseractOCRConfig tessConfig;

    // the default, generic parser for handling all document types (except PDF)
    private AutoDetectParser defaultParser;
    private ParseContext defaultParseContext;

    // the default parser for PDFs (no OCR)
    private PDFParser pdfTextParser;
    private ParseContext pdfTextParseContext;

    // the parser to extract text from PDFs using OCR
    private PDFParser pdfOcrParser;
    private ParseContext pdfOcrParseContext;

    // the parser to extract text from PDFs using OCR only for single-pages
    // (used to strip-off clutter from LibreOffice-generated PDFs just with images)
    private LegacyPdfProcessorParser pdfSinglePageOcrParser;
    private ParseContext pdfSinglePageOcrParseContext;

    private TikaFileResourceCrawler tikaFileResourceCrawler;
    private TikaConsumerManager tikaConsumersManager;
    private StatusReporter statusReporter;

    private final Logger logger = LogManager.getLogger(TikaServiceController.class);

    @PostConstruct
    @Override
    public void init() throws Exception {

        tikaConfig = new TikaConfig();

        initializeTesseractConfig();

        initializeDefaultParser();

        initializePdfTextOnlyParser();

        initializePdfOcrParser();

        if (compositeTikaProcessorConfig.isUseLegacyOcrParserForSinglePageDocuments()) {
            initializePdfLegacyOcrParser();
        }
    }

    @Override
    public void reset() throws Exception {
        // actually, we only need to re-initialize all the resources apart from the configuration
        init();
    }

    protected TikaProcessingResult processStream(TikaInputStream stream) {

        var currentTimeNanos =  System.nanoTime();
        TikaProcessingResult result;

        try {
            ByteArrayInputStream newStream = null;

            if (stream.markSupported())
                stream.mark(Integer.MAX_VALUE);

            if(stream.hasInputStreamFactory()) {
                newStream = new ByteArrayInputStream(stream.getInputStreamFactory().getInputStream().readAllBytes());
            }
            else {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                stream.transferTo(outputStream);
                newStream = new ByteArrayInputStream(outputStream.toByteArray());
            }

            final int MIN_TEXT_BUFFER_SIZE = 1;
            ByteArrayOutputStream outStream = new ByteArrayOutputStream(MIN_TEXT_BUFFER_SIZE);
            BodyContentHandler handler = new BodyContentHandler(outStream);
            Metadata metadata = new Metadata();
            metadata.add(IMAGE_PROCESSING_ENABLED, String.valueOf(tessConfig.isEnableImagePreprocessing()));

            // mark the stream for multi-pass processing
            if (newStream.markSupported()) {
                newStream.mark(Integer.MAX_VALUE);
            }

            // try to detect whether the document is PDF
            if (isDocumentOfPdfType(newStream)) {
                // firstly try the default parser
                pdfTextParser.parse(newStream, handler, metadata, pdfTextParseContext);

                // check if there have been enough characters read / extracted and that we read enough bytes from the stream
                // (images embedded in the documents will occupy quite more space than just raw text)
                if (outStream.size() >= compositeTikaProcessorConfig.getPdfMinDocTextLength() && !Objects.equals(compositeTikaProcessorConfig.getPdfOcrStrategy(), "NO_OCR")) {
                    // since we are performing a second pass over the document, we need to reset cursor position
                    // in both input and output streams

                    newStream.reset();
                    outStream.reset();

                    final boolean useOcrLegacyParser = compositeTikaProcessorConfig.isUseLegacyOcrParserForSinglePageDocuments()
                            && TikaUtils.getPageCount(metadata) == 1;

                    // TODO: Q: shall we use a clean metadata or re-use some of the previously parsed fields???
                    handler = new BodyContentHandler(outStream);
                    metadata = new Metadata();
                    metadata.add(IMAGE_PROCESSING_ENABLED, String.valueOf(tessConfig.isEnableImagePreprocessing()));

                    if (useOcrLegacyParser) {
                        pdfSinglePageOcrParser.parse(newStream, handler, metadata, pdfSinglePageOcrParseContext);
                        // since we use the parser manually, update the metadata with the name of the parser class used
                        metadata.add(MetadataKeys.X_TIKA_PARSED_BY, LegacyPdfProcessorParser.class.getName());
                    }
                    else {
                        pdfOcrParser.parse(newStream, handler, metadata, pdfOcrParseContext);
                        // since we use the parser manually, update the metadata with the name of the parser class used
                        metadata.add(MetadataKeys.X_TIKA_PARSED_BY, PDFParser.class.getName());
                    }
                }
                else {
                    // since we use the parser manually, update the metadata with the name of the parser class used
                    metadata.add(MetadataKeys.X_TIKA_PARSED_BY, PDFParser.class.getName());
                }
            }
            else if (isDocumentOfHTMLType(newStream)) {
                newStream.reset();
                HtmlParser htmlParser = new HtmlParser();
                defaultParseContext.set(HtmlParser.class, htmlParser);
                htmlParser.parse(newStream, handler, metadata, defaultParseContext);
                metadata.add(MetadataKeys.X_TIKA_PARSED_BY, HtmlParser.class.getName());
            }
            else {
                // otherwise, run default documents parser
                newStream.reset();
                defaultParser.parse(newStream, handler, metadata, defaultParseContext);
                metadata.add(MetadataKeys.X_TIKA_PARSED_BY, AutoDetectParser.class.getName());
            }

            // parse the metadata and store the result
            Map<String, Object> resultMeta = TikaUtils.extractMetadata(metadata);

            String outputText = "";

            if (compositeTikaProcessorConfig.isEnforceEncodingOutput()) {
                if (Objects.equals(compositeTikaProcessorConfig.getOutputEncoding(), "")) {
                    compositeTikaProcessorConfig.setOutputEncoding("UTF-8");
                }
                String detectedEncoding = TikaUtils.detectEncoding(new ByteArrayInputStream(outStream.toByteArray()));
                try {
                    outputText = new String(outStream.toString().getBytes(detectedEncoding), compositeTikaProcessorConfig.getOutputEncoding());
                }
                catch(Exception e) {
                    outputText = outStream.toString();
                    logger.error("Failed to convert text to encoding:" + compositeTikaProcessorConfig.getOutputEncoding());
                    logger.error("Outputting Text in detected encoding:" + detectedEncoding );
                }
            }
            else {
                outputText = outStream.toString();
            }

            result = TikaProcessingResult.builder()
                    .text(outputText)
                    .metadata(resultMeta)
                    .success(true)
                    .timestamp(OffsetDateTime.now())
                    .processingElapsedTime((long) ((System.nanoTime() - currentTimeNanos) * 1e-9))
                    .build();

            outStream.close();
        }
        catch (Exception e) {
            logger.error(e.getMessage());
            e.printStackTrace();
            result = TikaProcessingResult.builder()
                    .error("Exception caught while processing the document: " + e.getMessage())
                    .success(false)
                    .build();
        }

        return result;
    }

    protected List<TikaProcessingResult> processBatch(MultipartFile[] multipartFiles) {

        List <TikaProcessingResult> tikaProcessingResultList = new ArrayList<>();

        int timeoutMilisToSec = compositeTikaProcessorConfig.getOcrTimeout() * 60;

        try {
            List<TikaFileResource> tikaFileResourceList = new ArrayList<>();

            logger.info("Converting multi-part files to resources files....");

            for(MultipartFile file: multipartFiles) {
                TikaFileResource tikaFileResource = new TikaFileResource(file.getOriginalFilename(), new Metadata(), TikaInputStream.get(file.getBytes()));
                tikaFileResourceList.add(tikaFileResource);
            }
            logger.info("Conversion finished....");

            int numberOfBatches = compositeTikaProcessorConfig.getBatchNumConsumers() > tikaFileResourceList.size() ? 1 : compositeTikaProcessorConfig.getBatchNumConsumers();
            var queueBatches = TikaUtils.getBatchesFromList(tikaFileResourceList, numberOfBatches);

            ArrayBlockingQueue<FileResource> fileResourceArrayBlockingQueue = new ArrayBlockingQueue<>(tikaFileResourceList.size(), true, tikaFileResourceList);
            List<FileResourceConsumer> fileResourceConsumerList = new ArrayList<>();

            for(List<TikaFileResource> fileResourceList: queueBatches) {
                var tmpQueue = new ArrayBlockingQueue<FileResource>(fileResourceList.size(), true, fileResourceList);
                fileResourceConsumerList.add(new TikaResourceConsumer(tmpQueue));
            }

            tikaFileResourceCrawler = new TikaFileResourceCrawler(fileResourceArrayBlockingQueue, fileResourceConsumerList.size());
            tikaFileResourceCrawler.setMaxConsecWaitInMillis(1000);

            tikaConsumersManager = new TikaConsumerManager(fileResourceConsumerList);
            // does not let consumers hang over the specified maximum time
            tikaConsumersManager.setConsumersManagerMaxMillis(timeoutMilisToSec);

            statusReporter = new StatusReporter(tikaFileResourceCrawler, tikaConsumersManager);
            statusReporter.setSleepMillis(1000);
            statusReporter.setStaleThresholdMillis(1000);

            BatchProcess batchProcess = new BatchProcess(tikaFileResourceCrawler, tikaConsumersManager, statusReporter, null);
            batchProcess.setTimeoutCheckPulseMillis(100);
            batchProcess.setPauseOnEarlyTerminationMillis(100);
            batchProcess.setTimeoutThresholdMillis(timeoutMilisToSec);
            batchProcess.setMaxAliveTimeSeconds(compositeTikaProcessorConfig.getOcrTimeout());

            var parallelFileProcessingResult = batchProcess.call();

            for (TikaFileResource tikaFileResource : tikaFileResourceList) {
                tikaProcessingResultList.add(tikaFileResource.getTikaProcessingResult());
                tikaFileResource.close();
            }

            logger.info("Consumed:" + parallelFileProcessingResult.getConsumed());
            logger.info("Batch processing terminated with message: " + parallelFileProcessingResult.getCauseForTermination());
            logger.info("Successfully finished processing.");
        }
        catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage());
        }

        return tikaProcessingResultList;
    }

    private class TikaResourceConsumer extends TikaFileResourceConsumer {

        private int fileQueueSize = 0;

        public TikaResourceConsumer(ArrayBlockingQueue<FileResource> fileQueue) {
            super(fileQueue);
            fileQueueSize = fileQueue.size();
        }

        @Override
        public boolean processFileResource(FileResource fileResource) {
            int inactiveConsumers = 0;
            try {
                TikaProcessingResult result;
                result = processStream(TikaInputStream.get(fileResource.openInputStream()));
                logger.info("Processing file: " + fileResource.getResourceId());
                if(result.getSuccess()) {
                    ((TikaFileResource) fileResource).setTikaProcessingResult(result);
                }
                else {
                    logger.warn("OCR-ing failed" + result.getError());
                }

                if(this.getNumResourcesConsumed() + 1 >= fileQueueSize) {
                    this.pleaseShutdown();
                    this.flushAndClose((TikaFileResource) fileResource);
                    inactiveConsumers = inactiveConsumers + 1;
                }

                var consumers = tikaConsumersManager.getConsumers();

                for(FileResourceConsumer consumer : consumers) {
                    if(!consumer.isStillActive()) {
                        inactiveConsumers = inactiveConsumers + 1;
                        consumer.pleaseShutdown();
                    }
                }

                if(inactiveConsumers > consumers.size()) {
                    tikaFileResourceCrawler.shutDownNoPoison();
                    tikaConsumersManager.shutdown();
                    statusReporter.setIsShuttingDown(true);
                }

                return true;
            }
            catch (Exception e) {
                e.printStackTrace();
                logger.error("OCR-ing failed" + e.getMessage());
            }
            return false;
        }
    }

    private boolean isDocumentOfPdfType(InputStream stream) throws Exception {
        Metadata metadata = new Metadata();
        MediaType mediaType = defaultParser.getDetector().detect(stream, metadata);
        return mediaType.equals(MediaType.application("pdf"));
    }

    private boolean isDocumentOfImageType(InputStream stream) throws Exception {
        Metadata metadata = new Metadata();
        MediaType mediaType = defaultParser.getDetector().detect(stream, metadata);
        return mediaType.getType().contains("image");
    }

    private boolean isDocumentOfHTMLType(InputStream stream) throws Exception {
        Metadata metadata = new Metadata();
        MediaType mediaType = defaultParser.getDetector().detect(stream, metadata);

        boolean isHTML = mediaType.getSubtype().contains("html");

        // hack to deal with docs that have no type assigned
        if (!isHTML)
        {
            String detectedEncoding = TikaUtils.detectEncoding(stream);
            byte[] streamBytes = IOUtils.toByteArray(stream);

            String result = new String(streamBytes, detectedEncoding);
            if (result.contains("<html>") && result.contains("</html>")) {
                isHTML = true;
            }
        }

        return isHTML;
    }

    private void initializeTesseractConfig() {
        tessConfig = new TesseractOCRConfig();
        tessConfig.setTimeoutSeconds(compositeTikaProcessorConfig.getOcrTimeout());
        tessConfig.setApplyRotation(compositeTikaProcessorConfig.isOcrApplyRotation());
        tessConfig.setResize(compositeTikaProcessorConfig.getOcrResize());
        tessConfig.setFilter(compositeTikaProcessorConfig.getOcrFilter());
        tessConfig.setDensity(compositeTikaProcessorConfig.getOcrDensity());
        tessConfig.setDepth(compositeTikaProcessorConfig.getOcrDepth());
        tessConfig.setSkipOcr(false);
        tessConfig.setEnableImagePreprocessing(compositeTikaProcessorConfig.isOcrEnableImageProcessing());
        tessConfig.setLanguage(compositeTikaProcessorConfig.getOcrLanguage());
    }

    private void initializeDefaultParser() {
        defaultParser = new AutoDetectParser(tikaConfig);

        defaultParseContext = new ParseContext();
        defaultParseContext.set(TikaConfig.class, tikaConfig);
        defaultParseContext.set(TesseractOCRConfig.class, tessConfig);
        defaultParseContext.set(AutoDetectParser.class, defaultParser);
        defaultParseContext.set(Parser.class, defaultParser); //need to add this to make sure recursive parsing happens!
    }

    private void initializePdfTextOnlyParser() {
        PDFParserConfig pdfTextOnlyConfig = new PDFParserConfig();
        pdfTextOnlyConfig.setExtractInlineImages(false);
        pdfTextOnlyConfig.setExtractUniqueInlineImagesOnly(false); // do not extract multiple inline images
        pdfTextOnlyConfig.setImageStrategy(PDFParserConfig.IMAGE_STRATEGY.NONE);
        pdfTextOnlyConfig.setOcrRenderingStrategy(PDFParserConfig.OCR_RENDERING_STRATEGY.TEXT_ONLY);
        pdfTextOnlyConfig.setOcrStrategy(PDFParserConfig.OCR_STRATEGY.NO_OCR);

        pdfTextParser = new PDFParser();
        pdfTextParseContext = new ParseContext();
        pdfTextParseContext.set(TikaConfig.class, tikaConfig);
        pdfTextParseContext.set(PDFParserConfig.class, pdfTextOnlyConfig);
        // pdfTextParseContext.set(Parser.class, defaultParser); //need to add this to make sure recursive parsing happens!
    }

    private void initializePdfOcrParser() {
        PDFParserConfig pdfOcrConfig = new PDFParserConfig();
        pdfOcrConfig.setExtractUniqueInlineImagesOnly(false); // do not extract multiple inline images
        pdfOcrConfig.setOcrDPI(compositeTikaProcessorConfig.getPdfOcrDPI());
        pdfOcrConfig.setDetectAngles(compositeTikaProcessorConfig.isPdfOcrDetectAngles());

        if (compositeTikaProcessorConfig.isPdfOcrOnlyStrategy()) {
            pdfOcrConfig.setExtractInlineImages(false);
            pdfOcrConfig.setOcrStrategy(PDFParserConfig.OCR_STRATEGY.OCR_ONLY);
        }
        else {
            pdfOcrConfig.setExtractUniqueInlineImagesOnly(true); // do not extract multiple inline images
            // warn: note that applying 'OCR_AND_TEXT_EXTRACTION' the content can be duplicated
            pdfOcrConfig.setOcrStrategy(PDFParserConfig.OCR_STRATEGY.OCR_AND_TEXT_EXTRACTION);
        }

        pdfOcrConfig.setDropThreshold(compositeTikaProcessorConfig.getPdfOcrDropThreshold());
        pdfOcrConfig.setOcrImageQuality(compositeTikaProcessorConfig.getPdfOcrImageQuality());

        PDFParserConfig.OCR_RENDERING_STRATEGY ocrRenderStrategy = PDFParserConfig.OCR_RENDERING_STRATEGY.ALL;
        PDFParserConfig.IMAGE_STRATEGY ocrImageStrategy = PDFParserConfig.IMAGE_STRATEGY.RAW_IMAGES;
        PDFParserConfig.OCR_STRATEGY ocrStrategy = PDFParserConfig.OCR_STRATEGY.AUTO;

        switch (compositeTikaProcessorConfig.getPdfOcrRenderingStrategy()) {
            case "ALL" -> ocrRenderStrategy = PDFParserConfig.OCR_RENDERING_STRATEGY.ALL;
            case "NO_TEXT" -> ocrRenderStrategy = PDFParserConfig.OCR_RENDERING_STRATEGY.NO_TEXT;
            case "TEXT_ONLY" -> ocrRenderStrategy = PDFParserConfig.OCR_RENDERING_STRATEGY.TEXT_ONLY;
            case "VECTOR_GRAPHICS_ONLY" -> ocrRenderStrategy = PDFParserConfig.OCR_RENDERING_STRATEGY.VECTOR_GRAPHICS_ONLY;
        }

        switch (compositeTikaProcessorConfig.getPdfOcrImageStrategy()) {
            case "NONE" -> ocrImageStrategy = PDFParserConfig.IMAGE_STRATEGY.NONE;
            case "RAW_IMAGES" -> ocrImageStrategy = PDFParserConfig.IMAGE_STRATEGY.RAW_IMAGES;
            case "RENDER_PAGES_BEFORE_PARSE" -> ocrImageStrategy = PDFParserConfig.IMAGE_STRATEGY.RENDER_PAGES_BEFORE_PARSE;
            case "RENDER_PAGES_AT_PAGE_END" -> ocrImageStrategy = PDFParserConfig.IMAGE_STRATEGY.RENDER_PAGES_AT_PAGE_END;
        }

        switch (compositeTikaProcessorConfig.getPdfOcrStrategy()) {
            case "AUTO" : { ocrStrategy = PDFParserConfig.OCR_STRATEGY.AUTO; }
            case "NO_OCR" : { ocrStrategy = PDFParserConfig.OCR_STRATEGY.NO_OCR; }
            case "OCR_AND_TEXT_EXTRACTION": { ocrStrategy = PDFParserConfig.OCR_STRATEGY.OCR_AND_TEXT_EXTRACTION; pdfOcrConfig.setExtractUniqueInlineImagesOnly(true);}
            case "OCR_ONLY" : { ocrStrategy = PDFParserConfig.OCR_STRATEGY.OCR_ONLY; pdfOcrConfig.setExtractInlineImages(false); }
        }

        pdfOcrConfig.setOcrRenderingStrategy(ocrRenderStrategy);
        pdfOcrConfig.setImageStrategy(ocrImageStrategy);
        pdfOcrConfig.setOcrStrategy(ocrStrategy);

        pdfOcrParser = new PDFParser();
        pdfOcrParseContext = new ParseContext();
        pdfOcrParseContext.set(TikaConfig.class, tikaConfig);
        pdfOcrParseContext.set(PDFParserConfig.class, pdfOcrConfig);
        pdfOcrParseContext.set(TesseractOCRConfig.class, tessConfig);
        //pdfOcrParseContext.set(Parser.class, defaultParser); //need to add this to make sure recursive parsing happens!
    }

    private void initializePdfLegacyOcrParser() {
        pdfSinglePageOcrParser = new LegacyPdfProcessorParser();

        pdfSinglePageOcrParseContext = new ParseContext();
        pdfSinglePageOcrParseContext.set(TikaConfig.class, tikaConfig);
        pdfSinglePageOcrParseContext.set(LegacyPdfProcessorConfig.class, legacyPdfProcessorConfig);
        pdfSinglePageOcrParseContext.set(TesseractOCRConfig.class, tessConfig);

        ImageMagickConfig imgConfig = new ImageMagickConfig();
        imgConfig.setTimeout(legacyPdfProcessorConfig.getConversionTimeout());

        pdfSinglePageOcrParseContext.set(ImageMagickConfig.class, imgConfig);
        pdfSinglePageOcrParseContext.set(Parser.class, defaultParser); //need to add this to make sure recursive parsing happens!
    }
}
