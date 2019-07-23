package service.model;

import lombok.Data;
import tika.model.TikaBinaryDocument;


@Data
public class ServiceRequestContent {
    TikaBinaryDocument document;

    // TODO: footer as in NLP
}
