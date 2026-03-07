package dev.shaaf.kantra.rules.gen.validation;

import dev.shaaf.kantra.rules.gen.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for Go provider (go.referenced and go.dependency).
 * Based on real-world examples from Konveyor's Ingress to Gateway API migration.
 */
public class GoProviderTest {

    private RuleValidator validator;

    @BeforeEach
    void setUp() {
        validator = new RuleValidator();
    }

    @Test
    void testGoReferencedCondition() throws Exception {
        String yamlContent = """
            ruleID: "ingress-nginx-go-ref-00001"
            category: "mandatory"
            effort: 3
            when:
              go.referenced:
                pattern: "\\\\bIngress\\\\b"
            """;

        RuleValidator.ValidationResult result = validator.validateYamlRule(yamlContent);
        assertTrue(result.isValid(), "Rule should be valid. Errors: " + result.errors());

        Rule rule = validator.parseYamlToRule(yamlContent);
        assertNotNull(rule);
        assertTrue(rule.when() instanceof GoReferencedCondition);

        GoReferencedCondition condition = (GoReferencedCondition) rule.when();
        assertEquals("\\bIngress\\b", condition.pattern());
    }

    @Test
    void testGoReferencedConditionSimplePattern() throws Exception {
        String yamlContent = """
            ruleID: "ingress-nginx-go-ref-00002"
            category: "mandatory"
            effort: 3
            when:
              go.referenced:
                pattern: "IngressClass"
            """;

        RuleValidator.ValidationResult result = validator.validateYamlRule(yamlContent);
        assertTrue(result.isValid(), "Rule should be valid. Errors: " + result.errors());

        Rule rule = validator.parseYamlToRule(yamlContent);
        GoReferencedCondition condition = (GoReferencedCondition) rule.when();
        assertEquals("IngressClass", condition.pattern());
    }

    @Test
    void testGoReferencedMissingPattern() {
        String yamlContent = """
            ruleID: "test-go-ref"
            category: "mandatory"
            effort: 1
            when:
              go.referenced:
                pattern: ""
            """;

        RuleValidator.ValidationResult result = validator.validateYamlRule(yamlContent);
        assertFalse(result.isValid());
        assertTrue(result.errors().stream()
            .anyMatch(error -> error.contains("pattern")));
    }

    @Test
    void testGoDependencyByName() throws Exception {
        String yamlContent = """
            ruleID: "go-gin-version-001"
            category: "potential"
            effort: 1
            when:
              go.dependency:
                name: "github.com/gin-gonic/gin"
                lowerbound: "1.7.0"
                upperbound: "1.10.0"
            """;

        RuleValidator.ValidationResult result = validator.validateYamlRule(yamlContent);
        assertTrue(result.isValid(), "Rule should be valid. Errors: " + result.errors());

        Rule rule = validator.parseYamlToRule(yamlContent);
        assertTrue(rule.when() instanceof GoDependencyCondition);

        GoDependencyCondition condition = (GoDependencyCondition) rule.when();
        assertEquals("github.com/gin-gonic/gin", condition.name());
        assertEquals("1.7.0", condition.lowerbound());
        assertEquals("1.10.0", condition.upperbound());
    }

    @Test
    void testGoDependencyByRegex() throws Exception {
        String yamlContent = """
            ruleID: "go-gin-modules-001"
            category: "optional"
            effort: 2
            when:
              go.dependency:
                nameregex: "github.com/gin-gonic/.*"
            """;

        RuleValidator.ValidationResult result = validator.validateYamlRule(yamlContent);
        assertTrue(result.isValid(), "Rule should be valid. Errors: " + result.errors());

        Rule rule = validator.parseYamlToRule(yamlContent);
        assertTrue(rule.when() instanceof GoDependencyCondition);

        GoDependencyCondition condition = (GoDependencyCondition) rule.when();
        assertEquals("github.com/gin-gonic/.*", condition.nameRegex());
        assertNull(condition.name());
    }

    @Test
    void testGoDependencyMissingNameAndRegex() {
        String yamlContent = """
            ruleID: "test-go-dep"
            category: "mandatory"
            effort: 1
            when:
              go.dependency:
                lowerbound: "1.0.0"
            """;

        RuleValidator.ValidationResult result = validator.validateYamlRule(yamlContent);
        assertFalse(result.isValid());
        assertTrue(result.errors().stream()
            .anyMatch(error -> error.contains("name") && error.contains("nameregex")));
    }

    @Test
    void testGoReferencedWithFilepaths() throws Exception {
        String yamlContent = """
            ruleID: "go-specific-file-001"
            category: "mandatory"
            effort: 1
            when:
              go.referenced:
                pattern: "unsafe"
                filepaths: ["*.go", "!*_test.go"]
            """;

        RuleValidator.ValidationResult result = validator.validateYamlRule(yamlContent);
        assertTrue(result.isValid(), "Rule should be valid. Errors: " + result.errors());

        Rule rule = validator.parseYamlToRule(yamlContent);
        GoReferencedCondition condition = (GoReferencedCondition) rule.when();
        assertEquals("unsafe", condition.pattern());
        assertNotNull(condition.filepaths());
        assertEquals(2, condition.filepaths().size());
        assertEquals("*.go", condition.filepaths().get(0));
        assertEquals("!*_test.go", condition.filepaths().get(1));
    }

