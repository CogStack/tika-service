package tika;

import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import tika.legacy.LegacyPdfProcessorConfig;
import tika.legacy.LegacyTikaProcessor;
import tika.processor.AbstractTikaProcessor;


/**
 * Implements the tests using LegacyTikaProcessor as the documents processor
 */
@SpringBootTest(classes = LegacyTikaProcessor.class)
@RunWith(SpringRunner.class)
@DirtiesContext
@ContextConfiguration(classes = {LegacyPdfProcessorConfig.class})
public class LegacyTikaProcessorTests extends DocumentProcessorTests {

    @Autowired
    LegacyPdfProcessorConfig defaultConfig;

    @Autowired
    LegacyTikaProcessor processor;

    @Override
    protected AbstractTikaProcessor getProcessor() {
        return processor;
    }

    @After
    public void reset() throws Exception {
        processor.reset();
    }
}

