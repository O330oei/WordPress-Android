package org.wordpress.android.ui.stats.refresh.lists.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView.ScaleType.FIT_START
import android.widget.RemoteViews
import com.bumptech.glide.request.target.AppWidgetTarget
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.stats.OldStatsActivity
import org.wordpress.android.ui.stats.StatsTimeframe
import org.wordpress.android.ui.stats.refresh.StatsActivity
import org.wordpress.android.ui.stats.refresh.lists.widget.StatsViewsWidgetConfigureViewModel.Color
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType.ICON
import javax.inject.Inject
import kotlin.random.Random

const val SHOW_CHANGE_VALUE_KEY = "show_change_value_key"
const val COLOR_MODE_KEY = "color_mode_key"
const val SITE_ID_KEY = "site_id_key"

private const val MIN_WIDTH = 250

class StatsViewsWidget : AppWidgetProvider() {
    @Inject lateinit var appPrefsWrapper: AppPrefsWrapper
    @Inject lateinit var siteStore: SiteStore
    @Inject lateinit var imageManager: ImageManager

    override fun onReceive(context: Context, intent: Intent?) {
        super.onReceive(context, intent)
        (context.applicationContext as WordPress).component().inject(this)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        (context.applicationContext as WordPress).component().inject(this)
        for (appWidgetId in appWidgetIds) {
            val minWidth = appWidgetManager.getAppWidgetOptions(appWidgetId)
                    .getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 300)
            updateAppWidget(
                    context,
                    appWidgetManager,
                    appWidgetId,
                    appPrefsWrapper,
                    siteStore,
                    imageManager,
                    minWidth > MIN_WIDTH
            )
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        (context.applicationContext as WordPress).component().inject(this)
        for (appWidgetId in appWidgetIds) {
            appPrefsWrapper.removeAppWidgetColorModeId(appWidgetId)
            appPrefsWrapper.removeAppWidgetSiteId(appWidgetId)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context?,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        if (context != null) {
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)

            val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
            (context.applicationContext as WordPress).component().inject(this)
            updateAppWidget(
                    context,
                    appWidgetManager,
                    appWidgetId,
                    appPrefsWrapper,
                    siteStore,
                    imageManager,
                    minWidth > MIN_WIDTH
            )
        }
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
    }

    companion object {
        internal fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            appPrefsWrapper: AppPrefsWrapper,
            siteStore: SiteStore,
            imageManager: ImageManager,
            showChangeColumn: Boolean = true
        ) {
            drawList(appWidgetId, context, appPrefsWrapper, siteStore, imageManager, appWidgetManager, showChangeColumn)
        }

        private fun drawList(
            appWidgetId: Int,
            context: Context,
            appPrefsWrapper: AppPrefsWrapper,
            siteStore: SiteStore,
            imageManager: ImageManager,
            appWidgetManager: AppWidgetManager,
            showChangeColumn: Boolean
        ) {
            val colorModeId = appPrefsWrapper.getAppWidgetColorModeId(appWidgetId)
            val siteId = appPrefsWrapper.getAppWidgetSiteId(appWidgetId)
            val siteModel = siteStore.getSiteBySiteId(siteId)

            val layout = when (colorModeId) {
                Color.DARK.ordinal -> R.layout.stats_views_widget_dark
                Color.LIGHT.ordinal -> R.layout.stats_views_widget_light
                else -> R.layout.stats_views_widget_light
            }
            val views = RemoteViews(context.packageName, layout)
            val siteIconUrl = siteModel?.iconUrl
            val awt = AppWidgetTarget(context, R.id.widget_site_icon, views, appWidgetId)
            imageManager.load(awt, context, ICON, siteIconUrl ?: "", FIT_START)
            siteModel?.let {
                views.setOnClickPendingIntent(R.id.widget_title, getPendingSelfIntent(context, siteModel.id))
                views.setPendingIntentTemplate(R.id.widget_list, getPendingTemplate(context))
            }
            val listIntent = Intent(context, WidgetService::class.java)
            listIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            listIntent.putExtra(SHOW_CHANGE_VALUE_KEY, showChangeColumn)
            listIntent.putExtra(COLOR_MODE_KEY, colorModeId)
            listIntent.putExtra(SITE_ID_KEY, siteId)
            listIntent.data = Uri.parse(
                    listIntent.toUri(Intent.URI_INTENT_SCHEME)
            )
            views.setRemoteAdapter(R.id.widget_list, listIntent)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun getPendingSelfIntent(context: Context, localSiteId: Int): PendingIntent {
            val intent = Intent(context, StatsActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.putExtra(WordPress.LOCAL_SITE_ID, localSiteId)
            intent.putExtra(OldStatsActivity.ARG_DESIRED_TIMEFRAME, StatsTimeframe.DAY)
            intent.putExtra(OldStatsActivity.ARG_LAUNCHED_FROM, OldStatsActivity.StatsLaunchedFrom.STATS_WIDGET)
            return PendingIntent.getActivity(
                    context,
                    Random(localSiteId).nextInt(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        private fun getPendingTemplate(context: Context): PendingIntent {
            val intent = Intent(context, StatsActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }
    }
}