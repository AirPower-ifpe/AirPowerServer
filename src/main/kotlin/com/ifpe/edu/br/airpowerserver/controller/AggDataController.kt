package com.ifpe.edu.br.airpowerserver.controller

import com.ifpe.edu.br.airpowerserver.dto.DeviceAggregatedTelemetry
import com.ifpe.edu.br.airpowerserver.dto.TelemetryAggregationRequest
import com.ifpe.edu.br.airpowerserver.dto.TelemetryAggregationResponse
import com.ifpe.edu.br.airpowerserver.dto.agg.AggDataWrapperResponse
import com.ifpe.edu.br.airpowerserver.dto.agg.AggregationRequest
import com.ifpe.edu.br.airpowerserver.service.AggDataService
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Controller responsável por expor endpoints relacionados à agregação de dados de telemetria.
 * Fornece uma interface para que os clientes solicitem dados consolidados para visualização em gráficos.
 */
@RequestMapping("/api/v1/agg-data/")
@RestController
class AggDataController(
    val aggDataService: AggDataService
) {

    private val logger = LoggerFactory.getLogger(AggDataController::class.java)

    /**
     * Endpoint para obter dados de telemetria agregados para um ou mais dispositivos.
     *
     * Este método recebe uma requisição com os IDs dos dispositivos, a chave de telemetria,
     * a estratégia de agregação e o período desejado. Ele utiliza o [AggDataService]
     * para processar a requisição e retorna uma resposta estruturada pronta para ser
     * consumida por um componente de gráfico no frontend.
     *
     * @param request O objeto [AggregationRequest] contendo os parâmetros para a consulta de agregação.
     * @return Um [ResponseEntity] contendo o [AggDataWrapperResponse] com os dados do gráfico
     * e o valor total agregado. Retorna status 200 (OK) em caso de sucesso.
     * as exceções são tratadas automaticamente em [GlobalApiExceptionHandler]
     */
    @PostMapping("telemetry")
    fun getDeviceAggregatedDataWrapper(
        @Valid @RequestBody request: AggregationRequest
    ): ResponseEntity<AggDataWrapperResponse> {
        logger.info("Received getDeviceAggregatedDataWrapper request: $request")
        return ResponseEntity.ok(aggDataService.getAggDataWrapper(request))
    }

    /**
     * Endpoint para trazer dados agregados por chave e com dados de cada dispositivo
     *
     * * @param request O objeto [TelemetryAggregationRequest] contendo os parâmetros para a consulta de agregação.
     * * @return Um [TelemetryAggregationResponse] contendo uma lista de [DeviceAggregatedTelemetry] com os dados para
     * dispositivo encontrado
     *
     */
    @PostMapping("/telemetry/aggregate")
    fun getAggregatedTelemetry(
        @Valid @RequestBody request: TelemetryAggregationRequest
    ): ResponseEntity<TelemetryAggregationResponse> {
        logger.warn("Received telemetry aggregation request: $request")
        val results = aggDataService.aggregateTelemetry(request)
        return ResponseEntity.ok(TelemetryAggregationResponse(results = results))
    }
}