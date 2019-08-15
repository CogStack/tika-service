package common;

/**
 * Implements mechanisms to access only selected members of the class
 * during JSON serialization/deserialization
 */
public class JsonPropertyAccessView {
    public static class Public {}
    public static class Private {}
}
