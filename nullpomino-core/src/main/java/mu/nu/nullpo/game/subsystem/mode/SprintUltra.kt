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
import kotlin.math.ceil

/** ULTRA Mode */
class SprintUltra:NetDummyMode() {

	/** Most recent increase in score */
	private var lastscore:Int = 0
	private var sum:Int = 0
	private var pow:Int = 0

	/** Time to display the most recent increase in score */
	private var scgettime:Int = 0

	/** Most recent scoring event b2b */
	private var lastb2b:Boolean = false

	/** Most recent scoring event combo count */
	private var lastcombo:Int = 0

	/** Most recent scoring event piece ID */
	private var lastpiece:Int = 0

	/** BGM number */
	private var bgmno:Int = 0

	/** Big */
	private var big:Boolean = false

	/** Time limit type */
	private var goaltype:Int = 0

	/** Last preset number used */
	private var presetNumber:Int = 0

	/** Version */
	private var version:Int = 0

	/** Current round's ranking rank */
	private var rankingRank:IntArray = IntArray(0)

	/** Rankings' scores */
	private var rankingScore:Array<Array<IntArray>> = Array(GOALTYPE_MAX) {Array(RANKING_TYPE) {IntArray(RANKING_MAX)}}

	/** Rankings' line counts */
	private var rankingLines:Array<Array<IntArray>> = Array(GOALTYPE_MAX) {Array(RANKING_TYPE) {IntArray(RANKING_MAX)}}

	/** Rankings' sent line counts */
	private var rankingPower:Array<Array<IntArray>> = Array(GOALTYPE_MAX) {Array(RANKING_TYPE) {IntArray(RANKING_MAX)}}

	/* Mode name */
	override val name:String = "ULTRA Score Attack"
	override val gameIntensity:Int = 2

	/* Initialization */
	override fun playerInit(engine:GameEngine, playerID:Int) {
		super.playerInit(engine, playerID)
		lastscore = 0
		scgettime = 0
		lastb2b = false
		lastcombo = 0
		lastpiece = 0
		bgmno = 0
		pow = 0

		rankingRank = IntArray(RANKING_TYPE)
		rankingRank[0] = -1
		rankingRank[1] = -1

		rankingScore = Array(GOALTYPE_MAX) {Array(RANKING_TYPE) {IntArray(RANKING_MAX)}}
		rankingLines = Array(GOALTYPE_MAX) {Array(RANKING_TYPE) {IntArray(RANKING_MAX)}}
		rankingPower = Array(GOALTYPE_MAX) {Array(RANKING_TYPE) {IntArray(RANKING_MAX)}}

		engine.framecolor = GameEngine.FRAME_COLOR_BLUE

		netPlayerInit(engine, playerID)

		if(!engine.owner.replayMode) {
			presetNumber = engine.owner.modeConfig.getProperty("ultra.presetNumber", 0)
			loadPreset(engine, engine.owner.modeConfig, -1)
			loadRanking(owner.recordProp, engine.ruleopt.strRuleName)
			version = CURRENT_VERSION
		} else {
			presetNumber = 0
			loadPreset(engine, engine.owner.replayProp, -1)
			version = engine.owner.replayProp.getProperty("ultra.version", 0)
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
		engine.speed.gravity = prop.getProperty("ultra.gravity.$preset", 1)
		engine.speed.denominator = prop.getProperty("ultra.denominator.$preset", 256)
		engine.speed.are = prop.getProperty("ultra.are.$preset", 0)
		engine.speed.areLine = prop.getProperty("ultra.areLine.$preset", 0)
		engine.speed.lineDelay = prop.getProperty("ultra. lineDelay.$preset", 0)
		engine.speed.lockDelay = prop.getProperty("ultra.lockDelay.$preset", 30)
		engine.speed.das = prop.getProperty("ultra.das.$preset", 10)
		bgmno = prop.getProperty("ultra.bgmno.$preset", BGM.values.indexOf(BGM.Rush(0)))
		big = prop.getProperty("ultra.big.$preset", false)
		goaltype = prop.getProperty("ultra.goaltype.$preset", 2)
	}

	/** Save options to a preset
	 * @param engine GameEngine
	 * @param prop Property file to save to
	 * @param preset Preset number
	 */
	private fun savePreset(engine:GameEngine, prop:CustomProperties, preset:Int) {
		prop.setProperty("ultra.gravity.$preset", engine.speed.gravity)
		prop.setProperty("ultra.denominator.$preset", engine.speed.denominator)
		prop.setProperty("ultra.are.$preset", engine.speed.are)
		prop.setProperty("ultra.areLine.$preset", engine.speed.areLine)
		prop.setProperty("ultra.lineDelay.$preset", engine.speed.lineDelay)
		prop.setProperty("ultra.lockDelay.$preset", engine.speed.lockDelay)
		prop.setProperty("ultra.das.$preset", engine.speed.das)
		prop.setProperty("ultra.bgmno.$preset", bgmno)
		prop.setProperty("ultra.big.$preset", big)
		prop.setProperty("ultra.goaltype.$preset", goaltype)
	}

	/* Called at settings screen */
	override fun onSetting(engine:GameEngine, playerID:Int):Boolean {
		// NET: Net Ranking
		if(netIsNetRankingDisplayMode)
			netOnUpdateNetPlayRanking(engine, goaltype)
		else if(!engine.owner.replayMode) {
			// Configuration changes
			val change = updateCursor(engine, 17)

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
						if(goaltype<0) goaltype = GOALTYPE_MAX-1
						if(goaltype>GOALTYPE_MAX-1) goaltype = 0
					}
					10, 11 -> presetNumber = rangeCursor(presetNumber+change, 0, 99)
				}

				// NET: Signal options change
				if(netIsNetPlay&&netNumSpectators>0) netSendOptions(engine)
			}

