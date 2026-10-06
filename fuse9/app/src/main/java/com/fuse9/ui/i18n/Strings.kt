package com.fuse9.ui.i18n

import androidx.compose.runtime.staticCompositionLocalOf
import com.fuse9.game.GameMode
import com.fuse9.game.Look
import com.fuse9.game.Reason
import com.fuse9.game.TutorialGuide
import com.fuse9.game.Verdict
import com.fuse9.puzzle.Difficulty
import com.fuse9.puzzle.Grid
import com.fuse9.settings.AppLanguage
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

/**
 * Every word the player reads, per language. Kept in Kotlin rather than XML because many lines
 * are built from structured reasons (units, digits, cells) and Turkish needs case endings on
 * those pieces; a function per phrase handles that cleanly.
 */
abstract class Strings(val locale: Locale) {
    /** Locale-aware upper case: Turkish "i" must become "İ". */
    fun upper(s: String) = s.uppercase(locale)
    fun dayName(day: DayOfWeek): String = day.getDisplayName(TextStyle.FULL, locale).replaceFirstChar { it.titlecase(locale) }

    // Menu
    abstract val tagline: String
    abstract val continueGame: String
    abstract fun continueDetail(d: Difficulty, time: String, seals: Int): String
    abstract val newPuzzle: String
    abstract fun newPuzzleOf(d: Difficulty): String
    abstract val daily: String
    abstract fun doneToday(streak: Int): String
    abstract val modes: String
    abstract val stats: String
    abstract val settings: String
    abstract val howToPlay: String
    abstract val skip: String
    abstract fun difficulty(d: Difficulty): String
    abstract fun mode(m: GameMode): String
    abstract fun modeDescription(m: GameMode): String

    // Game
    abstract val back: String
    abstract val backToMenu: String
    abstract val pause: String
    abstract val resume: String
    abstract val paused: String
    abstract val tapToContinue: String
    abstract val preparing: String
    abstract val firstBoard: String
    abstract fun sealsFound(n: Int): String
    abstract fun strikes(used: Int, limit: Int): String
    abstract val undo: String
    abstract val erase: String
    abstract val notes: String
    abstract val seal: String
    abstract val suspect: String
    abstract val hint: String
    abstract val on: String
    abstract fun digitKey(d: Int): String
    abstract fun digitsLeft(n: Int): String
    abstract val digitComplete: String

    // Board, for screen readers
    abstract fun boardSummary(sealsFound: Int): String
    abstract fun cellAt(row: Int, col: Int): String
    abstract val hidden: String
    abstract val provenSafe: String
    abstract val markedSuspect: String
    abstract fun defusedSeal(d: Int): String
    abstract fun trippedSeal(d: Int): String
    abstract fun openCell(d: Int, seals: Int): String

    // Hints and guide
    abstract fun look(l: Look): String
    abstract fun reason(r: Reason): String
    fun reasons(rs: List<Reason>) = rs.joinToString(" ") { reason(it) }
    abstract fun guide(line: TutorialGuide.Line): String

    // Result
    abstract fun verdict(v: Verdict): String
    abstract fun verdictLine(v: Verdict): String
    abstract fun sealsOfNine(n: Int): String
    abstract fun strikeCount(n: Int): String
    abstract fun hintCount(n: Int): String
    abstract fun rippleCount(n: Int): String
    abstract fun dailyStreak(n: Int): String
    abstract val nextBoard: String
    abstract val anotherBoard: String
    abstract val menu: String
    abstract fun marks(n: Int): String

    // Stats
    abstract val solvedLabel: String
    abstract val cleanLabel: String
    abstract val dailyStreakLabel: String
    abstract val sealsDefused: String
    abstract val totalStrikes: String
    abstract val bestDailyStreak: String
    abstract val hardestSolved: String
    abstract val colSolved: String
    abstract val colBest: String
    abstract val colAverage: String

