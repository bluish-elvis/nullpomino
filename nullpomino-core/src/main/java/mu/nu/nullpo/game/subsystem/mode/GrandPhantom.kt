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
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil
import kotlin.math.ceil

/** PHANTOM MANIA mode (Original from NullpoUE build 121909 by Zircean) */
class GrandPhantom:AbstractMode() {

	/** Next section level */
	private var nextseclv:Int = 0

	/** Level up flag (Set to true when the level increases) */
	private var lvupflag:Boolean = false

	/** Current grade */
	private var grade:Int = 0

	/** Remaining frames of flash effect of grade display */
	private var gradeflash:Int = 0

	/** Used by combo scoring */
	private var comboValue:Int = 0

	/** Amount of points you just get from line clears */
	private var lastscore:Int = 0

	/** Elapsed time from last line clear
	 * (lastscore is displayed to screen until this reaches to 120) */
	private var scgettime:Int = 0

	/** Secret Grade */
	private var secretGrade:Int = 0

	/** Remaining ending time limit */
	private var rolltime:Int = 0

	/** 0:Died before ending, 1:Died during ending, 2:Completed ending */
	private var rollclear:Int = 0

	/** True if ending has started */
	private var rollstarted:Boolean = false

	/** Current BGM */
	private var bgmlv:Int = 0

	/** Section Time */
	private var sectionTime:IntArray = IntArray(SECTION_MAX)

	/** This will be true if the player achieves
	 * new section time record in specific section */
	private var sectionIsNewRecord:BooleanArray = BooleanArray(SECTION_MAX)

	/** Amount of sections completed */
	private var sectionscomp:Int = 0

	/** Average section time */
	private var sectionavgtime:Int = 0

	/** Current section time */
	private var sectionlasttime:Int = 0

	/** Number of 4-Line clears in current section */
	private var sectionfourline:Int = 0

	/** Set to true by default, set to false when sectionfourline is below 2 */
	private var gmfourline:Boolean = false

	/** AC medal (0:None, 1:Bronze, 2:Silver, 3:Gold) */
	private var medalAC:Int = 0

	/** ST medal */
	private var medalST:Int = 0

	/** SK medal */
	private var medalSK:Int = 0

	/** RE medal */
	private var medalRE:Int = 0

	/** RO medal */
	private var medalRO:Int = 0

	/** CO medal */
	private var medalCO:Int = 0

	/** Used by RE medal */
	private var recoveryFlag:Boolean = false

	/** Total rotations */
	private var rotateCount:Int = 0

	/** false:Leaderboard, true:Section time record
	 * (Push F in settings screen to flip it) */
	private var isShowBestSectionTime:Boolean = false

	/** Selected start level */
	private var startlevel:Int = 0

	/** Enable/Disable level stop sfx */
	private var lvstopse:Boolean = false

	/** Big mode */
	private var big:Boolean = false

	/** Show section time */
	private var showsectiontime:Boolean = false

	/** Version of this mode */
	private var version:Int = 0

	/** Your place on leaderboard (-1: out of rank) */
	private var rankingRank:Int = 0

	/** Grade records */
	private var rankingGrade:IntArray = IntArray(RANKING_MAX)

	/** Level records */
	private var rankingLevel:IntArray = IntArray(RANKING_MAX)

	/** Time records */
	private var rankingTime:IntArray = IntArray(RANKING_MAX)

	/** Roll-Cleared records */
	private var rankingRollclear:IntArray = IntArray(RANKING_MAX)

	/** Best section time records */
	private var bestSectionTime:IntArray = IntArray(SECTION_MAX)

	/** Returns the name of this mode */
	override val name:String = "Grand Phantom"
	override val gameIntensity:Int = 3
	/** This function will be called when the game enters
	 * the main game screen. */
	override fun playerInit(engine:GameEngine, playerID:Int) {
		super.playerInit(engine, playerID)
		menuTime = 0
		nextseclv = 0
		lvupflag = true
		grade = 0
		gradeflash = 0
		comboValue = 0
		lastscore = 0
		scgettime = 0
		rolltime = 0
		rollclear = 0
		rollstarted = false
		bgmlv = 0
		sectionTime = IntArray(SECTION_MAX)
		sectionIsNewRecord = BooleanArray(SECTION_MAX)
		sectionavgtime = 0
		sectionlasttime = 0
		sectionfourline = 0
		gmfourline = true
		medalAC = 0
		medalST = 0
		medalSK = 0
		medalRE = 0
		medalRO = 0
		medalCO = 0
		recoveryFlag = false
		rotateCount = 0
		isShowBestSectionTime = false
		startlevel = 0
		lvstopse = false
		big = false
		showsectiontime = true

		rankingRank = -1
		rankingGrade = IntArray(RANKING_MAX)
		rankingLevel = IntArray(RANKING_MAX)
		rankingTime = IntArray(RANKING_MAX)
		rankingRollclear = IntArray(RANKING_MAX)
		bestSectionTime = IntArray(SECTION_MAX)

		engine.twistEnable = false
		engine.b2bEnable = false
		engine.splitb2b = false
		engine.comboType = GameEngine.COMBO_TYPE_DOUBLE
		engine.framecolor = GameEngine.FRAME_COLOR_CYAN
		engine.bighalf = true
		engine.bigmove = true
		engine.staffrollEnable = true
		engine.staffrollNoDeath = false

		if(!owner.replayMode) {
			loadSetting(owner.modeConfig)
			loadRanking(owner.recordProp, engine.ruleopt.strRuleName)
			version = CURRENT_VERSION
		} else {
			for(i in 0 until SECTION_MAX)
				bestSectionTime[i] = DEFAULT_SECTION_TIME
			loadSetting(owner.replayProp)
			version = owner.replayProp.getProperty("phantommania.version", 0)
		}

		owner.backgroundStatus.bg = startlevel
	}

