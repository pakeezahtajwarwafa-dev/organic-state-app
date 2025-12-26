package com.example.organicstate.utils

import android.content.Context
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

// Toast extensions
fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

fun Context.showLongToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}

// View extensions
fun View.show() {
    visibility = View.VISIBLE
}

fun View.hide() {
    visibility = View.GONE
}

fun View.invisible() {
    visibility = View.INVISIBLE
}

fun View.enable() {
    isEnabled = true
    alpha = 1.0f
}

fun View.disable() {
    isEnabled = false
    alpha = 0.5f
}

// ImageView extensions
fun ImageView.loadImage(url: String?, placeholder: Int = android.R.drawable.ic_menu_gallery) {
    if (url.isNullOrEmpty()) {
        setImageResource(placeholder)
        return
    }

    Glide.with(this.context)
        .load(url)
        .placeholder(placeholder)
        .error(placeholder)
        .diskCacheStrategy(DiskCacheStrategy.ALL)
        .into(this)
}

fun ImageView.loadCircularImage(url: String?, placeholder: Int = android.R.drawable.ic_menu_gallery) {
    if (url.isNullOrEmpty()) {
        setImageResource(placeholder)
        return
    }

    Glide.with(this.context)
        .load(url)
        .placeholder(placeholder)
        .error(placeholder)
        .circleCrop()
        .diskCacheStrategy(DiskCacheStrategy.ALL)
        .into(this)
}

// Number formatting
fun Double.toCurrency(): String {
    return "৳${String.format("%.2f", this)}"
}

fun Double.toPrice(): String {
    return String.format("%.2f", this)
}

fun Int.toQuantityString(): String {
    return if (this == 1) "$this item" else "$this items"
}

// Date formatting
fun Long.toDateString(): String {
    val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    return sdf.format(Date(this))
}

fun Long.toSimpleDate(): String {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return sdf.format(Date(this))
}

fun Long.toTimeAgo(): String {
    val now = System.currentTimeMillis()
    val diff = now - this

    return when {
        diff < 60000 -> "Just now"
        diff < 3600000 -> "${diff / 60000} min ago"
        diff < 86400000 -> "${diff / 3600000} hours ago"
        diff < 604800000 -> "${diff / 86400000} days ago"
        else -> toSimpleDate()
    }
}

// String extensions
fun String.isValidEmail(): Boolean {
    return android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()
}

fun String.isValidPhone(): Boolean {
    return this.length >= 10 && this.all { it.isDigit() }
}

fun String.capitalize(): String {
    return this.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
    }
}
