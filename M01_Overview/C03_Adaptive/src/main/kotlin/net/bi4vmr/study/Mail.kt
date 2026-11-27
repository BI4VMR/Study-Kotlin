package net.bi4vmr.study

/**
 * 邮件数据。
 *
 * @property id 邮件ID，用作自适应布局导航的 contentKey。
 *   由于 contentKey 需要能够被保存和恢复，这里使用基础类型 [Int]。
 * @property sender 发件人。
 * @property subject 邮件主题。
 * @property content 邮件正文。
 */
data class Mail(
    val id: Int,
    val sender: String,
    val subject: String,
    val content: String,
)

/** 示例邮件列表。 */
val sampleMails: List<Mail> = listOf(
    Mail(
        id = 1,
        sender = "Alice",
        subject = "项目进度同步",
        content = "本周已完成登录模块的联调，下周开始进行首页功能的开发，预计周五前提交测试。",
    ),
    Mail(
        id = 2,
        sender = "Bob",
        subject = "周会纪要",
        content = "本次周会主要讨论了版本发布计划和遗留问题的处理方案，详细内容请查看附件。",
    ),
    Mail(
        id = 3,
        sender = "Carol",
        subject = "设计稿评审",
        content = "新版首页设计稿已经上传到共享目录，请大家在周四之前完成评审并留下意见。",
    ),
    Mail(
        id = 4,
        sender = "David",
        subject = "服务器告警通知",
        content = "今天凌晨 2 点测试环境出现短时 CPU 占用过高，目前已经恢复，稍后会给出分析报告。",
    ),
    Mail(
        id = 5,
        sender = "Eve",
        subject = "报销流程变更",
        content = "从下个月起，报销流程调整为线上审批，纸质单据不再需要提交到财务处。",
    ),
)