	/** Load the settings */
	override fun loadSetting(prop:CustomProperties) {
		startlevel = prop.getProperty("phantommania.startlevel", 0)
		lvstopse = prop.getProperty("phantommania.lvstopse", true)
		showsectiontime = prop.getProperty("phantommania.showsectiontime", true)
		big = prop.getProperty("phantommania.big", false)
	}

	/** Save the settings */
	override fun saveSetting(prop:CustomProperties) {
		prop.setProperty("phantommania.startlevel", startlevel)
		prop.setProperty("phantommania.lvstopse", lvstopse)
		prop.setProperty("phantommania.showsectiontime", showsectiontime)
		prop.setProperty("phantommania.big", big)
	}

	/** Set the starting bgmlv */
	private fun setStartBgmlv(engine:GameEngine) {
		bgmlv = 0
		while(tableBGMChange[bgmlv]!=-1&&engine.statistics.level>=tableBGMChange[bgmlv])
			bgmlv++
	}

	/** Set the gravity speed
	 * @param engine GameEngine object
	 */
	private fun setSpeed(engine:GameEngine) {
		engine.speed.gravity = -1

		var section = engine.statistics.level/100
		if(section>tableARE.size-1) section = tableARE.size-1
		engine.speed.are = tableARE[section]
		engine.speed.areLine = tableARELine[section]
		engine.speed.lineDelay = tableLineDelay[section]
		engine.speed.lockDelay = tableLockDelay[section]
		engine.speed.das = tableDAS[section]

		engine.blockHidden = tableHiddenDelay[section]
		if(engine.blockHidden<engine.ruleopt.lockflash) engine.blockHidden = engine.ruleopt.lockflash
	}

	/** Set Pressure Hidden params
	 * @param engine GameEngine
	 */
	private fun setHeboHidden(engine:GameEngine) {
		var section = engine.statistics.level/100
		if(section>tableARE.size-1) section = tableARE.size-1
		if(section==1) {
			engine.heboHiddenYLimit = 15
			engine.heboHiddenTimerMax = (engine.heboHiddenYNow+2)*120
		}
		if(section==2) {
			engine.heboHiddenYLimit = 17
			engine.heboHiddenTimerMax = (engine.heboHiddenYNow+1)*90
		}
		if(section==3) {
			engine.heboHiddenYLimit = 19
			engine.heboHiddenTimerMax = engine.heboHiddenYNow*60+60
		}
		if(section==4) {
			engine.heboHiddenYLimit = 19
			engine.heboHiddenTimerMax = engine.heboHiddenYNow*45+45
		}
		if(section>=5) {
			engine.heboHiddenYLimit = 19
			engine.heboHiddenTimerMax = engine.heboHiddenYNow*30+30
		}
	}

	/** Calculates average section time */
	private fun setAverageSectionTime() {
		if(sectionscomp>0) {
			var temp = 0
			for(i in startlevel until startlevel+sectionscomp)
				temp += sectionTime[i]
			sectionavgtime = temp/sectionscomp
		} else
			sectionavgtime = 0
	}

	/** Checks ST medal
	 * @param engine GameEngine
	 * @param sectionNumber Section Number
	 */
	private fun stMedalCheck(engine:GameEngine, sectionNumber:Int) {
		val best = bestSectionTime[sectionNumber]

		if(sectionlasttime<best) {
			if(medalST<3) {
				engine.playSE("medal3")
				medalST = 3
			}
			if(!owner.replayMode) sectionIsNewRecord[sectionNumber] = true
		} else if(sectionlasttime<best+300&&medalST<2) {
			engine.playSE("medal2")
			medalST = 2
		} else if(sectionlasttime<best+600&&medalST<1) {
			engine.playSE("medal1")
			medalST = 1
		}
	}

