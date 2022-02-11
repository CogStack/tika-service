package tika.model;

import org.apache.tika.batch.FileResource;
import org.apache.tika.metadata.Metadata;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

public class TikaFileResource implements FileResource, Closeable {

    private final String resourceId;
    private final Metadata metadata;
    private final InputStream content;
    private TikaProcessingResult tikaProcessingResult;

    public TikaFileResource(String resourceId, Metadata metadata, InputStream fileContent) {
        this.resourceId = resourceId;
        this.metadata = metadata;
        this.content = fileContent;
    }

    @Override
    public String getResourceId() {
        return resourceId;
    }

    @Override
    public Metadata getMetadata() {
        return metadata;
    }

    @Override
    public InputStream openInputStream() {
        return content;
    }

    public TikaProcessingResult getTikaProcessingResult()
    {
        return tikaProcessingResult;
    }

    public void setTikaProcessingResult(TikaProcessingResult tikaProcessingResult) {
        this.tikaProcessingResult = tikaProcessingResult;
    }

    @Override
    public void close() throws IOException {
        content.close();
    }
}
