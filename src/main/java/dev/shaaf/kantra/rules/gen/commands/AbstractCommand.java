package dev.shaaf.kantra.rules.gen.commands;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.shaaf.kantra.rules.gen.model.Category;
import dev.shaaf.kantra.rules.gen.model.JavaLocation;
import dev.shaaf.kantra.rules.gen.model.Link;
import dev.shaaf.kantra.rules.gen.model.Rule;
import dev.shaaf.kantra.rules.gen.validation.RuleValidator;
import io.quarkiverse.mcp.server.ToolCallException;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for commands with common utilities.
 * Extend this class to get helper methods for parameter extraction and YAML serialization.
 */
public abstract class AbstractCommand implements KantraCommand {

    @Inject
    protected ObjectMapper mapper;

    @Inject
    protected RuleValidator ruleValidator;

    /**
     * Safely extract a required string parameter.
     *
     * @param params JSON parameters
     * @param field  Field name to extract
     * @return The string value
     * @throws ToolCallException if the field is missing or null
     */
    protected String requireString(JsonNode params, String field) {
        JsonNode node = params.get(field);
        if (node == null || node.isNull()) {
            throw new ToolCallException("Missing required parameter: " + field);
        }
        return node.asText();
    }

    /**
     * Safely extract an optional string parameter.
     *
     * @param params JSON parameters
     * @param field  Field name to extract
     * @return The string value or null if not present
     */
    protected String optionalString(JsonNode params, String field) {
        return optionalString(params, field, null);
    }

    /**
     * Safely extract an optional string parameter with a default value.
     *
     * @param params       JSON parameters
     * @param field        Field name to extract
     * @param defaultValue Default value if field is missing
     * @return The string value or default
     */
    protected String optionalString(JsonNode params, String field, String defaultValue) {
        JsonNode node = params.get(field);
        return (node == null || node.isNull()) ? defaultValue : node.asText();
    }

    /**
     * Safely extract a required string parameter with a default fallback.
     *
     * @param params       JSON parameters
     * @param field        Field name to extract
     * @param defaultValue Default value if field is missing
     * @return The string value or default
     */
    protected String requireString(JsonNode params, String field, String defaultValue) {
        JsonNode node = params.get(field);
        if (node == null || node.isNull()) {
            return defaultValue;
        }
        return node.asText();
    }

    /**
     * Safely extract an optional integer parameter with a default value.
     *
     * @param params       JSON parameters
     * @param field        Field name to extract
     * @param defaultValue Default value if field is missing
     * @return The integer value or default
     */
    protected int optionalInt(JsonNode params, String field, int defaultValue) {
        JsonNode node = params.get(field);
        return (node == null || node.isNull()) ? defaultValue : node.asInt();
    }

    /**
     * Extract a required integer parameter.
     *
     * @param params JSON parameters
     * @param field  Field name to extract
     * @return The integer value
     * @throws ToolCallException if the field is missing or null
     */
    protected int requireInt(JsonNode params, String field) {
        JsonNode node = params.get(field);
        if (node == null || node.isNull()) {
            throw new ToolCallException("Missing required parameter: " + field);
        }
        return node.asInt();
    }

    /**
     * Extract a Category enum from parameters.
     *
     * @param params JSON parameters
     * @param field  Field name to extract
     * @return The Category enum value
     * @throws ToolCallException if the field is missing or invalid
     */
    protected Category requireCategory(JsonNode params, String field) {
        String value = requireString(params, field).toUpperCase();
        try {
            return Category.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new ToolCallException("Invalid category: " + value + ". Valid values: MANDATORY, OPTIONAL, POTENTIAL");
        }
    }

    /**
     * Extract a JavaLocation enum from parameters.
     *
     * @param params JSON parameters
     * @param field  Field name to extract
     * @return The JavaLocation enum value
     * @throws ToolCallException if the field is missing or invalid
     */
    protected JavaLocation requireJavaLocation(JsonNode params, String field) {
        String value = requireString(params, field).toUpperCase().replace(" ", "_");
        try {
            return JavaLocation.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new ToolCallException("Invalid location: " + value + 
                    ". Valid values: IMPORT, CLASS, METHOD_CALL, CONSTRUCTOR_CALL, ANNOTATION, " +
                    "FIELD, METHOD, INHERITANCE, IMPLEMENTS_TYPE, ENUM, RETURN_TYPE, " +
                    "VARIABLE_DECLARATION, TYPE, PACKAGE");
        }
    }

    /**
     * Convert a Rule to YAML string.
     *
     * @param rule Rule to serialize
     * @return YAML string
     */
    protected String toYaml(Rule rule) {
        try {
            return ruleValidator.ruleToYaml(rule);
        } catch (Exception e) {
            throw new ToolCallException("Failed to serialize rule to YAML: " + e.getMessage());
        }
    }

    /**
     * Serialize result to JSON string.
     *
     * @param result Object to serialize
     * @return JSON string
     * @throws Exception if serialization fails
     */
    protected String toJson(Object result) throws Exception {
        return mapper.writeValueAsString(result);
    }

    /**
     * Build Konveyor-formatted labels from source and target technologies.
     * Labels follow the format: konveyor.io/source=<technology> and konveyor.io/target=<technology>
     *
     * @param params JSON parameters containing optional "source" and "target" fields
     * @return List of properly formatted labels
     */
    protected List<String> buildLabels(JsonNode params) {
        List<String> labels = new ArrayList<>();
        
        String source = optionalString(params, "source", null);
        String target = optionalString(params, "target", null);
        
        if (source != null && !source.isBlank()) {
            labels.add("konveyor.io/source=" + source.toLowerCase().trim());
        }
        if (target != null && !target.isBlank()) {
            labels.add("konveyor.io/target=" + target.toLowerCase().trim());
        }
        
        return labels;
    }

    /**
     * Build links list from optional parameters.
     * Links can be provided as an array of objects with "title" and "url" fields.
     *
     * @param params JSON parameters containing optional "links" array
     * @return List of Link objects
     */
    protected List<Link> buildLinks(JsonNode params) {
        List<Link> links = new ArrayList<>();
        
        JsonNode linksNode = params.get("links");
        if (linksNode != null && linksNode.isArray()) {
            for (JsonNode linkNode : linksNode) {
                String title = linkNode.has("title") ? linkNode.get("title").asText() : null;
                String url = linkNode.has("url") ? linkNode.get("url").asText() : null;
                if (title != null && url != null) {
                    links.add(new Link(title, url));
                }
            }
        }
        
        return links;
    }
}

