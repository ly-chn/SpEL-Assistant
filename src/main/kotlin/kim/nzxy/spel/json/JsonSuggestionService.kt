package kim.nzxy.spel.json

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
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


@Service
class JsonSuggestionService {
    private val ignoredQualifiedPrefix = arrayOf("jdk.", "java.", "javax.", "jakarta.", "lombok.", "org.springframework", "kotlin.")

    // todo: for library and source tracker
    private val regularTracking = ModificationTracker { System.currentTimeMillis() / 1000 }

    companion object {
        fun getInstance(): JsonSuggestionService {
            return ApplicationManager.getApplication().getService(JsonSuggestionService::class.java)
        }
    }

    private fun getAnnoFields(scope: GlobalSearchScope): Set<String> {
        val res = HashSet<String>()
        val project = scope.project ?: return emptySet()
        val stringType = PsiTypesUtil.getClassType(getStringCls(project))
        ClassInheritorsSearch.search(getAnnoCls(project), scope, true)
            .forEach { anno ->
                val qualifiedName = anno.qualifiedName ?: return@forEach
                if (ignoredQualifiedPrefix.any { qualifiedName.startsWith(it) }) return@forEach
                anno.methods.forEach {
                    if (it.returnType == stringType) {
                        res.add("$qualifiedName@${it.name}")
                    }
                }
            }
        return res
    }

    fun getAllMetaConfigKeys(project: Project): HashSet<String> {
        val sourceAnnoFields = CachedValuesManager.getManager(project).getCachedValue(project) {
            val scope = GlobalSearchScope.projectScope(project)
            return@getCachedValue CachedValueProvider.Result.create(getAnnoFields(scope), regularTracking)
        }
        val libraryAnnoFields = CachedValuesManager.getManager(project).getCachedValue(project) {
            val scope = ProjectScope.getLibrariesScope(project)
            return@getCachedValue CachedValueProvider.Result.create(getAnnoFields(scope), regularTracking)
        }
        val keys = HashSet<String>()
        keys.addAll(sourceAnnoFields)
        keys.addAll(libraryAnnoFields)
        return keys
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