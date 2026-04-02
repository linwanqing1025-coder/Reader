package io.lin.reader.ui.components.textfield

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.lin.reader.R

/**系列名文本输入框
 * @param value 外部传递文本框内容
 * @param placeholder 占位文本
 * @param labelText  标签提示文本
 * @param errorText  错误提示文本
 * @param onValueChange 外部执行文本框内容的变化（inputSeriesName = it）
 * @param validateInput 校验输入合法性，仅需传入对字符的要求，错误处理在组件内部已完成
 * */
@Preview
@Composable
fun StyledOutlinedTextField(
    modifier: Modifier = Modifier,
    value: String = "",
    placeholder: String = "Default Placeholder",
    labelText: String = "Default Label Text",
    errorText: String = "Default Error Text",
    onValueChange: (String) -> Unit = {},
    validateInput: () -> Boolean = { true }
) {
    // 输入错误
    var isNameError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    // 聚焦状态
    var isFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    // 文本框
    OutlinedTextField(
        value = value,
        onValueChange = {
            onValueChange(it)
            // 输入时清除错误提示
            if (isNameError) {
                isNameError = false
                errorMessage = ""
            }
        },
        modifier = modifier
            .focusRequester(focusRequester)
            .onFocusChanged { isFocused = it.isFocused },
        placeholder = { Text(text = placeholder) },
        label = { Text(text = labelText) },
        singleLine = true,
        maxLines = 1,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Words,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                if (validateInput()) {
                    isNameError = false
                    errorMessage = ""
                    focusManager.clearFocus()
                } else {
                    isNameError = true
                    errorMessage = errorText
                }
            }
        ),
        supportingText = {
            if (isNameError) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Error,
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        },
        isError = isNameError,
        shape = RoundedCornerShape(dimensionResource(R.dimen.composable_rounded_corner_radius)),
        colors = OutlinedTextFieldDefaults.colors(
            // 边框
            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            errorBorderColor = MaterialTheme.colorScheme.error,
            // 键入的文字
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            focusedTextColor = MaterialTheme.colorScheme.primary,
            errorTextColor = MaterialTheme.colorScheme.error,
            // 占位符（Text）
            unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(
                alpha = 0.8f
            ),
            focusedPlaceholderColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
            // 标签提示文字
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            errorLabelColor = MaterialTheme.colorScheme.error,
        ),
    )
}