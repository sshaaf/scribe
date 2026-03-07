package dev.shaaf.kantra.rules.gen.validation;

import dev.shaaf.kantra.rules.gen.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RuleValidatorTest {

    private RuleValidator validator;

    @BeforeEach
    void setUp() {
        validator = new RuleValidator();
    }

    @Test
    void testValidYamlRuleParsing() throws Exception {
        String yamlContent = """
            ---
            - ruleID: "java-generic-information-00034"
              description: "org.apache.camel.ThreadPoolRejectedPolicy was moved"
              message: "`org.apache.camel.ThreadPoolRejectedPolicy` was moved to `org.apache.camel.util.concurrent.ThreadPoolRejectedPolicy`."
              category: "mandatory"
              effort: 1
              labels:
                - "konveyor.io/source=camel2"
                - "konveyor.io/source=camel"
                - "konveyor.io/target=camel3+"
                - "konveyor.io/target=camel"
              customVariable: []
              when:
                java.referenced:
                  pattern: "org.apache.camel.ThreadPoolRejectedPolicy"
                  location: "IMPORT"
            """;

        // Test validation
        RuleValidator.ValidationResult result = validator.validateYamlRule(yamlContent);
        assertTrue(result.isValid(), "Rule should be valid. Errors: " + result.errors());

        // Test parsing
        Rule rule = validator.parseYamlToRule(yamlContent);
        assertNotNull(rule);
        assertEquals("java-generic-information-00034", rule.ruleId());
        assertEquals("org.apache.camel.ThreadPoolRejectedPolicy was moved", rule.description());
        assertEquals(Category.MANDATORY, rule.category());
        assertEquals(1, rule.effort());
        assertEquals(4, rule.labels().size());
        assertTrue(rule.labels().contains("konveyor.io/source=camel2"));
        assertNotNull(rule.when());
        assertTrue(rule.when() instanceof JavaReferencedCondition);
        
        JavaReferencedCondition javaCondition = (JavaReferencedCondition) rule.when();
        assertEquals("IMPORT", javaCondition.location());
        assertEquals("org.apache.camel.ThreadPoolRejectedPolicy", javaCondition.pattern());
    }

    @Test
    void testJavaModelToYamlSerialization() throws Exception {
        // Create a rule using Java model
        JavaReferencedCondition whenCondition = new JavaReferencedCondition(
            "org.apache.camel.ThreadPoolRejectedPolicy",
            "IMPORT"
        );
        
        Rule rule = new Rule(
            "java-generic-information-00034",
            "`org.apache.camel.ThreadPoolRejectedPolicy` was moved to `org.apache.camel.util.concurrent.ThreadPoolRejectedPolicy`.",
            "org.apache.camel.ThreadPoolRejectedPolicy was moved",
            Category.MANDATORY,
            1,
            List.of("konveyor.io/source=camel2", "konveyor.io/source=camel", 
                   "konveyor.io/target=camel3+", "konveyor.io/target=camel"),
            null,
            null,
            List.of(),
            whenCondition
        );

        // Serialize to YAML
        String yaml = validator.ruleToYaml(rule);
        assertNotNull(yaml);
        assertTrue(yaml.contains("ruleID: \"java-generic-information-00034\""));
        assertTrue(yaml.contains("category: \"mandatory\""));
        assertTrue(yaml.contains("java.referenced:"));
        
        // Validate the generated YAML
        RuleValidator.ValidationResult result = validator.validateYamlRule(yaml);
        assertTrue(result.isValid(), "Generated YAML should be valid. Errors: " + result.errors());
        
        // Parse it back and verify
        Rule parsedRule = validator.parseYamlToRule(yaml);
        assertEquals(rule.ruleId(), parsedRule.ruleId());
        assertEquals(rule.category(), parsedRule.category());
        assertEquals(rule.effort(), parsedRule.effort());
    }

    @Test
    void testBuiltinFileCondition() throws Exception {
        String yamlContent = """
            ruleID: "builtin-file-test"
            category: "optional"
            effort: 2
            when:
              builtin.file:
                pattern: "*.xml"
            """;

        RuleValidator.ValidationResult result = validator.validateYamlRule(yamlContent);
        assertTrue(result.isValid(), "Rule should be valid. Errors: " + result.errors());

        Rule rule = validator.parseYamlToRule(yamlContent);
        assertNotNull(rule);
        assertTrue(rule.when() instanceof BuiltinFileCondition);
        
        BuiltinFileCondition fileCondition = (BuiltinFileCondition) rule.when();
        assertEquals("*.xml", fileCondition.pattern());
    }

    @Test
    void testAndOrConditions() throws Exception {
        String yamlContent = """
            ruleID: "complex-condition-test"
            category: "mandatory"
            effort: 3
            when:
              and:
                - builtin.file:
                    pattern: "*.java"
                - or:
                    - java.referenced:
                        pattern: "org.apache.camel.*"
                        location: "IMPORT"
                    - java.dependency:
                        name_regex: "camel-.*"
            """;

        RuleValidator.ValidationResult result = validator.validateYamlRule(yamlContent);
        assertTrue(result.isValid(), "Rule should be valid. Errors: " + result.errors());

        Rule rule = validator.parseYamlToRule(yamlContent);
        assertNotNull(rule);
        assertTrue(rule.when() instanceof AndCondition);
        
        AndCondition andCondition = (AndCondition) rule.when();
        assertEquals(2, andCondition.conditions().size());
        assertTrue(andCondition.conditions().get(0) instanceof BuiltinFileCondition);
        assertTrue(andCondition.conditions().get(1) instanceof OrCondition);
    }

    @Test
    void testInvalidYamlSyntax() {
        String invalidYaml = """
            ruleID: "test"
            category: mandatory
            when:
              java.referenced:
                location: IMPORT
                pattern: "test
            """; // Missing closing quote

        RuleValidator.ValidationResult result = validator.validateYamlRule(invalidYaml);
        assertFalse(result.isValid());
        assertTrue(result.errors().get(0).contains("Invalid YAML syntax"));
    }

    @Test
    void testMissingRequiredFields() {
        String yamlContent = """
            description: "Missing ruleID"
            category: "mandatory"
            """;

        RuleValidator.ValidationResult result = validator.validateYamlRule(yamlContent);
        assertFalse(result.isValid());
        assertTrue(result.errors().stream().anyMatch(error -> error.contains("ruleID")));
        assertTrue(result.errors().stream().anyMatch(error -> error.contains("when")));
    }

    @Test
    void testInvalidCategory() {
        String yamlContent = """
            ruleID: "test"
            category: "invalid-category"
            when:
              builtin.file:
                pattern: "*.java"
            """;

        RuleValidator.ValidationResult result = validator.validateYamlRule(yamlContent);
        assertFalse(result.isValid());
        // Should fail during parsing due to invalid enum value
    }

    @Test
    void testInvalidEffortValue() {
        String yamlContent = """
            ruleID: "test"
            category: "mandatory"
            effort: 10
            when:
              builtin.file:
                pattern: "*.java"
            """;

        RuleValidator.ValidationResult result = validator.validateYamlRule(yamlContent);
        assertFalse(result.isValid());
        assertTrue(result.errors().stream().anyMatch(error -> error.contains("effort must be between 1 and 5")));
    }

    @Test
    void testRulesetValidation() throws Exception {
        String rulesetYaml = """
            name: "Test Ruleset"
            description: "A test ruleset"
            rules:
              - ruleID: "rule1"
                category: "mandatory"
                when:
                  builtin.file:
                    pattern: "*.java"
              - ruleID: "rule2"
                category: "optional"
                when:
                  java.referenced:
                    pattern: "org.apache.camel.*"
                    location: "IMPORT"
            """;

        RuleValidator.ValidationResult result = validator.validateYamlRule(rulesetYaml);
        assertTrue(result.isValid(), "Ruleset should be valid. Errors: " + result.errors());
    }

    @Test
    void testMultipleRulesArray() throws Exception {
        String yamlContent = """
            ---
            - ruleID: "rule1"
              category: "mandatory"
              when:
                builtin.file:
                  pattern: "*.java"
            - ruleID: "rule2"
              category: "optional"
              when:
                java.referenced:
                  pattern: "org.apache.camel.*"
                  location: "IMPORT"
            """;

        RuleValidator.ValidationResult result = validator.validateYamlRule(yamlContent);
        assertTrue(result.isValid(), "Rules array should be valid. Errors: " + result.errors());

        List<Rule> rules = validator.parseYamlToRules(yamlContent);
        assertEquals(2, rules.size());
        assertEquals("rule1", rules.get(0).ruleId());
        assertEquals("rule2", rules.get(1).ruleId());
    }

    @Test
    void testJavaDependencyCondition() throws Exception {
        String yamlContent = """
            ruleID: "dependency-test"
            category: "mandatory"
            when:
              java.dependency:
                name: "org.apache.camel:camel-core"
                upperbound: "3.0.0"
            """;

        RuleValidator.ValidationResult result = validator.validateYamlRule(yamlContent);
        assertTrue(result.isValid(), "Rule should be valid. Errors: " + result.errors());

        Rule rule = validator.parseYamlToRule(yamlContent);
        assertTrue(rule.when() instanceof JavaDependencyCondition);
        
        JavaDependencyCondition depCondition = (JavaDependencyCondition) rule.when();
        assertEquals("org.apache.camel:camel-core", depCondition.name());
        assertEquals("3.0.0", depCondition.upperbound());
    }

    @Test
    void testBuiltinXmlCondition() throws Exception {
        String yamlContent = """
            ruleID: "xml-test"
            category: "optional"
            when:
              builtin.xml:
                xpath: "//dependency[artifactId='camel-core']"
                filepaths: ["pom.xml"]
                namespace:
                  maven: "http://maven.apache.org/POM/4.0.0"
            """;

        RuleValidator.ValidationResult result = validator.validateYamlRule(yamlContent);
        assertTrue(result.isValid(), "Rule should be valid. Errors: " + result.errors());

        Rule rule = validator.parseYamlToRule(yamlContent);
        assertTrue(rule.when() instanceof BuiltinXmlCondition);
        
        BuiltinXmlCondition xmlCondition = (BuiltinXmlCondition) rule.when();
        assertEquals("//dependency[artifactId='camel-core']", xmlCondition.xpath());
        assertEquals(1, xmlCondition.filepaths().size());
        assertEquals("pom.xml", xmlCondition.filepaths().get(0));
    }

    @Test
    void testBuiltinFileContentCondition() throws Exception {
        String yamlContent = """
            ruleID: "shell-env-export-001"
            description: "Detects export of environment variables"
            category: "potential"
            effort: 1
            when:
              builtin.filecontent:
                filePattern: "*.sh"
                pattern: "export\\\\s+\\\\w+="
            """;

        RuleValidator.ValidationResult result = validator.validateYamlRule(yamlContent);
        assertTrue(result.isValid(), "Rule should be valid. Errors: " + result.errors());

        Rule rule = validator.parseYamlToRule(yamlContent);
        assertNotNull(rule);
        assertTrue(rule.when() instanceof BuiltinFileContentCondition);

        BuiltinFileContentCondition condition = (BuiltinFileContentCondition) rule.when();
        assertEquals("*.sh", condition.filePattern());
        assertNotNull(condition.pattern());
    }
}
