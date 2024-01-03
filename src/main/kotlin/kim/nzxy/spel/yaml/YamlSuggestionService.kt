package kim.nzxy.spel.yaml

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.module.Module
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
import kim.nzxy.spel.SpELConst


@Service
class YamlSuggestionService {
    private val ignoredQualifiedPrefix = arrayOf("jdk.", "java.")

    // todo: for library and source tracker
    private val regularTracking = ModificationTracker { System.currentTimeMillis() / 1000 }

    companion object {
        fun getInstance(): YamlSuggestionService {
            return ApplicationManager.getApplication().getService(YamlSuggestionService::class.java)
        }
    }

    private fun getAnnoFields(scope: GlobalSearchScope): Map<String, String> {
        val res = HashMap<String, String>()
        val project = scope.project ?: return emptyMap()
        val stringType = PsiTypesUtil.getClassType(getStringCls(project))
        ClassInheritorsSearch.search(getAnnoCls(project), scope, true)
            .forEach { anno ->
                val qualifiedName = anno.qualifiedName ?: return@forEach
                if (ignoredQualifiedPrefix.any { qualifiedName.startsWith(it) }) return@forEach
                anno.methods.forEach {
                    if (it.returnType == stringType) {
                        SpELConst.spELDocMap.forEach { (suffix, doc) ->
                            res["$qualifiedName.${it.name}$suffix"] = doc.type
                        }
                    }
                }
            }
        return res
    }

    fun getAllMetaConfigKeys(module: Module): Map<String, String> {
        val project = module.project
        val sourceAnnoFields = CachedValuesManager.getManager(project).getCachedValue(module) {
            val scope = GlobalSearchScope.moduleScope(module)
            return@getCachedValue CachedValueProvider.Result.create(getAnnoFields(scope), regularTracking)
        }
        val libraryAnnoFields = CachedValuesManager.getManager(project).getCachedValue(module) {
            val scope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module, false)
            return@getCachedValue CachedValueProvider.Result.create(getAnnoFields(scope), regularTracking)
        }
        val keys = HashMap<String, String>()
        keys.putAll(sourceAnnoFields)
        keys.putAll(libraryAnnoFields)
        return keys
    }

    fun getCls(project: Project, name: String): PsiClass {
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