	/** Checks RO medal */
	private fun roMedalCheck(engine:GameEngine) {
		val rotateAverage = rotateCount.toFloat()/engine.statistics.totalPieceLocked.toFloat()

		if(rotateAverage>=1.2f&&medalRO<3) {
			engine.playSE("medal${++medalRO}")
		}
	}

	/** Main routine for game setup screen */
	override fun onSetting(engine:GameEngine, playerID:Int):Boolean {
		if(!engine.owner.replayMode) {
			// Configuration changes
			val change = updateCursor(engine, 3)

			if(change!=0) {
				engine.playSE("change")

				when(menuCursor) {
					0 -> {
						startlevel += change
						if(startlevel<0) startlevel = 9
						if(startlevel>9) startlevel = 0
						owner.backgroundStatus.bg = startlevel
					}
					1 -> lvstopse = !lvstopse
					2 -> showsectiontime = !showsectiontime
					3 -> big = !big
				}
			}

			// Check for F button, when pressed this will flip Leaderboard/Best Section Time Records
			if(engine.ctrl.isPush(Controller.BUTTON_F)&&menuTime>=5) {
				engine.playSE("change")
				isShowBestSectionTime = !isShowBestSectionTime
			}

			// Check for A button, when pressed this will begin the game
			if(engine.ctrl.isPush(Controller.BUTTON_A)&&menuTime>=5) {
				engine.playSE("decide")
				saveSetting(owner.modeConfig)
				owner.saveModeConfig()
				isShowBestSectionTime = false
				sectionscomp = 0
				return false
			}

			// Check for B button, when pressed this will shutdown the game engine.
			if(engine.ctrl.isPush(Controller.BUTTON_B)) engine.quitflag = true

			menuTime++
		} else {
			menuTime++
			menuCursor = -1

			return menuTime<60
		}

		return true
	}

	/** Renders game setup screen */
	override fun renderSetting(engine:GameEngine, playerID:Int) {
		drawMenu(engine, playerID, receiver, 0, COLOR.PURPLE, 0, "Level", (startlevel*100).toString(), "LVSTOPSE",
			GeneralUtil.getONorOFF(lvstopse), "SHOW STIME", GeneralUtil.getONorOFF(showsectiontime), "BIG",
			GeneralUtil.getONorOFF(big))
	}

	/** This function will be called before the game actually begins (after
	 * Ready&Go screen disappears) */
	override fun startGame(engine:GameEngine, playerID:Int) {
		engine.statistics.level = startlevel*100

		nextseclv = engine.statistics.level+100
		if(engine.statistics.level<0) nextseclv = 100
		if(engine.statistics.level>=900) nextseclv = 999

		owner.backgroundStatus.bg = engine.statistics.level/100

		engine.big = big
		engine.heboHiddenEnable = true

		setSpeed(engine)
		setStartBgmlv(engine)
		owner.bgmStatus.bgm = tableBGM[bgmlv]
	}

