package plugin.handler

import org.springframework.web.bind.annotation.RestController
import plugin.model.ControllerModel
import javax.lang.model.element.Element
import javax.lang.model.util.Elements

class RestControllerAnnotationHandler(
    private val elementUtils: Elements
) {
    fun handle(element: Element, controllerModel: ControllerModel) {
        controllerModel.rootResource = element.getAnnotation(RestController::class.java).value
        controllerModel.stubClassName = element.simpleName.toString() + "Stub"
        controllerModel.packageName = elementUtils.getPackageOf(element).qualifiedName.toString()
        controllerModel.stubFullyQualifiedName = controllerModel.packageName + "." + controllerModel.stubClassName
        controllerModel.stubBaseFullyQualifiedName = controllerModel.packageName + ".StubBase"
    }
}