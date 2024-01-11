package kim.nzxy.spel

import com.intellij.lang.injection.general.Injection
import com.intellij.lang.injection.general.LanguageInjectionContributor
import com.intellij.lang.injection.general.SimpleInjection
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLiteralExpression
import com.intellij.spring.el.SpringELLanguage
import org.jetbrains.kotlin.psi.KtStringTemplateExpression

class LySpELExtensionLanguageInject : LanguageInjectionContributor {
    private val injection = SimpleInjection(SpringELLanguage.INSTANCE, "", "", null)
    override fun getInjection(context: PsiElement): Injection? {
        if (context !is PsiLiteralExpression && context !is KtStringTemplateExpression) {
            return null
        }

        val service = SpELConfigService.getInstance()
        val path = service.getFieldPath(context) ?: return null
        if (service.hasSpELInfo(context.project, "${path.first}.${path.second}")) {
            return injection
        }
        return null
    }
}