/* Copyright (c) 2010, NullNoname
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * Neither the name of NullNoname nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE. */
package mu.nu.nullpo.game.subsystem.mode

import mu.nu.nullpo.game.component.BGMStatus.BGM
import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.game.net.NetUtil
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil
import org.apache.log4j.Logger

/** LINE RACE Mode */
class SprintLine:NetDummyMode() {

	/** BGM number */
	private var bgmno:Int = 0

	/** Big */
	private var big:Boolean = false

	/** Target line count type (0=20,1=40,2=100) */
	private var goaltype:Int = 0

	/** Last preset number used */
	private var presetNumber:Int = 0

	/** Current round's ranking rank */
	private var rankingRank:Int = 0

	/** Rankings' times */
	private var rankingTime:Array<IntArray> = Array(GOALTYPE_MAX) {IntArray(RANKING_MAX)}

	/** Rankings' piece counts */
	private var rankingPiece:Array<IntArray> = Array(GOALTYPE_MAX) {IntArray(RANKING_MAX)}

	/** Rankings' PPS values */
	private var rankingPPS:Array<FloatArray> = Array(GOALTYPE_MAX) {FloatArray(RANKING_MAX)}

	/* Mode name */
	override val name:String = "Lines SprintRace"
	override val gameIntensity:Int = 2
	/* Initialization for each player */
	override fun playerInit(engine:GameEngine, playerID:Int) {
		log.debug("playerInit")

		super.playerInit(engine, playerID)

		bgmno = 0
		big = false
		goaltype = 0
		presetNumber = 0

		rankingRank = -1
		rankingTime = Array(GOALTYPE_MAX) {IntArray(RANKING_MAX)}
		rankingPiece = Array(GOALTYPE_MAX) {IntArray(RANKING_MAX)}
		rankingPPS = Array(GOALTYPE_MAX) {FloatArray(RANKING_MAX)}

		engine.framecolor = GameEngine.FRAME_COLOR_RED

		netPlayerInit(engine, playerID)

		if(!engine.owner.replayMode) {
			presetNumber = engine.owner.modeConfig.getProperty("linerace.presetNumber", 0)
			loadPreset(engine, engine.owner.modeConfig, -1)
			loadRanking(owner.recordProp, engine.ruleopt.strRuleName)
		} else {
			presetNumber = 0
			loadPreset(engine, engine.owner.replayProp, -1)

			// NET: Load name
			netPlayerName = engine.owner.replayProp.getProperty("$playerID.net.netPlayerName", "")
		}
	}

	/** Load options from a preset
	 * @param engine GameEngine
	 * @param prop Property file to read from
	 * @param preset Preset number
	 */
	private fun loadPreset(engine:GameEngine, prop:CustomProperties, preset:Int) {
		engine.speed.gravity = prop.getProperty("linerace.gravity.$preset", 1)
		engine.speed.denominator = prop.getProperty("linerace.denominator.$preset", 256)
		engine.speed.are = prop.getProperty("linerace.are.$preset", 0)
		engine.speed.areLine = prop.getProperty("linerace.areLine.$preset", 0)
		engine.speed.lineDelay = prop.getProperty("linerace. lineDelay.$preset", 0)
		engine.speed.lockDelay = prop.getProperty("linerace.lockDelay.$preset", 30)
		engine.speed.das = prop.getProperty("linerace.das.$preset", 10)
		bgmno = prop.getProperty("linerace.bgmno.$preset", BGM.values.indexOf(BGM.Rush(0)))
		big = prop.getProperty("linerace.big.$preset", false)
		goaltype = prop.getProperty("linerace.goaltype.$preset", 1)
	}

	/** Save options to a preset
	 * @param engine GameEngine
	 * @param prop Property file to save to
	 * @param preset Preset number
	 */
	private fun savePreset(engine:GameEngine, prop:CustomProperties, preset:Int) {
		prop.setProperty("linerace.gravity.$preset", engine.speed.gravity)
		prop.setProperty("linerace.denominator.$preset", engine.speed.denominator)
		prop.setProperty("linerace.are.$preset", engine.speed.are)
		prop.setProperty("linerace.areLine.$preset", engine.speed.areLine)
		prop.setProperty("linerace.lineDelay.$preset", engine.speed.lineDelay)
		prop.setProperty("linerace.lockDelay.$preset", engine.speed.lockDelay)
		prop.setProperty("linerace.das.$preset", engine.speed.das)
		prop.setProperty("linerace.bgmno.$preset", bgmno)
		prop.setProperty("linerace.big.$preset", big)
		prop.setProperty("linerace.goaltype.$preset", goaltype)
	}

