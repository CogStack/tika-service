package tika.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;


@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ServiceResponseContent {

    TikaProcessingResult result;

    // TODO: footer as in NLP
}
