package dev.shaaf.kantra.rules.gen.commands.go;

import com.fasterxml.jackson.databind.JsonNode;
import dev.shaaf.kantra.rules.gen.KantraOperation;
import dev.shaaf.kantra.rules.gen.commands.AbstractCommand;
import dev.shaaf.kantra.rules.gen.commands.RegisteredCommand;
import dev.shaaf.kantra.rules.gen.model.*;
import io.quarkiverse.mcp.server.ToolCallException;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

/**
 * Creates a Kantra rule to detect Go module dependencies.
 *
 * Either 'name' (exact match) or 'nameRegex' (pattern match) must be provided.
 * Version bounds are optional and follow semantic versioning.
 */
@ApplicationScoped
@RegisteredCommand
public class CreateGoDependencyRuleCommand extends AbstractCommand {

    @Override
    public KantraOperation getOperation() {
        return KantraOperation.CREATE_GO_DEPENDENCY_RULE;
    }

    @Override
    public String[] getRequiredParams() {
        return new String[]{"ruleID", "message", "category", "effort"};
    }

    @Override
    public String getDescription() {
        return "Create a Kantra rule to detect Go module dependencies. " +
               "Specify 'name' for exact match or 'nameRegex' for pattern matching. " +
               "Optionally specify version bounds (lowerbound/upperbound). " +
               "At least one of 'name' or 'nameRegex' is required.";
    }

    @Override
    public String getExampleParams() {
        return """
            {
              "ruleID": "go-gin-version-check-001",
              "name": "github.com/gin-gonic/gin",
              "lowerbound": "1.7.0",
              "upperbound": "1.10.0",
              "message": "## Gin version compatibility check\\n\\nYour project uses Gin version between 1.7.0 and 1.10.0. Consider upgrading to latest stable version for security patches.\\n\\n**Update command:**\\n```bash\\ngo get -u github.com/gin-gonic/gin\\n```",
              "description": "Checks Gin framework version for updates",
              "category": "potential",
              "effort": 2,
              "labels": [
                "konveyor.io/source=go"
              ],
              "links": [
                {
                  "title": "Gin Web Framework",
                  "url": "https://github.com/gin-gonic/gin"
                }
              ]
            }
            """;
    }

    @Override
    public String execute(JsonNode params) throws Exception {
        String ruleID = requireString(params, "ruleID");
        String message = requireString(params, "message");
        Category category = requireCategory(params, "category");
        int effort = requireInt(params, "effort");

        // Either name or nameRegex must be provided
        String name = optionalString(params, "name");
        String nameRegex = optionalString(params, "nameRegex");

        if (name == null && nameRegex == null) {
            throw new ToolCallException("Either 'name' or 'nameRegex' must be provided for go.dependency");
        }

        if (name != null && nameRegex != null) {
            throw new ToolCallException("Cannot specify both 'name' and 'nameRegex' for go.dependency. Use one or the other.");
        }

        String lowerbound = optionalString(params, "lowerbound");
        String upperbound = optionalString(params, "upperbound");

        GoDependencyCondition condition = new GoDependencyCondition(
            name, nameRegex, upperbound, lowerbound, null, null, null, null
        );

        String description = optionalString(params, "description");
        if (description == null) {
            description = name != null
                ? "Detects Go dependency: " + name
                : "Detects Go dependencies matching: " + nameRegex;
        }

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