	/** Renders HUD (leaderboard or game statistics) */
	override fun renderLast(engine:GameEngine, playerID:Int) {
		receiver.drawScoreFont(engine, playerID, 0, 0, "PHANTOM MANIA", COLOR.WHITE)

		if(engine.stat==GameEngine.Status.SETTING||engine.stat==GameEngine.Status.RESULT&&!owner.replayMode) {
			if(!owner.replayMode&&startlevel==0&&!big&&engine.ai==null)
				if(!isShowBestSectionTime) {
					// Leaderboard
					val topY = if(receiver.nextDisplayType==2) 5 else 3
					receiver.drawScoreFont(engine, playerID, 3, topY-1, "GRADE LEVEL TIME", COLOR.PURPLE)

					for(i in 0 until RANKING_MAX) {
						var gcolor = COLOR.WHITE
						if(rankingRollclear[i]==1) gcolor = COLOR.GREEN
						if(rankingRollclear[i]==2) gcolor = COLOR.ORANGE
						receiver.drawScoreGrade(engine, playerID, 0, topY+i, String.format("%2d", i+1),
							if(rankingRank==i) COLOR.RAINBOW else COLOR.YELLOW)
						if(rankingGrade[i]>=0&&rankingGrade[i]<tableGradeName.size)
							receiver.drawScoreFont(engine, playerID, 3, topY+i, tableGradeName[rankingGrade[i]], gcolor)
						receiver.drawScoreNum(engine, playerID, 9, topY+i, String.format("%03d", rankingLevel[i]), i==rankingRank)
						receiver.drawScoreNum(engine, playerID, 15, topY+i, GeneralUtil.getTime(rankingTime[i]), i==rankingRank)
					}

					receiver.drawScoreFont(engine, playerID, 0, 17, "F:VIEW SECTION TIME", COLOR.GREEN)
				} else {
					// Best section time records
					receiver.drawScoreFont(engine, playerID, 0, 2, "SECTION TIME", COLOR.PURPLE)

					var totalTime = 0
					for(i in 0 until SECTION_MAX) {
						val temp = minOf(i*100, 999)
						val temp2 = minOf((i+1)*100-1, 999)

						val strSectionTime:String = String.format("%3d-%3d %s", temp, temp2, GeneralUtil.getTime(bestSectionTime[i]))

						receiver.drawScoreNum(engine, playerID, 0, 3+i, strSectionTime, sectionIsNewRecord[i])

						totalTime += bestSectionTime[i]
					}
					receiver.drawScoreFont(engine, playerID, 0, 17, "TOTAL", COLOR.PURPLE)
					receiver.drawScoreNum(engine, playerID, 0, 18, GeneralUtil.getTime(totalTime), 2f)
					receiver.drawScoreFont(engine, playerID, 9, 17, "AVERAGE", COLOR.PURPLE)
					receiver.drawScoreNum(engine, playerID, 9, 18, GeneralUtil.getTime((totalTime/SECTION_MAX)), 2f)

					receiver.drawScoreFont(engine, playerID, 0, 17, "F:VIEW RANKING", COLOR.GREEN)
				}
		} else {
			if(grade>=0&&grade<tableGradeName.size)
				receiver.drawScoreFont(engine, playerID, 0, 2, tableGradeName[grade], gradeflash>0&&gradeflash%4==0, 2f)

			// Score
			receiver.drawScoreFont(engine, playerID, 0, 5, "Score", COLOR.PURPLE)
			receiver.drawScoreNum(engine, playerID, 0, 6, "$scgettime"+"\n"+lastscore)
			if(scgettime<engine.statistics.score) scgettime += ceil(((engine.statistics.score-scgettime)/10f).toDouble()).toInt()

			receiver.drawScoreFont(engine, playerID, 0, 9, "Level", COLOR.PURPLE)
			receiver.drawScoreNum(engine, playerID, 1, 10, String.format("%3d", maxOf(engine.statistics.level, 0)))
			receiver.drawSpeedMeter(engine, playerID, 0, 11, if(engine.speed.gravity<0) 40 else engine.speed.gravity/128, 4)
			receiver.drawScoreNum(engine, playerID, 1, 12, String.format("%3d", nextseclv))

			receiver.drawScoreFont(engine, playerID, 0, 14, "Time", COLOR.PURPLE)
			receiver.drawScoreNum(engine, playerID, 0, 15, GeneralUtil.getTime(engine.statistics.time), 2f)

			if(engine.gameActive&&engine.ending==2) {
				var time = ROLLTIMELIMIT-rolltime
				if(time<0) time = 0
				receiver.drawScoreFont(engine, playerID, 0, 17, "ROLL TIME", COLOR.PURPLE)
				receiver.drawScoreNum(engine, playerID, 0, 18, GeneralUtil.getTime(time), time>0&&time<10*60, 2f)
			}

			receiver.drawScoreMedal(engine, playerID, 0, 20, "AC", medalAC)
			receiver.drawScoreMedal(engine, playerID, 3, 20, "ST", medalST)
			receiver.drawScoreMedal(engine, playerID, 0, 21, "SK", medalSK)
			receiver.drawScoreMedal(engine, playerID, 3, 21, "RE", medalRE)
			receiver.drawScoreMedal(engine, playerID, 0, 22, "RO", medalRO)
			receiver.drawScoreMedal(engine, playerID, 3, 22, "CO", medalCO)

			if(showsectiontime&&sectionTime.isNotEmpty()) {
				val x = if(receiver.nextDisplayType==2) 8 else 12
				val x2 = if(receiver.nextDisplayType==2) 9 else 12

				receiver.drawScoreFont(engine, playerID, x, 2, "SECTION TIME", COLOR.PURPLE)

				for(i in sectionTime.indices)
					if(sectionTime[i]>0) {
						var temp = i*100
						if(temp>999) temp = 999

						val section = engine.statistics.level/100
						var strSeparator = " "
						if(i==section&&engine.ending==0) strSeparator = "\u0082"

						val strSectionTime:String = String.format("%3d%s%s", temp, strSeparator, GeneralUtil.getTime(sectionTime[i]))

						receiver.drawScoreNum(engine, playerID, x, 3+i, strSectionTime, sectionIsNewRecord[i])
					}

				receiver.drawScoreFont(engine, playerID, x2, 17, "AVERAGE", COLOR.PURPLE)
				receiver.drawScoreNum(engine, playerID, x2, 18, GeneralUtil.getTime((engine.statistics.time/(sectionscomp+1))), 2f)
			}
		}
	}

