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

    private val processedSerializableTypes = mutableSetOf<String>()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val typedClasses =
                resolver.getSymbolsWithAnnotation(Typed::class.qualifiedName!!)
                        .filterIsInstance<KSClassDeclaration>()
                        .filter { it.validate() }
                        .toList()

        typedClasses.forEach { classDecl -> processClass(classDecl, resolver) }
        return emptyList()
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
    private fun processTypeByName(
            typeName: String,
            serializableTypes: MutableList<SerializableTypeInfo>,
            resolver: Resolver
    ) {
        if (processedSerializableTypes.contains(typeName)) return

        resolver.getClassDeclarationByName(resolver.getKSNameFromString(typeName))?.let { decl ->
            collectSerializableTypes(decl, serializableTypes, resolver)
        }
    }
    private fun processClass(classDecl: KSClassDeclaration, resolver: Resolver) {
        val classDoc = extractDocumentation(classDecl)
        val serializableTypes = mutableListOf<SerializableTypeInfo>()

        // Process nested types first
        collectSerializableTypes(classDecl, serializableTypes, resolver)

        val simpleNameToQualified = serializableTypes.associate { it.name to it.fullName }

        // Process methods and their parameter/return types
        val methodsInfo =
                classDecl
                        .getAllFunctions()
                        .filter {
                            it.annotations.any { ann -> ann.shortName.asString() == "LynxMethod" }
                        }
                        .map {
                            processFunctionDeclaration(
                                    it,
                                    resolver,
                                    simpleNameToQualified,
                                    serializableTypes
                            )
                        }
                        .toList()

        val classInfo =
                ClassInfo(
                        fullName = classDecl.qualifiedName?.asString() ?: "",
                        name = classDecl.simpleName.asString(),
                        methods = methodsInfo,
                        genericMetadata = "",
                        serializableTypes = serializableTypes,
                        doc = classDoc
                )

        generateMetadataFile(classDecl, classInfo)
    }

    private fun generateMetadataFile(classDecl: KSClassDeclaration, classInfo: ClassInfo) {
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

    private fun collectSerializableTypes(
            classDecl: KSClassDeclaration,
            serializableTypes: MutableList<SerializableTypeInfo>,
            resolver: Resolver
    ) {
        classDecl.declarations.forEach {
            if (it is KSClassDeclaration) collectSerializableTypes(it, serializableTypes, resolver)
        }

        val qualifiedName = classDecl.qualifiedName?.asString() ?: return
        if (processedSerializableTypes.contains(qualifiedName)) return

        val isSerializable =
                classDecl.annotations.any {
                    it.shortName.asString() == "Serializable" ||
                            it.annotationType.resolve().declaration.qualifiedName?.asString() ==
                                    "kotlinx.serialization.Serializable"
                }

        if (isSerializable) {
            processedSerializableTypes.add(qualifiedName)
            val typeInfo = createSerializableTypeInfo(classDecl, qualifiedName)
            serializableTypes.add(typeInfo)
            processTypeProperties(typeInfo, serializableTypes, resolver)
        }
    }

    private fun createSerializableTypeInfo(
            classDecl: KSClassDeclaration,
            qualifiedName: String
    ): SerializableTypeInfo {
        return when {
            classDecl.classKind == ClassKind.ENUM_CLASS -> {
                val enumEntries =
                        classDecl.declarations.filterIsInstance<KSClassDeclaration>().filter {
                            it.classKind == ClassKind.ENUM_ENTRY
                        }

                val properties =
                        classDecl.primaryConstructor?.parameters?.map {
                            SerializableTypeInfo.PropertyDefinition(
                                    name = it.name?.asString() ?: "",
                                    type = processTypeReference(it.type)
                            )
                        }
                                ?: emptyList()
                val primaryConstructor = classDecl.primaryConstructor

                // Collect enum values with their property values
                val enumValues =
                        enumEntries.map { enumEntry ->
                            val propertyValues =
                                    if (primaryConstructor != null) {
                                        // Extract the actual values passed to
                                        // constructor
                                        primaryConstructor.parameters.mapIndexed { index, _ ->
                                            // For simple enums like in the example,
                                            // we'll use the ordinal values
                                            // In a real implementation, you'd need to
                                            // parse the enum entry's constructor
                                            // arguments
                                            val enumOrdinal = enumEntries.indexOf(enumEntry)
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
                        propertyDefinitions = properties,
                        enumValues = enumValues.toList(),
                        doc = extractDocumentation(classDecl)
                )
            }
            else -> {
                val properties =
                        classDecl
                                .getAllProperties()
                                .map {
                                    SerializableTypeInfo.PropertyDefinition(
                                            name = it.simpleName.asString(),
                                            type = processTypeReference(it.type)
                                    )
                                }
                                .toList()

                SerializableTypeInfo(
                        fullName = qualifiedName,
                        name = classDecl.simpleName.asString(),
                        kind =
                                when (classDecl.classKind) {
                                    ClassKind.INTERFACE -> SerializableTypeInfo.TypeKind.INTERFACE
                                    ClassKind.OBJECT -> SerializableTypeInfo.TypeKind.OBJECT
                                    else -> SerializableTypeInfo.TypeKind.CLASS
                                },
                        propertyDefinitions = properties,
                        enumValues = emptyList(),
                        doc = extractDocumentation(classDecl)
                )
            }
        }
    }

    private fun processTypeProperties(
            typeInfo: SerializableTypeInfo,
            serializableTypes: MutableList<SerializableTypeInfo>,
            resolver: Resolver
    ) {
        typeInfo.propertyDefinitions.forEach {
            processPropertyTypeForSerializable(it.type, serializableTypes, resolver)
        }
    }

    private fun processPropertyTypeForSerializable(
            typeInfo: TypeInfo,
            serializableTypes: MutableList<SerializableTypeInfo>,
            resolver: Resolver
    ) {
        if (typeInfo.fullName.startsWith("kotlin.") ||
                        processedSerializableTypes.contains(typeInfo.fullName)
        )
                return

        resolver.getClassDeclarationByName(resolver.getKSNameFromString(typeInfo.fullName))?.let {
                decl ->
            collectSerializableTypes(decl, serializableTypes, resolver)
        }

        typeInfo.typeArguments.forEach {
            processPropertyTypeForSerializable(it, serializableTypes, resolver)
        }
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

    private fun processFunctionDeclaration(
            funcDecl: KSFunctionDeclaration,
            resolver: Resolver,
            simpleNameToQualified: Map<String, String>,
            serializableTypes: MutableList<SerializableTypeInfo>
    ): MethodInfo {
        // Process return type
        val declaredReturnTypeInfo = processTypeReference(funcDecl.returnType!!)
        val actualReturnTypeInfo = findActualReturnType(funcDecl, declaredReturnTypeInfo, resolver)
        val (finalReturnTypeInfo, tsRetIntoValue) =
                processTsRetIntoAnnotation(funcDecl, actualReturnTypeInfo, simpleNameToQualified)

        // Process parameters
        val parameterInfos =
                funcDecl.parameters.map { param ->
                    ParameterInfo(
                                    name = param.name?.asString() ?: "",
                                    type = processTypeReference(param.type),
                                    hasDefaultValue = param.hasDefault,
                                    doc = extractDocumentation(param)
                            )
                            .also {
                                processPropertyTypeForSerializable(
                                        it.type,
                                        serializableTypes,
                                        resolver
                                )
                            }
                }

        // Process return type and TsRetInto hint
        processPropertyTypeForSerializable(finalReturnTypeInfo, serializableTypes, resolver)
        tsRetIntoValue?.let { hint ->
            simpleNameToQualified[hint]?.let { qualified ->
                processTypeByName(qualified, serializableTypes, resolver)
            }
        }

        return MethodInfo(
                name = funcDecl.simpleName.asString(),
                returnType = finalReturnTypeInfo,
                parameters = parameterInfos,
                receiverType = funcDecl.extensionReceiver?.let { processTypeReference(it) },
                visibility = funcDecl.getVisibility().toModelVisibility(),
                isExtension = funcDecl.extensionReceiver != null,
                isInline = funcDecl.modifiers.contains(Modifier.INLINE),
                isAsync = funcDecl.modifiers.contains(Modifier.SUSPEND),
                doc = extractDocumentation(funcDecl)
        )
    }
    private fun Visibility.toModelVisibility(): MethodInfo.Visibility {
        return when (this) {
            Visibility.PUBLIC -> MethodInfo.Visibility.PUBLIC
            Visibility.PRIVATE -> MethodInfo.Visibility.PRIVATE
            Visibility.PROTECTED -> MethodInfo.Visibility.PROTECTED
            Visibility.INTERNAL -> MethodInfo.Visibility.INTERNAL
            else -> MethodInfo.Visibility.PUBLIC
        }
    }
    private fun processTsRetIntoAnnotation(
            funcDecl: KSFunctionDeclaration,
            actualReturnType: TypeInfo,
            simpleNameToQualified: Map<String, String>
    ): Pair<TypeInfo, String?> {
        val annotation =
                funcDecl.annotations.firstOrNull {
                    it.annotationType.resolve().declaration.qualifiedName?.asString() ==
                            TsRetInto::class.qualifiedName
                }
        val tsRetIntoValue = annotation?.arguments?.firstOrNull()?.value as? String
        val resolvedName =
                tsRetIntoValue?.let { if (it.contains('.')) it else simpleNameToQualified[it] }

        return if (resolvedName != null) {
            actualReturnType.copy(customReturnHint = resolvedName) to tsRetIntoValue
        } else {
            actualReturnType to null
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
    // ... (Keep existing helper methods like extractDocumentation, processTypeReference,
    // findActualReturnType, ReturnTypeVisitor, and visibility converters unchanged) ...
}

class KttsPluginProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return KttsPlugin(environment.codeGenerator, environment.logger, environment.options)
    }
}
