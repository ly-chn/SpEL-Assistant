package kim.nzxy.spel.service

import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.search.searches.ClassInheritorsSearch
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.util.PsiTypesUtil
import com.intellij.uast.UastMetaLanguage
import kim.nzxy.spel.json.ConfigJsonUtil
import org.jetbrains.kotlin.idea.KotlinLanguage
import java.util.concurrent.atomic.AtomicLong


@Service(Service.Level.PROJECT)
class JsonSuggestionService(private val project: Project) {
    private val uastChangeTracker: List<ModificationTracker> = listOf(JavaLanguage.INSTANCE, KotlinLanguage.INSTANCE).map {
        PsiModificationTracker.getInstance(project).forLanguage(it)
    }

    private val ignoredQualifiedPrefix =
        arrayOf("jdk.", "java.", "javax.", "jakarta.", "lombok.", "org.springframework", "kotlin.")

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

    fun getAllMetaConfigKeys(): Set<String> {
        val keys = CachedValuesManager.getManager(project).getCachedValue(project) {
            val scope = GlobalSearchScope.projectScope(project)
            scope.union(ProjectScope.getLibrariesScope(project))
            return@getCachedValue CachedValueProvider.Result.create(getAnnoFields(scope), uastChangeTracker)
        }
        return keys ?: emptySet()
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