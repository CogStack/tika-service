package tika.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import java.util.Map;


/**
 * Tika processing result payload
 */
@Data
@Builder
//@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TikaProcessingResult {

    // extracted text from the document
    String text;

    // document metadata
    Map<String, Object> metadata;

    // processing status
    Boolean success;

    // the error message in case processing failed
    String error;
}
