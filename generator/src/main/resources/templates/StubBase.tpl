package {{packageName}}

import com.fasterxml.jackson.databind.ObjectMapper

import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock.*

open class StubBase(objectMapper: ObjectMapper) {
    private val objectMapper: ObjectMapper

    init {
        this.objectMapper = objectMapper
    }

    fun buildGet(requestUrl: String?, status: Int, response: String) {
        stub(status, response, get(urlEqualTo(requestUrl)))
    }

    private fun stub(status: Int, response: String, mappingBuilder: MappingBuilder) {
        stubFor(
            mappingBuilder
                .willReturn(
                    aResponse()
                        .withStatus(status)
                        .withBody(response)
                        .withHeader(CONTENT_TYPE_HEADER_NAME, CONTENT_TYPE_HEADER_VALUE)
                )
        )
    }

    fun buildBody(`object`: Any?): String {
        return try {
            objectMapper.writeValueAsString(`object`)
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    companion object {
        private const val CONTENT_TYPE_HEADER_NAME = "Content-Type"
        private const val CONTENT_TYPE_HEADER_VALUE = "application/json; charset=utf-8"
    }
}