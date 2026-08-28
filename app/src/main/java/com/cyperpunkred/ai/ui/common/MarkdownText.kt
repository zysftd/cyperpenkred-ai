package com.cyperpunkred.ai.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A lightweight, dependency-free Markdown renderer for the GM chat.
 *
 * Supported syntax (intentionally limited to what the GM prompt
 * actually emits):
 *   - `# ` / `## ` / `### `   headings
 *   - `**bold**`              bold
 *   - `*italic*` / `_italic_` italic
 *   - `` `inline code` ``     inline code
 *   - ` ``` ` ... ` ``` `     fenced code block
 *   - `> `                   blockquote
 *   - `- ` / `* `            unordered list
 *   - `1. `                  ordered list
 *   - `---`                  horizontal divider
 *   - blank line             paragraph break
 *
 * Anything that doesn't match falls through as plain text, so the
 * renderer is also safe to use for untrusted user input.
 */
@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier
) {
    val blocks = remember(text) { parseMarkdown(text) }
    val bodyStyle = LocalTextStyle.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        blocks.forEach { block ->
            RenderBlock(block, bodyStyle)
        }
    }
}

private sealed interface MdBlock {
    data class Paragraph(val inlines: List<Inline>) : MdBlock
    data class Heading(val level: Int, val inlines: List<Inline>) : MdBlock
    data class CodeBlock(val text: String) : MdBlock
    data class Quote(val inlines: List<Inline>) : MdBlock
    data class BulletItem(val inlines: List<Inline>) : MdBlock
    data class NumberedItem(val number: Int, val inlines: List<Inline>) : MdBlock
    object Divider : MdBlock
    data class Blank(val lines: Int = 1) : MdBlock
}

private sealed interface Inline {
    data class Text(val value: String) : Inline
    data class Bold(val children: List<Inline>) : Inline
    data class Italic(val children: List<Inline>) : Inline
    data class Code(val value: String) : Inline
}

private fun parseMarkdown(src: String): List<MdBlock> {
    val blocks = mutableListOf<MdBlock>()
    val lines = src.lines()
    var i = 0
    while (i < lines.size) {
        val line = lines[i]
        when {
            line.isBlank() -> {
                val count = line.countLeadingBlanks(lines, i)
                if (count > 0) blocks += MdBlock.Blank(count)
                i += count
            }
            line.startsWith("```") -> {
                val lang = line.removePrefix("```").trim()
                val body = StringBuilder()
                var j = i + 1
                while (j < lines.size && !lines[j].startsWith("```")) {
                    if (body.isNotEmpty()) body.append('\n')
                    body.append(lines[j])
                    j++
                }
                blocks += MdBlock.CodeBlock(body.toString())
                i = if (j < lines.size) j + 1 else lines.size
                if (lang.isNotBlank()) { /* language hint ignored for now */ }
            }
            line.startsWith("# ") -> {
                blocks += MdBlock.Heading(1, parseInlines(line.removePrefix("# ")))
                i++
            }
            line.startsWith("## ") -> {
                blocks += MdBlock.Heading(2, parseInlines(line.removePrefix("## ")))
                i++
            }
            line.startsWith("### ") -> {
                blocks += MdBlock.Heading(3, parseInlines(line.removePrefix("### ")))
                i++
            }
            line.startsWith("#### ") -> {
                blocks += MdBlock.Heading(4, parseInlines(line.removePrefix("#### ")))
                i++
            }
            line == "---" || line == "***" -> {
                blocks += MdBlock.Divider
                i++
            }
            line.startsWith("> ") -> {
                blocks += MdBlock.Quote(parseInlines(line.removePrefix("> ")))
                i++
            }
            line.startsWith("- ") || line.startsWith("* ") -> {
                blocks += MdBlock.BulletItem(parseInlines(line.removePrefix("- ").removePrefix("* ")))
                i++
            }
            line.matches(Regex("^\\d+\\.\\s.*")) -> {
                val num = line.substringBefore('.').toIntOrNull() ?: 1
                val rest = line.removePrefix("$num.")
                blocks += MdBlock.NumberedItem(num, parseInlines(rest.trimStart()))
                i++
            }
            else -> {
                // Greedy paragraph: collect contiguous non-blank lines.
                val para = StringBuilder(line)
                var j = i + 1
                while (j < lines.size && lines[j].isNotBlank() &&
                    !lines[j].startsWith("# ") && !lines[j].startsWith("## ") &&
                    !lines[j].startsWith("### ") && !lines[j].startsWith("#### ") &&
                    !lines[j].startsWith("> ") && !lines[j].startsWith("- ") &&
                    !lines[j].startsWith("* ") && !lines[j].startsWith("```") &&
                    !lines[j].matches(Regex("^\\d+\\.\\s.*")) &&
                    lines[j] != "---"
                ) {
                    para.append('\n').append(lines[j])
                    j++
                }
                blocks += MdBlock.Paragraph(parseInlines(para.toString()))
                i = j
            }
        }
    }
    return blocks
}

