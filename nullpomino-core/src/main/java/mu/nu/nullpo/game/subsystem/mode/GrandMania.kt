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
import kotlin.math.*

/** GRADE MANIA 2 Mode */
class GrandMania:AbstractMode() {

	/** Current 落下速度の number (tableGravityChangeLevelの levelに到達するたびに1つ増える) */
	private var gravityindex:Int = 0

	/** Next Section の level (これ-1のときに levelストップする) */
	private var nextseclv:Int = 0

	/** Levelが増えた flag */
	private var lvupflag:Boolean = false

	/** 画面に表示されている実際の段位 */
	private var grade:Int = 0

	/** 内部段位 */
	private var gradeInternal:Int = 0

	/** 段位 point */
	private var gradePoint:Int = 0

	/** 段位 pointが1つ減る time */
	private var gradeDecay:Int = 0

	/** 最後に段位が上がった time */
	private var lastGradeTime:Int = 0

	/** Hard dropした段count */
	private var harddropBonus:Int = 0

	/** Combo bonus */
	private var comboValue:Int = 0

	/** Most recent increase in score */
	private var lastscore:Int = 0

	/** 獲得Render scoreがされる残り time */
	private var scgettime:Int = 0

	/** Roll 経過 time */
	private var rolltime:Int = 0

	/** Roll completely cleared flag */
	private var rollclear:Int = 0

	/** Roll started flag */
	private var rollstarted:Boolean = false

	/** 裏段位 */
	private var secretGrade:Int = 0

	/** Current BGM */
	private var bgmlv:Int = 0

	/** 段位表示を光らせる残り frame count */
	private var gradeflash:Int = 0

	/** Section Time */
	private var sectionTime:IntArray = IntArray(SECTION_MAX)

	/** 新記録が出たSection はtrue */
	private var sectionIsNewRecord:BooleanArray = BooleanArray(SECTION_MAX)

	/** Cleared Section count */
	private var sectionscomp:Int = 0

	/** Average Section Time */
	private var sectionavgtime:Int = 0

	/** 直前のSection Time */
	private var sectionlasttime:Int = 0

	/** Section 内で4-line clearした count */
	private var sectionquads:IntArray = IntArray(0)

	/** 消えRoll flag１ (Section Time) */
	private var mrollSectiontime:Boolean = false

	/** 消えRoll flag２ (4-line clear) */
	private var mrollFourline:Boolean = false

	/** 消えRoll started flag */
	private var mrollFlag:Boolean = false

	/** 消えRoll 中に消したline count */
	private var mrollLines:Int = 0

	/** medal 状態 */
	private var medalAC:Int = 0
	private var medalST:Int = 0
	private var medalSK:Int = 0
	private var medalRE:Int = 0
	private var medalRO:Int = 0
	private var medalCO:Int = 0

	/** 150個以上Blockがあるとtrue, 70個まで減らすとfalseになる */
	private var recoveryFlag:Boolean = false

	/** rotationした合計 count (Maximum4個ずつ増える) */
	private var rotateCount:Int = 0

	/** Section Time記録表示中ならtrue */
	private var isShowBestSectionTime:Boolean = false

	/** Level at start */
	private var startlevel:Int = 0

	/** When true, always ghost ON */
	private var alwaysghost:Boolean = false

	/** When true, always 20G */
	private var always20g:Boolean = false

	/** When true, levelstop sound is enabled */
	private var lvstopse:Boolean = false

	/** BigMode */
	private var big:Boolean = false

	/** When true, section time display is enabled */
	private var showsectiontime:Boolean = false

	/** Version */
	private var version:Int = 0

	/** Current round's ranking rank */
	private var rankingRank:Int = 0

	/** Rankings' 段位 */
	private var rankingGrade:IntArray = IntArray(RANKING_MAX)

	/** Rankings' level */
	private var rankingLevel:IntArray = IntArray(RANKING_MAX)

	/** Rankings' times */
	private var rankingTime:IntArray = IntArray(RANKING_MAX)

	/** Rankings' Roll completely cleared flag */
	private var rankingRollclear:IntArray = IntArray(RANKING_MAX)

	/** Section Time記録 */
	private var bestSectionTime:IntArray = IntArray(SECTION_MAX)
	private var bestSectionQuads:IntArray = IntArray(SECTION_MAX)

	private var decoration:Int = 0
	private var dectemp:Int = 0

	/* Mode name */
	override val name:String = "Grand Mania"

	/* Initialization */
	override fun playerInit(engine:GameEngine, playerID:Int) {
		super.playerInit(engine, playerID)
		menuTime = 0
		gravityindex = 0
		nextseclv = 0
		lvupflag = true
		grade = 0
		gradeInternal = 0
		gradePoint = 0
		gradeDecay = 0
		lastGradeTime = 0
		harddropBonus = 0
		comboValue = 0
		lastscore = 0
		scgettime = 0
		rolltime = 0
		rollclear = 0
		rollstarted = false
		secretGrade = 0
		bgmlv = 0
		gradeflash = 0
		sectionTime = IntArray(SECTION_MAX)
		sectionIsNewRecord = BooleanArray(SECTION_MAX)
		sectionscomp = 0
		sectionavgtime = 0
		sectionlasttime = 0
		sectionquads = IntArray(SECTION_MAX)
		mrollSectiontime = true
		mrollFourline = true
		mrollFlag = false
		mrollLines = 0
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
		alwaysghost = false
		always20g = false
		lvstopse = false
		big = false

		decoration = 0
		dectemp = 0

		rankingRank = -1
		rankingGrade = IntArray(RANKING_MAX)
		rankingLevel = IntArray(RANKING_MAX)
		rankingTime = IntArray(RANKING_MAX)
		rankingRollclear = IntArray(RANKING_MAX)
		bestSectionTime = IntArray(SECTION_MAX)
		bestSectionQuads = IntArray(SECTION_MAX)

		engine.twistEnable = false
		engine.b2bEnable = true
		engine.splitb2b = true
		engine.comboType = GameEngine.COMBO_TYPE_DOUBLE
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
			version = owner.replayProp.getProperty("grademania2.version", 0)
		}

