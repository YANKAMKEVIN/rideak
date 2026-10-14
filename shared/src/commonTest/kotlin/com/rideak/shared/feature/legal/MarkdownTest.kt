package com.rideak.shared.feature.legal

import kotlin.test.Test
import kotlin.test.assertEquals

class MarkdownTest {
    @Test
    fun blocks() {
        val doc = "# Titre\n\nUn paragraphe.\n\n## Section\n\n- point un\n- point deux\n"
        assertEquals(
            listOf(
                MarkdownBlock.Title("Titre"), MarkdownBlock.Paragraph("Un paragraphe."), MarkdownBlock.Heading("Section"),
                MarkdownBlock.Bullet("point un"), MarkdownBlock.Bullet("point deux"),
            ),
            parseMarkdown(doc),
        )
    }

    @Test
    fun inlineBoldAndLinks() {
        assertEquals(
            listOf(Span("Voir "), Span("ODbL", url = "https://opendatacommons.org/licenses/odbl/1-0/"), Span(" et "), Span("ceci", bold = true), Span(".")),
            parseInline("Voir [ODbL](https://opendatacommons.org/licenses/odbl/1-0/) et **ceci**."),
        )
        assertEquals(listOf(Span("[à compléter : adresse e-mail]")), parseInline("[à compléter : adresse e-mail]"), "a placeholder is not a link")
    }
}
