package tika.cogstack.legacy;

import lombok.Data;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.exception.TikaException;
import org.apache.tika.parser.ocr.TesseractOCRConfig;
import org.xml.sax.SAXException;

import java.io.IOException;


@Data
public class PdfProcessorConfig {
    private TikaConfig tikaConfig;
    private ImageMagickConfig imgConfig;
    private TesseractOCRConfig tesseractConfig;

    // TODO: externalize the parameters

    public PdfProcessorConfig() throws IOException, SAXException, TikaException  {
        tikaConfig = new TikaConfig(this.getClass().getClassLoader()
                .getResourceAsStream("legacy-tika-config.xml"));
    }

}
