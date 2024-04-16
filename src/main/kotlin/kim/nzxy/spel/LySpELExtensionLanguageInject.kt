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
        if (context !is PsiLiteralExpression) {
            if (!PluginChecker.getInstance().kotlin() || context !is KtStringTemplateExpression) {
                return null
            }
        }

        val service = SpELConfigService.getInstance()
        val path = service.getFieldPath(context) ?: return null
        val spELInfo = service.getSpELInfo(context.project, "${path.first}@${path.second}") ?: return null
        if (LyUtil.isEmpty(spELInfo.prefix) && LyUtil.isEmpty(spELInfo.suffix)) {
            return injection
        }
        return null
    }
}