			// Confirm
			if(engine.ctrl.isPush(Controller.BUTTON_A)&&menuTime>=5) {
				engine.playSE("decide")

				if(menuCursor==16) {
					loadPreset(engine, owner.modeConfig, presetNumber)

					// NET: Signal options change
					if(netIsNetPlay&&netNumSpectators>0) netSendOptions(engine)
				} else if(menuCursor==17) {
					savePreset(engine, owner.modeConfig, presetNumber)
					owner.saveModeConfig()
				} else {
					owner.modeConfig.setProperty("ultra.presetNumber", presetNumber)
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
			if(engine.ctrl.isPush(Controller.BUTTON_D)&&netIsNetPlay&&!netIsWatch
				&&netIsNetRankingViewOK(engine))
				netEnterNetPlayRankingScreen(engine, playerID, goaltype)

			menuTime++
		} else {
			menuTime++
			menuCursor = 0

			if(menuTime>=60) menuCursor = 10
			return menuTime<120
		}// Replay

		return true
	}

	/* Setting screen drawing */
	override fun renderSetting(engine:GameEngine, playerID:Int) {
		if(netIsNetRankingDisplayMode)
		// NET: Netplay Ranking
			netOnRenderNetPlayRanking(engine, playerID, receiver)
		else {
			drawMenuSpeeds(engine, playerID, receiver, 0, EventReceiver.COLOR.BLUE, 0)
			drawMenuBGM(engine, playerID, receiver, bgmno)
			drawMenuCompact(engine, playerID, receiver, "BIG", GeneralUtil.getONorOFF(big), "GOAL", (goaltype+1).toString()+"MIN")
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
		engine.b2bEnable = true
		engine.splitb2b = true
		engine.comboType = GameEngine.COMBO_TYPE_NORMAL
		engine.meterValue = 320
		engine.meterColor = GameEngine.METER_COLOR_GREEN

		if(netIsWatch)
			owner.bgmStatus.bgm = BGM.Silent
		else
			owner.bgmStatus.bgm = BGM.values[bgmno]

		engine.twistAllowKick = true
		engine.twistEnable = true
		engine.useAllSpinBonus = true
		engine.twistEnableEZ = true
	}

	/* Render score */
	override fun renderLast(engine:GameEngine, playerID:Int) {
		if(owner.menuOnly) return

		receiver.drawScoreFont(engine, playerID, 0, 0, name, EventReceiver.COLOR.CYAN)
		receiver.drawScoreFont(engine, playerID, 0, 1, "(${(goaltype+1)} MINUTES SPRINT)", EventReceiver.COLOR.CYAN)

		if(engine.stat==GameEngine.Status.SETTING||engine.stat==GameEngine.Status.RESULT&&!owner.replayMode) {
			if(!owner.replayMode&&!big&&engine.ai==null) {
				receiver.drawScoreFont(engine, playerID, 0, 3, "SCORE RANKING", EventReceiver.COLOR.GREEN)
				receiver.drawScoreFont(engine, playerID, 3, 4, "SCORE  LINE", EventReceiver.COLOR.BLUE)

				for(i in 0 until RANKING_MAX) {
					receiver.drawScoreGrade(engine, playerID, 0, 5+i, String.format("%2d", i+1), EventReceiver.COLOR.YELLOW)
					receiver.drawScoreNum(engine, playerID, 3, 5+i, "${rankingScore[goaltype][0][i]}", i==rankingRank[0])
					receiver.drawScoreNum(engine, playerID, 10, 5+i, "${rankingLines[goaltype][0][i]}", i==rankingRank[0])
				}

				receiver.drawScoreFont(engine, playerID, 0, 11, "LINE RANKING", EventReceiver.COLOR.GREEN)
				receiver.drawScoreFont(engine, playerID, 3, 12, "LINE SCORE", EventReceiver.COLOR.BLUE)

				for(i in 0 until RANKING_MAX) {
					receiver.drawScoreGrade(engine, playerID, 0, 13+i, String.format("%2d", i+1), EventReceiver.COLOR.YELLOW)
					receiver.drawScoreNum(engine, playerID, 3, 13+i, "${rankingLines[goaltype][1][i]}", i==rankingRank[1])
					receiver.drawScoreNum(engine, playerID, 8, 13+i, "${rankingScore[goaltype][1][i]}", i==rankingRank[1])
				}
			}
		} else {
			receiver.drawScoreFont(engine, playerID, 0, 3, "Score", EventReceiver.COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 5, 3, "+$lastscore")
			receiver.drawScoreNum(engine, playerID, 0, 4, "$scgettime", 2f)
			if(scgettime<engine.statistics.score) scgettime += ceil(((engine.statistics.score-scgettime)/10f).toDouble()).toInt()

			receiver.drawScoreFont(engine, playerID, 0, 6, "LINE", EventReceiver.COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 0, 7, engine.statistics.lines.toString(), 2f)

			receiver.drawScoreFont(engine, playerID, 0, 9, "SCORE/MIN", EventReceiver.COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 0, 10, String.format("%10g", engine.statistics.spm), 2f)

			receiver.drawScoreFont(engine, playerID, 0, 12, "LINE/MIN", EventReceiver.COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 0, 13, "${engine.statistics.lpm}", 2f)

			receiver.drawScoreFont(engine, playerID, 0, 15, "Time", EventReceiver.COLOR.BLUE)
			val time = maxOf(0, (goaltype+1)*3600-engine.statistics.time)
			receiver.drawScoreNum(engine, playerID, 0, 16, GeneralUtil.getTime(time), getTimeFontColor(time), 2f)
		}

		super.renderLast(engine, playerID)
	}

	/* Calculate score */
	override fun calcScore(engine:GameEngine, playerID:Int, lines:Int):Int {
		// Line clear bonus
		val pts = calcScore(engine, lines)
		val spd = maxOf(0, engine.lockDelay-engine.lockDelayNow)+if(engine.manualLock) 1 else 0
		// Combo
		val cmb = if(engine.combo>=1&&lines>=1) engine.combo-1 else 0
		// Add to score
		if(pts+cmb+spd>0) {
			var get = pts*(10+engine.statistics.level)/10+spd
			if(cmb>=1) {
				var b = sum*(1+cmb)/2
				sum += get
				b = sum*(2+cmb)/2-b
				get = b
			} else
				sum = get
			if(pts>0) lastscore = get
			if(lines>=1) engine.statistics.scoreLine += get
			else engine.statistics.scoreBonus += get
			scgettime += spd
		}
		pow += calcPower(engine, lines)
		return if(pts>0) lastscore else 0
	}

	/* Soft drop */
	override fun afterSoftDropFall(engine:GameEngine, playerID:Int, fall:Int) {
		engine.statistics.scoreSD += fall
	}

	/* Hard drop */
	override fun afterHardDropFall(engine:GameEngine, playerID:Int, fall:Int) {
		engine.statistics.scoreHD += fall*2
	}

	/* Each frame Processing at the end of */
	override fun onLast(engine:GameEngine, playerID:Int) {
		if(engine.gameActive&&engine.timerActive) {
			val limitTime = (goaltype+1)*3600
			val remainTime = (goaltype+1)*3600-engine.statistics.time

			// Time meter
			engine.meterValue = remainTime*receiver.getMeterMax(engine)/limitTime
			engine.meterColor = GameEngine.METER_COLOR_GREEN
			if(remainTime<=30*60) engine.meterColor = GameEngine.METER_COLOR_YELLOW
			if(remainTime<=20*60) engine.meterColor = GameEngine.METER_COLOR_ORANGE
			if(remainTime<=10*60) engine.meterColor = GameEngine.METER_COLOR_RED

			if(!netIsWatch) {
				// Out of time
				if(engine.statistics.time>=limitTime) {
					engine.gameEnded()
					engine.resetStatc()
					engine.stat = GameEngine.Status.ENDINGSTART
					return
				}

				// 10Seconds before the countdown
				if(engine.statistics.time>=limitTime-10*60&&engine.statistics.time%60==0) engine.playSE("countdown")

				// 5Of seconds beforeBGM fadeout
				if(engine.statistics.time>=limitTime-5*60) owner.bgmStatus.fadesw = true

				// 1Per-minuteBackgroundSwitching
				if(engine.statistics.time>0&&engine.statistics.time%3600==0) {
					engine.playSE("levelup")
					owner.backgroundStatus.fadesw = true
					owner.backgroundStatus.fadebg = owner.backgroundStatus.bg+1
				}
			}
		}
	}

	/* Render results screen */
	override fun renderResult(engine:GameEngine, playerID:Int) {
		drawResultStats(engine, playerID, receiver, 0, EventReceiver.COLOR.BLUE, Statistic.SCORE)
		if(rankingRank[0]==0) receiver.drawMenuFont(engine, playerID, 0, 2, "NEW RECORD", EventReceiver.COLOR.ORANGE)
		else if(rankingRank[0]!=-1)
			receiver.drawMenuFont(engine, playerID, 4, 2, String.format("RANK %d", rankingRank[0]+1), EventReceiver.COLOR.ORANGE)

		drawResultStats(engine, playerID, receiver, 3, EventReceiver.COLOR.BLUE, Statistic.LINES)
		if(rankingRank[1]==0) receiver.drawMenuFont(engine, playerID, 0, 5, "NEW RECORD", EventReceiver.COLOR.ORANGE)
		else if(rankingRank[1]!=-1)
			receiver.drawMenuFont(engine, playerID, 4, 5, String.format("RANK %d", rankingRank[1]+1), EventReceiver.COLOR.ORANGE)

		drawResultStats(engine, playerID, receiver, 6, EventReceiver.COLOR.BLUE, Statistic.PIECE, Statistic.SPL, Statistic.SPM,
			Statistic.LPM, Statistic.PPS)

		drawResultNetRank(engine, playerID, receiver, 16, EventReceiver.COLOR.BLUE, netRankingRank[0])
		drawResultNetRankDaily(engine, playerID, receiver, 18, EventReceiver.COLOR.BLUE, netRankingRank[1])

		if(netIsPB) receiver.drawMenuFont(engine, playerID, 2, 21, "NEW PB", EventReceiver.COLOR.ORANGE)

		if(netIsNetPlay&&netReplaySendStatus==1)
			receiver.drawMenuFont(engine, playerID, 0, 22, "SENDING...", EventReceiver.COLOR.PINK)
		else if(netIsNetPlay&&!netIsWatch
			&&netReplaySendStatus==2)
			receiver.drawMenuFont(engine, playerID, 1, 22, "A: RETRY", EventReceiver.COLOR.RED)
	}

	/* Called when saving replay */
	override fun saveReplay(engine:GameEngine, playerID:Int, prop:CustomProperties) {
		savePreset(engine, engine.owner.replayProp, -1)
		engine.owner.replayProp.setProperty("ultra.version", version)

		// NET: Save name
		if(netPlayerName!=null&&netPlayerName!!.isNotEmpty()) prop.setProperty("$playerID.net.netPlayerName", netPlayerName)

		// Update rankings
		if(!owner.replayMode&&!big&&engine.ai==null) {
			val type = goaltype
			updateRanking(type, engine.statistics.score, engine.statistics.lines)

			if(rankingRank[0]!=-1||rankingRank[1]!=-1) {
				saveRanking(engine.ruleopt.strRuleName, type)
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
			for(j in 0 until RANKING_TYPE)
				for(k in 0 until RANKING_MAX) {
					rankingScore[i][j][k] = prop.getProperty("$ruleName.$i.$j.score.$k", 0)
					rankingLines[i][j][k] = prop.getProperty("$ruleName.$i.$j.lines.$k", 0)
					rankingPower[i][j][k] = prop.getProperty("$ruleName.$i.$j.lines.$k", 0)
				}
	}

	/** Save rankings to property file
	 * @param ruleName Rule name
	 * @param type Game Type
	 */
	fun saveRanking(ruleName:String, type:Int) {
		super.saveRanking(ruleName, (0 until RANKING_TYPE).flatMap {j ->
			(0 until RANKING_MAX).flatMap {i ->
				listOf("$ruleName.$type.$i.score" to rankingScore[type][j][i],
					"$ruleName.$type.$i.lines" to rankingLines[type][j][i],
					"$ruleName.$type.$i.power" to rankingPower[type][j][i])
			}
		})
	}

	/** Update rankings
	 * @param sc Score
	 * @param li Lines
	 */
	private fun updateRanking(type:Int, sc:Int, li:Int) {
		for(i in 0 until RANKING_TYPE) {
			rankingRank[i] = checkRanking(sc, li, i)

			if(rankingRank[i]!=-1) {
				// Shift down ranking entries
				for(j in RANKING_MAX-1 downTo rankingRank[i]+1) {
					rankingScore[type][i][j] = rankingScore[type][i][j-1]
					rankingLines[type][i][j] = rankingLines[type][i][j-1]
				}

				// Add new data
				rankingScore[type][i][rankingRank[i]] = sc
				rankingLines[type][i][rankingRank[i]] = li
			}
		}
	}

	/** Calculate ranking position
	 * @param sc Score
	 * @param li Lines
	 * @param rankingtype Number of ranking types
	 * @return Position (-1 if unranked)
	 */
	private fun checkRanking(sc:Int, li:Int, rankingtype:Int):Int {
		for(i in 0 until RANKING_MAX)
			if(rankingtype==0) {
				if(sc>rankingScore[goaltype][rankingtype][i])
					return i
				else if(sc==rankingScore[goaltype][rankingtype][i]&&li>rankingLines[goaltype][rankingtype][i]) return i
			} else if(li>rankingLines[goaltype][rankingtype][i])
				return i
			else if(li==rankingLines[goaltype][rankingtype][i]&&sc>rankingScore[goaltype][rankingtype][i]) return i

		return -1
	}

	/** NET: Send various in-game stats (as well as goaltype)
	 * @param engine GameEngine
	 */
	override fun netSendStats(engine:GameEngine) {
		val bg = if(owner.backgroundStatus.fadesw) owner.backgroundStatus.fadebg else owner.backgroundStatus.bg
		var msg = "game\tstats\t"
		msg += "${engine.statistics.scoreLine}\t${engine.statistics.scoreSD}\t${engine.statistics.scoreHD}\t"
		msg += "${engine.statistics.scoreBonus}\t${engine.statistics.lines}\t"
		msg += "${engine.statistics.totalPieceLocked}\t${engine.statistics.time}\t$goaltype\t"
		msg += "${engine.gameActive}\t${engine.timerActive}\t"
		msg += "$lastscore\t$scgettime\t${engine.lastevent}\t$lastb2b\t$lastcombo\t$lastpiece\t"
		msg += "$bg\n"
		netLobby!!.netPlayerClient!!.send(msg)
	}

	/** NET: Receive various in-game stats (as well as goaltype) */
	override fun netRecvStats(engine:GameEngine, message:Array<String>) {
		listOf<(String)->Unit>({}, {}, {}, {},
			{engine.statistics.scoreLine = it.toInt()},
			{engine.statistics.scoreSD = it.toInt()},
			{engine.statistics.scoreHD = it.toInt()},
			{engine.statistics.scoreBonus = it.toInt()},
			{engine.statistics.lines = it.toInt()},
			{engine.statistics.totalPieceLocked = it.toInt()},
			{engine.statistics.time = it.toInt()},
			{goaltype = it.toInt()},
			{engine.gameActive = it.toBoolean()},
			{engine.timerActive = it.toBoolean()},
			{lastscore = it.toInt()},
			{scgettime = it.toInt()},
			{engine.lastevent = GameEngine.ScoreEvent.parseInt(it)},
			{lastb2b = it.toBoolean()},
			{lastcombo = it.toInt()},
			{lastpiece = it.toInt()},
			{owner.backgroundStatus.bg = it.toInt()}).zip(message).forEach {(x, y) ->
			x(y)
		}

		// Time meter
		val limitTime = (goaltype+1)*3600
		val remainTime = (goaltype+1)*3600-engine.statistics.time
		engine.meterValue = remainTime*receiver.getMeterMax(engine)/limitTime
		engine.meterColor = GameEngine.METER_COLOR_GREEN
		if(remainTime<=30*60) engine.meterColor = GameEngine.METER_COLOR_YELLOW
		if(remainTime<=20*60) engine.meterColor = GameEngine.METER_COLOR_ORANGE
		if(remainTime<=10*60) engine.meterColor = GameEngine.METER_COLOR_RED
	}

	/** NET: Send end-of-game stats
	 * @param engine GameEngine
	 */
	override fun netSendEndGameStats(engine:GameEngine) {
		var subMsg = ""
		subMsg += "SCORE;${engine.statistics.score}\t"
		subMsg += "LINE;${engine.statistics.lines}\t"
		subMsg += "PIECE;${engine.statistics.totalPieceLocked}\t"
		subMsg += "SCORE/LINE;${engine.statistics.spl}\t"
		subMsg += "SCORE/MIN;${engine.statistics.spm}\t"
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
		msg += "${engine.speed.gravity}\t${engine.speed.denominator}\t${engine.speed.are}\t"
		msg += "${engine.speed.areLine}\t${engine.speed.lineDelay}\t${engine.speed.lockDelay}\t"
		msg += "${engine.speed.das}\t$bgmno\t$big\t$goaltype\t$presetNumber\t"
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

	companion object {
		/** Current version */
		private const val CURRENT_VERSION = 1

		/** Number of entries in rankings */
		private const val RANKING_MAX = 5

		/** Number of ranking types */
		private const val RANKING_TYPE = 2

		/** Time limit type */
		private const val GOALTYPE_MAX = 5
	}
}
