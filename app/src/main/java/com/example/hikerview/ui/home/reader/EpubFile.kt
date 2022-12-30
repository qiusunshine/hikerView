package com.example.hikerview.ui.home.reader

import android.content.Context
import android.content.Intent
import android.util.Base64
import com.alibaba.fastjson.JSON
import com.example.hikerview.service.parser.CommonParser
import com.example.hikerview.service.parser.PageParser
import com.example.hikerview.ui.home.FilmListActivity
import com.example.hikerview.ui.home.model.ArticleListRule
import com.example.hikerview.utils.FilesInAppUtil
import com.example.hikerview.utils.StringUtil
import com.example.hikerview.utils.ToastMgr
import me.ag2s.epublib.domain.EpubBook
import me.ag2s.epublib.domain.Metadata
import me.ag2s.epublib.domain.Resource
import me.ag2s.epublib.domain.TOCReference
import me.ag2s.epublib.epub.EpubReader
import me.ag2s.epublib.util.zip.AndroidZipFile
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URLDecoder
import java.nio.charset.Charset

/**
 * 作者：By 15968
 * 日期：On 2022/12/23
 * 时间：At 16:13
 */
object EpubFile {
    private var epubBook: EpubBook? = null
    private var epubPath: String? = null
    private var chapters: ArrayList<BookChapter>? = null

    private fun readEpub(path: String): EpubBook {
        val zipFile = AndroidZipFile(File(path))
        return EpubReader().readEpubLazy(zipFile, "utf-8")
    }

    @Synchronized
    private fun loadEpub(path: String): EpubBook {
        if (epubPath != path || epubBook == null || chapters == null) {
            epubPath = path
            epubBook = readEpub(path)
            chapters = epubBook!!.getChapterList()
        }
        return epubBook!!
    }

    fun getMetadata(path: String): Metadata? {
        return loadEpub(path).metadata
    }

    fun getChapters(path: String): ArrayList<BookChapter> {
        loadEpub(path)
        return chapters!!
    }

    fun getContent(path: String, chapter: String): String {
        loadEpub(path)
        chapters?.let {
            for (bookChapter in it) {
                if (chapter == bookChapter.url) {
                    return epubBook?.getContent(bookChapter) ?: ""
                }
            }
        }
        return ""
    }

