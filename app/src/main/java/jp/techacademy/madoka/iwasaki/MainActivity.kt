package jp.techacademy.madoka.iwasaki

import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity(), View.OnClickListener {

    private val PERMISSIONS_REQUEST_CODE = 100      // ユーザの許可選択結果を識別するための定数
    private var permissionFlag = false              // 外部ストレージの読み込みが許可されているかを示すフラグ(true: 許可)

    private var imageList = arrayListOf<Uri>()      //画像のURIを格納するためのリスト
    private var imageListIndex = 0                  // imageList中、現在の表示している画像のIndex

    private var slideShowMode = false               // スライドショーモードかを示すフラグ
    private var slideShowTimer: Timer? = null       // スライドショー用のタイマー
    private val SLIDESHOW_Time:Long = 2000               // スライドショー用のタイマーの時間[ms]
    private var slideShowHandler = Handler()        // 他のスレッドに処理を依頼するためのハンドラ

    override fun onCreate(savedInstanceState: Bundle?) { // Activity生成時に1度だけ呼ばれる
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // イベントリスナーをセット
        next_button.setOnClickListener(this)
        prev_button.setOnClickListener(this)
        operate_button.setOnClickListener(this)

    }

    override fun onResume() { // フォアグラウンドになる時に呼ばれる
        super.onResume()

        // 外部ストレージの読み込み許可を確認
        checkPermission()

        // スライドショーモードを無効にする
        stopSlideShow()

        if(permissionFlag){ // 外部ストレージの読み込みが許可されている場合

            next_button.isEnabled = true        // next_buttonのクリックを有効にする
            prev_button.isEnabled = true        // perv_buttonのクリックを有効にする
            operate_button.isEnabled = true    // operate_buttonのクリックを有効にする

            // 最初の画像の表示
            displayFirstImage()

        } else { // 外部ストレージの読み込みが許可されていない場合
            message_text.text = "画像の読み込みが許可されていません"
            next_button.isEnabled = false        // next_buttonのクリックを無効にする
            prev_button.isEnabled = false        // perv_buttonのクリックを無効にする
            operate_button.isEnabled = false    // operate_buttonのクリックを無効にする
        }
    }

    // ボタンが押された場合の処理
    override fun onClick(v: View) {
        when(v.id){
            R.id.next_button -> {
                displayNextImage()
            }
            R.id.prev_button -> {
                displayPrevImage()
            }
            R.id.operate_button -> {
                if(slideShowMode){ // 現在スライドショー中の場合
                    stopSlideShow()
                } else { // 現在スライドショー中でない場合
                    startSlideShow()
                }
            }
        }
    }

    // 許可ダイアログで入力されたユーザーの選択結果を受け取る
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSIONS_REQUEST_CODE ->
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) { // 許可された場合
                    permissionFlag = true
                    displayFirstImage()
                } else { // 許可されなかった場合
                    permissionFlag = false
                }
        }
    }

    // パーミッションの許可確認
    private fun checkPermission(){

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // Android 6.0以降の場合
            // パーミッションの許可状態を確認する
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) { // 許可されている
                permissionFlag = true
            } else { // 許可されていない
                // 許可ダイアログを表示する
                requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), PERMISSIONS_REQUEST_CODE)
            }
        } else { // Android 5系以下の場合
            // アプリのインストール時に許可されている
            permissionFlag = true
        }
    }

    // スライドショーを開始する
    private fun startSlideShow(){
        slideShowMode = true
        operate_button.text = "停止"
        next_button.isEnabled = false        // next_buttonのクリックを無効にする
        prev_button.isEnabled = false        // perv_buttonのクリックを無効にする

        if(slideShowTimer == null){ // タイマーがまだ生成されていなかったら
            // タイマーの作成
            slideShowTimer = Timer()

            // タイマーの始動
            slideShowTimer!!.schedule(object : TimerTask() {
                override fun run() { // runメソッドがループ間隔(SLIDESHOW_Time)ごとに呼び出される
                    slideShowHandler.post { // 次の画像表示をメインスレッドに依頼
                        displayNextImage()
                    }
                }
            }, SLIDESHOW_Time, SLIDESHOW_Time) // 最初に始動させるまで SLIDESHOW_Time、ループの間隔を SLIDESHOW_Time に設定
        }
    }

    // スライドショーを停止する
    private fun stopSlideShow(){
        slideShowMode = false
        operate_button.text = "再生"
        next_button.isEnabled = true        // next_buttonのクリックを有効にする
        prev_button.isEnabled = true        // perv_buttonのクリックを有効にする

        if(slideShowTimer != null){ // タイマーが生成されていたら
            // タイマーを破棄
            slideShowTimer!!.cancel()
            slideShowTimer = null
        }
    }

    // 最初にImageViewに画像を表示する
    private fun displayFirstImage(){
        // 画像情報を取得
        getContentsInfo()

        if(imageList.isEmpty()){ // 画像がなかったら
            message_text.text = "画像が見つかりませんでした"
        } else {
            // 最初の画像を表示
            imageListIndex = 0
            displayImage()
        }

    }

    // 次の画像を表示
    private fun displayNextImage(){
        // インデックスを一つ進める
        imageListIndex++
        if(imageListIndex == imageList.size){ // 最後の要素を超えた場合
            imageListIndex = 0 // 最初に戻す
        }

        // 画像を表示
        displayImage()
    }

    // 前の画像を表示
    private fun displayPrevImage(){
        // インデックスを一つ戻す
        imageListIndex--
        if(imageListIndex < 0){ // 最初の要素より前になった場合
            imageListIndex = imageList.size - 1 // 最後に戻す
        }

        // 画像を表示
        displayImage()
    }

    // 画像とメッセージを表示
    private fun displayImage(){
        // 画像を表示
        imageView.setImageURI(imageList[imageListIndex])
        // メッセージを表示
        var currentNum = imageListIndex + 1
        message_text.text = "${imageList.size}件中${currentNum}件目"
    }

    // 画像のURIを取得し、リストに格納する
    private fun getContentsInfo() {
        // 画像の情報を取得する
        val resolver = contentResolver
        val cursor = resolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, // データの種類
            null, // 項目(null = 全項目)
            null, // フィルタ条件(null = フィルタなし)
            null, // フィルタ用パラメータ
            null // ソート (null ソートなし)
        )

        // リストを初期化
        imageList.clear()
        imageListIndex = 0

        if (cursor!!.moveToFirst()) { // 1番目の要素があれば
            do {
                // indexからIDを取得し、そのIDから画像のURIを取得する
                val fieldIndex = cursor.getColumnIndex(MediaStore.Images.Media._ID)
                val id = cursor.getLong(fieldIndex)
                val imageUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                imageList.add(imageUri) // リストに要素を追加
            } while (cursor.moveToNext()) //次の要素があればループ
        }
        cursor.close() // カーソルをクローズ
    }

}
