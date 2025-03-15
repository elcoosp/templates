package lynxpo.ktts.model

import kotlinx.serialization.Serializable

@Serializable
data class SourceLocation(
        val filePath: String = "",
        val startLine: Int = 0,
        val startColumn: Int = 0,
        val endLine: Int = 0,
        val endColumn: Int = 0
)

@Serializable
data class TypeInfo(
        val fullName: String, // Renamed from qualifiedName
        val name: String, // Renamed from simpleName
        val isNullable: Boolean = false,
        val typeArguments: List<TypeInfo> = emptyList(),
        val customReturnHint: String? = null, // Generalized from tsReturnInto
        val doc: String = "", // Added documentation field
        val location: SourceLocation = SourceLocation() // Added location information
)

@Serializable
data class ParameterInfo(
        val name: String,
        val type: TypeInfo,
        val hasDefaultValue: Boolean = false,
        val doc: String = "", // Added documentation field
        val location: SourceLocation = SourceLocation() // Added location information
)

@Serializable
data class MethodInfo(
        val name: String,
        val returnType: TypeInfo,
        val parameters: List<ParameterInfo> = emptyList(),
        val receiverType: TypeInfo? =
                null, // Relevant for both Kotlin extensions and Swift extensions
        val visibility: Visibility = Visibility.PUBLIC,
        val isExtension: Boolean = false,
        val isInline: Boolean = false,
        val isAsync: Boolean = false, // Renamed from isSuspend
        val doc: String = "", // Added documentation field
        val location: SourceLocation = SourceLocation() // Added location information
) {
        @Serializable
        enum class Visibility {
                PUBLIC,
                INTERNAL,
                PROTECTED,
                PRIVATE
        }
}

@Serializable
data class EnumValueInfo(
        val name: String,
        val associatedValues: List<AssociatedValue> = emptyList(), // Renamed from properties
        val doc: String = "", // Added documentation field
        val location: SourceLocation = SourceLocation() // Added location information
) {
        @Serializable
        data class AssociatedValue(
                val name: String,
                val type: TypeInfo,
                val doc: String = "", // Added documentation field
                val location: SourceLocation = SourceLocation() // Added location information
        )
}

@Serializable
data class SerializableTypeInfo(
        val fullName: String,
        val name: String,
        val kind: TypeKind,
        val propertyDefinitions: List<PropertyDefinition> =
                emptyList(), // Common property structure
        val enumValues: List<EnumValue> = emptyList(), // Values per enum case
        val doc: String = "", // Added documentation field
        val location: SourceLocation = SourceLocation() // Added location information
) {
        @Serializable
        data class PropertyDefinition(
                val name: String,
                val type: TypeInfo,
                val location: SourceLocation = SourceLocation() // Added location information
        )

        @Serializable
        data class EnumValue(
                val name: String,
                val propertyValues: List<String> =
                        emptyList(), // Ordered values matching propertyDefinitions
                val doc: String = "", // Added documentation field
                val location: SourceLocation = SourceLocation() // Added location information
        )

        @Serializable
        enum class TypeKind {
                CLASS,
                INTERFACE,
                ENUM,
                OBJECT,
                STRUCT
        }
}

@Serializable
data class ClassInfo(
        val fullName: String,
        val name: String,
        val methods: List<MethodInfo> = emptyList(),
        val genericMetadata: String = "", // Generalized from lynxType
        val serializableTypes: List<SerializableTypeInfo> = emptyList(),
        val doc: String = "", // Added documentation field
        val location: SourceLocation = SourceLocation() // Added location information
)
