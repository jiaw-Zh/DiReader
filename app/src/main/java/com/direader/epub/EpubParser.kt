package com.direader.epub

import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import java.io.File
import java.net.URLDecoder
import java.util.zip.ZipFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ParsedBook(
    val title: String,
    val author: String,
    val coverData: ByteArray?,
    val chapters: List<ParsedChapter>
)

data class ParsedChapter(
    val index: Int,
    val title: String,
    val text: String
)

/**
 * EPUB 解析器，支持 EPUB 2 和 EPUB 3
 * 手动解压 ZIP 并解析 OEBPS / OPF 规范
 */
class EpubParser {
    
    suspend fun parse(file: File): ParsedBook = withContext(Dispatchers.IO) {
        ZipFile(file).use { zip ->
            // 1. 读取 META-INF/container.xml 寻找 OPF 路径
            val containerEntry = zip.getEntry("META-INF/container.xml")
                ?: throw IllegalArgumentException("无效的 EPUB: 找不到 META-INF/container.xml")
            
            val containerXml = zip.getInputStream(containerEntry).bufferedReader().readText()
            val containerDoc = Jsoup.parse(containerXml, "", Parser.xmlParser())
            
            val opfPath = containerDoc.select("rootfile").attr("full-path")
            if (opfPath.isEmpty()) throw IllegalArgumentException("无效的 EPUB: 找不到 OPF 路径")
            
            val opfEntry = zip.getEntry(opfPath)
                ?: throw IllegalArgumentException("无效的 EPUB: 找不到 OPF 文件 ($opfPath)")
            
            val opfXml = zip.getInputStream(opfEntry).bufferedReader().readText()
            val opfDoc = Jsoup.parse(opfXml, "", Parser.xmlParser())
            
            // 计算 OPF 所在目录，作为相对路径的基准
            val opfDir = if (opfPath.contains("/")) opfPath.substringBeforeLast("/") + "/" else ""
            
            // 2. 提取元数据 (兼容命名空间前缀)
            var title = opfDoc.getElementsByTag("dc:title").firstOrNull()?.text()
            if (title.isNullOrEmpty()) title = opfDoc.getElementsByTag("title").firstOrNull()?.text()
            if (title.isNullOrEmpty()) title = "未知书名"
            
            var author = opfDoc.getElementsByTag("dc:creator").firstOrNull()?.text()
            if (author.isNullOrEmpty()) author = opfDoc.getElementsByTag("creator").firstOrNull()?.text()
            if (author.isNullOrEmpty()) author = "未知作者"
            
            // 提取封面
            var coverData: ByteArray? = null
            val coverId = opfDoc.select("meta[name=cover]").attr("content")
            if (coverId.isNotEmpty()) {
                val coverHref = opfDoc.select("item[id=$coverId]").attr("href")
                if (coverHref.isNotEmpty()) {
                    val decodedCoverHref = URLDecoder.decode(coverHref, "UTF-8")
                    val coverEntry = zip.getEntry(opfDir + decodedCoverHref)
                    if (coverEntry != null) {
                        coverData = zip.getInputStream(coverEntry).readBytes()
                    }
                }
            }
            
            // 3. 解析 manifest 和 spine 提取章节
            val manifest = mutableMapOf<String, String>()
            opfDoc.select("manifest > item").forEach { item ->
                manifest[item.attr("id")] = item.attr("href")
            }
            
            val spineIds = opfDoc.select("spine > itemref").map { it.attr("idref") }
            
            val chapters = mutableListOf<ParsedChapter>()
            var chapterIndex = 0
            val extractor = ChapterExtractor()
            
            for (id in spineIds) {
                val href = manifest[id] ?: continue
                val decodedHref = URLDecoder.decode(href, "UTF-8")
                // 有些 href 会带有锚点（如 chapter.xhtml#part1），直接去除
                val cleanHref = decodedHref.substringBefore("#")
                
                val chapterPath = opfDir + cleanHref
                val chapterEntry = zip.getEntry(chapterPath) ?: continue
                
                val html = zip.getInputStream(chapterEntry).bufferedReader().readText()
                val chapter = extractor.extract(html, chapterIndex)
                chapters.add(chapter)
                chapterIndex++
            }
            
            ParsedBook(title, author, coverData, chapters)
        }
    }
}
