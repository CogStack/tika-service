package tika.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import common.JsonPropertyAccessView;
import lombok.Data;
import org.apache.tika.Tika;

/**
 * A helper class providing information about the implementation details of used Tika package
 */
@Data
@JsonIgnoreProperties(value={"specification_version", "implementation_version"}, allowGetters=true)
public class TikaPackageInformation {

    @JsonProperty("specification_version")
    @JsonView(JsonPropertyAccessView.Public.class)
    String getTikaSpecificationVersion() {
        return Tika.class.getPackage().getSpecificationVersion();
    }

    @JsonProperty("implementation_version")
    @JsonView(JsonPropertyAccessView.Public.class)
    final String getTikaImplementationVersion() {
        return Tika.class.getPackage().getImplementationVersion();
    }
}
