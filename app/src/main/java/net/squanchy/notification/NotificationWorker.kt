package net.squanchy.notification

import android.content.Context
import android.preference.PreferenceManager
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.RxWorker
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import net.squanchy.R
import net.squanchy.schedule.domain.view.Event
import net.squanchy.support.system.CurrentTime
import org.threeten.bp.Duration
import org.threeten.bp.ZonedDateTime
import timber.log.Timber
import java.util.concurrent.TimeUnit

class NotificationWorker(context: Context, parameters: WorkerParameters) : RxWorker(context, parameters) {

    private val service: NotificationService
    private val notificationCreator: NotificationCreator
    private val notifier: Notifier
    private val currentTime: CurrentTime
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)

    init {
        notificationComponent(context).run {
            service = service()
            notificationCreator = notificationCreator()
            notifier = notifier()
            currentTime = currentTime()
        }
    }

    override fun createWork(): Single<Result> {
        val now = currentTime.currentDateTime()

        val notificationIntervalEnd = now.plusMinutes(NOTIFICATION_INTERVAL_MINUTES)

        val showNotification = if (shouldShowNotifications()) {
            service.sortedFavourites()
                .map { events -> events.filter { it.zonedStartTime.isAfter(now) } }
                .map { events -> events.filter { isBeforeOrEqualTo(it.zonedStartTime, notificationIntervalEnd) } }
                .map(notificationCreator::createFrom)
                .doOnSuccess(notifier::showNotifications)
                .doOnError(::logError)
                .map { Result.success() }
                .onErrorReturnItem(Result.failure())
        } else {
            Single.just(Result.success())
        }

        val scheduleNext = service.sortedFavourites()
            .map { events -> events.filter { it.zonedStartTime.isAfter(notificationIntervalEnd) } }
            .doOnSuccess(::scheduleNextAlarm)
            .doOnError(::logError)
            .map { Result.success() }
            .onErrorReturnItem(Result.failure())

        return Single.zip(
            showNotification,
            scheduleNext,
            BiFunction<Result, Result, Result> { notificationResult, scheduleResult ->
                if (notificationResult is Result.Failure || scheduleResult is Result.Failure) {
                    Result.failure()
                } else {
                    Result.success()
                }
            }

        )
    }

    private fun logError(error: Throwable) {
        Timber.e(error, "Error in NotificationsIntentService: ${error.javaClass}")
    }

    private fun shouldShowNotifications(): Boolean {
        val notificationPreferenceKey = applicationContext.getString(R.string.about_to_start_notification_preference_key)
        return preferences.getBoolean(notificationPreferenceKey, SHOW_NOTIFICATIONS_DEFAULT)
    }

    private fun isBeforeOrEqualTo(start: ZonedDateTime, notificationIntervalEnd: ZonedDateTime): Boolean {
        return start.isBefore(notificationIntervalEnd) || start.isEqual(notificationIntervalEnd)
    }

    private fun scheduleNextAlarm(events: List<Event>) {
        if (events.isEmpty()) {
            Timber.d("No future events to schedule an alarm for.")
            return
        }
        val startTime = events[0].zonedStartTime
        val serviceAlarm = startTime.minusMinutes(NOTIFICATION_INTERVAL_MINUTES)

        Timber.d("Next alarm scheduled for %s", serviceAlarm.toString())

        scheduleNotificationWork(
            Duration.between(
                currentTime.currentDateTime(),
                serviceAlarm
            )
        )
    }

    companion object {
        private const val NOTIFICATION_INTERVAL_MINUTES = 10L
        private const val SHOW_NOTIFICATIONS_DEFAULT = true
    }
}

fun scheduleNotificationWork(delay: Duration = Duration.ZERO) {
    val request = OneTimeWorkRequestBuilder<NotificationWorker>()
        .setInitialDelay(delay.seconds, TimeUnit.SECONDS)
        .build()

    WorkManager.getInstance().enqueue(request)
}