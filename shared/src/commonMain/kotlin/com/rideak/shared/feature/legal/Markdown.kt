package com.rideak.shared.feature.legal

/**
 * The tiny Markdown subset of the legal documents (the .md files in files/legal), which must stay readable as
 * plain files and publishable on the web: `# ` and `## ` headings, `- ` bullets, paragraphs, and,
 * inline, `**bold**` and `[text](url)`.
 */
sealed interface MarkdownBlock {
    data class Title(val text: String) : MarkdownBlock
    data class Heading(val text: String) : MarkdownBlock
    data class Bullet(val text: String) : MarkdownBlock
    data class Paragraph(val text: String) : MarkdownBlock
}

fun parseMarkdown(text: String): List<MarkdownBlock> = text.lines().map { it.trim() }.filter { it.isNotEmpty() }.map { line ->
    when {
        line.startsWith("## ") -> MarkdownBlock.Heading(line.removePrefix("## "))
        line.startsWith("# ") -> MarkdownBlock.Title(line.removePrefix("# "))
        line.startsWith("- ") -> MarkdownBlock.Bullet(line.removePrefix("- "))
        else -> MarkdownBlock.Paragraph(line)
    }
}

/** A run of inline text: plain, bold, or a link. */
data class Span(val text: String, val bold: Boolean = false, val url: String? = null)

private val inline = Regex("""\*\*(.+?)\*\*|\[(.+?)]\((https?://[^)\s]+)\)""")

fun parseInline(text: String): List<Span> {
    val spans = mutableListOf<Span>()
    var last = 0
    for (match in inline.findAll(text)) {
        if (match.range.first > last) spans += Span(text.substring(last, match.range.first))
        val (bold, label, url) = match.destructured
        spans += if (bold.isNotEmpty()) Span(bold, bold = true) else Span(label, url = url)
        last = match.range.last + 1
    }
    if (last < text.length) spans += Span(text.substring(last))
    return spans
}
