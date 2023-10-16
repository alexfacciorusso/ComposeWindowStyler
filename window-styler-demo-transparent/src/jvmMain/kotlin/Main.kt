import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.application
import com.mayakapps.compose.windowstyler.NativeLookWindow
import com.mayakapps.compose.windowstyler.WindowBackdrop

@Composable
@Preview
fun App() {
    Button(onClick = {}) {
        Text("Button")
    }
}

fun main() = application {
    NativeLookWindow(
        onCloseRequest = ::exitApplication,
        title = "Compose Window Styler Demo",
        preferredBackdropType = WindowBackdrop.Mica(isSystemInDarkTheme()),
        manageTitlebar = true
    ) {
        MaterialTheme {
            App()
        }
    }
}
