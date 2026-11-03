package com.clairjour.app.shield

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import com.clairjour.app.R

/**
 * Black panel drawn above Instagram by the accessibility service. It swallows the touches
 * in its area, so the covered content can neither be seen nor scrolled. On the Explore grid
 * it offers a shortcut to the search bar.
 */
class CoverOverlay(
    private val context: Context,
    private val onSearchAccounts: () -> Unit
) {

    private val windowManager = context.getSystemService(WindowManager::class.java)
    private var view: View? = null
    private var shownRule: ShieldRule? = null

    fun show(bounds: NodeBounds, rule: ShieldRule) {
        // Leaves a small gap so that the bottom of Instagram's search bar is not hidden.
        val topGap = if (rule == ShieldRule.EXPLORE_GRID) dp(SEARCH_BAR_GAP_DP) else 0
        val params = WindowManager.LayoutParams(
            bounds.width,
            (bounds.height - topGap).coerceAtLeast(1),
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.OPAQUE
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = bounds.left
            y = bounds.top + topGap
        }
        val current = view
        if (current != null && shownRule == rule) {
            windowManager.updateViewLayout(current, params)
            return
        }
        hide()
        val created = createView(rule)
        windowManager.addView(created, params)
        view = created
        shownRule = rule
    }

    fun hide() {
        view?.let { runCatching { windowManager.removeView(it) } }
        view = null
        shownRule = null
    }

    private fun createView(rule: ShieldRule): View {
        val backgroundColor = color(R.color.shield_cover_background)
        val textColor = color(R.color.shield_cover_text)
        val accent = color(R.color.clairjour_accent)

        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(32), 0, dp(32), 0)

            addView(
                ImageView(context).apply {
                    setImageResource(R.drawable.ic_shield_cover)
                    setColorFilter(color(R.color.clairjour_surface_light))
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(accent)
                    }
                    val padding = dp(16)
                    setPadding(padding, padding, padding, padding)
                },
                LinearLayout.LayoutParams(dp(ICON_SIZE_DP), dp(ICON_SIZE_DP))
            )
            addView(
                text(R.string.shield_cover_title, textColor, TITLE_TEXT_SIZE_SP).apply {
                    typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                },
                spacedBelow(20)
            )
            if (rule == ShieldRule.EXPLORE_GRID) {
                addView(
                    text(R.string.shield_cover_search_accounts, textColor, BODY_TEXT_SIZE_SP)
                        .apply {
                            typeface = Typeface.DEFAULT_BOLD
                            background = GradientDrawable().apply {
                                cornerRadius = dp(24).toFloat()
                                setColor(color(R.color.clairjour_primary))
                                setStroke(dp(1), accent)
                            }
                            setPadding(dp(24), dp(12), dp(24), dp(12))
                            isClickable = true
                            setOnClickListener { onSearchAccounts() }
                        },
                    spacedBelow(28).apply { width = LinearLayout.LayoutParams.WRAP_CONTENT }
                )
            }
        }

        return FrameLayout(context).apply {
            setBackgroundColor(backgroundColor)
            // Swallows touches so the hidden content cannot be scrolled.
            isClickable = true
            addView(
                content,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER
                )
            )
        }
    }

    private fun text(textRes: Int, textColor: Int, sizeSp: Float) = TextView(context).apply {
        setText(textRes)
        setTextColor(textColor)
        textSize = sizeSp
        gravity = Gravity.CENTER
    }

    private fun spacedBelow(topDp: Int) = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    ).apply { topMargin = dp(topDp) }

    private fun color(@ColorRes id: Int) = ContextCompat.getColor(context, id)

    private fun dp(value: Int) = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        value.toFloat(),
        context.resources.displayMetrics
    ).toInt()

    private companion object {
        const val ICON_SIZE_DP = 64
        const val TITLE_TEXT_SIZE_SP = 22f
        const val BODY_TEXT_SIZE_SP = 15f
        const val SEARCH_BAR_GAP_DP = 12
    }
}
