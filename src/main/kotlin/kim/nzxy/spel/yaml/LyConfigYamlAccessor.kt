package kim.nzxy.spel.yaml

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.IncorrectOperationException
import com.intellij.util.ObjectUtils
import org.jetbrains.yaml.YAMLElementGenerator
import org.jetbrains.yaml.YAMLUtil
import org.jetbrains.yaml.psi.*

/**
 * @author ly-chn
 * @since 2023/12/27 10:54
 */
class LyConfigYamlAccessor(context: PsiElement?) {
    private val myDocument: YAMLDocument? = PsiTreeUtil.getNonStrictParentOfType<PsiElement>(
        context, YAMLDocument::class.java
    ) as YAMLDocument?


    private fun isValid(): Boolean {
        return this.myDocument != null
    }

    fun findExistingKey(qualifiedKey: String): YAMLKeyValue? {
        if (!this.isValid()) return null
        if (StringUtil.isEmptyOrSpaces(qualifiedKey)) return null
        var searchEle = myDocument!!.topLevelValue
        val key = StringUtil.split(qualifiedKey, ".")

        for (i in key.indices) {
            if (searchEle !is YAMLMapping) {
                return null
            }

            val relaxedChild = findChildRelaxed(
                searchEle,
                key[i]
            )
            if (relaxedChild == null || i + 1 == key.size) {
                return relaxedChild
            }

            searchEle = relaxedChild.value
        }

        throw IllegalStateException("Should have returned from the loop '$qualifiedKey'")
    }

    @JvmOverloads
    @Throws(IncorrectOperationException::class)
    fun create(qualifiedKey: String, inWriteAction: Boolean = true): YAMLKeyValue? {
        if (!this.isValid()) {
            return null
        }
        val keyParts = StringUtil.split(qualifiedKey, ".")
        val generator = YAMLElementGenerator.getInstance(myDocument!!.project)
        val topLevelValue = myDocument.topLevelValue
        val writeRunnable: Runnable
        if (topLevelValue is YAMLMapping) {
            var topMostExistingMapping: YAMLMapping? = topLevelValue
            var topMostExistingKey: YAMLKeyValue? = null
            var foundHierarchies = 0

            var chainedKey: String
            for (keyPart in keyParts) {
                chainedKey = keyPart
                if (topMostExistingMapping == null) {
                    break
                }

                topMostExistingKey = findChildRelaxed(topMostExistingMapping, chainedKey)
                if (topMostExistingKey == null) {
                    break
                }

                topMostExistingMapping = ObjectUtils.tryCast(topMostExistingKey.value, YAMLMapping::class.java)
                ++foundHierarchies
            }

            if (foundHierarchies == keyParts.size) {
                throw IncorrectOperationException("key exists already: $qualifiedKey\n${myDocument.text}")
            }

            assert(topMostExistingKey != null || topMostExistingMapping != null)
            val indent = if (topMostExistingMapping != null) {
                YAMLUtil.getIndentToThisElement(topMostExistingMapping)
            } else {
                YAMLUtil.getIndentToThisElement(topMostExistingKey!!) + 2
            }

            chainedKey =
                YAMLElementGenerator.createChainedKey(keyParts.subList(foundHierarchies, keyParts.size), indent)
            val dummyFile = generator.createDummyYamlWithText(chainedKey)
            val topLevelKeys = YAMLUtil.getTopLevelKeys(dummyFile)
            check(!topLevelKeys.isEmpty()) { "no top level keys (" + chainedKey + "): " + myDocument.text }

            val dummyKeyValue = topLevelKeys.iterator().next()
            checkNotNull(dummyKeyValue.parentMapping) {
                "no containing mapping for a kv (" + chainedKey + "): " + myDocument.text
            }

            writeRunnable = if (topMostExistingMapping == null) {
                Runnable { topMostExistingKey!!.setValue(dummyKeyValue.parentMapping!!) }
            } else {
                Runnable { topMostExistingMapping.putKeyValue(dummyKeyValue) }
            }
        } else {
            val dummyFile = generator.createDummyYamlWithText(YAMLElementGenerator.createChainedKey(keyParts, 0))
            val dummyValue = (dummyFile.documents[0] as YAMLDocument).topLevelValue!!

            writeRunnable = Runnable {
                if (topLevelValue == null) {
                    myDocument.add(dummyValue)
                } else {
                    topLevelValue.replace(dummyValue)
                }
            }
        }

        if (inWriteAction) {
            WriteCommandAction.runWriteCommandAction(myDocument.project, "Insert Key", null, writeRunnable)
        } else {
            writeRunnable.run()
        }

        return this.findExistingKey(qualifiedKey)
    }


    companion object {
        private fun findChildRelaxed(
            searchElement: YAMLMapping,
            subKey: String
        ): YAMLKeyValue? {
            searchElement.getKeyValueByKey(subKey)?.let { return it }
            for (value in searchElement.keyValues) {
                val name = value.name ?: continue
                if (ConfigYamlUtil.matchPrefix(subKey, name)){
                    return value
                }
            }
            return null
        }

    }
}
