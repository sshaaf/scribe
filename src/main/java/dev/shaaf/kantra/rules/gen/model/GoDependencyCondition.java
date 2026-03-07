package dev.shaaf.kantra.rules.gen.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Condition for detecting Go module dependencies.
 * Maps to `go.dependency` in Kantra rules.
 *
 * Example YAML:
 * <pre>
 * when:
 *   go.dependency:
 *     name: "github.com/gin-gonic/gin"
 *     lowerbound: "1.7.0"
 *     upperbound: "1.9.0"
 * </pre>
 *
 * Either `name` or `nameRegex` must be specified, but not both.
 * Version bounds are optional and follow semantic versioning.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GoDependencyCondition(
    String name,             // Exact dependency name (mutually exclusive with nameRegex)
    @JsonProperty("nameregex") String nameRegex,  // Regex pattern for dependency name
    String upperbound,       // Upper bound version (exclusive)
    String lowerbound,       // Lower bound version (inclusive)
    String as,               // Optional: Store results for condition chaining
    String from,             // Optional: Use results from another condition
    Boolean ignore,          // Optional: Ignore this condition (for chaining)
    Boolean not              // Optional: Negate the condition
) implements Condition {

    /**
     * Constructor for exact name match with version bounds
     */
    public GoDependencyCondition(String name, String lowerbound, String upperbound) {
        this(name, null, upperbound, lowerbound, null, null, null, null);
    }

    /**
     * Constructor for regex pattern match
     */
    public GoDependencyCondition(String nameRegex) {
        this(null, nameRegex, null, null, null, null, null, null);
    }

    /**
     * Simple constructor with only name
     */
    public static GoDependencyCondition byName(String name) {
        return new GoDependencyCondition(name, null, null, null, null, null, null, null);
    }

    /**
     * Simple constructor with only nameRegex
     */
    public static GoDependencyCondition byNameRegex(String nameRegex) {
        return new GoDependencyCondition(null, nameRegex, null, null, null, null, null, null);
    }
}