    // Settings
    abstract val soundAndTouch: String
    abstract val sound: String
    abstract val soundVolume: String
    abstract fun percent(n: Int): String
    abstract val haptics: String
    abstract val lookSection: String
    abstract val theme: String
    abstract val themeOptions: List<String>
    abstract val highContrast: String
    abstract val sealCounts: String
    abstract val sealCountsNote: String
    abstract val countOptions: List<String>
    abstract val playSection: String
    abstract val autoClean: String
    abstract val autoCleanNote: String
    abstract val highlightMatching: String
    abstract val showTimer: String
    abstract val language: String
    /** Order matches [AppLanguage]; language names are written in their own language. */
    val languageOptions: List<String> get() = listOf(system, "Türkçe", "English")
    abstract val system: String
    abstract val fontsCredit: String

    // Rules
    abstract val rules: List<Pair<String, String>>
    abstract val rulesFooter: String
}

object English : Strings(Locale.ENGLISH) {
    override val tagline = "Sudoku, with nine sealed cells"
    override val continueGame = "Continue"
    override fun continueDetail(d: Difficulty, time: String, seals: Int) = "${difficulty(d)} · $time · $seals/9 seals"
    override val newPuzzle = "New Puzzle"
    override fun newPuzzleOf(d: Difficulty) = "New ${difficulty(d)} puzzle"
    override val daily = "Daily"
    override fun doneToday(streak: Int) = "Done today" + if (streak > 0) " · streak $streak" else ""
    override val modes = "Modes"
    override val stats = "Stats"
    override val settings = "Settings"
    override val howToPlay = "How to play"
    override val skip = "Skip"
    override fun difficulty(d: Difficulty) = when (d) {
        Difficulty.EASY -> "Easy"; Difficulty.MEDIUM -> "Medium"; Difficulty.HARD -> "Hard"
        Difficulty.EXPERT -> "Expert"; Difficulty.MASTER -> "Master"
    }
    override fun mode(m: GameMode) = when (m) {
        GameMode.CLASSIC -> "Classic"; GameMode.DAILY -> "Daily"; GameMode.HARDCORE -> "Hardcore"; GameMode.ZEN -> "Zen"
        GameMode.TIME_PRESSURE -> "Time Pressure"; GameMode.ENDLESS -> "Endless"; GameMode.CUSTOM -> "Custom"
    }
    override fun modeDescription(m: GameMode) = when (m) {
        GameMode.CLASSIC -> "Three strikes. The standard board."
        GameMode.DAILY -> "One board for everyone, every day."
        GameMode.HARDCORE -> "A single strike ends the board."
        GameMode.ZEN -> "No strike limit. No clock pressure."
        else -> "Coming later."
    }

    override val back = "Back"
    override val backToMenu = "Back to menu"
    override val pause = "Pause"
    override val resume = "Resume"
    override val paused = "Paused"
    override val tapToContinue = "Tap to continue"
    override val preparing = "Preparing a board…"
    override val firstBoard = "First board"
    override fun sealsFound(n: Int) = "$n of 9 seals found"
    override fun strikes(used: Int, limit: Int) = "$used of $limit strikes"
    override val undo = "Undo"
    override val erase = "Erase"
    override val notes = "Notes"
    override val seal = "Seal"
    override val suspect = "Suspect"
    override val hint = "Hint"
    override val on = "on"
    override fun digitKey(d: Int) = "Digit $d"
    override fun digitsLeft(n: Int) = "$n left"
    override val digitComplete = "complete"

    override fun boardSummary(sealsFound: Int) = "FUSE9 board, $sealsFound of 9 seals found"
    override fun cellAt(row: Int, col: Int) = "row $row, column $col"
    override val hidden = "hidden"
    override val provenSafe = "proven safe"
    override val markedSuspect = "marked suspect"
    override fun defusedSeal(d: Int) = "defused seal, digit $d"
    override fun trippedSeal(d: Int) = "tripped seal, digit $d"
    override fun openCell(d: Int, seals: Int) = "digit $d, $seals seals adjacent"

    private fun unit(u: Int) = Grid.unitName(u)
    private fun Unit_(u: Int) = unit(u).replaceFirstChar { it.uppercase() }
    private fun cell(c: Int) = Grid.cellName(c)

