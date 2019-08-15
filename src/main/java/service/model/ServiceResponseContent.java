package service.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import tika.model.TikaProcessingResult;


/**
 * The response from the service containing the document processing results
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ServiceResponseContent {

    TikaProcessingResult result;

    // TODO: footer as in NLP
}
