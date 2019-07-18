package server.controller;

import com.fasterxml.jackson.annotation.JsonView;
import common.JsonPropertyAccessView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import tika.legacy.LegacyPdfProcessorConfig;
import tika.processor.CompositeTikaProcessorConfig;


@Configuration
@ComponentScan({"tika.legacy", "tika.processor"})
public class TikaServerInformation {

    @Autowired
    @JsonView(JsonPropertyAccessView.Public.class)
    LegacyPdfProcessorConfig legacyProcessorConfig;

    @Autowired
    @JsonView(JsonPropertyAccessView.Public.class)
    CompositeTikaProcessorConfig compositeProcesorConfig;
}
