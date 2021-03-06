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

import mu.nu.nullpo.game.component.*
import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil

/** SQUARE Mode */
class SquareMode:AbstractMode() {

	private val tableGravityChangeScore = intArrayOf(150, 300, 400, 500, 600, 700, 800, 900, 1000, 1500, 2500, 4000, 5000)

	private val tableGravityValue = intArrayOf(1, 2, 3, 4, 6, 8, 10, 20, 30, 60, 120, 180, 300, -1)

	/** GameManager object (Manages entire game status) */

	/** EventReceiver object (This receives many game events, can also be used
	 * for drawing the fonts.) */

	/** Current gravity number (When the point reaches tableGravityChangeScore's
	 * value, this variable will increase) */
	private var gravityindex:Int = 0

	/** Amount of points you just get from line clears */
	private var lastscore:Int = 0

	/** Elapsed time from last line clear (lastscore is displayed to screen
	 * until this reaches to 120) */
	private var scgettime:Int = 0

	/** Number of squares created */
	private var squares:Int = 0

	/** Selected game type */
	private var gametype:Int = 0

	/** Outline type */
	private var outlinetype:Int = 0

	/** Type of spins allowed (0=off 1=t-only 2=all) */
	private var twistEnableType:Int = 0

	/** Use TNT64 avalanche (native+cascade) */
	private var tntAvalanche:Boolean = false

	/** Grayout broken blocks */
	private var grayoutEnable:Int = 0

	/** Version number */
	private var version:Int = 0

	/** Your place on leaderboard (-1: out of rank) */
	private var rankingRank:Int = 0

