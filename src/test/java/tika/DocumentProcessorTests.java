package tika;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import tika.model.TikaProcessingResult;
import tika.processor.AbstractTikaProcessor;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;


/**
 * All the document processor tests are implemented in this abstract class in order to keep the
 * rationale behind the tests and results in one single place.
 */
public abstract class DocumentProcessorTests {

    protected DocumentTestUtils utils = new DocumentTestUtils();

    /**
     * Helper methods used in tests that can be overloaded in child classes
     */
    protected AbstractTikaProcessor getProcessor() { return null; }

    protected TikaProcessingResult processDocument(final String docPath) throws Exception  {
        AbstractTikaProcessor processor = getProcessor();
        assertNotNull(processor);

        InputStream stream = utils.getDocumentStream(docPath);
        return processor.process(stream);
    }


    /**
     * The actual tests start from here
     *
     *
     */

    @Test
    public void testGenericExtractPattern1SourceTxt() throws Exception {
        final String docPathPrefix = "generic/pat_id_1";
        final String docExt = ".txt";

        TikaProcessingResult result = processDocument(docPathPrefix + docExt);
        assertTrue(result.getSuccess());

        // test parsing status
        String parsedString = result.getText();
        assertEquals(310, parsedString.length());

        // test metadata
        utils.assertOcrApplied(false, result);
    }

    @Test
    public void testGenericExtractPattern1Doc() throws Exception {
        final String docPathPrefix = "generic/pat_id_1";
        final String docExt = ".doc";

        TikaProcessingResult result = processDocument(docPathPrefix + docExt);
        assertTrue(result.getSuccess());

        utils.testContentMatch(result);

        // test metadata
        utils.assertPageCount(1, result);
        utils.assertOcrApplied(true, result);
    }

    @Test
    public void testGenericExtractPattern1Docx() throws Exception {
        final String docPathPrefix = "generic/pat_id_1";
        final String docExt = ".docx";

        TikaProcessingResult result = processDocument(docPathPrefix + docExt);
        assertTrue(result.getSuccess());

        utils.testContentMatch(result);

        // test metadata
        utils.assertPageCount(1, result);
        utils.assertOcrApplied(true, result);
    }

    @Test
    public void testGenericExtractPattern1Odt() throws Exception {
        final String docPathPrefix = "generic/pat_id_1";
        final String docExt = ".odt";

        TikaProcessingResult result = processDocument(docPathPrefix + docExt);
        assertTrue(result.getSuccess());

        utils.testContentMatch(result);

        // test metadata
        utils.assertPageCount(1, result);
        utils.assertOcrApplied(true, result);
    }

    @Test
    public void testGenericExtractPattern1Rtf() throws Exception {
        final String docPathPrefix = "generic/pat_id_1";
        final String docExt = ".txt";

        TikaProcessingResult result = processDocument(docPathPrefix + docExt);
        assertTrue(result.getSuccess());

        utils.testContentMatch(result);

        // test metadata
        // rtf does not contain page count
        utils.assertOcrApplied(false, result);
    }

    @Test
    public void testGenericExtractPattern1Png() throws Exception {
        final String docPathPrefix = "generic/pat_id_1";
        final String docExt = ".png";

        TikaProcessingResult result = processDocument(docPathPrefix + docExt);
        assertTrue(result.getSuccess());

        utils.testContentMatch(result);

        // test metadata
        // png does not contain page count
        utils.assertOcrApplied(true, result);
    }

    @Test
    public void testGenericExtractPattern1Pdf() throws Exception {
        final String docPathPrefix = "generic/pat_id_1";
        final String docExt = ".pdf";

        TikaProcessingResult result = processDocument(docPathPrefix + docExt);
        assertTrue(result.getSuccess());

        utils.testContentMatch(result);

        // test metadata
        utils.assertPageCount(1, result);
        utils.assertOcrApplied(true, result); // Although this pdf contains text-only, enable-image-processing has been set to true
    }

    @Test
    public void testExtractPdfEx1WithOcr() throws Exception {
        final String docPath = "pdf/ex1.pdf";

        TikaProcessingResult result = processDocument(docPath);

        // check an example string
        assertTrue(result.getSuccess());
        assertTrue(result.getText().contains("An Example Paper"));

        // test metadata
        utils.assertPageCount(10, result);
        utils.assertOcrApplied(true, result); // Although this pdf contains text-only, enable-image-processing has been set to true
    }

    @Test
    public void testExtractPdfEx1Encrypted() throws Exception  {
        final String docPath = "pdf/ex1_enc.pdf";

        TikaProcessingResult result = processDocument(docPath);

        // extraction from encrypted PDF will fail with the proper error message
        assertFalse(result.getSuccess());
        assertTrue(result.getError().contains("document is encrypted"));
    }

    @Test
    public void testExtractPdfEx2WithOcr() throws Exception {
        final String docPath = "pdf/ex2_ocr.pdf";

        TikaProcessingResult result = processDocument(docPath);

        // check the content
        assertTrue(result.getSuccess());
        final String parsedString = result.getText();
        assertTrue(parsedString.length() > 0);

        // example text from the first page
        assertTrue(parsedString.contains("Father or mother"));
        // example text from the second page
        assertTrue(parsedString.contains("how you have determined who is the Nearest"));

        // test medatata
        utils.assertPageCount(2, result);
        utils.assertOcrApplied(true, result);
    }


    // TODO: need to double-check how to handle invalid TIFFs or image files
    @Disabled
    @Test
    public void testExtractTiffWithOCR() throws Exception {
        InputStream stream = utils.getDocumentZipStream("invalid/tiff_multipage_spp2.tiff.zip", "tiff_multipage_spp2.tiff");

        AbstractTikaProcessor processor = getProcessor();
        TikaProcessingResult result = processor.process(stream);
        assertTrue(result.getSuccess());

        // HINT: the test should fail either as the TIFF is invalid
        // or should an additional pre-processing of the image happen

        // test parsing status
        String parsedString = result.getText();
        assertTrue(parsedString.length() > 0);

        // test metadata
        utils.assertPageCount(6, result);

        // test example content
        // - from first page
        assertTrue(parsedString.contains("Sample Narrative Report"));
    }


    //TODO: need to create a proper docx encrypted file
    @Disabled
    @Test
    public void testExtractWordEncrypted() throws Exception {
        InputStream stream = utils.getDocumentStream("word_enc_noerror.docx");

        AbstractTikaProcessor processor = getProcessor();
        TikaProcessingResult result = processor.process(stream);

        // extraction from encrypted DOCX will succeed but with the content empty and no error message
        // uses: org.apache.tika.parser.microsoft.OfficeParser
        //TODO: this one needs an internal fix or further investigation
        assertTrue(result.getSuccess());
    }
}