    fun getImage(href0: String): InputStream? {
        val s = href0.split("?pid=")
        val href = s[0]
        if (href == "cover.jpeg") return epubBook?.coverImage?.inputStream
        val abHref = URLDecoder.decode(href.replace("../", ""), "UTF-8")
        val res = epubBook?.resources?.getByHref(abHref)
        try {
            if (res == null && epubBook?.resources != null && s.size > 1) {
                val pid = s[1]
                val s2 = pid.split("@@")
                if (s2.size > 1) {
                    val chapterHref = String(Base64.decode(s2[1], Base64.NO_WRAP))
                    val domain = if (chapterHref.startsWith("/")) {
                        "http://xxxx.xxx"
                    } else {
                        "http://xxxx.xxx/"
                    }
                    val realUrl = CommonParser.joinUrl("${domain}$chapterHref", href)
                        .replace(domain, "")
                    val res1 = epubBook?.resources?.getByHref(realUrl)
                    if (res1 != null) {
                        return res1.inputStream
                    }
                }
                for (key in epubBook?.resources!!.resourceMap.keys) {
                    if (key.endsWith(href)) {
                        return epubBook?.resources!!.getByHref(key)?.inputStream
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return res?.inputStream
    }

    fun showEpubView(context: Context, path: String) {
        try {
            val file = File(path)
            val json = FilesInAppUtil.getAssetsString(context, "tools.json")
            val rule = JSON.parseObject(json, ArticleListRule::class.java)
            val b64 = String(Base64.encode(path.toByteArray(), Base64.NO_WRAP))
            val nextPage =
                PageParser.getNextPage(
                    rule,
                    "hiker://page/epub#autoCache#?p=$b64&pageTitle=" + file.name,
                    null
                ) ?: return
            val intent = Intent(context, FilmListActivity::class.java)
            FilmListActivity.putTempRule(intent, JSON.toJSONString(nextPage))
            intent.putExtra("title", file.name)
            intent.putExtra("parentTitle", file.name)
            intent.putExtra("parentUrl", path)
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            ToastMgr.shortCenter(context, e.message)
        }
    }
}

data class BookChapter(
    var title: String? = null,
    var url: String? = null,
    var index: Int = -1,
    var nextUrl: String? = null,
    var startFragmentId: String? = null,
    var endFragmentId: String? = null,
)

fun EpubBook.getChapterList(): ArrayList<BookChapter> {
    val eBook = this
    val chapterList = arrayListOf<BookChapter>()
    val refs = eBook.tableOfContents.tocReferences
    if (refs == null || refs.isEmpty()) {
        val spineReferences = eBook.spine.spineReferences
        var i = 0
        val size = spineReferences.size
        while (i < size) {
            val resource = spineReferences[i].resource
            var title = resource.title
            if (StringUtil.isEmpty(title)) {
                try {
                    val doc =
                        Jsoup.parse(String(resource.data, Charset.defaultCharset()))
                    val elements = doc.getElementsByTag("title")
                    if (elements.size > 0) {
                        title = elements[0].text()
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            val chapter = BookChapter()
            chapter.index = i
            chapter.url = resource.href
            if (i == 0 && title.isEmpty()) {
                chapter.title = "封面"
            } else {
                chapter.title = title
            }
            chapterList.lastOrNull()?.nextUrl = chapter.url
            chapterList.add(chapter)
            i++
        }
    } else {
        parseFirstPage(eBook, chapterList, refs)
        parseMenu(chapterList, refs, 0)
        for (i in chapterList.indices) {
            chapterList[i].index = i
        }
    }
    return chapterList
}

/*获取书籍起始页内容。部分书籍第一章之前存在封面，引言，扉页等内容*/
/*tile获取不同书籍风格杂乱，格式化处理待优化*/
private var durIndex = 0
private fun parseFirstPage(
    epubBook: EpubBook?,
    chapterList: ArrayList<BookChapter>,
    refs: List<TOCReference>?
) {
    val contents = epubBook?.contents
    if (epubBook == null || contents == null || refs == null) return
    var i = 0
    durIndex = 0
    while (i < contents.size) {
        val content = contents[i]
        if (!content.mediaType.toString().contains("htm")) continue
        /**
         * 检索到第一章href停止
         * completeHref可能有fragment(#id) 必须去除
         * fix https://github.com/gedoor/legado/issues/1932
         */
        try {
            if (refs[0].completeHref?.substringBeforeLast("#") == content.href) break
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val chapter = BookChapter()
        var title = content.title
        if (StringUtil.isEmpty(title)) {
            val elements = Jsoup.parse(
                String(epubBook!!.resources.getByHref(content.href).data, Charset.defaultCharset())
            ).getElementsByTag("title")
            title =
                if (elements.size > 0 && elements[0].text().isNotBlank())
                    elements[0].text()
                else
                    "--卷首--"
        }
        chapter.title = title
        chapter.url = content.href
        chapter.startFragmentId =
            if (content.href.substringAfter("#") == content.href) null
            else content.href.substringAfter("#")

        chapterList.lastOrNull()?.endFragmentId = chapter.startFragmentId
        chapterList.lastOrNull()?.nextUrl = chapter.url
        chapterList.add(chapter)
        durIndex++
        i++
    }
}

private fun parseMenu(
    chapterList: ArrayList<BookChapter>,
    refs: List<TOCReference>?,
    level: Int
) {
    refs?.forEach { ref ->
        if (ref.resource != null) {
            val chapter = BookChapter()
            chapter.title = ref.title
            chapter.url = ref.completeHref
            chapter.startFragmentId = ref.fragmentId
            chapterList.lastOrNull()?.endFragmentId = chapter.startFragmentId
            chapterList.lastOrNull()?.nextUrl = chapter.url
            chapterList.add(chapter)
            durIndex++
        }
        if (ref.children != null && ref.children.isNotEmpty()) {
            parseMenu(chapterList, ref.children, level + 1)
        }
    }
}

fun EpubBook.getContent(chapter: BookChapter): String {
    val epubBook = this
    /**
     * <image width="1038" height="670" xlink:href="..."/>
     * ...titlepage.xhtml
     * 大多数epub文件的封面页都会带有cover，可以一定程度上解决封面读取问题
     */
    if (chapter.url?.contains("titlepage.xhtml") == true ||
        chapter.url?.contains("cover") == true
    ) {
        return "<img src=\"cover.jpeg\" />"
    }
    /*获取当前章节文本*/
    epubBook.let { epubBook ->
        val nextUrl = chapter.nextUrl
        val startFragmentId = chapter.startFragmentId
        val endFragmentId = chapter.endFragmentId
        val elements = Elements()
        var hasMoreResources = false
        val includeNextChapterResource = !endFragmentId.isNullOrBlank()
        /*一些书籍依靠href索引的resource会包含多个章节，需要依靠fragmentId来截取到当前章节的内容*/
        /*注:这里较大增加了内容加载的时间，所以首次获取内容后可存储到本地cache，减少重复加载*/
        for (res in epubBook.contents) {
            val isFirstResource = chapter.url?.substringBeforeLast("#") == res.href
            val isNextChapterResource = res.href == nextUrl?.substringBeforeLast("#")
            if (isFirstResource) {
                // add first resource to elements
                elements.add(
                    /* pass endFragmentId if only has one resource */
                    getBody(res, startFragmentId, endFragmentId)
                )
                // check current resource
                if (isNextChapterResource) {
                    /* FragmentId should not be same in same resource */
                    if (!endFragmentId.isNullOrBlank() && endFragmentId == startFragmentId)
                        Timber.d("Epub: Resource (${res.href}) has same FragmentId, check the file")
                    break
                }
                hasMoreResources = true
            } else if (hasMoreResources) {
                if (isNextChapterResource) {
                    if (includeNextChapterResource) elements.add(
                        getBody(
                            res,
                            null/* FragmentId may be same in different resources, pass null */,
                            endFragmentId
                        )
                    )
                    break
                }
                // rest resource should not have fragmentId, pass null
                elements.add(getBody(res, null, null))
            }
        }
        //title标签中的内容不需要显示在正文中，去除
        elements.select("title").remove()
        return elements.html()
    }
}

private fun getBody(res: Resource, startFragmentId: String?, endFragmentId: String?): Element {
    val body = Jsoup.parse(String(res.data, Charset.defaultCharset())).body()
    if (!startFragmentId.isNullOrBlank()) {
        body.getElementById(startFragmentId)?.previousElementSiblings()?.remove()
    }
    if (!endFragmentId.isNullOrBlank() && endFragmentId != startFragmentId) {
        body.getElementById(endFragmentId)?.run {
            nextElementSiblings().remove()
            remove()
        }
    }
    /*选择去除正文中的H标签，部分书籍标题与阅读标题重复待优化*/

    body.getElementsByTag("h1").remove()
    body.getElementsByTag("h2").remove()
    body.getElementsByTag("h3").remove()
    body.getElementsByTag("h4").remove()
    body.getElementsByTag("h5").remove()
    body.getElementsByTag("h6").remove()
    //body.getElementsMatchingOwnText(chapter.title)?.remove()

    val children = body.children()
    children.select("script").remove()
    children.select("style").remove()
    return body
}