	/* Called at settings screen */
	override fun onSetting(engine:GameEngine, playerID:Int):Boolean {
		// NET: Net Ranking
		if(netIsNetRankingDisplayMode)
			netOnUpdateNetPlayRanking(engine, goaltype)
		else if(!engine.owner.replayMode) {
			// Configuration changes
			val change = updateCursor(engine, 11, playerID)

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
					7 -> bgmno = rangeCursor(bgmno+change, 0, BGM.count-1)
					8 -> big = !big
					9 -> {
						goaltype += change
						if(goaltype<0) goaltype = 2
						if(goaltype>2) goaltype = 0
					}
					10, 11 -> presetNumber = rangeCursor(presetNumber+change, 0, 99)
				}

				// NET: Signal options change
				if(netIsNetPlay&&netNumSpectators>0) netSendOptions(engine)
			}

			// Confirm
			if(engine.ctrl.isPush(Controller.BUTTON_A)&&menuTime>=5&&!netIsWatch) {
				engine.playSE("decide")

				if(menuCursor==10) {
					// Load preset
					loadPreset(engine, owner.modeConfig, presetNumber)

					// NET: Signal options change
					if(netIsNetPlay&&netNumSpectators>0) netSendOptions(engine)
				} else if(menuCursor==11) {
					// Save preset
					savePreset(engine, owner.modeConfig, presetNumber)
					owner.saveModeConfig()
				} else {
					// Save settings
					owner.modeConfig.setProperty("linerace.presetNumber", presetNumber)
					savePreset(engine, owner.modeConfig, -1)
					owner.saveModeConfig()

					// NET: Signal start of the game
					if(netIsNetPlay) netLobby!!.netPlayerClient!!.send("start1p\n")

					return false
				}
			}

			// Cancel
			if(engine.ctrl.isPush(Controller.BUTTON_B)&&!netIsNetPlay) engine.quitflag = true

			// NET: Netplay Ranking
			if(engine.ctrl.isPush(Controller.BUTTON_D)&&netIsNetPlay&&!big
				&&engine.ai==null)
				netEnterNetPlayRankingScreen(engine, playerID, goaltype)

