package kim.nzxy.spel.json

import com.intellij.psi.PsiFile

/**
 * @author ly-chn
 * @since 2024/1/5 15:41
 */
data class SpELInfo(
    var method: SpELInfoMethod? = SpELInfoMethod(),
    var fields: HashMap<String, String>? = hashMapOf(),
    var sourceFile: PsiFile?,
    var prefix: String?,
    var suffix: String?
)

data class SpELInfoMethod(
    var result: Boolean? = false,
    var resultName: String? = "result",
    var parameters: Boolean? = false,
    var parametersPrefix: HashSet<String>? = null
)
