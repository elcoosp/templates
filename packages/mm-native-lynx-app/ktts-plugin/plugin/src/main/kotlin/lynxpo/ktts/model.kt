package lynxpo.ktts.model

import kotlinx.serialization.Serializable

@Serializable
data class TypeInfo(
        val qualifiedName: String,
        val simpleName: String,
        val isNullable: Boolean = false,
        val typeArguments: List<TypeInfo> = emptyList()
)

@Serializable
data class ParameterInfo(
        val name: String,
        val type: TypeInfo,
        val hasDefaultValue: Boolean = false
)

@Serializable
data class MethodInfo(
        val name: String,
        val returnType: TypeInfo,
        val parameters: List<ParameterInfo> = emptyList(),
        val receiverType: TypeInfo? = null,
        val visibility: String = "public",
        val isExtension: Boolean = false,
        val isInline: Boolean = false,
        val isSuspend: Boolean = false
)

@Serializable
data class EnumValueInfo(
    val name: String,
    val ordinal: Int,
    val properties: List<Property> = emptyList()
) {
    @Serializable
    data class Property(
        val name: String,
        val value: String
    )
}

@Serializable
data class SerializableTypeInfo(
    val qualifiedName: String,
    val simpleName: String,
    val kind: String, // "class", "interface", "enum", "object", etc.
    val properties: List<Property> = emptyList(),
    val enumValues: List<EnumValueInfo> = emptyList()
) {
    @Serializable
    data class Property(
        val name: String,
        val type: TypeInfo,
        val isNullable: Boolean = false,
        val hasDefaultValue: Boolean = false
    )
}

@Serializable
data class ClassInfo(
        val qualifiedName: String,
        val simpleName: String,
        val methods: List<MethodInfo> = emptyList(),
        val lynxType: String = "",
        val serializableTypes: List<SerializableTypeInfo> = emptyList()
)