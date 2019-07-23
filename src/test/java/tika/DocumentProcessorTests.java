package tika;

import org.junit.Ignore;
import org.junit.Test;
import tika.model.TikaProcessingResult;
import tika.processor.AbstractTikaProcessor;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public abstract class DocumentProcessorTests {

    private DocumentTestUtils utils = new DocumentTestUtils();

    protected abstract AbstractTikaProcessor getProcessor();

    
    @Test
    public void testGenericExtractPattern1SourceTxt() throws Exception {
        final String docPathPrefix = "generic/pat_id_1";

        TikaProcessingResult result = processDocument(docPathPrefix, ".txt");
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

        TikaProcessingResult result = processDocument(docPathPrefix, ".doc");


        assertTrue(result.getSuccess());

        utils.testContentMatch(result, docPathPrefix);

        // test metadata
        utils.assertPageCount(1, result);
        utils.assertOcrApplied(false, result);
    }

    @Test
    public void testGenericExtractPattern1Docx() throws Exception {
        final String docPathPrefix = "generic/pat_id_1";

        TikaProcessingResult result = processDocument(docPathPrefix, ".docx");
        assertTrue(result.getSuccess());

        utils.testContentMatch(result, docPathPrefix);

        // test metadata
        utils.assertPageCount(1, result);
        utils.assertOcrApplied(false, result);
    }

    @Test
    public void testGenericExtractPattern1Odt() throws Exception {
        // read document
        final String docPathPrefix = "generic/pat_id_1";

        TikaProcessingResult result = processDocument(docPathPrefix, ".odt");
        assertTrue(result.getSuccess());

        utils.testContentMatch(result, docPathPrefix);

        // test metadata
        utils.assertPageCount(1, result);
        utils.assertOcrApplied(false, result);
    }

    @Test
    public void testGenericExtractPattern1Rtf() throws Exception {
        // read document
        final String docPathPrefix = "generic/pat_id_1";

        TikaProcessingResult result = processDocument(docPathPrefix, ".rtf");
        assertTrue(result.getSuccess());

        utils.testContentMatch(result, docPathPrefix);

        // test metadata
        // rtf does not contain page count
        utils.assertOcrApplied(false, result);
    }

    @Test
    public void testGenericExtractPattern1Png() throws Exception {
        // read document
        final String docPathPrefix = "generic/pat_id_1";

        TikaProcessingResult result = processDocument(docPathPrefix, ".png");
        assertTrue(result.getSuccess());

        utils.testContentMatch(result, docPathPrefix);

        // test metadata
        // png does not contain page count
        utils.assertOcrApplied(true, result);
    }

    @Test
    public void testGenericExtractPattern1Pdf() throws Exception {
        // read document
        final String docPathPrefix = "generic/pat_id_1";

        TikaProcessingResult result = processDocument(docPathPrefix, ".pdf");
        assertTrue(result.getSuccess());

        utils.testContentMatch(result, docPathPrefix);

        // test metadata
        utils.assertPageCount(1, result);
        utils.assertOcrApplied(false, result); // this pdf contains text-only
    }

    @Test
    public void testExtractPdfEx1WithoutOcr() throws Exception {
        InputStream stream = utils.getDocumentStream("pdf/ex1.pdf");

        AbstractTikaProcessor processor = getProcessor();
        TikaProcessingResult result = processor.process(stream);

        // check an example string
        assertTrue(result.getSuccess());
        assertTrue(result.getText().contains("An Example Paper"));

        // test metadata
        utils.assertPageCount(10, result);
        utils.assertOcrApplied(false, result); // this pdf contains text-only
    }

    @Test
    public void testExtractPdfEx1Encrypted() throws Exception  {
        InputStream stream = utils.getDocumentStream("pdf/ex1_enc.pdf");

        AbstractTikaProcessor processor = getProcessor();
        TikaProcessingResult result = processor.process(stream);

        // extraction from encrypted PDF will fail with the proper error message
        assertFalse(result.getSuccess());
        assertTrue(result.getError().contains("document is encrypted"));
    }

    @Test
    public void testExtractPdfEx2WithOcr() throws Exception {
        InputStream stream = utils.getDocumentStream("pdf/ex2_ocr.pdf");

        AbstractTikaProcessor processor = getProcessor();
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
        utils.assertPageCount(2, result);
        utils.assertOcrApplied(true, result);
    }


    // TODO: need to double-check how to handle invalid TIFFs or image files
    @Ignore
    @Test
    public void testExtractTiffWithOCR() throws Exception {
        InputStream stream = utils.getDocumentZipStream("var/tiff_multipage_spp2.tiff.zip", "tiff_multipage_spp2.tiff");

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
    @Ignore
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


    // helper methods
    //
    private TikaProcessingResult processDocument(final String docPathPrefix,
                                                 final String fileExt) throws Exception  {
        AbstractTikaProcessor processor = getProcessor();
        InputStream stream = utils.getDocumentStream(docPathPrefix + fileExt);
        return processor.process(stream);
    }
}
