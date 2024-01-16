package kim.nzxy.spel

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler
import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonProperty
import com.intellij.openapi.editor.Editor
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.util.PsiTreeUtil
import kim.nzxy.spel.json.ConfigJsonUtil

/**
 * @author ly-chn
 * @since 2024/1/12 14:45
 */
class JsonSpELSymbolGotoDeclarationHandler : GotoDeclarationHandler {
    override fun getGotoDeclarationTargets(element: PsiElement?, offset: Int, editor: Editor?): Array<PsiElement>? {
        element ?: return null
        if (element.containingFile !is JsonFile || !ConfigJsonUtil.isSpELFilename(element.containingFile.name)) return null
        if (!ConfigJsonUtil.isPropertyKey(element)) return null
        val property = PsiTreeUtil.getParentOfType(element, JsonProperty::class.java) ?: return null
        if (ConfigJsonUtil.getParentNames(property).isNotEmpty()) return null
        val symbol = element.text.trim('"').split('@')
        if (symbol.size != 2) return null
        val (cls, method) = symbol
        JavaPsiFacade.getInstance(element.project)
            .findClass(cls, ProjectScope.getAllScope(element.project))?.methods?.forEach {
                if (it.name == method) return arrayOf(it)
            }
        return null
    }
}