package oneclick.client.apps.user.core

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        val entrypoint = (application as OneClickApplication).entrypoint

        setContent {
            entrypoint.App()
        }
    }
}
