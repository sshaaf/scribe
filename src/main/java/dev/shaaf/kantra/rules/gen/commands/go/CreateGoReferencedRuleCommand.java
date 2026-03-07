package dev.shaaf.kantra.rules.gen.commands.go;

import com.fasterxml.jackson.databind.JsonNode;
import dev.shaaf.kantra.rules.gen.KantraOperation;
import dev.shaaf.kantra.rules.gen.commands.AbstractCommand;
import dev.shaaf.kantra.rules.gen.commands.RegisteredCommand;
import dev.shaaf.kantra.rules.gen.model.*;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

/**
 * Creates a Kantra rule to detect Go type references using semantic analysis.
 *
 * The go.referenced provider uses the Go language server to detect actual type
 * references, distinguishing them from variables or strings with similar names.
 */
@ApplicationScoped
@RegisteredCommand
public class CreateGoReferencedRuleCommand extends AbstractCommand {

    @Override
    public KantraOperation getOperation() {
        return KantraOperation.CREATE_GO_REFERENCED_RULE;
    }

    @Override
    public String[] getRequiredParams() {
        return new String[]{"ruleID", "pattern", "message", "category", "effort"};
    }

    @Override
    public String getDescription() {
        return "Create a Kantra rule to detect Go type references using semantic analysis. " +
               "The pattern is a regex matched against Go type names. " +
               "Use this to identify usage of deprecated types, replaced APIs, or migration targets. " +
               "For method calls or string literals, use builtin.filecontent instead.";
    }

    @Override
    public String getExampleParams() {
        return """
            {
              "ruleID": "ingress-nginx-go-ref-00001",
              "pattern": "\\\\bIngress\\\\b",
              "message": "This code references `networkingv1.Ingress`. Migrate to `gatewayv1.HTTPRoute`.\\n\\n**Before:**\\n\\n```go\\ntype MyController struct {\\n    ingress *networkingv1.Ingress\\n}\\n```\\n\\n**After:**\\n\\n```go\\ntype MyController struct {\\n    httpRoute *gatewayv1.HTTPRoute\\n}\\n```\\n\\n## Additional info\\n\\n- Gateway API is the successor to Ingress\\n- HTTPRoute provides more flexible routing\\n- See migration guide for full details",
              "description": "Detects Ingress type usage for Gateway API migration",
              "category": "mandatory",
              "effort": 3,
              "labels": [
                "konveyor.io/source=ingress-nginx",
                "konveyor.io/target=gateway-api"
              ],
              "links": [
                {
                  "title": "Kubernetes Gateway API",
                  "url": "https://gateway-api.sigs.k8s.io/"
                }
              ]
            }
            """;
    }

    @Override
    public String execute(JsonNode params) throws Exception {
        String ruleID = requireString(params, "ruleID");
        String pattern = requireString(params, "pattern");
        String message = requireString(params, "message");
        Category category = requireCategory(params, "category");
        int effort = requireInt(params, "effort");

        GoReferencedCondition condition = new GoReferencedCondition(pattern);

        String description = requireString(params, "description",
            "Detects Go type reference: " + pattern);

        Rule rule = new Rule(
            ruleID,
            message,
            description,
            category,
            effort,
            buildLabels(params),
            buildLinks(params),
            List.of(),
            List.of(),
            condition
        );

        return toYaml(rule);
    }
}
