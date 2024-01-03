package kim.nzxy.spel.yaml

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.impl.scopes.ModulesScope
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.search.searches.ClassInheritorsSearch
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiTypesUtil
import com.intellij.util.Processors
import org.mozilla.javascript.commonjs.module.ModuleScope


@Service
class YamlSuggestionService {
    private val ignoredQualifiedPrefix = arrayOf("jdk.", "java.")

    companion object {
        fun getInstance(): YamlSuggestionService {
            return ApplicationManager.getApplication().getService(YamlSuggestionService::class.java)
        }
    }


    private fun getAnnotationFields(module: Module): Set<String> {
        val sourceAnnotations = getAnnotationClasses(module.project, GlobalSearchScope.projectScope(module.project))
        val libraryAnnotationCache =
            getAnnotationClasses(module.project, ProjectScope.getLibrariesScope(module.project))
        val res = HashSet<String>()
        libraryAnnotationCache.forEach { (k, v) -> v.forEach { res.add("$k.$it") } }
        sourceAnnotations.forEach { (k, v) -> v.forEach { res.add("$k.$it") } }
        return res
    }

    private fun getAnnotationClasses(project: Project, scope: GlobalSearchScope): HashMap<String, List<String>> {
        val result = HashMap<String, List<String>>()
        val stringType = PsiTypesUtil.getClassType(getStringCls(project))
        ClassInheritorsSearch.search(getAnnoCls(project), scope, true)
            .forEach { anno ->
                val qualifiedName = anno.qualifiedName ?: return@forEach
                if (ignoredQualifiedPrefix.any { qualifiedName.startsWith(it) }) return@forEach
                val methods = anno.methods
                val methodNames = methods.filter { method -> stringType == method.returnType }.map { it.name }
                if (methodNames.isNotEmpty()) {
                    result[qualifiedName] = methodNames
                }
            }
        return result
    }
    private fun getAnnoFields(project: Project, scope: GlobalSearchScope): Set<String> {
        val res = HashSet<String>()
        val stringType = PsiTypesUtil.getClassType(getStringCls(project))
        ClassInheritorsSearch.search(getAnnoCls(project), scope, true)
            .forEach { anno ->
                val qualifiedName = anno.qualifiedName ?: return@forEach
                if (ignoredQualifiedPrefix.any { qualifiedName.startsWith(it) }) return@forEach
                anno.methods.forEach {
                    if (it.returnType == stringType) {
                        res.add("$qualifiedName${it.name}")
                    }
                }
            }
        return res
    }

    fun getAllMetaConfigKeys(module: Module?): Set<String> {
        module ?: return emptySet()
        val project = module.project

        val sourceAnnotations = getAnnotationClasses(project, module.moduleScope)
        val libraryAnnotationCache = getAnnotationClasses(project,
            ModulesScope.moduleWithDependenciesAndLibrariesScope(module))
        return CachedValuesManager.getManager(project).getCachedValue(module) {
            val keys = HashSet<String>()
            val collect = Processors.cancelableCollectProcessor(keys)
            val fields = getAnnotationFields(module)
            fields.forEach {
                collect.process(it)
                collect.process("$it.fields")
                collect.process("$it.method.result")
                collect.process("$it.method.parameters")
                collect.process("$it.method.resultName")
                collect.process("$it.method.parametersPrefix")
            }
            // todo: 追踪依赖, 区分源码和库
            return@getCachedValue CachedValueProvider.Result.create<Set<String>>(
                keys,
                ModificationTracker.NEVER_CHANGED
            )
        }
    }

    private fun getCls(project: Project, name: String): PsiClass {
        return JavaPsiFacade.getInstance(project)
            .findClass(name, ProjectScope.getLibrariesScope(project))!!
    }

    private fun getAnnoCls(project: Project): PsiClass {
        return CachedValuesManager.getManager(project).getCachedValue(project) {
            return@getCachedValue CachedValueProvider.Result.create(
                getCls(project, "java.lang.annotation.Annotation"),
                ModificationTracker.NEVER_CHANGED
            )
        }
    }

    private fun getStringCls(project: Project): PsiClass {
        return CachedValuesManager.getManager(project).getCachedValue(project) {
            return@getCachedValue CachedValueProvider.Result.create(
                getCls(project, "java.lang.String"),
                ModificationTracker.NEVER_CHANGED
            )
        }
    }

}