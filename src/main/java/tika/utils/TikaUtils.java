package tika.utils;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.ocr.TesseractOCRParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tika.model.MetadataKeys;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static tika.model.MetadataKeys.IMAGE_PROCESSING_ENABLED;


/**
 * A set of Tika utilities helpful for parsing and handling documents
 *
 */
public class TikaUtils {

    /**
     * The metadata keys that should be extracted by the processor
     */
    private static final String[] metaKeysSingleValue = {MetadataKeys.CONTENT_TYPE, MetadataKeys.CREATION_DATE,
            MetadataKeys.LAST_MODIFIED, MetadataKeys.OCR_APPLIED};
    private static final String[] metaKeysMultiValue = {MetadataKeys.PARSED_BY};


    /**
     * Returns the number of pages if such information exists in metadata
     */
    static public int getPageCount(final Metadata docMeta) {
        Map<String, Object> resultMeta = new HashMap<>();
        extractPageCount(docMeta, resultMeta);

        if (resultMeta.containsKey(MetadataKeys.PAGE_COUNT)) {
            return (Integer) resultMeta.get(MetadataKeys.PAGE_COUNT);
        }
        return -1;
    }

    /**
     * Checks whether the document is valid according to the metadata content type and whether it's empty
     */
    static public boolean isValidDocumentType(final Map<String, Object> resultMeta) {
        return !( !resultMeta.containsKey(MetadataKeys.CONTENT_TYPE) ||
                resultMeta.get(MetadataKeys.CONTENT_TYPE).equals(MediaType.OCTET_STREAM.toString()) ||
                resultMeta.get(MetadataKeys.CONTENT_TYPE).equals(MediaType.EMPTY.toString()));
    }

    /**
     * Extracts and harmonizes the document metadata
     */
    public static Map<String, Object> extractMetadata(final Metadata docMeta) {
        Map<String, Object> resultMeta = new HashMap<>();
        Arrays.stream(metaKeysSingleValue).forEach(name -> {
            if (docMeta.get(name) != null)
                resultMeta.put(name, docMeta.get(name));
        });

        Arrays.stream(metaKeysMultiValue).forEach(name -> {
            if (docMeta.getValues(name) != null)
                resultMeta.put(name, docMeta.getValues(name));
        });

        extractPageCount(docMeta, resultMeta);

        extractOcrApplied(docMeta, resultMeta);

        return resultMeta;
    }

    static private void extractPageCount(final Metadata docMeta, Map<String, Object> resultMeta) {
        String pgKey = "";
        if (docMeta.get("xmpTPg:NPages") != null) {
            pgKey = "xmpTPg:NPages";
        }
        else if (docMeta.get("meta:page-count") != null) {
            pgKey = "meta:page-count";
        }
        else if (docMeta.get("exif:PageCount") != null) {
            pgKey = "exif:PageCount";
        }
        else if (docMeta.get("Page-Count") != null) {
            pgKey = "Page-Count";
        }

        if (!pgKey.isBlank()) {
            try {
                String pgValue = docMeta.get(pgKey);
                resultMeta.put(MetadataKeys.PAGE_COUNT, Integer.parseInt(pgValue));
            } catch (Exception e) {
                Logger logger = LogManager.getLogger(TikaUtils.class);
                logger.warn("Cannot parse metadata 'Page-Count' value using key name: " + pgKey);
            }
        }
    }

    static private void extractOcrApplied(final Metadata docMeta, Map<String, Object> resultMeta) {
        if ("true".equals(docMeta.get(IMAGE_PROCESSING_ENABLED)) &&
            // hacky...but raw text content should not require OCR regardless
            !Arrays.stream(docMeta.getValues("Content-Type")).anyMatch(ct -> ct.startsWith("text/"))) {
            resultMeta.put(MetadataKeys.OCR_APPLIED, true);
        }
        else {
            resultMeta.put(MetadataKeys.OCR_APPLIED, false);
        }
    }
}
