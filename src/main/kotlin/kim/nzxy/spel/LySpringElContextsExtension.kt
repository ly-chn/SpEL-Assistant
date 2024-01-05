package kim.nzxy.spel

import com.intellij.lang.jvm.annotation.JvmAnnotationAttribute
import com.intellij.lang.jvm.annotation.JvmAnnotationClassValue
import com.intellij.lang.jvm.annotation.JvmAnnotationConstantValue
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.impl.source.PsiClassReferenceType
import com.intellij.psi.impl.source.PsiTypeElementImpl
import com.intellij.psi.impl.source.tree.java.PsiClassObjectAccessExpressionImpl
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.spring.el.contextProviders.SpringElContextsExtension
import com.intellij.util.SmartList
import org.jetbrains.uast.getContainingAnnotationEntry
import org.jetbrains.uast.toUElement

class LySpringElContextsExtension : SpringElContextsExtension() {

    override fun getContextVariables(contextElement: PsiElement): MutableCollection<out PsiVariable> {
        val collector = SmartList<PsiVariable>()
        val module = ModuleUtilCore.findModuleForPsiElement(contextElement)
        val service = YamlConfigService.getInstance()
        service.getAllMetaConfigKeys(module!!)
        try {
            direct(contextElement, collector)
            if (collector.isNotEmpty()) {
                return collector
            }
        } catch (_: Exception) {
        }
        try {
            indirect(contextElement, collector)
        } catch (_: Exception) {
        }
        return collector
    }

    private fun direct(contextElement: PsiElement, collector: SmartList<PsiVariable>) {
        val anno = getTargetAnno(contextElement) ?: return
        val spELAnnoList = getTargetSpELAnnoList(contextElement, anno) ?: return
        methodTip(anno, spELAnnoList, collector)
        fieldTip(anno.project, spELAnnoList, collector)
    }

    private fun indirect(contextElement: PsiElement, collector: SmartList<PsiVariable>) {
        val parent = contextElement.context?.parent ?: return
        if (parent !is PsiModifierListOwner) {
            return
        }
        val spELWith = parent.modifierList?.annotations
            ?.find { it.qualifiedName == SpELConst.spELWithAnno } ?: return
        val attrs = spELWith.attributes
        val annoValue = attrs.find { it.attributeName == SpELConst.spELWithAttrAnno }?.attributeValue
        val anno = (annoValue as? JvmAnnotationClassValue)?.qualifiedName ?: return
        val fieldValue = attrs.find { it.attributeName == SpELConst.spELWithAttrField }?.attributeValue
        val fieldName = ((fieldValue as? JvmAnnotationConstantValue)?.constantValue as? String)
            ?: SpELConst.spELWithAttrFieldDefault
        val project = contextElement.project
        val annoClass = JavaPsiFacade.getInstance(project)
            .findClass(anno, GlobalSearchScope.allScope(project)) ?: return
        val spELAnnoList = annoClass.findMethodsByName(fieldName, false)[0].modifierList.annotations
        fieldTip(project, spELAnnoList, collector)
    }

    private fun methodTip(anno: PsiAnnotation, spELAnnoList: Array<PsiAnnotation>, collector: SmartList<PsiVariable>) {
        val attrs = spELAnnoList.find { it.qualifiedName == SpELConst.methodAnno }?.parameterList?.attributes
            ?: return

        val result = attrValue(attrs, SpELConst.methodAttrResult, SpELConst.methodAttrResultDefault)
        val parameters = attrValue(attrs, SpELConst.methodAttrParams, SpELConst.methodAttrParamsDefault)
        if (!result && !parameters) {
            return
        }
        val method = PsiTreeUtil.getParentOfType(anno, PsiMethod::class.java) ?: return
        if (result) {
            val resultName = attrValue(attrs, SpELConst.methodAttrResultName, SpELConst.methodAttrResultNameDefault)
            val returnTypeElement = method.returnTypeElement
            val returnType = method.returnType
            if (returnTypeElement != null && returnType != null) {
                collector.add(LySpELContextVariable(resultName, returnType, returnTypeElement))
            }
        }
        if (parameters) {
            val parametersPrefix =
                attrValue(attrs, SpELConst.methodAttrParamsPrefix, SpELConst.methodAttrParamsPrefixDefault)
            for ((index, parameter) in method.parameterList.parameters.withIndex()) {
                val parameterType = parameter.type
                collector.add(parameter)
                parametersPrefix.forEach {
                    collector.add(LySpELContextVariable("$it$index", parameterType, parameter))
                }
            }
        }
    }

