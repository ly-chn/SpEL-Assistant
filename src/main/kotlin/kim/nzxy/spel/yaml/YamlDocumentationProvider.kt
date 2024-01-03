package kim.nzxy.spel.yaml

import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiElement
import kim.nzxy.spel.SpELConst
import org.jetbrains.annotations.Nls
import org.jetbrains.yaml.psi.YAMLKeyValue

/**
 * @author ly-chn
 * @since 2024/1/3 14:11
 */
class YamlDocumentationProvider : AbstractDocumentationProvider() {
    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String? {
        if (element is YAMLKeyValue) {
            val qualifiedName = ConfigYamlUtil.getQualifiedConfigKeyName(element)
            if (qualifiedName.isEmpty()) {
                return null
            }
            // package, class, method, spEL
            val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return null
            val service = YamlSuggestionService.getInstance()
            val keys = service.getAllMetaConfigKeys(module)
            // SpEL Config
            if (keys.keys.any { it == qualifiedName }) {
                val key = SpELConst.spELDocMap.keys.find { qualifiedName.endsWith(it) } ?: return null
                return SpELConst.spELDocMap[key]?.toHTML(qualifiedName)
            }
            // 其为类
            var cls = service.getCls(module.project, qualifiedName)

            // 其父为类
            // cls = ...

        }
        return "if (element !is DocumentationElement) null else element.getDocumentation()"
    }
}