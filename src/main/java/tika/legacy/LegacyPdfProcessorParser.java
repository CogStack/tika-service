/*
 * Copyright 2016 King's College London, Richard Jackson <richgjackson@gmail.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package tika.legacy;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.LogFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.AbstractParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.external.ExternalParser;
import org.apache.tika.parser.ocr.TesseractOCRParser;
import org.apache.tika.parser.pdf.PDFParser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import tika.model.MetadataKeys;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;


public class LegacyPdfProcessorParser extends AbstractParser {

    private static final long serialVersionUID = -8167538283213097265L;
    private static Map<String, Boolean> IMAGEMAGICK_PRESENT = new HashMap<String, Boolean>();
    private static final ImageMagickConfig DEFAULT_IMAGEMAGICK_CONFIG = new ImageMagickConfig();

    private static final Set<MediaType> SUPPORTED_TYPES = Collections.unmodifiableSet(
            new HashSet<>(Collections.singletonList(MediaType.application("pdf"))));

    private static final Logger logger = LogManager.getLogger(LegacyPdfProcessorParser.class);

    @Override
    public Set<MediaType> getSupportedTypes(ParseContext context) {
        // If ImageMagick is installed, offer our supported image types
        ImageMagickConfig imageMagickConfig = context.get(ImageMagickConfig.class, DEFAULT_IMAGEMAGICK_CONFIG);
        if (hasImageMagick(imageMagickConfig)) {
            return SUPPORTED_TYPES;
        }

        // Otherwise don't advertise anything, so the other parsers
        //  can be selected instead
        return Collections.emptySet();
    }

    private boolean hasImageMagick(ImageMagickConfig config) {
        // Fetch where the config says to find hasImageMagick
        String imageMagick = config.getImageMagickPath() + getImageMagickProg();

        // Have we already checked for a copy of ImageMagick there?
        if (IMAGEMAGICK_PRESENT.containsKey(imageMagick)) {
            return IMAGEMAGICK_PRESENT.get(imageMagick);
        }

        // Try running ImageMagick from there, and see if it exists + works
        String[] checkCmd = {imageMagick};
        try {
            boolean hasImageMagick = ExternalParser.check(checkCmd);
            IMAGEMAGICK_PRESENT.put(imageMagick, hasImageMagick);
            return hasImageMagick;
        } catch (NoClassDefFoundError e) {
            // This happens under OSGi + Fork Parser - see TIKA-1507
            // As a workaround for now, just say we can't use OCR
            // TODO Resolve it so we don't need this try/catch block
            IMAGEMAGICK_PRESENT.put(imageMagick, false);
            return false;
        }
    }

    @Override
    public void parse(InputStream stream, ContentHandler handler, Metadata metadata, ParseContext context)
            throws IOException, SAXException, TikaException {
        ImageMagickConfig config = context.get(ImageMagickConfig.class, DEFAULT_IMAGEMAGICK_CONFIG);

        // If ImageMagick is not on the path with the current config, do not try to run OCR
        // getSupportedTypes shouldn't have listed us as handling it, so this should only
        //  occur if someone directly calls this parser, not via DefaultParser or similar
        //TemporaryResources tmp = new TemporaryResources();
        //TikaInputStream pdfStream = TikaInputStream.get(stream);
        PDFParser pdfParser = new PDFParser();

        //create temp handlers to investigate object
        BodyContentHandler body = new BodyContentHandler(-1);
        Metadata pdfMetadata = new Metadata();

        //needed to reset stream
        if (stream.markSupported()) {
            stream.mark(Integer.MAX_VALUE);
        }

        //first do initial parse to see if there's subsantial content in pdf metadata already
        pdfParser.parse(stream, body, pdfMetadata, context);
        stream.reset();
        //if there's content - reparse with official handlers/metadata. What else can you do? Also check imagemagick is available

        LegacyPdfProcessorConfig generalConfig = context.get(LegacyPdfProcessorConfig.class);

        if (body.toString().length() > generalConfig.getPdfMinDocTextLength() || !hasImageMagick(config)) {
            pdfParser.parse(stream, handler, metadata, context);
            //metadata.set("X-PDFPREPROC-OCR-APPLIED", "NA");
            return;
        }

        //metadata.set("X-PDFPREPROC-ORIGINAL", body.toString());
        // "FAIL" will be overwritten if it succeeds later

        //add the PDF metadata to the official metadata object
        Arrays.asList(pdfMetadata.names()).forEach(name -> {
            metadata.add(name, pdfMetadata.get(name));
        });

        //objects to hold file references for manipulation outside of Java
        File tiffFileOfPDF = null;
        File pdfFileFromStream = File.createTempFile("tempPDF", ".pdf");
        try {
            FileUtils.copyInputStreamToFile(stream, pdfFileFromStream);
            tiffFileOfPDF = File.createTempFile("tempTIFF", ".tiff");
            makeTiffFromPDF(pdfFileFromStream,tiffFileOfPDF, config);
            if (tiffFileOfPDF.exists()) {
                long tessStartTime = System.currentTimeMillis();
                TesseractOCRParser tesseract = new TesseractOCRParser();

                tesseract.parse(FileUtils.openInputStream(tiffFileOfPDF), handler, metadata, context);

                metadata.set(MetadataKeys.OCR_APPLIED, "true");
                metadata.add(MetadataKeys.X_TIKA_PARSED_BY, TesseractOCRParser.class.getName());

                logger.debug("Document parsing -- OCR processing time: {} ms", System.currentTimeMillis() - tessStartTime);
            }
        } catch (Exception e) {
            logger.warn("Error while running OCR over the document");
            throw e;
        }
        finally {
            if (tiffFileOfPDF.exists()) {
                tiffFileOfPDF.delete();
            }
            if (pdfFileFromStream.exists()) {
                pdfFileFromStream.delete();
            }
        }
    }

    static String getImageMagickProg() {
        return System.getProperty("os.name").startsWith("Windows") ? "convert.exe" : "convert";
    }

    private File makeTiffFromPDF(File input, File output, ImageMagickConfig config) throws IOException, TikaException {
        String[] cmd = {config.getImageMagickPath() + getImageMagickProg(),
                "-density", config.getDensity(), input.getPath(),
                "-depth", config.getDepth(),
                "-quality", config.getQuality(),
                "-background", "white", "+matte",
                output.getPath()};

        ProcessBuilder pb = new ProcessBuilder(cmd);
        //setEnv(config, pb);
        final Process process = pb.start();

        process.getOutputStream().close();
        InputStream out = process.getInputStream();
        InputStream err = process.getErrorStream();

        logStream("ImageMagick-stdout", out, input);
        logStream("ImageMagick-stderr", err, input);

        FutureTask<Integer> waitTask = new FutureTask<Integer>(new Callable<Integer>() {
            public Integer call() throws Exception {
                return process.waitFor();
            }
        });

        Thread waitThread = new Thread(waitTask);
        waitThread.start();

        try {
            waitTask.get(config.getTimeout(), TimeUnit.SECONDS);
            return output;
        } catch (Exception e) {
            waitThread.interrupt();
            process.destroy();

            if (output.exists())
            {
                IOUtils.closeQuietly(out);
                output.delete();
            }

            if (e instanceof InterruptedException)
            {
                Thread.currentThread().interrupt();
                throw new TikaException("ImageMagick-OCR-PDFParser: interrupted", e);
            }
            else if (e instanceof  ExecutionException)
            {
                throw new TikaException("ImageMagick-OCR-PDFParser: execution exception ", e);
            }
            else if (e instanceof TimeoutException)
            {
                throw new TikaException("ImageMagick-OCR-PDFParser: timeout", e);
            }
        }

        return null;
    }

    /**
     * Starts a thread that reads the contents of the standard output or error
     * stream of the given process to not block the process. The stream is
     * closed once fully processed.
     */
    private void logStream(final String logType, final InputStream stream, final File file) {
        new Thread() {
            public void run() {
                Reader reader = new InputStreamReader(stream);
                StringBuilder out = new StringBuilder();
                char[] buffer = new char[1024];
                try {
                    for (int n = reader.read(buffer); n != -1; n = reader.read(buffer)) {
                        out.append(buffer, 0, n);
                    }
                } catch (IOException e) {

                } finally {
                    IOUtils.closeQuietly(stream);
                }

                String msg = out.toString();
                LogFactory.getLog(LegacyPdfProcessorParser.class).debug(msg);
            }
        }.start();
    }
}