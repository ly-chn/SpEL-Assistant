package kim.nzxy.spel

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.extensions.PluginId

/**
 * @author ly-chn
 * @since 2024/1/11 15:51
 */
@Service
class PluginChecker {

    private val kotlinPluginId = PluginId.findId("org.jetbrains.kotlin")

    private var kotlinPluginEnabled: Boolean? = null


    companion object {
        fun getInstance(): PluginChecker {
            return ApplicationManager.getApplication().getService(PluginChecker::class.java)
        }
    }

    fun kotlin(): Boolean {
        if (kotlinPluginEnabled == null) {
            kotlinPluginEnabled = PluginManagerCore.getPlugin(kotlinPluginId)?.isEnabled == true
        }
        return kotlinPluginEnabled!!
    }
}