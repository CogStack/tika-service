package tika.model;

import org.apache.tika.batch.FileResourceConsumer;
import org.apache.tika.batch.FileResource;;

import java.util.concurrent.ArrayBlockingQueue;

public abstract class TikaFileResourceConsumer extends FileResourceConsumer {
    public TikaFileResourceConsumer(ArrayBlockingQueue<FileResource> fileQueue) {
        super(fileQueue);
    }

    @Override
    public boolean processFileResource(FileResource fileResource) {
        return false;
    }
}
