package ani.saikou.settings

import android.app.AlertDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updateLayoutParams
import androidx.core.widget.addTextChangedListener
import ani.saikou.*
import ani.saikou.anime.ExoplayerView
import ani.saikou.databinding.ActivityPlayerSettingsBinding
import ani.saikou.media.Media
import ani.saikou.others.getSerializable
import ani.saikou.parsers.Subtitle
import ani.saikou.parsers.SubtitleType
import com.google.android.material.snackbar.Snackbar
import kotlin.math.roundToInt


class PlayerSettingsActivity : AppCompatActivity() {
    lateinit var binding: ActivityPlayerSettingsBinding
    private val player = "player_settings"

    var media:Media?=null
    var subtitle:Subtitle?=null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initActivity(this)

        onBackPressedDispatcher.addCallback(this) {
            val settings = loadData<PlayerSettings>(player, toast = false) ?: PlayerSettings().apply { saveData(player, this) }
            if(media!=null) {
                var newSubtitle: Subtitle? = null
                if(subtitle != null){
                    // Change Kamyroll Subtitle Type
                    val oldUrl = subtitle?.url?.url
                    if (oldUrl?.contains("api.kamyroll.tech", true) == true) {
                        var newUrl: FileUrl? = subtitle?.url
                        var newType: SubtitleType? = null
                        when(settings.kamySubType){
                            0 -> {
                                newType = SubtitleType.ASS
                                newUrl = FileUrl(oldUrl.replace("&out=vtt", "&out=ass").replace("&out=srt", "&out=ass"))
                            }
                            1 -> {
                                newType = SubtitleType.VTT
                                newUrl = FileUrl(oldUrl.replace("&out=ass", "&out=vtt").replace("&out=srt", "&out=vtt"))
                            }
                            2 -> {
                                newType = SubtitleType.SRT
                                newUrl = FileUrl(oldUrl.replace("&out=ass", "&out=srt").replace("&out=vtt", "&out=srt"))
                            }
                        }
                        val newLanguage: String = subtitle!!.language
                        if(newUrl != null && newType != null) newSubtitle = Subtitle(newLanguage, newUrl, newType) }
                }
                val intent = Intent(this@PlayerSettingsActivity, ExoplayerView::class.java).apply {
                    putExtra("media", media)
                    if(newSubtitle != null) putExtra("subtitle", newSubtitle)
                }
                finish()
                startActivity(intent)
            }
            else{
                finish()
            }
        }

        try {
            media = intent.getSerializable("media",Media::class)
            subtitle = intent.getSerializable("subtitle",Subtitle::class)
        } catch (e: Exception) {
            toast(e.toString())
        }