    @Test
    void testComplexGoCondition() throws Exception {
        String yamlContent = """
            ruleID: "go-migration-check-001"
            category: "mandatory"
            effort: 3
            when:
              and:
                - go.referenced:
                    pattern: "Ingress"
                - go.dependency:
                    nameregex: "k8s.io/api/networking/.*"
            """;

        RuleValidator.ValidationResult result = validator.validateYamlRule(yamlContent);
        assertTrue(result.isValid(), "Rule should be valid. Errors: " + result.errors());

        Rule rule = validator.parseYamlToRule(yamlContent);
        assertTrue(rule.when() instanceof AndCondition);

        AndCondition andCondition = (AndCondition) rule.when();
        assertEquals(2, andCondition.conditions().size());
        assertTrue(andCondition.conditions().get(0) instanceof GoReferencedCondition);
        assertTrue(andCondition.conditions().get(1) instanceof GoDependencyCondition);
    }

    @Test
    void testGoModelToYamlSerialization() throws Exception {
        // Create a rule using Java model
        GoReferencedCondition whenCondition = new GoReferencedCondition("\\bIngress\\b");

        Rule rule = new Rule(
            "ingress-nginx-go-ref-00001",
            "Migrate from Ingress to HTTPRoute",
            "Detects Ingress type usage",
            Category.MANDATORY,
            3,
            List.of("konveyor.io/source=ingress-nginx", "konveyor.io/target=gateway-api"),
            null,
            null,
            List.of(),
            whenCondition
        );

        // Serialize to YAML
        String yaml = validator.ruleToYaml(rule);
        assertNotNull(yaml);
        assertTrue(yaml.contains("ruleID: \"ingress-nginx-go-ref-00001\""));
        assertTrue(yaml.contains("category: \"mandatory\""));
        assertTrue(yaml.contains("go.referenced:"));
        assertTrue(yaml.contains("pattern: \"\\\\bIngress\\\\b\""));

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
    void testGoDependencyModelToYamlSerialization() throws Exception {
        // Create a rule using Java model
        GoDependencyCondition whenCondition = new GoDependencyCondition(
            "github.com/gin-gonic/gin", "1.7.0", "1.10.0"
        );

        Rule rule = new Rule(
            "go-gin-version-001",
            "Update Gin framework version",
            "Checks Gin version",
            Category.POTENTIAL,
            2,
            List.of("konveyor.io/source=go"),
            null,
            null,
            List.of(),
            whenCondition
        );

        // Serialize to YAML
        String yaml = validator.ruleToYaml(rule);
        assertNotNull(yaml);
        assertTrue(yaml.contains("go.dependency:"));
        assertTrue(yaml.contains("name: \"github.com/gin-gonic/gin\""));
        assertTrue(yaml.contains("lowerbound: \"1.7.0\""));
        assertTrue(yaml.contains("upperbound: \"1.10.0\""));

        // Validate and parse back
        RuleValidator.ValidationResult result = validator.validateYamlRule(yaml);
        assertTrue(result.isValid(), "Generated YAML should be valid. Errors: " + result.errors());

        Rule parsedRule = validator.parseYamlToRule(yaml);
        assertEquals(rule.ruleId(), parsedRule.ruleId());
        assertTrue(parsedRule.when() instanceof GoDependencyCondition);
    }

    @Test
    void testIngressToGatewayMigrationRule() throws Exception {
        // Real-world example from Konveyor blog
        String yamlContent = """
            ruleID: "ingress-nginx-go-ref-00001"
            description: "Detects Ingress type usage for Gateway API migration"
            message: |
              This code references `networkingv1.Ingress`. Migrate to `gatewayv1.HTTPRoute`.

              **Before:**
              ```go
              type Controller struct {
                  ingress *networkingv1.Ingress
              }
              ```

              **After:**
              ```go
              type Controller struct {
                  httpRoute *gatewayv1.HTTPRoute
              }
              ```
            category: "mandatory"
            effort: 3
            labels:
              - "konveyor.io/source=ingress-nginx"
              - "konveyor.io/target=gateway-api"
            when:
              go.referenced:
                pattern: "\\\\bIngress\\\\b"
            """;

        RuleValidator.ValidationResult result = validator.validateYamlRule(yamlContent);
        assertTrue(result.isValid(), "Real-world rule should be valid. Errors: " + result.errors());

        Rule rule = validator.parseYamlToRule(yamlContent);
        assertNotNull(rule);
        assertEquals("ingress-nginx-go-ref-00001", rule.ruleId());
        assertEquals(Category.MANDATORY, rule.category());
        assertEquals(3, rule.effort());
        assertTrue(rule.labels().contains("konveyor.io/source=ingress-nginx"));
        assertTrue(rule.labels().contains("konveyor.io/target=gateway-api"));
        assertTrue(rule.when() instanceof GoReferencedCondition);
    }
}
