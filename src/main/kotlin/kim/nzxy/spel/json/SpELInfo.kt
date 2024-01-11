package kim.nzxy.spel.json

/**
 * @author ly-chn
 * @since 2024/1/5 15:41
 */
data class SpELInfo(var method: SpELInfoMethod = SpELInfoMethod(), var fields: HashMap<String, String> = hashMapOf())
data class SpELInfoMethod(
    var result: Boolean = false,
    var resultName: String? = "result",
    var parameters: Boolean = false,
    var parametersPrefix: HashSet<String>? = null
)
