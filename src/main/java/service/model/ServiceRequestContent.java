package service.model;

import lombok.Data;
import tika.model.TikaBinaryDocument;

/**
 * Service request content when used with JSON-accepting endpoints
 *
 * Current status: NOT USED
 *
 * NB: for the moment, documents are sent either as:
 * - ocet stream
 * - multi-part files
 * as encoding binary document content into JSON may be an overkill,
 * but may be revisited when going forward with gRPC
 *
 * [keeping for now as a placeholder]
 */
@Data
public class ServiceRequestContent {
    TikaBinaryDocument document;

    // TODO: footer as in NLP
}
