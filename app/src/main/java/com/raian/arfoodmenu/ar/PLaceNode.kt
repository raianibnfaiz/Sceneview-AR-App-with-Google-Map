import android.content.Context
import android.content.res.ColorStateList
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.google.android.filament.Engine
import com.google.android.filament.Texture
import com.google.android.filament.utils.Float3
import com.google.ar.sceneform.rendering.ViewRenderable
import com.raian.arfoodmenu.R
import com.raian.arfoodmenu.model.Place
import io.github.sceneview.collision.Vector3
import io.github.sceneview.math.Position
import io.github.sceneview.math.Transform
import io.github.sceneview.node.Node
import io.github.sceneview.math.toVector3
import io.github.sceneview.node.ViewNode

private const val TAG = "PlaceNode"

class PlaceNode(context: Context, engine: Engine) : Node(engine) {
    private lateinit var textTexture : Texture
    var textView = TextView(context)
    //var renderable: ViewRenderable? = null
    init {
        ViewRenderable.builder()
            .setView(context, TextView(context).also {
                it.text = "Test Text"
                it.setTextColor( ColorStateList.valueOf(Color.Yellow.toArgb()))
                it.layout(100, 100, 400, 200)

            }).build(engine).thenAccept{
                textView = it.view as TextView
                textView.text = "Hello World!"
                textView.setTextColor( ColorStateList.valueOf(Color.Yellow.toArgb()))
                textView.textSize = 60f
                textView.backgroundTintList = ColorStateList.valueOf(Color.Blue.toArgb())
                textView.measure(100, 100)
                textView.layout(100, 100, 200, 200)
                textView.visibility = View.VISIBLE
                Log.d(TAG, "onActivate: ${it.view}")
            }

        ViewRenderable.builder()
            .setView(context, textView).build(engine).thenAccept{
                textView = it.view as TextView
                textView.text = "Hello World!"
                textView.setTextColor( ColorStateList.valueOf(Color.Yellow.toArgb()))
                textView.textSize = 60f
                textView.backgroundTintList = ColorStateList.valueOf(Color.Blue.toArgb())
                textView.measure(100, 100)
                textView.layout(100, 100, 200, 200)
                textView.visibility = View.VISIBLE
                Log.d(TAG, "onActivate: ${it.view}")
            }

        /*val testTextView = TextView(context).also {
              it.text = "Test Text"
              it.setTextColor( ColorStateList.valueOf(Color.Yellow.toArgb()))
              it.layout(100, 100, 400, 200)

          }

          val bitmap = testTextView.drawToBitmap().also { bitmap ->
              textTexture = Texture.Builder()
                  .width(bitmap.width)
                  .height(bitmap.height)
                  .sampler(Texture.Sampler.SAMPLER_3D)
                  .format(Texture.InternalFormat.RGBA4)
                  .build(engine)
                  .also { texture ->
                      TextureHelper.setBitmap(engine, texture, 0, bitmap)
                  }
          }
  */

        /*
                // Position the TextViewNode above the anchor
                textViewNode.transform = Transform(position = Position(0.0f, 1.0f, 0.0f))*/
    }
    fun setPosition(position: Vector3) {
        val float3Position = Position(position.x, position.y, position.z)
        this.transform = Transform(position = float3Position)
    }
}
/*
class PlaceNode(
    val context: Context,
    val place: List<Place>?,
    ) {
    private var inflatedView: View? = null

    fun getView(): View? {
        if (inflatedView == null) {
            inflatedView = inflate(context, R.layout.place_view, null)
        }
        return inflatedView
    }

    fun showInfoWindow() {
        withView { view ->
            val textViewPlace = view.findViewById<TextView>(R.id.placeName)
            place?.let {
                textViewPlace.text = it[0].name
            }
        }

    }

    private fun withView(block: (View) -> Unit) {
        val view = getView()
        if (view != null) {
            block(view)
        } else {
            Log.w(TAG, "withView: View is not ready yet")
        }
    }
}
*/
