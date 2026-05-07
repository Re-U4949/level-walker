package com.example.pdjissen.ui.dashboard

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.pdjissen.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions

import com.example.pdjissen.ui.shared.UserStatusViewModel
import androidx.fragment.app.activityViewModels

class DashboardFragment : Fragment(), OnMapReadyCallback {

    // UI
    private lateinit var textDistance: TextView
    private lateinit var stepCountText: TextView
    private lateinit var btnStart: Button
    private lateinit var btnStop: Button
    private lateinit var mapView: MapView

    private lateinit var textDestination: TextView

    // 地図関連
    private var googleMap: GoogleMap? = null
    private var currentMarker: Marker? = null
    private var destinationMarker: Marker? = null

    private var mapManager: MapManager? = null

    // 計測管理
    private var pedometerManager: PedometerManager? = null
    private var locationTracker: LocationTracker? = null
    private var lastLocation: Location? = null
    private var totalDistance = 0f
    private var isTracking = false

    // センサー値（前回値）
    private var lastTotalStepsSensorValue: Int = 0

    // セッション中の一時値
    private var currentSessionSteps = 0
    private var previousSessionDistance = 0f
    private var currentSessionDistance = 0f

    // 徒歩向けフィルタ設定
    private val stationaryThresholdMeters = 1.0f
    private val minSpeedMetersPerSecond = 0.3f
    private val minTimeIntervalMs = 1000L

