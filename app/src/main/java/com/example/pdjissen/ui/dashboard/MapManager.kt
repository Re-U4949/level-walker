// 地図関連
package com.example.pdjissen.ui.dashboard

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import okhttp3.*
import org.json.JSONObject
import com.google.maps.android.PolyUtil
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.model.Polyline
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import java.io.IOException

// GoogleMap 操作のラッパークラス
class MapManager(private val context: Context) {

    private var googleMap: GoogleMap? = null
    private var currentMarker: Marker? = null

    // 地図の準備完了時に呼び出される
    fun setupMap(map: GoogleMap) {
        googleMap = map
    }

    // 現在地表示を有効化（権限チェックは呼び出し側で行う）
    @SuppressLint("MissingPermission")
    fun enableMyLocation() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap?.isMyLocationEnabled = true
            googleMap?.uiSettings?.isMyLocationButtonEnabled = true
        }
    }

    // カメラ移動（ズームあり）
    fun moveCameraTo(latLng: LatLng, zoomLevel: Float = 17f) {
        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoomLevel))
    }

    // カメラ移動（ズームなし・アニメーションあり）
    fun animateCameraTo(latLng: LatLng) {
        googleMap?.animateCamera(CameraUpdateFactory.newLatLng(latLng))
    }

    // 現在地マーカーの更新（未作成なら追加）
    fun updateMarker(latLng: LatLng, title: String = "現在地") {
        if (currentMarker == null) {
            currentMarker = googleMap?.addMarker(MarkerOptions().position(latLng).title(title))
        } else {
            currentMarker?.position = latLng
        }
    }

    // Fragment 破棄時に呼ぶ
    fun cleanup() {
        googleMap = null
        currentMarker = null
    }

    // 描画中のルート線
    private var currentRoutePolyline: Polyline? = null

    // ルート検索して描画。コールバックで距離テキストを返す。
    fun drawRoute(origin: LatLng, destination: LatLng, apiKey: String, onDistanceFound: (String) -> Unit) {
        // Directions API リクエスト URL
        val url = "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=${origin.latitude},${origin.longitude}" +
                "&destination=${destination.latitude},${destination.longitude}" +
                "&mode=walking" +
                "&key=$apiKey"

        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        // 非同期通信
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) return

                val json = response.body?.string() ?: return

                try {
                    val jsonObject = JSONObject(json)
                    val routes = jsonObject.getJSONArray("routes")

                    if (routes.length() > 0) {
                        val route = routes.getJSONObject(0)

                        // routes[0] -> legs[0] -> distance.text (例: "1.2 km")
                        val legs = route.getJSONArray("legs")
                        val leg = legs.getJSONObject(0)
                        val distanceObj = leg.getJSONObject("distance")
                        val distanceText = distanceObj.getString("text")

                        // ルート形状の polyline
                        val overviewPolyline = route.getJSONObject("overview_polyline")
                        val encodedString = overviewPolyline.getString("points")
                        val path = PolyUtil.decode(encodedString)

                        // メインスレッドで描画
                        Handler(Looper.getMainLooper()).post {
                            currentRoutePolyline?.remove()

                            val lineOptions = PolylineOptions()
                                .addAll(path)
                                .color(Color.BLUE)
                                .width(15f)

                            currentRoutePolyline = googleMap?.addPolyline(lineOptions)

                            onDistanceFound(distanceText)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        })
    }

    fun drawPresetRoute(routePoints: List<LatLng>) {
        val lineOptions = PolylineOptions()
            .addAll(routePoints)
            .color(Color.MAGENTA)
            .width(12f)
            .clickable(false) // タップ無効

        googleMap?.addPolyline(lineOptions)
    }
}