package kim.nzxy.spel.yaml

import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import com.intellij.util.SmartList
import kim.nzxy.spel.yaml.hints.BooleanHintReference
import org.jetbrains.yaml.YAMLFileType
import org.jetbrains.yaml.YAMLLanguage
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLScalar

/**
 * value提示功能
 * @author ly-chn
 * @since 2023/12/28 16:27
 */
class LyYamlReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        val pattern = PlatformPatterns.psiElement(YAMLScalar::class.java).withLanguage(YAMLLanguage.INSTANCE)
            .inVirtualFile(
                PlatformPatterns.virtualFile()
                    .withName("spel-extension.yml")
                    .ofType(YAMLFileType.YML)
            )
        registrar.registerReferenceProvider(pattern, object : PsiReferenceProvider() {
            override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
                val yamlKeyValue = PsiTreeUtil.getParentOfType(element, YAMLKeyValue::class.java)
                    ?: return PsiReference.EMPTY_ARRAY
                val key = ConfigYamlUtil.getQualifiedConfigKeyName(yamlKeyValue)
                val references = SmartList<PsiReference>()
                if (key.endsWith(".method.result") || key.endsWith(".method.parameters")) {
                    return arrayOf(BooleanHintReference(element, ElementManipulators.getValueTextRange(element)))
                }
                return references.toArray(PsiReference.EMPTY_ARRAY)
            }
        })
    }
}