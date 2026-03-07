package dev.shaaf.kantra.rules.gen.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Condition for detecting Go type references using semantic analysis.
 * Maps to `go.referenced` in Kantra rules.
 *
 * The Konveyor Go provider uses the Go language server to detect actual type
 * references, distinguishing them from variables or strings with similar names.
 * This is semantic analysis, not simple text matching.
 *
 * Example YAML (from Ingress to Gateway API migration):
 * <pre>
 * when:
 *   go.referenced:
 *     pattern: '\bIngress\b'
 * </pre>
 *
 * Real-world patterns:
 * <ul>
 *   <li>'\bIngress\b' - Matches networkingv1.Ingress type (word boundary)</li>
 *   <li>'IngressClass' - Matches IngressClass type</li>
 *   <li>'.*Route$' - Matches any type ending with Route</li>
 * </ul>
 *
 * NOTE: For method calls, function names, or string literals in Go code,
 * use builtin.filecontent with filePattern: "*.go" instead.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GoReferencedCondition(
    String pattern,          // REQUIRED: Regex pattern to match Go type names (semantic type detection)
    List<String> filepaths,  // Optional: Limit search to specific files (rarely used)
    String as,               // Optional: Store results for condition chaining
    String from,             // Optional: Use results from another condition
    Boolean ignore,          // Optional: Ignore this condition (for chaining)
    Boolean not              // Optional: Negate the condition
) implements Condition {

    /**
     * Simple constructor with only required field
     */
    public GoReferencedCondition(String pattern) {
        this(pattern, null, null, null, null, null);
    }

    /**
     * Constructor with pattern and filepaths
     */
    public GoReferencedCondition(String pattern, List<String> filepaths) {
        this(pattern, filepaths, null, null, null, null);
    }
}
