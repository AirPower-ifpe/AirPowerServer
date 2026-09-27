package com.ifpe.edu.br.airpowerserver.config

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.core.annotation.Order
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

/**
 * Validador de restrição de versão do ThingsBoard.
 *
 * Como o AirPowerServer consulta diretamente as tabelas internas do ThingsBoard
 * (relation, dashboard, ts_kv, key_dictionary, tb_user), alterações de versão
 * do ThingsBoard podem quebrar o funcionamento ou alterar a semântica dos schemas.
 *
 * Este runner valida no startup se a versão do schema instalada corresponde
 * exatamente à versão homologada (ThingsBoard CE 4.0.1 - schema 4000001).
 */
@Component
@Order(1)
class ThingsBoardVersionValidator(
    @Qualifier("jdbcTemplate") private val jdbcTemplate: JdbcTemplate
) : ApplicationRunner {

    private val logger = LoggerFactory.getLogger(ThingsBoardVersionValidator::class.java)

    @Value("\${thingsboard.version.constraint.schema-version:4000001}")
    private var expectedSchemaVersion: Long = 4000001L

    @Value("\${thingsboard.version.constraint.product:CE}")
    private var expectedProduct: String = "CE"

    @Value("\${thingsboard.version.constraint.enforce:true}")
    private var enforceConstraint: Boolean = true

    override fun run(args: ApplicationArguments?) {
        logger.info("Verificando restrição de versão do ThingsBoard (Esperado: {} schema: {})...", expectedProduct, expectedSchemaVersion)

        val query = "SELECT schema_version, product FROM tb_schema_settings LIMIT 1"
        try {
            val (installedSchemaVersion, installedProduct) = jdbcTemplate.queryForObject(query) { rs, _ ->
                rs.getLong("schema_version") to rs.getString("product")
            } ?: (null to null)

            logger.info("Versão do ThingsBoard detectada no banco: {} (schema: {})", installedProduct, installedSchemaVersion)

            if (installedSchemaVersion != expectedSchemaVersion || !installedProduct.equals(expectedProduct, ignoreCase = true)) {
                val errorMsg = "VIOLAÇÃO DE RESTRIÇÃO DE VERSÃO DO THINGSBOARD: " +
                        "O AirPowerServer requer ThingsBoard $expectedProduct (schema $expectedSchemaVersion), " +
                        "mas foi encontrado: $installedProduct (schema $installedSchemaVersion). " +
                        "Atualizações de versão do ThingsBoard podem quebrar as queries de telemetria e dashboards."

                if (enforceConstraint) {
                    logger.error(errorMsg)
                    throw IllegalStateException(errorMsg)
                } else {
                    logger.warn(errorMsg)
                }
            } else {
                logger.info("Restrição de versão do ThingsBoard validada com sucesso: {} (schema: {})", installedProduct, installedSchemaVersion)
            }
        } catch (e: IllegalStateException) {
            throw e
        } catch (e: Exception) {
            val errorMsg = "Falha ao verificar a versão do ThingsBoard no banco de dados: ${e.message}"
            if (enforceConstraint) {
                logger.error(errorMsg, e)
                throw IllegalStateException(errorMsg, e)
            } else {
                logger.warn(errorMsg)
            }
        }
    }
}
