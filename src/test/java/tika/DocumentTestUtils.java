package tika;

import tika.model.MetadataKeys;
import tika.model.TikaProcessingResult;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.apache.commons.text.similarity.LevenshteinDistance;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Helper utilities used in tests
 */
public class DocumentTestUtils {

    public static final double SIMILARITY_THRESHOLD = 0.95;

    public InputStream getDocumentStream(final String docName) throws Exception {
        final String fullPath = "tika/docs/" + docName;
        InputStream stream = getClass().getClassLoader().getResourceAsStream(fullPath);
        assertNotNull(stream);  
        return new ByteArrayInputStream(stream.readAllBytes());
    }

    public InputStream getDocumentZipStream(final String archiveName, final String zipEntry) throws Exception {
        final String fullPath = "tika/docs/" + archiveName;
        final ZipEntry entry = new ZipEntry(zipEntry);
        ZipFile zf = new ZipFile(Objects.requireNonNull(getClass().getClassLoader().getResource(fullPath)).getPath());
        InputStream stream = zf.getInputStream(entry);
        assertNotNull(stream);
        return stream;
    }

    public String getDocumentText() throws Exception {
        final String fullPath = "tika/docs/generic/pat_id_1.txt";
        InputStream stream = getClass().getClassLoader().getResourceAsStream(fullPath);
        assertNotNull(stream);
        return new String(stream.readAllBytes());
    }

    public void assertContentMatches(final String expected, final String actual) {
        // note that this check is a very naive method of content comparison, as we only
        // strip all the special characters and compare the content in lowercase
        final String regexPattern = "[^\\dA-Za-z]";
        final String s1parsed = expected.replaceAll(regexPattern, "");
        final String s2parsed = actual.replaceAll(regexPattern, "");
        assertTrue(getSimilarityScore(s1parsed, s2parsed) > SIMILARITY_THRESHOLD);
    }

    public void assertPageCount(final int expectedPageCount, TikaProcessingResult result) {
        Map<String, Object> metadata = result.getMetadata();
        assertTrue(metadata.containsKey(MetadataKeys.PAGE_COUNT));
        assertEquals(metadata.get(MetadataKeys.PAGE_COUNT), expectedPageCount);
    }

    public void assertOcrApplied(final boolean expectedStatus, TikaProcessingResult result) {
        Map<String, Object> metadata = result.getMetadata();
        if (metadata.containsKey(MetadataKeys.OCR_APPLIED)) {
            assertEquals(metadata.get(MetadataKeys.OCR_APPLIED), expectedStatus);
        }
        else {
            assertFalse(expectedStatus);
        }
    }

    public void testContentMatch(final TikaProcessingResult result) throws Exception {
        // read truth document
        final String sourceText = getDocumentText();

        // test status and content
        assertTrue(result.getText().length() > 0);
        assertContentMatches(sourceText, result.getText());
    }

    private double getSimilarityScore(final String s1, final String s2) {
        final int led = new LevenshteinDistance().apply(s1, s2);
        return 1 - ((double) led) / (Math.max(s1.length(), s2.length()));
    }
}
