package com.example.pdjissen.ui.dashboard

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.*
import java.util.concurrent.TimeUnit

// 徒歩用 LocationTracker。GPS のドリフト・ノイズ補正を行う。
class LocationTracker(
    private val context: Context,
    private val onLocationUpdated: (Location, Float) -> Unit // (位置, 累計距離) を返すコールバック
) {

    private var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback
    private var locationRequest: LocationRequest

    private var lastLocation: Location? = null
    private var totalDistance = 0f

    // 誤差対策パラメータ（ノイズ・ドリフト対策）
    // 1.3m: 通常の歩行（約 1.1m/s）を捉える最小移動距離
    private val MIN_DISTANCE_THRESHOLD = 1.3f

    // 12.0m: GPS 精度許容上限
    private val MAX_ACCURACY_THRESHOLD = 12.0f

    // 5.5m/s (約 20km/h): 走行速度の上限（GPS 飛び対策）
    private val MAX_SPEED_THRESHOLD = 5.5f

    init {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        // 徒歩用: 高精度・1 秒間隔
        locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            TimeUnit.SECONDS.toMillis(1)
        ).setMinUpdateIntervalMillis(TimeUnit.SECONDS.toMillis(1))
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { newLocation ->
                    processNewLocation(newLocation)
                }
            }
        }
    }

    // 位置情報の精査と距離加算
    private fun processNewLocation(newLocation: Location) {

        // 精度チェック
        if (newLocation.accuracy > MAX_ACCURACY_THRESHOLD) {
            Log.d("LocationTracker", "精度低 (${newLocation.accuracy}m) -> 無視 (閾値: $MAX_ACCURACY_THRESHOLD m)")
            onLocationUpdated(newLocation, totalDistance) // マーカー位置は更新する
            return
        }

        // 初回位置の保存
        if (lastLocation == null) {
            lastLocation = newLocation
            onLocationUpdated(newLocation, totalDistance)
            return
        }

        // 距離と時間の計算
        val distance = newLocation.distanceTo(lastLocation!!)
        val timeDelta = (newLocation.elapsedRealtimeNanos - lastLocation!!.elapsedRealtimeNanos) / 1_000_000_000.0
        val calculatedSpeed = if (timeDelta > 0) distance / timeDelta else 0.0

        // ノイズ・ドリフト除外
        if (distance < MIN_DISTANCE_THRESHOLD) {
            Log.d("LocationTracker", "微小移動 (${String.format("%.2f", distance)}m) -> ノイズとして無視 (閾値: $MIN_DISTANCE_THRESHOLD m)")
            onLocationUpdated(newLocation, totalDistance)
            return
        }

        // GPS 飛び除外
        if (calculatedSpeed > MAX_SPEED_THRESHOLD) {
            Log.d("LocationTracker", "異常速度 (${String.format("%.2f", calculatedSpeed)}m/s) -> GPS飛びとして無視 (閾値: $MAX_SPEED_THRESHOLD m/s)")
            onLocationUpdated(newLocation, totalDistance)
            return
        }

        // 採用：累計距離に加算し、基準点を更新
        totalDistance += distance
        lastLocation = newLocation

        Log.d("LocationTracker", "距離更新: +${String.format("%.2f", distance)}m (計: ${String.format("%.1f", totalDistance)}m)")
        onLocationUpdated(newLocation, totalDistance)
    }

    // 計測開始
    @SuppressLint("MissingPermission")
    fun start() {
        Log.d("LocationTracker", "位置情報の更新を開始します")
        lastLocation = null
        totalDistance = 0f

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        } catch (e: Exception) {
            Log.e("LocationTracker", "requestLocationUpdatesでエラー", e)
        }
    }

    // 計測停止
    fun stop() {
        Log.d("LocationTracker", "位置情報の更新を停止します")
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    // 最後の既知の現在地を取得
    @SuppressLint("MissingPermission")
    fun getLastKnownLocation(onSuccess: (Location?) -> Unit) {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location -> onSuccess(location) }
            .addOnFailureListener { onSuccess(null) }
    }
}