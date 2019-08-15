package tika.model;

import lombok.Data;

/**
 * A simplified representation of Tika Binary document
 * that can be used as a payload for requests
 */
@Data
public class TikaBinaryDocument {
        byte[] content;
}
