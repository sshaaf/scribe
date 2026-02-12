package dev.shaaf.kantra.rules.gen;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.shaaf.kantra.rules.gen.commands.CommandRegistry;
import dev.shaaf.kantra.rules.gen.commands.KantraCommand;
import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import io.quarkiverse.mcp.server.ToolCallException;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Unified Kantra Tool implementing the "Parametric Collapse" strategy.
 * <p>
 * This single tool handles all Kantra rule generation operations by routing to the appropriate
 * command based on the operation parameter. Commands are auto-discovered via CDI
 * and can be enabled/disabled via application.properties.
 * </p>
 * <p>
 * Configuration options in application.properties:
 * <ul>
 *   <li>kantra.mcp.commands.enabled - Comma-separated list of enabled commands (whitelist mode)</li>
 *   <li>kantra.mcp.commands.disabled - Comma-separated list of disabled commands</li>
 *   <li>kantra.mcp.commands.enable-all-by-default - Enable all discovered commands (default: true)</li>
 *   <li>kantra.mcp.commands.log-on-startup - Log available commands on startup (default: true)</li>
 * </ul>
 * </p>
 */
@ApplicationScoped
public class KantraTool {

    @Inject
    CommandRegistry registry;

    @Inject
    ObjectMapper mapper;

    /**
     * Single unified tool method that handles all Kantra rule generation operations.
     * Routes to the appropriate command based on the operation parameter.
     *
     * @param operation The type of Kantra operation to perform
     * @param params    JSON string containing the parameters for the operation
     * @return YAML rule string or result message from the operation
     */
    @Tool(description = "Constructs Konveyor Kantra static analysis rules for application migration. capable of synthesizing custom YAML rulesets for discovering specific code patterns." +
            "Use this tool to create granular rules that pinpoint explicitly defined patterns in the codebase." +
            "Creates migration rules for Java, file content, XML, and JSON patterns. " +
            "Pass the operation type and parameters as JSON. " +
            "Java ops: CREATE_JAVA_CLASS_RULE (supports all JavaLocation types: IMPORT, CLASS, METHOD_CALL, CONSTRUCTOR_CALL, ANNOTATION, FIELD, METHOD, INHERITANCE, IMPLEMENTS_TYPE, ENUM, RETURN_TYPE, VARIABLE_DECLARATION, TYPE, PACKAGE); " +
            "File ops: CREATE_FILE_CONTENT_RULE, CREATE_FILE_RULE; " +
            "XML ops: CREATE_XML_RULE; " +
            "JSON ops: CREATE_JSON_RULE; " +
            "Utility ops: VALIDATE_RULE, GET_HELP")
    public String executeKantraOperation(
            @ToolArg(description = "The operation to perform (e.g., CREATE_JAVA_CLASS_RULE, CREATE_FILE_CONTENT_RULE, CREATE_XML_RULE, VALIDATE_RULE, GET_HELP), Be as specific as possible when choosing the operation and the parameters.")
            KantraOperation operation,
            @ToolArg(description = "JSON object containing operation parameters. " +
                    "For CREATE_JAVA_CLASS_RULE: {ruleID, javaPattern, location (IMPORT/CLASS/METHOD_CALL/etc), message (should include before and after sections with code examples and additional tips), category (MANDATORY/OPTIONAL/POTENTIAL), effort (1-5)}. if location is ANNOTATION, you can also provide an annotated condition. " +
                    "For CREATE_FILE_CONTENT_RULE: {ruleID, filePattern, contentPattern, message, category, effort}. " +
                    "For CREATE_XML_RULE: {ruleID, xpath, message, category, effort}. " +
                    "For VALIDATE_RULE: {yamlContent}. " +
                    "For GET_HELP: {topic (java/file/xml/json/operations)}")
            String params) {

        // Check if operation is available (might be disabled via config)
        if (!registry.isAvailable(operation)) {
            throw new ToolCallException(
                    "Operation " + operation + " is not enabled. " +
                    "Available operations: " + registry.getAvailableOperationsString()
            );
        }

        try {
            JsonNode paramsNode = mapper.readTree(params);
            KantraCommand command = registry.getCommand(operation);

            Log.debugf("Executing %s with params: %s", operation, params);

            return command.execute(paramsNode);

        } catch (ToolCallException e) {
            // Re-throw tool call exceptions as-is
            throw e;
        } catch (Exception e) {
            Log.errorf(e, "Failed to execute %s", operation);
            throw new ToolCallException("Failed to execute operation " + operation + ": " + e.getMessage());
        }
    }
}