			menuTime++
		} else {
			menuTime++
			menuCursor = -1

			return menuTime<60
		}// Replay

		return true
	}

	/* Render settings screen */
	override fun renderSetting(engine:GameEngine, playerID:Int) {
		if(netIsNetRankingDisplayMode)
		// NET: Netplay Ranking
			netOnRenderNetPlayRanking(engine, playerID, receiver)
		else {
			drawMenuSpeeds(engine, playerID, receiver, 0, EventReceiver.COLOR.BLUE, 0)
			drawMenuBGM(engine, playerID, receiver, bgmno)
			drawMenuCompact(engine, playerID, receiver, "BIG", GeneralUtil.getONorOFF(big), "GOAL", "${GOAL_TABLE[goaltype]}")
			if(!engine.owner.replayMode) {
				menuColor = EventReceiver.COLOR.GREEN
				drawMenuCompact(engine, playerID, receiver, "LOAD", "$presetNumber", "SAVE", "$presetNumber")
			}
		}
	}

	/* This function will be called before the game actually begins (after
 * Ready&Go screen disappears) */
	override fun startGame(engine:GameEngine, playerID:Int) {
		engine.big = big

		owner.bgmStatus.bgm = if(netIsWatch) BGM.Silent else BGM.values[bgmno]

		engine.meterColor = GameEngine.METER_COLOR_GREEN
		engine.meterValue = receiver.getMeterMax(engine)
	}

	/* Score display */
	override fun renderLast(engine:GameEngine, playerID:Int) {
		if(owner.menuOnly) return

		receiver.drawScoreFont(engine, playerID, 0, 0, "LINE RACE", EventReceiver.COLOR.RED)
		receiver.drawScoreFont(engine, playerID, 0, 1, "(${GOAL_TABLE[goaltype]} LINES GAME)", EventReceiver.COLOR.RED)

		if(engine.stat==GameEngine.Status.SETTING||engine.stat==GameEngine.Status.RESULT&&!owner.replayMode) {
			if(!owner.replayMode&&!big&&engine.ai==null&&!netIsWatch) {
				val scale = if(receiver.nextDisplayType==2) .5f else 1f
				val topY = if(receiver.nextDisplayType==2) 6 else 4
				receiver.drawScoreFont(engine, playerID, 3, topY-1, "TIME     PIECE P/SEC", EventReceiver.COLOR.BLUE, scale)

				for(i in 0 until RANKING_MAX) {
					receiver.drawScoreGrade(engine, playerID, 0, topY+i, String.format("%2d", i+1), EventReceiver.COLOR.YELLOW, scale)
					receiver.drawScoreNum(engine, playerID, 3, topY+i,
						GeneralUtil.getTime(rankingTime[goaltype][i]), rankingRank==i, scale)
					receiver.drawScoreNum(engine, playerID, 12, topY+i,
						"${rankingPiece[goaltype][i]}", rankingRank==i, scale)
					receiver.drawScoreNum(engine, playerID, 18, topY+i,
						String.format("%.5g", rankingPPS[goaltype][i]), rankingRank==i, scale)
				}
			}
		} else {
			receiver.drawScoreFont(engine, playerID, 0, 3, "LINE", EventReceiver.COLOR.BLUE)
			val remainLines = GOAL_TABLE[goaltype]-engine.statistics.lines
			var strLines = "$remainLines"
			if(remainLines<0) strLines = "0"
			var fontcolor = EventReceiver.COLOR.WHITE
			if(remainLines in 1..30) fontcolor = EventReceiver.COLOR.YELLOW
			if(remainLines in 1..20) fontcolor = EventReceiver.COLOR.ORANGE
			if(remainLines in 1..10) fontcolor = EventReceiver.COLOR.RED
			receiver.drawScoreNum(engine, playerID, 0, 4, strLines, fontcolor)

			if(strLines.length==1)
				receiver.drawMenuNum(engine, playerID, 4, 21, strLines, fontcolor, 2f)
			else if(strLines.length==2)
				receiver.drawMenuNum(engine, playerID, 3, 21, strLines, fontcolor, 2f)
			else if(strLines.length==3) receiver.drawMenuNum(engine, playerID, 2, 21, strLines, fontcolor, 2f)

			receiver.drawScoreFont(engine, playerID, 0, 6, "PIECE", EventReceiver.COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 0, 7, engine.statistics.totalPieceLocked.toString())

			receiver.drawScoreFont(engine, playerID, 0, 9, "LINE/MIN", EventReceiver.COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 0, 10, "${engine.statistics.lpm}")

			receiver.drawScoreFont(engine, playerID, 0, 12, "PIECE/SEC", EventReceiver.COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 0, 13, engine.statistics.pps.toString())

			receiver.drawScoreFont(engine, playerID, 0, 15, "Time", EventReceiver.COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 0, 16, GeneralUtil.getTime(engine.statistics.time))
		}

		super.renderLast(engine, playerID)
	}

	/* Calculate score */
	override fun calcScore(engine:GameEngine, playerID:Int, lines:Int):Int {
		val remainLines = GOAL_TABLE[goaltype]-engine.statistics.lines
		engine.meterValue = remainLines*receiver.getMeterMax(engine)/GOAL_TABLE[goaltype]

		if(remainLines<=30) engine.meterColor = GameEngine.METER_COLOR_YELLOW
		if(remainLines<=20) engine.meterColor = GameEngine.METER_COLOR_ORANGE
		if(remainLines<=10) engine.meterColor = GameEngine.METER_COLOR_RED

		// Game completed
		if(engine.statistics.lines>=GOAL_TABLE[goaltype]) {
			engine.ending = 1
			engine.gameEnded()
		} else if(engine.statistics.lines>=GOAL_TABLE[goaltype]-5) owner.bgmStatus.fadesw = true
		return 0
	}

	/* Render results screen */
	override fun renderResult(engine:GameEngine, playerID:Int) {
		drawResultStats(engine, playerID, receiver, 1, EventReceiver.COLOR.BLUE, Statistic.LINES, Statistic.PIECE, Statistic.TIME,
			Statistic.LPM, Statistic.PPS)
		drawResultRank(engine, playerID, receiver, 11, EventReceiver.COLOR.BLUE, rankingRank)
		drawResultNetRank(engine, playerID, receiver, 13, EventReceiver.COLOR.BLUE, netRankingRank[0])
		drawResultNetRankDaily(engine, playerID, receiver, 15, EventReceiver.COLOR.BLUE, netRankingRank[1])

		if(netIsPB) receiver.drawMenuFont(engine, playerID, 2, 18, "NEW PB", EventReceiver.COLOR.ORANGE)

		if(netIsNetPlay&&netReplaySendStatus==1)
			receiver.drawMenuFont(engine, playerID, 0, 19, "SENDING...", EventReceiver.COLOR.PINK)
		else if(netIsNetPlay&&!netIsWatch
			&&netReplaySendStatus==2)
			receiver.drawMenuFont(engine, playerID, 1, 19, "A: RETRY", EventReceiver.COLOR.RED)
	}

	/* Save replay file */
	override fun saveReplay(engine:GameEngine, playerID:Int, prop:CustomProperties) {
		savePreset(engine, engine.owner.replayProp, -1)

		// NET: Save name
		if(netPlayerName!=null&&netPlayerName!!.isNotEmpty()) prop.setProperty("$playerID.net.netPlayerName", netPlayerName)

		// Update rankings
		if(!owner.replayMode&&engine.statistics.lines>=GOAL_TABLE[goaltype]&&!big&&engine.ai==null&&!netIsWatch) {
			updateRanking(engine.statistics.time, engine.statistics.totalPieceLocked, engine.statistics.pps)

			if(rankingRank!=-1) {
				saveRanking(owner.recordProp, engine.ruleopt.strRuleName)
				owner.saveModeConfig()
			}
		}
	}

	/** Read rankings from property file
	 * @param prop Property file
	 * @param ruleName Rule name
	 */
	override fun loadRanking(prop:CustomProperties, ruleName:String) {
		for(i in 0 until GOALTYPE_MAX)
			for(j in 0 until RANKING_MAX) {
				rankingTime[i][j] = prop.getProperty("linerace.ranking.$ruleName.$i.time.$j", -1)
				rankingPiece[i][j] = prop.getProperty("linerace.ranking.$ruleName.$i.piece.$j", 0)
				rankingPPS[i][j] = prop.getProperty("linerace.ranking.$ruleName.$i.pps.$j", 0f)
			}
	}

	/** Save rankings to property file
	 * @param prop Property file
	 * @param ruleName Rule name
	 */
	fun saveRanking(prop:CustomProperties, ruleName:String) {
		for(i in 0 until GOALTYPE_MAX)
			for(j in 0 until RANKING_MAX) {
				prop.setProperty("linerace.ranking.$ruleName.$i.time.$j", rankingTime[i][j])
				prop.setProperty("linerace.ranking.$ruleName.$i.piece.$j", rankingPiece[i][j])
				prop.setProperty("linerace.ranking.$ruleName.$i.pps.$j", rankingPPS[i][j])
			}
	}

	/** Update rankings
	 * @param time Time
	 * @param piece Piece count
	 */
	private fun updateRanking(time:Int, piece:Int, pps:Float) {
		rankingRank = checkRanking(time, piece, pps)

		if(rankingRank!=-1) {
			// Shift down ranking entries
			for(i in RANKING_MAX-1 downTo rankingRank+1) {
				rankingTime[goaltype][i] = rankingTime[goaltype][i-1]
				rankingPiece[goaltype][i] = rankingPiece[goaltype][i-1]
				rankingPPS[goaltype][i] = rankingPPS[goaltype][i-1]
			}

			// Add new data
			rankingTime[goaltype][rankingRank] = time
			rankingPiece[goaltype][rankingRank] = piece
			rankingPPS[goaltype][rankingRank] = pps
		}
	}

	/** Calculate ranking position
	 * @param time Time
	 * @param piece Piece count
	 * @return Position (-1 if unranked)
	 */
	private fun checkRanking(time:Int, piece:Int, pps:Float):Int {
		for(i in 0 until RANKING_MAX)
			if(time<rankingTime[goaltype][i]||rankingTime[goaltype][i]<0)
				return i
			else if(time==rankingTime[goaltype][i]&&(piece<rankingPiece[goaltype][i]||rankingPiece[goaltype][i]==0))
				return i
			else if(time==rankingTime[goaltype][i]&&piece==rankingPiece[goaltype][i]
				&&pps>rankingPPS[goaltype][i])
				return i

		return -1
	}

	/** NET: Send various in-game stats (as well as goaltype)
	 * @param engine GameEngine
	 */
	override fun netSendStats(engine:GameEngine) {
		var msg = "game\tstats\t"
		msg += "${engine.statistics.lines}\t${engine.statistics.totalPieceLocked}\t"
		msg += "${engine.statistics.time}\t${engine.statistics.lpm}\t"
		msg += engine.statistics.pps.toString()+"\t$goaltype\t"
		msg += engine.gameActive.toString()+"\t"+engine.timerActive
		msg += "\n"
		netLobby!!.netPlayerClient!!.send(msg)
	}

	/** NET: Receive various in-game stats (as well as goaltype) */
	override fun netRecvStats(engine:GameEngine, message:Array<String>) {
		engine.statistics.lines = message[4].toInt()
		engine.statistics.totalPieceLocked = message[5].toInt()
		engine.statistics.time = message[6].toInt()
		//engine.statistics.lpm = message[7].toFloat()
		//engine.statistics.pps = message[8].toFloat()
		goaltype = message[9].toInt()
		engine.gameActive = message[10].toBoolean()
		engine.timerActive = message[11].toBoolean()

		// Update meter
		val remainLines = GOAL_TABLE[goaltype]-engine.statistics.lines
		engine.meterValue = remainLines*receiver.getMeterMax(engine)/GOAL_TABLE[goaltype]
		if(remainLines<=30) engine.meterColor = GameEngine.METER_COLOR_YELLOW
		if(remainLines<=20) engine.meterColor = GameEngine.METER_COLOR_ORANGE
		if(remainLines<=10) engine.meterColor = GameEngine.METER_COLOR_RED
	}

	/** NET: Send end-of-game stats
	 * @param engine GameEngine
	 */
	override fun netSendEndGameStats(engine:GameEngine) {
		var subMsg = ""
		subMsg += "LINE;${engine.statistics.lines}/${GOAL_TABLE[goaltype]}\t"
		subMsg += "PIECE;${engine.statistics.totalPieceLocked}\t"
		subMsg += "TIME;${GeneralUtil.getTime(engine.statistics.time)}\t"
		subMsg += "LINE/MIN;${engine.statistics.lpm}\t"
		subMsg += "PIECE/SEC;${engine.statistics.pps}\t"

		val msg = "gstat1p\t${NetUtil.urlEncode(subMsg)}\n"
		netLobby!!.netPlayerClient!!.send(msg)
	}

	/** NET: Send game options to all spectators
	 * @param engine GameEngine
	 */
	override fun netSendOptions(engine:GameEngine) {
		var msg = "game\toption\t"
		msg += engine.speed.gravity.toString()+"\t${engine.speed.denominator}\t${engine.speed.are}\t"
		msg += engine.speed.areLine.toString()+"\t${engine.speed.lineDelay}\t${engine.speed.lockDelay}\t"
		msg += engine.speed.das.toString()+"\t$bgmno\t$big\t$goaltype\t"+presetNumber
		msg += "\n"
		netLobby!!.netPlayerClient!!.send(msg)
	}

	/** NET: Receive game options */
	override fun netRecvOptions(engine:GameEngine, message:Array<String>) {
		engine.speed.gravity = message[4].toInt()
		engine.speed.denominator = message[5].toInt()
		engine.speed.are = message[6].toInt()
		engine.speed.areLine = message[7].toInt()
		engine.speed.lineDelay = message[8].toInt()
		engine.speed.lockDelay = message[9].toInt()
		engine.speed.das = message[10].toInt()
		bgmno = message[11].toInt()
		big = message[12].toBoolean()
		goaltype = message[13].toInt()
		presetNumber = message[14].toInt()
	}

	/** NET: Get goal type */
	override fun netGetGoalType():Int = goaltype

	/** NET: It returns true when the current settings doesn't prevent
	 * leaderboard screen from showing. */
	override fun netIsNetRankingViewOK(engine:GameEngine):Boolean = !big&&engine.ai==null

	/** NET: It returns true when the current settings doesn't prevent replay
	 * data from sending. */
	override fun netIsNetRankingSendOK(engine:GameEngine):Boolean =
		netIsNetRankingViewOK(engine)&&engine.statistics.lines>=GOAL_TABLE[goaltype]

	companion object {
		/* ----- Main variables ----- */
		/** Logger */
		internal val log = Logger.getLogger(SprintLine::class.java)

		/** Number of entries in rankings */
		private const val RANKING_MAX = 10

		/** Target line count type */
		private const val GOALTYPE_MAX = 3

		/** Target line count constants */
		private val GOAL_TABLE = intArrayOf(20, 40, 100)
	}
}
