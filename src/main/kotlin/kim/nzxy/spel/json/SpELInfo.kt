package kim.nzxy.spel.json

/**
 * @author ly-chn
 * @since 2024/1/5 15:41
 */
data class SpELInfo(val method: SpELInfoMethod, val fields: Map<String, String>)
data class SpELInfoMethod(
    val result: Boolean,
    val resultName: String,
    val parameters: Boolean,
    val parametersPrefix: List<String>
)
