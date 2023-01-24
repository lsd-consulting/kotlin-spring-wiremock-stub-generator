package plugin

import com.mitchellbosecke.pebble.PebbleEngine
import org.apache.commons.lang3.StringUtils.capitalize
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import plugin.handler.RestControllerAnnotationHandler
import plugin.model.ControllerModel
import java.io.IOException
import java.io.PrintWriter
import java.nio.file.Files
import java.nio.file.Path
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic

@SupportedSourceVersion(SourceVersion.RELEASE_17)
class ControllerProcessor : AbstractProcessor() {
    private lateinit var restControllerAnnotationHandler: RestControllerAnnotationHandler
    private lateinit var messager: Messager
    private val engine = PebbleEngine.Builder().build()
    private val stubTemplate = engine.getTemplate("templates/Stub.tpl")
    private val stubBaseTemplate = engine.getTemplate("templates/StubBase.tpl")

    @Synchronized
    override fun init(processingEnv: ProcessingEnvironment) {
        super.init(processingEnv)
        val elementUtils = processingEnv.elementUtils
        messager = processingEnv.messager
        restControllerAnnotationHandler = RestControllerAnnotationHandler(elementUtils)
    }

    override fun getSupportedAnnotationTypes() = mutableSetOf(
        Controller::class.java.canonicalName,
        RestController::class.java.canonicalName,
        GetMapping::class.java.canonicalName,
        PostMapping::class.java.canonicalName,
        ResponseBody::class.java.canonicalName,
        RequestBody::class.java.canonicalName,
        RequestParam::class.java.canonicalName
    )

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.RELEASE_17
    }

    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
        val controllerModel = ControllerModel()

        for (annotation in annotations) {
            val annotatedElements = roundEnv.getElementsAnnotatedWith(annotation)
            annotatedElements.forEach { element: Element? ->
                if (element!!.getAnnotation(RestController::class.java) != null) {
                    messager.printMessage(Diagnostic.Kind.NOTE, "Processing RestController annotation")
                    restControllerAnnotationHandler.handle(element, controllerModel)
                } else if (element.getAnnotation(GetMapping::class.java) != null) {
                    messager.printMessage(Diagnostic.Kind.NOTE, "Processing GetMapping annotation")
                    val path: Array<String> = element.getAnnotation(GetMapping::class.java).path
                    val value: Array<String> = element.getAnnotation(GetMapping::class.java).value
                    if (path.isNotEmpty()) {
                        controllerModel.subResource = path[0]
                    } else if (value.isNotEmpty()) {
                        controllerModel.subResource = value[0]
                    }
                    controllerModel.methodName = capitalize(element.simpleName.toString())

                    controllerModel.responseType = element.asType().toString().replace("()", "")
                } else {
                    messager.printMessage(Diagnostic.Kind.NOTE, "Unknown annotation")
                }
            }
        }

        if (annotations.isNotEmpty()) {
            messager.printMessage(Diagnostic.Kind.NOTE, "Writing files for model:$controllerModel")
            writeStubFile(controllerModel)
            writeStubBaseFile(controllerModel)
        }

        return true
    }

    private fun writeStubBaseFile(controllerModel: ControllerModel) {
        try {
            val builderFile = processingEnv.filer.createSourceFile(controllerModel.stubBaseFullyQualifiedName)
            val stubBasePathName = builderFile.toUri().path.replace(
                "generated/source/kapt/main",
                "generated-stub-sources"
            )
            val directory = stubBasePathName.replace("StubBase.java", "")
            messager.printMessage(Diagnostic.Kind.NOTE, "Creating directory:$directory")
            Files.createDirectories(Path.of(directory))
            messager.printMessage(Diagnostic.Kind.NOTE, "Creating file:$stubBasePathName")
            val path = Files.createFile(Path.of(stubBasePathName))
            PrintWriter(builderFile.openWriter()).use { writer ->
                stubBaseTemplate.evaluate(writer, mapOf("packageName" to controllerModel.packageName))
            }
            PrintWriter(path.toFile()).use { writer ->
                stubBaseTemplate.evaluate(writer, mapOf("packageName" to controllerModel.packageName))
            }
        } catch (e: IOException) {
            e.printStackTrace()
            throw e
        }
    }


    private fun writeStubFile(controllerModel: ControllerModel) {
        try {
            val builderFile = processingEnv.filer.createSourceFile(controllerModel.stubFullyQualifiedName)
            messager.printMessage(Diagnostic.Kind.NOTE, "builderFile:$builderFile")
            messager.printMessage(Diagnostic.Kind.NOTE, "builderFile.toUri().path:${builderFile.toUri().path}")
            val stubBasePathName = builderFile.toUri().path.replace(
                "generated/source/kapt/main",
                "generated-stub-sources"
            )
            messager.printMessage(Diagnostic.Kind.NOTE, "stubBasePathName:$stubBasePathName")
            val directory: String = stubBasePathName.replace(controllerModel.stubClassName + ".java", "")
            messager.printMessage(Diagnostic.Kind.NOTE, "Creating directory:$directory")
            Files.createDirectories(Path.of(directory))
            messager.printMessage(Diagnostic.Kind.NOTE, "Creating file:$stubBasePathName")
            val path = Files.createFile(Path.of(stubBasePathName))
            PrintWriter(builderFile.openWriter()).use { writer ->
                stubTemplate.evaluate(writer, mapOf("model" to controllerModel))
            }
            PrintWriter(path.toFile()).use { writer ->
                stubTemplate.evaluate(writer, mapOf("model" to controllerModel))
            }
        } catch (e: IOException) {
            e.printStackTrace()
            throw e
        }
    }
}