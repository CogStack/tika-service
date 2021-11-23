package tika.model;

/**
 * Metadata keys that are to be used to extract relevant information
 * from the document alongside the text.
 * Note that some of these keys may not be available, depending on the document type.
 */
public class MetadataKeys {
    public final static String CONTENT_TYPE = "Content-Type";
    public final static String CREATION_DATE = "Creation-Date";
    public final static String LAST_MODIFIED = "Last-Modified";
    public final static String OCR_APPLIED = "X-OCR-Applied";
    public final static String PARSED_BY = "X-Parsed-By";
    public final static String X_TIKA_PARSED_BY = "X-TIKA:Parsed-By";
    public final static String PAGE_COUNT = "Page-Count";
    public final static String IMAGE_PROCESSING_ENABLED = "Image-Processing-Enabled";
}
