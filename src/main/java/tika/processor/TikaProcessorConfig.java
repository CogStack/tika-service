package tika.processor;


import lombok.Data;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.exception.TikaException;
import org.apache.tika.parser.ocr.TesseractOCRConfig;
import org.xml.sax.SAXException;
import tika.parser.ImageMagickConfig;

import javax.annotation.PostConstruct;
import java.io.IOException;


@Data
public class TikaProcessorConfig {
    private TikaConfig tikaConfig;
    private ImageMagickConfig imgConfig;
    private TesseractOCRConfig tesseractConfig;

    // TODO: externalize the parameters

    @PostConstruct
    void init() throws IOException, SAXException, TikaException  {
        tikaConfig = new TikaConfig(this.getClass().getClassLoader()
                .getResourceAsStream("tika-config.xml"));
    }

}
