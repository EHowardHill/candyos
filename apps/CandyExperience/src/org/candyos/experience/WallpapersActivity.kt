package org.candyos.experience

import android.app.Activity
import android.app.WallpaperManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.GridView
import android.widget.ImageView
import android.widget.Toast

/**
 * The CandyOS bundled wallpapers - every image in assets/wallpapers/ at the
 * last candyos-gen-wallpapers.py run, listed as a Holo grid of thumbnails
 * (the 4.4 Gallery pick-a-wallpaper feel). A tap decodes the full image,
 * subsampled to the screen, and applies it as the system AND lock
 * wallpaper; the app is platform-signed so SET_WALLPAPER_HINTS (signature)
 * is granted - if that ever stops holding, fall back to the system wallpaper
 * only rather than failing. The list is data-driven from the generated
 * values/candyos_wallpapers.xml string-array, so folder edits need no code
 * change here.
 */
class WallpapersActivity : Activity() {

    private lateinit var wallpapers: List<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.wallpapers_activity)

        wallpapers = resources.getStringArray(R.array.candyos_wallpapers).toList()
        val list = requireViewById<GridView>(android.R.id.list)
        list.adapter = WallpaperAdapter()
        list.setOnItemClickListener { _, _, position, _ -> apply(position) }
    }

    private fun drawableId(name: String): Int =
        resources.getIdentifier(name, "drawable", packageName)

    /** Thumb for the grid: decode scaled to ~256px, reused across cells. */
    private fun thumb(name: String): Bitmap? {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeResource(resources, drawableId(name), opts)
        var sample = 1
        while (maxOf(opts.outWidth, opts.outHeight) / (sample * 2) >= 256) {
            sample *= 2
        }
        return BitmapFactory.decodeResource(resources, drawableId(name),
            BitmapFactory.Options().apply { inSampleSize = sample })
    }

    /** The full image, subsampled to roughly the screen - WallpaperManager
     *  would otherwise hold the raw 40MP bitmap it gets handed. */
    private fun fullBitmap(name: String): Bitmap {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeResource(resources, drawableId(name), opts)
        val metrics: DisplayMetrics = resources.displayMetrics
        var sample = 1
        while ((opts.outWidth / (sample * 2)) >= metrics.widthPixels &&
               (opts.outWidth / (sample * 2)) >= metrics.heightPixels) {
            sample *= 2
        }
        return BitmapFactory.decodeResource(resources, drawableId(name),
            BitmapFactory.Options().apply { inSampleSize = sample })
            ?: throw IllegalStateException("wallpaper decode failed: $name")
    }

    private fun apply(position: Int) {
        val name = wallpapers[position]
        val wm = WallpaperManager.getInstance(this)
        val bitmap = fullBitmap(name)
        try {
            wm.setBitmap(bitmap, null, true,
                WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK)
        } catch (e: SecurityException) {
            // No SET_WALLPAPER_HINTS (lost the platform signature?) - the
            // lock wallpaper stays whatever it was; still set the system one.
            wm.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
        }
        bitmap.recycle()
        Toast.makeText(this, R.string.wallpaper_set_toast, Toast.LENGTH_SHORT).show()
    }

    private inner class WallpaperAdapter : BaseAdapter() {
        override fun getCount(): Int = wallpapers.size
        override fun getItem(position: Int): Any = wallpapers[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView as? ImageView ?: ImageView(parent.context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    (128 * resources.displayMetrics.density).toInt())
                scaleType = ImageView.ScaleType.CENTER_CROP
                background = context.getDrawable(android.R.drawable.panel_background)
            }
            view.setImageBitmap(thumb(wallpapers[position]))
            return view
        }
    }
}