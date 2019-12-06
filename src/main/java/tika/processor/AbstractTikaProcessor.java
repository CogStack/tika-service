package tika.processor;

import java.io.InputStream;
import org.apache.tika.io.TikaInputStream;
import tika.model.TikaBinaryDocument;
import tika.model.TikaProcessingResult;


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
    protected abstract TikaProcessingResult processStream(TikaInputStream stream);


    /**
     * Wrappers over the main document processing method
     */
    public TikaProcessingResult process(final TikaBinaryDocument binaryDoc) {
        return processStream(TikaInputStream.get(binaryDoc.getContent()));
    }

    public TikaProcessingResult process(InputStream stream) {
        return processStream(TikaInputStream.get(stream));
    }
}
