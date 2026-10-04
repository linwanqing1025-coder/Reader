package io.lin.reader.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview(showBackground = true)
@Composable
fun SelectionContainerDemo() {
    // SelectionContainer 的作用是让其内部的 Text 组件（或包含文本的组合）变得可以被长按选择、拖拽和复制。
    // 默认情况下，Compose 的 Text 是一个完全静态的绘制元素，不可交互。
    SelectionContainer {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("这段文字可以被长按选中和复制。")
            Text("甚至跨越了多个 Text 组件的段落，它们也可以被一起划词选中！")
            
            // DisableSelection 可以用来在允许选中的区域内，排除某些特定的不可选元素
            DisableSelection {
                Text(
                    text = "（但这段备注文字被特意排除了，它是选中不了的）",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}