package tika.processor;

import org.apache.tika.batch.FileResource;
import org.apache.tika.batch.FileResourceCrawler;
import java.util.concurrent.ArrayBlockingQueue;

public class TikaFileResourceCrawler extends FileResourceCrawler {
    /**
     * @param queue        shared queue
     * @param numConsumers number of consumers (needs to know how many poisons to add when done)
     */
    public TikaFileResourceCrawler(ArrayBlockingQueue<FileResource> queue, int numConsumers) {
        super(queue, numConsumers);
    }

    @Override
    public void start() throws InterruptedException {

    }

}