    override fun look(l: Look) = when (l.at) {
        com.fuse9.game.LookAt.AROUND_BOX -> "Look around ${unit(l.unit)}."
        com.fuse9.game.LookAt.UNIT -> "Look at ${unit(l.unit)}."
        com.fuse9.game.LookAt.DOTS -> "Look at the dots in ${cell(l.cell)}."
        com.fuse9.game.LookAt.SEALS -> "Look at the seals you have found."
    }

    override fun reason(r: Reason) = when (r.why) {
        com.fuse9.game.Why.PROVEN_SAFE -> "It's proven safe."
        com.fuse9.game.Why.SAFE -> "It's safe."
        com.fuse9.game.Why.UNIT_SEALED -> "${Unit_(r.unit)} already has its seal, so this is safe."
        com.fuse9.game.Why.COUNT_SATISFIED -> "The dots at ${cell(r.cell)} are satisfied, so this is safe."
        com.fuse9.game.Why.SEALED_DIGIT_SAFE -> "Its digit is already sealed, and a digit hides only once — safe."
        com.fuse9.game.Why.SEAL_POINTING -> "The ${r.digit}-seal lies in ${unit(r.unit)}; this cell can't be ${r.digit}, so it's safe."
        com.fuse9.game.Why.OVERLAP_SAFE -> "Overlapping dots leave no room for a seal here."
        com.fuse9.game.Why.UNIT_LAST_CELL -> "${Unit_(r.unit)} needs one seal, and every other cell in it is safe."
        com.fuse9.game.Why.COUNT_FULL -> "The dots at ${cell(r.cell)} need every hidden neighbour to be a seal."
        com.fuse9.game.Why.SEAL_HOME -> "Some seal must hide the ${r.digit}. This is the only cell left that can."
        com.fuse9.game.Why.OVERLAP_SEAL -> "Compare the overlapping dots: their seals can only fit here."
        com.fuse9.game.Why.MUST_BE_SEAL -> "This cell has to be a seal."
        com.fuse9.game.Why.HIDDEN_SINGLE -> "In ${unit(r.unit)}, ${r.digit} has nowhere else to go."
        com.fuse9.game.Why.SEAL_HOME_DIGIT -> "The ${r.digit}-seal has only this home."
        com.fuse9.game.Why.NARROWED -> "After narrowing ${unit(r.unit)}, only ${r.digit} fits."
        com.fuse9.game.Why.ONLY_FIT -> "Only ${r.digit} fits: its row, column and box hold the rest."
    }

    override fun guide(line: TutorialGuide.Line) = when (line.kind) {
        TutorialGuide.Kind.LOOK -> "Open cells show a digit. The dots say how many seals touch them."
        TutorialGuide.Kind.PLACE_PROVEN -> "Dashed cells are proven safe. Only ${line.digit} fits the marked one — place it."
        TutorialGuide.Kind.PLACE_BECAUSE -> "${reasons(line.reasons)} Only ${line.digit} fits the marked one — place it."
        TutorialGuide.Kind.DEFUSE -> "${reasons(line.reasons)} Hold the cell to defuse it."
        TutorialGuide.Kind.NINE_SEALS -> "Nine seals: one in every row, column and box. Watch the dots."
        TutorialGuide.Kind.OPENS -> "Placing a digit opens the cell and shows its dots."
        TutorialGuide.Kind.DIGIT_LINK -> "That seal hid a ${line.digit}. Each seal hides a different digit — so every other ${line.digit} is safe."
        TutorialGuide.Kind.DONE -> "That's the whole idea. The rest is yours."
    }

