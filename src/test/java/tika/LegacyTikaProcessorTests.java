package tika;

import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import tika.legacy.LegacyPdfProcessorConfig;
import java.io.InputStream;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.junit.Test;
import tika.legacy.LegacyTikaProcessor;
import tika.model.MetadataKeys;
import tika.model.TikaProcessingResult;

import static org.junit.Assert.*;


@SpringBootTest(classes = LegacyTikaProcessor.class)
@RunWith(SpringRunner.class)
@DirtiesContext
@ContextConfiguration(classes = {LegacyPdfProcessorConfig.class})
@TestPropertySource(properties = {"spring.config.location = classpath:processor.yaml,classpath:tika/config/tika-processor-config.yaml"})
public class LegacyTikaProcessorTests {

    @Autowired
    LegacyPdfProcessorConfig defaultConfig;

    @Autowired
    LegacyTikaProcessor processor;


    @After
    public void reset() {
        processor.reset();
    }

    @Test
    public void testGenericExtractPattern1SourceTxt() throws Exception {
        final String docPathPrefix = "generic/pat_id_1";

        TikaProcessingResult result = processDocument(docPathPrefix, ".txt");
        assertTrue(result.getSuccess());

        // test parsing status
        String parsedString = result.getText();
        assertEquals(310, parsedString.length());

        // test metadata
        assertOcrApplied(false, result);
    }

    @Test
    public void testGenericExtractPattern1Doc() throws Exception {
        final String docPathPrefix = "generic/pat_id_1";

        TikaProcessingResult result = processDocument(docPathPrefix, ".doc");
        assertTrue(result.getSuccess());

        testContentMatch(result, docPathPrefix);

        // test metadata
        assertPageCount(1, result);
        assertOcrApplied(false, result);
    }

    @Test
    public void testGenericExtractPattern1Docx() throws Exception {
        final String docPathPrefix = "generic/pat_id_1";

        TikaProcessingResult result = processDocument(docPathPrefix, ".docx");
        assertTrue(result.getSuccess());

        testContentMatch(result, docPathPrefix);

        // test metadata
        assertPageCount(1, result);
        assertOcrApplied(false, result);
    }

    @Test
    public void testGenericExtractPattern1Odt() throws Exception {
        // read document
        final String docPathPrefix = "generic/pat_id_1";

        TikaProcessingResult result = processDocument(docPathPrefix, ".odt");
        assertTrue(result.getSuccess());

        testContentMatch(result, docPathPrefix);

        // test metadata
        assertPageCount(1, result);
        assertOcrApplied(false, result);
    }

    @Test
    public void testGenericExtractPattern1Rtf() throws Exception {
        // read document
        final String docPathPrefix = "generic/pat_id_1";

        TikaProcessingResult result = processDocument(docPathPrefix, ".rtf");
        assertTrue(result.getSuccess());

        testContentMatch(result, docPathPrefix);

        // test metadata
        // rtf does not contain page count
        assertOcrApplied(false, result);
    }

    @Test
    public void testGenericExtractPattern1Png() throws Exception {
        // read document
        final String docPathPrefix = "generic/pat_id_1";

        TikaProcessingResult result = processDocument(docPathPrefix, ".png");
        assertTrue(result.getSuccess());

        testContentMatch(result, docPathPrefix);

        // test metadata
        // png does not contain page count
        assertOcrApplied(true, result);
    }

    @Test
    public void testGenericExtractPattern1Pdf() throws Exception {
        // read document
        final String docPathPrefix = "generic/pat_id_1";

        TikaProcessingResult result = processDocument(docPathPrefix, ".pdf");
        assertTrue(result.getSuccess());

        testContentMatch(result, docPathPrefix);

        // test metadata
        assertPageCount(1, result);
        assertOcrApplied(false, result); // this pdf contains text-only
    }



    @Test
    public void testExtractPdfEx1WithoutOcr() throws Exception {
        InputStream stream = getDocumentStream("pdf/ex1.pdf");

        TikaProcessingResult result = processor.process(stream);

        // check an example string
        assertTrue(result.getSuccess());
        assertTrue(result.getText().contains("An Example Paper"));

        // test metadata
        assertPageCount(10, result);
        assertOcrApplied(false, result); // this pdf contains text-only
    }

    @Test
    public void testExtractPdfEx1Encrypted() throws Exception  {
        InputStream stream = getDocumentStream("pdf/ex1_enc.pdf");

        TikaProcessingResult result = processor.process(stream);

        // extraction from encrypted PDF will fail with the proper error message
        assertFalse(result.getSuccess());
        assertTrue(result.getError().contains("document is encrypted"));
    }

