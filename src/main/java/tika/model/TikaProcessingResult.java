package tika.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import java.time.OffsetDateTime;
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

    // when the document was processed
    OffsetDateTime timestamp;
}
