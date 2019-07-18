package tika;

import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import tika.legacy.LegacyPdfProcessorConfig;
import java.io.InputStream;
import java.util.Map;
import org.junit.Test;
import tika.legacy.LegacyTikaProcessor;
import tika.model.TikaProcessingResult;

import static org.junit.Assert.*;


@SpringBootTest(classes = LegacyTikaProcessor.class)
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {LegacyPdfProcessorConfig.class})
@TestPropertySource(locations = "classpath:tika/config/tika-processor-config.yaml")
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
    public void testExtractPdfEncrypted() {
        InputStream stream = getDocumentStream("pdf_encrypted_test.pdf");

        TikaProcessingResult result = processor.process(stream);

        // extraction from encrypted PDF will fail with the proper error message
        assertFalse(result.getSuccess());
        assertTrue(result.getError().contains("document is encrypted"));
    }

    @Test
    public void testExtractWordEncrypted() {
        InputStream stream = getDocumentStream("encryptedWordDocx.docx");

        TikaProcessingResult result = processor.process(stream);

        // extraction from encrypted DOCX will succeed but with the content empty and no error message
        //TODO: this one needs an internal fix or further investigation
        assertTrue(result.getSuccess());
        assertEquals(result.getText().length(), 0);
    }

    @Test
    public void testExtractPdfWithOCR() {
        InputStream stream = getDocumentStream("pdf_ocr_test.pdf");

        TikaProcessingResult result = processor.process(stream);
        assertTrue(result.getSuccess());

        // test parsing status
        String parsedString = result.getText();
        assertTrue(parsedString.length() > 0);

        // test metadata
        // - page count
        Map<String, Object> metadata = result.getMetadata();
        assertTrue(metadata.containsKey("Page-Count"));
        assertEquals(Integer.parseInt(metadata.get("Page-Count").toString()), 2);

        // test example content
        // - from first page
        assertTrue(parsedString.contains("Father or mother"));

        // - from second (last) page
        assertTrue(parsedString.contains("how you have determined who is the Nearest"));
    }

    @Test
    public void testExtractPdfWithoutOCR() {
        InputStream stream = getDocumentStream("pdf_nonOCR_test.pdf");

        TikaProcessingResult result = processor.process(stream);
        assertTrue(result.getSuccess());

        // test parsing status
        String parsedString = result.getText();
        assertTrue(parsedString.length() > 0);

        // test metadata
        // - page count
        Map<String, Object> metadata = result.getMetadata();
        assertTrue(metadata.containsKey("Page-Count"));
        assertEquals(Integer.parseInt(metadata.get("Page-Count").toString()), 10);

        // test example content
        // - from first page
        assertTrue(parsedString.contains("An example paper"));
    }

    @Ignore
    @Test
    public void testExtractTiffWithOCR() {
        InputStream stream = getDocumentStream("tiff_multi_pages.tiff");

        TikaProcessingResult result = processor.process(stream);
        assertTrue(result.getSuccess());

        // test parsing status
        String parsedString = result.getText();
        assertTrue(parsedString.length() > 0);

        // test metadata
        // - page count
        Map<String, Object> metadata = result.getMetadata();
        assertTrue(metadata.containsKey("Page-Count"));
        assertEquals(Integer.parseInt(metadata.get("Page-Count").toString()), 6);

        // test example content
        // - from first page
        assertTrue(parsedString.contains("Sample Narrative Report"));
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
}

