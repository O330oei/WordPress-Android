package org.wordpress.android.ui.posts

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.ui.posts.EditPostPublishSettingsViewModel.PublishUiModel
import org.wordpress.android.ui.posts.PostNotificationTimeDialogFragment.NotificationTime.ONE_HOUR_BEFORE
import org.wordpress.android.util.LocaleManagerWrapper
import org.wordpress.android.viewmodel.ResourceProvider
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class EditPostPublishSettingsViewModelTest : BaseUnitTest() {
    @Mock lateinit var resourceProvider: ResourceProvider
    @Mock lateinit var postSettingsUtils: PostSettingsUtils
    @Mock lateinit var localeManagerWrapper: LocaleManagerWrapper
    private lateinit var viewModel: EditPostPublishSettingsViewModel

    private val dateCreated = "2019-05-05T14:33:20+0000"
    private val currentCalendar = Calendar.getInstance(Locale.US)
    private val dateLabel = "Updated date"

    @Before
    fun setUp() {
        viewModel = EditPostPublishSettingsViewModel(resourceProvider, postSettingsUtils, localeManagerWrapper)
        currentCalendar.set(2019, 6, 6, 10, 20)
        whenever(localeManagerWrapper.getCurrentCalendar()).thenReturn(currentCalendar)
        whenever(localeManagerWrapper.getTimeZone()).thenReturn(TimeZone.getTimeZone("GMT"))
        whenever(postSettingsUtils.getPublishDateLabel(any())).thenReturn(dateLabel)
        whenever(resourceProvider.getString(R.string.immediately)).thenReturn("Immediately")
    }

    @Test
    fun `on start sets values and builds formatted label`() {
        val post = PostModel()
        post.dateCreated = dateCreated

        val expectedLabel = "Scheduled for 2019"
        whenever(postSettingsUtils.getPublishDateLabel(post)).thenReturn(expectedLabel)
        var uiModel: PublishUiModel? = null
        viewModel.onUiModel.observeForever {
            uiModel = it
        }

        viewModel.start(post)

        assertThat(viewModel.year).isEqualTo(2019)
        assertThat(viewModel.month).isEqualTo(4)
        assertThat(viewModel.day).isEqualTo(5)
        assertThat(viewModel.hour).isEqualTo(14)
        assertThat(viewModel.minute).isEqualTo(33)

        assertThat(uiModel!!.publishDateLabel).isEqualTo(expectedLabel)
    }

    @Test
    fun `on start sets current date when post not present`() {
        var uiModel: PublishUiModel? = null
        viewModel.onUiModel.observeForever {
            uiModel = it
        }

        viewModel.start(null)

        assertThat(viewModel.year).isEqualTo(2019)
        assertThat(viewModel.month).isEqualTo(6)
        assertThat(viewModel.day).isEqualTo(6)
        assertThat(viewModel.hour).isEqualTo(10)
        assertThat(viewModel.minute).isEqualTo(20)

        assertThat(uiModel!!.publishDateLabel).isEqualTo("Immediately")
    }

    @Test
    fun `on publishNow updates published date`() {
        var publishedDate: Calendar? = null
        viewModel.onPublishedDateChanged.observeForever {
            publishedDate = it
        }

        viewModel.publishNow()

        assertThat(publishedDate).isEqualTo(currentCalendar)
    }

    @Test
    fun `onDateSelected updates date and triggers onDatePicked`() {
        var datePicked: Unit? = null
        viewModel.onDatePicked.observeForever {
            datePicked = it?.getContentIfNotHandled()
        }

        val updatedYear = 2018
        val updatedMonth = 1
        val updatedDay = 5
        viewModel.onDateSelected(updatedYear, updatedMonth, updatedDay)

        assertThat(viewModel.year).isEqualTo(updatedYear)
        assertThat(viewModel.month).isEqualTo(updatedMonth)
        assertThat(viewModel.day).isEqualTo(updatedDay)

        assertThat(datePicked).isNotNull
    }

    @Test
    fun `onTimeSelected updates time and triggers onPublishedDateChanged`() {
        viewModel.start(null)

        var publishedDate: Calendar? = null
        viewModel.onPublishedDateChanged.observeForever {
            publishedDate = it
        }

        val updatedHour = 15
        val updatedMinute = 15

        viewModel.onTimeSelected(updatedHour, updatedMinute)

        assertThat(viewModel.hour).isEqualTo(updatedHour)
        assertThat(viewModel.minute).isEqualTo(updatedMinute)

        assertThat(publishedDate).isNotNull()
    }

    @Test
    fun `updatePost updates post status from DRAFT to PUBLISHED to published when date in the future`() {
        val post = PostModel()
        post.status = PostStatus.DRAFT.toString()
        val futureDate = Calendar.getInstance()
        futureDate.add(Calendar.MINUTE, 15)

        var updatedStatus: PostStatus? = null
        viewModel.onPostStatusChanged.observeForever {
            updatedStatus = it
        }

        var uiModel: PublishUiModel? = null
        viewModel.onUiModel.observeForever {
            uiModel = it
        }

        viewModel.updatePost(futureDate, post)

        assertThat(post.status).isEqualTo(PostStatus.SCHEDULED.toString())
        assertThat(post.dateCreated).isNotNull()

        assertThat(updatedStatus).isEqualTo(PostStatus.SCHEDULED)
        uiModel?.apply {
            assertThat(this.publishDateLabel).isEqualTo("Updated date")
            assertThat(this.notificationLabel).isEqualTo(R.string.post_notification_off)
            assertThat(this.notificationEnabled).isTrue()
            assertThat(this.notificationVisible).isTrue()
        }
    }

    @Test
    fun `updatePost updates post status from PUBLISHED to DRAFT for local draft`() {
        val post = PostModel()
        post.status = PostStatus.PUBLISHED.toString()
        post.setIsLocalDraft(true)

        var updatedStatus: PostStatus? = null
        viewModel.onPostStatusChanged.observeForever {
            updatedStatus = it
        }

        var uiModel: PublishUiModel? = null
        viewModel.onUiModel.observeForever {
            uiModel = it
        }

        viewModel.updatePost(currentCalendar, post)

        assertThat(post.status).isEqualTo(PostStatus.DRAFT.toString())
        assertThat(post.dateCreated).isNotNull()

        assertThat(updatedStatus).isEqualTo(PostStatus.DRAFT)
        uiModel?.apply {
            assertThat(this.publishDateLabel).isEqualTo("Updated date")
            assertThat(this.notificationLabel).isEqualTo(R.string.post_notification_off)
            assertThat(this.notificationEnabled).isFalse()
            assertThat(this.notificationVisible).isTrue()
        }
    }

    @Test
    fun `updatePost updates post status from SCHEDULED to DRAFT when published date in past`() {
        val expectedToastMessage = "Message"
        whenever(resourceProvider.getString(R.string.editor_post_converted_back_to_draft)).thenReturn(
                expectedToastMessage
        )
        val post = PostModel()
        post.status = PostStatus.SCHEDULED.toString()
        post.setIsLocalDraft(true)
        val pastDate = Calendar.getInstance()
        pastDate.add(Calendar.MINUTE, -100)

        var updatedStatus: PostStatus? = null
        viewModel.onPostStatusChanged.observeForever {
            updatedStatus = it
        }

        var uiModel: PublishUiModel? = null
        viewModel.onUiModel.observeForever {
            uiModel = it
        }

        var toastMessage: String? = null
        viewModel.onToast.observeForever {
            toastMessage = it?.getContentIfNotHandled()
        }

        viewModel.updatePost(currentCalendar, post)

        assertThat(post.status).isEqualTo(PostStatus.DRAFT.toString())
        assertThat(post.dateCreated).isNotNull()

        assertThat(updatedStatus).isEqualTo(PostStatus.DRAFT)
        uiModel?.apply {
            assertThat(this.publishDateLabel).isEqualTo("Updated date")
            assertThat(this.notificationLabel).isEqualTo(R.string.post_notification_off)
            assertThat(this.notificationEnabled).isFalse()
            assertThat(this.notificationVisible).isTrue()
        }

        assertThat(toastMessage).isEqualTo(expectedToastMessage)
    }

    @Test
    fun `hides notification when publish date in the past`() {
        val post = PostModel()
        post.status = PostStatus.PUBLISHED.toString()
        post.dateCreated = "2019-05-05T14:33:20+0000"
        val pastDate = Calendar.getInstance()
        pastDate.set(2019, 6, 6, 10, 10, 10)
        whenever(localeManagerWrapper.getCurrentCalendar()).thenReturn(pastDate)

        var uiModel: PublishUiModel? = null
        viewModel.onUiModel.observeForever {
            uiModel = it
        }

        viewModel.updateUiModel(ONE_HOUR_BEFORE, post)

        uiModel?.apply {
            assertThat(this.publishDateLabel).isEqualTo("Updated date")
            assertThat(this.notificationLabel).isEqualTo(R.string.post_notification_off)
            assertThat(this.notificationEnabled).isFalse()
            assertThat(this.notificationVisible).isFalse()
        }
    }

    @Test
    fun `DISABLES notification when publish date in NOW`() {
        val post = PostModel()
        post.status = PostStatus.PUBLISHED.toString()
        post.dateCreated = "2019-05-05T14:33:20+0000"
        val pastDate = Calendar.getInstance(Locale.US)
        pastDate.timeZone = TimeZone.getTimeZone("GMT")
        pastDate.set(2019, 4, 5, 14, 33, 20)
        whenever(localeManagerWrapper.getCurrentCalendar()).thenReturn(pastDate)

        var uiModel: PublishUiModel? = null
        viewModel.onUiModel.observeForever {
            uiModel = it
        }

        viewModel.updateUiModel(ONE_HOUR_BEFORE, post)

        uiModel?.apply {
            assertThat(this.publishDateLabel).isEqualTo("Updated date")
            assertThat(this.notificationEnabled).isFalse()
            assertThat(this.notificationVisible).isTrue()
            assertThat(this.notificationLabel).isEqualTo(R.string.post_notification_off)
        }
    }

    @Test
    fun `DISABLES notification when publish date in missing`() {
        val post = PostModel()
        post.status = PostStatus.PUBLISHED.toString()
        val pastDate = Calendar.getInstance(Locale.US)
        pastDate.timeZone = TimeZone.getTimeZone("GMT")
        pastDate.set(2019, 4, 5, 14, 33, 20)
        whenever(localeManagerWrapper.getCurrentCalendar()).thenReturn(pastDate)

        var uiModel: PublishUiModel? = null
        viewModel.onUiModel.observeForever {
            uiModel = it
        }

        viewModel.updateUiModel(ONE_HOUR_BEFORE, post)

        uiModel?.apply {
            assertThat(this.publishDateLabel).isEqualTo("Updated date")
            assertThat(this.notificationEnabled).isFalse()
            assertThat(this.notificationVisible).isTrue()
            assertThat(this.notificationLabel).isEqualTo(R.string.post_notification_off)
        }
    }
}