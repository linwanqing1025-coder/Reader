package io.lin.reader.ui.components.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.lin.reader.R
import io.lin.reader.ui.components.textfield.StyledOutlinedTextField

/**
 * 文本输入对话框
 * @param title 对话框标题
 * @param placeholder 占位文本
 * @param labelText  标签提示文本
 * @param errorText  错误提示文本
 * @param value
 * @param onDismiss 取消回调（关闭对话框）
 * @param onConfirm 确定回调（传递输入的系列名）
 * */
@Preview
@Composable
fun DialogWithTextField(
    modifier: Modifier = Modifier,
    title: String = "Default Dialog Title",
    value: String = "",
    placeholder: String = "Default Placeholder",
    labelText: String = "Default Label Text",
    errorText: String = "Default Error Text",
    onValueChange: (String) -> Unit = {},
    validateInput: () -> Boolean = { true },
    onDismiss: () -> Unit = {},
    onConfirm: () -> Unit = {}, // 提供输入的系列名
) {
    // 对话框主体
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = true
        )
    ) {
        Surface(
            modifier = modifier.width(dimensionResource(R.dimen.dialog_size)),
            shape = RoundedCornerShape(
                dimensionResource(R.dimen.composable_rounded_corner_radius)
                        + dimensionResource(R.dimen.inner_padding_of_container)
            ),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(dimensionResource(R.dimen.inner_padding_of_container)),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. 对话框标题
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(
                        bottom = dimensionResource(R.dimen.inner_padding_of_container)
                    )
                )
                // 2.输入框
                StyledOutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = value,
                    placeholder = placeholder,
                    labelText = labelText,
                    errorText = errorText,
                    onValueChange = { onValueChange(it) },
                    validateInput = validateInput
                )

                // 3. 取消/确定按钮
                Row(
                    modifier = Modifier
                        .padding(top = dimensionResource(R.dimen.inner_padding_of_container))
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // 取消按钮
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(
                            dimensionResource(R.dimen.composable_rounded_corner_radius)
                        ),
                        modifier = Modifier
                            .height(40.dp)
                            .height(IntrinsicSize.Min)
                    ) {
                        Text(text = stringResource(R.string.cancel), fontSize = 16.sp)
                    }
                    // 确定按钮（输入为空则禁用）
                    Button(
                        onClick = onConfirm,
                        enabled = validateInput(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(
                            dimensionResource(R.dimen.composable_rounded_corner_radius)
                        ),
                        modifier = Modifier
                            .height(40.dp)
                            .height(IntrinsicSize.Min)
                    ) {
                        Text(text = stringResource(R.string.confirm), fontSize = 16.sp)
                    }
                }
            }
        }
    }
}