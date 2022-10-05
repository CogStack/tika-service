package tika.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

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

    String resourceId;

    // extracted text from the document
    String text;

    // document metadata
    Map<String, Object> metadata;

    // processing status
    Boolean success;

    // the error message in case processing failed
    String error;

    // when the document was processed
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    OffsetDateTime timestamp;

    // total elapsed time in seconds
    long processingElapsedTime;
}
