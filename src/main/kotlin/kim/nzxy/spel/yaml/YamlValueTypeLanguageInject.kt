package kim.nzxy.spel.yaml

import com.intellij.lang.injection.general.Injection
import com.intellij.lang.injection.general.LanguageInjectionContributor
import com.intellij.lang.injection.general.SimpleInjection
import com.intellij.lang.java.JavaLanguage
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PatternCondition
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.spring.el.SpringELLanguage
import com.intellij.util.ProcessingContext
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLMapping
import org.jetbrains.yaml.psi.YAMLScalar

/**
 * @author ly-chn
 * @since 2024/1/3 16:34
 */
class YamlValueTypeLanguageInject: LanguageInjectionContributor {
    private val pattern = PlatformPatterns.psiElement(YAMLScalar::class.java)
        .withParent(YAMLKeyValue::class.java)
        .withSuperParent(2, YAMLMapping::class.java)
        .withSuperParent(3, PlatformPatterns.psiElement(YAMLKeyValue::class.java).with(object:
            PatternCondition<YAMLKeyValue>("yaml kv is in fields") {
            override fun accepts(element: YAMLKeyValue, context: ProcessingContext?): Boolean {
                return ConfigYamlUtil.getQualifiedConfigKeyName(element).endsWith(".fields")
            }
        }))

    override fun getInjection(element: PsiElement): Injection? {
        if (pattern.accepts(element)) {
            return SimpleInjection(JavaLanguage.INSTANCE, "interface A { public static ", " value = null;}", null)
        }
        return null
    }
}