package com.example.myandroidapp.data.remote

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

/**
 * Debug 构建专用 Mock 拦截器——当 NewsAPI Key 未配置时返回模拟新闻数据。
 * 按 category + page 参数返回分页数据，最多 3 页 × 20 条。
 * 拦截 newsapi.org 域名的 /v2/top-headlines 请求。
 */
class MockNewsInterceptor : Interceptor {

    companion object {
        private val JSON_MEDIA = "application/json".toMediaType()
        private const val MAX_PAGES = 3
        private const val PAGE_SIZE = 20
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url
        val path = url.encodedPath

        // 仅拦截 NewsAPI top-headlines 请求
        if (path != "/v2/top-headlines" && !path.startsWith("/v2/top-headlines")) {
            return chain.proceed(request)
        }

        val category = url.queryParameter("category") ?: "general"
        val page = url.queryParameter("page")?.toIntOrNull() ?: 1

        if (page > MAX_PAGES) {
            return buildJsonResponse(request, buildEmptyResponse())
        }

        val articles = allArticles
            .filter { it["category"] == category }
            .drop((page - 1) * PAGE_SIZE)
            .take(PAGE_SIZE)

        val json = buildTopHeadlinesResponse(articles, allArticles.count { it["category"] == category })
        return buildJsonResponse(request, json)
    }

    private fun buildJsonResponse(request: okhttp3.Request, json: String): Response {
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(json.toResponseBody(JSON_MEDIA))
            .build()
    }

    private fun buildEmptyResponse(): String = """{"status":"ok","totalResults":0,"articles":[]}"""

    private fun buildTopHeadlinesResponse(articles: List<Map<String, String>>, total: Int): String {
        val items = articles.joinToString(",\n") { a ->
            """
            {
                "source": {"id": null, "name": "${a["source"]}"},
                "author": "${a["author"]}",
                "title": "${a["title"]}",
                "description": "${a["description"]}",
                "url": "${a["url"]}",
                "urlToImage": ${if (a["urlToImage"]?.isNotBlank() == true) "\"${a["urlToImage"]}\"" else "null"},
                "publishedAt": "${a["publishedAt"]}",
                "content": "${a["description"]}"
            }""".trimIndent()
        }
        return """{"status":"ok","totalResults":$total,"articles":[$items]}"""
    }

