package com.raian.arfoodmenu


import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.filament.Engine
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.ar.core.Anchor
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Pose
import com.google.ar.core.TrackingFailureReason
import com.google.ar.core.TrackingState
import com.raian.arfoodmenu.api.NearbyPlacesResponse
import com.raian.arfoodmenu.api.PlacesService
import com.raian.arfoodmenu.ui.theme.ARFoodMenuTheme
import io.github.sceneview.ar.ARScene
import io.github.sceneview.ar.getDescription
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.ar.rememberARCameraNode
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.model.ModelInstance
import io.github.sceneview.node.CubeNode
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberCollisionSystem
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes
import io.github.sceneview.rememberOnGestureListener
import io.github.sceneview.rememberView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

private const val kModelFile = "damaged_helmet.glb"
private const val kMaxModelInstances = 10
private const val TAG = "MainActivity"
class MainActivity : ComponentActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var mapView: MapView
    private var currentLocation by mutableStateOf(LatLng(40.7128, -74.0060)) // Default to New York
    private lateinit var placesService: PlacesService
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        placesService = PlacesService.create()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setContent {
            ARFoodMenuTheme {
                BoxWithConstraints(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val screenHeight = constraints.maxHeight
                    val arSceneHeight = screenHeight * 1 / 4
                    val mapHeight = screenHeight - arSceneHeight
                    val childNodes = rememberNodes()
                    val engine = rememberEngine()
                    val modelLoader = rememberModelLoader(engine)
                    val materialLoader = rememberMaterialLoader(engine)
                    val cameraNode = rememberARCameraNode(engine)
                    val view = rememberView(engine)
                    val collisionSystem = rememberCollisionSystem(view)

                    var planeRenderer by remember { mutableStateOf(false) }
                    var session by remember { mutableStateOf<com.google.ar.core.Session?>(null) }
                    var frame by remember { mutableStateOf<Frame?>(null) }
                    var trackingFailureReason by remember {
                        mutableStateOf<TrackingFailureReason?>(null)
                    }
                    val modelInstances = remember { mutableListOf<ModelInstance>() }
                    Column {
                        // ARScene Box
                        Box(
                            modifier = Modifier
                                .height(arSceneHeight.dp)
                                .fillMaxWidth()
                        ) {
                            // Your ARScene implementation here
                            // ...

                            ARScene(
                                modifier = Modifier.fillMaxSize(),
                                childNodes = childNodes,
                                engine = engine,
                                view = view,
                                modelLoader = modelLoader,
                                collisionSystem = collisionSystem,
                                sessionConfiguration = { updatedSession, config ->
                                    session = updatedSession
                                    config.depthMode =
                                        when (updatedSession.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                                            true -> Config.DepthMode.AUTOMATIC
                                            else -> Config.DepthMode.DISABLED
                                        }
                                    config.instantPlacementMode =
                                        Config.InstantPlacementMode.LOCAL_Y_UP
                                    config.lightEstimationMode =
                                        Config.LightEstimationMode.ENVIRONMENTAL_HDR
                                },
                                cameraNode = cameraNode,
                                planeRenderer = planeRenderer,
                                onTrackingFailureChanged = {
                                    trackingFailureReason = it
                                },
                                onSessionUpdated = { _, updatedFrame ->
                                    frame = updatedFrame
                                    // Rest of the onSessionUpdated code...
                                },
                                onGestureListener = rememberOnGestureListener(
                                    onSingleTapConfirmed = { _, _ ->
                                        if (session != null && frame != null && frame!!.camera.trackingState == TrackingState.TRACKING) {
                                            val cameraPose = frame!!.camera.displayOrientedPose
                                            val offsets = listOf(
                                                Pose.makeTranslation(
                                                    0f,
                                                    0f,
                                                    -1f
                                                ), // In front of the camera
                                                Pose.makeTranslation(
                                                    0.5f,
                                                    0f,
                                                    -1f
                                                ), // Right and in front of the camera
                                                Pose.makeTranslation(
                                                    -0.5f,
                                                    0f,
                                                    -1f
                                                ) // Left and in front of the camera
                                            )

                                            offsets.forEach { offset ->
                                                val anchorPose = cameraPose.compose(offset)
                                                val anchor = session?.createAnchor(anchorPose)

                                                anchor?.let {
                                                    childNodes += createAnchorNode(
                                                        engine = engine,
                                                        modelLoader = modelLoader,
                                                        materialLoader = materialLoader,
                                                        modelInstances = modelInstances,
                                                        anchor = it
                                                    )
                                                }
                                            }
                                        }
                                    }
                                )


                            )

                            Text(
                                modifier = Modifier
                                    .systemBarsPadding()
                                    .fillMaxWidth()
                                    .align(Alignment.TopCenter)
                                    .padding(top = 16.dp, start = 32.dp, end = 32.dp),
                                textAlign = TextAlign.Center,
                                fontSize = 28.sp,
                                color = Color.White,
                                text = trackingFailureReason?.let {
                                    it.getDescription(LocalContext.current)
                                } ?: if (childNodes.isEmpty()) {
                                    stringResource(R.string.point_your_phone_down)
                                } else {
                                    stringResource(R.string.tap_anywhere_to_add_model)
                                }
                            )

                            // The rest of your ARScene related code
                            // ...
                        }

                        // Google Map Box
                        Box(
                            modifier = Modifier
                                .height(mapHeight.dp)
                                .fillMaxWidth()
                        ) {
                            GoogleMapComponent(currentLocation)
                        }
                    }
                }

            }
        }
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }
        getCurrentLocation()

    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 100
    }
    private fun getNearbyPlaces(location: LatLng) {
        val apiKey = "AIzaSyBxeQ3OAnwaRSmDTQ-hoWyeRkIhrzZYXJY"
        placesService.nearbyPlaces(
            apiKey = apiKey,
            location = "${location.latitude},${location.longitude}",
            radiusInMeters = 2000,
            placeType = "restaurant" // Example type
        ).enqueue(object : Callback<NearbyPlacesResponse> {
            override fun onFailure(call: Call<NearbyPlacesResponse>, t: Throwable) {
                Log.e("MainActivity", "Failed to get nearby places", t)
            }

            override fun onResponse(
                call: Call<NearbyPlacesResponse>,
                response: Response<NearbyPlacesResponse>
            ) {
                if (!response.isSuccessful) {
                    Log.e("MainActivity", "Failed to get nearby places")
                    return
                }

                val places = response.body()?.results ?: emptyList()
                //Log.d("MainActivity", "Nearby Places: $places")
                Log.d(TAG, "onResponse: \"Nearby Places: $places\"")
            }
        })
    }
    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    updateMapLocation(LatLng(location.latitude, location.longitude))
                }
            }
    }

    private fun updateMapLocation(location: LatLng) {
        currentLocation = location // Update the mutable state
        getNearbyPlaces(currentLocation)
    }
}


