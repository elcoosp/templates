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
        // Extract class documentation
        val classDoc = extractDocumentation(classDecl)

        // Collect all nested serializable types
        val serializableTypes = mutableListOf<SerializableTypeInfo>()
        collectSerializableTypes(classDecl, serializableTypes, resolver)
        val simpleNameToQualified =
                serializableTypes.associate {
                    it.name to it.fullName
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
                        fullName = classDecl.qualifiedName?.asString() ?: "",
                        name = classDecl.simpleName.asString(),
                        methods = methodsInfo,
                        genericMetadata = "", // No longer extracting the value from the annotation
                        serializableTypes = serializableTypes, // Add collected serializable types
                        doc = classDoc // Add class documentation
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
    }

    private fun extractDocumentation(element: KSClassDeclaration): String {
        return element.docString?.trim() ?: ""
    }
    private fun extractDocumentation(element: KSAnnotated): String {
        return ""
    }
    private fun extractDocumentation(element: KSPropertyDeclaration): String {
        return element.docString?.trim() ?: ""
    }

    private fun extractDocumentation(_element: KSValueParameter): String {
        return ""
    }
    private fun extractDocumentation(element: KSFunctionDeclaration): String {
        return element.docString?.trim() ?: ""
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

            // Extract class documentation
            val classDoc = extractDocumentation(classDecl)

            // Create type info for this serializable class
            val serializableType =
                    when {
                        classDecl.classKind == ClassKind.ENUM_CLASS -> {
                            // For enums, collect all enum entries and their properties
                            val enumEntries =
                                    classDecl
                                            .declarations
                                            .filterIsInstance<KSClassDeclaration>()
                                            .filter { it.classKind == ClassKind.ENUM_ENTRY }
                                            .toList()

                            // Get the primary constructor properties for the enum class
                            val primaryConstructor = classDecl.primaryConstructor
                            val propertyDefinitions =
                                    if (primaryConstructor != null) {
                                        primaryConstructor.parameters.map { param ->
                                            SerializableTypeInfo.PropertyDefinition(
                                                    name = param.name?.asString() ?: "",
                                                    type = processTypeReference(param.type),
                                            )
                                        }
                                    } else {
                                        emptyList()
                                    }

                            // Collect enum values with their property values
                            val enumValues =
                                    enumEntries.map { enumEntry ->
                                        val propertyValues =
                                                if (primaryConstructor != null) {
                                                    // Extract the actual values passed to
                                                    // constructor
                                                    primaryConstructor.parameters.mapIndexed {
                                                            index,
                                                            _ ->
                                                        // For simple enums like in the example,
                                                        // we'll use the ordinal values
                                                        // In a real implementation, you'd need to
                                                        // parse the enum entry's constructor
                                                        // arguments
                                                        val enumOrdinal =
                                                                enumEntries.indexOf(enumEntry)
                                                        enumOrdinal.toString()
                                                    }
                                                } else {
                                                    emptyList()
                                                }

                                        SerializableTypeInfo.EnumValue(
                                                name = enumEntry.simpleName.asString(),
                                                propertyValues = propertyValues,
                                                doc =
                                                        extractDocumentation(
                                                                enumEntry
                                                        ) // Add enum value documentation
                                        )
                                    }

                            SerializableTypeInfo(
                                    fullName = qualifiedName,
                                    name = classDecl.simpleName.asString(),
                                    kind = SerializableTypeInfo.TypeKind.ENUM,
                                    propertyDefinitions = propertyDefinitions,
                                    enumValues = enumValues,
                                    doc = classDoc // Add enum class documentation
                            )
                        }
                        else -> {
                            // For normal classes or data classes, collect all properties
                            val properties =
                                    classDecl
                                            .getAllProperties()
                                            .map { prop ->
                                                val propType = processTypeReference(prop.type)
                                                SerializableTypeInfo.PropertyDefinition(
                                                        name = prop.simpleName.asString(),
                                                        type = propType,
                                                )
                                            }
                                            .toList()

                            val kind =
                                    when (classDecl.classKind) {
                                        ClassKind.CLASS -> SerializableTypeInfo.TypeKind.CLASS
                                        ClassKind.INTERFACE ->
                                                SerializableTypeInfo.TypeKind.INTERFACE
                                        ClassKind.OBJECT -> SerializableTypeInfo.TypeKind.OBJECT
                                        else -> SerializableTypeInfo.TypeKind.CLASS // Default
                                    }

                            SerializableTypeInfo(
                                    fullName = qualifiedName,
                                    name = classDecl.simpleName.asString(),
                                    kind = kind,
                                    propertyDefinitions = properties,
                                    enumValues = emptyList(), // Not an enum
                                    doc = classDoc // Add class documentation
                            )
                        }
                    }

            serializableTypes.add(serializableType)

            // Now recursively process property types that might be serializable
            if (serializableType.kind != SerializableTypeInfo.TypeKind.ENUM) {
                for (property in serializableType.propertyDefinitions) {
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
        if (typeInfo.fullName.startsWith("kotlin.") ||
                        processedSerializableTypes.contains(typeInfo.fullName)
        ) {
            return
        }

        // Look up the type declaration
        val declaration =
                resolver.getClassDeclarationByName(resolver.getKSNameFromString(typeInfo.fullName))
                        ?: return

        // Process this type
        collectSerializableTypes(declaration, serializableTypes, resolver)

        // Also process any generic type arguments
        for (typeArg in typeInfo.typeArguments) {
            processPropertyTypeForSerializable(typeArg, serializableTypes, resolver)
        }
    }

    private fun typeInfoToString(typeInfo: TypeInfo): String {
        val nullableSuffix = if (typeInfo.isNullable) "?" else ""
        val genericPart =
                if (typeInfo.typeArguments.isNotEmpty()) {
                    "<${typeInfo.typeArguments.joinToString(", ") { typeInfoToString(it) }}>"
                } else ""

        return "${typeInfo.fullName}$genericPart$nullableSuffix"
    }

    private fun processFunctionDeclaration(
            funcDecl: KSFunctionDeclaration,
            resolver: Resolver,
            simpleNameToQualified: Map<String, String>
    ): MethodInfo {
        // Extract function documentation
        val funcDoc = extractDocumentation(funcDecl)

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
                                "@TsRetInto on function ${funcDecl.simpleName.asString()} references unknown type '$tsRetIntoValue'",
                                funcDecl
                        )
                    }
                    actualReturnTypeInfo.copy(customReturnHint = tsRetIntoValue)
                } else {
                    actualReturnTypeInfo
                }

        // Get parameter information
        val parameterInfos =
                funcDecl.parameters.map { param ->
                    ParameterInfo(
                            name = param.name?.asString() ?: "",
                            type = processTypeReference(param.type),
                            hasDefaultValue = param.hasDefault,
                            doc = extractDocumentation(param) // Add parameter documentation
                    )
                }

        // Get receiver type if it's an extension function
        val receiverTypeInfo = funcDecl.extensionReceiver?.let { processTypeReference(it) }

        val visibility =
                when (funcDecl.getVisibility()) {
                    Visibility.PUBLIC -> MethodInfo.Visibility.PUBLIC
                    Visibility.PRIVATE -> MethodInfo.Visibility.PRIVATE
                    Visibility.PROTECTED -> MethodInfo.Visibility.PROTECTED
                    Visibility.INTERNAL -> MethodInfo.Visibility.INTERNAL
                    else -> MethodInfo.Visibility.PUBLIC
                }

        return MethodInfo(
                name = funcDecl.simpleName.asString(),
                returnType = finalReturnTypeInfo, // Updated return type
                parameters = parameterInfos,
                receiverType = receiverTypeInfo,
                visibility = visibility,
                isExtension = funcDecl.extensionReceiver != null,
                isInline = funcDecl.modifiers.contains(Modifier.INLINE),
                isAsync = funcDecl.modifiers.contains(Modifier.SUSPEND),
                doc = funcDoc // Add function documentation
        )
    }

    private fun findActualReturnType(
            funcDecl: KSFunctionDeclaration,
            declaredType: TypeInfo,
            resolver: Resolver
    ): TypeInfo {
        val visitor = ReturnTypeVisitor(resolver, processedSerializableTypes)
        funcDecl.accept(visitor, Unit)
        return visitor.foundSerializableType?.let { declaredType.copy(fullName = it) }
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
                fullName = declaration.qualifiedName?.asString() ?: "kotlin.Any",
                name = declaration.simpleName.asString(),
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
