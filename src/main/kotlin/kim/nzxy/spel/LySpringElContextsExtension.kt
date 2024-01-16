package kim.nzxy.spel

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiVariable
import com.intellij.psi.impl.source.PsiClassReferenceType
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.spring.el.contextProviders.SpringElContextsExtension
import com.intellij.util.SmartList
import kim.nzxy.spel.json.SpELInfo

class LySpringElContextsExtension : SpringElContextsExtension() {

    override fun getContextVariables(contextElement: PsiElement): MutableCollection<out PsiVariable> {
        val collector = SmartList<PsiVariable>()
        val service = SpELConfigService.getInstance()
        val context = contextElement.context ?: return collector
        val fieldPath = service.getFieldPath(context) ?: return collector
        val spELInfo = service.getSpELInfo(contextElement.project, "${fieldPath.first}@${fieldPath.second}")
            ?: return collector
        fixMethod(context, spELInfo, collector)
        fixFields(context, spELInfo, collector)
        return collector
    }

    private fun fixMethod(context: PsiElement, config: SpELInfo, collector: SmartList<PsiVariable>) {
        val methodConfig = config.method
        val method = PsiTreeUtil.getParentOfType(context, PsiMethod::class.java) ?: return
        if (methodConfig.result) {
            val resultName = methodConfig.resultName ?: SpELConst.methodResultNameDefault
            val returnTypeElement = method.returnTypeElement
            val returnType = method.returnType
            if (returnTypeElement != null && returnType != null) {
                collector.add(LySpELContextVariable(resultName, returnType, config.sourceFile!!))
            }
        }
        if (!methodConfig.parameters) return
        for ((index, parameter) in method.parameterList.parameters.withIndex()) {
            val parameterType = parameter.type
            collector.add(parameter)
            if (methodConfig.parametersPrefix != null) {
                methodConfig.parametersPrefix!!.forEach {
                    collector.add(LySpELContextVariable("$it$index", parameterType, parameter))
                }
            } else {
                SpELConst.methodParamsPrefixDefault.forEach {
                    collector.add(LySpELContextVariable("$it$index", parameterType, parameter))
                }
            }
        }
    }

    private fun fixFields(context: PsiElement, config: SpELInfo, collector: SmartList<PsiVariable>) {
        val project = context.project
        config.fields.forEach { (name, typeStr) ->
            val service = SpELConfigService.getInstance()
            service.findPsiType(project, typeStr)?.let {
                collector.add(LySpELContextVariable(name, it, config.sourceFile!!))
            }
        }
    }

    override fun getRootMethods(contextElement: PsiElement): MutableCollection<PsiMethod> {
        val collector = HashSet<PsiMethod>()
        val context = contextElement.context ?: return collector
        val service = SpELConfigService.getInstance()
        val fieldPath = service.getFieldPath(context) ?: return collector
        val spElMetaConfig = service.getSpELInfo(contextElement.project, "${fieldPath.first}@${fieldPath.second}")
            ?: return collector
        spElMetaConfig.fields[SpELConst.rootName]?.let { rootClass ->
            service.findPsiType(contextElement.project, rootClass)?.let { psiType ->
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
