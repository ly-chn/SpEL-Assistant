package kim.nzxy.spel

import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.components.service
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiVariable
import com.intellij.psi.impl.source.PsiClassReferenceType
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.spring.el.contextProviders.SpringElContextsExtension
import com.intellij.util.SmartList
import kim.nzxy.spel.json.SpELInfo
import kim.nzxy.spel.service.SpELJsonConfigService
import org.jetbrains.kotlin.asJava.toLightMethods
import org.jetbrains.kotlin.asJava.toPsiParameters
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtNamedFunction

class LySpringElContextsExtension : SpringElContextsExtension() {

    override fun getContextVariables(contextElement: PsiElement): MutableCollection<out PsiVariable> {
        val collector = SmartList<PsiVariable>()
        val service = contextElement.project.service<SpELJsonConfigService>()
        val context = contextElement.context ?: return collector
        val fieldPath = service.getFieldPath(context) ?: return collector
        val spELInfo = service.getSpELInfo("${fieldPath.first}@${fieldPath.second}")
            ?: return collector
        fixMethod(context, spELInfo, collector)
        fixKtFun(context, spELInfo, collector)
        fixFields(context, spELInfo, collector)
        return collector
    }

    private fun fixMethod(context: PsiElement, config: SpELInfo, collector: SmartList<PsiVariable>) {
        if(context.language != JavaLanguage.INSTANCE) return
        val methodConfig = config.method
        if (methodConfig?.parameters != true && methodConfig?.result != true) return
        val method = PsiTreeUtil.getParentOfType(context, PsiMethod::class.java) ?: return
        if (methodConfig.result == true) {
            val resultName = methodConfig.resultName ?: SpELConst.methodResultNameDefault
            val returnTypeElement = method.returnTypeElement
            val returnType = method.returnType
            if (returnTypeElement != null && returnType != null) {
                collector.add(LySpELContextVariable(resultName, returnType, config.sourceFile!!))
            }
        }
        if (methodConfig.parameters != true) return
        for ((index, parameter) in method.parameterList.parameters.withIndex()) {
            fillParameter(index, parameter, collector, methodConfig.parametersPrefix)
        }
    }

    private fun fixKtFun(context: PsiElement, config: SpELInfo, collector: SmartList<PsiVariable>) {
        if(context.language != KotlinLanguage.INSTANCE) return
        val methodConfig = config.method
        if (methodConfig?.parameters != true && methodConfig?.result != true) return
        val method = PsiTreeUtil.getParentOfType(context, KtNamedFunction::class.java) ?: return
        if (methodConfig.result == true && method.hasDeclaredReturnType()) {
            val lightMethod = method.toLightMethods().first()
            val resultName = methodConfig.resultName ?: SpELConst.methodResultNameDefault
            val returnType = lightMethod.returnType
            if (returnType != null) {
                collector.add(LySpELContextVariable(resultName, returnType, config.sourceFile!!))
            }
        }
        if (methodConfig.parameters != true) return
        for ((index, parameter) in method.valueParameters.withIndex()) {
            val psiParameter = parameter.toPsiParameters().first()
            fillParameter(index, psiParameter, collector, methodConfig.parametersPrefix)
        }
    }

    private fun fillParameter(index: Int, parameter: PsiParameter, collector: SmartList<PsiVariable>, prefix: Set<String>?) {
        val psiType = parameter.type
        collector.add(parameter)
        if (prefix != null) {
            prefix.forEach {
                collector.add(LySpELContextVariable("$it$index", psiType, parameter))
            }
        } else {
            SpELConst.methodParamsPrefixDefault.forEach {
                collector.add(LySpELContextVariable("$it$index", psiType, parameter))
            }
        }
    }

    private fun fixFields(context: PsiElement, config: SpELInfo, collector: SmartList<PsiVariable>) {
        config.fields?.forEach { (name, typeStr) ->
            val service = context.project.service<SpELJsonConfigService>()
            service.findPsiType(typeStr)?.let {
                collector.add(LySpELContextVariable(name, it, config.sourceFile!!))
            }
        }
    }

    override fun getRootMethods(contextElement: PsiElement): MutableCollection<PsiMethod> {
        val collector = HashSet<PsiMethod>()
        val context = contextElement.context ?: return collector
        val service = contextElement.project.service<SpELJsonConfigService>()
        val fieldPath = service.getFieldPath(context) ?: return collector
        val spElMetaConfig = service.getSpELInfo("${fieldPath.first}@${fieldPath.second}")
            ?: return collector
        spElMetaConfig.fields?.get(SpELConst.rootName)?.let { rootClass ->
            service.findPsiType(rootClass)?.let { psiType ->
                if (psiType is PsiClassReferenceType) {
                    psiType.resolve()?.allMethods?.forEach {
                        if (!it.hasModifierProperty("public") || it.isConstructor) {
                            return@forEach
                        }
                        val containingClass = it.containingClass
                        if (containingClass != null && containingClass.qualifiedName != SpELConst.objClass) {
                            collector.add(it)
                        }
                    }
                }
            }
        }
        return collector
    }
}