        binding.playerSettingsContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            topMargin = statusBarHeight
            bottomMargin = navBarHeight
        }

        val settings = loadData<PlayerSettings>(player, toast = false) ?: PlayerSettings().apply { saveData(player, this) }

        binding.playerSettingsBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        //Video
        binding.playerSettingsVideoInfo.isChecked = settings.videoInfo
        binding.playerSettingsVideoInfo.setOnCheckedChangeListener { _, isChecked ->
            settings.videoInfo = isChecked
            saveData(player, settings)
        }

        binding.playerSettingsQualityHeight.setText((loadData<Int>("maxHeight", toast = false) ?: 480).toString())
        binding.playerSettingsQualityHeight.addTextChangedListener {
            val height = binding.playerSettingsQualityHeight.text.toString().toIntOrNull()
            saveData("maxHeight", height)
        }
        binding.playerSettingsQualityWidth.setText((loadData<Int>("maxWidth", toast = false) ?: 720).toString())
        binding.playerSettingsQualityWidth.addTextChangedListener {
            val height = binding.playerSettingsQualityWidth.text.toString().toIntOrNull()
            saveData("maxWidth", height)
        }


        val speeds = arrayOf(0.25f, 0.33f, 0.5f, 0.66f, 0.75f, 1f, 1.25f, 1.33f, 1.5f, 1.66f, 1.75f, 2f)
        val cursedSpeeds = arrayOf(1f, 1.25f, 1.5f, 1.75f, 2f, 2.5f, 3f, 4f, 5f, 10f, 25f, 50f)
        var curSpeedArr = if (settings.cursedSpeeds) cursedSpeeds else speeds
        var speedsName = curSpeedArr.map { "${it}x" }.toTypedArray()
        binding.playerSettingsSpeed.text = getString(R.string.default_playback_speed, speedsName[settings.defaultSpeed])
        val speedDialog = AlertDialog.Builder(this, R.style.DialogTheme).setTitle("Default Speed")
        binding.playerSettingsSpeed.setOnClickListener {
            speedDialog.setSingleChoiceItems(speedsName, settings.defaultSpeed) { dialog, i ->
                settings.defaultSpeed = i
                binding.playerSettingsSpeed.text = getString(R.string.default_playback_speed, speedsName[i])
                saveData(player, settings)
                dialog.dismiss()
            }.show()
        }

        binding.playerSettingsCursedSpeeds.isChecked = settings.cursedSpeeds
        binding.playerSettingsCursedSpeeds.setOnCheckedChangeListener { _, isChecked ->
            settings.cursedSpeeds = isChecked
            curSpeedArr = if (settings.cursedSpeeds) cursedSpeeds else speeds
            settings.defaultSpeed = if (settings.cursedSpeeds) 0 else 5
            speedsName = curSpeedArr.map { "${it}x" }.toTypedArray()
            binding.playerSettingsSpeed.text = getString(R.string.default_playback_speed, speedsName[settings.defaultSpeed])
            saveData(player, settings)
        }

        //Time Stamp
        binding.playerSettingsTimeStamps.isChecked = settings.timeStampsEnabled
        binding.playerSettingsTimeStamps.setOnCheckedChangeListener { _, isChecked ->
            settings.timeStampsEnabled = isChecked
            saveData(player, settings)
        }

        binding.playerSettingsShowTimeStamp.isChecked = settings.showTimeStampButton
        binding.playerSettingsShowTimeStamp.setOnCheckedChangeListener { _, isChecked ->
            settings.showTimeStampButton = isChecked
            saveData(player, settings)
        }


        //Auto
        binding.playerSettingsAutoSkipOpEd.isChecked = settings.autoSkipOPED
        binding.playerSettingsAutoSkipOpEd.setOnCheckedChangeListener { _, isChecked ->
            settings.autoSkipOPED = isChecked
            saveData(player, settings)
        }

        binding.playerSettingsAutoPlay.isChecked = settings.autoPlay
        binding.playerSettingsAutoPlay.setOnCheckedChangeListener { _, isChecked ->
            settings.autoPlay = isChecked
            saveData(player, settings)
        }
        binding.playerSettingsAutoSkip.isChecked = settings.autoSkipFiller
        binding.playerSettingsAutoSkip.setOnCheckedChangeListener { _, isChecked ->
            settings.autoSkipFiller = isChecked
            saveData(player, settings)
        }

        //Update Progress
        binding.playerSettingsAskUpdateProgress.isChecked = settings.askIndividual
        binding.playerSettingsAskUpdateProgress.setOnCheckedChangeListener { _, isChecked ->
            settings.askIndividual = isChecked
            saveData(player, settings)
        }
        binding.playerSettingsAskUpdateHentai.isChecked = settings.updateForH
        binding.playerSettingsAskUpdateHentai.setOnCheckedChangeListener { _, isChecked ->
            settings.updateForH = isChecked
            if (isChecked) toastString(getString(R.string.very_bold))
            saveData(player, settings)
        }
        binding.playerSettingsCompletePercentage.value = (settings.watchPercentage * 100).roundToInt().toFloat()
        binding.playerSettingsCompletePercentage.addOnChangeListener { _, value, _ ->
            settings.watchPercentage = value / 100
            saveData(player, settings)
        }

        //Behaviour
        binding.playerSettingsAlwaysContinue.isChecked = settings.alwaysContinue
        binding.playerSettingsAlwaysContinue.setOnCheckedChangeListener { _, isChecked ->
            settings.alwaysContinue = isChecked
            saveData(player, settings)
        }

        binding.playerSettingsPauseVideo.isChecked = settings.focusPause
        binding.playerSettingsPauseVideo.setOnCheckedChangeListener { _, isChecked ->
            settings.focusPause = isChecked
            saveData(player, settings)
        }

        binding.playerSettingsVerticalGestures.isChecked = settings.gestures
        binding.playerSettingsVerticalGestures.setOnCheckedChangeListener { _, isChecked ->
            settings.gestures = isChecked
            saveData(player, settings)
        }

        binding.playerSettingsDoubleTap.isChecked = settings.doubleTap
        binding.playerSettingsDoubleTap.setOnCheckedChangeListener { _, isChecked ->
            settings.doubleTap = isChecked
            saveData(player, settings)
        }

        binding.playerSettingsSeekTime.value = settings.seekTime.toFloat()
        binding.playerSettingsSeekTime.addOnChangeListener { _, value, _ ->
            settings.seekTime = value.toInt()
            saveData(player, settings)
        }

        binding.exoSkipTime.setText(settings.skipTime.toString())
        binding.exoSkipTime.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                binding.exoSkipTime.clearFocus()
            }
            false
        }
        binding.exoSkipTime.addTextChangedListener {
            val time = binding.exoSkipTime.text.toString().toIntOrNull()
            if (time != null) {
                settings.skipTime = time
                saveData(player, settings)
            }
        }

        //Other
        binding.playerSettingsPiP.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                visibility = View.VISIBLE
                isChecked = settings.pip
                setOnCheckedChangeListener { _, isChecked ->
                    settings.pip = isChecked
                    saveData(player, settings)
                }
            } else visibility = View.GONE
        }

        binding.playerSettingsCast.isChecked = settings.cast
        binding.playerSettingsCast.setOnCheckedChangeListener { _, isChecked ->
            settings.cast = isChecked
            saveData(player, settings)
        }

        val resizeModes = arrayOf("Original", "Zoom", "Stretch")
        val resizeDialog = AlertDialog.Builder(this, R.style.DialogTheme).setTitle("Default Resize Mode")
        binding.playerResizeMode.setOnClickListener {
            resizeDialog.setSingleChoiceItems(resizeModes, settings.resize) { dialog, count ->
                settings.resize = count
                saveData(player, settings)
                dialog.dismiss()
            }.show()
        }
        fun restartApp() {
            Snackbar.make(
                binding.root,
                R.string.restart_app, Snackbar.LENGTH_SHORT
            ).apply {
                val mainIntent =
                    Intent.makeRestartActivityTask(context.packageManager.getLaunchIntentForPackage(context.packageName)!!.component)
                setAction("Do it!") {
                    context.startActivity(mainIntent)
                    Runtime.getRuntime().exit(0)
                }
                show()
            }
        }

        fun toggleButton(button: android.widget.Button, toggle: Boolean) {
            button.isClickable = toggle
            button.alpha = when (toggle) {
                true  -> 1f
                false -> 0.5f
            }
        }

        fun toggleSubOptions(isChecked: Boolean) {
            toggleButton(binding.videoSubColorPrimary, isChecked)
            toggleButton(binding.videoSubColorSecondary, isChecked)
            toggleButton(binding.videoSubOutline, isChecked)
            toggleButton(binding.videoSubFont, isChecked)
            toggleButton(binding.kamyLang, isChecked)
            binding.subtitleFontSizeCard.isEnabled = isChecked
            binding.subtitleFontSizeCard.isClickable = isChecked
            binding.subtitleFontSizeCard.alpha = when (isChecked) {
                true  -> 1f
                false -> 0.5f
            }
            binding.subtitleFontSize.isEnabled = isChecked
            binding.subtitleFontSize.isClickable = isChecked
            binding.subtitleFontSize.alpha = when (isChecked) {
                true  -> 1f
                false -> 0.5f
            }
            ActivityPlayerSettingsBinding.bind(binding.root).subtitleFontSizeText.isEnabled = isChecked
            ActivityPlayerSettingsBinding.bind(binding.root).subtitleFontSizeText.isClickable = isChecked
            ActivityPlayerSettingsBinding.bind(binding.root).subtitleFontSizeText.alpha = when (isChecked) {
                true  -> 1f
                false -> 0.5f
            }
        }
        binding.subSwitch.isChecked = settings.subtitles
        binding.subSwitch.setOnCheckedChangeListener { _, isChecked ->
            settings.subtitles = isChecked
            saveData(player, settings)
            toggleSubOptions(isChecked)
            restartApp()
        }
        val subtitleTypes =
            arrayOf("ASS", "VTT", "SRT")
        val kamyrollSubTypeDialog = AlertDialog.Builder(this, R.style.DialogTheme).setTitle("Kamyroll Subtitle Type")
        binding.kamySubtitleType.setOnClickListener {
            kamyrollSubTypeDialog.setSingleChoiceItems(subtitleTypes, settings.kamySubType) { dialog, count ->
                settings.kamySubType = count
                saveData(player, settings)
                dialog.dismiss()
            }.show()
        }
        val colorsPrimary =
            arrayOf("Black", "Dark Gray", "Gray", "Light Gray", "White", "Red", "Yellow", "Green", "Cyan", "Blue", "Magenta")
        val primaryColorDialog = AlertDialog.Builder(this, R.style.DialogTheme).setTitle("Primary Sub Color")
        binding.videoSubColorPrimary.setOnClickListener {
            primaryColorDialog.setSingleChoiceItems(colorsPrimary, settings.primaryColor) { dialog, count ->
                settings.primaryColor = count
                saveData(player, settings)
                dialog.dismiss()
            }.show()
        }
        val colorsSecondary = arrayOf(
            "Black",
            "Dark Gray",
            "Gray",
            "Light Gray",
            "White",
            "Red",
            "Yellow",
            "Green",
            "Cyan",
            "Blue",
            "Magenta",
            "Transparent"
        )
        val secondaryColorDialog = AlertDialog.Builder(this, R.style.DialogTheme).setTitle("Outline Sub Color")
        binding.videoSubColorSecondary.setOnClickListener {
            secondaryColorDialog.setSingleChoiceItems(colorsSecondary, settings.secondaryColor) { dialog, count ->
                settings.secondaryColor = count
                saveData(player, settings)
                dialog.dismiss()
            }.show()
        }
        val typesOutline = arrayOf("Outline", "Shine", "Drop Shadow", "None")
        val outlineDialog = AlertDialog.Builder(this, R.style.DialogTheme).setTitle("Outline Type")
        binding.videoSubOutline.setOnClickListener {
            outlineDialog.setSingleChoiceItems(typesOutline, settings.outline) { dialog, count ->
                settings.outline = count
                saveData(player, settings)
                dialog.dismiss()
            }.show()
        }
        val colorsSubBackground = arrayOf(
            "Transparent",
            "Black",
            "Dark Gray",
            "Gray",
            "Light Gray",
            "White",
            "Red",
            "Yellow",
            "Green",
            "Cyan",
            "Blue",
            "Magenta"
        )
        val subBackgroundDialog = AlertDialog.Builder(this, R.style.DialogTheme).setTitle("Outline Sub Color")
        binding.videoSubColorBackground.setOnClickListener {
            subBackgroundDialog.setSingleChoiceItems(colorsSubBackground, settings.subBackground) { dialog, count ->
                settings.subBackground = count
                saveData(player, settings)
                dialog.dismiss()
            }.show()
        }

        val colorsSubWindow = arrayOf(
            "Transparent",
            "Black",
            "Dark Gray",
            "Gray",
            "Light Gray",
            "White",
            "Red",
            "Yellow",
            "Green",
            "Cyan",
            "Blue",
            "Magenta"
        )
        val subWindowDialog = AlertDialog.Builder(this, R.style.DialogTheme).setTitle("Outline Sub Color")
        binding.videoSubColorWindow.setOnClickListener {
            subWindowDialog.setSingleChoiceItems(colorsSubWindow, settings.subWindow) { dialog, count ->
                settings.subWindow = count
                saveData(player, settings)
                dialog.dismiss()
            }.show()
        }
        val fonts = arrayOf("Poppins Semi Bold", "Poppins Bold", "Poppins", "Poppins Thin")
        val fontDialog = AlertDialog.Builder(this, R.style.DialogTheme).setTitle("Subtitle Font")
        binding.videoSubFont.setOnClickListener {
            fontDialog.setSingleChoiceItems(fonts, settings.font) { dialog, count ->
                settings.font = count
                saveData(player, settings)
                dialog.dismiss()
            }.show()
        }
        val locales = arrayOf(
            "Default (en-US)",
            "[ja-JP] Japanese",
            "[en-US] English",
            "[de-DE] German",
            "[es-419] Spanish",
            "[es-ES] Spanish (Spain)",
            "[fr-FR] French",
            "[it-IT] Italian",
            "[ar-SA] Arabic (Saudi Arabia)",
            "[ar-ME] Arabic (Montenegro)",
            "[pt-BR] Portuguese (Brazil)",
            "[pt-PT] Portuguese (Portugal)",
            "[ru-RU] Russian",
            "[zh-CN] Chinese",
            "[tr-TR] Turkish"
        )
        val localeDialog = AlertDialog.Builder(this, R.style.DialogTheme).setTitle("Kamyroll Language")
        binding.kamyLang.setOnClickListener {
            localeDialog.setSingleChoiceItems(locales, settings.locale) { dialog, count ->
                settings.locale = count
                saveData(player, settings)
                dialog.dismiss()
                restartApp()
            }.show()
        }
        binding.subtitleFontSize.setText(settings.fontSize.toString())
        binding.subtitleFontSize.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                binding.subtitleFontSize.clearFocus()
            }
            false
        }
        binding.subtitleFontSize.addTextChangedListener {
            val size = binding.subtitleFontSize.text.toString().toIntOrNull()
            if (size != null) {
                settings.fontSize = size
                saveData(player, settings)
            }
        }
        toggleSubOptions(settings.subtitles)
    }
}
