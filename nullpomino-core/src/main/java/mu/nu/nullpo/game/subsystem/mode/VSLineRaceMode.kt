package mu.nu.nullpo.game.subsystem.mode

import mu.nu.nullpo.game.component.BGMStatus.BGM
import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.play.GameManager
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil
import kotlin.random.Random

/** VS-LINE RACE Mode */
class VSLineRaceMode:AbstractMode() {

	/** Each player's frame cint */
	private val PLAYER_COLOR_FRAME = intArrayOf(GameEngine.FRAME_COLOR_RED, GameEngine.FRAME_COLOR_BLUE)

	/** Number of lines to clear */
	private var goalLines:IntArray = IntArray(0)

	/** BGM number */
	private var bgmno:Int = 0

	/** Big */
	private var big:BooleanArray = BooleanArray(0)

	/** Sound effects ON/OFF */
	private var enableSE:BooleanArray = BooleanArray(0)

	/** Last preset number used */
	private var presetNumber:IntArray = IntArray(0)

	/** Winner player ID */
	private var winnerID:Int = 0

	/** Win count for each player */
	private var winCount:IntArray = IntArray(0)

	/** Version */
	private var version:Int = 0

	/* Mode name */
	override val name:String = "VS-LINE RACE"

	override val isVSMode:Boolean
		get() = true

	/* Number of players */
	override val players:Int
		get() = MAX_PLAYERS

	/* Mode init */
	override fun modeInit(manager:GameManager) {

		goalLines = IntArray(MAX_PLAYERS)
		bgmno = 0
		big = BooleanArray(MAX_PLAYERS)
		enableSE = BooleanArray(MAX_PLAYERS)
		presetNumber = IntArray(MAX_PLAYERS)
		winnerID = -1
		winCount = IntArray(MAX_PLAYERS)
		version = CURRENT_VERSION
	}

	/** Read speed presets
	 * @param engine GameEngine
	 * @param prop Property file to read from
	 * @param preset Preset number
	 */
	private fun loadPreset(engine:GameEngine, prop:CustomProperties, preset:Int) {
		engine.speed.gravity = prop.getProperty("vslinerace.gravity.$preset", 4)
		engine.speed.denominator = prop.getProperty("vslinerace.denominator.$preset", 256)
		engine.speed.are = prop.getProperty("vslinerace.are.$preset", 10)
		engine.speed.areLine = prop.getProperty("vslinerace.areLine.$preset", 5)
		engine.speed.lineDelay = prop.getProperty("vslinerace.lineDelay.$preset", 20)
		engine.speed.lockDelay = prop.getProperty("vslinerace.lockDelay.$preset", 30)
		engine.speed.das = prop.getProperty("vslinerace.das.$preset", 14)
	}

	/** Save speed presets
	 * @param engine GameEngine
	 * @param prop Property file to save to
	 * @param preset Preset number
	 */
	private fun savePreset(engine:GameEngine, prop:CustomProperties, preset:Int) {
		prop.setProperty("vslinerace.gravity.$preset", engine.speed.gravity)
		prop.setProperty("vslinerace.denominator.$preset", engine.speed.denominator)
		prop.setProperty("vslinerace.are.$preset", engine.speed.are)
		prop.setProperty("vslinerace.areLine.$preset", engine.speed.areLine)
		prop.setProperty("vslinerace.lineDelay.$preset", engine.speed.lineDelay)
		prop.setProperty("vslinerace.lockDelay.$preset", engine.speed.lockDelay)
		prop.setProperty("vslinerace.das.$preset", engine.speed.das)
	}

	/** Load settings not related to speeds
	 * @param engine GameEngine
	 * @param prop Property file to read from
	 */
	private fun loadOtherSetting(engine:GameEngine, prop:CustomProperties) {
		val playerID = engine.playerID
		goalLines[playerID] = prop.getProperty("vslinerace.goalLines.p$playerID", 40)
		bgmno = prop.getProperty("vslinerace.bgmno", 0)
		big[playerID] = prop.getProperty("vslinerace.big.p$playerID", false)
		enableSE[playerID] = prop.getProperty("vslinerace.enableSE.p$playerID", true)
		presetNumber[playerID] = prop.getProperty("vslinerace.presetNumber.p$playerID", 0)
	}

	/** Save settings not related to speeds
	 * @param engine GameEngine
	 * @param prop Property file to save to
	 */
	private fun saveOtherSetting(engine:GameEngine, prop:CustomProperties) {
		val playerID = engine.playerID
		prop.setProperty("vslinerace.goalLines.p$playerID", goalLines[playerID])
		prop.setProperty("vslinerace.bgmno", bgmno)
		prop.setProperty("vslinerace.big.p$playerID", big[playerID])
		prop.setProperty("vslinerace.enableSE.p$playerID", enableSE[playerID])
		prop.setProperty("vslinerace.presetNumber.p$playerID", presetNumber[playerID])
	}