private fun String.countLeadingBlanks(all: List<String>, from: Int): Int {
    var count = 0
    var k = from
    while (k < all.size && all[k].isBlank()) { count++; k++ }
    return count
}

/**
 * Parses a single line (or joined paragraph) into a flat list of
 * inline tokens.  Code spans are matched first (so `**foo**` inside
 * a code block is literal), then `**bold**`, then `*italic*` /
 * `_italic_`.
 */
private fun parseInlines(src: String): List<Inline> {
    val out = mutableListOf<Inline>()
    var i = 0
    val buf = StringBuilder()
    fun flush() {
        if (buf.isNotEmpty()) { out += Inline.Text(buf.toString()); buf.clear() }
    }
    while (i < src.length) {
        when {
            src[i] == '`' -> {
                val end = src.indexOf('`', i + 1)
                if (end > i) {
                    flush()
                    out += Inline.Code(src.substring(i + 1, end))
                    i = end + 1
                } else {
                    buf.append(src[i]); i++
                }
            }
            src.startsWith("**", i) -> {
                val end = src.indexOf("**", i + 2)
                if (end > i + 1) {
                    flush()
                    out += Inline.Bold(parseInlines(src.substring(i + 2, end)))
                    i = end + 2
                } else {
                    buf.append(src[i]); i++
                }
            }
            src[i] == '*' || src[i] == '_' -> {
                val marker = src[i]
                val end = src.indexOf(marker, i + 1)
                if (end > i && end > i + 1 && src[end - 1] != ' ') {
                    flush()
                    out += Inline.Italic(parseInlines(src.substring(i + 1, end)))
                    i = end + 1
                } else {
                    buf.append(src[i]); i++
                }
            }
            src[i] == '\n' -> {
                flush()
                out += Inline.Text(" ")
                i++
            }
            else -> { buf.append(src[i]); i++ }
        }
    }
    flush()
    return out
}

@Composable
private fun RenderBlock(block: MdBlock, bodyStyle: androidx.compose.ui.text.TextStyle) {
    when (block) {
        is MdBlock.Paragraph -> Text(
            text = renderInlines(block.inlines, bodyStyle),
            style = bodyStyle.copy(lineHeight = 20.sp)
        )
        is MdBlock.Heading -> {
            val size = when (block.level) {
                1 -> 22.sp
                2 -> 19.sp
                3 -> 17.sp
                else -> 15.sp
            }
            Text(
                text = renderInlines(block.inlines, bodyStyle),
                style = bodyStyle.copy(
                    fontSize = size,
                    fontWeight = FontWeight.Bold,
                    lineHeight = (size.value + 6).sp
                )
            )
        }
        is MdBlock.CodeBlock -> {
            Text(
                text = block.text,
                style = bodyStyle.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(6.dp)
                    )
                    .padding(10.dp)
            )
        }
        is MdBlock.Quote -> {
            Row(modifier = Modifier.fillMaxWidth()) {
                Spacer(
                    modifier = Modifier
                        .width(3.dp)
                        .background(MaterialTheme.colorScheme.primary)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = renderInlines(block.inlines, bodyStyle),
                    style = bodyStyle.copy(
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
        is MdBlock.BulletItem -> {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("•  ", style = bodyStyle)
                Text(
                    text = renderInlines(block.inlines, bodyStyle),
                    style = bodyStyle
                )
            }
        }
        is MdBlock.NumberedItem -> {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("${block.number}.  ", style = bodyStyle)
                Text(
                    text = renderInlines(block.inlines, bodyStyle),
                    style = bodyStyle
                )
            }
        }
        MdBlock.Divider -> {
            androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
        is MdBlock.Blank -> {
            // paragraph spacing is already handled by Column.verticalArrangement
        }
    }
}

@Composable
private fun renderInlines(inlines: List<Inline>, base: androidx.compose.ui.text.TextStyle): AnnotatedString =
    buildAnnotatedString {
        inlines.forEach { inline ->
            when (inline) {
                is Inline.Text -> append(inline.value)
                is Inline.Bold -> withStyle(
                    SpanStyle(fontWeight = FontWeight.Bold, fontSize = base.fontSize)
                ) { appendInlineChildren(inline.children, base) }
                is Inline.Italic -> withStyle(
                    SpanStyle(fontStyle = FontStyle.Italic, fontSize = base.fontSize)
                ) { appendInlineChildren(inline.children, base) }
                is Inline.Code -> withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = (base.fontSize.value - 1).sp,
                        background = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) { append(' '); append(inline.value); append(' ') }
            }
        }
    }

private fun androidx.compose.ui.text.AnnotatedString.Builder.appendInlineChildren(
    children: List<Inline>,
    base: androidx.compose.ui.text.TextStyle
) {
    children.forEach { child ->
        when (child) {
            is Inline.Text -> append(child.value)
            is Inline.Bold -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { appendInlineChildren(child.children, base) }
            is Inline.Italic -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { appendInlineChildren(child.children, base) }
            is Inline.Code -> withStyle(
                SpanStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = (base.fontSize.value - 1).sp,
                    background = Color(0x33808080)
                )
            ) { append(' '); append(child.value); append(' ') }
        }
    }
}
