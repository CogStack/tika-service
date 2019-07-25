package service.controller;

import com.fasterxml.jackson.annotation.JsonView;
import common.JsonPropertyAccessView;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;


@Data
@Configuration
public class TikaServiceConfig {
    @JsonView(JsonPropertyAccessView.Public.class)
    @Value("${application.version}")
    String appVersion;

    @JsonView(JsonPropertyAccessView.Public.class)
    @Value("${processing.use-legacy-tika-processor-as-default:true}")
    boolean useLegacyTikaProcessor;

    @JsonView(JsonPropertyAccessView.Public.class)
    @Value("${processing.fail-on-empty-documents:true}")
    boolean failOnEmptyDocuments;

    @JsonView(JsonPropertyAccessView.Public.class)
    @Value("${processing.fail-on-non-document-types:true}")
    boolean failOnNonDocumentTypes;
}
