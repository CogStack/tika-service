package service.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import tika.model.TikaProcessingResult;

import java.util.List;


/**
 * The response from the service containing the document processing results
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ServiceResponseContent {

    TikaProcessingResult result;
    List<TikaProcessingResult> results;
    // TODO: footer as in NLP
}
