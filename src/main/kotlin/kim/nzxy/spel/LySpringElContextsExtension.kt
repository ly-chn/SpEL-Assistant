package kim.nzxy.spel

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiType
import com.intellij.psi.PsiVariable
import com.intellij.psi.impl.source.PsiClassReferenceType
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.spring.el.contextProviders.SpringElContextsExtension
import com.intellij.util.SmartList
import kim.nzxy.spel.json.SpELInfoMethod

class LySpringElContextsExtension : SpringElContextsExtension() {

    override fun getContextVariables(contextElement: PsiElement): MutableCollection<out PsiVariable> {
        val collector = SmartList<PsiVariable>()
        val service = SpELConfigService.getInstance()
        val context = contextElement.context ?: return collector
        val fieldPath = service.getFieldPath(context) ?: return collector
        val spElMetaConfig = service.getSpELInfo(contextElement.project, "${fieldPath.first}.${fieldPath.second}")
            ?: return collector
        fixMethod(context, spElMetaConfig.method, collector)
        fixFields(context, spElMetaConfig.fields, collector)
        return collector
    }

    private fun fixMethod(context: PsiElement, config: SpELInfoMethod, collector: SmartList<PsiVariable>) {
        val method = PsiTreeUtil.getParentOfType(context, PsiMethod::class.java) ?: return
        if (config.result) {
            val resultName = config.resultName ?: SpELConst.methodResultNameDefault
            val returnTypeElement = method.returnTypeElement
            val returnType = method.returnType
            if (returnTypeElement != null && returnType != null) {
                collector.add(LySpELContextVariable(resultName, returnType, returnTypeElement))
            }
        }
        if (config.parameters) {
            for ((index, parameter) in method.parameterList.parameters.withIndex()) {
                val parameterType = parameter.type
                collector.add(parameter)
                if (config.parametersPrefix != null) {
                    config.parametersPrefix!!.forEach {
                        collector.add(LySpELContextVariable("$it$index", parameterType, parameter))
                    }
                } else {
                    SpELConst.methodParamsPrefixDefault.forEach {
                        collector.add(LySpELContextVariable("$it$index", parameterType, parameter))
                    }
                }
            }
        }
    }

    private fun fixFields(context: PsiElement, config: Map<String, String>, collector: SmartList<PsiVariable>) {
        val project = context.project
        config.forEach { (name, typeStr) ->
            val service = SpELConfigService.getInstance()
            service.findPsiType(project, typeStr)?.let {
                collector.add(LySpELContextVariable(name, it, context))
                if (name == SpELConst.rootName) {
                    genRootField(collector, it)
                }
            }
        }
    }

    private fun genRootField(collector: SmartList<PsiVariable>, psiType: PsiType) {
        if (psiType is PsiClassReferenceType) {
            psiType.resolve()?.allFields?.forEach {
                collector.add(LySpELContextVariable(it.name, it.type, it))
            }
        }
    }

    override fun getRootMethods(contextElement: PsiElement): MutableCollection<PsiMethod> {
        val collector = HashSet<PsiMethod>()
        val context = contextElement.context ?: return collector
        val service = SpELConfigService.getInstance()
        val fieldPath = service.getFieldPath(context) ?: return collector
        val spElMetaConfig = service.getSpELInfo(contextElement.project, "${fieldPath.first}.${fieldPath.second}")
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
