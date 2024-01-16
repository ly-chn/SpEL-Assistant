package kim.nzxy.spel.json

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.json.psi.JsonElementVisitor
import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonObject
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor

/**
 * @author ly-chn
 * @since 2024/1/8 11:12
 */
class SpELJsonKeyInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        val isSpELFile = ConfigJsonUtil.isSpELFilename(holder.file.name)
        return object : JsonElementVisitor() {
            override fun visitObject(o: JsonObject) {
                if (!isSpELFile || o.parent !is JsonFile) {
                    return
                }
                val keys = HashMap<String, PsiElement>()
                for (property in o.propertyList) {
                    keys[property.name] = property.nameElement
                }
                visitKeys(keys, holder)
            }
        }
    }

    private fun visitKeys(keys: HashMap<String, PsiElement>, holder: ProblemsHolder) {
        val service = JsonSuggestionService.getInstance()
        val enabledKeys = service.getAllMetaConfigKeys(holder.project)
        keys.forEach { (k, v) ->
            if (!enabledKeys.contains(k)) {
                // todo: msg bundle
                holder.registerProblem(
                    v,
                    "Could not find the corresponding annotation or field in the annotation",
                    ProblemHighlightType.WARNING
                )
            }
        }
    }
}