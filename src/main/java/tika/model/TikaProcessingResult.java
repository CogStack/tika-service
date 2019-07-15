package tika.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.Map;


@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TikaProcessingResult {
    String text;
    Map<String, Object> metadata;
    Boolean success;
    String error;
}
