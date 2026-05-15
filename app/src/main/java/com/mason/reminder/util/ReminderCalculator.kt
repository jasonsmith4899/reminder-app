package com.mason.reminder.util

import com.mason.reminder.data.model.IntervalUnit
import com.mason.reminder.data.model.ReminderLevel
import com.mason.reminder.data.model.Task
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * 计算提醒时间、下次到期时间、倒计时天数。
 */
object ReminderCalculator {

    private val NOTIFY_TIME = LocalTime.of(9, 0)

    /**
     * 根据提醒级别计算首次提醒时间。
     *
     * - LIGHT:  到期当天 09:00
     * - MEDIUM: 到期前 3 天 09:00
     * - HEAVY:  到期前 15 天 09:00
     * - CUSTOM: 到期前 customAdvanceDays 天 09:00
     *
     * @param dueDate         到期日期
     * @param level           提醒级别
     * @param customAdvanceDays 自定义提前天数（仅 CUSTOM 级别需要）
     * @return 首次提醒的 LocalDateTime；如果提前天数导致提醒时间在过去则返回到期当天 09:00
     */
    fun firstNotifyTime(
        dueDate: LocalDate,
        level: ReminderLevel,
        customAdvanceDays: Int? = null
    ): LocalDateTime {
        val advanceDays = when (level) {
            ReminderLevel.LIGHT  -> 0
            ReminderLevel.MEDIUM -> 3
            ReminderLevel.HEAVY  -> 15
            ReminderLevel.CUSTOM -> customAdvanceDays ?: 0
        }
        val notifyDate = dueDate.minusDays(advanceDays.toLong())
        // 如果提醒日期在今天之前，退回到到期当天（至少到期当天要提醒）
        val effectiveDate = if (notifyDate < LocalDate.now()) dueDate else notifyDate
        return LocalDateTime.of(effectiveDate, NOTIFY_TIME)
    }

    /**
     * 计算循环任务的下次到期时间。
     *
     * 规则：lastCompletedAt + interval；若无 lastCompletedAt 则 start_date + interval。
     *
     * @param task 循环任务
     * @return 下次到期日期的 epoch millis
     */
    fun nextDueDate(task: Task): Long {
        val baseDate = if (task.lastCompletedAt != null) {
            task.lastCompletedAt.toLocalDate()
        } else {
            task.startDate?.toLocalDate() ?: LocalDate.now()
        }

        val nextDate = when (task.intervalUnit) {
            IntervalUnit.DAY   -> baseDate.plusDays(task.intervalValue?.toLong() ?: 1)
            IntervalUnit.WEEK  -> baseDate.plusWeeks(task.intervalValue?.toLong() ?: 1)
            IntervalUnit.MONTH -> baseDate.plusMonths(task.intervalValue?.toLong() ?: 1)
            null               -> baseDate // fallback，不应出现
        }

        return nextDate.toEpochMillis()
    }

    /**
     * 计算倒计时天数（从 [now] 到 [dueDate] 的天数差）。
     *
     * 正数表示还有几天到期，0 表示今天到期，负数表示已过期天数。
     */
    fun countdownDays(dueDate: LocalDate, now: LocalDate): Long {
        return dueDate.toEpochDay() - now.toEpochDay()
    }
}