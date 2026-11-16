package com.washmycar.ui.components

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

/** Point affiché sur la carte. [isUser] = position de l'utilisateur (marqueur vert). */
data class MapMarker(
    val id: Long,
    val latitude: Double,
    val longitude: Double,
    val title: String,
    val snippet: String? = null,
    val isUser: Boolean = false,
)

/**
 * Carte OpenStreetMap (osmdroid) intégrée à Compose.
 * @param onMarkerClick clic sur un marqueur de station (id)
 * @param onMapTap clic sur la carte (choix d'un emplacement)
 * @param interactive false = carte figée (aperçu dans une page qui défile)
 */
@SuppressLint("ClickableViewAccessibility")
@Composable
fun OsmMap(
    centerLatitude: Double,
    centerLongitude: Double,
    markers: List<MapMarker>,
    modifier: Modifier = Modifier,
    zoom: Double = 13.0,
    interactive: Boolean = true,
    onMarkerClick: ((Long) -> Unit)? = null,
    onMapTap: ((Double, Double) -> Unit)? = null,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val currentMarkerClick by rememberUpdatedState(onMarkerClick)
    val currentMapTap by rememberUpdatedState(onMapTap)

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(interactive)
            zoomController.setVisibility(
                if (interactive) CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT
                else CustomZoomButtonsController.Visibility.NEVER
            )
            controller.setZoom(zoom)
            controller.setCenter(GeoPoint(centerLatitude, centerLongitude))
            if (!interactive) setOnTouchListener { _, _ -> true }
        }
    }

    // La carte suit le cycle de vie de l'écran (chargement des tuiles, libération des ressources).
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    LaunchedEffect(centerLatitude, centerLongitude) {
        mapView.controller.setCenter(GeoPoint(centerLatitude, centerLongitude))
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier,
        update = { map ->
            map.overlays.clear()
            if (currentMapTap != null) {
                map.overlays.add(MapEventsOverlay(object : MapEventsReceiver {
                    override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                        currentMapTap?.invoke(p.latitude, p.longitude)
                        return true
                    }

                    override fun longPressHelper(p: GeoPoint) = false
                }))
            }
            markers.forEach { m ->
                val marker = Marker(map).apply {
                    position = GeoPoint(m.latitude, m.longitude)
                    title = m.title
                    snippet = m.snippet
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    // Copie indépendante (l'icône par défaut d'osmdroid est partagée par tous les marqueurs) :
                    // stations en bleu, utilisateur en vert.
                    val couleur = if (m.isUser) android.graphics.Color.rgb(21, 128, 61) else android.graphics.Color.rgb(3, 105, 161)
                    icon = icon?.constantState?.newDrawable(map.resources)?.mutate()?.apply { setTint(couleur) }
                    setOnMarkerClickListener { mk, _ ->
                        val click = currentMarkerClick
                        if (click != null && !m.isUser) click(m.id) else mk.showInfoWindow()
                        true
                    }
                }
                map.overlays.add(marker)
            }
            map.invalidate()
        },
    )
}
