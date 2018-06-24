package org.wordpress.android.ui.activitylog.list

import android.support.annotation.DrawableRes
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.activity.ActivityLogModel
import java.text.DateFormat
import java.util.Date
import java.util.Locale

sealed class ActivityLogListItem {
    abstract val activityId: String
    abstract val header: String
    abstract val title: String
    abstract val description: String
    abstract val icon: Icon
    abstract val status: Status
    abstract val date: Date
    abstract val buttonIcon: Icon
    abstract var isButtonVisible: Boolean
    abstract val isProgressBarVisible: Boolean
    abstract var isHeaderVisible: Boolean

    val formattedDate: String by lazy {
        DateFormat.getDateInstance(DateFormat.LONG, Locale.getDefault()).format(date)
    }

    val formattedTime: String by lazy {
        DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault()).format(date)
    }

    data class Event(
        override val activityId: String,
        override val title: String,
        override val description: String,
        private val gridIcon: String?,
        private val eventStatus: String?,
        val isRewindable: Boolean,
        val rewindId: String?,
        override val date: Date,
        override var isHeaderVisible: Boolean = false,
        override val buttonIcon: Icon = Icon.HISTORY,
        override var isButtonVisible: Boolean = isRewindable,
        override val isProgressBarVisible: Boolean = false) : ActivityLogListItem() {
        override val icon = Icon.fromValue(gridIcon)
        override val status = Status.fromValue(eventStatus)

        constructor(model: ActivityLogModel) : this(model.activityID, model.summary, model.text, model.gridicon,
                model.status, model.rewindable ?: false, model.rewindID, model.published)

        override val header = formattedDate
    }

    data class Progress(
        override val activityId: String,
        override val title: String,
        override val description: String,
        override val header: String,
        override val date:Date = Date(),
        override var isHeaderVisible: Boolean = false,
        override val isProgressBarVisible: Boolean = true,
        override val buttonIcon: Icon = Icon.DEFAULT,
        override var isButtonVisible: Boolean = false,
        override val icon: Icon = Icon.NOTICE_OUTLINE,
        override val status: Status = Status.INFO) : ActivityLogListItem() {
    }

    enum class Status(val value: String, @DrawableRes val color: Int) {
        NEGATIVE("error", R.drawable.shape_oval_red),
        INFO("warning", R.drawable.shape_oval_blue_wordpress),
        POSITIVE("success", R.drawable.shape_oval_green),
        NEUTRAL("", R.drawable.shape_oval_grey);

        companion object {
            private val map = Status.values().associateBy(Status::value)
            fun fromValue(value: String?) = map[value] ?: NEUTRAL
        }
    }

    enum class Icon(val value: String, @DrawableRes val drawable: Int) {
        CHECKMARK("checkmark", R.drawable.ic_checkmark_white_24dp),
        CLOUD("cloud", R.drawable.ic_cloud_white_24dp),
        COG("cog", R.drawable.ic_cog_white_24dp),
        COMMENT("comment", R.drawable.ic_comment_white_24dp),
        CROSS("cross", R.drawable.ic_cross_white_24dp),
        DOMAINS("domains", R.drawable.ic_domains_white_24dp),
        HISTORY("history", R.drawable.ic_history_white_24dp),
        IMAGE("image", R.drawable.ic_image_white_24dp),
        LAYOUT("layout", R.drawable.ic_layout_white_24dp),
        LOCK("lock", R.drawable.ic_lock_white_24dp),
        LOGOUT("logout", R.drawable.ic_sign_out_white_24dp),
        MAIL("mail", R.drawable.ic_mail_white_24dp),
        MENU("menu", R.drawable.ic_menu_white_24dp),
        MY_SITES("my-sites", R.drawable.ic_my_sites_white_24dp),
        NOTICE("notice", R.drawable.ic_notice_white_24dp),
        NOTICE_OUTLINE("notice-outline", R.drawable.ic_notice_outline_white_24dp),
        PAGES("pages", R.drawable.ic_pages_white_24dp),
        PLANS("plans", R.drawable.ic_plans_white_24dp),
        PLUGINS("plugins", R.drawable.ic_plugins_white_24dp),
        POSTS("posts", R.drawable.ic_posts_white_24dp),
        SHARE("share", R.drawable.ic_share_white_24dp),
        SHIPPING("shipping", R.drawable.ic_shipping_white_24dp),
        SPAM("spam", R.drawable.ic_spam_white_24dp),
        THEMES("themes", R.drawable.ic_themes_white_24dp),
        TRASH("trash", R.drawable.ic_trash_white_24dp),
        USER("user", R.drawable.ic_user_white_24dp),
        DEFAULT("", R.drawable.ic_notice_white_24dp);

        companion object {
            private val map = Icon.values().associateBy(Icon::value)
            fun fromValue(value: String?) = map[value] ?: DEFAULT
        }
    }
}