    private fun fieldTip(project: Project, spELAnnoList: Array<PsiAnnotation>, collector: SmartList<PsiVariable>) {
        spELAnnoList.forEach { fieldAnno ->
            if (fieldAnno.qualifiedName != SpELConst.fieldAnno) {
                return@forEach
            }
            val attrs = fieldAnno.parameterList.attributes
            val name = attrValue(attrs, SpELConst.fieldAttrName, "")
            if (name.isEmpty()) {
                return@forEach
            }
            val namePos = attrs.find { it.name == SpELConst.fieldAttrName }?.value ?: return@forEach
            val typeStr = attrValue(attrs, SpELConst.fieldAttrTypeStr, SpELConst.fieldAttrTypeStrDefault)
            val sourceType: PsiAnnotationMemberValue? =
                if (typeStr.isEmpty()) attrs.find { it.name == SpELConst.fieldAttrType }?.value else null
            val psiType = getPsiType(project, typeStr, sourceType)
            collector.add(LySpELContextVariable(name, psiType, namePos))
            if (name == SpELConst.rootName) {
                genRootField(collector, psiType)
            }
        }
    }

    private fun getPsiType(project: Project, typeStr: String?, sourceType: PsiAnnotationMemberValue?): PsiType {
        val factory = JavaPsiFacade.getElementFactory(project)
        try {
            if (!typeStr.isNullOrEmpty()) {
                return factory.createTypeFromText(typeStr, null)
            }
            if (sourceType != null && sourceType is PsiClassObjectAccessExpressionImpl) {
                return (sourceType.firstChild as PsiTypeElementImpl).type
            }
            return factory.createTypeFromText(SpELConst.fieldAttrTypeDefault, null)
        } catch (e: Exception) {
            thisLogger().error("获取类型出错： typeStr: $typeStr, sourceType: $sourceType")
            return factory.createTypeFromText(SpELConst.fieldAttrTypeDefault, null)
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
        val anno = getTargetAnno(contextElement) ?: return collector
        val spELAnnoList = getTargetSpELAnnoList(contextElement, anno) ?: return collector
        val rootAnno = spELAnnoList.find {
            it.qualifiedName == SpELConst.fieldAnno
                    && attrValue(it.parameterList.attributes, SpELConst.fieldAttrName, "") == SpELConst.rootName
        } ?: return collector
        val attrs = rootAnno.parameterList.attributes
        val typeStr = attrValue(attrs, SpELConst.fieldAttrTypeStr, SpELConst.fieldAttrTypeStrDefault)
        val sourceType: PsiAnnotationMemberValue? =
            if (typeStr.isEmpty()) attrs.find { it.name == SpELConst.fieldAttrType }?.value else null
        val psiType = getPsiType(anno.project, typeStr, sourceType)
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
        return collector
    }

    private fun getTargetAnno(contextElement: PsiElement): PsiAnnotation? {
        val context = contextElement.context ?: return null
        val anno = getContainingAnnotationEntry(context.toUElement())?.first ?: return null
        return anno
    }

    private fun getTargetSpELAnnoList(contextElement: PsiElement, anno: PsiAnnotation): Array<PsiAnnotation>? {
        val context = contextElement.context ?: return null
        val qualifiedName = anno.qualifiedName ?: return null
        val attrName = PsiTreeUtil.getParentOfType(context, PsiElement::class.java)
            ?.children?.find { it is PsiIdentifier }?.text ?: SpELConst.defaultAttrName
        val annotationClass = JavaPsiFacade.getInstance(contextElement.project)
            .findClass(qualifiedName, GlobalSearchScope.allScope(contextElement.project)) ?: return null
        val spELAnnoList = annotationClass.findMethodsByName(attrName, false)[0].modifierList.annotations
        return spELAnnoList
    }

    private fun <T> attrValue(attributes: Array<out JvmAnnotationAttribute>, name: String, defaultValue: T): T {
        val attr = attributes.find { it.attributeName == name }?.attributeValue ?: return defaultValue
        if (attr is JvmAnnotationConstantValue) {
            @Suppress("UNCHECKED_CAST")
            return attr.constantValue as T
        }
        return defaultValue
    }
}
