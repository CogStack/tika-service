package tika.processor;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.ocr.TesseractOCRParser;
import tika.model.MetadataKeys;
import tika.model.TikaBinaryDocument;
import tika.model.TikaProcessingResult;


public abstract class AbstractTikaProcessor {

    private static final String[] metaKeysSingleValue = {MetadataKeys.CONTENT_TYPE, MetadataKeys.CREATION_DATE,
            MetadataKeys.LAST_MODIFIED, MetadataKeys.OCR_APPLIED};
    private static final String[] metaKeysMultiValue = {MetadataKeys.PARSED_BY};


    public void init() throws Exception {}

    public void reset() throws Exception {}

    public TikaProcessingResult process(final TikaBinaryDocument binaryDoc) {
        return processStream(TikaInputStream.get(binaryDoc.getContent()));
    }

    public TikaProcessingResult process(InputStream stream) {
        return processStream(TikaInputStream.get(stream));
    }

    protected abstract TikaProcessingResult processStream(TikaInputStream stream);


    // helper methods used to extract document metadata -- TODO: can be moved to utils
    //
    static public int getPageCount(final Metadata docMeta) {
        Map<String, Object> resultMeta = new HashMap<>();
        extractPageCount(docMeta, resultMeta);

        if (resultMeta.containsKey(MetadataKeys.PAGE_COUNT)) {
            return Integer.parseInt(resultMeta.get(MetadataKeys.PAGE_COUNT).toString());
        }
        return -1;
    }

    static public boolean isValidDocumentType(final Map<String, Object> resultMeta) {
        return !( !resultMeta.containsKey(MetadataKeys.CONTENT_TYPE) ||
                   resultMeta.get(MetadataKeys.CONTENT_TYPE).equals(MediaType.OCTET_STREAM.toString()) ||
                   resultMeta.get(MetadataKeys.CONTENT_TYPE).equals(MediaType.EMPTY.toString()));
    }

    static private void extractPageCount(final Metadata docMeta, Map<String, Object> resultMeta) {
        String pgValue = null;
        if (docMeta.get("xmpTPg:NPages") != null) {
            pgValue = docMeta.get("xmpTPg:NPages");
        }
        else if (docMeta.get("meta:page-count") != null) {
            pgValue = docMeta.get("meta:page-count");
        }
        else if (docMeta.get("exif:PageCount") != null) {
            pgValue = docMeta.get("exif:PageCount");
        }
        else if (docMeta.get("Page-Count") != null) {
            pgValue = docMeta.get("Page-Count");
        }

        if (pgValue != null) {
            resultMeta.put(MetadataKeys.PAGE_COUNT, pgValue);
        }
    }

    static private void extractOcrApplied(final Metadata docMeta, Map<String, Object> resultMeta) {
        if (docMeta.get("X-Parsed-By") != null
                && (Arrays.asList(docMeta.getValues("X-Parsed-By")).contains(TesseractOCRParser.class.getName())
                // note that some parsers are also adding class prefix to the name: 'class org...
               || Arrays.asList(docMeta.getValues("X-Parsed-By")).contains(TesseractOCRParser.class.toString()))) {
            resultMeta.put(MetadataKeys.OCR_APPLIED, "true");
        }
        else {
            resultMeta.put(MetadataKeys.OCR_APPLIED, "false");
        }
    }

    protected Map<String, Object> extractMetadata(final Metadata docMeta) {
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
}
