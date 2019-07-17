package server.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import tika.model.TikaProcessingResult;


@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ServiceResponseContent {

    TikaProcessingResult result;

    // TODO: footer as in NLP
}
