package com.direader.epub

import org.jsoup.Jsoup

/**
 * 章节内容提取器，从 XHTML 中提取纯文本内容
 */
class ChapterExtractor {
    
    fun extract(html: String, index: Int): ParsedChapter {
        val doc = Jsoup.parse(html)
        
        // 移除不需要的无用元素 (样式、脚本、侧边栏、底部信息、导航、脚注等)
        doc.select("style, script, aside, footer, nav, .footnote, .endnote").remove()
        
        // 提取标题 (h1 - h3)
        var title = doc.select("h1, h2, h3").firstOrNull()?.text()?.trim()
        if (title.isNullOrEmpty()) {
            title = doc.title().trim()
        }
        if (title.isNullOrEmpty()) {
            title = "第 ${index + 1} 章"
        }
        
        // 保留段落格式：将换行/块级元素替换为特定占位符，以便之后替换为实际的换行符
        doc.select("br").append("___NEWLINE___")
        doc.select("p, div, li, blockquote, h1, h2, h3, h4, h5, h6").append("___NEWLINE______NEWLINE___")
        
        // 提取文本，Jsoup 的 text() 方法会自动规范化空白字符，但保留了我们的占位符
        var text = doc.text()
        
        // 恢复换行符
        text = text.replace("___NEWLINE___", "\n")
        
        // 移除每行首尾的空白字符
        text = text.replace(Regex("^[ \\t\\x0B\\f\\r]+", RegexOption.MULTILINE), "")
        text = text.replace(Regex("[ \\t\\x0B\\f\\r]+$", RegexOption.MULTILINE), "")
        
        // 折叠多余的换行符（超过2个的连续换行变为2个，即保留段落之间的单行空行）
        text = text.replace(Regex("\\n{3,}"), "\n\n")
        
        return ParsedChapter(index, title, text.trim())
    }
}
