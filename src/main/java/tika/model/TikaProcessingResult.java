package tika.model;

import lombok.Builder;
import lombok.Data;

import java.util.Map;


@Data
@Builder
public class TikaProcessingResult {
    String documentContent;
    Map<String, String[]> metadata;
    Boolean success;
    String errorMessage;
}
