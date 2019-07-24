package tika;

import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import tika.legacy.LegacyPdfProcessorConfig;
import tika.processor.AbstractTikaProcessor;
import tika.processor.CompositeTikaProcessor;
import tika.processor.CompositeTikaProcessorConfig;


@SpringBootTest(classes = CompositeTikaProcessor.class)
@RunWith(SpringRunner.class)
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

    @After
    public void reset() throws Exception {
        processor.reset();
    }
}

