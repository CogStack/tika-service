package tika.processor;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ocr.TesseractOCRParser;
import tika.model.TikaBinaryDocument;
import tika.model.TikaProcessingResult;


public abstract class AbstractTikaProcessor {

    private final String[] metaKeysSingleValue = {"Content-Type", "Creation-Date", "Last-Modified", "X-OCR-Applied"};
    private final String[] metaKeysMultiValue = {"X-Parsed-By"};

    public TikaProcessingResult process(final TikaBinaryDocument binaryDoc) {
        return processStream(TikaInputStream.get(binaryDoc.getContent()));
    }

    public TikaProcessingResult process(InputStream binaryStream) {
        return processStream(TikaInputStream.get(binaryStream));
    }

    protected abstract TikaProcessingResult processStream(TikaInputStream stream);


    // helper methods used to extract document metadata
    //
    static public int getPageCount(final Metadata docMeta) {
        Map<String, Object> resultMeta = new HashMap<>();
        extractPageCount(docMeta, resultMeta);

        if (resultMeta.containsKey("Page-Count")) {
            return Integer.parseInt(resultMeta.get("Page-Count").toString());
        }
        return -1;
    }

    static private void extractPageCount(final Metadata docMeta, Map<String, Object> resultMeta) {
        if (docMeta.get("xmpTPg:NPages") != null) {
            resultMeta.put("Page-Count", docMeta.get("xmpTPg:NPages"));
        }
        else if (docMeta.get("Page-Count") != null) {
            resultMeta.put("Page-Count", docMeta.get("Page-Count"));
        }
        else if (docMeta.get("meta:page-count") != null) {
            resultMeta.put("Page-Count", docMeta.get("meta:page-count"));
        }
    }

    static private void extractOcrApplied(final Metadata docMeta, Map<String, Object> resultMeta) {
        if (docMeta.get("X-Parsed-By") != null
                && Arrays.asList(docMeta.getValues("X-Parsed-By")).contains(TesseractOCRParser.class.getName())) {
            resultMeta.put("X-OCR-Applied", "true");
        }
        else {
            resultMeta.put("X-OCR-Applied", "false");
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
