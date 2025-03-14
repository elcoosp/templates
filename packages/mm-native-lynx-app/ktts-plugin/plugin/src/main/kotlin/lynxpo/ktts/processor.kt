package lynxpo.ktts.processor

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import java.io.OutputStreamWriter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import lynxpo.ktts.annotations.TsRetInto
import lynxpo.ktts.annotations.Typed
import lynxpo.ktts.model.ClassInfo
import lynxpo.ktts.model.EnumValueInfo
import lynxpo.ktts.model.MethodInfo
import lynxpo.ktts.model.ParameterInfo
import lynxpo.ktts.model.SerializableTypeInfo
import lynxpo.ktts.model.TypeInfo

class KttsPlugin(
        private val codeGenerator: CodeGenerator,
        private val logger: KSPLogger,
        private val options: Map<String, String>
) : SymbolProcessor {

    private val prettyJson = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    // Keep track of processed serializable types to avoid duplicates
    private val processedSerializableTypes = mutableSetOf<String>()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        // Find all classes with the @Typed annotation
        val typedClasses =
                resolver.getSymbolsWithAnnotation(Typed::class.qualifiedName!!)
                        .filterIsInstance<KSClassDeclaration>()
                        .filter { it.validate() }
                        .toList()

        if (typedClasses.isEmpty()) {
            return emptyList()
        }

        // Process each class
        typedClasses.forEach { classDecl -> processClass(classDecl, resolver) }

        return emptyList()
    }

    private fun processClass(classDecl: KSClassDeclaration, resolver: Resolver) {

        // Collect all nested serializable types
        val serializableTypes = mutableListOf<SerializableTypeInfo>()
        collectSerializableTypes(classDecl, serializableTypes, resolver)
        val simpleNameToQualified =
                serializableTypes.associate {
                    it.simpleName to it.qualifiedName
                } // Get all methods with @LynxMethod annotation
        val methodsInfo =
                classDecl
                        .getAllFunctions()
                        .filter {
                            it.annotations.any { ann -> ann.shortName.asString() == "LynxMethod" }
                        }
                        .map { processFunctionDeclaration(it, resolver, simpleNameToQualified) }
                        .toList()

        // Build class info object
        val classInfo =
                ClassInfo(
                        qualifiedName = classDecl.qualifiedName?.asString() ?: "",
                        simpleName = classDecl.simpleName.asString(),
                        methods = methodsInfo,
                        lynxType = "", // No longer extracting the value from the annotation
                        serializableTypes = serializableTypes // Add collected serializable types
                )

        // Generate metadata file
        val packageName = classDecl.packageName.asString()
        val fileName = "${classDecl.simpleName.asString()}TypeInfo"

        codeGenerator.createNewFile(
                        Dependencies(false, classDecl.containingFile!!),
                        packageName,
                        fileName,
                        "json"
                )
                .use { output ->
                    OutputStreamWriter(output).use { writer ->
                        writer.write(prettyJson.encodeToString(classInfo))
                    }
                }

        // Generate a Kotlin companion file that allows accessing this information at compile time
        generateTypeCompanion(classInfo, packageName)
    }

    private fun collectSerializableTypes(
            classDecl: KSClassDeclaration,
            serializableTypes: MutableList<SerializableTypeInfo>,
            resolver: Resolver
    ) {
        // Process nested declarations first
        classDecl.declarations.forEach { declaration ->
            if (declaration is KSClassDeclaration) {
                collectSerializableTypes(declaration, serializableTypes, resolver)
            }
        }

        // Check if this class is serializable
        val qualifiedName = classDecl.qualifiedName?.asString() ?: return
        if (processedSerializableTypes.contains(qualifiedName)) {
            return // Already processed this type
        }

        val isSerializable =
                classDecl.annotations.any {
                    val shortName = it.shortName.asString()
                    val qualifiedName =
                            it.annotationType.resolve().declaration.qualifiedName?.asString()
                    shortName == "Serializable" ||
                            qualifiedName == "kotlinx.serialization.Serializable"
                }

        if (isSerializable) {
            processedSerializableTypes.add(qualifiedName)

            // Create type info for this serializable class
            val serializableType =
                    when {
                        classDecl.classKind == ClassKind.ENUM_CLASS -> {
                            // For enums, collect all enum entries and their properties
                            val enumValues =
                                    classDecl
                                            .declarations
                                            .filterIsInstance<KSClassDeclaration>()
                                            .filter { it.classKind == ClassKind.ENUM_ENTRY }
                                            .mapIndexed { index, enumEntry ->
                                                // Get the primary constructor properties
                                                val primaryConstructor =
                                                        classDecl.primaryConstructor
                                                val properties =
                                                        if (primaryConstructor != null) {
                                                            primaryConstructor.parameters.map {
                                                                    param ->
                                                                val name =
                                                                        param.name?.asString() ?: ""
                                                                val value = index.toString() // Use
                                                                // index as
                                                                // value
                                                                EnumValueInfo.Property(name, value)
                                                            }
                                                        } else {
                                                            emptyList()
                                                        }

                                                EnumValueInfo(
                                                        name = enumEntry.simpleName.asString(),
                                                        ordinal = index,
                                                        properties = properties
                                                )
                                            }
                                            .toList()

                            SerializableTypeInfo(
                                    qualifiedName = qualifiedName,
                                    simpleName = classDecl.simpleName.asString(),
                                    kind = "enum",
                                    enumValues = enumValues,
                                    properties = emptyList() // Enums don't have properties in this
                                    // context
                                    )
                        }
                        else -> {
                            // For normal classes or data classes, collect all properties
                            val properties =
                                    classDecl
                                            .getAllProperties()
                                            .map { prop ->
                                                val propType = processTypeReference(prop.type)
                                                SerializableTypeInfo.Property(
                                                        name = prop.simpleName.asString(),
                                                        type = propType,
                                                        isNullable = propType.isNullable,
                                                        hasDefaultValue =
                                                                false // We can't easily determine
                                                        // this, might require more
                                                        // analysis
                                                        )
                                            }
                                            .toList()

                            SerializableTypeInfo(
                                    qualifiedName = qualifiedName,
                                    simpleName = classDecl.simpleName.asString(),
                                    kind =
                                            when (classDecl.classKind) {
                                                ClassKind.CLASS -> "class"
                                                ClassKind.INTERFACE -> "interface"
                                                ClassKind.OBJECT -> "object"
                                                else -> "class" // Default
                                            },
                                    properties = properties,
                                    enumValues = emptyList() // Not an enum
                            )
                        }
                    }

            serializableTypes.add(serializableType)

            // Now recursively process property types that might be serializable
            if (serializableType.kind != "enum") {
                for (property in serializableType.properties) {
                    processPropertyTypeForSerializable(property.type, serializableTypes, resolver)
                }
            }
        }
    }

    private fun processPropertyTypeForSerializable(
            typeInfo: TypeInfo,
            serializableTypes: MutableList<SerializableTypeInfo>,
            resolver: Resolver
    ) {
        // Skip primitive types and already processed types
        if (typeInfo.qualifiedName.startsWith("kotlin.") ||
                        processedSerializableTypes.contains(typeInfo.qualifiedName)
        ) {
            return
        }

        // Look up the type declaration
        val declaration =
                resolver.getClassDeclarationByName(
                        resolver.getKSNameFromString(typeInfo.qualifiedName)
                )
                        ?: return

        // Process this type
        collectSerializableTypes(declaration, serializableTypes, resolver)

        // Also process any generic type arguments
        for (typeArg in typeInfo.typeArguments) {
            processPropertyTypeForSerializable(typeArg, serializableTypes, resolver)
        }
    }

    private fun generateTypeCompanion(classInfo: ClassInfo, packageName: String) {
        val fileName = "${classInfo.simpleName}TypeCompanion"

        codeGenerator.createNewFile(Dependencies(false), packageName, fileName).use { output ->
            OutputStreamWriter(output).use { writer ->
                writer.write(
                        """
                    |package $packageName
                    |
                    |/**
                    | * Compile-time type information for ${classInfo.simpleName}
                    | * Generated by KttsPlugin
                    | */
                    |@Suppress("unused")
                    |object ${classInfo.simpleName}TypeCompanion {
                    |
                    |    object Methods {
                    |${classInfo.methods.joinToString("\n") { method ->
                        """        /**
                        | * ${method.name} type information
                        | * Return type: ${typeInfoToString(method.returnType)}
                        | * Parameters: ${method.parameters.joinToString(", ") { "${it.name}: ${typeInfoToString(it.type)}" }}
                        | */
                        |        const val ${method.name.uppercase()} = "${method.name}"""".trimMargin()
                    }}
                    |    }
                    |
                    |    object Types {
                    |${classInfo.methods.joinToString("\n") { method ->
                        """        const val ${method.name.uppercase()}_RETURN = "${typeInfoToString(method.returnType)}"""".trimMargin()
                    }}
                    |
                    |${if (classInfo.serializableTypes.isNotEmpty()) generateSerializableCompanion(classInfo.serializableTypes) else ""}
                    |    }
                    |}
                    |""".trimMargin()
                )
            }
        }
    }

    private fun generateSerializableCompanion(types: List<SerializableTypeInfo>): String {
        val sb = StringBuilder()
        sb.append("        object Serializable {\n")

        // Generate constants for all serializable types
        for (type in types) {
            sb.append(
                    """
            |            /**
            | * ${type.simpleName} type information
            | * Kind: ${type.kind}
            | * Qualified name: ${type.qualifiedName}
            | */
            |            const val ${type.simpleName.uppercase()} = "${type.qualifiedName}"
            |""".trimMargin()
            )

            // For enums, generate additional constants for their values
            if (type.kind == "enum" && type.enumValues.isNotEmpty()) {
                sb.append("\n            object ${type.simpleName} {\n")

                // Generate constants for each enum value
                for (enumValue in type.enumValues) {
                    sb.append(
                            "                const val ${enumValue.name} = \"${enumValue.name}\"\n"
                    )

                    // If the enum has properties, generate constants for them
                    if (enumValue.properties.isNotEmpty()) {
                        for (prop in enumValue.properties) {
                            sb.append(
                                    "                const val ${enumValue.name}_${prop.name.uppercase()} = ${prop.value}\n"
                            )
                        }
                    }
                }

                sb.append("            }\n")
            }
        }

        sb.append("        }\n")
        return sb.toString()
    }

    private fun typeInfoToString(typeInfo: TypeInfo): String {
        val nullableSuffix = if (typeInfo.isNullable) "?" else ""
        val genericPart =
                if (typeInfo.typeArguments.isNotEmpty()) {
                    "<${typeInfo.typeArguments.joinToString(", ") { typeInfoToString(it) }}>"
                } else ""

        return "${typeInfo.qualifiedName}$genericPart$nullableSuffix"
    }

    private fun processFunctionDeclaration(
            funcDecl: KSFunctionDeclaration,
            resolver: Resolver,
            simpleNameToQualified: Map<String, String>
    ): MethodInfo {
        // Get return type information from the declaration
        val declaredReturnTypeInfo = processTypeReference(funcDecl.returnType!!)

        // Analyze the function body to find potential serializable type references
        val actualReturnTypeInfo = findActualReturnType(funcDecl, declaredReturnTypeInfo, resolver)
        // Check for @TsRetInto annotation
        val tsRetIntoAnnotation =
                funcDecl.annotations.firstOrNull { ann ->
                    ann.annotationType.resolve().declaration.qualifiedName?.asString() ==
                            TsRetInto::class.qualifiedName
                }
        val tsRetIntoValue = tsRetIntoAnnotation?.arguments?.firstOrNull()?.value as? String
        // Resolve the annotation value to a qualified name
        val resolvedQualifiedName =
                when {
                    tsRetIntoValue == null -> null
                    '.' in tsRetIntoValue -> {
                        // Check if qualified name exists globally
                        if (tsRetIntoValue in processedSerializableTypes) tsRetIntoValue else null
                    }
                    else -> {
                        // Check if simple name exists in the current class's serializable types
                        simpleNameToQualified[tsRetIntoValue]
                    }
                }

        val finalReturnTypeInfo =
                if (tsRetIntoValue != null) {
                    if (resolvedQualifiedName == null) {
                        logger.error(
                                "@JsRetInto on function ${funcDecl.simpleName.asString()} references unknown type '$tsRetIntoValue'",
                                funcDecl
                        )
                    }
                    actualReturnTypeInfo.copy(tsReturnInto = tsRetIntoValue)
                } else {
                    actualReturnTypeInfo
                }

        // Get parameter information
        val parameterInfos =
                funcDecl.parameters.map { param ->
                    ParameterInfo(
                            name = param.name?.asString() ?: "",
                            type = processTypeReference(param.type),
                            hasDefaultValue = param.hasDefault
                    )
                }

        // Get receiver type if it's an extension function
        val receiverTypeInfo = funcDecl.extensionReceiver?.let { processTypeReference(it) }
        return MethodInfo(
                name = funcDecl.simpleName.asString(),
                returnType = finalReturnTypeInfo, // Updated return type
                parameters = parameterInfos,
                receiverType = receiverTypeInfo,
                visibility = funcDecl.getVisibility().name.lowercase(),
                isExtension = funcDecl.extensionReceiver != null,
                isInline = funcDecl.modifiers.contains(Modifier.INLINE),
                isSuspend = funcDecl.modifiers.contains(Modifier.SUSPEND)
        )
    }

    private fun findActualReturnType(
            funcDecl: KSFunctionDeclaration,
            declaredType: TypeInfo,
            resolver: Resolver
    ): TypeInfo {
        val visitor = ReturnTypeVisitor(resolver, processedSerializableTypes)
        funcDecl.accept(visitor, Unit)
        return visitor.foundSerializableType?.let { declaredType.copy(qualifiedName = it) }
                ?: declaredType
    }
    // A visitor class to analyze function bodies for return statements
    private inner class ReturnTypeVisitor(
            private val resolver: Resolver,
            private val serializableTypes: Set<String>
    ) : KSVisitorVoid() {
        var foundSerializableType: String? = null

        override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: Unit) {
            // Directly use the declared return type (KSP cannot inspect function bodies)
            val returnType = function.returnType?.resolve()?.declaration?.qualifiedName?.asString()
            if (returnType != null && returnType in serializableTypes) {
                foundSerializableType = returnType
            }
        }

        override fun visitNode(node: KSNode, data: Unit) {
            // Fallback: Traverse declarations, but avoid statement/expression-level nodes
            if (node is KSDeclaration || node is KSFile) {
                node.accept(this, data)
            }
        }
    }
    private fun processTypeReference(typeRef: KSTypeReference): TypeInfo {
        val resolvedType = typeRef.resolve()
        val declaration = resolvedType.declaration

        return TypeInfo(
                qualifiedName = declaration.qualifiedName?.asString() ?: "kotlin.Any",
                simpleName = declaration.simpleName.asString(),
                isNullable = resolvedType.nullability == Nullability.NULLABLE,
                typeArguments =
                        resolvedType.arguments.map { arg ->
                            arg.type?.let { processTypeReference(it) }
                                    ?: TypeInfo(
                                            "kotlin.Any",
                                            "Any"
                                    ) // Default for star projections or unresolvable types
                        }
        )
    }

    private fun KSFunctionDeclaration.getVisibility(): Visibility {
        return when {
            modifiers.contains(Modifier.PUBLIC) -> Visibility.PUBLIC
            modifiers.contains(Modifier.PRIVATE) -> Visibility.PRIVATE
            modifiers.contains(Modifier.PROTECTED) -> Visibility.PROTECTED
            modifiers.contains(Modifier.INTERNAL) -> Visibility.INTERNAL
            else -> Visibility.PUBLIC
        }
    }
}

class KttsPluginProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return KttsPlugin(environment.codeGenerator, environment.logger, environment.options)
    }
}
