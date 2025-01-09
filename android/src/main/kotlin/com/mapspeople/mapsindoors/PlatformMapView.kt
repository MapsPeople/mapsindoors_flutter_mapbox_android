package com.mapspeople.mapsindoors

import android.animation.Animator
import android.annotation.SuppressLint
import android.content.Context
import com.mapsindoors.core.MPFloorSelectorInterface
import com.mapsindoors.mapbox.MPMapConfig
import com.google.gson.Gson
import android.view.View
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.toCameraOptions
import com.mapbox.maps.MapInitOptions
import com.mapbox.maps.plugin.attribution.attribution
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.flyTo
import com.mapspeople.mapsindoors.core.*
import com.mapspeople.mapsindoors.core.models.*

abstract class PlatformMapView(private val context: Context, private val args: HashMap<*,*>?) : PlatformMapViewInterface {
    private val mMap: MapView
    private var mMapboxMap: MapboxMap? = null

    init {
        val options = Gson().fromJson(args?.get("initialCameraPosition") as? String, CameraPosition::class.java)?.toCameraState()?.toCameraOptions()

        mMap = MapView(context, MapInitOptions(context, cameraOptions = options))

        mMapboxMap = mMap.getMapboxMap()
        mMapboxMap?.loadStyle(Style.MAPBOX_STREETS)
        //TODO: This solution is temporary until we find a way to make the attribution view compatible with Flutter
        mMap?.attribution?.updateSettings {
            this.enabled = false
        }
        whenMapReady()
    }

    @SuppressLint("Lifecycle")
    override fun disposeMap() {
        mMap.onDestroy()
        mMapboxMap = null
    }

    override fun getMapView(): View {
        return mMap
    }

    override fun makeMPConfig(config: MapConfig?, floorSelectorInterface: MPFloorSelectorInterface) : MPMapConfig? {
        return config?.makeMPMapConfig(context, mMapboxMap!!, mMap, context.getString(R.string.mapbox_api_key), floorSelectorInterface);
    }

    override val currentCameraPosition: CameraPosition? get() {
        return mMapboxMap?.cameraState?.toCameraPosition()
    }

    override fun updateCamera(move: Boolean, update: CameraUpdate, duration: Int?, success: () -> Unit) {
        mMapboxMap?.let { update.toCameraOptions(it) }?.let {
            if (move) {
                mMapboxMap?.setCamera(it)
                success()
            } else if (duration != null) {
                mMapboxMap?.flyTo(it, MapAnimationOptions.mapAnimationOptions {
                    duration(duration.toLong())
                }, object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {
                    /* Do nothing */
                }

                override fun onAnimationEnd(animation: Animator) {
                    success()
                }

                override fun onAnimationCancel(animation: Animator) {
                    success()
                }

                override fun onAnimationRepeat(animation: Animator) {
                    /* Do nothing */
                }
            })
            } else {
                mMapboxMap?.flyTo(it)
                success()
            }
        }
    }
}