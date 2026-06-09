package io.github.darrindeyoung791.habitpulse.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import io.github.darrindeyoung791.habitpulse.R
import io.github.darrindeyoung791.habitpulse.data.model.Habit
import java.time.LocalTime
import java.time.format.DateTimeFormatter

object NotificationSender {

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun replaceTemplateVariables(
        template: String,
        habit: Habit,
        checkinTime: LocalTime = LocalTime.now()
    ): String {
        var result = template
        result = result.replace("{habit_name}", habit.title)
        result = result.replace("{checkin_time}", checkinTime.format(timeFormatter))
        result = result.replace("{habit_notes}", habit.notes.ifBlank { "" })
        return result
    }

    fun sendEmail(context: Context, emails: List<String>, subject: String, body: String) {
        if (emails.isEmpty()) return
        val recipients = emails.joinToString(",")
        val uri = Uri.parse("mailto:$recipients")
        val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(
                context,
                context.getString(R.string.notification_confirm_no_email_app),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun sendSms(context: Context, phone: String, body: String) {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phone")).apply {
            putExtra("sms_body", body)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(
                context,
                context.getString(R.string.notification_confirm_no_sms_app),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