@Composable
fun GoogleMapComponent(currentLocation: LatLng) {
    val mapView = rememberMapViewWithLifecycle()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        AndroidView({ mapView }) { mapView ->
            mapView.getMapAsync { googleMap ->
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15f))
            }
        }
    }
}


// Helper function to create a MapView with lifecycle
@Composable
fun rememberMapViewWithLifecycle(): MapView {
    val context = LocalContext.current
    val mapView = remember {
        MapView(context)
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val lifecycleObserver = getMapLifecycleObserver(mapView)
        lifecycle.addObserver(lifecycleObserver)
        onDispose {
            lifecycle.removeObserver(lifecycleObserver)
        }
    }
    return mapView
}

fun getMapLifecycleObserver(mapView: MapView): LifecycleEventObserver =
    LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_CREATE -> mapView.onCreate(Bundle())
            Lifecycle.Event.ON_START -> mapView.onStart()
            Lifecycle.Event.ON_RESUME -> mapView.onResume()
            Lifecycle.Event.ON_PAUSE -> mapView.onPause()
            Lifecycle.Event.ON_STOP -> mapView.onStop()
            Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
            else -> throw IllegalStateException()
        }
    }

//create anchore
private fun createAnchorNode(
    engine: Engine,
    modelLoader: ModelLoader,
    materialLoader: MaterialLoader,
    modelInstances: MutableList<ModelInstance>,
    anchor: Anchor
): AnchorNode {
    val anchorNode = AnchorNode(engine = engine, anchor = anchor)
    val modelNode = ModelNode(
        modelInstance = modelInstances.apply {
            if (isEmpty()) {
                this += modelLoader.createInstancedModel(kModelFile, kMaxModelInstances)
            }
        }.removeLast(),
        scaleToUnits = 0.5f
    ).apply {
        isEditable = true
    }
    val boundingBoxNode = CubeNode(
        engine,
        size = modelNode.extents,
        center = modelNode.center,
        materialInstance = materialLoader.createColorInstance(Color.White.copy(alpha = 0.5f))
    ).apply {
        isVisible = false
    }
    modelNode.addChildNode(boundingBoxNode)
    anchorNode.addChildNode(modelNode)

    listOf(modelNode, anchorNode).forEach {
        it.onEditingChanged = { editingTransforms ->
            boundingBoxNode.isVisible = editingTransforms.isNotEmpty()
        }
    }
    return anchorNode
}

