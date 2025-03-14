package kim.nzxy.spel.service

import com.intellij.openapi.application.NonBlockingReadAction
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.ModificationTracker
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.containers.ConcurrentFactoryMap
import kim.nzxy.spel.PluginChecker
import kim.nzxy.spel.SpELConst
import kim.nzxy.spel.json.ConfigJsonUtil
import kim.nzxy.spel.json.SpELInfo
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.uast.getContainingAnnotationEntry
import org.jetbrains.uast.toUElement
import java.util.concurrent.atomic.AtomicLong

/**
 * @author ly-chn
 * @since 2024-08-27
 */
@Service(Service.Level.PROJECT)
class SpELJsonConfigService(private val project: Project) : ModificationTracker {
    companion object {
        private val fieldNameRegex = Regex("^[a-zA-Z_]\\w*$")
    }

    private val changeTimes = AtomicLong(0)

    init {
        PsiManager.getInstance(project)
            .addPsiTreeChangeListener(object : PsiTreeAnyChangeAbstractAdapter() {
                override fun onChange(file: PsiFile?) {
                    if (file != null) {
                        val filename = file.viewProvider.virtualFile.name
                        if (ConfigJsonUtil.isSpELFilename(filename)) {
                            changeTimes.incrementAndGet()
                        }
                    }
                }
            }, project)
    }

    override fun getModificationCount() = changeTimes.get()


    fun getSpELInfo(fieldPath: String): SpELInfo? {
        return getLocalMetaConfigKeys()[fieldPath] ?: getLibrariesConfigKeys()[fieldPath]
    }

    fun findPsiType(className: String): PsiType? {
        val cache: Map<String, PsiType?> = CachedValuesManager.getManager(project).getCachedValue(project) {
            val map: Map<String, PsiType?> = ConcurrentFactoryMap.createMap { key: String ->
                JavaPsiFacade.getElementFactory(project).createTypeFromText(key, null)
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

    private fun getLocalMetaConfigKeys(): HashMap<String, SpELInfo> {
        return CachedValuesManager.getManager(project).getCachedValue(project) {
            val allKeys = HashMap<String, SpELInfo>()
            for (module in ModuleManager.getInstance(project).modules) {
                for (sourceRoot in ModuleRootManager.getInstance(module).sourceRoots) {
                    sourceRoot.findFileByRelativePath(ConfigJsonUtil.FILENAME)?.let {
                        parseConfig(it, allKeys)
                    }
                }
            }
            return@getCachedValue CachedValueProvider.Result.create(allKeys, this)
        }
    }


    private fun getLibrariesConfigKeys(): HashMap<String, SpELInfo> {
        return CachedValuesManager.getManager(project).getCachedValue(project) {
            val metaInfConfigFiles = findConfigFiles()
            val allKeys = HashMap<String, SpELInfo>()
            for (file in metaInfConfigFiles) {
                parseConfig(file, allKeys)
            }
            CachedValueProvider.Result.create(
                allKeys, this
            )
        }
    }


    private fun parseConfig(file: VirtualFile, collector: HashMap<String, SpELInfo>) {
        val psiFile = file.toPsiFile(project) ?: return
        val text = psiFile.text
        val info = ConfigJsonUtil.parseSpELInfo(text) ?: return
        info.forEach { (_, v) ->
            if (!isValidFieldName(v.method?.resultName)) {
                v.method?.resultName = SpELConst.methodResultNameDefault
            }
            v.fields?.entries?.removeIf { !isValidFieldName(it.key) }
            v.method?.parametersPrefix?.removeIf { !isValidFieldName(it) }
            v.sourceFile = psiFile
        }
        collector.putAll(info)
    }

    private fun findConfigFiles(): List<VirtualFile> {
        return FilenameIndex.getVirtualFilesByName(ConfigJsonUtil.FILENAME, ProjectScope.getLibrariesScope(project))
            .filter { !it.path.contains("-sources.jar!") }
    }
}