	/** This function will be called when the piece is active */
	override fun onMove(engine:GameEngine, playerID:Int):Boolean {
		if(engine.ending==0&&engine.statc[0]==0&&!engine.holdDisable&&!lvupflag) {
			if(engine.statistics.level<nextseclv-1) {
				engine.statistics.level++
				if(engine.statistics.level==nextseclv-1&&lvstopse) engine.playSE("levelstop")
			}
			levelUp(engine)

			if(engine.timerActive&&medalRE<3) {
				val blocks = engine.field!!.howManyBlocks

				if(!recoveryFlag) {
					if(blocks>=150) recoveryFlag = true
				} else if(blocks<=70) {
					recoveryFlag = false
					engine.playSE("medal${++medalRE}")
				}
			}
		}
		if(engine.ending==0&&engine.statc[0]>0) lvupflag = false

		if(engine.ending==2&&!rollstarted) rollstarted = true

		return false
	}

	/** This function will be called during ARE */
	override fun onARE(engine:GameEngine, playerID:Int):Boolean {
		if(engine.ending==0&&engine.statc[0]>=engine.statc[1]-1&&!lvupflag) {
			if(engine.statistics.level<nextseclv-1) {
				engine.statistics.level++
				if(engine.statistics.level==nextseclv-1&&lvstopse) engine.playSE("levelstop")
			}
			levelUp(engine)
			lvupflag = true
		}

		return false
	}

	/** Levelup */
	private fun levelUp(engine:GameEngine) {
		engine.meterValue = engine.statistics.level%100*receiver.getMeterMax(engine)/99
		engine.meterColor = GameEngine.METER_COLOR_GREEN
		if(engine.statistics.level%100>=50) engine.meterColor = GameEngine.METER_COLOR_YELLOW
		if(engine.statistics.level%100>=80) engine.meterColor = GameEngine.METER_COLOR_ORANGE
		if(engine.statistics.level>=nextseclv-1) engine.meterColor = GameEngine.METER_COLOR_RED

		setSpeed(engine)

		if(tableBGMFadeout[bgmlv]!=-1&&engine.statistics.level>=tableBGMFadeout[bgmlv]) owner.bgmStatus.fadesw = true
	}

	/** Calculates line-clear score
	 * (This function will be called even if no lines are cleared) */
	override fun calcScore(engine:GameEngine, playerID:Int, lines:Int):Int {
		comboValue = if(lines==0) 1
		else maxOf(1, comboValue+2*lines-2)

		var rotateTemp = engine.nowPieceRotateCount
		if(rotateTemp>4) rotateTemp = 4
		rotateCount += rotateTemp

		if(lines>=1&&engine.ending==0) {
			if(lines>=4) {
				sectionfourline++

				if(big) {
					if(engine.statistics.totalQuadruple==1||engine.statistics.totalQuadruple==2
						||engine.statistics.totalQuadruple==4) {
						engine.playSE("medal${++medalSK}")

					}
				} else if(engine.statistics.totalQuadruple==5||engine.statistics.totalQuadruple==10
					||engine.statistics.totalQuadruple==17) {
					engine.playSE("medal${++medalSK}")
				}
			}

			if(engine.field!!.isEmpty)
				if(medalAC<3) {
					engine.playSE("medal${++medalAC}")
				}

			if(big) {
				if(engine.combo>=2&&medalCO<1) {
					engine.playSE("medal1")
					medalCO = 1
				} else if(engine.combo>=3&&medalCO<2) {
					engine.playSE("medal2")
					medalCO = 2
				} else if(engine.combo>=4&&medalCO<3) {
					engine.playSE("medal3")
					medalCO = 3
				}
			} else if(engine.combo>=4&&medalCO<1) {
				engine.playSE("medal1")
				medalCO = 1
			} else if(engine.combo>=5&&medalCO<2) {
				engine.playSE("medal2")
				medalCO = 2
			} else if(engine.combo>=7&&medalCO<3) {
				engine.playSE("medal3")
				medalCO = 3
			}

			val levelb = engine.statistics.level
			engine.statistics.level += lines
			levelUp(engine)

			if(engine.statistics.level>=999) {
				if(engine.timerActive) {
					sectionscomp++
					setAverageSectionTime()
				}

				engine.playSE("endingstart")
				engine.statistics.level = 999
				engine.timerActive = false
				engine.ending = 2
				rollclear = 1

				sectionlasttime = sectionTime[levelb/100]

				stMedalCheck(engine, levelb/100)

				roMedalCheck(engine)

				if(engine.statistics.totalQuadruple>=31&&gmfourline&&sectionfourline>=1) {
					grade = 6
					gradeflash = 180
				}
			} else if(nextseclv==300&&engine.statistics.level>=300&&engine.statistics.time>LV300TORIKAN) {
				if(engine.timerActive) {
					sectionscomp++
					setAverageSectionTime()
				}

				engine.playSE("endingstart")
				engine.statistics.level = 300
				engine.timerActive = false
				engine.ending = 2

				if(tableBGMChange[bgmlv]!=-1&&engine.statistics.level>=tableBGMChange[bgmlv]) {
					bgmlv++
					owner.bgmStatus.fadesw = false
					owner.bgmStatus.bgm = tableBGM[bgmlv]
				}

				sectionlasttime = sectionTime[levelb/100]

				stMedalCheck(engine, levelb/100)
			} else if(nextseclv==500&&engine.statistics.level>=500&&engine.statistics.time>LV500TORIKAN) {
				if(engine.timerActive) {
					sectionscomp++
					setAverageSectionTime()
				}

				engine.playSE("endingstart")
				engine.statistics.level = 500
				engine.timerActive = false
				engine.ending = 2

				if(tableBGMChange[bgmlv]!=-1&&engine.statistics.level>=tableBGMChange[bgmlv]) {
					bgmlv++
					owner.bgmStatus.fadesw = false
					owner.bgmStatus.bgm = tableBGM[bgmlv]
				}

				sectionlasttime = sectionTime[levelb/100]

				stMedalCheck(engine, levelb/100)
			} else if(nextseclv==800&&engine.statistics.level>=800&&engine.statistics.time>LV800TORIKAN) {
				if(engine.timerActive) {
					sectionscomp++
					setAverageSectionTime()
				}

				engine.playSE("endingstart")
				engine.statistics.level = 800
				engine.timerActive = false
				engine.ending = 2

				if(tableBGMChange[bgmlv]!=-1&&engine.statistics.level>=tableBGMChange[bgmlv]) {
					bgmlv++
					owner.bgmStatus.fadesw = false
					owner.bgmStatus.bgm = tableBGM[bgmlv]
				}

				sectionlasttime = sectionTime[levelb/100]

				stMedalCheck(engine, levelb/100)
			} else if(engine.statistics.level>=nextseclv) {

				owner.backgroundStatus.fadesw = true
				owner.backgroundStatus.fadecount = 0
				owner.backgroundStatus.fadebg = nextseclv/100

				if(tableBGMChange[bgmlv]!=-1&&engine.statistics.level>=tableBGMChange[bgmlv]) {
					bgmlv++
					owner.bgmStatus.fadesw = false
					owner.bgmStatus.bgm = tableBGM[bgmlv]
					engine.playSE("levelup_section")
				} else engine.playSE("levelup")

				sectionscomp++

				sectionlasttime = sectionTime[levelb/100]

				if(sectionfourline<2) gmfourline = false

				sectionfourline = 0

				stMedalCheck(engine, levelb/100)

				if(nextseclv==300||nextseclv==700) roMedalCheck(engine)

				if(startlevel==0)
					for(i in 0 until tableGradeLevel.size-1)
						if(engine.statistics.level>=tableGradeLevel[i]) {
							grade = i
							gradeflash = 180
						}

				nextseclv += 100
				if(nextseclv>999) nextseclv = 999
			} else if(engine.statistics.level==nextseclv-1&&lvstopse) engine.playSE("levelstop")

			lastscore = ((((levelb+lines)/4+engine.softdropFall+if(engine.manualLock) 1 else 0)*lines*comboValue
				*if(engine.field!!.isEmpty) 4 else 1)
				+engine.statistics.level/2+maxOf(0, engine.lockDelay-engine.lockDelayNow)*7)
			engine.statistics.scoreLine += lastscore
			//return lastscore
		}
		return 0
	}