	/** Score records */
	private var rankingScore:Array<IntArray> = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}

	/** Time records */
	private var rankingTime:Array<IntArray> = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}

	/** Squares records */
	private var rankingSquares:Array<IntArray> = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}

	/* Returns the name of this mode */
	override val name:String = "SQUARE"
	override val gameIntensity:Int = -1
	/* This function will be called when the game enters the main game
 * screen. */
	override fun playerInit(engine:GameEngine, playerID:Int) {
		super.playerInit(engine, playerID)
		lastscore = 0
		scgettime = 0
		squares = 0

		outlinetype = 0
		twistEnableType = 2
		grayoutEnable = 1

		rankingRank = -1
		rankingScore = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}
		rankingTime = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}
		rankingSquares = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}

		if(!owner.replayMode) {
			loadSetting(owner.modeConfig)
			loadRanking(owner.recordProp, engine.ruleopt.strRuleName)
			version = CURRENT_VERSION
		} else
			loadSetting(owner.replayProp)

		engine.framecolor = GameEngine.FRAME_COLOR_PURPLE
	}

	/** Set the gravity speed
	 * @param engine GameEngine
	 */
	fun setSpeed(engine:GameEngine) {
		if(gametype==0) {
			var speedlv = engine.statistics.score
			if(speedlv<0) speedlv = 0
			if(speedlv>5000) speedlv = 5000

			while(speedlv>=tableGravityChangeScore[gravityindex])
				gravityindex++
			engine.speed.gravity = tableGravityValue[gravityindex]
		} else
			engine.speed.gravity = 1
		engine.speed.denominator = 60
	}

	/* Main routine for game setup screen */
	override fun onSetting(engine:GameEngine, playerID:Int):Boolean {
		// Main menu
		if(!engine.owner.replayMode) {
			// Configuration changes
			val change = updateCursor(engine, 4)

			if(change!=0) {
				engine.playSE("change")

				when(menuCursor) {
					0 -> {
						gametype += change
						if(gametype<0) gametype = GAMETYPE_MAX-1
						if(gametype>GAMETYPE_MAX-1) gametype = 0
					}
					1 -> {
						outlinetype += change
						if(outlinetype<0) outlinetype = 2
						if(outlinetype>2) outlinetype = 0
					}
					2 -> {
						twistEnableType += change
						if(twistEnableType<0) twistEnableType = 2
						if(twistEnableType>2) twistEnableType = 0
					}
					3 -> tntAvalanche = !tntAvalanche
					4 -> {
						grayoutEnable += change
						if(grayoutEnable<0) grayoutEnable = 2
						if(grayoutEnable>2) grayoutEnable = 0
					}
				}
			}

			// A button (confirm)
			if(engine.ctrl.isPush(Controller.BUTTON_A)&&menuTime>=5) {
				engine.playSE("decide")
				saveSetting(owner.modeConfig)
				owner.saveModeConfig()
				return false
			}

			// B button (cancel)
			if(engine.ctrl.isPush(Controller.BUTTON_B)) engine.quitflag = true

			menuTime++
		} else {
			menuTime++
			menuCursor = -1

			return menuTime<60
		}

		return true
	}

	/* Renders game setup screen */
	override fun renderSetting(engine:GameEngine, playerID:Int) {
		var strOutline = ""
		if(outlinetype==0) strOutline = "NORMAL"
		if(outlinetype==1) strOutline = "CONNECT"
		if(outlinetype==2) strOutline = "NONE"
		var grayoutStr = ""
		if(grayoutEnable==0) grayoutStr = "OFF"
		if(grayoutEnable==1) grayoutStr = "SPIN ONLY"
		if(grayoutEnable==2) grayoutStr = "ALL"
		drawMenu(engine, playerID, receiver, 0, EventReceiver.COLOR.BLUE, 0, "GAME TYPE", GAMETYPE_NAME[gametype],
			"OUTLINE", strOutline,
			"SPIN BONUS", if(twistEnableType==0) "OFF" else if(twistEnableType==1) "T-ONLY" else "ALL",
			"AVALANCHE", if(tntAvalanche) "TNT" else "WORLDS",
			"GRAYOUT", grayoutStr)
	}

	/* This function will be called before the game actually begins (after
 * Ready&Go screen disappears) */
	override fun startGame(engine:GameEngine, playerID:Int) {
		engine.comboType = GameEngine.COMBO_TYPE_DISABLE

		if(outlinetype==0) engine.blockOutlineType = GameEngine.BLOCK_OUTLINE_NORMAL
		if(outlinetype==1) engine.blockOutlineType = GameEngine.BLOCK_OUTLINE_CONNECT
		if(outlinetype==2) engine.blockOutlineType = GameEngine.BLOCK_OUTLINE_NONE

		if(twistEnableType==0)
			engine.twistEnable = false
		else if(twistEnableType==1)
			engine.twistEnable = true
		else {
			engine.twistEnable = true
			engine.useAllSpinBonus = true
		}

		engine.speed.are = 30
		engine.speed.areLine = 30
		engine.speed.das = 10
		engine.speed.lockDelay = 30

		setSpeed(engine)
	}

	/* Piece movement */
	override fun onMove(engine:GameEngine, playerID:Int):Boolean {
		// Disable cascade
		engine.lineGravityType = GameEngine.LineGravity.NATIVE
		return false
	}

	/* Renders HUD (leaderboard or game statistics) */
	override fun renderLast(engine:GameEngine, playerID:Int) {
		receiver.drawScoreFont(engine, playerID, 0, 0, "SQUARE (${GAMETYPE_NAME[gametype]})", EventReceiver.COLOR.COBALT)

		if(engine.stat==GameEngine.Status.SETTING||engine.stat==GameEngine.Status.RESULT&&!owner.replayMode) {
			if(!owner.replayMode&&engine.ai==null) {
				val scale = if(receiver.nextDisplayType==2&&gametype==0) .5f else 1f
				val topY = if(receiver.nextDisplayType==2&&gametype==0) 6 else 4

				when(gametype) {
					0 -> receiver.drawScoreFont(engine, playerID, 3, topY-1, "SCORE SQUARE TIME", EventReceiver.COLOR.BLUE, scale)
					1 -> receiver.drawScoreFont(engine, playerID, 3, 3, "SCORE SQUARE", EventReceiver.COLOR.BLUE)
					2 -> receiver.drawScoreFont(engine, playerID, 3, 3, "TIME     SQUARE", EventReceiver.COLOR.BLUE)
				}

				for(i in 0 until RANKING_MAX) {
					receiver.drawScoreGrade(engine, playerID, 0, topY+i, String.format("%2d", i+1), EventReceiver.COLOR.YELLOW, scale)
					when(gametype) {
						0 -> {
							receiver.drawScoreFont(engine, playerID, 3, topY+i, "${rankingScore[gametype][i]}", i==rankingRank, scale)
							receiver.drawScoreFont(engine, playerID, 9, topY+i, "${rankingSquares[gametype][i]}", i==rankingRank, scale)
							receiver.drawScoreFont(engine, playerID, 16, topY+i, GeneralUtil.getTime(rankingTime[gametype][i]),
								i==rankingRank, scale)
						}
						1 -> {
							receiver.drawScoreFont(engine, playerID, 3, 4+i, "${rankingScore[gametype][i]}", i==rankingRank)
							receiver.drawScoreFont(engine, playerID, 9, 4+i, "${rankingSquares[gametype][i]}", i==rankingRank)
						}
						2 -> {
							receiver.drawScoreFont(engine, playerID, 3, 4+i, GeneralUtil.getTime(rankingTime[gametype][i]), i==rankingRank)
							receiver.drawScoreFont(engine, playerID, 12, 4+i, "${rankingSquares[gametype][i]}", i==rankingRank)
						}
					}
				}
			}
		} else {
			receiver.drawScoreFont(engine, playerID, 0, 3, "Score", EventReceiver.COLOR.BLUE)
			receiver.drawScoreFont(engine, playerID, 0, 4, "${engine.statistics.score}${
				if(lastscore==0||scgettime<=0)
					"(+$lastscore)" else ""
			}")

			receiver.drawScoreFont(engine, playerID, 0, 6, "LINE", EventReceiver.COLOR.BLUE)
			receiver.drawScoreFont(engine, playerID, 0, 7, "${engine.statistics.lines}")

			receiver.drawScoreFont(engine, playerID, 0, 9, "SQUARE", EventReceiver.COLOR.BLUE)
			receiver.drawScoreFont(engine, playerID, 0, 10, "$squares")

			receiver.drawScoreFont(engine, playerID, 0, 12, "Time", EventReceiver.COLOR.BLUE)
			if(gametype==1) {
				// Ultra timer
				var time = ULTRA_MAX_TIME-engine.statistics.time
				if(time<0) time = 0
				receiver.drawScoreFont(engine, playerID, 0, 13, GeneralUtil.getTime(time), getTimeFontColor(time))
			} else
			// Normal timer
				receiver.drawScoreFont(engine, playerID, 0, 13, GeneralUtil.getTime(engine.statistics.time))
		}
	}

	/* This function will be called when the game timer updates */
	override fun onLast(engine:GameEngine, playerID:Int) {
		if(scgettime>0) scgettime--

		if(gametype==1) {
			val remainTime = ULTRA_MAX_TIME-engine.statistics.time
			// Timer meter
			engine.meterValue = remainTime*receiver.getMeterMax(engine)/ULTRA_MAX_TIME
			engine.meterColor = GameEngine.METER_COLOR_GREEN
			if(remainTime<=3600) engine.meterColor = GameEngine.METER_COLOR_YELLOW
			if(remainTime<=1800) engine.meterColor = GameEngine.METER_COLOR_ORANGE
			if(remainTime<=600) engine.meterColor = GameEngine.METER_COLOR_RED

			// Countdown
			if(remainTime>0&&remainTime<=10*60&&engine.statistics.time%60==0
				&&engine.timerActive)
				engine.playSE("countdown")

			// BGM fadeout
			if(remainTime<=5*60&&engine.timerActive) owner.bgmStatus.fadesw = true

			// Time up!
			if(engine.statistics.time>=ULTRA_MAX_TIME&&engine.timerActive) {
				engine.gameEnded()
				engine.resetStatc()
				engine.stat = GameEngine.Status.ENDINGSTART
				return
			}
		} else if(gametype==2) {
			var remainScore = SPRINT_MAX_SCORE-engine.statistics.score
			if(!engine.timerActive) remainScore = 0
			engine.meterValue = remainScore*receiver.getMeterMax(engine)/SPRINT_MAX_SCORE
			engine.meterColor = GameEngine.METER_COLOR_GREEN
			if(remainScore<=50) engine.meterColor = GameEngine.METER_COLOR_YELLOW
			if(remainScore<=30) engine.meterColor = GameEngine.METER_COLOR_ORANGE
			if(remainScore<=10) engine.meterColor = GameEngine.METER_COLOR_RED

			// Goal
			if(engine.statistics.score>=SPRINT_MAX_SCORE&&engine.timerActive) {
				engine.gameEnded()
				engine.resetStatc()
				engine.stat = GameEngine.Status.ENDINGSTART
			}
		}
	}

	/* Line clear */
	override fun onLineClear(engine:GameEngine, playerID:Int):Boolean {
		if(engine.statc[0]==1) if(grayoutEnable==2) grayoutBrokenBlocks(engine.field!!)
		return false
	}

	/** Make all broken blocks gray.
	 * @param field Field
	 */
	private fun grayoutBrokenBlocks(field:Field) {
		for(i in field.hiddenHeight*-1 until field.heightWithoutHurryupFloor)
			for(j in 0 until field.width) {
				val blk = field.getBlock(j, i)
				if(blk!=null&&!blk.isEmpty&&blk.getAttribute(Block.ATTRIBUTE.BROKEN))
					blk.cint = Block.BLOCK_COLOR_GRAY
			}
	}

	/* Calculates line-clear score
 * (This function will be called even if no lines are cleared) */
	override fun calcScore(engine:GameEngine, playerID:Int, lines:Int):Int {
		if(lines>0&&engine.twist) {
			avalanche(engine, playerID, lines)
			return 0
		}

		// Line clear bonus
		var pts = lines

		if(lines>0) {
			engine.lineGravityType = GameEngine.LineGravity.NATIVE
			if(engine.field!!.isEmpty) engine.playSE("bravo")

			if(lines>3) pts = 3+(lines-3)*2

			val squareClears = engine.field!!.howManySquareClears
			pts += 10*squareClears[0]+5*squareClears[1]

			lastscore = pts
			scgettime = 120
			engine.statistics.scoreLine += pts
			setSpeed(engine)
			return pts
		}
		return 0
	}

	/** Spin avalanche routine.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @param lines Number of lines cleared
	 */
	private fun avalanche(engine:GameEngine, playerID:Int, lines:Int) {
		val field = engine.field
		field!!.setAllAttribute(false, Block.ATTRIBUTE.ANTIGRAVITY)

		val hiddenHeight = field.hiddenHeight
		val height = field.height
		val affectY = BooleanArray(height+hiddenHeight)
		for(i in affectY.indices)
			affectY[i] = false
		val minY = engine.nowPieceObject!!.minimumBlockY+engine.nowPieceY
		if(field.getLineFlag(minY))
			for(i in minY+hiddenHeight downTo 0)
				affectY[i] = true

		var testY = minY+1

		while(!field.getLineFlag(testY)&&testY<height)
			testY++
		for(y in testY+hiddenHeight until affectY.size)
			affectY[y] = true

		for(y in hiddenHeight*-1 until height)
			if(affectY[y+hiddenHeight])
				for(x in 0 until field.width) {
					val blk = field.getBlock(x, y)
					if(blk!=null&&!blk.isEmpty) {
						// Change each affected block to broken and garbage, and break connections.
						blk.setAttribute(true, Block.ATTRIBUTE.GARBAGE)
						blk.setAttribute(true, Block.ATTRIBUTE.BROKEN)
						blk.setAttribute(false, Block.ATTRIBUTE.CONNECT_UP)
						blk.setAttribute(false, Block.ATTRIBUTE.CONNECT_DOWN)
						blk.setAttribute(false, Block.ATTRIBUTE.CONNECT_LEFT)
						blk.setAttribute(false, Block.ATTRIBUTE.CONNECT_RIGHT)
						if(grayoutEnable!=0) blk.cint = Block.BLOCK_COLOR_GRAY
					}
				}
			else if(tntAvalanche)
			// Set anti-gravity when TNT avalanche is used
				for(x in 0 until field.width) {
					field.getBlock(x, y)?.also {
						if(!it.isEmpty) it.setAttribute(true, Block.ATTRIBUTE.ANTIGRAVITY)
					}
					field.getBlock(x, y-1)?.also {
						if(!it.isEmpty) it.setAttribute(true, Block.ATTRIBUTE.ANTIGRAVITY)
					}
				}
		// Reset line flags
		for(y in -1*hiddenHeight until height)
			engine.field!!.setLineFlag(y, false)
		// Set cascade flag
		engine.lineGravityType = GameEngine.LineGravity.CASCADE
	}

	/* When the line clear ends */
	override fun lineClearEnd(engine:GameEngine, playerID:Int):Boolean {
		if(engine.lineGravityType==GameEngine.LineGravity.CASCADE&&engine.lineGravityTotalLines>0&&tntAvalanche) {
			val field = engine.field
			for(i in field!!.heightWithoutHurryupFloor-1 downTo field.hiddenHeight*-1)
				if(field.isEmptyLine(i)) {
					field.cutLine(i, 1)
					engine.lineGravityTotalLines--
					return true
				}
		}
		return false
	}

	/* Check for squares when piece locks */
	override fun pieceLocked(engine:GameEngine, playerID:Int, lines:Int) {
		val sq = engine.field!!.checkForSquares()
		squares += sq[0]+sq[1]
		if(sq[0]==0&&sq[1]>0)
			engine.playSE("square_s")
		else if(sq[0]>0) engine.playSE("square_g")
	}

	/* Results screen */
	override fun renderResult(engine:GameEngine, playerID:Int) {
		receiver.drawMenuFont(engine, playerID, 0, 1, "PLAY DATA", EventReceiver.COLOR.ORANGE)

		drawResult(engine, playerID, receiver, 3, EventReceiver.COLOR.BLUE, "Score", String.format("%10d", engine.statistics.score),
			"LINE", String.format("%10d", engine.statistics.lines), "SQUARE", String.format("%10d", squares), "Time",
			String.format("%10s", GeneralUtil.getTime(engine.statistics.time)))
		drawResultRank(engine, playerID, receiver, 11, EventReceiver.COLOR.BLUE, rankingRank)
	}

	/* This function will be called when the replay data is going to be saved */
	override fun saveReplay(engine:GameEngine, playerID:Int, prop:CustomProperties) {
		saveSetting(prop)
		prop.setProperty("square.squares", squares)

		// Update the ranking
		if(!owner.replayMode&&engine.ai==null) {
			updateRanking(engine.statistics.score, engine.statistics.time, squares, gametype)

			if(rankingRank!=-1) {
				saveRanking(owner.recordProp, engine.ruleopt.strRuleName)
				owner.saveModeConfig()
			}
		}
	}

	/** Load the settings from CustomProperties
	 * @param prop CustomProperties to read
	 */
	override fun loadSetting(prop:CustomProperties) {
		gametype = prop.getProperty("square.gametype", 0)
		outlinetype = prop.getProperty("square.outlinetype", 0)
		twistEnableType = prop.getProperty("square.twistEnableType", 2)
		tntAvalanche = prop.getProperty("square.tntAvalanche", false)
		grayoutEnable = if(version==0)
			if(prop.getProperty("square.grayoutEnable", false)) 2 else 0
		else
			prop.getProperty("square.grayoutEnable", 2)
		version = prop.getProperty("square.version", 0)
	}

	/** Save the settings to CustomProperties
	 * @param prop CustomProperties to write
	 */
	override fun saveSetting(prop:CustomProperties) {
		prop.setProperty("square.gametype", gametype)
		prop.setProperty("square.outlinetype", outlinetype)
		prop.setProperty("square.twistEnableType", twistEnableType)
		prop.setProperty("square.tntAvalanche", tntAvalanche)
		prop.setProperty("square.grayoutEnable", grayoutEnable)
		prop.setProperty("square.version", version)
	}

	/** Load the ranking from CustomProperties
	 * @param prop CustomProperties to read
	 * @param ruleName Rule name
	 */
	override fun loadRanking(prop:CustomProperties, ruleName:String) {
		for(i in 0 until RANKING_MAX)
			for(j in 0 until GAMETYPE_MAX) {
				rankingScore[j][i] = prop.getProperty("square.ranking.$ruleName.$j.score.$i", 0)
				rankingTime[j][i] = prop.getProperty("square.ranking.$ruleName.$j.time.$i", -1)
				rankingSquares[j][i] = prop.getProperty("square.ranking.$ruleName.$j.squares.$i", 0)
			}
	}

	/** Save the ranking to CustomProperties
	 * @param prop CustomProperties to write
	 * @param ruleName Rule name
	 */
	fun saveRanking(prop:CustomProperties, ruleName:String) {
		for(i in 0 until RANKING_MAX)
			for(j in 0 until GAMETYPE_MAX) {
				prop.setProperty("square.ranking.$ruleName.$j.score.$i", rankingScore[j][i])
				prop.setProperty("square.ranking.$ruleName.$j.time.$i", rankingTime[j][i])
				prop.setProperty("square.ranking.$ruleName.$j.squares.$i", rankingSquares[j][i])
			}
	}

	/** Update the ranking
	 * @param sc Score
	 * @param time Time
	 * @param sq Squares
	 * @param type GameType
	 */
	private fun updateRanking(sc:Int, time:Int, sq:Int, type:Int) {
		rankingRank = checkRanking(sc, time, sq, type)

		if(rankingRank!=-1) {
			// Shift the old records
			for(i in RANKING_MAX-1 downTo rankingRank+1) {
				rankingScore[type][i] = rankingScore[type][i-1]
				rankingTime[type][i] = rankingTime[type][i-1]
				rankingSquares[type][i] = rankingSquares[type][i-1]
			}

			// Register new record
			rankingScore[type][rankingRank] = sc
			rankingTime[type][rankingRank] = time
			rankingSquares[type][rankingRank] = sq
		}
	}

	/** This function will check the ranking and returns which place you are.
	 * (-1: Out of rank)
	 * @param sc Score
	 * @param time Time
	 * @param sq Squares
	 * @param type GameType
	 * @return Place (-1: Out of rank)
	 */
	private fun checkRanking(sc:Int, time:Int, sq:Int, type:Int):Int {
		for(i in 0 until RANKING_MAX)
			if(gametype==0) {
				// Marathon
				if(sc>rankingScore[type][i])
					return i
				else if(sc==rankingScore[type][i]&&sq>rankingSquares[type][i])
					return i
				else if(sc==rankingScore[type][i]&&sq==rankingSquares[type][i]&&time<rankingTime[type][i]) return i
			} else if(gametype==1&&time>=ULTRA_MAX_TIME) {
				// Ultra
				if(sc>rankingScore[type][i])
					return i
				else if(sc==rankingScore[type][i]&&sq>rankingSquares[type][i]) return i
			} else if(gametype==2&&sc>=SPRINT_MAX_SCORE)
			// Sprint
				if(time<rankingTime[type][i]||rankingTime[type][i]<0)
					return i
				else if(time==rankingTime[type][i]&&sq>rankingSquares[type][i]) return i

		return -1
	}

	companion object {
		/** Current version */
		private const val CURRENT_VERSION = 1

		/** Number of ranking records */
		private const val RANKING_MAX = 10

		/** Number of ranking types */
		private const val RANKING_TYPE = 3

		/** Name of game types */
		private val GAMETYPE_NAME = arrayOf("MARATHON", "ULTRA", "SPRINT")

		/** Number of game types */
		private const val GAMETYPE_MAX = 3

		/** Max time in Ultra */
		private const val ULTRA_MAX_TIME = 10800

		/** Max score in Sprint */
		private const val SPRINT_MAX_SCORE = 150
	}
}
