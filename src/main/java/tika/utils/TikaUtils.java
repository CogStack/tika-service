package tika.utils;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tika.parser.txt.CharsetDetector;
import org.apache.tika.parser.txt.CharsetMatch;
import tika.model.MetadataKeys;

import java.io.InputStream;
import java.util.*;

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
            MetadataKeys.LAST_MODIFIED, MetadataKeys.OCR_APPLIED, MetadataKeys.CREATION_DATE,
            MetadataKeys.LAST_SAVED_DATE, MetadataKeys.AUTHOR, MetadataKeys.CATEGORY, MetadataKeys.KEYWORDS,
            MetadataKeys.APPLICATION_NAME, MetadataKeys.CONTENT_ENCODING, MetadataKeys.WORD_COUNT,
            MetadataKeys.CHARACTER_COUNT, MetadataKeys.MIME_TYPE_TAG, MetadataKeys.MODIFIED_DATE, MetadataKeys.COMPANY,
            MetadataKeys.COMMENTS, MetadataKeys.CREATOR, MetadataKeys.IDENTIFIER, MetadataKeys.SUBJECT,
            MetadataKeys.DESCRIPTION};

    private static final String[] metaKeysMultiValue = {MetadataKeys.X_TIKA_PARSED_BY};

    public static <T> List<List<T>> getBatchesFromList(List<T> collection, int batchSize){
        int i = 0;
        List<List<T>> batches = new ArrayList<List<T>>();
        while(i < collection.size()){
            int nextInc = Math.min(collection.size()-i,batchSize);
            List<T> batch = collection.subList(i,i+nextInc);
            batches.add(batch);
            i = i + nextInc;
        }

        return batches;
    }

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

        var meta_keys = docMeta.names();

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

    public static String detectEncoding(final InputStream inputStream) {
        String result = "";

        // detect charset (pick the one with the highest confidence)
        try {
            CharsetDetector charsetDetector = new CharsetDetector();
            charsetDetector.setText(inputStream);
            CharsetMatch charsetMatch = charsetDetector.detect();
            inputStream.reset();

            result = charsetMatch.getName();
        }
        catch (Exception e) {
            Logger logger = LogManager.getLogger(TikaUtils.class);
            e.printStackTrace();
            logger.error("Failed to detect encoding type in text" + e.getMessage());
        }

        return result;
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
