package tika.processor;

import org.apache.tika.batch.ConsumersManager;
import org.apache.tika.batch.FileResourceConsumer;

import java.util.List;

public class TikaConsumerManager extends ConsumersManager {
    public TikaConsumerManager(List<FileResourceConsumer> consumers) {
        super(consumers);
    }
}
