package com.mayakapps.compose.windowstyler.windows

import androidx.compose.ui.awt.ComposeWindow
import com.mayakapps.compose.windowstyler.WindowBackdrop
import com.mayakapps.compose.windowstyler.WindowFrameStyle
import com.mayakapps.compose.windowstyler.WindowStyleManager
import com.mayakapps.compose.windowstyler.hackContentPane
import com.mayakapps.compose.windowstyler.setComposeLayerTransparency
import com.mayakapps.compose.windowstyler.windows.jna.Dwm
import com.mayakapps.compose.windowstyler.windows.jna.enums.DwmWindowAttribute
import com.sun.jna.platform.win32.WinDef.HWND
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.swing.SwingUtilities
import kotlin.properties.Delegates


/**
 * Windows implementation of [WindowStyleManager]. It is not recommended to use this class directly.
 *
 * If used on an OS other than Windows, it'll crash.
 */
class WindowsWindowStyleManager internal constructor(
    private val window: ComposeWindow,
    preferredBackdrop: WindowBackdrop,
    frameStyle: WindowFrameStyle,
    manageTitlebar: Boolean,
) : WindowStyleManager {
    private val hwnd: HWND = window.hwnd
    private var isApplied = false

    private val backdropApis = WindowsBackdropApis.install(hwnd)
    override var preferredBackdrop: WindowBackdrop by Delegates.observable(preferredBackdrop) { _, oldValue, _ ->
        if (!isApplied) return@observable

        backdrop.applyDiff(oldValue, hwnd, backdropApis)
    }

    override var frameStyle: WindowFrameStyle by Delegates.observable(frameStyle) { _, oldValue, newValue ->
        if (!isApplied) return@observable

        if (oldValue != newValue) {
            updateFrameStyle()
        }
    }

    override var manageTitlebar: Boolean = manageTitlebar
        get() = field
        set(value) {
            field = if (value) {
                true
            } else {
                // TODO: reset the window proc to the default one
                false
            }
        }

    private val backdrop: WindowBackdrop get() = preferredBackdrop.fallbackIfNotSupported()

    override suspend fun apply(): WindowBackdrop {
        withContext(Dispatchers.IO) {
            // invokeLater is called to make sure that ComposeLayer was initialized first
            SwingUtilities.invokeAndWait {
                // If the window is not already transparent, hack it to be transparent
                if (backdrop !is WindowBackdrop.Solid && !window.isTransparent) {
                    // For some reason, reversing the order of these two calls doesn't work.
                    window.setComposeLayerTransparency(true)
                    window.hackContentPane()
                }

                updateFrameStyle()
                backdrop.applyDiff(null, hwnd, backdropApis)

                // TODO
                val customDecorationWindowProc = if (manageTitlebar) {
                    CustomDecorationWindowProc.install(hwnd).also {
                        backdropApis.createSheetOfGlassEffect()
                    }
                } else null
            }
        }
        isApplied = true
        return backdrop
    }

    private fun updateFrameStyle() {
        if (windowsBuild < WIN11_BUILD_22000_21H2) {
            // Unsupported
            return
        }

        Dwm.setWindowCornerPreference(hwnd, frameStyle.cornerPreference.toDwmWindowCornerPreference())
        Dwm.setWindowAttribute(hwnd, DwmWindowAttribute.DWMWA_BORDER_COLOR, frameStyle.borderColor.toBgr())
        Dwm.setWindowAttribute(hwnd, DwmWindowAttribute.DWMWA_CAPTION_COLOR, frameStyle.titleBarColor.toBgr())
        Dwm.setWindowAttribute(hwnd, DwmWindowAttribute.DWMWA_TEXT_COLOR, frameStyle.captionColor.toBgr())
    }
}