package dev.shaaf.kantra.rules.gen;

/**
 * Enum defining all available Kantra rule generation operations.
 * Operations are grouped by category for organization.
 */
public enum KantraOperation {
    // Java Rule Operations
    CREATE_JAVA_CLASS_RULE,
    CREATE_JAVA_IMPORT_RULE,
    CREATE_JAVA_ANNOTATION_RULE,
    CREATE_JAVA_METHOD_CALL_RULE,
    CREATE_JAVA_CONSTRUCTOR_CALL_RULE,
    CREATE_JAVA_INHERITANCE_RULE,
    CREATE_JAVA_IMPLEMENTS_RULE,
    CREATE_JAVA_FIELD_RULE,
    CREATE_JAVA_ENUM_RULE,
    CREATE_JAVA_RETURN_TYPE_RULE,
    CREATE_JAVA_VARIABLE_DECLARATION_RULE,
    CREATE_JAVA_TYPE_RULE,
    CREATE_JAVA_PACKAGE_RULE,
    CREATE_JAVA_METHOD_RULE,

    // Go Rule Operations
    CREATE_GO_REFERENCED_RULE,
    CREATE_GO_DEPENDENCY_RULE,

    // File Content Operations
    CREATE_FILE_CONTENT_RULE,
    CREATE_PROPERTIES_RULE,

    // XML Operations
    CREATE_XML_RULE,
    CREATE_XML_PUBLIC_ID_RULE,

    // JSON Operations
    CREATE_JSON_RULE,

    // Built-in File Operations
    CREATE_FILE_RULE,
    CREATE_HAS_TAGS_RULE,

    // Utility Operations
    VALIDATE_RULE,
    GET_HELP,
    COMBINE_WITH_AND,
    COMBINE_WITH_OR,

    // Ruleset Operations
    CREATE_RULESET,
    VALIDATE_RULESET
}

