package dev.shaaf.kantra.rules.gen.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import dev.shaaf.kantra.rules.gen.model.*;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class RuleValidator {
    
    private final ObjectMapper yamlMapper;
    
    public RuleValidator() {
        YAMLFactory yamlFactory = new YAMLFactory();
        // Disable YAML type tags to force wrapper object format
        yamlFactory.disable(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID);
        
        this.yamlMapper = new ObjectMapper(yamlFactory);
        this.yamlMapper.findAndRegisterModules();
    }
    private final ObjectMapper jsonMapper = new ObjectMapper();
    
    /**
     * Validates a YAML string containing Kantra rules
     * @param yamlContent The YAML content to validate
     * @return ValidationResult containing success status and any error messages
     */
    public ValidationResult validateYamlRule(String yamlContent) {
        List<String> errors = new ArrayList<>();
        
        // Step 1: Check if it's valid YAML
        JsonNode yamlNode;
        try {
            yamlNode = yamlMapper.readTree(yamlContent);
        } catch (Exception e) {
            errors.add("Invalid YAML syntax: " + e.getMessage());
            return new ValidationResult(false, errors);
        }
        
        // Step 2: Try to parse as Rule or Ruleset according to OpenAPI spec
        try {
            // First try to parse as a ruleset
            if (yamlNode.isObject() && yamlNode.has("rules")) {
                Ruleset ruleset = yamlMapper.treeToValue(yamlNode, Ruleset.class);
                validateRuleset(ruleset, errors);
            }
            // Then try to parse as a single rule (any object that's not a ruleset)
            else if (yamlNode.isObject()) {
                Rule rule = yamlMapper.treeToValue(yamlNode, Rule.class);
                validateRule(rule, errors);
            }
            // Try to parse as an array of rules
            else if (yamlNode.isArray()) {
                for (JsonNode ruleNode : yamlNode) {
                    Rule rule = yamlMapper.treeToValue(ruleNode, Rule.class);
                    validateRule(rule, errors);
                }
            }
            else {
                errors.add("YAML must contain either a single rule (with ruleID), a ruleset (with rules array), or an array of rules");
            }
        } catch (Exception e) {
            errors.add("Failed to parse YAML as valid Kantra rule structure: " + e.getMessage());
        }
        
        return new ValidationResult(errors.isEmpty(), errors);
    }
    
    /**
     * Parses a YAML string into a Rule object
     * @param yamlContent The YAML content to parse
     * @return The parsed Rule object
     * @throws Exception if parsing fails
     */
    public Rule parseYamlToRule(String yamlContent) throws Exception {
        JsonNode yamlNode = yamlMapper.readTree(yamlContent);
        
        if (yamlNode.isArray() && yamlNode.size() == 1) {
            // Handle single-element array
            return yamlMapper.treeToValue(yamlNode.get(0), Rule.class);
        } else if (yamlNode.isObject()) {
            // Handle single object
            return yamlMapper.treeToValue(yamlNode, Rule.class);
        } else {
            throw new IllegalArgumentException("YAML must contain a single rule object or single-element array");
        }
    }
    
    /**
     * Parses a YAML string into a list of Rule objects
     * @param yamlContent The YAML content to parse
     * @return List of parsed Rule objects
     * @throws Exception if parsing fails
     */
    public List<Rule> parseYamlToRules(String yamlContent) throws Exception {
        JsonNode yamlNode = yamlMapper.readTree(yamlContent);
        List<Rule> rules = new ArrayList<>();
        
        if (yamlNode.isArray()) {
            for (JsonNode ruleNode : yamlNode) {
                rules.add(yamlMapper.treeToValue(ruleNode, Rule.class));
            }
        } else if (yamlNode.isObject()) {
            rules.add(yamlMapper.treeToValue(yamlNode, Rule.class));
        } else {
            throw new IllegalArgumentException("YAML must contain rule objects");
        }
        
        return rules;
    }
    
    /**
     * Converts a Rule object to YAML string
     * @param rule The Rule object to serialize
     * @return YAML string representation
     * @throws Exception if serialization fails
     */
    public String ruleToYaml(Rule rule) throws Exception {
        return yamlMapper.writeValueAsString(List.of(rule));
    }
    
    /**
     * Converts a list of Rule objects to YAML string
     * @param rules The list of Rule objects to serialize
     * @return YAML string representation
     * @throws Exception if serialization fails
     */
    public String rulesToYaml(List<Rule> rules) throws Exception {
        return yamlMapper.writeValueAsString(rules);
    }
    
    private void validateRule(Rule rule, List<String> errors) {
        if (rule.ruleId() == null || rule.ruleId().trim().isEmpty()) {
            errors.add("Rule must have a non-empty ruleID");
        }

        if (rule.category() == null) {
            errors.add("Rule must have a category (potential, optional, or mandatory)");
        }

        if (rule.when() == null) {
            errors.add("Rule must have a 'when' condition");
        } else {
            validateCondition(rule.when(), errors);
        }

        if (rule.effort() != null && (rule.effort() < 1 || rule.effort() > 5)) {
            errors.add("Rule effort must be between 1 and 5");
        }
    }

    private void validateCondition(Condition condition, List<String> errors) {
        if (condition instanceof GoReferencedCondition go) {
            if (go.pattern() == null || go.pattern().trim().isEmpty()) {
                errors.add("go.referenced condition must have a non-empty 'pattern'");
            }
        } else if (condition instanceof GoDependencyCondition go) {
            if (go.name() == null && go.nameRegex() == null) {
                errors.add("go.dependency condition must specify either 'name' or 'nameregex'");
            }
            if (go.name() != null && go.nameRegex() != null) {
                errors.add("go.dependency condition cannot specify both 'name' and 'nameregex'");
            }
        } else if (condition instanceof AndCondition and) {
            if (and.conditions() != null) {
                for (Condition subCondition : and.conditions()) {
                    validateCondition(subCondition, errors);
                }
            }
        } else if (condition instanceof OrCondition or) {
            if (or.conditions() != null) {
                for (Condition subCondition : or.conditions()) {
                    validateCondition(subCondition, errors);
                }
            }
        }
    }
    
    private void validateRuleset(Ruleset ruleset, List<String> errors) {
        if (ruleset.rules() == null || ruleset.rules().isEmpty()) {
            errors.add("Ruleset must contain at least one rule");
        } else {
            for (int i = 0; i < ruleset.rules().size(); i++) {
                Rule rule = ruleset.rules().get(i);
                List<String> ruleErrors = new ArrayList<>();
                validateRule(rule, ruleErrors);
                for (String error : ruleErrors) {
                    errors.add("Rule " + i + ": " + error);
                }
            }
        }
    }
    
    public record ValidationResult(
        boolean isValid,
        List<String> errors
    ) {}
}
