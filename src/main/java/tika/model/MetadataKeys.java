package tika.model;

/**
 * Metadata keys that are to be used to extract relevant information
 * from the document alongside the text.
 * Note that some of these keys may not be available, depending on the document type.
 */
public class MetadataKeys {
    // Tika tags
    public final static String CONTENT_TYPE = "Content-Type";
    public final static String OCR_APPLIED = "X-OCR-Applied";
    public final static String X_TIKA_PARSED_BY = "X-TIKA:Parsed-By";
    public final static String PAGE_COUNT = "Page-Count";
    public final static String IMAGE_PROCESSING_ENABLED = "Image-Processing-Enabled";

    // MS OFFICE metadata tags
    public final static String COMMENTS = "meta:comments";
    public final static String AUTHOR = "meta:last-author";
    public final static String CATEGORY = "Category";
    public final static String CREATOR = "dc:creator";
    public final static String KEYWORDS = "Keywords";
    public final static String WORD_COUNT = "meta:word-count";
    public final static String CHARACTER_COUNT = "meta:character-count";
    public final static String LAST_SAVED_DATE = "Last-Save-Date";
    public final static String MODIFIED_DATE = "dcterms:modified";
    public final static String APPLICATION_NAME = "extended-properties:Application";
    public final static String COMPANY = "extender-properties:Company";
    public final static String CREATION_DATE = "dcterms:created";
    public final static String DESCRIPTION = "dc:description";
    public final static String IDENTIFIER = "dc:identifier";
    public final static String SUBJECT = "dc:subject";

    // HTML metadata tags
    public final static String LAST_MODIFIED = "Last-Modified";
    public static final String CONTENT_ENCODING = "Content-Encoding";

    // MIME type
    public static final String MIME_TYPE_TAG = "mime-type";
}
