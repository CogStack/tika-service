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
    @Value("${application.use-legacy-tika-processor-as-default:false}")
    boolean useLegacyTikaProcessor;
}