    override fun verdict(v: Verdict) = when (v) {
        Verdict.CLEAN -> "Clean"; Verdict.SHARP -> "Sharp"; Verdict.SOLVED -> "Solved"; Verdict.UNSOLVED -> "Unsealed"
    }
    override fun verdictLine(v: Verdict) = when (v) {
        Verdict.CLEAN -> "No strikes, no hints."
        Verdict.SHARP -> "Almost spotless."
        Verdict.SOLVED -> "Every seal found."
        Verdict.UNSOLVED -> "The board held this time."
    }
    override fun sealsOfNine(n: Int) = "$n of 9 seals"
    override fun strikeCount(n: Int) = if (n == 1) "1 strike" else "$n strikes"
    override fun hintCount(n: Int) = if (n == 1) "1 hint" else "$n hints"
    override fun rippleCount(n: Int) = if (n == 1) "1 ripple" else "$n ripples"
    override fun dailyStreak(n: Int) = "Daily streak  $n"
    override val nextBoard = "Next board"
    override val anotherBoard = "Another board"
    override val menu = "Menu"
    override fun marks(n: Int) = "$n of 5 marks"

    override val solvedLabel = "solved"
    override val cleanLabel = "clean"
    override val dailyStreakLabel = "daily streak"
    override val sealsDefused = "Seals defused"
    override val totalStrikes = "Total strikes"
    override val bestDailyStreak = "Best daily streak"
    override val hardestSolved = "Hardest solved"
    override val colSolved = "Solved"
    override val colBest = "Best"
    override val colAverage = "Average"

    override val soundAndTouch = "Sound & touch"
    override val sound = "Sound"
    override val soundVolume = "Sound volume"
    override fun percent(n: Int) = "$n percent"
    override val haptics = "Haptics"
    override val lookSection = "Look"
    override val theme = "Theme"
    override val themeOptions = listOf("System", "Paper", "Graphite")
    override val highContrast = "High contrast"
    override val sealCounts = "Seal counts"
    override val sealCountsNote = "How open cells show nearby seals"
    override val countOptions = listOf("Dots", "Numbers")
    override val playSection = "Play"
    override val autoClean = "Clear notes automatically"
    override val autoCleanNote = "Remove a digit from notes when it is placed nearby"
    override val highlightMatching = "Highlight matching digits"
    override val showTimer = "Show timer"
    override val language = "Language"
    override val system = "System"
    override val fontsCredit = "Typefaces: Manrope, Fraunces (SIL Open Font License)"

    override val rules = listOf(
        "Fill the grid like Sudoku." to "Every row, column and box holds 1 to 9 once.",
        "Nine cells are seals." to "Exactly one in every row, every column and every box.",
        "Dots count seals." to "An open cell shows how many seals touch it, diagonals included.",
        "Each seal hides a different digit." to "Find the 4-seal, and every other 4 is safe. Defusing a seal shows its digit.",
    )
    override val rulesFooter = "To open a cell, place its digit. To defuse a seal, hold the cell or press the seal key. A wrong move is a strike — and shows you the truth about that cell."
}

/**
 * Turkish. Digits are kept free of case suffixes by pairing them with "rakam" ("7 rakamının
 * mührü"), so no suffix tables for numerals are needed; units get their endings below.
 */
object Turkish : Strings(Locale.forLanguageTag("tr-TR")) {
    private enum class Case { NOM, LOC, DAT, GEN }

    /** "4. satır" with case endings: satırda / satıra / satırın, sütunda / sütuna / sütunun, kutuda / kutuya / kutunun. */
    private fun unit(u: Int, case: Case = Case.NOM): String {
        val (n, base) = when {
            u < 9 -> u + 1 to "satır"
            u < 18 -> u - 8 to "sütun"
            else -> u - 17 to "kutu"
        }
        val vowelEnd = base == "kutu"
        val suffix = when (case) {
            Case.NOM -> ""
            Case.LOC -> "da"
            Case.DAT -> if (vowelEnd) "ya" else "a"
            Case.GEN -> when (base) { "satır" -> "ın"; "sütun" -> "un"; else -> "nun" }
        }
        return "$n. $base$suffix"
    }

    /** "3. satır 4. sütundaki" — the adjective form used before "noktalar". */
    private fun cellAdj(c: Int) = "${Grid.row(c) + 1}. satır ${Grid.col(c) + 1}. sütundaki"
    private fun cap(s: String) = s.replaceFirstChar { it.titlecase(locale) }

