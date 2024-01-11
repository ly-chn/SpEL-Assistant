package kim.nzxy.spel

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.fileEditor.impl.LoadTextUtil
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.SmartList
import com.intellij.util.containers.ConcurrentFactoryMap
import com.intellij.util.containers.ContainerUtil
import kim.nzxy.spel.json.ConfigJsonUtil
import kim.nzxy.spel.json.SpELInfo
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.uast.getContainingAnnotationEntry
import org.jetbrains.uast.toUElement

/**
 * @author ly-chn
 * @since 2024/1/4 10:07
 */
@Service
class SpELConfigService {
    companion object {
        private val fieldNameRegex = Regex("^[a-zA-Z_]\\w*$")
        fun getInstance(): SpELConfigService {
            return ApplicationManager.getApplication().getService(SpELConfigService::class.java)
        }
    }

    fun getSpELInfo(project: Project, fieldPath: String): SpELInfo? {
        return getLocalMetaConfigKeys(project)[fieldPath] ?: getLibrariesConfigKeys(project)[fieldPath]
    }

    fun hasSpELInfo(project: Project, fieldPath: String): Boolean {
        return getLibrariesConfigKeys(project).containsKey(fieldPath)
                || getLocalMetaConfigKeys(project).containsKey(fieldPath)
    }

    fun findPsiType(project: Project, className: String): PsiType? {
        val cache: Map<String, PsiType?> = CachedValuesManager.getManager(project).getCachedValue(project) {
            val map: Map<String, PsiType?> = ConcurrentFactoryMap.createMap { key: String ->
                DumbService.getInstance(project).runReadActionInSmartMode<PsiType?> {
                    JavaPsiFacade.getElementFactory(project).createTypeFromText(key, null)
                }
            }
            CachedValueProvider.Result.createSingleDependency(
                map,
                ProjectRootManager.getInstance(project)
            )
        }
        return cache[className]
    }

    fun getFieldPath(context: PsiElement): Pair<String, String>? {
        val anno = getContainingAnnotationEntry(context.toUElement())?.first ?: return null
        val qualifiedName = anno.qualifiedName ?: return null
        var attrName = PsiTreeUtil.getParentOfType(context, PsiNameValuePair::class.java)?.attributeName
        if (attrName == null && PluginChecker.getInstance().kotlin()) {
            val valueArgument = PsiTreeUtil.getParentOfType(context, KtValueArgument::class.java) ?: return null
            attrName = valueArgument.getArgumentName()?.text ?: "value"
        }
        attrName ?: return null
        return Pair(qualifiedName, attrName)
    }

    private fun isValidFieldName(fieldName: String?): Boolean {
        return when (fieldName) {
            null -> false
            else -> fieldNameRegex.matches(fieldName)
        }
    }

    private fun getLocalMetaConfigKeys(project: Project): HashMap<String, SpELInfo> {
        return CachedValuesManager.getManager(project).getCachedValue(project) {
            val dependencies = SmartList<Any>(PsiModificationTracker.MODIFICATION_COUNT)
            val allKeys = HashMap<String, SpELInfo>()
            for (module in ModuleManager.getInstance(project).modules) {
                for (sourceRoot in ModuleRootManager.getInstance(module).sourceRoots) {
                    sourceRoot.findFileByRelativePath(ConfigJsonUtil.FILENAME)?.let {
                        dependencies.add(it)
                        parseConfig(it, allKeys)
                    }
                }
            }
            return@getCachedValue CachedValueProvider.Result.create(
                allKeys, dependencies
            )
        }
    }

    private fun parseConfig(file: VirtualFile, collector: HashMap<String, SpELInfo>) {
        val text = LoadTextUtil.loadText(file)
        val info = ConfigJsonUtil.parseSpELInfo(text) ?: return
        info.forEach { (_, v) ->
            if (!isValidFieldName(v.method.resultName)) {
                v.method.resultName = SpELConst.methodResultNameDefault
            }
            v.fields.forEach { (k, _) ->
                if (!isValidFieldName(k)) {
                    v.fields.remove(k)
                }
            }
            v.method.parametersPrefix?.removeIf { !isValidFieldName(it) }
        }
        collector.putAll(info)
    }

    private fun getLibrariesConfigKeys(project: Project): HashMap<String, SpELInfo> {
        return CachedValuesManager.getManager(project).getCachedValue(project) {
            val metaInfConfigFiles = findConfigFiles(project)
            val allKeys = HashMap<String, SpELInfo>()

            for (psiFile in metaInfConfigFiles) {
                parseConfig(psiFile.virtualFile, allKeys)
            }

            CachedValueProvider.Result.create(
                allKeys, PsiModificationTracker.MODIFICATION_COUNT
            )
        }
    }

    private fun findConfigFiles(project: Project): List<PsiFile> {
        val configFiles =
            FilenameIndex.getVirtualFilesByName(ConfigJsonUtil.FILENAME, ProjectScope.getLibrariesScope(project))
        if (configFiles.isEmpty()) {
            return emptyList()
        }
        return ContainerUtil.findAll(configFiles, PsiFile::class.java)
    }
}