	/* Initialization for each player */
	override fun playerInit(engine:GameEngine, playerID:Int) {
		if(playerID==1) {
			engine.randSeed = owner.engine[0].randSeed
			engine.random = Random(owner.engine[0].randSeed)
		}

		engine.framecolor = PLAYER_COLOR_FRAME[playerID]

		if(!engine.owner.replayMode) {
			version = CURRENT_VERSION
			loadOtherSetting(engine, engine.owner.modeConfig)
			loadPreset(engine, engine.owner.modeConfig, -1-playerID)
		} else {
			version = owner.replayProp.getProperty("vsbattle.version", 0)
			loadOtherSetting(engine, engine.owner.replayProp)
			loadPreset(engine, engine.owner.replayProp, -1-playerID)
		}
	}

	/* Called at settings screen */
	override fun onSetting(engine:GameEngine, playerID:Int):Boolean {
		// Menu
		if(!engine.owner.replayMode&&engine.statc[4]==0) {
			// Configuration changes
			val change = updateCursor(engine, 12, playerID)

			if(change!=0) {
				engine.playSE("change")

				var m = 1
				if(engine.ctrl.isPress(Controller.BUTTON_E)) m = 100
				if(engine.ctrl.isPress(Controller.BUTTON_F)) m = 1000

				when(menuCursor) {

					0 -> engine.speed.gravity = rangeCursor(engine.speed.gravity+change*m, -1, 99999)
					1 -> engine.speed.denominator = rangeCursor(change*m, -1, 99999)
					2 -> engine.speed.are = rangeCursor(engine.speed.are+change, 0, 99)
					3 -> engine.speed.areLine = rangeCursor(engine.speed.areLine+change, 0, 99)
					4 -> engine.speed.lineDelay = rangeCursor(engine.speed.lineDelay+change, 0, 99)
					5 -> engine.speed.lockDelay = rangeCursor(engine.speed.lockDelay+change, 0, 99)
					6 -> engine.speed.das = rangeCursor(engine.speed.das+change, 0, 99)
					7, 8 -> presetNumber[playerID] = rangeCursor(presetNumber[playerID]+change, 0, 99)
					9 -> {
						goalLines[playerID] += change
						if(goalLines[playerID]<1) goalLines[playerID] = 100
						if(goalLines[playerID]>100) goalLines[playerID] = 1
					}
					10 -> big[playerID] = !big[playerID]
					11 -> enableSE[playerID] = !enableSE[playerID]
					12 -> bgmno = rangeCursor(bgmno+change, 0, BGM.count-1)
				}
			}

			// Confirm
			if(engine.ctrl.isPush(Controller.BUTTON_A)&&menuTime>=5) {
				engine.playSE("decide")

				if(menuCursor==7)
					loadPreset(engine, owner.modeConfig, presetNumber[playerID])
				else if(menuCursor==8) {
					savePreset(engine, owner.modeConfig, presetNumber[playerID])
					owner.saveModeConfig()
				} else {
					saveOtherSetting(engine, owner.modeConfig)
					savePreset(engine, owner.modeConfig, -1-playerID)
					owner.saveModeConfig()
					engine.statc[4] = 1
				}
			}

			// Cancel
			if(engine.ctrl.isPush(Controller.BUTTON_B)) engine.quitflag = true

			menuTime++
		} else if(engine.statc[4]==0) {
			// Replay start
			menuTime++
			menuCursor = 0

			if(menuTime>=60) engine.statc[4] = 1
		} else // Start the game when both players are ready
			if(owner.engine[0].statc[4]==1&&owner.engine[1].statc[4]==1&&playerID==1) {
				owner.engine[0].stat = GameEngine.Status.READY
				owner.engine[1].stat = GameEngine.Status.READY
				owner.engine[0].resetStatc()
				owner.engine[1].resetStatc()
			} else if(engine.ctrl.isPush(Controller.BUTTON_B)) engine.statc[4] = 0// Cancel

		return true
	}