	/** This function will be called when the game timer updates */
	override fun onLast(engine:GameEngine, playerID:Int) {
		if(gradeflash>0) gradeflash--
		setHeboHidden(engine)
		if(engine.timerActive&&engine.ending==0) {
			val section = engine.statistics.level/100

			if(section>=0&&section<sectionTime.size) {
				sectionTime[section]++
				setAverageSectionTime()
			}
		}

		if(engine.gameActive&&engine.ending==2) {
			if(engine.ctrl.isPress(Controller.BUTTON_F)&&engine.statistics.level<999) rolltime += 5
			rolltime++

			val remainRollTime = ROLLTIMELIMIT-rolltime
			engine.meterValue = remainRollTime*receiver.getMeterMax(engine)/ROLLTIMELIMIT
			engine.meterColor = GameEngine.METER_COLOR_GREEN
			if(remainRollTime<=30*60) engine.meterColor = GameEngine.METER_COLOR_YELLOW
			if(remainRollTime<=20*60) engine.meterColor = GameEngine.METER_COLOR_ORANGE
			if(remainRollTime<=10*60) engine.meterColor = GameEngine.METER_COLOR_RED

			if(rolltime>=ROLLTIMELIMIT) {
				if(engine.statistics.level>=999) rollclear = 2

				engine.gameEnded()
				engine.resetStatc()
				engine.stat = GameEngine.Status.EXCELLENT
			}
		}
	}

	/** This function will be called when the player tops out */
	override fun onGameOver(engine:GameEngine, playerID:Int):Boolean {
		if(engine.statc[0]==0) secretGrade = engine.field!!.secretGrade
		return false
	}

