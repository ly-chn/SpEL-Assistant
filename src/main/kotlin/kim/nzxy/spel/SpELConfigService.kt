package kim.nzxy.spel

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.fileEditor.impl.LoadTextUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.VFileProperty
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.util.SmartList
import com.intellij.util.containers.ContainerUtil
import kim.nzxy.spel.json.ConfigJsonUtil
import kim.nzxy.spel.json.SpELInfo
import kotlinx.coroutines.flow.merge

/**
 * @author ly-chn
 * @since 2024/1/4 10:07
 */
@Service
class SpELConfigService {
    companion object {
        fun getInstance(): SpELConfigService {
            return ApplicationManager.getApplication().getService(SpELConfigService::class.java)
        }
    }


    fun getAllMetaConfigKeys(module: Module): HashMap<String, SpELInfo> {
        val fromLibraries = getLibrariesConfigKeys(module)
        val localKeys = getLocalMetaConfigKeys(module.project)
        val res = HashMap<String, SpELInfo>()
        res.putAll(fromLibraries)
        localKeys.forEach { (k, v) -> res[k] = mergeSpELInfo(v, res[k]) }
        return res
    }

    private fun getLocalMetaConfigKeys(project: Project): HashMap<String, SpELInfo> {
        return CachedValuesManager.getManager(project).getCachedValue(project) {
            val dependencies = SmartList<Any>(PsiModificationTracker.MODIFICATION_COUNT)
            val allKeys = HashMap<String, SpELInfo>()
            for (module in ModuleManager.getInstance(project).modules) {
                for (sourceRoot in ModuleRootManager.getInstance(module).sourceRoots) {
                    sourceRoot.findFileByRelativePath("spel-extension.json").let {
                        if (it != null) {
                            dependencies.add(it)
                            parseConfig(it, allKeys)
                        }
                    }
                }
            }
            return@getCachedValue CachedValueProvider.Result.create(
                allKeys, dependencies
            )
        }
    }

    private fun parseConfig(file: VirtualFile, collect: HashMap<String, SpELInfo>) {
        val text = LoadTextUtil.loadText(file)
        val info = ConfigJsonUtil.parseSpELInfo(text) ?: return
        info.forEach { (k, v) ->
            collect[k] = mergeSpELInfo(v, collect[k])
        }
    }

    private fun mergeSpELInfo(first: SpELInfo, second: SpELInfo?): SpELInfo {
        if (second == null) {
            return first
        }
        first.fields.putAll(second.fields)
        first.method.result = first.method.result || second.method.result
        first.method.parameters = first.method.parameters || second.method.parameters
        first.method.resultName = first.method.resultName ?: second.method.resultName
        first.method.parametersPrefix?.addAll(second.method.parametersPrefix ?: emptySet())
        val prefix = first.method.parametersPrefix ?: second.method.parametersPrefix
        first.method.parametersPrefix = prefix
        return first
    }

    private fun getLibrariesConfigKeys(module: Module): HashMap<String, SpELInfo> {
        return CachedValuesManager.getManager(module.project).getCachedValue(module) {
            val metaInfConfigFiles = findConfigFiles(module)
            val allKeys = HashMap<String, SpELInfo>()

            for (psiFile in metaInfConfigFiles) {
                parseConfig(psiFile.virtualFile, allKeys)
            }

            CachedValueProvider.Result.create(
                allKeys, PsiModificationTracker.MODIFICATION_COUNT
            )
        }
    }

    private fun findConfigFiles(module: Module): List<PsiFile> {
        val configFiles = FilenameIndex.getFilesByName(
            module.project,
            "spel-extension.json",
            ProjectScope.getLibrariesScope(module.project)
        )
        if (configFiles.isEmpty()) {
            return emptyList()
        }
        return ContainerUtil.findAll(configFiles, PsiFile::class.java)
    }
}