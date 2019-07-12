package tika.processor;

import lombok.Data;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.parser.ocr.TesseractOCRConfig;


@Data
public class TikaProcessorConfig {
    private TikaConfig tikaConfig;
    private TesseractOCRConfig tesseractConfig;

    // TODO: externalize the parameters
    /*
    public TikaProcessorConfig() throws IOException, SAXException, TikaException  {
        tikaConfig = new TikaConfig(this.getClass().getClassLoader()
                .getResourceAsStream("tika-config.xml"));
    }
    */

    public TikaProcessorConfig() throws Exception {
        tikaConfig = new TikaConfig();
        tesseractConfig = new TesseractOCRConfig();
    }

    public TikaProcessorConfig(TikaConfig tikaConfig, TesseractOCRConfig tesseractConfig) {
        this.tikaConfig = tikaConfig;
        this.tesseractConfig = tesseractConfig;
    }
}
