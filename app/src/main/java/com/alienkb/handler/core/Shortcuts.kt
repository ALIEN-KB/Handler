package com.alienkb.handler.core

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmap
import com.alienkb.handler.ui.SafeLaunchActivity

/** Home-screen "lock" shortcuts that pause accessibility *before* the app opens. */
object Shortcuts {

    fun intentFor(ctx: Context, pkg: String): Intent =
        Intent(ctx, SafeLaunchActivity::class.java)
            .setAction(Intent.ACTION_VIEW)
            .putExtra(SafeLaunchActivity.EXTRA_PKG, pkg)

    fun build(ctx: Context, pkg: String): ShortcutInfoCompat? {
        val pm = ctx.packageManager
        return try {
            val ai = pm.getApplicationInfo(pkg, 0)
            val label = pm.getApplicationLabel(ai).toString()
            val icon = badged(pm.getApplicationIcon(ai).toBitmap(192, 192, Bitmap.Config.ARGB_8888))
            ShortcutInfoCompat.Builder(ctx, "safe_$pkg")
                .setShortLabel("🔒 $label")
                .setLongLabel("Safe-open $label")
                .setIcon(IconCompat.createWithBitmap(icon))
                .setIntent(intentFor(ctx, pkg))
                .build()
        } catch (_: Exception) {
            null
        }
    }

    fun requestPin(ctx: Context, pkg: String): Boolean {
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(ctx)) return false
        val info = build(ctx, pkg) ?: return false
        return try {
            ShortcutManagerCompat.requestPinShortcut(ctx, info, null)
        } catch (_: Exception) {
            false
        }
    }

    /** Long-press the Handler icon to see your protected apps. */
    fun syncDynamic(ctx: Context) {
        try {
            val max = ShortcutManagerCompat.getMaxShortcutCountPerActivity(ctx).coerceAtMost(4)
            val list = Prefs(ctx).protectedApps.sorted().mapNotNull { build(ctx, it) }.take(max)
            ShortcutManagerCompat.setDynamicShortcuts(ctx, list)
        } catch (_: Exception) {
        }
    }

    /** App icon with a small mint lock badge in the corner. */
    private fun badged(src: Bitmap): Bitmap {
        val out = src.copy(Bitmap.Config.ARGB_8888, true)
        val c = Canvas(out)
        val s = out.width.toFloat()
        val r = s * 0.2f
        val cx = s - r - s * 0.02f
        val cy = s - r - s * 0.02f
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF07090D.toInt() }
        val mint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF5EF2C6.toInt() }
        val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF5EF2C6.toInt(); style = Paint.Style.STROKE; strokeWidth = s * 0.025f
        }
        c.drawCircle(cx, cy, r, fill)
        c.drawCircle(cx, cy, r, ring)
        val bw = r * 0.95f
        val bh = r * 0.7f
        c.drawRoundRect(RectF(cx - bw / 2, cy - bh * 0.15f, cx + bw / 2, cy + bh * 0.85f), r * 0.12f, r * 0.12f, mint)
        val shackle = Paint(ring).apply { strokeWidth = r * 0.16f }
        val sw = bw * 0.62f
        c.drawArc(RectF(cx - sw / 2, cy - bh * 0.85f, cx + sw / 2, cy + bh * 0.25f), 180f, 180f, false, shackle)
        c.drawLine(cx - sw / 2, cy - bh * 0.3f, cx - sw / 2, cy - bh * 0.1f, shackle)
        c.drawLine(cx + sw / 2, cy - bh * 0.3f, cx + sw / 2, cy - bh * 0.1f, shackle)
        return out
    }
}
