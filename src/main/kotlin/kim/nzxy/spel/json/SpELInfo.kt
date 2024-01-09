package kim.nzxy.spel.json

/**
 * @author ly-chn
 * @since 2024/1/5 15:41
 */
data class SpELInfo(val method: SpELInfoMethod, val fields: HashMap<String, String>)
data class SpELInfoMethod(
    var result: Boolean = false,
    var resultName: String? = "result",
    var parameters: Boolean = false,
    var parametersPrefix: HashSet<String>?
)