    // ===== 模拟数据：5 分类 × 各 10 条 = 50 条 =====
    private val allArticles: List<Map<String, String>> = listOf(
        // ===== 科技 (technology) =====
        mapOf(
            "source" to "36氪", "author" to "张鹏", "category" to "technology",
            "title" to "OpenAI 发布 GPT-5，推理能力超越人类博士水平",
            "description" to "OpenAI 今日正式发布 GPT-5 大模型，在多项基准测试中推理能力首次超越人类博士平均水平。Sam Altman 表示这标志着 AGI 的早期形态已经到来。",
            "url" to "https://example.com/tech/1", "urlToImage" to "https://picsum.photos/seed/tech1/400/300",
            "publishedAt" to "2026-06-04T08:00:00Z"
        ),
        mapOf(
            "source" to "机器之心", "author" to "李沐", "category" to "technology",
            "title" to "苹果 Vision Pro 2 通过中国 3C 认证，售价或降至 19999 元",
            "description" to "第二代 Apple Vision Pro 已通过中国 3C 认证，消息称起售价将从第一代的 29999 元降至 19999 元，重量减轻 30%，有望于本月发布。",
            "url" to "https://example.com/tech/3", "urlToImage" to "https://picsum.photos/seed/tech3/400/300",
            "publishedAt" to "2026-06-04T06:30:00Z"
        ),
        mapOf(
            "source" to "量子位", "author" to "编辑部", "category" to "technology",
            "title" to "华为发布鸿蒙 5.0：全场景分布式架构重构，性能提升 40%",
            "description" to "华为在开发者大会上正式发布 HarmonyOS 5.0，首次实现手机、平板、车机、IoT 设备的统一内核架构，AI 算力调度效率翻倍。",
            "url" to "https://example.com/tech/4", "urlToImage" to "https://picsum.photos/seed/tech4/400/300",
            "publishedAt" to "2026-06-03T15:00:00Z"
        ),
        mapOf(
            "source" to "InfoQ", "author" to "徐川", "category" to "technology",
            "title" to "Rust 语言在 Linux 内核中的采用率达到 15%，Linus Torvalds 表示满意",
            "description" to "最新统计显示 Linux 6.12 内核中 Rust 代码占比已达 15%，涵盖 DRM 驱动、Binder、网络协议栈等模块。Linus 在邮件列表中罕见地表达了对进展的满意。",
            "url" to "https://example.com/tech/5", "urlToImage" to "https://picsum.photos/seed/tech5/400/300",
            "publishedAt" to "2026-06-03T09:00:00Z"
        ),
        mapOf(
            "source" to "差评", "author" to "托尼", "category" to "technology",
            "title" to "特斯拉 Optimus 机器人开始在工厂拧螺丝，时薪仅为人类 1/3",
            "description" to "特斯拉弗里蒙特工厂已部署 12 台 Optimus Gen3 机器人参与电池组装，官方称每台可替代 1.5 名工人的工作量，引发工会新一轮谈判。",
            "url" to "https://example.com/tech/6", "urlToImage" to "https://picsum.photos/seed/tech6/400/300",
            "publishedAt" to "2026-06-02T11:00:00Z"
        ),
        mapOf(
            "source" to "极客公园", "author" to "周航", "category" to "technology",
            "title" to "高通骁龙 8 Gen 5 跑分曝光：单核超越苹果 M4",
            "description" to "GeekBench 数据库出现骁龙 8 Gen 5 工程机跑分，单核 3812 分超越 M4 芯片的 3765 分，采用台积电 2nm 工艺，预计年底发布。",
            "url" to "https://example.com/tech/7", "urlToImage" to "",
            "publishedAt" to "2026-06-02T14:00:00Z"
        ),
        mapOf(
            "source" to "虎嗅", "author" to "李岷", "category" to "technology",
            "title" to "百度文心一言用户突破 5 亿，推出企业级私有化部署方案",
            "description" to "百度宣布文心一言累计用户突破 5 亿，同时发布「文心企业版」支持完全私有化部署，已签约 200+ 大型企业客户。",
            "url" to "https://example.com/tech/8", "urlToImage" to "https://picsum.photos/seed/tech8/400/300",
            "publishedAt" to "2026-06-01T08:00:00Z"
        ),
        mapOf(
            "source" to "品玩", "author" to "骆轶航", "category" to "technology",
            "title" to "谷歌 DeepMind 公布 AlphaFold 4，可预测蛋白质动态折叠全过程",
            "description" to "AlphaFold 4 首次实现蛋白质动态折叠路径的完整预测，精度达到原子级别，药物研发周期有望缩短 60%。论文已在 Nature 发表。",
            "url" to "https://example.com/tech/9", "urlToImage" to "",
            "publishedAt" to "2026-05-31T10:00:00Z"
        ),
        mapOf(
            "source" to "爱范儿", "author" to "刘学文", "category" to "technology",
            "title" to "三星展示可拉伸 OLED 屏幕：拉伸 30% 不变形，可用于服装",
            "description" to "三星显示在 SID 展会上展示了可拉伸 30% 的 OLED 屏幕原型，可集成到服装面料中，计划 2027 年量产，首批客户为运动品牌。",
            "url" to "https://example.com/tech/10", "urlToImage" to "https://picsum.photos/seed/tech10/400/300",
            "publishedAt" to "2026-05-30T07:00:00Z"
        ),

        // ===== 财经 (business) =====
        mapOf(
            "source" to "财新", "author" to "胡舒立", "category" to "business",
            "title" to "央行下调 LPR 25 个基点，房贷利率创历史新低",
            "description" to "中国人民银行宣布 1 年期 LPR 降至 3.15%，5 年期以上降至 3.65%，均下调 25 个基点。首套房贷利率最低可至 3.25%，创有记录以来最低水平。",
            "url" to "https://example.com/biz/1", "urlToImage" to "https://picsum.photos/seed/biz1/400/300",
            "publishedAt" to "2026-06-04T09:00:00Z"
        ),
        mapOf(
            "source" to "第一财经", "author" to "秦朔", "category" to "business",
            "title" to "A 股三大指数全线大涨，沪指重回 3500 点",
            "description" to "受 LPR 降息及外资回流推动，A 股三大指数今日集体大涨。上证指数收涨 2.8% 报 3521 点，深证成指涨 3.5%，创业板指涨 4.1%，两市成交额突破 1.5 万亿。",
            "url" to "https://example.com/biz/2", "urlToImage" to "",
            "publishedAt" to "2026-06-04T15:30:00Z"
        ),
        mapOf(
            "source" to "华尔街见闻", "author" to "编辑部", "category" to "business",
            "title" to "美联储暗示年内降息 3 次，人民币汇率升破 6.8",
            "description" to "美联储 6 月议息会议纪要暗示年内将累计降息 75 个基点。受此影响，离岸人民币兑美元汇率升破 6.80 关口，创近一年新高。",
            "url" to "https://example.com/biz/3", "urlToImage" to "https://picsum.photos/seed/biz3/400/300",
            "publishedAt" to "2026-06-03T20:00:00Z"
        ),
        mapOf(
            "source" to "经济观察报", "author" to "沈建光", "category" to "business",
            "title" to "5 月 CPI 同比上涨 0.8%，PPI 降幅收窄至 -1.2%",
            "description" to "国家统计局公布 5 月物价数据：CPI 同比涨 0.8%，食品价格上涨为主要推动力；PPI 同比下降 1.2%，降幅较上月收窄 0.4 个百分点，工业通缩压力缓解。",
            "url" to "https://example.com/biz/4", "urlToImage" to "",
            "publishedAt" to "2026-06-02T09:30:00Z"
        ),
        mapOf(
            "source" to "每日经济新闻", "author" to "编辑部", "category" to "business",
            "title" to "比亚迪市值突破 1.5 万亿，超越特斯拉成为全球最大车企",
            "description" to "比亚迪今日股价涨超 5%，总市值突破 1.5 万亿元人民币，超越特斯拉成为全球市值最高汽车制造商。出口业务同比增长 120% 为关键驱动。",
            "url" to "https://example.com/biz/5", "urlToImage" to "https://picsum.photos/seed/biz5/400/300",
            "publishedAt" to "2026-06-01T10:00:00Z"
        ),
        mapOf(
            "source" to "雪球", "author" to "不明真相的群众", "category" to "business",
            "title" to "拼多多 Temu 季度 GMV 突破 300 亿美元，即将在巴西上线",
            "description" to "拼多多旗下跨境电商平台 Temu 公布 Q1 财报：季度 GMV 突破 300 亿美元，同比增长 450%。同时宣布将于 7 月在巴西正式上线，全球站点将达 72 个。",
            "url" to "https://example.com/biz/6", "urlToImage" to "https://picsum.photos/seed/biz6/400/300",
            "publishedAt" to "2026-05-30T16:00:00Z"
        ),

        // ===== 体育 (sports) =====
        mapOf(
            "source" to "新浪体育", "author" to "陈驰", "category" to "sports",
            "title" to "中国男足 2:1 击败韩国，世预赛出线形势大好",
            "description" to "世预赛亚洲区 18 强赛，中国男足在沈阳奥体中心 2:1 战胜韩国队。武磊梅开二度，国足积 11 分升至小组第二，距离直接出线仅差 1 分。",
            "url" to "https://example.com/sports/1", "urlToImage" to "https://picsum.photos/seed/sports1/400/300",
            "publishedAt" to "2026-06-04T20:00:00Z"
        ),
        mapOf(
            "source" to "虎扑", "author" to "虎扑JR", "category" to "sports",
            "title" to "NBA 总决赛：掘金 4:2 击败凯尔特人，约基奇场均三双拿下 FMVP",
            "description" to "掘金在总决赛 G6 中以 112:98 击败凯尔特人，总比分 4:2 夺冠。尼古拉·约基奇场均 32 分 14 篮板 11 助攻拿下 FMVP，成为史上第三位在总决赛场均三双的球员。",
            "url" to "https://example.com/sports/2", "urlToImage" to "https://picsum.photos/seed/sports2/400/300",
            "publishedAt" to "2026-06-04T12:00:00Z"
        ),
        mapOf(
            "source" to "体坛周报", "author" to "马德兴", "category" to "sports",
            "title" to "巴黎奥运会中国代表团金牌数锁定第一，创境外奥运最佳战绩",
            "description" to "巴黎奥运会倒数第二个比赛日，中国代表团以 42 枚金牌锁定奖牌榜第一，超越 2008 年北京奥运会的 48 金虽已无望，但已创造境外参赛金牌数新纪录。",
            "url" to "https://example.com/sports/3", "urlToImage" to "",
            "publishedAt" to "2026-06-03T06:00:00Z"
        ),
        mapOf(
            "source" to "直播吧", "author" to "编辑部", "category" to "sports",
            "title" to "中超联赛：上海海港 3:0 大胜北京国安，武磊帽子戏法",
            "description" to "中超第 15 轮焦点战，上海海港主场 3:0 完胜北京国安。武磊上演帽子戏法，赛季进球已达 18 球，领跑射手榜。海港继续保持不败领跑积分榜。",
            "url" to "https://example.com/sports/4", "urlToImage" to "https://picsum.photos/seed/sports4/400/300",
            "publishedAt" to "2026-06-02T18:00:00Z"
        ),

        // ===== 娱乐 (entertainment) =====
        mapOf(
            "source" to "新浪娱乐", "author" to "编辑部", "category" to "entertainment",
            "title" to "《哪吒 3》定档 2027 春节，饺子导演亲自确认",
            "description" to "国产动画电影《哪吒 3》正式定档 2027 年大年初一。导演饺子在微博发文称「这次故事会更宏大，让观众看到一个完全不同的哪吒」。前作票房达 72 亿。",
            "url" to "https://example.com/ent/1", "urlToImage" to "https://picsum.photos/seed/ent1/400/300",
            "publishedAt" to "2026-06-04T10:00:00Z"
        ),
        mapOf(
            "source" to "腾讯娱乐", "author" to "编辑部", "category" to "entertainment",
            "title" to "周杰伦新专辑《月光奏鸣曲》上线即破纪录，24 小时销量突破 500 万张",
            "description" to "周杰伦时隔两年推出的全新专辑《月光奏鸣曲》上线 QQ 音乐 24 小时销量突破 500 万张，刷新数字专辑销售纪录。专辑融合古典与 R&B，被歌迷称为「杰伦回归巅峰之作」。",
            "url" to "https://example.com/ent/2", "urlToImage" to "https://picsum.photos/seed/ent2/400/300",
            "publishedAt" to "2026-06-03T08:00:00Z"
        ),
        mapOf(
            "source" to "豆瓣电影", "author" to "影志", "category" to "entertainment",
            "title" to "《封神第二部》票房破 50 亿，成为中国影史前十",
            "description" to "乌尔善执导的《封神第二部》上映 15 天累计票房突破 50 亿，超越《流浪地球 2》成为中国影史票房第 8 位。第三部已进入后期制作阶段。",
            "url" to "https://example.com/ent/3", "urlToImage" to "",
            "publishedAt" to "2026-06-01T14:00:00Z"
        ),

        // ===== 推荐 (general) =====
        mapOf(
            "source" to "人民日报", "author" to "编辑部", "category" to "general",
            "title" to "新质生产力写入「十五五」规划纲要，科技创新投入将翻番",
            "description" to "国务院常务会议审议通过「十五五」规划纲要草案，首次将「新质生产力」列为国家战略核心。研发投入占 GDP 比重目标从 2.5% 提升至 4%，AI、量子、航天为三大主攻方向。",
            "url" to "https://example.com/gen/1", "urlToImage" to "https://picsum.photos/seed/gen1/400/300",
            "publishedAt" to "2026-06-04T07:00:00Z"
        ),
        mapOf(
            "source" to "新华社", "author" to "编辑部", "category" to "general",
            "title" to "全国高考今日开考，考生人数达 1350 万再创新高",
            "description" to "2026 年全国高考今日开考，报名人数达 1350 万，较去年增加 36 万，再创历史新高。教育部已部署 AI 监考辅助系统在全国 90% 考场投入使用。",
            "url" to "https://example.com/gen/2", "urlToImage" to "",
            "publishedAt" to "2026-06-04T06:00:00Z"
        ),
        mapOf(
            "source" to "央视新闻", "author" to "白岩松", "category" to "general",
            "title" to "中欧班列累计开行突破 10 万列，年度货值超 5000 亿美元",
            "description" to "中欧班列自 2011 年首列开行以来累计突破 10 万列，2025 年全年货值首次超过 5000 亿美元。新开通的北极线路将亚欧货运时间缩短至 12 天。",
            "url" to "https://example.com/gen/3", "urlToImage" to "https://picsum.photos/seed/gen3/400/300",
            "publishedAt" to "2026-06-03T12:00:00Z"
        ),
        mapOf(
            "source" to "澎湃新闻", "author" to "编辑部", "category" to "general",
            "title" to "全国多地将迎来极端高温天气，最高气温或达 42°C",
            "description" to "中央气象台发布高温红色预警：未来三天华北中南部、黄淮、江淮等地将出现 40°C 以上极端高温，其中河北南部、河南北部局地可达 42°C。建议公众减少户外活动。",
            "url" to "https://example.com/gen/4", "urlToImage" to "https://picsum.photos/seed/gen4/400/300",
            "publishedAt" to "2026-06-02T08:00:00Z"
        ),
        mapOf(
            "source" to "环球时报", "author" to "胡锡进", "category" to "general",
            "title" to "中美达成新一轮气候合作协议：2035 年前双方减排 45%",
            "description" to "中美两国在华盛顿签署新一轮气候合作框架协议，承诺在 2035 年前各自减少温室气体排放 45%（以 2005 年为基线），并设立 200 亿美元联合清洁能源基金。",
            "url" to "https://example.com/gen/5", "urlToImage" to "",
            "publishedAt" to "2026-05-30T09:00:00Z"
        ),
    )
}