	/* Settings screen */
	override fun renderSetting(engine:GameEngine, playerID:Int) {
		if(engine.statc[4]==0) {
			drawMenu(engine, playerID, receiver, 0, EventReceiver.COLOR.ORANGE, 0, "GRAVITY", engine.speed.gravity.toString(),
				"G-MAX", engine.speed.denominator.toString(), "ARE", engine.speed.are.toString(), "ARE LINE",
				engine.speed.areLine.toString(), "LINE DELAY", engine.speed.lineDelay.toString(), "LOCK DELAY",
				engine.speed.lockDelay.toString(), "DAS", engine.speed.das.toString())
			menuColor = EventReceiver.COLOR.GREEN
			drawMenuCompact(engine, playerID, receiver, "LOAD", "${presetNumber[playerID]}", "SAVE", "${presetNumber[playerID]}")
			menuColor = EventReceiver.COLOR.CYAN
			drawMenuCompact(engine, playerID, receiver, "GOAL", "${goalLines[playerID]}", "BIG",
				GeneralUtil.getONorOFF(big[playerID]), "SE", GeneralUtil.getONorOFF(enableSE[playerID]))
			menuColor = EventReceiver.COLOR.PINK
			drawMenuCompact(engine, playerID, receiver, "BGM", "${BGM.values[bgmno]}")
		} else
			receiver.drawMenuFont(engine, playerID, 3, 10, "WAIT", EventReceiver.COLOR.YELLOW)
	}

	/* Called at game start */
	override fun startGame(engine:GameEngine, playerID:Int) {
		engine.big = big[playerID]
		engine.enableSE = enableSE[playerID]
		if(playerID==1) owner.bgmStatus.bgm = BGM.values[bgmno]

		engine.meterColor = GameEngine.METER_COLOR_GREEN
		engine.meterValue = receiver.getMeterMax(engine)
	}

	/* Render score */
	override fun renderLast(engine:GameEngine, playerID:Int) {
		var enemyID = 0
		if(playerID==0) enemyID = 1

		val x = receiver.fieldX(engine, playerID)
		val y = receiver.fieldY(engine, playerID)

		val remainLines = maxOf(0, goalLines[playerID]-engine.statistics.lines)
		var fontColor = EventReceiver.COLOR.WHITE
		if(remainLines in 1..30) fontColor = EventReceiver.COLOR.YELLOW
		if(remainLines in 1..20) fontColor = EventReceiver.COLOR.ORANGE
		if(remainLines in 1..10) fontColor = EventReceiver.COLOR.RED

		val enemyRemainLines = maxOf(0, goalLines[enemyID]-owner.engine[enemyID].statistics.lines)
		var fontColorEnemy = EventReceiver.COLOR.WHITE
		if(enemyRemainLines in 1..30) fontColorEnemy = EventReceiver.COLOR.YELLOW
		if(enemyRemainLines in 1..20) fontColorEnemy = EventReceiver.COLOR.ORANGE
		if(enemyRemainLines in 1..10) fontColorEnemy = EventReceiver.COLOR.RED

		// Lines left (bottom)
		val strLines = "$remainLines"

		if(strLines.length==1)
			receiver.drawMenuFont(engine, playerID, 4, 21, strLines, fontColor, 2f)
		else if(strLines.length==2)
			receiver.drawMenuFont(engine, playerID, 3, 21, strLines, fontColor, 2f)
		else if(strLines.length==3) receiver.drawMenuFont(engine, playerID, 2, 21, strLines, fontColor, 2f)

		// 1st/2nd
		if(remainLines<enemyRemainLines)
			receiver.drawMenuFont(engine, playerID, -2, 22, "1ST", EventReceiver.COLOR.ORANGE)
		else if(remainLines>enemyRemainLines) receiver.drawMenuFont(engine, playerID, -2, 22, "2ND", EventReceiver.COLOR.WHITE)

		// Timer
		if(playerID==0) receiver.drawDirectFont(256, 16, GeneralUtil.getTime(engine.statistics.time))

		// Normal layout
		if(owner.receiver.nextDisplayType!=2&&playerID==0) {
			receiver.drawScoreFont(engine, playerID, 0, 2, "1P LINES", EventReceiver.COLOR.RED)
			receiver.drawScoreFont(engine, playerID, 0, 3, "$remainLines", fontColor)

			receiver.drawScoreFont(engine, playerID, 0, 5, "2P LINES", EventReceiver.COLOR.BLUE)
			receiver.drawScoreFont(engine, playerID, 0, 6, "$enemyRemainLines", fontColorEnemy)

			if(!owner.replayMode) {
				receiver.drawScoreFont(engine, playerID, 0, 8, "1P WINS", EventReceiver.COLOR.RED)
				receiver.drawScoreFont(engine, playerID, 0, 9, "${winCount[0]}")

				receiver.drawScoreFont(engine, playerID, 0, 11, "2P WINS", EventReceiver.COLOR.BLUE)
				receiver.drawScoreFont(engine, playerID, 0, 12, "${winCount[1]}")
			}
		}

		// Big-side-next layout
		if(owner.receiver.nextDisplayType==2) {
			val fontColor2 = if(playerID==0) EventReceiver.COLOR.RED else EventReceiver.COLOR.BLUE

			if(!owner.replayMode) {
				receiver.drawDirectFont(x-44, y+190, "WINS", fontColor2, .5f)
				if(winCount[playerID]>=10)
					receiver.drawDirectFont(x-44, y+204, "${winCount[playerID]}")
				else
					receiver.drawDirectFont(x-36, y+204, "${winCount[playerID]}")
			}
		}
	}

