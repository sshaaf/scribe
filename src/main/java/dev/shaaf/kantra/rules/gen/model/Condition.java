package dev.shaaf.kantra.rules.gen.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.WRAPPER_OBJECT,
        property = "@type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = AndCondition.class, name = "and"),
        @JsonSubTypes.Type(value = OrCondition.class, name = "or"),
        @JsonSubTypes.Type(value = BuiltinFileCondition.class, name = "builtin.file"),
        @JsonSubTypes.Type(value = BuiltinFileContentCondition.class, name = "builtin.filecontent"),
        @JsonSubTypes.Type(value = BuiltinHasTagsCondition.class, name = "builtin.hasTags"),
        @JsonSubTypes.Type(value = BuiltinJsonCondition.class, name = "builtin.json"),
        @JsonSubTypes.Type(value = BuiltinXmlCondition.class, name = "builtin.xml"),
        @JsonSubTypes.Type(value = BuiltinXmlPublicIdCondition.class, name = "builtin.xmlPublicID"),
        @JsonSubTypes.Type(value = JavaReferencedCondition.class, name = "java.referenced"),
        @JsonSubTypes.Type(value = JavaDependencyCondition.class, name = "java.dependency"),
        @JsonSubTypes.Type(value = GoReferencedCondition.class, name = "go.referenced"),
        @JsonSubTypes.Type(value = GoDependencyCondition.class, name = "go.dependency")
})
public interface Condition {
}
