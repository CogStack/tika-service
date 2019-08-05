package tika;

import tika.model.MetadataKeys;
import tika.model.TikaProcessingResult;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.Assert.*;
import static org.junit.Assert.assertTrue;


/**
 * Helper utilities used in tests
 */
public class DocumentTestUtils {
    public InputStream getDocumentStream(final String docName) throws Exception {
        final String fullPath = "tika/docs/" + docName;
        InputStream stream = getClass().getClassLoader().getResourceAsStream(fullPath);
        assertNotNull(stream);
        ByteArrayInputStream bas = new ByteArrayInputStream(stream.readAllBytes());
        return bas;
    }

    public InputStream getDocumentZipStream(final String archiveName, final String zipEntry) throws Exception {
        final String fullPath = "tika/docs/" + archiveName;
        final ZipEntry entry = new ZipEntry(zipEntry);
        ZipFile zf = new ZipFile(getClass().getClassLoader().getResource(fullPath).getPath());
        InputStream stream = zf.getInputStream(entry);
        assertNotNull(stream);
        return stream;
    }

    public String getDocumentText(final String path) throws Exception {
        return new String(getDocumentStream(path).readAllBytes());
    }


    public void assertContentMatches(final String expected, final String actual) {
        // note that this check is a very naive method of content comparison, as we only
        // strip all the special characters and compare the content in lowercase
        final String regexPattern = "[^\\dA-Za-z]";
        final String s1parsed = expected.replaceAll(regexPattern, "");
        final String s2parsed = actual.replaceAll(regexPattern, "");
        assertEquals(s1parsed, s2parsed);
    }

    public void assertPageCount(final int expectedPageCount, TikaProcessingResult result) {
        Map<String, Object> metadata = result.getMetadata();
        assertTrue(metadata.containsKey(MetadataKeys.PAGE_COUNT));
        assertEquals(Integer.parseInt(metadata.get(MetadataKeys.PAGE_COUNT).toString()), expectedPageCount);
    }

    public void assertOcrApplied(final boolean expectedStatus, TikaProcessingResult result) {
        Map<String, Object> metadata = result.getMetadata();
        if (metadata.containsKey(MetadataKeys.OCR_APPLIED)) {
            assertEquals(Boolean.parseBoolean(metadata.get(MetadataKeys.OCR_APPLIED).toString()), expectedStatus);
        }
        else {
            assertFalse(expectedStatus);
        }
    }


    public void testContentMatch(final TikaProcessingResult result, final String docPathPrefix) throws Exception {
        // read truth document
        final String sourceText = getDocumentText(docPathPrefix + ".txt");

        // test status and content
        assertTrue(result.getText().length() > 0);
        assertContentMatches(sourceText, result.getText());
    }
}