	/* Calculate score */
	override fun calcScore(engine:GameEngine, playerID:Int, lines:Int):Int {
		var enemyID = 0
		if(playerID==0) enemyID = 1

		val remainLines = goalLines[playerID]-engine.statistics.lines
		engine.meterValue = remainLines*receiver.getMeterMax(engine)/goalLines[playerID]

		if(remainLines<=30) engine.meterColor = GameEngine.METER_COLOR_YELLOW
		if(remainLines<=20) engine.meterColor = GameEngine.METER_COLOR_ORANGE
		if(remainLines<=10) engine.meterColor = GameEngine.METER_COLOR_RED

		// Game completed
		if(engine.statistics.lines>=goalLines[playerID]) {
			engine.timerActive = false
			owner.engine[enemyID].stat = GameEngine.Status.GAMEOVER
			owner.engine[enemyID].resetStatc()
		}
		return 0
	}

	override fun onLast(engine:GameEngine, playerID:Int) {
		// Game End
		if(playerID==1&&owner.engine[0].gameActive)
			if(owner.engine[0].stat==GameEngine.Status.GAMEOVER&&owner.engine[1].stat==GameEngine.Status.GAMEOVER) {
				// Draw
				winnerID = -1
				owner.engine[0].gameEnded()
				owner.engine[1].gameEnded()
				owner.bgmStatus.bgm = BGM.Silent
			} else if(owner.engine[0].stat!=GameEngine.Status.GAMEOVER&&owner.engine[1].stat==GameEngine.Status.GAMEOVER) {
				// 1P win
				winnerID = 0
				owner.engine[0].gameEnded()
				owner.engine[1].gameEnded()
				owner.engine[0].stat = GameEngine.Status.EXCELLENT
				owner.engine[0].resetStatc()
				owner.engine[0].statc[1] = 1
				owner.bgmStatus.bgm = BGM.Silent
				if(!owner.replayMode) winCount[0]++
			} else if(owner.engine[0].stat==GameEngine.Status.GAMEOVER&&owner.engine[1].stat!=GameEngine.Status.GAMEOVER) {
				// 2P win
				winnerID = 1
				owner.engine[0].gameEnded()
				owner.engine[1].gameEnded()
				owner.engine[1].stat = GameEngine.Status.EXCELLENT
				owner.engine[1].resetStatc()
				owner.engine[1].statc[1] = 1
				owner.bgmStatus.bgm = BGM.Silent
				if(!owner.replayMode) winCount[1]++
			}
	}

	/* Render results screen */
	override fun renderResult(engine:GameEngine, playerID:Int) {
		receiver.drawMenuFont(engine, playerID, 0, 0, "RESULT", EventReceiver.COLOR.ORANGE)
		when(winnerID) {
			-1 -> receiver.drawMenuFont(engine, playerID, 6, 1, "DRAW", EventReceiver.COLOR.GREEN)
			playerID -> receiver.drawMenuFont(engine, playerID, 6, 1, "WIN!", EventReceiver.COLOR.YELLOW)
			else -> receiver.drawMenuFont(engine, playerID, 6, 1, "LOSE", EventReceiver.COLOR.WHITE)
		}
		drawResultStats(engine, playerID, receiver, 2, EventReceiver.COLOR.ORANGE, Statistic.LINES, Statistic.PIECE, Statistic.LPM,
			Statistic.PPS, Statistic.TIME)
	}

	/* Called when saving replay */
	override fun saveReplay(engine:GameEngine, playerID:Int, prop:CustomProperties) {
		saveOtherSetting(engine, owner.replayProp)
		savePreset(engine, owner.replayProp, -1-playerID)
		owner.replayProp.setProperty("vslinerace.version", version)
	}

	companion object {
		/** Current version */
		private const val CURRENT_VERSION = 0

		/** Number of players */
		private const val MAX_PLAYERS = 2
	}
}
