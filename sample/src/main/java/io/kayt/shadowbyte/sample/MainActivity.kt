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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import io.kayt.shadowbyte.Shadowbyte.Property
import io.kayt.shadowbyte.Shadowbyte.Shadowed
import io.kayt.shadowbyte.sample.theme.ShadowbyteTheme
import io.kayt.shadowbyte.shadow

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
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
        shadow<TabRowParameter> {
            ScrollableTabRowMinimumTabWidth = 0f
            ScrollableTabRow(0) {
                repeat(100) {
                    Tab(selected = false, onClick = {}) {
                        Text("salam")
                    }
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

@Shadowed(qualifiedName = "androidx.compose.material3.TabRowKt")
interface TabRowParameter {
    @Property(name = "ScrollableTabRowMinimumTabWidth")
    var ScrollableTabRowMinimumTabWidth: Float
}