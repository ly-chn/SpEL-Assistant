package kim.nzxy.spel

import com.intellij.lang.injection.general.Injection
import com.intellij.lang.injection.general.LanguageInjectionContributor
import com.intellij.lang.injection.general.SimpleInjection
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.*
import com.intellij.spring.el.SpringELLanguage

class LySpELExtensionLanguageInject : LanguageInjectionContributor {
    override fun getInjection(context: PsiElement): Injection? {
        if (context !is PsiLiteralExpression) {
            return null
        }

        val parent = context.parent
        if (parent is PsiModifierListOwner) {
            if (parent.modifierList?.annotations?.any { it.qualifiedName == SpELConst.spELWithAnno } == true) {
                return SimpleInjection(SpringELLanguage.INSTANCE, "", "", null)
            }
        }
        if (parent is PsiNameValuePair) {
            val anno = parent.parent.parent
            if (anno is PsiAnnotation) {
                val attrName = parent.attributeName
                val method = anno.resolveAnnotationType()?.methods?.find { it.name == attrName } ?: return null
                if (method.annotations.any { SpELConst.spELInjectTarget.contains(it.qualifiedName) }) {
                    return SimpleInjection(SpringELLanguage.INSTANCE, "", "", null)
                }
                val service = SpELConfigService.getInstance()
                val configKeys = service.getAllMetaConfigKeys(ModuleUtilCore.findModuleForPsiElement(context)!!)
                if (configKeys.containsKey(anno.qualifiedName + "." + method.name)) {
                    return SimpleInjection(SpringELLanguage.INSTANCE, "", "", null)
                }
            }
        }
        return null
    }
}