    @Test
    public void testExtractPdfEx2WithOcr() throws Exception {
        InputStream stream = getDocumentStream("pdf/ex2_ocr.pdf");

        TikaProcessingResult result = processor.process(stream);

        // check the content
        assertTrue(result.getSuccess());
        final String parsedString = result.getText();
        assertTrue(parsedString.length() > 0);

        // example text from the first page
        assertTrue(parsedString.contains("Father or mother"));
        // example text from the second page
        assertTrue(parsedString.contains("how you have determined who is the Nearest"));

        // test medatata
        assertPageCount(2, result);
        assertOcrApplied(true, result);
    }


    // TODO: need to double-check how to handle invalid TIFFs or image files
    @Ignore
    @Test
    public void testExtractTiffWithOCR() throws Exception {
        InputStream stream = getDocumentZipStream("var/tiff_multipage_spp2.tiff.zip", "tiff_multipage_spp2.tiff");
        TikaProcessingResult result = processor.process(stream);
        assertTrue(result.getSuccess());

        // HINT: the test should fail either as the TIFF is invalid
        // or should an additional pre-processing of the image happen

        // test parsing status
        String parsedString = result.getText();
        assertTrue(parsedString.length() > 0);

        // test metadata
        assertPageCount(6, result);

        // test example content
        // - from first page
        assertTrue(parsedString.contains("Sample Narrative Report"));
    }


    //TODO: need to create a proper docx encrypted file
    @Ignore
    @Test
    public void testExtractWordEncrypted() throws Exception {
        InputStream stream = getDocumentStream("word_enc_noerror.docx");

        TikaProcessingResult result = processor.process(stream);

        // extraction from encrypted DOCX will succeed but with the content empty and no error message
        // uses: org.apache.tika.parser.microsoft.OfficeParser
        //TODO: this one needs an internal fix or further investigation
        assertTrue(result.getSuccess());
    }


    // helper methods
    // TODO: make as a part of utils
    //
    private InputStream getDocumentStream(final String docName) {
        final String fullPath = "tika/docs/" + docName;
        InputStream stream = getClass().getClassLoader().getResourceAsStream(fullPath);
        assertNotNull(stream);
        return stream;
    }

    private InputStream getDocumentZipStream(final String archiveName, final String zipEntry) throws Exception {
        final String fullPath = "tika/docs/" + archiveName;
        final ZipEntry entry = new ZipEntry(zipEntry);
        ZipFile zf = new ZipFile(getClass().getClassLoader().getResource(fullPath).getPath());
        InputStream stream = zf.getInputStream(entry);
        assertNotNull(stream);
        return stream;
    }

    private String getDocumentText(final String path) throws Exception {
        return new String(getDocumentStream(path).readAllBytes());
    }

    private void assertContentMatches(final String expected, final String actual) {
        final String regexPattern = "[^\\dA-Za-z]";
        final String s1parsed = expected.replaceAll(regexPattern, "");
        final String s2parsed = actual.replaceAll(regexPattern, "");
        assertEquals(s1parsed, s2parsed);
    }

    private void assertPageCount(final int expectedPageCount, TikaProcessingResult result) {
        Map<String, Object> metadata = result.getMetadata();
        assertTrue(metadata.containsKey(MetadataKeys.PAGE_COUNT));
        assertEquals(Integer.parseInt(metadata.get(MetadataKeys.PAGE_COUNT).toString()), expectedPageCount);
    }

    private void assertOcrApplied(final boolean expectedStatus, TikaProcessingResult result) {
        Map<String, Object> metadata = result.getMetadata();
        if (metadata.containsKey(MetadataKeys.OCR_APPLIED)) {
            assertEquals(Boolean.parseBoolean(metadata.get(MetadataKeys.OCR_APPLIED).toString()), expectedStatus);
        }
        else {
            assertFalse(expectedStatus);
        }
    }

    private TikaProcessingResult processDocument(final String docPathPrefix, final String fileExt) throws Exception  {
        InputStream stream = getDocumentStream(docPathPrefix + fileExt);
        return processor.process(stream);
    }

    private void testContentMatch(final TikaProcessingResult result, final String docPathPrefix) throws Exception {
        // read truth document
        final String sourceText = getDocumentText(docPathPrefix + ".txt");

        // test status and content
        assertTrue(result.getText().length() > 0);
        assertContentMatches(sourceText, result.getText());
    }
}

