package cl.mastermartini.app.nativeaccess

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast

fun buzz(ctx: Context, enabled: Boolean) {
    if (!enabled) return
    val v = ctx.getSystemService(Vibrator::class.java) ?: return
    if (v.hasVibrator()) v.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
}

private fun launch(ctx: Context, i: Intent) {
    try { ctx.startActivity(i) }
    catch (e: ActivityNotFoundException) {
        Toast.makeText(ctx, "No hay una aplicación disponible para esta acción", Toast.LENGTH_SHORT).show()
    }
}

fun dial(ctx: Context, phone: String) =
    launch(ctx, Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone.filter { it.isDigit() || it == '+' })))

fun sendEmail(ctx: Context, to: String, subject: String = "", body: String = "") =
    launch(ctx, Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).apply {
        putExtra(Intent.EXTRA_EMAIL, arrayOf(to))
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, body)
    })

fun openMap(ctx: Context, address: String) =
    launch(ctx, Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(address))))

fun openUrl(ctx: Context, url: String) =
    launch(ctx, Intent(Intent.ACTION_VIEW, Uri.parse(url)))