    override val tagline = "Dokuz mühürlü hücreyle Sudoku"
    override val continueGame = "Devam et"
    override fun continueDetail(d: Difficulty, time: String, seals: Int) = "${difficulty(d)} · $time · $seals/9 mühür"
    override val newPuzzle = "Yeni bulmaca"
    override fun newPuzzleOf(d: Difficulty) = "Yeni ${difficulty(d).lowercase(locale)} bulmaca"
    override val daily = "Günlük"
    override fun doneToday(streak: Int) = "Bugün tamamlandı" + if (streak > 0) " · seri $streak" else ""
    override val modes = "Modlar"
    override val stats = "İstatistikler"
    override val settings = "Ayarlar"
    override val howToPlay = "Nasıl oynanır"
    override val skip = "Geç"
    override fun difficulty(d: Difficulty) = when (d) {
        Difficulty.EASY -> "Kolay"; Difficulty.MEDIUM -> "Orta"; Difficulty.HARD -> "Zor"
        Difficulty.EXPERT -> "Uzman"; Difficulty.MASTER -> "Usta"
    }
    override fun mode(m: GameMode) = when (m) {
        GameMode.CLASSIC -> "Klasik"; GameMode.DAILY -> "Günlük"; GameMode.HARDCORE -> "Tek Şans"; GameMode.ZEN -> "Zen"
        GameMode.TIME_PRESSURE -> "Zamana Karşı"; GameMode.ENDLESS -> "Sonsuz"; GameMode.CUSTOM -> "Özel"
    }
    override fun modeDescription(m: GameMode) = when (m) {
        GameMode.CLASSIC -> "Üç hata hakkı. Standart tahta."
        GameMode.DAILY -> "Herkese aynı tahta, her gün."
        GameMode.HARDCORE -> "Tek bir hata tahtayı bitirir."
        GameMode.ZEN -> "Hata sınırı yok. Saat baskısı yok."
        else -> "Yakında."
    }

    override val back = "Geri"
    override val backToMenu = "Menüye dön"
    override val pause = "Duraklat"
    override val resume = "Devam et"
    override val paused = "Duraklatıldı"
    override val tapToContinue = "Devam etmek için dokun"
    override val preparing = "Tahta hazırlanıyor…"
    override val firstBoard = "İlk tahta"
    override fun sealsFound(n: Int) = "9 mühürden $n tanesi bulundu"
    override fun strikes(used: Int, limit: Int) = "$limit hata hakkından $used tanesi kullanıldı"
    override val undo = "Geri al"
    override val erase = "Sil"
    override val notes = "Not"
    override val seal = "Mühür"
    override val suspect = "Şüphe"
    override val hint = "İpucu"
    override val on = "açık"
    override fun digitKey(d: Int) = "Rakam $d"
    override fun digitsLeft(n: Int) = "$n tane kaldı"
    override val digitComplete = "tamamlandı"

    override fun boardSummary(sealsFound: Int) = "FUSE9 tahtası, 9 mühürden $sealsFound tanesi bulundu"
    override fun cellAt(row: Int, col: Int) = "$row. satır, $col. sütun"
    override val hidden = "gizli"
    override val provenSafe = "güvenli olduğu kanıtlandı"
    override val markedSuspect = "şüpheli olarak işaretli"
    override fun defusedSeal(d: Int) = "etkisiz mühür, rakam $d"
    override fun trippedSeal(d: Int) = "tetiklenmiş mühür, rakam $d"
    override fun openCell(d: Int, seals: Int) = "rakam $d, $seals komşu mühür"

    override fun look(l: Look) = when (l.at) {
        com.fuse9.game.LookAt.AROUND_BOX -> "${unit(l.unit, Case.GEN)} çevresine bak."
        com.fuse9.game.LookAt.UNIT -> "${unit(l.unit, Case.DAT)} bak."
        com.fuse9.game.LookAt.DOTS -> "${cellAdj(l.cell)} noktalara bak."
        com.fuse9.game.LookAt.SEALS -> "Bulduğun mühürlere bak."
    }

