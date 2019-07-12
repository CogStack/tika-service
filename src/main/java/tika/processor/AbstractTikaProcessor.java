package tika.processor;

import java.io.InputStream;
import org.apache.tika.io.TikaInputStream;
import tika.model.TikaBinaryDocument;
import tika.model.TikaProcessingResult;


public abstract class AbstractTikaProcessor {

    public TikaProcessingResult process(final TikaBinaryDocument binaryDoc) {
        return processStream(TikaInputStream.get(binaryDoc.getContent()));
    }

    public TikaProcessingResult process(InputStream binaryStream) {
        return processStream(TikaInputStream.get(binaryStream));
    }

    protected abstract TikaProcessingResult processStream(TikaInputStream stream);
}
