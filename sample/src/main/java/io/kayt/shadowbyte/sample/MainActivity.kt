package io.kayt.shadowbyte.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import io.kayt.shadowbyte.Shadowbyte
import io.kayt.shadowbyte.sample.theme.ShadowbyteTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TabRowDefaults
            ShadowbyteTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        ScrollableTabRow(0) {
            repeat(100) {
                Tab(selected = false, onClick = {}) {
                    Text("salam")
                }
            }
        }
        Text(
            text = "Hello $name!",
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ShadowbyteTheme {
        Greeting("Android")
    }
}

object ComposeTabBar : Shadowbyte.Shadowed {

    @Shadowbyte.Property("androidx.compose.material3.TabRowKt.ScrollableTabRowMinimumTabWidth")
    var ScrollableTabRowMinimumTabWidth: Float = default()
}