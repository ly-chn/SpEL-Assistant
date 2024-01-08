package kim.nzxy.spel

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.fileEditor.impl.LoadTextUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.VFileProperty
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiFile
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PackageScope
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.util.SmartList
import com.intellij.util.containers.ContainerUtil
import kim.nzxy.spel.json.ConfigJsonUtil
import kim.nzxy.spel.json.SpELInfo

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


    fun getAllMetaConfigKeys(module: Module): List<SpELInfo> {
        // val fromLibraries = getLibrariesConfigKeys(project)
        // val localKeys = getLocalMetaConfigKeys(project)

        // return ContainerUtil.concat(fromLibraries, localKeys)
        val dependencyModuleNames = ModuleRootManager.getInstance(module).dependencyModuleNames
        return SmartList()
    }

    private fun findMetadataFile(root: VirtualFile): VirtualFile? {
        if (!root.`is`(VFileProperty.SYMLINK)) {
            for (child in root.children) {
                if (child.name == "spel-extension.yml") {
                    return child
                }
            }
        }
        return null
    }

    private fun findMetadataFileForModule(module: Module): VirtualFile? {
        for (sourceRoot in ModuleRootManager.getInstance(module)
            .sourceRoots) {
            return sourceRoot.findFileByRelativePath("spel-extension.yml") ?: continue
        }
        return null
    }

    private fun getLocalMetaConfigKeys(localModule: Module?): HashMap<String, SpELInfo> {
        return CachedValuesManager.getManager(localModule!!.project).getCachedValue(localModule) {

            val allModules = LinkedHashSet<Module>()
            ModuleUtilCore.getDependencies(localModule, allModules)
            val dependencies =
                SmartList<Any>(PsiModificationTracker.MODIFICATION_COUNT)
            val allKeys = HashMap<String, SpELInfo>()
            for (module in allModules) {
                val localJsonFile = findMetadataFileForModule(module)
                if (localJsonFile != null) {
                    parseConfig(localJsonFile, allKeys)
                    ContainerUtil.addIfNotNull(dependencies, localJsonFile)
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
        println("parse info from file ${file.name}, value is: $info")
        collect.putAll(info)
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
        var scope = GlobalSearchScope.moduleRuntimeScope(module, false)
        val metaInfPackage = JavaPsiFacade.getInstance(module.project).findPackage("META-INF")
        if (metaInfPackage == null) {
            return emptyList()
        } else {
            val packageScope = PackageScope.packageScope(metaInfPackage, false)
            scope = scope.intersectWith(packageScope)
        }
        val configFiles = FilenameIndex.getFilesByName(module.project, "spel-extension.json", scope)
        if (configFiles.isEmpty()) {
            return emptyList()
        }
        return ContainerUtil.findAll(configFiles, PsiFile::class.java)
    }
}