    // 権限管理
    private val locationPermissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            when {
                permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                    enableMyLocationOnMap()
                }
                else -> {
                    Toast.makeText(context, "位置情報の権限が必要です。", Toast.LENGTH_SHORT).show()
                }
            }
        }

    private val activityRecognitionPermissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                pedometerManager?.start()
                locationTracker?.start()
            } else {
                Toast.makeText(context, "歩数計機能を使用するには権限が必要です。", Toast.LENGTH_SHORT).show()
            }
        }

    // Activity 共有 ViewModel
    private val userStatusViewModel: UserStatusViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)

        // =========== 画面部品の取得 ===========
        textDistance = view.findViewById(R.id.textDistance)
        textDestination = view.findViewById(R.id.textDestination)
        stepCountText = view.findViewById(R.id.step_count_text)
        btnStart = view.findViewById(R.id.btnStartMeasure)
        btnStop = view.findViewById(R.id.btnStopMeasure)
        mapView = view.findViewById(R.id.mapView)

        // =========== ボタン設定 ===========

        mapManager = MapManager(requireContext())

        // 初期状態
        btnStop.visibility = View.GONE
        btnStart.visibility = View.VISIBLE

        btnStart.setOnClickListener {
            startTracking()
            btnStart.visibility = View.GONE
            btnStop.visibility = View.VISIBLE
        }

        btnStop.setOnClickListener {
            stopTracking()
            btnStart.visibility = View.VISIBLE
            btnStop.visibility = View.GONE
        }

        // =========== 初期化 ===========

        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        // GPS 初期化
        locationTracker = LocationTracker(requireContext()) { location, sessionDistance ->
            textDistance.text = String.format("移動距離: %.1f m", sessionDistance)

            val currentLatLng = LatLng(location.latitude, location.longitude)

            moveMarker(currentLatLng)

            // 計測中はカメラを追従、それ以外はズーム位置にスナップ
            if (isTracking) {
                googleMap?.animateCamera(CameraUpdateFactory.newLatLng(currentLatLng))
                currentSessionDistance = sessionDistance
            } else {
                googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 18f))
            }
        }

        // 歩数計初期化
        pedometerManager = PedometerManager(requireContext()) { stepsSinceStart ->
            // センサー値の差分計算
            val stepIncrement = if (lastTotalStepsSensorValue == 0) {
                lastTotalStepsSensorValue = stepsSinceStart
                0
            } else {
                val increment = stepsSinceStart - lastTotalStepsSensorValue
                lastTotalStepsSensorValue = stepsSinceStart
                increment
            }

            currentSessionSteps = stepsSinceStart
            stepCountText.text = "歩数: $stepsSinceStart 歩"
        }

        return view
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ViewModel データ監視（必要に応じて UI を更新）
        userStatusViewModel.userStatus.observe(viewLifecycleOwner) { status ->
            // 計測中以外で表示する想定
            // stepCountText.text = "総歩数: ${status.totalSteps} 歩"
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        mapManager?.setupMap(map)
        googleMap?.uiSettings?.isZoomControlsEnabled = true
        googleMap?.uiSettings?.isMyLocationButtonEnabled = true

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            enableMyLocationOnMap()
        } else {
            locationPermissionRequest.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
        val walkingCourse1 = listOf(    // 富樫の里コース
            LatLng(36.523797738679136, 136.61139627936268),
            LatLng(36.53097472427011, 136.61232468192307),
            LatLng(36.532423216124776, 136.6065401100907),
            LatLng(36.52943591075241, 136.60507124439795),
            LatLng(36.529564184076534, 136.6004491018365),
            LatLng(36.52589971334728, 136.6001657873157),
            LatLng(36.52411786098464, 136.60003887196592),
            LatLng(36.52315786291072, 136.59775131517029),
            LatLng(36.52438288411275, 136.59881068192306),
            LatLng(36.5243461069619, 136.59984472854975),
            LatLng(36.52488429018258, 136.60024644868307),
            LatLng(36.523797738679136, 136.61139627936268)
        )
        val walkingCourse2 = listOf(    // 白山やまなみコース
            LatLng(36.523797738679136, 136.61139627936268),
            LatLng(36.51155268262952, 136.61035982958367),
            LatLng(36.51142166918602, 136.60639355360044),
            LatLng(36.51134734376298, 136.6057800634767),
            LatLng(36.512478033802296, 136.6055136791334),
            LatLng(36.51247209998527, 136.60500893132448),
            LatLng(36.52093743026234, 136.60418303457317),
            LatLng(36.522393410906254, 136.6042085521887),
            LatLng(36.523377720325264, 136.6042085521887),
            LatLng(36.523797738679136, 136.61139627936268)
        )

        // プリセットコースを地図に描画
        mapManager?.drawPresetRoute(walkingCourse1)
        mapManager?.drawPresetRoute(walkingCourse2)

        map.setOnMapClickListener { latLng ->
            locationTracker?.getLastKnownLocation { location ->
                location?.let {
                    val currentPos = LatLng(it.latitude, it.longitude)

                    val apiKey = com.example.pdjissen.BuildConfig.MAPS_API_KEY
                    mapManager?.drawRoute(currentPos, latLng, apiKey) { distance ->

                        // 目的地までの距離を textDestination に表示
                        textDestination.text = "目的地まで: $distance"

                        Toast.makeText(context, "目的地まで: $distance", Toast.LENGTH_SHORT).show()
                    }

                    destinationMarker?.remove()
                    destinationMarker = map.addMarker(MarkerOptions().position(latLng).title("目的地"))
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocationOnMap() {
        googleMap?.isMyLocationEnabled = true
        getLastLocationAndMoveCamera()
    }

    @SuppressLint("MissingPermission")
    private fun getLastLocationAndMoveCamera() {
        locationTracker?.getLastKnownLocation { location ->
            location?.let {
                val currentLatLng = LatLng(it.latitude, it.longitude)
                moveMarker(currentLatLng)
                googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 18f))
            }
        }
    }

    private fun moveMarker(position: LatLng) {
        if (currentMarker == null) {
            currentMarker = googleMap?.addMarker(MarkerOptions().position(position).title("現在地"))
        } else {
            currentMarker?.position = position
        }
    }

    // 計測制御
    private fun startTracking() {
        if (!isTracking) {
            isTracking = true
            totalDistance = 0f
            previousSessionDistance = 0f
            currentSessionSteps = 0
            currentSessionDistance = 0f
            lastLocation = null

            lastTotalStepsSensorValue = 0

            checkPermissionsAndStart()
            Toast.makeText(context, "計測を開始しました", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopTracking() {
        if (isTracking) {
            isTracking = false
            locationTracker?.stop()
            pedometerManager?.stop()

            // 計測終了時に歩数と距離をまとめて保存
            if (currentSessionSteps > 0 || currentSessionDistance > 0f) {
                userStatusViewModel.updateSteps(currentSessionSteps)
                userStatusViewModel.updateDistance(currentSessionDistance)

                Toast.makeText(context, "${currentSessionSteps}歩、${String.format("%.1f", currentSessionDistance)}m を保存しました。", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "計測を終了しました（データなし）", Toast.LENGTH_SHORT).show()
            }

            // 次回のためリセット
            currentSessionSteps = 0
            currentSessionDistance = 0f
        }
    }

    private fun checkPermissionsAndStart() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            checkActivityRecognitionPermission()
        } else {
            locationPermissionRequest.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun checkActivityRecognitionPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            locationTracker?.start()
            pedometerManager?.start()
        } else {
            activityRecognitionPermissionRequest.launch(Manifest.permission.ACTIVITY_RECOGNITION)
        }
    }

    // ライフサイクル
    override fun onResume() {
        super.onResume()
        mapView.onResume()
        if (isTracking) checkPermissionsAndStart()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
        locationTracker?.stop()
        pedometerManager?.stop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }
}