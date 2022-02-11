package tika.processor;

import org.apache.tika.io.TikaInputStream;
import org.springframework.web.multipart.MultipartFile;
import tika.model.TikaBinaryDocument;
import tika.model.TikaProcessingResult;

import java.io.InputStream;
import java.util.List;


/**
 * An abstract class for a Tika Processor
 */
public abstract class AbstractTikaProcessor {

    /**
     * Processor lifecycle methods
     */
    public void init() throws Exception {}

    public void reset() throws Exception {}

    /**
     * The main documents processing method
     */
    protected abstract TikaProcessingResult processStream(TikaInputStream tikaInputStream);

    protected abstract List<TikaProcessingResult> processBatch(MultipartFile[] multipartFiles);

    /**
     * Wrappers over the main document processing method
     */
    public TikaProcessingResult process(final TikaBinaryDocument tikaBinaryDocument) {
        return processStream(TikaInputStream.get(tikaBinaryDocument.getContent()));
    }

    public TikaProcessingResult process(InputStream stream) {
        return processStream(TikaInputStream.get(stream));
    }

    public TikaProcessingResult process(TikaInputStream tikaInputStream) {
        return processStream(tikaInputStream);
    }

    public List<TikaProcessingResult> process(MultipartFile[] multipartFiles) {
        return processBatch(multipartFiles);
    }

}
