package {{model.packageName}}

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock.*
import java.lang.String.format
import java.net.URLEncoder
import javax.annotation.processing.Generated

@Generated("com.lsdconsulting.stub.plugin.ControllerProcessor")
class {{model.stubClassName}}(objectMapper: ObjectMapper): StubBase(objectMapper) {

    fun get{{model.methodName}}(response: {{model.responseType}} ) {
        buildGet(format({{model.methodName.toUpperCase()}}_URL), OK, buildBody(response));
    }

    fun get{{model.methodName}}(status: Int, response: {{model.responseType}}) {
        buildGet(format({{model.methodName.toUpperCase()}}_URL), status, buildBody(response));
    }

    fun verifyGet{{model.methodName}}() {
        verifyGet{{model.methodName}}(ONCE);
    }

    fun verifyGet{{model.methodName}}(times: Int) {
        verify(times, getRequestedFor(urlEqualTo(format({{model.methodName.toUpperCase()}}_URL))));
    }

    companion object {
        private const val {{model.methodName.toUpperCase()}}_URL = "{{model.rootResource}}{{model.subResource}}"
        private const val OK = 200
        private const val ONCE = 1
    }
}
