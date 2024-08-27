package kim.nzxy.spel

import com.intellij.lang.injection.MultiHostInjector
import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.openapi.components.service
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.PsiLiteralExpression
import com.intellij.spring.el.SpringELLanguage
import kim.nzxy.spel.service.SpELJsonConfigService
import org.jetbrains.kotlin.psi.KtStringTemplateExpression


class LySpELLanguageMultiHostInjector : MultiHostInjector {
    override fun getLanguagesToInject(registrar: MultiHostRegistrar, context: PsiElement) {
        val service = context.project.service<SpELJsonConfigService>()
        val path = service.getFieldPath(context) ?: return
        val spELInfo = service.getSpELInfo("${path.first}@${path.second}") ?: return
        if (LyUtil.isEmpty(spELInfo.prefix) || LyUtil.isEmpty(spELInfo.suffix)) {
            return
        }
        registrar.startInjecting(SpringELLanguage.INSTANCE)
        val text = context.text
        val prefix = spELInfo.prefix!!
        val suffix = spELInfo.suffix!!
        var index = 0
        while (true) {
            val start = text.indexOf(prefix, index)
            if (start == -1) {
                break
            }
            val end = text.indexOf(suffix, start + prefix.length)
            if (end == -1) {
                break
            }
            registrar.addPlace(null, null, context as PsiLanguageInjectionHost, TextRange(start + prefix.length, end))
            println(context.text)
            println(context.textRange)
            index = end + suffix.length
        }
        registrar.doneInjecting()
    }

    override fun elementsToInjectIn(): MutableList<out Class<out PsiElement>> {
        val result: MutableList<Class<out PsiElement>> = mutableListOf(PsiLiteralExpression::class.java)
        if (PluginChecker.getInstance().kotlin()) {
            result.add(KtStringTemplateExpression::class.java)
        }
        return result
    }
}