	/** Renders game result screen */
	override fun renderResult(engine:GameEngine, playerID:Int) {
		receiver.drawMenuFont(engine, playerID, 0, 0, "\u0090\u0093 PAGE${(engine.statc[1]+1)}/3", COLOR.RED)

		if(engine.statc[1]==0) {
			var gcolor = COLOR.WHITE
			if(rollclear==1) gcolor = COLOR.GREEN
			if(rollclear==2) gcolor = COLOR.ORANGE
			receiver.drawMenuFont(engine, playerID, 0, 2, "GRADE", COLOR.PURPLE)
			val strGrade = String.format("%10s", tableGradeName[grade])
			receiver.drawMenuFont(engine, playerID, 0, 3, strGrade, gcolor)

			drawResultStats(engine, playerID, receiver, 4, COLOR.PURPLE, Statistic.SCORE, Statistic.LINES, Statistic.LEVEL_MANIA,
				Statistic.TIME)
			drawResultRank(engine, playerID, receiver, 12, COLOR.PURPLE, rankingRank)
			if(secretGrade>4)
				drawResult(engine, playerID, receiver, 15, COLOR.PURPLE, "S. GRADE",
					String.format("%10s", tableSecretGradeName[secretGrade-1]))
		} else if(engine.statc[1]==1) {
			receiver.drawMenuFont(engine, playerID, 0, 2, "SECTION", COLOR.PURPLE)

			for(i in sectionTime.indices)
				if(sectionTime[i]>0)
					receiver.drawMenuFont(engine, playerID, 2, 3+i, GeneralUtil.getTime(sectionTime[i]), sectionIsNewRecord[i])

			if(sectionavgtime>0) {
				receiver.drawMenuFont(engine, playerID, 0, 14, "AVERAGE", COLOR.PURPLE)
				receiver.drawMenuFont(engine, playerID, 2, 15, GeneralUtil.getTime(sectionavgtime))
			}
		} else if(engine.statc[1]==2) {
			receiver.drawMenuFont(engine, playerID, 0, 2, "MEDAL", COLOR.PURPLE)
			getMedalFontColor(medalAC)?.let {receiver.drawMenuFont(engine, playerID, 5, 3, "AC", it)}
			getMedalFontColor(medalST)?.let {receiver.drawMenuFont(engine, playerID, 8, 3, "ST", it)}
			getMedalFontColor(medalSK)?.let {receiver.drawMenuFont(engine, playerID, 5, 4, "SK", it)}
			getMedalFontColor(medalRE)?.let {receiver.drawMenuFont(engine, playerID, 8, 4, "RE", it)}
			getMedalFontColor(medalRO)?.let {receiver.drawMenuFont(engine, playerID, 5, 5, "SK", it)}
			getMedalFontColor(medalCO)?.let {receiver.drawMenuFont(engine, playerID, 8, 5, "CO", it)}

			drawResultStats(engine, playerID, receiver, 6, COLOR.PURPLE, Statistic.LPM, Statistic.SPM, Statistic.PIECE, Statistic.PPS)
		}
	}

	/** Additional routine for game result screen */
	override fun onResult(engine:GameEngine, playerID:Int):Boolean {
		// Page change
		if(engine.ctrl.isMenuRepeatKey(Controller.BUTTON_UP)) {
			engine.statc[1]--
			if(engine.statc[1]<0) engine.statc[1] = 2
			engine.playSE("change")
		}
		if(engine.ctrl.isMenuRepeatKey(Controller.BUTTON_DOWN)) {
			engine.statc[1]++
			if(engine.statc[1]>2) engine.statc[1] = 0
			engine.playSE("change")
		}
		// Flip Leaderboard/Best Section Time Records
		if(engine.ctrl.isPush(Controller.BUTTON_F)) {
			engine.playSE("change")
			isShowBestSectionTime = !isShowBestSectionTime
		}

		return false
	}

	/** This function will be called when the replay data is going to be
	 * saved */
	override fun saveReplay(engine:GameEngine, playerID:Int, prop:CustomProperties) {
		saveSetting(owner.replayProp)
		owner.replayProp.setProperty("phantommania.version", version)

		if(!owner.replayMode&&startlevel==0&&!big&&engine.ai==null) {
			updateRanking(grade, engine.statistics.level, engine.statistics.time, rollclear)
			if(medalST==3) updateBestSectionTime()

			if(rankingRank!=-1||medalST==3) {
				saveRanking(engine.ruleopt.strRuleName)
				owner.saveModeConfig()
			}
		}
	}

	/** Read rankings from property file
	 * @param prop Property file
	 * @param ruleName Rule name
	 */
	override fun loadRanking(prop:CustomProperties, ruleName:String) {
		for(i in 0 until RANKING_MAX) {
			rankingGrade[i] = prop.getProperty("$ruleName.$i.grade", 0)
			rankingLevel[i] = prop.getProperty("$ruleName.$i.level", 0)
			rankingTime[i] = prop.getProperty("$ruleName.$i.time", 0)
			rankingRollclear[i] = prop.getProperty("$ruleName.$i.clear", 0)
		}
		for(i in 0 until SECTION_MAX)
			bestSectionTime[i] = prop.getProperty("$ruleName.$i.sectiontime", bestSectionTime[i])

//		decoration = owner.statsProp.getProperty("decoration", 0)
	}

