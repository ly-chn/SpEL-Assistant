package kim.nzxy.spel

import com.intellij.microservices.config.MetaConfigKey
import com.intellij.microservices.config.MetaConfigKeyManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.fileEditor.impl.LoadTextUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.IndexNotReadyException
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.OrderEnumerator
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VFileProperty
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.spring.boot.application.metadata.SpringBootApplicationMetaConfigKeyManagerImpl
import com.intellij.spring.model.utils.SpringCommonUtils
import com.intellij.util.ArrayUtil
import com.intellij.util.Processors
import com.intellij.util.SmartList
import com.intellij.util.containers.ContainerUtil
import org.yaml.snakeyaml.Yaml

/**
 * @author ly-chn
 * @since 2024/1/4 10:07
 */
@Service
class YamlConfigService {
    companion object {
        fun getInstance(): YamlConfigService {
            return ApplicationManager.getApplication().getService(YamlConfigService::class.java)
        }
    }


    fun getAllMetaConfigKeys(module: Module): List<MetaConfigKey> {
        // val fromLibraries= getLibrariesConfigKeys(module)
        val localKeys = getLocalMetaConfigKeys(module)
        // return ContainerUtil.concat(fromLibraries, localKeys)
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

    private fun getLocalMetaConfigKeys(localModule: Module?): List<MetaConfigKey> {
        return CachedValuesManager.getManager(localModule!!.project).getCachedValue(localModule) {
            val allModules = LinkedHashSet<Module>()
            ModuleUtilCore.getDependencies(localModule, allModules)
            val unitTestMode =
                ApplicationManager.getApplication().isUnitTestMode
            val dependencies =
                SmartList<Any>(PsiModificationTracker.MODIFICATION_COUNT)
            val allKeys: MutableList<MetaConfigKey> = ArrayList()
            for (module in allModules) {
                val localJsonFile = findMetadataFileForModule(module)
                if (localJsonFile != null) {
                    val keys: List<MetaConfigKey> = ArrayList()
                    val collect = Processors.cancelableCollectProcessor(keys)

                    try {
                        // val parser = SpringBootConfigurationMetadataParser(module, localJsonFile)
                        // parser.processKeys(module, collect)
                        parseConfig(localJsonFile)
                    } catch (var11: IndexNotReadyException) {
                        throw var11
                    } catch (var11: ProcessCanceledException) {
                        throw var11
                    } catch (e: Throwable) {
                        thisLogger().warn("Error parsing " + localJsonFile.path, e)
                    }

                    allKeys.addAll(keys)
                    ContainerUtil.addIfNotNull(dependencies,localJsonFile)
                }
            }
            return@getCachedValue CachedValueProvider.Result.create(
                allKeys,
                *ArrayUtil.toObjectArray(dependencies)
            )
        }
    }

    private fun parseConfig(file: VirtualFile) {
        val text = LoadTextUtil.loadText(file)
        val yaml = Yaml()
        println()
    }

/*    private fun getLocalMetaConfigKeys(localModule: Module?): List<MetaConfigKey> {
        return CachedValuesManager.getManager(localModule!!.project).getCachedValue(localModule) {
            val allModules = LinkedHashSet<Module>()
            ModuleUtilCore.getDependencies(localModule, allModules)
            val unitTestMode =
                ApplicationManager.getApplication().isUnitTestMode
            val dependencies =
                SmartList(PsiModificationTracker.MODIFICATION_COUNT)
            val allKeys: MutableList<MetaConfigKey> = ArrayList()
            for (module in allModules) {
                val moduleOrderEnumerator = OrderEnumerator.orderEntries(module)
                // moduleOrderEnumerator.source
                val localJsonFile = MetaConfigKeyManager.findLocalMetadataJsonFile(
                    module,
                    "spring-configuration-metadata.json",
                    unitTestMode
                )
                if (localJsonFile != null) {
                    val keys: List<MetaConfigKey> = ArrayList()
                    val collect = Processors.cancelableCollectProcessor(keys)

                    try {
                        val parser =
                            SpringBootConfigurationMetadataParser(module, localJsonFile)
                        parser.processKeys(module, collect)
                    } catch (var11: IndexNotReadyException) {
                        throw var11
                    } catch (var11: ProcessCanceledException) {
                        throw var11
                    } catch (var12: Throwable) {
                        SpringBootApplicationMetaConfigKeyManagerImpl.LOG.warn(
                            "Error parsing " + localJsonFile.path,
                            var12
                        )
                    }

                    allKeys.addAll(keys)
                    ContainerUtil.addIfNotNull(
                        dependencies,
                        LocalFileSystem.getInstance().findFileByIoFile(localJsonFile)
                    )
                }
            }
            return@getCachedValue CachedValueProvider.Result.create(
                allKeys,
                *ArrayUtil.toObjectArray(dependencies)
            )
        }
    }*/

/*    private fun getLibrariesConfigKeys(module: Module): MutableList<MetaConfigKey> {
        return CachedValuesManager.getManager(module.project).getCachedValue(module) {
            val metaInfConfigFiles =
                SpringCommonUtils.findConfigFilesInMetaInf(
                    module, true, "spring-configuration-metadata.json",
                    PsiFile::class.java
                )
            val allKeys: MutableList<MetaConfigKey> = ArrayList()
            val var3: Iterator<*> = metaInfConfigFiles.iterator()

            while (var3.hasNext()) {
                val configMetadataFile = var3.next() as PsiFile
                val keys = getConfigKeysForFile(module, configMetadataFile)
                allKeys.addAll(keys)
            }
            CachedValueProvider.Result.create(
                allKeys, PsiModificationTracker.MODIFICATION_COUNT
            )
        }
    }*/
    /*    private fun getLibrariesConfigKeys(module: Module): MutableList<MetaConfigKey> {
            return CachedValuesManager.getManager(module.project).getCachedValue(module) {
                val metaInfConfigFiles =
                    SpringCommonUtils.findConfigFilesInMetaInf(
                        module, true, "spring-configuration-metadata.json",
                        PsiFile::class.java
                    )
                val allKeys: MutableList<MetaConfigKey> = ArrayList()
                val var3: Iterator<*> = metaInfConfigFiles.iterator()

                while (var3.hasNext()) {
                    val configMetadataFile = var3.next() as PsiFile
                    val keys =
                        getConfigKeysForFile(module, configMetadataFile)
                    allKeys.addAll(keys)
                }
                CachedValueProvider.Result.create(
                    allKeys,PsiModificationTracker.MODIFICATION_COUNT
                )
            }
        }

        private fun getConfigKeysForFile(module: Module, jsonFile: PsiFile): List<MetaConfigKey> {
            val keys: List<MetaConfigKey> = ArrayList()
            val collect = Processors.cancelableCollectProcessor(keys)

            try {
                val parser = SpringBootConfigurationMetadataParser(jsonFile)
                parser.processKeys(module, collect)
            } catch (var5: IndexNotReadyException) {
                throw var5
            } catch (var5: ProcessCanceledException) {
                throw var5
            } catch (var6: Throwable) {
                thisLogger().warn("Error parsing " + jsonFile.virtualFile.path, var6)
            }

            return keys
        }

*/
}