		owner.backgroundStatus.bg = 10+startlevel
	}

	/** Load settings from property file
	 * @param prop Property file
	 */
	override fun loadSetting(prop:CustomProperties) {
		startlevel = prop.getProperty("grademania2.startlevel", 0)
		alwaysghost = prop.getProperty("grademania2.alwaysghost", true)
		always20g = prop.getProperty("grademania2.always20g", false)
		lvstopse = prop.getProperty("grademania2.lvstopse", true)
		showsectiontime = prop.getProperty("grademania2.showsectiontime", true)
		big = prop.getProperty("grademania2.big", false)
	}

	/** Save settings to property file
	 * @param prop Property file
	 */
	override fun saveSetting(prop:CustomProperties) {
		prop.setProperty("grademania2.startlevel", startlevel)
		prop.setProperty("grademania2.alwaysghost", alwaysghost)
		prop.setProperty("grademania2.always20g", always20g)
		prop.setProperty("grademania2.lvstopse", lvstopse)
		prop.setProperty("grademania2.showsectiontime", showsectiontime)
		prop.setProperty("grademania2.big", big)
	}

	/** Set BGM at start of game
	 * @param engine GameEngine
	 */
	private fun setStartBgmlv(engine:GameEngine) {
		bgmlv = 0
		while(tableBGMChange[bgmlv]!=-1&&engine.statistics.level>=tableBGMChange[bgmlv])
			bgmlv++
	}

	/** Update falling speed
	 * @param engine GameEngine
	 */
	private fun setSpeed(engine:GameEngine) {
		if(always20g||engine.statistics.time>=54000)
			engine.speed.gravity = -1
		else {
			while(engine.statistics.level>=tableGravityChangeLevel[gravityindex])
				gravityindex++
			engine.speed.gravity = tableGravityValue[gravityindex]
		}

		var section = engine.statistics.level/100
		if(section>tableARE.size-1) section = tableARE.size-1
		engine.speed.das = tableDAS[section]

		if(engine.statistics.time>=54000) {
			engine.speed.are = 3
			engine.speed.areLine = 6
			engine.speed.lineDelay = 6
			engine.speed.lockDelay = 19
		} else {
			engine.speed.are = tableARE[section]
			engine.speed.areLine = tableARELine[section]
			engine.speed.lineDelay = tableLineDelay[section]
			engine.speed.lockDelay = tableLockDelay[section]
		}
	}

	/** Update average section time */
	private fun setAverageSectionTime() {
		if(sectionscomp>0) {
			var temp = 0
			for(i in startlevel until startlevel+sectionscomp)
				if(i>=0&&i<sectionTime.size) temp += sectionTime[i]

			sectionavgtime = temp/sectionscomp
		} else
			sectionavgtime = 0

	}

	/** 消えRoll 条件を満たしているか check
	 * @param levelb 上がる前の level
	 */
	private fun mrollCheck(levelb:Int) {
		if(sectionlasttime>mrollTime(levelb/100)) mrollSectiontime = false
		// 4-line clear
		var required4line = 2
		if(levelb in 500..899) required4line = 1
		if(levelb>=900) required4line = 0

		if(sectionquads[levelb/100]<required4line) mrollFourline = false
	}

	private fun mrollTime(section:Int):Int {
		// Section Time
		var temp = 4500-section*100
		if(section>=6)
			if(section==6) {
				for(i in 0..4)
					temp += sectionTime[i]
				temp = temp/5+1000
			} else
				temp = sectionTime[section-1]+(1140-section*60)
		//780 720 660 600
		return temp
	}

	/** ST medal check
	 * @param engine GameEngine
	 * @param sectionNumber Section number
	 */
	private fun stMedalCheck(engine:GameEngine, sectionNumber:Int) {
		val best = bestSectionTime[sectionNumber]

		if(sectionlasttime<best) {
			if(medalST<3) {
				engine.playSE("medal3")
				if(medalST<1) dectemp += 3
				if(medalST<2) dectemp += 6
				medalST = 3
				dectemp += 6
			}
			if(!owner.replayMode) {
				dectemp++
				sectionIsNewRecord[sectionNumber] = true
			}
		} else if(sectionlasttime<best+300&&medalST<2) {
			engine.playSE("medal2")
			if(medalST<1) dectemp += 3
			medalST = 2
			dectemp += 6
		} else if(sectionlasttime<best+600&&medalST<1) {
			engine.playSE("medal1")
			medalST = 1
			dectemp += 3// 12
		}
	}

	/** RO medal check
	 * @param engine Engine
	 */
	private fun roMedalCheck(engine:GameEngine) {
		val rotateAverage = rotateCount.toFloat()/engine.statistics.totalPieceLocked.toFloat()

		if(rotateAverage>=1.2f&&medalRO<3) {
			engine.playSE("medal${++medalRO}")
			dectemp += 6
		}
	}

	/* Called at settings screen */
	override fun onSetting(engine:GameEngine, playerID:Int):Boolean {
		// Menu
		if(!engine.owner.replayMode) {
			// Configuration changes
			val change = updateCursor(engine, 5)

			if(change!=0) {
				engine.playSE("change")

				when(menuCursor) {
					0 -> {
						startlevel += change
						if(startlevel<0) startlevel = 9
						if(startlevel>9) startlevel = 0
						owner.backgroundStatus.bg = 10+startlevel
					}
					1 -> alwaysghost = !alwaysghost
					2 -> always20g = !always20g
					3 -> lvstopse = !lvstopse
					4 -> showsectiontime = !showsectiontime
					5 -> big = !big
				}
			}

			// section time display切替
			if(engine.ctrl.isPush(Controller.BUTTON_F)&&menuTime>=5) {
				engine.playSE("change")
				isShowBestSectionTime = !isShowBestSectionTime
			}

			// 決定
			if(engine.ctrl.isPush(Controller.BUTTON_A)&&menuTime>=5) {
				engine.playSE("decide")
				saveSetting(owner.modeConfig)
				owner.saveModeConfig()
				isShowBestSectionTime = false
				sectionscomp = 0

				owner.bgmStatus.fadesw = true
				return false
			}

			// Cancel
			if(engine.ctrl.isPush(Controller.BUTTON_B)) engine.quitflag = true

			menuTime++
		} else {
			menuTime++
			menuCursor = -1

			return menuTime<60
		}

		return true
	}

	/* Render the settings screen */
	override fun renderSetting(engine:GameEngine, playerID:Int) {
		drawMenu(engine, playerID, receiver, 0, COLOR.BLUE, 0, "Level", (startlevel*100).toString(), "FULL GHOST",
			GeneralUtil.getONorOFF(alwaysghost), "20G MODE", GeneralUtil.getONorOFF(always20g), "LVSTOPSE",
			GeneralUtil.getONorOFF(lvstopse), "SHOW STIME", GeneralUtil.getONorOFF(showsectiontime), "BIG",
			GeneralUtil.getONorOFF(big))
	}

	/* Called at game start */
	override fun startGame(engine:GameEngine, playerID:Int) {
		engine.statistics.level = startlevel*100

		dectemp = 0
		nextseclv = engine.statistics.level+100
		if(engine.statistics.level<0) nextseclv = 100
		if(engine.statistics.level>=900) nextseclv = 999

		owner.backgroundStatus.bg = 10+engine.statistics.level/100

		engine.big = big

		setSpeed(engine)
		setStartBgmlv(engine)
		owner.bgmStatus.bgm = tableBGM[bgmlv]
	}

	/* Render score */
	override fun renderLast(engine:GameEngine, playerID:Int) {
		receiver.drawScoreFont(engine, playerID, 0, 0, "GRAND MANIA", COLOR.CYAN)

		receiver.drawScoreFont(engine, playerID, -1, -4*2, "DECORATION", scale = .5f)
		receiver.drawScoreBadges(engine, playerID, 0, -3, 100, decoration)
		receiver.drawScoreBadges(engine, playerID, 5, -4, 100, dectemp)
		if(engine.stat==GameEngine.Status.SETTING||engine.stat==GameEngine.Status.RESULT&&!owner.replayMode) {
			if(!owner.replayMode&&startlevel==0&&!big&&!always20g
				&&engine.ai==null)
				if(!isShowBestSectionTime) {
					// Rankings

					receiver.drawScoreFont(engine, playerID, 3, 2, "GRADE TIME LEVEL", COLOR.BLUE)

					for(i in 0 until RANKING_MAX) {
						var gcolor = COLOR.WHITE
						if(rankingRollclear[i]==1||rankingRollclear[i]==3) gcolor = COLOR.GREEN
						if(rankingRollclear[i]==2||rankingRollclear[i]==4) gcolor = COLOR.ORANGE

						receiver.drawScoreGrade(engine, playerID, 0, 3+i, String.format("%2d", i+1),
							if(rankingRank==i) COLOR.RAINBOW else COLOR.YELLOW)
						if(rankingGrade[i]>=0&&rankingGrade[i]<tableGradeName.size)
							receiver.drawScoreGrade(engine, playerID, 3, 3+i, tableGradeName[rankingGrade[i]], gcolor)
						receiver.drawScoreNum(engine, playerID, 7, 3+i, GeneralUtil.getTime(rankingTime[i]), i==rankingRank)
						receiver.drawScoreNum(engine, playerID, 15, 3+i, String.format("%03d", rankingLevel[i]), i==rankingRank)
					}

					receiver.drawScoreFont(engine, playerID, 0, 17, "F:VIEW SECTION TIME", COLOR.GREEN)
				} else {
					// Section Time
					receiver.drawScoreFont(engine, playerID, 0, 2, "SECTION TIME QUADS", COLOR.BLUE)

					var totalTime = 0
					for(i in 0 until SECTION_MAX) {
						val temp = minOf(i*100, 999)
						val temp2 = minOf((i+1)*100-1, 999)

						val strSectionTime:String = String.format("%3d-%3d %s %d", temp, temp2, GeneralUtil.getTime(bestSectionTime[i]),
							bestSectionQuads[i])

						receiver.drawScoreNum(engine, playerID, 0, 3+i, strSectionTime, sectionIsNewRecord[i])

						totalTime += bestSectionTime[i]
					}

					receiver.drawScoreFont(engine, playerID, 0, 14, "TOTAL", COLOR.BLUE)
					receiver.drawScoreNum(engine, playerID, 0, 15, GeneralUtil.getTime(totalTime), 2f)
					receiver.drawScoreFont(engine, playerID, if(receiver.nextDisplayType==2)
						0
					else
						12, if(receiver.nextDisplayType==2) 18 else 14, "AVERAGE", COLOR.BLUE)
					receiver.drawScoreNum(engine, playerID, if(receiver.nextDisplayType==2)
						0
					else
						12, if(receiver.nextDisplayType==2) 19 else 15, GeneralUtil.getTime((totalTime/SECTION_MAX)), 2f)

					receiver.drawScoreFont(engine, playerID, 0, 17, "F:VIEW RANKING", COLOR.GREEN)

				}
		} else {
			val g20 = engine.speed.gravity<0&&engine.statistics.time%3==0
			// 段位
			if(grade>=0&&grade<tableGradeName.size)
				receiver.drawScoreGrade(engine, playerID, 0, 1, tableGradeName[grade], gradeflash>0&&gradeflash%4==0||g20, 2f)
			if(grade<17)
				receiver.drawScoreNum(engine, playerID, 0, 3,
					String.format("%02.1f%%", gradePoint-gradeDecay*1f/tableGradeDecayRate[gradeInternal]),
					if(g20)
						COLOR.YELLOW
					else if(mrollFourline&&mrollSectiontime)
						COLOR.CYAN
					else
						COLOR.BLUE)
			if(gradeInternal>=0&&gradeInternal<tableDetailGradeName.size)
				receiver.drawScoreGrade(engine, playerID, 3, 3, tableDetailGradeName[gradeInternal],
					gradeflash>0&&gradeflash%4==0||g20)

			// Score
			receiver.drawScoreFont(engine, playerID, 0, 6, "Score",
				if(g20&&mrollFourline) COLOR.CYAN else COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 5, 6, "+$lastscore", g20)
			receiver.drawScoreNum(engine, playerID, 0, 7, "$scgettime", g20, 2f)
			if(scgettime<engine.statistics.score) scgettime += ceil(((engine.statistics.score-scgettime)/10f).toDouble()).toInt()

			// level
			receiver.drawScoreFont(engine, playerID, 0, 9, "Level",
				if(g20&&mrollSectiontime&&mrollFourline) COLOR.CYAN else COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 1, 10, String.format("%3d", maxOf(engine.statistics.level, 0)), g20)
			receiver.drawSpeedMeter(engine, playerID, 0, 11,
				if(g20) 40 else floor(ln(engine.speed.gravity.toDouble())).toInt()*4, 4)
			receiver.drawScoreNum(engine, playerID, 1, 12, String.format("%3d", nextseclv), g20)

			// Time
			receiver.drawScoreFont(engine, playerID, 0, 14, "Time",
				if(g20&&mrollSectiontime) COLOR.CYAN else COLOR.BLUE)
			if(engine.ending!=2||rolltime/20%2==0)
				receiver.drawScoreNum(engine, playerID, 0, 15, GeneralUtil.getTime(engine.statistics.time), g20&&mrollSectiontime,
					2f)

			// Roll 残り time
			if(engine.gameActive&&engine.ending==2) {
				var time = ROLLTIMELIMIT-rolltime
				if(time<0) time = 0
				receiver.drawScoreFont(engine, playerID, 0, 17, "ROLL TIME", COLOR.BLUE)
				receiver.drawScoreNum(engine, playerID, 0, 18, GeneralUtil.getTime(time), time>0&&time<10*60, 2f)
			}

			// medal
			receiver.drawScoreMedal(engine, playerID, 0, 20, "AC", medalAC)
			receiver.drawScoreMedal(engine, playerID, 3, 20, "ST", medalST)
			receiver.drawScoreMedal(engine, playerID, 0, 21, "SK", medalSK)
			receiver.drawScoreMedal(engine, playerID, 3, 21, "RE", medalRE)
			receiver.drawScoreMedal(engine, playerID, 0, 22, "SK", medalRO)
			receiver.drawScoreMedal(engine, playerID, 3, 22, "CO", medalCO)

			// Section Time
			if(showsectiontime&&sectionTime.isNotEmpty()) {
				val x = receiver.nextDisplayType==2
				receiver.drawScoreFont(engine, playerID, if(x) 8 else 10, 2, "SECTION TIME", COLOR.BLUE)

				val section = engine.statistics.level/100
				for(i in sectionTime.indices)
					if(sectionTime[i]>0) {

						var strSeparator = "-"
						if(i==section&&engine.ending==0) strSeparator = "+"

						val color:COLOR =
							if(sectionquads[i]>=(if(i<5) 2 else 1)&&sectionTime[i]<mrollTime(i))
								if(sectionIsNewRecord[i]) COLOR.CYAN else COLOR.GREEN
							else if(sectionIsNewRecord[i]) COLOR.RED else COLOR.WHITE

						val strSectionTime = StringBuilder()
						for(l in 0 until i)
							strSectionTime.append("\n")
						strSectionTime.append(String.format("%3d%s%s %d\n",
							if(i<10) i*100 else 999, strSeparator, GeneralUtil.getTime(sectionTime[i]), sectionquads[i]))
						receiver.drawScoreNum(engine, playerID, if(x) 9 else 10, 3, "$strSectionTime", color, if(x) .75f else 1f)
					}

				receiver.drawScoreFont(engine, playerID, if(x) 8 else 12, if(x) 11 else 14, "AVERAGE", COLOR.BLUE)
				receiver.drawScoreNum(engine, playerID, if(x) 8 else 12, if(x) 12 else 15,
					GeneralUtil.getTime((engine.statistics.time/(sectionscomp+if(engine.ending==0) 1 else 0))), 2f)

			}
		}
	}

	/* 移動中の処理 */
	override fun onMove(engine:GameEngine, playerID:Int):Boolean {
		// 新規ピース出現時
		if(engine.ending==0&&engine.statc[0]==0&&!engine.holdDisable&&!lvupflag) {
			// Level up
			if(engine.statistics.level<nextseclv-1) {
				engine.statistics.level++
				if(engine.statistics.level==nextseclv-1&&lvstopse) engine.playSE("levelstop")
			}
			levelUp(engine)

		}
		if(engine.ending==0&&engine.statc[0]>0&&(version>=1||!engine.holdDisable)) lvupflag = false

		// 段位 point減少
		if(engine.timerActive&&gradePoint>0&&engine.combo<=0&&engine.lockDelayNow<engine.lockDelay-1) {
			gradeDecay++

			var index = gradeInternal
			if(index>tableGradeDecayRate.size-1) index = tableGradeDecayRate.size-1

			if(gradeDecay>=tableGradeDecayRate[index]) {
				gradeDecay = 0
				gradePoint--
			}
		}

		// Endingスタート
		if(engine.ending==2&&!rollstarted) {
			rollstarted = true

			if(mrollFlag) {
				engine.blockHidden = engine.ruleopt.lockflash
				engine.blockHiddenAnim = false
				engine.blockOutlineType = GameEngine.BLOCK_OUTLINE_NONE
			} else {
				engine.blockHidden = 300
				engine.blockHiddenAnim = true
				engine.blockOutlineType = GameEngine.BLOCK_OUTLINE_NONE
			}

			owner.bgmStatus.bgm = BGM.Ending(1)
		}

		return false
	}

	/* ARE中の処理 */
	override fun onARE(engine:GameEngine, playerID:Int):Boolean {
		// 最後の frame
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

	/** levelが上がったときの共通処理 */
	private fun levelUp(engine:GameEngine) {
		// Meter
		engine.meterValue = engine.statistics.level%100*receiver.getMeterMax(engine)/99
		engine.meterColor = GameEngine.METER_COLOR_LEVEL

		// 速度変更
		setSpeed(engine)

		// LV100到達でghost を消す
		if(engine.statistics.level>=100&&!alwaysghost) engine.ghost = false

		// BGM fadeout
		if(tableBGMFadeout[bgmlv]!=-1&&engine.statistics.level>=tableBGMFadeout[bgmlv]) owner.bgmStatus.fadesw = true

		if(version>=2) {
			// Hard drop bonusInitialization
			harddropBonus = 0

			// RE medal
			if(engine.timerActive&&medalRE<3) {
				val blocks = engine.field!!.howManyBlocks

				if(!recoveryFlag) {
					if(blocks>=150) recoveryFlag = true

				} else if(blocks<=70) {
					recoveryFlag = false
					dectemp += 5+medalRE// 5 11 18
					engine.playSE("medal${++medalRE}")
				}

			}
		}
	}

	/* Calculate score */
	override fun calcScore(engine:GameEngine, playerID:Int, lines:Int):Int {
		// Combo
		comboValue = if(lines==0) 1
		else maxOf(1, comboValue+2*lines-2)

		// RO medal 用カウント
		var rotateTemp = engine.nowPieceRotateCount
		if(rotateTemp>4) rotateTemp = 4
		rotateCount += rotateTemp

		if(lines>=1&&engine.ending==0) {
			// 段位 point
			var index = gradeInternal
			if(index>10) index = 10
			val basepoint = tableGradePoint[lines-1][index]

			var indexcombo = engine.combo+if(engine.b2b) 0 else -1
			if(indexcombo<0) indexcombo = 0
			if(indexcombo>tableGradeComboBonus[lines-1].size-1) indexcombo = tableGradeComboBonus[lines-1].size-1
			val combobonus = tableGradeComboBonus[lines-1][indexcombo]

			val levelbonus = 1+engine.statistics.level/250

			val point = basepoint.toFloat()*combobonus*levelbonus.toFloat()
			gradePoint += point.toInt()

			// 内部段位上昇
			while(gradePoint>=100) {
				gradePoint -= 100
				gradeDecay = 0
				if(gradeInternal<31) gradeInternal++

				if(tableGradeChange[grade]!=-1&&gradeInternal>=tableGradeChange[grade]) {
					engine.playSE("gradeup")
					engine.playSE("grade${grade*4/17}")
					grade++
					dectemp++
					gradeflash = 180
					lastGradeTime = engine.statistics.time
				}
			}

			// 4-line clearカウント
			if(lines>=4) {
				sectionquads[engine.statistics.level/100]++

				// SK medal
				if(big) {
					if(engine.statistics.totalQuadruple==1||engine.statistics.totalQuadruple==2
						||engine.statistics.totalQuadruple==4) {
						engine.playSE("medal${++medalSK}")
					}
				} else if(engine.statistics.totalQuadruple==10||engine.statistics.totalQuadruple==20
					||engine.statistics.totalQuadruple==30) {
					dectemp += 3+medalSK*2// 3 8 15
					engine.playSE("medal${++medalSK}")
				}

			}

			// AC medal
			if(engine.field!!.isEmpty) {

				dectemp += lines*25
				if(lines==3) dectemp += 25
				if(lines==4) dectemp += 150
				if(medalAC<3) {
					dectemp += 3+medalAC*4// 3 10 21
					engine.playSE("medal${++medalAC}")
				}
			}

			// CO medal
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
			} else if(engine.combo>=3&&medalCO<1) {
				engine.playSE("medal1")
				medalCO = 1
				dectemp += 3// 3
			} else if(engine.combo>=4&&medalCO<2) {
				engine.playSE("medal2")
				medalCO = 2
				dectemp += 4// 7
			} else if(engine.combo>=5&&medalCO<3) {
				engine.playSE("medal3")
				medalCO = 3
				dectemp += 5// 12
			}
			// Level up
			val levelb = engine.statistics.level
			engine.statistics.level += lines+if(engine.b2b) 2 else if(engine.b2bcount>=1) 1 else 0
			levelUp(engine)

			if(engine.statistics.level>=999) {
				// Ending
				engine.statistics.level = 999
				engine.timerActive = false
				engine.ending = 1
				rollclear = 1

				lastGradeTime = engine.statistics.time

				// Section Timeを記録
				sectionlasttime = sectionTime[levelb/100]
				sectionscomp++
				setAverageSectionTime()

				// 消えRoll check
				mrollCheck(levelb)
				if(engine.statistics.time>M_ROLL_TIME_REQUIRE) mrollSectiontime = false

				mrollFlag = mrollSectiontime&&mrollFourline&&gradeInternal>=31
				// ST medal
				stMedalCheck(engine, levelb/100)

				// RO medal
				roMedalCheck(engine)

				// 段位M
				if(mrollFlag) {
					engine.playSE("applause4")
					grade = 18
					gradeInternal = 32
					gradeflash = 180
					lastGradeTime = engine.statistics.time
				} else engine.playSE("applause3")
			} else if(engine.statistics.level>=nextseclv) {
				// Next Section

				// Background切り替え
				owner.backgroundStatus.fadesw = true
				owner.backgroundStatus.fadecount = 0
				owner.backgroundStatus.fadebg = 10+nextseclv/100

				// BGM切り替え
				if(tableBGMChange[bgmlv]!=-1&&engine.statistics.level>=tableBGMChange[bgmlv]) {
					bgmlv++
					owner.bgmStatus.fadesw = false
					owner.bgmStatus.bgm = tableBGM[bgmlv]
					engine.playSE("levelup_section")
				} else engine.playSE("levelup")

				// Section Timeを記録
				sectionlasttime = sectionTime[levelb/100]
				sectionscomp++
				setAverageSectionTime()

				// 消えRoll check
				mrollCheck(levelb)
				if(engine.statistics.time>M_ROLL_TIME_REQUIRE) mrollSectiontime = false
				mrollFlag = mrollSectiontime
				if(mrollFlag&&mrollFourline) engine.playSE("cool")
				// ST medal
				stMedalCheck(engine, levelb/100)

				// RO medal
				if(nextseclv==300||nextseclv==700) roMedalCheck(engine)

				// Update level for next section
				nextseclv += 100
				if(nextseclv>999) nextseclv = 999
			} else if(engine.statistics.level==nextseclv-1&&lvstopse) engine.playSE("levelstop")

			// Calculate score
			lastscore =
				((((levelb+lines)/(if(engine.b2b) 3 else 4)+engine.softdropFall+(if(engine.manualLock) 1 else 0)+harddropBonus)*lines
					*comboValue*if(engine.field!!.isEmpty) 4 else 1)+engine.statistics.level/2
					+maxOf(0, engine.lockDelay-engine.lockDelayNow)*7)
			engine.statistics.scoreLine += lastscore
			return lastscore
		} else if(lines>=1&&mrollFlag&&engine.ending==2)
		// 消えRoll 中のLine clear
			mrollLines += lines
		return 0
	}

	/* Called when hard drop used */
	override fun afterHardDropFall(engine:GameEngine, playerID:Int, fall:Int) {
		if(fall*2>harddropBonus) harddropBonus = fall*2
	}

	/* 各 frame の終わりの処理 */
	override fun onLast(engine:GameEngine, playerID:Int) {
		// 段位上昇時のフラッシュ
		if(gradeflash>0) gradeflash--

		// 15分経過
		if(engine.statistics.time>=54000) setSpeed(engine)

		// Section Time増加
		if(engine.timerActive&&engine.ending==0) {
			val section = engine.statistics.level/100

			if(section>=0&&section<sectionTime.size) sectionTime[section]++

		}

		// Ending
		if(engine.gameActive&&engine.ending==2) {
			rolltime++

			// Time meter
			val remainRollTime = ROLLTIMELIMIT-rolltime
			engine.meterValue = remainRollTime*receiver.getMeterMax(engine)/ROLLTIMELIMIT
			engine.meterColor = GameEngine.METER_COLOR_LIMIT

			// Roll 終了
			if(rolltime>=ROLLTIMELIMIT) {
				rollclear = 2

				if(mrollFlag) {
					grade = 19
					gradeflash = 180
					lastGradeTime = engine.statistics.time
					engine.playSE("applause5")
					engine.playSE("gradeup")
					engine.playSE("grade4")
					gradeInternal = 33
					rollclear = 3
					if(mrollLines>=16) {
						rollclear = 4
						grade = 20
					}
				}

				engine.blockOutlineType = GameEngine.BLOCK_OUTLINE_NORMAL

				engine.gameEnded()
				engine.resetStatc()
				engine.stat = GameEngine.Status.EXCELLENT
				if(engine.statistics.time<28800) dectemp += (28800-engine.statistics.time)/1800
			}
		} else if(engine.statistics.level==nextseclv-1)
			engine.meterColor = if(engine.meterColor==-0x1)
				-0x10000
			else
				-0x1

	}

	/* game over */
	override fun onGameOver(engine:GameEngine, playerID:Int):Boolean {

		if(engine.statc[0]==0) {
			val time = engine.statistics.time
			if(time<6000)
				dectemp -= 3
			else {
				dectemp++
				if(time%3600<=60||time%3600>=3540) dectemp++
			}

			if(time>41100) dectemp -= 1+(time-41100)/1800

			// Blockの表示を元に戻す
			engine.blockOutlineType = GameEngine.BLOCK_OUTLINE_NORMAL
			// 裏段位
			secretGrade = engine.field!!.secretGrade
			decoration += dectemp+secretGrade
		}

		return false
	}

	/* 結果画面 */
	override fun renderResult(engine:GameEngine, playerID:Int) {
		receiver.drawMenuFont(engine, playerID, 0, 0, "\u0090\u0093 PAGE${engine.statc[1]+1}/3", COLOR.RED)

		when(engine.statc[1]) {
			0 -> {
				var gcolor = COLOR.WHITE
				if(rollclear==1||rollclear==3) gcolor = COLOR.GREEN
				if(rollclear==2||rollclear==4) gcolor = COLOR.ORANGE
				receiver.drawMenuFont(engine, playerID, 0, 3, "GRADE", COLOR.BLUE)
				receiver.drawMenuGrade(engine, playerID, 6, 2, tableGradeName[grade], gcolor, 2f)
				receiver.drawMenuGrade(engine, playerID, 3, 2, tableDetailGradeName[gradeInternal], gcolor)

				drawResultStats(engine, playerID, receiver, 4, COLOR.BLUE, Statistic.SCORE, Statistic.LINES, Statistic.LEVEL_MANIA,
					Statistic.TIME)
				drawResultRank(engine, playerID, receiver, 13, COLOR.BLUE, rankingRank)
				if(secretGrade>4)
					drawResult(engine, playerID, receiver, 15, COLOR.BLUE, "S. GRADE",
						String.format("%10s", tableSecretGradeName[secretGrade-1]))
			}
			1 -> {
				receiver.drawMenuFont(engine, playerID, 0, 2, "SECTION", COLOR.BLUE)

				for(i in sectionTime.indices)
					if(sectionTime[i]>0)
						receiver.drawMenuNum(engine, playerID, 2, 3+i, GeneralUtil.getTime(sectionTime[i]), sectionIsNewRecord[i])

				if(sectionavgtime>0) {
					receiver.drawMenuFont(engine, playerID, 0, 14, "AVERAGE", COLOR.BLUE)
					receiver.drawMenuNum(engine, playerID, 0, 15, GeneralUtil.getTime(sectionavgtime), 1.7f)
				}
			}
			2 -> {
				receiver.drawMenuFont(engine, playerID, 0, 2, "MEDAL", COLOR.BLUE)
				receiver.drawMenuMedal(engine, playerID, 5, 2, "SK", medalSK)
				receiver.drawMenuMedal(engine, playerID, 8, 2, "ST", medalST)
				receiver.drawMenuMedal(engine, playerID, 1, 3, "AC", medalAC)
				receiver.drawMenuMedal(engine, playerID, 4, 3, "CO", medalCO)
				receiver.drawMenuMedal(engine, playerID, 7, 3, "RE", medalRE)
				receiver.drawMenuMedal(engine, playerID, 8, 4, "RO", medalRO)

				drawResultStats(engine, playerID, receiver, 4, COLOR.BLUE, Statistic.LPM, Statistic.SPM, Statistic.PIECE,
					Statistic.PPS)

				drawResult(engine, playerID, receiver, 15, COLOR.BLUE, "DECORATION", String.format("%10d", dectemp))
			}
		}
	}

	override fun renderExcellent(engine:GameEngine, playerID:Int) {
		val offsetX = receiver.fieldX(engine, playerID)
		val offsetY = receiver.fieldY(engine, playerID)
		var col = COLOR.WHITE

		if(grade>=19) {
			col = when {
				engine.statc[0]%4==0 -> COLOR.YELLOW
				engine.statc[0]%2==0 -> col
				else -> COLOR.ORANGE
			}
			receiver.drawDirectFont(offsetX+12, offsetY+230, "YOU ARE A", COLOR.WHITE, 1f)
			receiver.drawDirectFont(offsetX+22, offsetY+250, "GRAND", col, 1.5f)
			receiver.drawDirectFont(offsetX+12, offsetY+274, "MASTER", col, 1.5f)
			if(grade==19) {
				col = when {
					engine.statc[0]%4==0 -> COLOR.RED
					engine.statc[0]%2==0 -> COLOR.YELLOW
					else -> COLOR.ORANGE
				}
				receiver.drawDirectFont(offsetX+20, offsetY+266, "LET'S TRY", COLOR.BLUE, 1f)
				receiver.drawDirectFont(offsetX+4, offsetY+282, "MORE LINES", col, 1f)
				receiver.drawDirectFont(offsetX+12, offsetY+292, "IN STEALTH", COLOR.WHITE, 1f)
			}
		} else if(grade>=17) {
			receiver.drawDirectFont(offsetX+44, offsetY+250, "BUT...", COLOR.WHITE, 1f)
			if(mrollSectiontime&&!mrollFourline) {
				col = when {
					engine.statc[0]%4==0 -> COLOR.RED
					engine.statc[0]%2==0 -> COLOR.YELLOW
					else -> COLOR.ORANGE
				}
				receiver.drawDirectFont(offsetX+20, offsetY+266, "CHALLENGE", COLOR.BLUE, 1f)
				receiver.drawDirectFont(offsetX+4, offsetY+282, "MORE QUADS", col, 1f)
				receiver.drawDirectFont(offsetX+12, offsetY+292, "NEXT TIME", COLOR.WHITE, 1f)
			} else {
				col = when {
					engine.statc[0]%4==0 -> COLOR.CYAN
					engine.statc[0]%2==0 -> col
					else -> COLOR.BLUE
				}
				receiver.drawDirectFont(offsetX+12, offsetY+266, "CHALLENGE", COLOR.BLUE, 1f)
				receiver.drawDirectFont(offsetX-4, offsetY+282, "MORE FASTER", col, 1f)
				receiver.drawDirectFont(offsetX+12, offsetY+292, "NEXT TIME", COLOR.WHITE, 1f)
			}
		}
		super.renderExcellent(engine, playerID)
	}

	/* 結果画面の処理 */
	override fun onResult(engine:GameEngine, playerID:Int):Boolean {

		owner.bgmStatus.fadesw = false
		owner.bgmStatus.bgm = if(engine.ending>0)
			if(rollclear<=1) BGM.Result(2) else BGM.Result(3)
		else BGM.Result(0)
		// ページ切り替え
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
		// section time display切替
		if(engine.ctrl.isPush(Controller.BUTTON_F)) {
			engine.playSE("change")
			isShowBestSectionTime = !isShowBestSectionTime
		}

		return false
	}

	/* リプレイ保存 */
	override fun saveReplay(engine:GameEngine, playerID:Int, prop:CustomProperties) {
		saveSetting(owner.replayProp)
		owner.replayProp.setProperty("result.grade.name", tableGradeName[grade])
		owner.replayProp.setProperty("result.grade.number", grade)
		owner.replayProp.setProperty("grademania2.version", version)

		// Update rankings
		if(!owner.replayMode&&startlevel==0&&!always20g&&!big&&engine.ai==null) {
			updateRanking(grade, engine.statistics.level, lastGradeTime, rollclear)
			if(medalST==3) updateBestSectionTime()

			if(rankingRank!=-1||medalST==3) {
				saveRanking(engine.ruleopt.strRuleName)
				owner.saveModeConfig()
			}
			owner.modeConfig.setProperty("decoration", decoration)
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
		for(i in 0 until SECTION_MAX) {
			bestSectionTime[i] = prop.getProperty("$ruleName.sectiontime.$i", DEFAULT_SECTION_TIME)
			bestSectionQuads[i] = prop.getProperty("$ruleName.sectionquads.$i", 0)
		}

		decoration = owner.statsProp.getProperty("decoration", 0)
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
			listOf("$ruleName.sectiontime.$i" to bestSectionTime[i],
				"$ruleName.sectionquads.$i" to bestSectionQuads[i])
		})

		owner.statsProp.setProperty("decoration", decoration)
		receiver.saveProperties(owner.statsFile, owner.statsProp)
	}

	/** Update rankings
	 * @param gr 段位
	 * @param lv level
	 * @param time Time
	 */
	private fun updateRanking(gr:Int, lv:Int, time:Int, clear:Int) {
		rankingRank = checkRanking(gr, lv, time, clear)

		if(rankingRank!=-1) {
			// Shift down ranking entries
			for(i in RANKING_MAX-1 downTo rankingRank+1) {
				rankingGrade[i] = rankingGrade[i-1]
				rankingLevel[i] = rankingLevel[i-1]
				rankingTime[i] = rankingTime[i-1]
				rankingRollclear[i] = rankingRollclear[i-1]
			}

			// Add new data
			rankingGrade[rankingRank] = gr
			rankingLevel[rankingRank] = lv
			rankingTime[rankingRank] = time
			rankingRollclear[rankingRank] = clear
		}
	}

	/** Calculate ranking position
	 * @param gr 段位
	 * @param lv level
	 * @param time Time
	 * @return Position (-1 if unranked)
	 */
	private fun checkRanking(gr:Int, lv:Int, time:Int, clear:Int):Int {
		for(i in 0 until RANKING_MAX)
			if(clear>rankingRollclear[i]) return i
			else if(clear==rankingRollclear[i]&&gr>rankingGrade[i]) return i
			else if(clear==rankingRollclear[i]&&gr==rankingGrade[i]&&lv>rankingLevel[i]) return i
			else if(clear==rankingRollclear[i]&&gr==rankingGrade[i]&&lv==rankingLevel[i]&&time<rankingTime[i]) return i

		return -1
	}

	/** Update best section time records */
	private fun updateBestSectionTime() {
		for(i in 0 until SECTION_MAX)
			if(sectionIsNewRecord[i]) {
				bestSectionTime[i] = sectionTime[i]
				bestSectionQuads[i] = sectionquads[i]
			}

	}

	companion object {
		/** Current version */
		private const val CURRENT_VERSION = 2

		/** 落下速度 table */
		private val tableGravityValue =
			intArrayOf(4, 6, 8, 10, 12, 16, 32, 48, 64, 80, 96, 112, 128, 144, 4, 32, 64, 96, 128, 160, 192, 224, 256, 512, 768,
				1024, 1280, 1024, 768, -1)

		/** 落下速度が変わる level */
		private val tableGravityChangeLevel =
			intArrayOf(30, 35, 40, 50, 60, 70, 80, 90, 100, 120, 140, 160, 170, 200, 220, 230, 233, 236, 239, 243, 247, 251, 300,
				330, 360, 400, 420, 450, 500, 10000)

		/** ARE table */
		private val tableARE = intArrayOf(25, 25, 24, 23, 22, 21, 19, 16, 14, 12)

		/** ARE after line clear table */
		private val tableARELine = intArrayOf(25, 25, 24, 22, 20, 18, 15, 12, 9, 6)

		/** Line clear time table */
		private val tableLineDelay = intArrayOf(40, 38, 35, 31, 25, 20, 16, 12, 9, 6)

		/** 固定 time table */
		private val tableLockDelay = intArrayOf(30, 30, 30, 30, 30, 30, 28, 26, 24, 20)

		/** DAS table */
		private val tableDAS = intArrayOf(15, 14, 13, 12, 11, 10, 9, 8, 7, 6)

		/** BGM fadeout levels */
		private val tableBGMFadeout = intArrayOf(475, 680, 880, -1)

		/** BGM change levels */
		private val tableBGMChange = intArrayOf(500, 700, 900, -1)
		private val tableBGM = arrayOf(BGM.GrandA(0), BGM.GrandA(1), BGM.GrandA(2), BGM.GrandA(3))

		/** Line clear時に入る段位 point */
		private val tableGradePoint =
			arrayOf(intArrayOf(10, 10, 10, 10, 10, 9, 8, 7, 6, 5, 4, 3, 2),
				intArrayOf(20, 20, 20, 18, 16, 15, 13, 10, 11, 11, 12), intArrayOf(40, 36, 33, 30, 27, 24, 20, 18, 17, 16, 15),
				intArrayOf(50, 47, 44, 40, 40, 38, 36, 34, 32, 31, 30))

		/** 段位 pointのCombo bonus */
		private val tableGradeComboBonus =
			arrayOf(floatArrayOf(1.0f, 1.2f, 1.2f, 1.4f, 1.4f, 1.4f, 1.4f, 1.5f, 1.5f, 2.0f),
				floatArrayOf(1.0f, 1.4f, 1.5f, 1.6f, 1.7f, 1.8f, 1.9f, 2.0f, 2.1f, 2.5f),
				floatArrayOf(1.0f, 1.5f, 1.8f, 2.0f, 2.2f, 2.3f, 2.4f, 2.5f, 2.6f, 3.0f),
				floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f))

		/** 実際の段位を上げるのに必要な内部段位 */
		private val tableGradeChange = intArrayOf(1, 2, 3, 4, 5, 7, 9, 12, 15, 18, 19, 20, 23, 25, 27, 29, 31, -1)

		/** 段位 pointが1つ減る time */
		private val tableGradeDecayRate =
			intArrayOf(125, 100, 80, 50, 48, 47, 45, 44, 43, 42, 41, 40, 36, 33, 30, 28, 26, 24, 22, 20, 19, 18, 17, 16, 15, 15,
				14, 14, 13, 13, 11, 10)

		/** 段位のName */
		private val tableGradeName = arrayOf("9", "8", "7", "6", "5", "4", "3", "2", "1", // 0～8
			"S1", "S2", "S3", "S4", "S5", "S6", "S7", "S8", "S9", // 9～17
			"m", "Gm", "GM" // 18～20
		)

		/** 詳細段位のName */
		private val tableDetailGradeName = arrayOf("9", "8", "7", "6", "5", "4", "3", "2", "1", // 0～8
			"S1", "S2", "S3", "S4", "S5", "S6", "S7", "S8", "S9", // 9～17
			"M1", "M2", "M3", "M4", "M5", "M6", "M7", "M8", "M9", // 18～26
			"M", "MK", "MV", "MO", "MM", "Gm", "GM" // 27～33
		)

		/** 裏段位のName */
		private val tableSecretGradeName = arrayOf("9", "8", "7", "6", "5", "4", "3", "2", "1", // 0～8
			"S1", "S2", "S3", "S4", "S5", "S6", "S7", "S8", "S9", // 9～17
			"GM" // 18
		)

		/** LV999 roll time */
		private const val ROLLTIMELIMIT = 3590

		/** 消えRoll に必要なLV999到達時のTime */
		private const val M_ROLL_TIME_REQUIRE = 37440

		/** Number of entries in rankings */
		private const val RANKING_MAX = 10

		/** Number of sections */
		private const val SECTION_MAX = 10

		/** Default section time */
		private const val DEFAULT_SECTION_TIME = 5400
	}
}