	/** Save rankings to property file
	 * @param ruleName Rule name
	 */
	fun saveRanking(ruleName:String) {
		super.saveRanking(ruleName, (0 until RANKING_MAX).flatMap {i ->
			listOf("$ruleName.$i.grade" to rankingGrade[i],
				"$ruleName.$i.level" to rankingLevel[i],
				"$ruleName.$i.time" to rankingTime[i],
				"$ruleName.$i.clear" to rankingRollclear[i])
		}+(0 until SECTION_MAX).flatMap {i ->
			listOf("$ruleName.sectiontime.$i" to bestSectionTime[i])
		})

//		owner.statsProp.setProperty("decoration", decoration)
//		receiver.saveProperties(owner.statsFile, owner.statsProp)
	}

	/** Update the ranking */
	private fun updateRanking(gr:Int, lv:Int, time:Int, clear:Int) {
		rankingRank = checkRanking(gr, lv, time, clear)

		if(rankingRank!=-1) {
			for(i in RANKING_MAX-1 downTo rankingRank+1) {
				rankingGrade[i] = rankingGrade[i-1]
				rankingLevel[i] = rankingLevel[i-1]
				rankingTime[i] = rankingTime[i-1]
				rankingRollclear[i] = rankingRollclear[i-1]
			}

			rankingGrade[rankingRank] = gr
			rankingLevel[rankingRank] = lv
			rankingTime[rankingRank] = time
			rankingRollclear[rankingRank] = clear
		}
	}

	/** This function will check the ranking and returns which place you are.
	 * (-1: Out of rank) */
	private fun checkRanking(gr:Int, lv:Int, time:Int, clear:Int):Int {
		for(i in 0 until RANKING_MAX)
			if(clear>rankingRollclear[i])
				return i
			else if(clear==rankingRollclear[i]&&gr>rankingGrade[i])
				return i
			else if(clear==rankingRollclear[i]&&gr==rankingGrade[i]&&lv>rankingLevel[i])
				return i
			else if(clear==rankingRollclear[i]&&gr==rankingGrade[i]&&lv==rankingLevel[i]&&time<rankingTime[i]) return i

		return -1
	}

	/** Updates best section time records */
	private fun updateBestSectionTime() {
		for(i in 0 until SECTION_MAX)
			if(sectionIsNewRecord[i]) bestSectionTime[i] = sectionTime[i]
	}

	companion object {
		/** Current version */
		private const val CURRENT_VERSION = 1

		/** ARE table */
		private val tableARE = intArrayOf(15, 11, 11, 5, 4, 3)

		/** ARE Line table */
		private val tableARELine = intArrayOf(11, 5, 5, 4, 4, 3)

		/** Line Delay table */
		private val tableLineDelay = intArrayOf(12, 6, 6, 7, 5, 4)

		/** Lock Delay table */
		private val tableLockDelay = intArrayOf(31, 29, 28, 27, 26, 25)

		/** DAS table */
		private val tableDAS = intArrayOf(11, 11, 10, 9, 7, 7)
		private val tableHiddenDelay = intArrayOf(360, 320, 256, 192, 160, 128)

		/** BGM fadeout level */
		private val tableBGMFadeout = intArrayOf(280, 480, -1)

		/** BGM change level */
		private val tableBGMChange = intArrayOf(300, 500, -1)
		private val tableBGM = arrayOf(BGM.GrandT(5), BGM.GrandT(4), BGM.GrandT(3), BGM.GrandA(3))
		/** Grade names */

		private val tableGradeName = arrayOf("", "m", "MK", "MV", "MO", "MM", "GM")

		/** Required level for grade */
		private val tableGradeLevel = intArrayOf(0, 500, 600, 700, 800, 900, 999)

		/** Secret grade names */
		private val tableSecretGradeName = arrayOf("S1", "S2", "S3", "S4", "S5", "S6", "S7", "S8", "S9", //  0?` 8
			"M1", "M2", "M3", "M4", "M5", "M6", "M7", "M8", "M9", //  9?`17
			"GM" // 18
		)

		/** Ending time limit */
		private const val ROLLTIMELIMIT = 1982

		/** Number of hiscore records */
		private const val RANKING_MAX = 10

		/** Level 300 time limit */
		private const val LV300TORIKAN = 8880

		/** Level 500 time limit */
		private const val LV500TORIKAN = 13080

		/** Level 800 time limit */
		private const val LV800TORIKAN = 19380

		/** Number of sections */
		private const val SECTION_MAX = 10

		/** Default section time */
		private const val DEFAULT_SECTION_TIME = 3600
	}
}
