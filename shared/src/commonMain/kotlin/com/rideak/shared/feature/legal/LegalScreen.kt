package com.rideak.shared.feature.legal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.rideak.shared.designsystem.icons.RideakIcons
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.rideak.shared.designsystem.RideakTheme
import com.rideak.shared.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

/** A legal document bundled as Markdown (privacy policy, credits), with a back arrow. */
@OptIn(ExperimentalResourceApi::class)
@Composable
fun LegalScreen(file: String, onBack: () -> Unit) {
    var blocks by remember(file) { mutableStateOf<List<MarkdownBlock>>(emptyList()) }
    LaunchedEffect(file) { blocks = parseMarkdown(Res.readBytes(file).decodeToString()) }
    LegalDocument(blocks, onBack)
}

@Composable
fun LegalDocument(blocks: List<MarkdownBlock>, onBack: () -> Unit) {
    val c = RideakTheme.colors
    LazyColumn(
        Modifier.fillMaxSize().background(c.night).windowInsetsPadding(WindowInsets.safeDrawing),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(blocks) { block ->
            when (block) {
                is MarkdownBlock.Title -> Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(RideakIcons.Back, contentDescription = "Retour", tint = c.textPrimary)
                    }
                    Text(block.text, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = c.textPrimary, modifier = Modifier.semantics { heading() })
                }
                is MarkdownBlock.Heading -> Text(
                    block.text,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = c.textPrimary,
                    modifier = Modifier.padding(top = 12.dp).semantics { heading() },
                )
                is MarkdownBlock.Bullet -> Row {
                    Text("•", style = MaterialTheme.typography.bodyLarge, color = c.reward, modifier = Modifier.padding(end = 8.dp))
                    Text(rich(block.text), style = MaterialTheme.typography.bodyLarge, color = c.textSecondary)
                }
                is MarkdownBlock.Paragraph -> Text(rich(block.text), style = MaterialTheme.typography.bodyLarge, color = c.textSecondary)
            }
        }
    }
}

@Composable
private fun rich(text: String): AnnotatedString {
    val c = RideakTheme.colors
    val linkStyle = TextLinkStyles(SpanStyle(color = c.exploration, textDecoration = TextDecoration.Underline))
    return buildAnnotatedString {
        parseInline(text).forEach { span ->
            when {
                span.url != null -> withLink(LinkAnnotation.Url(span.url, linkStyle)) { append(span.text) }
                span.bold -> withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = c.textPrimary)) { append(span.text) }
                else -> append(span.text)
            }
        }
    }
}
