package tika;

import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import tika.legacy.LegacyPdfProcessorConfig;
import tika.processor.AbstractTikaProcessor;
import tika.processor.CompositeTikaProcessor;
import tika.processor.CompositeTikaProcessorConfig;


/**
 * Implements the tests using CompositeTikaProcessor as the documents processor
 */
@SpringBootTest(classes = CompositeTikaProcessor.class)
@ContextConfiguration(classes = {LegacyPdfProcessorConfig.class, CompositeTikaProcessorConfig.class})
public class CompositeTikaProcessorTests extends DocumentProcessorTests {

    @Autowired
    LegacyPdfProcessorConfig legacyProcessorConfig;

    @Autowired
    CompositeTikaProcessorConfig compositeProcessorConfig;

    @Autowired
    CompositeTikaProcessor processor;


    @Override
    protected AbstractTikaProcessor getProcessor() {
        return processor;
    }

    @AfterEach
    public void reset() throws Exception {
        processor.reset();
    }
}

