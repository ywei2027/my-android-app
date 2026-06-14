package com.example.app.feature.register.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.dp

@Composable
fun AgreementRow(
    accepted: Boolean,
    onToggle: (Boolean) -> Unit,
    onNavigateAgreement: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onToggle(!accepted) }
            .semantics { contentDescription = "同意用户协议" }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = accepted,
            onCheckedChange = null,
            modifier = Modifier
                .size(48.dp)
                .semantics { testTag = "checkbox_agreement" },
            colors = CheckboxDefaults.colors()
        )
        Text(
            text = "我已阅读并同意",
            style = MaterialTheme.typography.bodyMedium
        )
        TextButton(onClick = onNavigateAgreement) {
            Text("《用户协议》")
        }
    }
}