    override fun reason(r: Reason) = when (r.why) {
        com.fuse9.game.Why.PROVEN_SAFE -> "Burası güvenli olarak kanıtlandı."
        com.fuse9.game.Why.SAFE -> "Burası güvenli."
        com.fuse9.game.Why.UNIT_SEALED -> "${cap(unit(r.unit, Case.LOC))} mühür zaten bulundu; burası güvenli."
        com.fuse9.game.Why.COUNT_SATISFIED -> "${cap(cellAdj(r.cell))} noktalar tamam; burası güvenli."
        com.fuse9.game.Why.SEALED_DIGIT_SAFE -> "Rakamı zaten mühürlendi ve her rakam yalnızca bir kez saklanır — güvenli."
        com.fuse9.game.Why.SEAL_POINTING -> "${r.digit} rakamının mührü ${unit(r.unit, Case.LOC)}; bu hücre ${r.digit} olamaz, yani güvenli."
        com.fuse9.game.Why.OVERLAP_SAFE -> "Kesişen noktalar burada mühre yer bırakmıyor."
        com.fuse9.game.Why.UNIT_LAST_CELL -> "${cap(unit(r.unit))} bir mühür istiyor ve diğer bütün hücreleri güvenli."
        com.fuse9.game.Why.COUNT_FULL -> "${cap(cellAdj(r.cell))} noktalar, gizli komşuların hepsinin mühür olmasını gerektiriyor."
        com.fuse9.game.Why.SEAL_HOME -> "${r.digit} rakamını bir mühür saklamak zorunda. Bunu yapabilecek tek hücre bu."
        com.fuse9.game.Why.OVERLAP_SEAL -> "Kesişen noktaları karşılaştır: mühürleri yalnızca buraya sığar."
        com.fuse9.game.Why.MUST_BE_SEAL -> "Bu hücre mühür olmak zorunda."
        com.fuse9.game.Why.HIDDEN_SINGLE -> "${cap(unit(r.unit, Case.LOC))} ${r.digit} rakamının gidebileceği başka yer yok."
        com.fuse9.game.Why.SEAL_HOME_DIGIT -> "${r.digit} rakamının mührü için tek yer burası."
        com.fuse9.game.Why.NARROWED -> "${cap(unit(r.unit, Case.LOC))} adaylar daraltılınca yalnızca ${r.digit} kalıyor."
        com.fuse9.game.Why.ONLY_FIT -> "Yalnızca ${r.digit} uyuyor; diğerleri satırında, sütununda ya da kutusunda var."
    }

    override fun guide(line: TutorialGuide.Line) = when (line.kind) {
        TutorialGuide.Kind.LOOK -> "Açık hücreler rakamını gösterir. Noktalar ona kaç mührün değdiğini söyler."
        TutorialGuide.Kind.PLACE_PROVEN -> "Kesik çizgili hücreler güvenli. İşaretli olana yalnızca ${line.digit} uyuyor — yerleştir."
        TutorialGuide.Kind.PLACE_BECAUSE -> "${reasons(line.reasons)} İşaretli olana yalnızca ${line.digit} uyuyor — yerleştir."
        TutorialGuide.Kind.DEFUSE -> "${reasons(line.reasons)} Etkisiz hale getirmek için hücreye basılı tut."
        TutorialGuide.Kind.NINE_SEALS -> "Dokuz mühür var: her satırda, sütunda ve kutuda bir tane. Noktaları izle."
        TutorialGuide.Kind.OPENS -> "Rakam yerleştirmek hücreyi açar ve noktalarını gösterir."
        TutorialGuide.Kind.DIGIT_LINK -> "O mühür ${line.digit} rakamını saklıyordu. Her mühür farklı bir rakam saklar — yani diğer bütün ${line.digit} rakamları güvenli."
        TutorialGuide.Kind.DONE -> "Bütün fikir bu. Gerisi senin."
    }

