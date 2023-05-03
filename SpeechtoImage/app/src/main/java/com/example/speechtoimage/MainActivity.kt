package com.example.speechtoimage
import android.Manifest
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.backup.BackupManager
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Color
import android.graphics.ColorFilter
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.OnInitListener
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.airbnb.lottie.*
import com.airbnb.lottie.model.KeyPath
import com.airbnb.lottie.value.LottieValueCallback
import java.io.*
import java.util.*


enum class Mode {
    DEMO, PARENT, CHILD
}

enum class Mand {
    MAND1, MAND2, MAND3, SETMENU, SETLOCK
}



@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity(),GestureDetector.OnGestureListener {
    lateinit var outputTV: TextView
//    lateinit var mandtext: TextView
    lateinit var micIV: ImageView
    lateinit var dbHelper: DatabaseHelper
    private var speechRecognizer: SpeechRecognizer? = null
    private lateinit var animationView: LottieAnimationView
//    private val handler = Handler()
    private lateinit var mediaPlayer: MediaPlayer
    private var currentMode: Mode = Mode.DEMO
    private var currentMand: Mand = Mand.MAND1
    private lateinit var gestureDetector: GestureDetector
    private var tapCount: Int = 0
    private lateinit var textToSpeech: TextToSpeech

    object ColorConstants {
        val transparent = "#00000000"
        val black = "#ff000000"
        val dark_gray = "#ff444444"
        val gray = "#ff888888"
        val light_gray = "#ffcccccc"
        val white = "#ffffffff"
        val red = "#ffff0000"
        val green = "#ff00ff00"
        val blue = "#ff0000ff"
        val yellow = "#ffffff00"
        val cyan = "#ff00ffff"
        val magenta = "#ffff00ff"
    }




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val backupManager = BackupManager(this)
        backupManager.dataChanged()
        gestureDetector = GestureDetector(this, this)
        mediaPlayer = MediaPlayer.create(applicationContext, R.raw.drum)

        dbHelper = DatabaseHelper(this)
        dbHelper.SaveMenu("menu","show me menu")
        dbHelper.SaveMenu("lock","close the app")
        addData()
        initializeTextToSpeech()


        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) // keep screen awake


        //child()
        parent()


    }


    private fun addData(){
        val myList_adj = listOf(
            "/new_sound_json/ADJECTIVES/GREEN/green.json", "/new_sound_json/ADJECTIVES/BLUE/blue.json", "/new_sound_json/ADJECTIVES/RED/red.json", "/new_sound_json/ADJECTIVES/ANGRY/angry.json", "/new_sound_json/ADJECTIVES/HOT/hot.json", "/new_sound_json/NOUNS/CAT/cat.json", "/new_sound_json/NOUNS/CAR/car.json", "/new_sound_json/NOUNS/DOG/dog.json", "/new_sound_json/NOUNS/BEE/bee.json", "/new_sound_json/NOUNS/DRUM/drum.json", "/new_sound_json/NOUNS/BELL/bell.json", "/new_sound_json/NOUNS/HORN/horn.json", "/new_sound_json/NOUNS/COW/cow.json", "/new_sound_json/NOUNS/CAKE/cake.json", "/new_sound_json/NOUNS/LION/lion.json", "/new_sound_json/VERBS/RUNNING/running.json", "/new_sound_json/VERBS/DANCING/dancing.json", "/new_sound_json/VERBS/SLEEPING/sleeping.json", "/new_sound_json/VERBS/SINGING/singing.json", "/new_sound_json/VERBS/JUMPING/jumping.json", "/new_sound_json/VERBS/CLAPPING/clapping.json", "/new_sound_json/VERBS/CLIMBING/climbing.json", "/new_sound_json/VERBS/SWIMMING/swimming.json", "/new_sound_json/VERBS/EATING/eating.json", "/new_sound_json/VERBS/CUTTING/cutting.json"
        )

        val types = listOf(
            "ADJECTIVES", "ADJECTIVES", "ADJECTIVES", "ADJECTIVES", "ADJECTIVES", "NOUNS", "NOUNS", "NOUNS", "NOUNS", "NOUNS", "NOUNS", "NOUNS", "NOUNS", "NOUNS", "NOUNS", "VERBS", "VERBS", "VERBS", "VERBS", "VERBS", "VERBS", "VERBS", "VERBS", "VERBS", "VERBS"
        )
        for( (item, type) in myList_adj.zip(types)) {
            val image = item.split("/").last()
            val image_name = image.split(".").first().split('/').last().replace("_"," ")
            try{
                Log.e("MM:", "data: $item")
                val inputStream = this.javaClass.getResourceAsStream(item)

                val bufferedReader = BufferedReader(InputStreamReader(inputStream))
                val jsonString = bufferedReader.use { it.readText() }


                dbHelper.SaveData(image_name,type,jsonString)
            }
            catch (e: Exception) {
                Log.e("SSSSSSSSSSSS:", "Error writing file: ${e.message}")
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun child(){
        setContentView(R.layout.activity_main_child)

        outputTV = findViewById(R.id.idTVOutput)
        if (currentMand==Mand.SETMENU)
            outputTV.text = "Speak to setup new menu command"
        if (currentMand==Mand.SETLOCK)
            outputTV.text = "Speak to setup new lock command"
        micIV = findViewById(R.id.idIVMic)

//        if (currentMand == Mand.MAND1)
//            mandtext.text = "current mand: "+ "1 MAND"
//        if (currentMand == Mand.MAND2)
//            mandtext.text = "current mand: "+ "2 MAND"
//        if (currentMand == Mand.MAND3)
//            mandtext.text = "current mand: "+ "3 MAND"
        checkAudioPermission()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        }
        micIV.setOnClickListener {
            micIV.setColorFilter(ContextCompat.getColor(this, R.color.mic_enabled_color)) // #FF0E87E7
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            startSpeechToText()
        }

        micIV.setColorFilter(ContextCompat.getColor(this, R.color.mic_enabled_color)) // #FF0E87E7
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

        startSpeechToText()
        //testing()

        //println(findNouns("Dog cat elephant"))
    }
    private fun getColorCodeByName(colorName: String): String {
        return when (colorName.toLowerCase()) {
            "transparent" -> ColorConstants.transparent
            "black" -> ColorConstants.black
            "dark gray" -> ColorConstants.dark_gray
            "gray" -> ColorConstants.gray
            "light gray" -> ColorConstants.light_gray
            "white" -> ColorConstants.white
            "red" -> ColorConstants.red
            "green" -> ColorConstants.green
            "blue" -> ColorConstants.blue
            "yellow" -> ColorConstants.yellow
            "cyan" -> ColorConstants.cyan
            "magenta" -> ColorConstants.magenta
            else -> "null"
        }
    }

    private fun color_anim(color_:String){

        val yourColor = Color.parseColor(color_)
        val filter = SimpleColorFilter(yourColor)
        val keyPath = KeyPath("**")
        val callback: LottieValueCallback<ColorFilter> = LottieValueCallback(filter)

        animationView.addValueCallback(
            keyPath,
            LottieProperty.COLOR_FILTER,
            callback,
        )
        animationView.playAnimation()



    }
    private fun bigsize()
    {
        val startScale = 1.5f
        val endScale = 1.5f

        val scaleAnimator = ValueAnimator.ofFloat(startScale, endScale).apply {
            duration = 10000
            repeatCount = 2
            repeatMode = ValueAnimator.REVERSE
            addUpdateListener {

                val value = it.animatedValue as Float
                animationView.scaleX = value
                animationView.scaleY = value
            }
        }

        scaleAnimator.start()

    }
    private fun smallsize()
    {
        val startScale = 0.5f
        val endScale = 0.5f

        val scaleAnimator = ValueAnimator.ofFloat(startScale, endScale).apply {
            duration = 10000
            repeatCount = 2
            repeatMode = ValueAnimator.REVERSE
            addUpdateListener {

                val value = it.animatedValue as Float
                animationView.scaleX = value
                animationView.scaleY = value
            }
        }

        scaleAnimator.start()

    }

    private fun rotate(){
        val rotateDuration = resources.getInteger(R.integer.rotate_duration)

        val rotateAnimator = ObjectAnimator.ofFloat(animationView, "rotation", 0f, 360f).apply {
            duration = rotateDuration.toLong()
            repeatCount = 5
            repeatMode = ValueAnimator.RESTART
        }

        rotateAnimator.start()
    }
    private fun jump(){
        val jumpHeight = 220f
        val jumpDuration = 1000


        val valueAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = jumpDuration.toLong()
            repeatCount = 5
            repeatMode = ValueAnimator.REVERSE
            addUpdateListener { animator ->
                val fraction = animator.animatedFraction
                val translationY = -jumpHeight * fraction * (fraction - 2)
                animationView.translationY = translationY
            }
        }

        valueAnimator.start()

    }

    @SuppressLint("RestrictedApi")
    private fun setAnimation(image: String, input:String, input2:String){
        try {

            animationView = findViewById(R.id.animation_view)

            LottieComposition.Factory.fromJsonString(image
            ) { composition ->

                if (composition != null) {
                    animationView.setComposition(composition)

                    animationView.playAnimation()

                }
            }

            if (input == "rotate" || input2 == "rotate")
                rotate()
            else if (input == "jumping" || input2 == "jumping")
                jump()
            else if (input == "small" || input2 == "small")
                smallsize()
            else if (input == "big" || input2 == "big")
                bigsize()

            if (input == "none" && input2 != "none") {

                val color = getColorCodeByName(input2)
                if (color != "null")
                    color_anim(color)
            }else if(input != "none" && input2 == "none")
            {

                val color = getColorCodeByName(input)
                if (color != "null")
                    color_anim(color)
            }
            else{
                val color = getColorCodeByName(input2)

                if (color != "null")
                    color_anim(color)
                val color1 = getColorCodeByName(input)
                if (color1 != "null")
                    color_anim(color1)
            }



        } catch (e: Exception) {
            Log.e("set:", "Error in set animation: ${e.message}")
        }
    }
    @SuppressLint("UseSwitchCompatOrMaterialCode", "SetTextI18n")
    private fun parent(){
        setContentView(R.layout.activity_main_parent)
        val toggleSwitch: Switch = findViewById(R.id.toggle_switch)
        if (currentMode==Mode.DEMO)
            toggleSwitch.isChecked = true

        currentMode = Mode.PARENT

        val mand_1 = findViewById<Button>(R.id.btn1Mand)
        val mand_2 = findViewById<Button>(R.id.btn2Mand)
        val mand_3 = findViewById<Button>(R.id.btn3Mand)
        val setmenu = findViewById<Button>(R.id.btnsetmenu)
        val setlock = findViewById<Button>(R.id.btnsetlock)


        setlock.setOnClickListener{
            currentMand = Mand.SETLOCK
            child()
        }
        setmenu.setOnClickListener{
            currentMand = Mand.SETMENU
            child()
        }
        mand_1.setOnClickListener{
            currentMand = Mand.MAND1
            if (toggleSwitch.isChecked)
                currentMode = Mode.DEMO

            child()

        }
        mand_2.setOnClickListener{
            currentMand = Mand.MAND2
            if (toggleSwitch.isChecked)
                currentMode = Mode.DEMO

            child()
        }

        mand_3.setOnClickListener{
            currentMand = Mand.MAND3
            if (toggleSwitch.isChecked)
                currentMode = Mode.DEMO

            child()
        }

        toggleSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                currentMode = Mode.DEMO
            } else {
                currentMode = Mode.CHILD
            }
            child()
        }

    }



    private fun speak(text: String) {
        if (text.isNotEmpty()) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        } else {
            Toast.makeText(this, "Please enter some text", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initializeTextToSpeech() {
        textToSpeech = TextToSpeech(this, object : OnInitListener {
            override fun onInit(status: Int) {
                if (status == TextToSpeech.SUCCESS) {
                    val result = textToSpeech.setLanguage(Locale.US)
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Toast.makeText(
                            this@MainActivity,
                            "Language not supported",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "Failed to initialize TextToSpeech",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        })
    }


    var count:Int = 1

    @SuppressLint("SuspiciousIndentation", "Recycle")
    private fun testing() {
        setContentView(R.layout.activity_main_demo)
        outputTV = findViewById(R.id.idTVOutput)
        val db = dbHelper.readableDatabase
        val sql = "SELECT image,name,type FROM images ORDER BY RANDOM() LIMIT 1"
        val cursor = db.rawQuery(
            /* sql = */sql , /* selectionArgs = */
            null
            //arrayOf("cat")
        )
        if (cursor.moveToNext()) {
            outputTV.text = cursor.getString(1)
            val image = cursor.getString(0)
            speak(cursor.getString(1))

            val options = listOf("rotate", "jumping", "big", "small","red","green","blue","yellow")

            if (currentMand==Mand.MAND1) {
                Log.e("test","mand1")
                setAnimation(image, "none","none")

            }
            else if (currentMand==Mand.MAND2) {
                val randomOption = options.random()
                setAnimation(image,randomOption,"none")

            }
            else{
                val options1 = listOf("rotate", "jumping")
                val options2 = listOf("big", "small","red","green","blue","yellow")
                setAnimation(image,options1.random(),options2.random())
                //setAnimation(image,)
            }


        }
        else {

            outputTV.text ="error"
        }

    }



    private fun startSpeechToText() {
        outputTV = findViewById(R.id.idTVOutput)

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        speechRecognizer?.startListening(intent)

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(bundle: Bundle?) {
               // Toast.makeText(applicationContext, "Listening...", Toast.LENGTH_SHORT).show()
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(v: Float)  {

            }
            override fun onBufferReceived(bytes: ByteArray?) {}
            override fun onEndOfSpeech() {
                //Toast.makeText(applicationContext, "Stopped Listening", Toast.LENGTH_SHORT).show()
                micIV.setColorFilter(
                    ContextCompat.getColor(
                        applicationContext,
                        R.color.mic_disabled_color
                    )
                )


            }

            override fun onError(i: Int) {
                //if (i == SpeechRecognizer.ERROR_NO_SPEECH)
                val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager
                val appProcesses = am.runningAppProcesses

                for (appProcess in appProcesses) {
                    if (appProcess.processName == packageName) {
                        // If the app is in the foreground, the importance will be IMPORTANCE_FOREGROUND or IMPORTANCE_VISIBLE
                        val importance = appProcess.importance
                        if (importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                            || importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE) {


                            micIV.setColorFilter(
                                ContextCompat.getColor(
                                    applicationContext,
                                    R.color.mic_enabled_color
                                )
                            )
                            // The user has stopped speaking, so start listening again
                            if (currentMode != Mode.PARENT ) {
                                speechRecognizer?.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH))
                            }
                        } else {

                            speechRecognizer?.stopListening()
                        }
                    }
                }

            }

            @SuppressLint("Recycle", "SetTextI18n")
            override fun onResults(bundle: Bundle) {
                val result = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (result != null) {
                    val db = dbHelper.readableDatabase

                    val split_result = result[0].split(" ")
                    var color = "null"
                    var color_name = "none"
                    var rotate = false
                    var jumping = false
                    var small = false
                    var big = false
                    var last_val = "null"
                    for (i in split_result)
                    {
                        color = getColorCodeByName(i)
                        if (color!="null")
                            color_name = i
                        if (i =="spinning" || i=="spin"){
                            rotate = true
                        }
                        if (i =="jumping" || i=="bounce" || i =="jump" || i=="bouncing"){
                            jumping = true
                        }
                        if (i =="small" || i=="shrink" || i =="compress" || i=="decrease"){
                            small = true
                        }
                        if (i =="big" || i=="large" || i =="enlarge" || i=="expand" || i=="increase"){
                            big = true
                        }
                        val sql = "SELECT image,name,type FROM images WHERE name=? "
                        val cursor11 = db.rawQuery(
                            /* sql = */ sql,
                            /* selectionArgs = */ arrayOf(i)
                        )
                        if (cursor11.moveToNext())
                        {
                        last_val = i
                        }

                    }

                    val cursor: Cursor
                    val sql = "SELECT image,name,type FROM images WHERE name LIKE '%' || ? || '%' ORDER BY RANDOM() LIMIT 1"

                    cursor = db.rawQuery(
                            /* sql = */ sql,
                            /* selectionArgs = */ arrayOf(last_val)
                        )
                    if (cursor.moveToNext() && (currentMand!=Mand.SETMENU && currentMand!=Mand.SETLOCK) )
                    {
                        outputTV.text = result[0]

                        val image = cursor.getString(0)
                        var flag = false
                        if (currentMand==Mand.MAND1) {
                            setAnimation(image, "none","none")

                        }
                        else if (currentMand==Mand.MAND2)
                        {

                            if (rotate) {
                                setAnimation(image, "rotate","none")

                                flag = true
                            }
                            else if(jumping) {
                                setAnimation(image, "jumping","none")
                                flag = true
                            }
                            else if(big)
                            {
                                setAnimation(image, "big","none")
                                flag = true
                            }
                            else if(small)
                            {
                                setAnimation(image, "small","none")
                                flag = true
                            }
                            if (!flag)
                                setAnimation(image, color_name,"none")
                            else
                            {
                                val options = listOf("rotate", "jumping", "big", "small","red","green","blue","yellow")
                                setAnimation(image, options.random(), "none")

                            }



                        }

                        else if (currentMand==Mand.MAND3){
                            //speak(result[0])
                            var text1 = "none"
                            var text2 = "none"
                            if (rotate) {
                                text1 = "rotate"

                            }
                            else if(jumping) {
                                text1 = "jumping"

                            }
                            else if(big)
                            {
                                text2 = "big"

                            }
                            else if(small)
                            {
                                text2 = "small"

                            }

                            if (text1=="none" && text2=="none")
                            {
                                val options1 = listOf("rotate", "jumping","big", "small")
                                if (color_name=="none") {
                                    val options2 = listOf("red", "green", "blue", "yellow")
                                    setAnimation(image, options1.random(), options2.random())
                                }
                                else{
                                    setAnimation(image, color_name, options1.random())
                                }
                            }

                            else if (text1=="none" && text2!="none")
                            {
                                val options1 = listOf("rotate", "jumping","big", "small")
                                if (color_name=="none") {
                                    setAnimation(image, options1.random(), text2)
                                }
                                else{

                                    setAnimation(image, color_name, text2)
                                }
                            }
                            else if (text1!="none" && text2 == "none")
                            {
                                val options1 = listOf("rotate", "jumping","big", "small")
                                if (color_name=="none") {
                                    setAnimation(image, options1.random(), text1)
                                }
                                else{

                                    setAnimation(image, color_name, text1)
                                }
                            }
                            else {
                                setAnimation(image, text2, text1)
                                setAnimation(image, color_name, "none")
                            }



                        }



                        speechRecognizer?.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH))

                    } else {
                        try {
                            outputTV.text = result[0]
                            val sql1 = "SELECT menu_text,name FROM menu where name=? ORDER BY RANDOM() LIMIT 1"

                             val cursor1 = db.rawQuery(
                                /* sql = */sql1,
                                /* selectionArgs = */ arrayOf("menu")
                            )

                            val cursor2 = db.rawQuery(
                                /* sql = */ sql1,
                                /* selectionArgs = */arrayOf("lock")
                            )
                        if (cursor1.moveToNext() && cursor2.moveToNext() ) {
                            if (currentMand == Mand.SETMENU) {
                                dbHelper.UpdateMenu("menu", result[0])
                                val text = "menu voice: " + result[0] + " is fixed"
                                Toast.makeText(applicationContext, text, Toast.LENGTH_SHORT).show()
                                parent()
                            }
                            else if (currentMand == Mand.SETLOCK) {
                                dbHelper.UpdateMenu("lock", result[0])
                                val text = "lock voice: " + result[0] + " is fixed"
                                Toast.makeText(applicationContext, text, Toast.LENGTH_SHORT).show()
                                parent()
                            }

                            else if (result[0] == cursor1.getString(0) && cursor1.getString(1)=="menu") {
                                tapCount = 0
                                speechRecognizer?.stopListening()
                                parent()
                            } else if (result[0] == "show me") {
                                tapCount = 0
                                speechRecognizer?.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH))
                                if (currentMode == Mode.DEMO) {
                                    testing()
                                }


                            } else if (result[0] == "close") {
                                tapCount = 0
                                child()
                            }
                            else if (result[0] == cursor2.getString(0) && cursor2.getString(1)=="lock") {
                                System.exit(0)
                            }

                        }


                            speechRecognizer?.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH))
                            //speechRecognizer?.startListening(intent)
                        }
                        catch (e: Exception) {
                            Log.e("set:", "Error in set animation: ${e.message}")
                            Toast.makeText(applicationContext, e.message, Toast.LENGTH_SHORT).show()
                        }

                    }


                }

                    speechRecognizer?.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH))
                    //speechRecognizer?.startListening(intent)



            }

            override fun onPartialResults(bundle: Bundle) {}
            override fun onEvent(i: Int, bundle: Bundle?) {}

        })



    }

    private fun checkAudioPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                "android.permission.RECORD_AUDIO"
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            val intent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:com.programmingtech.offlinespeechtotext")
            )
            startActivity(intent)
            Toast.makeText(this, "Please Allow Your Microphone Permission", Toast.LENGTH_SHORT)
                .show()
        }


    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event != null) {
            gestureDetector.onTouchEvent(event)
        }
        return super.onTouchEvent(event)
    }

    override fun onDown(p0: MotionEvent): Boolean {

        return true
    }

    override fun onShowPress(p0: MotionEvent) {

//        return true
    }

    override fun onSingleTapUp(p0: MotionEvent): Boolean {
        tapCount++
        if (tapCount == 3) {
            System.exit(0)
        }

        return true
    }

    override fun onScroll(p0: MotionEvent, p1: MotionEvent, p2: Float, p3: Float): Boolean {

        return true
    }

    override fun onLongPress(p0: MotionEvent) {

        //return true
    }

    override fun onFling(p0: MotionEvent, p1: MotionEvent, p2: Float, p3: Float): Boolean {

        return true
    }


}