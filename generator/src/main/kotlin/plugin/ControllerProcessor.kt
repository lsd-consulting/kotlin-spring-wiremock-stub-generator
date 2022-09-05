package plugin

import com.mitchellbosecke.pebble.PebbleEngine
import org.apache.commons.lang3.StringUtils
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import plugin.handler.RestControllerAnnotationHandler
import plugin.model.ControllerModel
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.io.PrintWriter
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
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

//    override fun getSupportedSourceVersion(): SourceVersion {
//        return SourceVersion.RELEASE_11
//    }

    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {

        val kaptKotlinGeneratedDir =
            processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME]
                ?: return false

        val sourcesDir = File(System.getProperty("user.dir") + "/build/generated-stub-sources")
        sourcesDir.mkdir()

        val controllerModel = ControllerModel()
        for (annotation in annotations) {
            roundEnv.getElementsAnnotatedWith(annotation).forEach {
                if (it.getAnnotation(RestController::class.java) != null) {
                    messager.printMessage(Diagnostic.Kind.NOTE, "Processing RestController annotation")
                    restControllerAnnotationHandler.handle(it, controllerModel)
                } else if (it.getAnnotation(GetMapping::class.java) != null) {
                    messager.printMessage(Diagnostic.Kind.NOTE, "Processing GetMapping annotation")
                    val path: Array<String> = it.getAnnotation(GetMapping::class.java).path
                    val value: Array<String> = it.getAnnotation(GetMapping::class.java).value
                    if (path.isNotEmpty()) {
                        controllerModel.subResource = path[0]
                    } else if (value.isNotEmpty()) {
                        controllerModel.subResource = value[0]
                    }
                    controllerModel.methodName = StringUtils.capitalize(it.simpleName.toString())
                    controllerModel.responseType = it.asType().toString().replace("()", "")
                } else {
                    messager.printMessage(Diagnostic.Kind.NOTE, "Unknown annotation")
                }
            }
        }
        if (annotations.isNotEmpty()) {
            messager.printMessage(Diagnostic.Kind.NOTE, "Writing files for model:$controllerModel")
            writeStubFile(kaptKotlinGeneratedDir, controllerModel)
            writeStubBaseFile(kaptKotlinGeneratedDir, controllerModel)
        }
        return true
    }

    private fun writeStubBaseFile(kaptKotlinGeneratedDir: String, controllerModel: ControllerModel) {
        try {
            val builderFile = FileWriter("$kaptKotlinGeneratedDir/" + controllerModel.stubBaseFullyQualifiedName + ".kt")

//            val builderFile = processingEnv.filer.createSourceFile(controllerModel.stubBaseFullyQualifiedName)
//            val stubBasePathName = builderFile.toUri().path.replace("generated/sources/annotationProcessor/java/main", "generated-stub-sources")
//            val directory = stubBasePathName.replace("StubBase.tpl", "")
//            Files.createDirectories(Path.of(directory))
//            val path = Files.createFile(Path.of(stubBasePathName))
            PrintWriter(builderFile).use { writer ->
                stubBaseTemplate.evaluate(
                    writer, mapOf(
                        "packageName" to controllerModel.packageName
                    )
                )
            }
//            PrintWriter(path.toFile()).use { writer ->
//                stubBaseTemplate.evaluate(
//                    writer, mapOf(
//                        "packageName" to controllerModel.packageName
//                    )
//                )
//            }
        } catch (e: IOException) {
            e.printStackTrace()
            throw e
        }
    }

    private fun writeStubFile(kaptKotlinGeneratedDir: String, controllerModel: ControllerModel) {
//        val builderFile = processingEnv.filer.createSourceFile(controllerModel.stubFullyQualifiedName)

        val builderFile = FileWriter("$kaptKotlinGeneratedDir/" + controllerModel.stubFullyQualifiedName + ".kt")

//        val stubBasePathName = kaptKotlinGeneratedDir.replace("generated/sources/kaptKotlin/main", "generated-stub-sources")
//        val directory = stubBasePathName.replace(controllerModel.stubClassName + ".java", "")
//        Files.createDirectories(Path.of(directory))
//        val path = Files.createFile(Path.of(stubBasePathName))
//        PrintWriter(builderFile.openWriter()).use { writer -> stubTemplate.evaluate(writer, mapOf("model" to controllerModel)) }
        PrintWriter(builderFile).use { writer -> stubTemplate.evaluate(writer, mapOf("model" to controllerModel)) }
    }

    companion object {
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"
    }
}