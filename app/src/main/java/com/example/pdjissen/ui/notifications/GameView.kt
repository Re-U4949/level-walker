package com.example.pdjissen.ui.notifications

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class GameView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    // 画像リソースをBitmapに変換
    // これらの画像ファイルが res/drawable フォルダに存在することを確認してください
    private val charBitmap: Bitmap = BitmapFactory.decodeResource(resources, android.R.drawable.ic_menu_camera)
    private val itemBitmap: Bitmap = BitmapFactory.decodeResource(resources, android.R.drawable.ic_menu_help)
    private val obstacleBitmap: Bitmap = BitmapFactory.decodeResource(resources, android.R.drawable.ic_delete)

    // ゲームオブジェクトの描画位置とサイズを保持するRectF
    var characterRect = RectF()
    var itemRects = mutableListOf<RectF>()
    var obstacleRects = mutableListOf<RectF>()

    // onDrawメソッドで、Canvasに画像を描画する
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // キャラクターを描画
        if (!characterRect.isEmpty) {
            canvas.drawBitmap(charBitmap, null, characterRect, null)
        }

        // アイテムを描画
        itemRects.forEach { rect ->
            canvas.drawBitmap(itemBitmap, null, rect, null)
        }

        // 障害物を描画
        obstacleRects.forEach { rect ->
            canvas.drawBitmap(obstacleBitmap, null, rect, null)
        }
    }

    // ゲーム状態が更新されたときに再描画を要求する
    fun updateView() {
        invalidate() // このメソッドを呼ぶと onDraw が再実行される
    }
}