    override fun verdict(v: Verdict) = when (v) {
        Verdict.CLEAN -> "Kusursuz"; Verdict.SHARP -> "Keskin"; Verdict.SOLVED -> "Çözüldü"; Verdict.UNSOLVED -> "Mühürlü kaldı"
    }
    override fun verdictLine(v: Verdict) = when (v) {
        Verdict.CLEAN -> "Hata yok, ipucu yok."
        Verdict.SHARP -> "Neredeyse kusursuz."
        Verdict.SOLVED -> "Bütün mühürler bulundu."
        Verdict.UNSOLVED -> "Tahta bu sefer dayandı."
    }
    override fun sealsOfNine(n: Int) = "9 mühürden $n"
    override fun strikeCount(n: Int) = "$n hata"
    override fun hintCount(n: Int) = "$n ipucu"
    override fun rippleCount(n: Int) = "$n dalga"
    override fun dailyStreak(n: Int) = "Günlük seri  $n"
    override val nextBoard = "Sonraki tahta"
    override val anotherBoard = "Bir tahta daha"
    override val menu = "Menü"
    override fun marks(n: Int) = "5 işaretten $n"

    override val solvedLabel = "çözülen"
    override val cleanLabel = "kusursuz"
    override val dailyStreakLabel = "günlük seri"
    override val sealsDefused = "Etkisizleştirilen mühür"
    override val totalStrikes = "Toplam hata"
    override val bestDailyStreak = "En iyi günlük seri"
    override val hardestSolved = "Çözülen en zor"
    override val colSolved = "Çözülen"
    override val colBest = "En iyi"
    override val colAverage = "Ortalama"

    override val soundAndTouch = "Ses ve dokunuş"
    override val sound = "Ses"
    override val soundVolume = "Ses düzeyi"
    override fun percent(n: Int) = "yüzde $n"
    override val haptics = "Titreşim"
    override val lookSection = "Görünüm"
    override val theme = "Tema"
    override val themeOptions = listOf("Sistem", "Kâğıt", "Grafit")
    override val highContrast = "Yüksek kontrast"
    override val sealCounts = "Mühür sayıları"
    override val sealCountsNote = "Açık hücrelerin yakındaki mühürleri gösterme biçimi"
    override val countOptions = listOf("Nokta", "Sayı")
    override val playSection = "Oyun"
    override val autoClean = "Notları otomatik temizle"
    override val autoCleanNote = "Yakına yerleştirilen rakamı notlardan siler"
    override val highlightMatching = "Aynı rakamları vurgula"
    override val showTimer = "Süreyi göster"
    override val language = "Dil"
    override val system = "Sistem"
    override val fontsCredit = "Yazı tipleri: Manrope, Fraunces (SIL Open Font License)"

    override val rules = listOf(
        "Tabloyu Sudoku gibi doldur." to "Her satır, sütun ve kutuda 1'den 9'a kadar her rakam bir kez bulunur.",
        "Dokuz hücre mühürdür." to "Her satırda, her sütunda ve her kutuda tam bir tane.",
        "Noktalar mühürleri sayar." to "Açık bir hücre, çaprazlar dahil ona kaç mührün değdiğini gösterir.",
        "Her mühür farklı bir rakam saklar." to "4'ün mührünü bul; diğer bütün 4'ler güvenlidir. Bir mührü etkisizleştirmek rakamını gösterir.",
    )
    override val rulesFooter = "Bir hücreyi açmak için rakamını yerleştir. Bir mührü etkisizleştirmek için hücreye basılı tut ya da mühür tuşuna bas. Yanlış hamle bir hatadır — ve o hücrenin gerçeğini gösterir."
}

/** SYSTEM resolves against the current configuration locale, so Android 13+ per-app language works too. */
fun stringsFor(language: AppLanguage, systemLocale: Locale): Strings = when (language) {
    AppLanguage.TURKISH -> Turkish
    AppLanguage.ENGLISH -> English
    AppLanguage.SYSTEM -> if (systemLocale.language == "tr") Turkish else English
}

val LocalStrings = staticCompositionLocalOf<Strings> { English }
