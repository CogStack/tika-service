package service.controller;

import com.fasterxml.jackson.annotation.JsonView;
import common.JsonPropertyAccessView;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;


/**
 * A general Tika Service processing configuration
 */
@Data
@Configuration
public class TikaServiceConfig {

    @JsonView(JsonPropertyAccessView.Public.class)
    @Value("${application.version}")
    String appVersion;

    // specifies whether to use the legacy Tika processor (as in CogStack-Pipeline)
    // as the default documents processor
    @JsonView(JsonPropertyAccessView.Public.class)
    @Value("${processing.use-legacy-tika-processor-as-default:true}")
    boolean useLegacyTikaProcessor;

    // specifies whether providing an empty file shall result in reporting failure
    // due to invalid input provided by the client
    @JsonView(JsonPropertyAccessView.Public.class)
    @Value("${processing.fail-on-empty-files:true}")
    boolean failOnEmptyFiles;

    // specifies whether providing a non-document type of data (e.g. executable) should fail
    // due to invalid input provided by the client
    @JsonView(JsonPropertyAccessView.Public.class)
    @Value("${processing.fail-on-non-document-types:true}")
    boolean failOnNonDocumentTypes;

}
