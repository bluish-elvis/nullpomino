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
package mu.nu.nullpo.game.play

import mu.nu.nullpo.game.component.*
import mu.nu.nullpo.game.component.BGMStatus.BGM
import mu.nu.nullpo.game.component.SpeedParam.Companion.SDS_FIXED
import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.game.subsystem.ai.DummyAI
import mu.nu.nullpo.game.subsystem.wallkick.Wallkick
import mu.nu.nullpo.gui.common.PopupCombo.CHAIN
import mu.nu.nullpo.util.GeneralUtil
import mu.nu.nullpo.util.toInt
import net.omegaboshi.nullpomino.game.subsystem.randomizer.MemorylessRandomizer
import net.omegaboshi.nullpomino.game.subsystem.randomizer.Randomizer
import org.apache.log4j.Logger
import java.util.*
import kotlin.random.Random

/** Each player's Game processing */
@Suppress("MemberVisibilityCanBePrivate")
class GameEngine
/** Constructor
 * @param owner このゲームエンジンを所有するGameOwnerクラス
 * @param playerID Playerの number
 * @param ruleopt ルール設定
 * @param wallkick Wallkickシステム
 * @param randomizer Blockピースの出現順の生成アルゴリズム
 */
(
	/** GameManager: Owner of this GameEngine */
	val owner:GameManager,
	/** Player ID (0=1P) */
	val playerID:Int,
	/** RuleOptions: Most game settings are here */
	var ruleopt:RuleOptions = RuleOptions(),
	/** Wallkick: The wallkick system */
	var wallkick:Wallkick? = null,
	/** Randomizer: Used by creation of next piece sequence */
	var randomizer:Randomizer = MemorylessRandomizer()
) {

	/** Field: The playfield */
	var field:Field? = null

	/** Controller: You can get player's input from here */
	var ctrl:Controller = Controller()

	/** Statistics: Various game statistics
	 * such as score, number of lines, etc */
	val statistics:Statistics = Statistics()

	/** SpeedParam: Parameters of game speed
	 * (Gravity, ARE, Line clear delay, etc) */
	val speed:SpeedParam = SpeedParam()

	/** Gravity counter (The piece falls when this reaches
	 * to the value of speed.denominator) */
	var gcount:Int = 0; private set

	/** The first random-seed */
	var randSeed:Long = 0

	/** Random: Used for creating various randomness */
	var random:Random = Random.Default

	/** ReplayData: Manages input data for replays */
	var replayData:ReplayData = ReplayData()

	/** AIPlayer: AI for auto playing */
	var ai:DummyAI? = null

	/** AI move delay */
	var aiMoveDelay:Int = 0

	/** AI think delay (Only when using thread) */
	var aiThinkDelay:Int = 0

	/** Use thread for AI */
	var aiUseThread:Boolean = false

	/** Show Hint with AI */
	var aiShowHint:Boolean = false

	/** Prethink with AI */
	var aiPrethink:Boolean = false

	/** Show internal state of AI */
	var aiShowState:Boolean = false

	/** AI Hint piece (copy of current or hold) */
	var aiHintPiece:Piece? = null

	/** AI Hint X position */
	var aiHintX:Int = 0

	/** AI Hint Y position */
	var aiHintY:Int = 0

	/** AI Hint piece direction */
	var aiHintRt:Int = 0

	/** True if AI Hint is ready */
	var aiHintReady:Boolean = false

	/** Current main game status */
	lateinit var stat:Status

	/** Free status counters */
	var statc:IntArray = IntArray(MAX_STATC)

	/** true if game play, false if menu.
	 * Used for alternate keyboard mappings. */
	var isInGame:Boolean = false

	/** true if the game is active */
	var gameActive:Boolean = false

	/** true if the timer is active */
	var timerActive:Boolean = false

	/** true if the game is started
	 * (It will not change back to false until the game is reset) */
	var gameStarted:Boolean = false

	/** Timer for replay */
	var replayTimer:Int = 0; private set

	/** Time of game start in milliseconds */
	var startTime:Long = 0

	/** Time of game end in milliseconds */
	var endTime:Long = 0

	/** Major version */
	var versionMajor:Float = 0f

	/** Minor version */
	var versionMinor:Int = 0

	/** OLD minor version (Used for 6.9 or earlier replays) */
	var versionMinorOld:Float = 0f

	/** Dev build flag */
	var versionIsDevBuild:Boolean = false

	/** Game quit flag */
	var quitflag:Boolean = false

	/** Piece object of current piece */
	var nowPieceObject:Piece? = null

	/** X coord of current piece */
	var nowPieceX:Int = 0

	/** Y coord of current piece */
	var nowPieceY:Int = 0

	/** Bottommost Y coord of current piece
	 * (Used for ghost piece and harddrop) */
	var nowPieceBottomY:Int = 0

	/** Write anything other than -1 to override whole current piece cint */
	var nowPieceColorOverride:Int = 0

	/** Allow/Disallow certain piece */
	var nextPieceEnable:BooleanArray = BooleanArray(Piece.PIECE_COUNT) {i:Int -> i<Piece.PIECE_STANDARD_COUNT}

	/** Preferred size of next piece array
	 * Might be ignored by certain Randomizer. (Default:1400) */
	var nextPieceArraySize:Int = 0

	/** Array of next piece IDs */
	var nextPieceArrayID:IntArray = IntArray(0)

	/** Array of next piece Objects */
	var nextPieceArrayObject:Array<Piece?> = emptyArray()

	/** Number of pieces put (Used by next piece sequence) */
	var nextPieceCount:Int = 0

	/** Hold piece (null: None) */
	var holdPieceObject:Piece? = null

	/** true if hold is disabled because player used it already */
	var holdDisable:Boolean = false

	/** Number of holds used */
	var holdUsedCount:Int = 0

	/** Number of lines currently clearing */
	var lineClearing:Int = 0
	var garbageClearing:Int = 0

	/** Line gravity type (Native, Cascade, etc) */
	var lineGravityType:LineGravity = LineGravity.NATIVE

	/** Current number of chains */
	var chain:Int = 0

	/** Number of lines cleared for this chains */
	var lineGravityTotalLines:Int = 0

	/** Lock delay counter */
	var lockDelayNow:Int = 0

	/** DAS counter */
	var dasCount:Int = 0

	/** DAS direction (-1:Left 0:None 1:Right) */
	var dasDirection:Int = 0

	/** DAS delay counter */
	var dasSpeedCount:Int = 0

	/** Repeat statMove() for instant DAS */
	var dasRepeat:Boolean = false

	/** In the middle of an instant DAS loop */
	var dasInstant:Boolean = false

	/** Disallow shift while locking key is pressed */
	var shiftLock:Int = 0

	/** IRS direction */
	var initialRotateDirection:Int = 0

	/** Last IRS direction */
	var initialRotateLastDirection:Int = 0

	/** IRS continuous use flag */
	var initialRotateContinuousUse:Boolean = false

	/** IHS */
	var initialHoldFlag:Boolean = false

	/** IHS continuous use flag */
	var initialHoldContinuousUse:Boolean = false

	/** Number of current piece movement */
	var nowPieceMoveCount:Int = 0

	/** Number of current piece rotations */
	var nowPieceRotateCount:Int = 0

	/** Number of current piece failed rotations */
	var nowPieceRotateFailCount:Int = 0

	/** Number of movement while touching to the floor */
	var extendedMoveCount:Int = 0

	/** Number of rotations while touching to the floor */
	var extendedRotateCount:Int = 0

	/** Number of wallkicks used by current piece */
	var nowWallkickCount:Int = 0

	/** Number of upward wallkicks used by current piece */
	var nowUpwardWallkickCount:Int = 0

	/** Number of rows falled by soft drop (Used by soft drop bonuses) */
	var softdropFall:Int = 0

	/** Number of rows falled by hard drop (Used by soft drop bonuses) */
	var harddropFall:Int = 0

	/** fall per frame */
	var fpf:Int = 0

	/** Soft drop continuous use flag */
	var softdropContinuousUse:Boolean = false

	/** Hard drop continuous use flag */
	var harddropContinuousUse:Boolean = false

	/** true if the piece was manually locked by player */
	var manualLock:Boolean = false

	/** Last successful movement */
	lateinit var lastmove:LastMove
	var lastline:Int = 0

	/** Most recent scoring event type */
	var lastevent:ScoreEvent? = null
	val lasteventshape:Piece.Shape? get() = lastevent?.piece?.type
	val lasteventpiece:Int get() = lastevent?.piece?.id ?: 0

	var lastlines:IntArray = IntArray(0)

	/** ture if last erased line is Splitted */
	var split:Boolean = false

	/** last Twister */
	var twistType:Twister? = null

	/** true if Twister */
	val twist:Boolean get() = twistType!=null

	/** true if Twister Mini */
	val twistmini:Boolean get() = twistType==Twister.IMMOBILE_MINI||twistType==Twister.POINT_MINI

	/** EZ Twister */
	val twistez:Boolean get() = twistType==Twister.IMMOBILE_EZ

	/** true if B2B */
	var b2b:Boolean = false

	/** B2B counter */
	var b2bcount:Int = 0
	var b2bbuf:Int = 0

	/** Number of combos */
	var combo:Int = 0
	var combobuf:Int = 0

	/** Twister enable flag */
	var twistEnable:Boolean = false

	/** EZ-T toggle */
	var twistEnableEZ:Boolean = false

	/** Allow Twister with wallkicks */
	var twistAllowKick:Boolean = false

	/** Twister Mini detection type */
	var twistminiType:Int = 0

	/** All Spins flag */
	var useAllSpinBonus:Boolean = false

	/** B2B enable flag */
	var b2bEnable:Boolean = false

	/** Does Split trigger B2B flag */
	var splitb2b:Boolean = false

	/** Combo type */
	var comboType:Int = 0

	/** Number of frames before placed blocks disappear (-1:Disable) */
	var blockHidden:Int = 0

	/** Use alpha-blending for blockHidden */
	var blockHiddenAnim:Boolean = false

	/** Outline type */
	var blockOutlineType:Int = 0

	/** Show outline only flag.
	 * If enabled it does not show actual image of blocks. */
	var blockShowOutlineOnly:Boolean = false

	/** Hebo-hidden Enable flag */
	var heboHiddenEnable:Boolean = false

	/** Hebo-hidden Timer */
	var heboHiddenTimerNow:Int = 0

	/** Hebo-hidden Timer Max */
	var heboHiddenTimerMax:Int = 0

	/** Hebo-hidden Y coord */
	var heboHiddenYNow:Int = 0

	/** Hebo-hidden Y coord Limit */
	var heboHiddenYLimit:Int = 0

	/** Set when ARE or line delay is canceled */
	var delayCancel:Boolean = false

	/** Piece must move left after canceled delay */
	var delayCancelMoveLeft:Boolean = false

	/** Piece must move right after canceled delay */
	var delayCancelMoveRight:Boolean = false

	/** Use bone blocks [][][][] */
	var bone:Boolean = false

	/** Big blocks */
	var big:Boolean = false

	/** Big movement type (false:1cell true:2cell) */
	var bigmove:Boolean = false

	/** Halves the amount of lines cleared in Big mode */
	var bighalf:Boolean = false

	/** true if wallkick is used */
	var kickused:Boolean = false

	/** Field size (-1:Default) */
	var fieldWidth:Int = 0
	var fieldHeight:Int = 0
	var fieldHiddenHeight:Int = 0

	/** Ending mode (0:During the normal game) */
	var ending:Int = 0

	/** Enable staffroll challenge (Credits) in ending */
	var staffrollEnable:Boolean = false

	/** Disable death in staffroll challenge */
	var staffrollNoDeath:Boolean = false

	/** Update various statistics in staffroll challenge */
	var staffrollEnableStatistics:Boolean = false

	/** Frame cint */
	var framecolor:Int = 0

	/** Duration of Ready->Go */
	var readyStart:Int = 0
	var readyEnd:Int = 0
	var goStart:Int = 0
	var goEnd:Int = 0

	/** true if Ready->Go is already done */
	var readyDone:Boolean = false

	/** Number of lives */
	var lives:Int = 0

	/** Ghost piece flag */
	var ghost:Boolean = false

	/** Amount of meter */
	var meterValue:Int = 0

	/** Color of meter */
	var meterColor:Int = 0

	/** Amount of meter (layer 2) */
	var meterValueSub:Int = 0

	/** Color of meter (layer 2) */
	var meterColorSub:Int = 0

	/** Lag flag (Infinite length of ARE will happen
	 * after placing a piece until this flag is set to false) */
	var lagARE:Boolean = false

	/** Lag flag (Pause the game completely) */
	var lagStop:Boolean = false

	/** Field display size (-1 for mini, 1 for big, 0 for normal) */
	var displaysize:Int = 0

	/** Sound effects enable flag */
	var enableSE:Boolean = false

	/** Stops all other players when this player dies */
	var gameoverAll:Boolean = false

	/** Field visible flag (false for invisible challenge) */
	var isVisible:Boolean = false

	/** Piece preview visible flag */
	var isNextVisible:Boolean = false

	/** Hold piece visible flag */
	var isHoldVisible:Boolean = false

	/** Field edit screen: Cursor coord */
	var fldeditX:Int = 0
	var fldeditY:Int = 0

	/** Field edit screen: Selected cint */
	var fldeditColor:Int = 0

	/** Field edit screen: Previous game status number */
	lateinit var fldeditPreviousStat:Status

	/** Field edit screen: Frame counter */
	var fldeditFrames:Int = 0

	/** Next-skip during Ready->Go */
	var holdButtonNextSkip:Boolean = false

	/** Allow default text rendering
	 * (such as "READY", "GO!", "GAME OVER",etc) */
	var allowTextRenderByReceiver:Boolean = false

	/** RollRoll (Auto rotation) enable flag */
	var itemRollRollEnable:Boolean = false

	/** RollRoll (Auto rotation) interval */
	var itemRollRollInterval:Int = 0

	/** X-RAY enable flag */
	var itemXRayEnable:Boolean = false

	/** X-RAY counter */
	var itemXRayCount:Int = 0

	/** Color-block enable flag */
	var itemColorEnable:Boolean = false

	/** Color-block counter */
	var itemColorCount:Int = 0

	/** Gameplay-interruptable item */
	var interruptItemNumber:Int = 0

	/** Post-status of interruptable item */
	var interruptItemPreviousStat:Status = Status.MOVE

	/** Backup field for Mirror item */
	var interruptItemMirrorField:Field? = null

	/** A button direction -1=Auto(Use rule settings) 0=Left 1=Right */
	var owRotateButtonDefaultRight:Int = -1

	/** Block Skin (-1=Auto -2=Random 0orAbove=Fixed) */
	var owSkin:Int = -1

	/** Min/Max DAS (-1=Auto 0orAbove=Fixed) */
	var owMinDAS:Int = -1
	var owMaxDAS:Int = -1

	/** ARR (-1=Auto 0orAbove=Fixed) */
	var owARR:Int = -1

	/** SoftDrop Speed -1(Below 0)=Auto 0=Always Instant  above=Fixed */
	var owSDSpd:Int = 0

	/** Reverse roles of up/down keys in-game */
	var owReverseUpDown:Boolean = false

	/** Diagonal move (-1=Auto 0=Disable 1=Enable) */
	var owMoveDiagonal:Int = -1

	/** Outline type (-1:Auto 0orAbove:Fixed) */
	var owBlockOutlineType:Int = 0

	/** Show outline only flag
	 * (-1:Auto 0:Always Normal 1:Always Outline Only) */
	var owBlockShowOutlineOnly:Int = 0

	/** Clear mode selection */
	var clearMode:ClearType = ClearType.LINE

	/** Size needed for a cint-group clear */
	var colorClearSize:Int = 0

	/** If true, cint clears will also clear adjacent garbage blocks. */
	var garbageColorClear:Boolean = false

	/** If true, each individual block is a random cint. */
	var randomBlockColor:Boolean = false

	/** If true, block in pieces are connected. */
	var connectBlocks:Boolean = false

	/** List of block colors to use for random block colors. */
	var blockColors:IntArray = BLOCK_COLORS_DEFAULT

	/** Number of colors in blockColors to use. */
	var numColors:Int = 0

	/** If true, line cint clears can be diagonal. */
	var lineColorDiagonals:Boolean = false

	/** If true, gems count as the same cint as
	 * their respectively-colored normal blocks */
	var gemSameColor:Boolean = false

	/** Delay for each step in cascade animations */
	var cascadeDelay:Int = 0

	/** Delay between landing and checking for clears in cascade */
	var cascadeClearDelay:Int = 0

	/** If true, cint clears will ignore hidden rows */
	var ignoreHidden:Boolean = false

	/** Set to true to process rainbow block effects, false to skip. */
	var rainbowAnimate:Boolean = false

	/** If true, the game will execute double rotation to I2 piece
	 * when regular rotation fails twice */
	var dominoQuickTurn:Boolean = false

	/** 0 = default, 1 = link by cint,
	 * 2 = link by cint but ignore links forcascade (Avalanche) */
	var sticky:Int = 0

	/** Hanabi発生間隔 */
	var temphanabi:Int = 0
	var inthanabi:Int = 0

	var explodSize:Array<IntArray> = EXPLOD_SIZE_DEFAULT

	/** Current AREの値を取得 (ルール設定も考慮）
	 * @return Current ARE
	 */
	val are:Int
		get() = if(speed.are<ruleopt.minARE&&ruleopt.minARE>=0) ruleopt.minARE
		else if(speed.are>ruleopt.maxARE&&ruleopt.maxARE>=0) ruleopt.maxARE else speed.are

	/** Current ARE after line clearの値を取得 (ルール設定も考慮）
	 * @return Current ARE after line clear
	 */
	val areLine:Int
		get() = if(speed.areLine<ruleopt.minARELine&&ruleopt.minARELine>=0) ruleopt.minARELine
		else if(speed.areLine>ruleopt.maxARELine&&ruleopt.maxARELine>=0) ruleopt.maxARELine else speed.areLine

	/** Current Line clear timeの値を取得 (ルール設定も考慮）
	 * @return Current Line clear time
	 */
	val lineDelay:Int
		get() = if(speed.lineDelay<ruleopt.minLineDelay&&ruleopt.minLineDelay>=0) ruleopt.minLineDelay
		else if(speed.lineDelay>ruleopt.maxLineDelay&&ruleopt.maxLineDelay>=0) ruleopt.maxLineDelay else speed.lineDelay

	/** Current 固定 timeの値を取得 (ルール設定も考慮）
	 * @return Current 固定 time
	 */
	val lockDelay:Int
		get() = if(speed.lockDelay<ruleopt.minLockDelay&&ruleopt.minLockDelay>=0) ruleopt.minLockDelay
		else if(speed.lockDelay>ruleopt.maxLockDelay&&ruleopt.maxLockDelay>=0) ruleopt.maxLockDelay else speed.lockDelay

	/** Current DASの値を取得 (ルール設定も考慮）
	 * @return Current DAS
	 */
	val das:Int
		get() = if(speed.das<owMinDAS&&owMinDAS>=0) owMinDAS
		else if(speed.das>owMaxDAS&&owMaxDAS>=0) owMaxDAS
		else if(speed.das<ruleopt.minDAS&&ruleopt.minDAS>=0) ruleopt.minDAS
		else if(speed.das>ruleopt.maxDAS&&ruleopt.maxDAS>=0) ruleopt.maxDAS else speed.das

	/** Current SoftDropの形式を取得 (ルール設定も考慮）
	 * @return false:固定値 true:倍率
	 */
	val sdMul:Boolean get() = if(owSDSpd<0) ruleopt.softdropMultiplyNativeSpeed else owSDSpd>=SDS_FIXED.size

	/** Current SoftDrop速度を取得 (ルール設定も考慮）
	 * @return Current DAS
	 */
	val softDropSpd:Float
		get() {
			return when {
				owSDSpd<0 -> ruleopt.softdropSpeed
				else -> (if(owSDSpd<SDS_FIXED.size) SDS_FIXED[owSDSpd] else owSDSpd-SDS_FIXED.size+5f)
			}*(if(sdMul||speed.denominator<=0) speed.gravity else speed.denominator).toFloat()

		}

	/** @return Controller.BUTTON_UP if controls are normal,
	 * Controller.BUTTON_DOWN if up/down are reversed
	 */
	val up:Int
		get() = if(owReverseUpDown) Controller.BUTTON_DOWN else Controller.BUTTON_UP

	/** @return Controller.BUTTON_DOWN if controls are normal,
	 * Controller.BUTTON_UP if up/down are reversed
	 */
	val down:Int
		get() = if(owReverseUpDown) Controller.BUTTON_UP else Controller.BUTTON_DOWN

	/** Current 横移動速度を取得
	 * @return 横移動速度
	 */
	val dasDelay:Int
		get() = if(owARR>=0) owARR else ruleopt.dasARR

	/** 現在使用中のBlockスキン numberを取得
	 * @return Blockスキン number
	 */
	val skin:Int
		get() = if(owSkin>=0) owSkin else ruleopt.skin

	/** @return A buttonを押したときに左rotationするならfalse, 右rotationするならtrue
	 */
	val isRotateButtonDefaultRight:Boolean
		get() = if(owRotateButtonDefaultRight>=0) owRotateButtonDefaultRight!=0 else ruleopt.rotateButtonDefaultRight

	/** Is diagonal movement enabled?
	 * @return true if diagonal movement is enabled
	 */
	val isDiagonalMoveEnabled:Boolean
		get() = if(owMoveDiagonal>=0) owMoveDiagonal==1 else ruleopt.moveDiagonal

	/** 横移動 input のDirectionを取得
	 * @return -1:左 0:なし 1:右
	 */
	val moveDirection:Int
		get() =
			if(ruleopt.moveLeftAndRightAllow&&ctrl.isPress(Controller.BUTTON_LEFT)&&ctrl.isPress(Controller.BUTTON_RIGHT)) {
				when {
					ctrl.buttonTime[Controller.BUTTON_LEFT]>ctrl.buttonTime[Controller.BUTTON_RIGHT] ->
						if(ruleopt.moveLeftAndRightUsePreviousInput) -1 else 1
					ctrl.buttonTime[Controller.BUTTON_LEFT]<ctrl.buttonTime[Controller.BUTTON_RIGHT] ->
						if(ruleopt.moveLeftAndRightUsePreviousInput) 1 else -1
					else -> 0
				}
			} else if(ctrl.isPress(Controller.BUTTON_LEFT)) -1 else if(ctrl.isPress(Controller.BUTTON_RIGHT)) 1 else 0

	/** 移動 count制限を超過しているか判定
	 * @return 移動 count制限を超過したらtrue
	 */
	val isMoveCountExceed:Boolean
		get() = if(ruleopt.lockresetLimitShareCount) {
			extendedMoveCount+extendedRotateCount>=ruleopt.lockresetLimitMove&&ruleopt.lockresetLimitMove>=0
		} else ruleopt.lockresetLimitMove in 0..extendedMoveCount

	/** rotation count制限を超過しているか判定
	 * @return rotation count制限を超過したらtrue
	 */
	val isRotateCountExceed:Boolean
		get() = if(ruleopt.lockresetLimitShareCount) {
			extendedMoveCount+extendedRotateCount>=ruleopt.lockresetLimitMove&&ruleopt.lockresetLimitMove>=0
		} else ruleopt.lockresetLimitRotate in 0..extendedRotateCount

	/** ホールド可能かどうか判定
	 * @return ホールド可能ならtrue
	 */
	val isHoldOK:Boolean
		get() = (ruleopt.holdEnable&&!holdDisable&&(holdUsedCount<ruleopt.holdLimit||ruleopt.holdLimit<0)
			&&!initialHoldContinuousUse)

	/** READY前のInitialization */
	fun init() {
		log.debug("GameEngine init() playerID:$playerID")

		field = null
		statistics.reset()
		speed.reset()
		gcount = 0
		owner.receiver.loadProperties(owner.recorder(ruleopt.strRuleName))?.let {owner.recordProp = it}
		replayData = ReplayData()

		if(!owner.replayMode) {
			versionMajor = GameManager.versionMajor
			versionMinor = GameManager.versionMinor
			versionMinorOld = GameManager.versionMinorOld

			val tempRand = Random.Default
			randSeed = tempRand.nextLong()
			if(owSkin==-2) owSkin = tempRand.nextInt(owner.receiver.skinMax)

			random = Random(randSeed)
		} else {
			versionMajor = owner.replayProp.getProperty("version.core.major", 0f)
			versionMinor = owner.replayProp.getProperty("version.core.minor", 0)
			versionMinorOld = owner.replayProp.getProperty("version.core.minor", 0f)

			replayData.readProperty(owner.replayProp, playerID)

			owRotateButtonDefaultRight =
				owner.replayProp.getProperty("$playerID.tuning.owRotateButtonDefaultRight", -1)
			owSkin = owner.replayProp.getProperty("$playerID.tuning.owSkin", -1)
			owMinDAS = owner.replayProp.getProperty("$playerID.tuning.owMinDAS", -1)
			owMaxDAS = owner.replayProp.getProperty("$playerID.tuning.owMaxDAS", -1)
			owARR = owner.replayProp.getProperty("$playerID.tuning.owDasDelay", -1)
			owReverseUpDown = owner.replayProp.getProperty("$playerID.tuning.owReverseUpDown", false)
			owMoveDiagonal = owner.replayProp.getProperty("$playerID.tuning.owMoveDiagonal", -1)

			val tempRand = owner.replayProp.getProperty("$playerID.replay.randSeed", "0")
			randSeed = tempRand.toLong(16)
			random = Random(randSeed)

		}

		quitflag = false

		stat = Status.SETTING
		statc = IntArray(MAX_STATC)

		lastevent = null
		lastlines = IntArray(0)
		lastline = fieldHeight

		isInGame = false
		gameActive = false
		timerActive = false
		gameStarted = false
		replayTimer = 0

		nowPieceObject = null
		nowPieceX = 0
		nowPieceY = 0
		nowPieceBottomY = 0
		nowPieceColorOverride = -1

		nextPieceArraySize = 1400
		nextPieceEnable = BooleanArray(Piece.PIECE_COUNT) {it<Piece.PIECE_STANDARD_COUNT}
		nextPieceArrayID = IntArray(0)
		nextPieceArrayObject = emptyArray()
		nextPieceCount = 0

		holdPieceObject = null
		holdDisable = false
		holdUsedCount = 0

		lineClearing = 0
		garbageClearing = 0
		lineGravityType = LineGravity.NATIVE
		chain = 0
		lineGravityTotalLines = 0

		lockDelayNow = 0

		dasCount = 0
		dasDirection = 0
		dasSpeedCount = dasDelay
		dasRepeat = false
		dasInstant = false
		shiftLock = 0

		initialRotateDirection = 0
		initialRotateLastDirection = 0
		initialHoldFlag = false
		initialRotateContinuousUse = false
		initialHoldContinuousUse = false

		nowPieceMoveCount = 0
		nowPieceRotateCount = 0
		nowPieceRotateFailCount = 0

		extendedMoveCount = 0
		extendedRotateCount = 0

		nowWallkickCount = 0
		nowUpwardWallkickCount = 0

		softdropFall = 0
		harddropFall = 0
		softdropContinuousUse = false
		harddropContinuousUse = false

		manualLock = false

		lastmove = LastMove.NONE
		split = false
		twistType = null
		b2b = false
		b2bbuf = 0
		b2bcount = b2bbuf
		combobuf = 0
		combo = combobuf
		inthanabi = 0
		temphanabi = inthanabi

		twistEnable = false
		twistEnableEZ = false
		twistAllowKick = true
		twistminiType = TWISTMINI_TYPE_ROTATECHECK
		useAllSpinBonus = false
		b2bEnable = false
		splitb2b = false
		comboType = COMBO_TYPE_DISABLE

		blockHidden = -1
		blockHiddenAnim = true
		blockOutlineType = BLOCK_OUTLINE_NORMAL
		blockShowOutlineOnly = false

		heboHiddenEnable = false
		heboHiddenTimerNow = 0
		heboHiddenTimerMax = 0
		heboHiddenYNow = 0
		heboHiddenYLimit = 0

		delayCancel = false
		delayCancelMoveLeft = false
		delayCancelMoveRight = false

		bone = false

		big = false
		bigmove = true
		bighalf = true

		kickused = false

		fieldWidth = -1
		fieldHeight = -1
		fieldHiddenHeight = -1

		ending = 0
		staffrollEnable = false
		staffrollNoDeath = false
		staffrollEnableStatistics = false

		framecolor = FRAME_COLOR_WHITE

		readyStart = READY_START
		readyEnd = READY_END
		goStart = GO_START
		goEnd = GO_END

		readyDone = false

		lives = 0

		ghost = true

		meterValue = 0
		meterColor = METER_COLOR_RED

		lagARE = false
		lagStop = false
		displaysize = if(playerID>=2) -1 else 0

		enableSE = true
		gameoverAll = true

		isNextVisible = true
		isHoldVisible = true
		isVisible = true

		holdButtonNextSkip = false

		allowTextRenderByReceiver = true

		itemRollRollEnable = false
		itemRollRollInterval = 30

		itemXRayEnable = false
		itemXRayCount = 0

		itemColorEnable = false
		itemColorCount = 0

		interruptItemNumber = INTERRUPTITEM_NONE

		clearMode = ClearType.LINE
		colorClearSize = -1
		garbageColorClear = false
		ignoreHidden = false
		connectBlocks = true
		lineColorDiagonals = false
		blockColors = BLOCK_COLORS_DEFAULT
		cascadeDelay = 0
		cascadeClearDelay = 0

		rainbowAnimate = false

		startTime = 0
		endTime = 0

		//  event 発生
		owner.mode?.let {
			it.playerInit(this, playerID)
			if(owner.replayMode) it.loadReplay(this, playerID, owner.replayProp)
			else it.loadRanking(owner.recordProp, ruleopt.strRuleName)

		}
		owner.receiver.playerInit(this, playerID)
		ai?.also {
			it.shutdown()
			it.init(this, playerID)
		}
	}

	/** 終了処理 */
	fun shutdown() {
		//log.debug("GameEngine shutdown() playerID:" + playerID);

		ai?.shutdown()
		/*owner = null
		ruleopt = null
		wallkick = null
		randomizer = null
		field = null
		statistics = null
		speed = null
		random = null
		replayData = null*/
	}

	/** ステータス counterInitialization */
	fun resetStatc() {
		for(i in statc.indices)
			statc[i] = 0
	}

	/** Sound effectsを再生する (enableSEがtrueのときだけ）
	 * @param name Sound effectsのName
	 */

	fun playSE(name:String, freq:Float = 1f, vol:Float = 1f) {
		if(enableSE) owner.receiver.playSE(name, freq, vol)
	}

	fun loopSE(name:String, freq:Float = 1f, vol:Float = 1f) {
		if(enableSE) owner.receiver.loopSE(name, freq, vol)
	}

	fun stopSE(name:String) {
		owner.receiver.stopSE(name)
	}

	/** NEXTピースのIDを取得
	 * @param c 取得したいNEXTの位置
	 * @return NEXTピースのID
	 */
	fun getNextID(c:Int):Int {
		if(nextPieceArrayObject.isNullOrEmpty()) return Piece.PIECE_NONE
		nextPieceArrayID.let {
			var c2 = c
			while(it.size in 1..c2) c2 -= it.size
			return it[c2]
		}
	}

	/** NEXTピースのオブジェクトを取得
	 * @param c 取得したいNEXTの位置
	 * @return NEXTピースのオブジェクト
	 */
	fun getNextObject(c:Int):Piece? {
		if(nextPieceArrayObject.isNullOrEmpty()) return null
		nextPieceArrayObject.let {
			var c2 = c
			while(it.size in 1..c2)
				c2 -= it.size
			return it[c2]
		}
	}

	/** NEXTピースのオブジェクトのコピーを取得
	 * @param c 取得したいNEXTの位置
	 * @return NEXTピースのオブジェクトのコピー
	 */
	fun getNextObjectCopy(c:Int):Piece? = getNextObject(c)?.let {Piece(it)}

	/** 見え／消えRoll 状態のfieldを通常状態に戻す */
	fun resetFieldVisible() {
		field?.let {f ->
			for(x in 0 until f.width) for(y in 0 until f.height)
				f.getBlock(x, y)?.run {
					if(color!=null) {
						alpha = 1f
						darkness = 0f
						setAttribute(true, Block.ATTRIBUTE.VISIBLE)
						setAttribute(true, Block.ATTRIBUTE.OUTLINE)
					}
				}
		}
	}

	/** ソフト・Hard drop・先行ホールド・先行rotationの使用制限解除 */
	fun checkDropContinuousUse() {
		if(gameActive) {
			if(!ctrl.isPress(down)||!ruleopt.softdropLimit) softdropContinuousUse = false
			if(!ctrl.isPress(up)||!ruleopt.harddropLimit) harddropContinuousUse = false
			if(!ctrl.isPress(Controller.BUTTON_D)||!ruleopt.holdInitialLimit) initialHoldContinuousUse = false
			if(!ruleopt.rotateInitialLimit) initialRotateContinuousUse = false

			if(initialRotateContinuousUse) {
				var dir = 0
				if(ctrl.isPress(Controller.BUTTON_A)||ctrl.isPress(Controller.BUTTON_C))
					dir = -1
				else if(ctrl.isPress(Controller.BUTTON_B)) dir = 1
				else if(ctrl.isPress(Controller.BUTTON_E)) dir = 2

				if(initialRotateLastDirection!=dir||dir==0) initialRotateContinuousUse = false
			}
		}
	}

	/** 横溜め処理 */
	fun padRepeat() {
		if(moveDirection!=0) dasCount++
		else if(!ruleopt.dasStoreChargeOnNeutral) dasCount = 0
		dasDirection = moveDirection
	}

	/** Called if delay doesn't allow charging but dasRedirectInDelay == true
	 * Updates dasDirection so player can change direction without dropping
	 * charge on entry. */
	fun dasRedirect() {
		dasDirection = moveDirection
	}

	/** Twister routine
	 * @param x X coord
	 * @param y Y coord
	 * @param p Current p object
	 * @param f Field object
	 */
	fun checkTwisted(x:Int, y:Int, p:Piece?, f:Field?):Twister? {
		p ?: return null
		f ?: return null
		if(!twistAllowKick&&kickused) return null
		val m = if(p.big) 2 else 1
		var res:Twister? = null
		if(p.checkCollision(x, y-m, f)&&p.checkCollision(x+m, y, f)
			&&p.checkCollision(x-m, y, f)) {
			res = Twister.IMMOBILE
			val copyField = Field().apply {copy(f)}
			p.placeToField(x, y, copyField)
			if(p.height+1!=copyField.checkLineNoFlag()&&kickused) res = Twister.IMMOBILE_MINI
			if(copyField.checkLineNoFlag()==1&&kickused) res = Twister.IMMOBILE_MINI
		} else if(twistEnableEZ&&kickused&&p.checkCollision(x, y+m, f))
			res = Twister.IMMOBILE_EZ

		if(p.type==Piece.Shape.T) {
			val tx = IntArray(4)
			val ty = IntArray(4)

			// Setup 4-point coordinates
			if(p.big) {
				tx[0] = 1
				tx[1] = 4
				tx[2] = 1
				tx[3] = 4
				ty[0] = 1
				ty[1] = 1
				ty[2] = 4
				ty[3] = 4
			} else {
				tx[0] = 0
				tx[1] = 2
				tx[2] = 0
				tx[3] = 2
				ty[0] = 0
				ty[1] = 0
				ty[2] = 2
				ty[3] = 2
			}

			tx.indices.forEach {
				if(p.big) {
					tx[it] += ruleopt.pieceOffsetX[p.id][p.direction]*2
					ty[it] += ruleopt.pieceOffsetY[p.id][p.direction]*2
				} else {
					tx[it] += ruleopt.pieceOffsetX[p.id][p.direction]
					ty[it] += ruleopt.pieceOffsetY[p.id][p.direction]
				}
			}
			// Check the corner of the T p
			if(tx.indices.count {f.getBlockColor(x+tx[it], y+ty[it])!=Block.BLOCK_COLOR_NONE}>=3) res = Twister.POINT
			else if((twistminiType==TWISTMINI_TYPE_ROTATECHECK
					&&p.checkCollision(x, y, getRotateDirection(-1), f)
					&&p.checkCollision(x, y, getRotateDirection(1), f))
				||(twistminiType==TWISTMINI_TYPE_WALLKICKFLAG&&kickused)) res = Twister.POINT_MINI

		} else {
			val offsetX = ruleopt.pieceOffsetX[p.id][p.direction]
			val offsetY = ruleopt.pieceOffsetY[p.id][p.direction]
			if(!p.big)
				for(i in 0 until Piece.SPINBONUSDATA_HIGH_X[p.id][p.direction].size/2) {
					var isHighSpot1 = false
					var isHighSpot2 = false
					var isLowSpot1 = false
					var isLowSpot2 = false

					if(!f.getBlockEmptyF(x+Piece.SPINBONUSDATA_HIGH_X[p.id][p.direction][i*2]+offsetX,
							y+Piece.SPINBONUSDATA_HIGH_Y[p.id][p.direction][i*2]+offsetY))
						isHighSpot1 = true
					if(!f.getBlockEmptyF(x+Piece.SPINBONUSDATA_HIGH_X[p.id][p.direction][i*2+1]+offsetX,
							y+Piece.SPINBONUSDATA_HIGH_Y[p.id][p.direction][i*2+1]+offsetY))
						isHighSpot2 = true
					if(!f.getBlockEmptyF(x+Piece.SPINBONUSDATA_LOW_X[p.id][p.direction][i*2]+offsetX,
							y+Piece.SPINBONUSDATA_LOW_Y[p.id][p.direction][i*2]+offsetY))
						isLowSpot1 = true
					if(!f.getBlockEmptyF(x+Piece.SPINBONUSDATA_LOW_X[p.id][p.direction][i*2+1]+offsetX,
							y+Piece.SPINBONUSDATA_LOW_Y[p.id][p.direction][i*2+1]+offsetY))
						isLowSpot2 = true

					//log.debug(isHighSpot1 + "," + isHighSpot2 + "," + isLowSpot1 + "," + isLowSpot2);

					if(isHighSpot1&&isHighSpot2&&(isLowSpot1||isLowSpot2))
						res = Twister.POINT
					else if(isLowSpot1&&isLowSpot2&&(isHighSpot1||isHighSpot2))
						res = Twister.POINT_MINI

				}
		}
		return res
	}

	fun setTWIST(x:Int, y:Int, p:Piece?, f:Field?) {
		twistType = null
		if(p==null||f==null||p.type!=Piece.Shape.T)
			return

		twistType = checkTwisted(x, y, p, f)
	}

	/** Spin判定(全スピンルールのとき用)
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param p Current Blockピース
	 * @param f field
	 */
	fun setAllSpin(x:Int, y:Int, p:Piece?, f:Field?) {
		twistType = null

		if((!twistAllowKick&&kickused)||p==null||f==null) return
		twistType = checkTwisted(x, y, p, f)
	}

	/** ピースが出現するX-coordinateを取得
	 * @param fld field
	 * @param piece Piece
	 * @return 出現位置のX-coordinate
	 */
	fun getSpawnPosX(fld:Field?, piece:Piece?):Int {
		var x = 0
		piece?.let {
			x = (fld?.width ?: 0-it.width+1)/2-piece.centerX
			if(big&&bigmove&&x%2!=0) x++

			x += if(big) ruleopt.pieceSpawnXBig[it.id][it.direction]
			else ruleopt.pieceSpawnX[it.id][it.direction]

		}
		return x
	}

	/** ピースが出現するY-coordinateを取得
	 * @param piece Piece
	 * @return 出現位置のY-coordinate
	 */
	fun getSpawnPosY(piece:Piece?):Int {
		var y = 0
		piece?.let {
			if(ruleopt.pieceEnterAboveField&&!ruleopt.fieldCeiling) {
				y = -1-it.maximumBlockY
				if(big) y--
			} else y = -it.minimumBlockY

			y += if(big) ruleopt.pieceSpawnYBig[it.id][it.direction]
			else ruleopt.pieceSpawnY[it.id][it.direction]
		}
		return y
	}

	/** rotation buttonを押したあとのピースのDirectionを取得
	 * @param move rotationDirection (-1:左 1:右 2:180度）
	 * @return rotation buttonを押したあとのピースのDirection
	 */
	fun getRotateDirection(move:Int):Int {
		var rt = move
		nowPieceObject?.let {rt = it.direction+move}

		if(move==2) {
			if(rt>3) rt -= 4
			if(rt<0) rt += 4
		} else {
			if(rt>3) rt = 0
			if(rt<0) rt = 3
		}

		return rt
	}

	/** 先行rotationと先行ホールドの処理 */
	fun initialRotate() {
		initialRotateDirection = 0
		initialHoldFlag = false

		ctrl.let {
			if(ruleopt.rotateInitial&&!initialRotateContinuousUse) {
				var dir = 0
				if(it.isPress(Controller.BUTTON_A)||it.isPress(Controller.BUTTON_C))
					dir = -1
				else if(it.isPress(Controller.BUTTON_B)) dir = 1
				else if(it.isPress(Controller.BUTTON_E)) dir = 2
				initialRotateDirection = dir
			}

			if(it.isPress(Controller.BUTTON_D)&&ruleopt.holdInitial&&isHoldOK) {
				initialHoldFlag = true
				initialHoldContinuousUse = true
				playSE("initialhold")
			}
		}
	}

	/** fieldのBlock stateを更新 */
	private fun fieldUpdate() {
		var outlineOnly = blockShowOutlineOnly // Show outline only flag
		if(owBlockShowOutlineOnly==0) outlineOnly = false
		if(owBlockShowOutlineOnly==1) outlineOnly = true

		field?.let {f ->
			for(i in 0 until f.width)
				for(j in f.hiddenHeight*-1 until f.height) {
					f.getBlock(i, j)?.run {
						if(cint>=Block.BLOCK_COLOR_GRAY) {
							if(elapsedFrames<0) {
								if(!getAttribute(Block.ATTRIBUTE.GARBAGE)) darkness = 0f
							} else if(elapsedFrames<ruleopt.lockflash) {
								darkness = -.8f
								if(outlineOnly) {
									setAttribute(true, Block.ATTRIBUTE.OUTLINE)
									setAttribute(false, Block.ATTRIBUTE.VISIBLE)
									setAttribute(false, Block.ATTRIBUTE.BONE)
								}
							} else {
								darkness = 0f
								setAttribute(true, Block.ATTRIBUTE.OUTLINE)
								if(outlineOnly) {
									setAttribute(false, Block.ATTRIBUTE.VISIBLE)
									setAttribute(false, Block.ATTRIBUTE.BONE)
								}
							}

							if(blockHidden!=-1&&elapsedFrames>=blockHidden-10&&gameActive) {
								if(blockHiddenAnim) {
									alpha -= .1f
									if(alpha<0.0f) alpha = 0.0f
								}

								if(elapsedFrames>=blockHidden) {
									alpha = 0.0f
									setAttribute(false, Block.ATTRIBUTE.OUTLINE)
									setAttribute(false, Block.ATTRIBUTE.VISIBLE)
								}
							}

							if(elapsedFrames>=0) elapsedFrames++
						}
					}
				}

			// X-RAY
			if(gameActive) {
				if(itemXRayEnable) {
					for(i in 0 until f.width)
						for(j in f.hiddenHeight*-1 until f.height) {
							f.getBlock(i, j)?.apply {
								if(cint>=Block.BLOCK_COLOR_GRAY) {
									setAttribute(itemXRayCount%36==i, Block.ATTRIBUTE.VISIBLE)
									setAttribute(itemXRayCount%36==i, Block.ATTRIBUTE.OUTLINE)
								}
							}
						}
					itemXRayCount++
				} else itemXRayCount = 0

				// COLOR
				if(itemColorEnable) {
					for(i in 0 until f.width)
						for(j in f.hiddenHeight*-1 until f.height) {
							var bright = j
							if(bright>=5) bright = 9-bright
							bright = 40-((20-i+bright)*4+itemColorCount)%40
							if(bright>=0&&bright<ITEM_COLOR_BRIGHT_TABLE.size) bright = 10-ITEM_COLOR_BRIGHT_TABLE[bright]
							if(bright>10) bright = 10

							f.getBlock(i, j)?.apply {
								alpha = bright*.1f
								setAttribute(false, Block.ATTRIBUTE.OUTLINE)
								setAttribute(true, Block.ATTRIBUTE.VISIBLE)
							}
						}
					itemColorCount++
				} else itemColorCount = 0
			}

			// ヘボHIDDEN
			if(heboHiddenEnable) {
				heboHiddenTimerNow++

				if(heboHiddenTimerNow>heboHiddenTimerMax) {
					heboHiddenTimerNow = 0
					heboHiddenYNow++
					if(heboHiddenYNow>heboHiddenYLimit) heboHiddenYNow = heboHiddenYLimit
				}
			}
		}
	}

	/** Called when saving replay */
	fun saveReplay() {
		if(owner.replayMode&&!owner.replayRerecord) return

		owner.replayProp.setProperty("version.core", "$versionMajor.$versionMinor")
		owner.replayProp.setProperty("version.core.major", versionMajor)
		owner.replayProp.setProperty("version.core.minor", versionMinor)
		owner.replayProp.setProperty("version.core.dev", versionIsDevBuild)

		owner.replayProp.setProperty("$playerID.replay.randSeed", 16.toString())

		replayData.writeProperty(owner.replayProp, playerID, replayTimer)
		statistics.writeProperty(owner.replayProp, playerID)
		ruleopt.writeProperty(owner.replayProp, playerID)

		if(playerID==0) {
			owner.mode?.let {owner.replayProp.setProperty("name.mode", it.id)}
			owner.replayProp.setProperty("name.rule", ruleopt.strRuleName)

			// Local timestamp
			val time = Calendar.getInstance()
			val month = time.get(Calendar.MONTH)+1
			val strDate = String.format("%04d/%02d/%02d", time.get(Calendar.YEAR), month, time.get(Calendar.DATE))
			val strTime =
				String.format("%02d:%02d:%02d", time.get(Calendar.HOUR_OF_DAY), time.get(Calendar.MINUTE), time.get(Calendar.SECOND))
			owner.replayProp.setProperty("timestamp.date", strDate)
			owner.replayProp.setProperty("timestamp.time", strTime)

			// GMT timestamp
			owner.replayProp.setProperty("timestamp.gmt", GeneralUtil.exportCalendarString())
		}

		owner.replayProp.setProperty("$playerID.tuning.owRotateButtonDefaultRight", owRotateButtonDefaultRight)
		owner.replayProp.setProperty("$playerID.tuning.owSkin", owSkin)
		owner.replayProp.setProperty("$playerID.tuning.owMinDAS", owMinDAS)
		owner.replayProp.setProperty("$playerID.tuning.owMaxDAS", owMaxDAS)
		owner.replayProp.setProperty("$playerID.tuning.owDasDelay", owARR)
		owner.replayProp.setProperty("$playerID.tuning.owReverseUpDown", owReverseUpDown)
		owner.replayProp.setProperty("$playerID.tuning.owMoveDiagonal", owMoveDiagonal)

		owner.mode?.saveReplay(this, playerID, owner.replayProp)
	}

	/** fieldエディット画面に入る処理 */
	fun enterFieldEdit() {
		fldeditPreviousStat = stat
		stat = Status.FIELDEDIT
		fldeditX = 0
		fldeditY = 0
		fldeditColor = Block.BLOCK_COLOR_GRAY
		fldeditFrames = 0
		owner.menuOnly = false
		createFieldIfNeeded()
	}

	/** fieldをInitialization (まだ存在しない場合） */
	fun createFieldIfNeeded():Field {
		if(!gameActive&&!owner.replayMode) {
			val tempRand = Random.Default
			randSeed = tempRand.nextLong()
			random = Random(randSeed)
		}
		if(fieldWidth<0) fieldWidth = ruleopt.fieldWidth
		if(fieldHeight<0) fieldHeight = ruleopt.fieldHeight
		if(fieldHiddenHeight<0) fieldHiddenHeight = ruleopt.fieldHiddenHeight
		val new = Field(fieldWidth, fieldHeight, fieldHiddenHeight, ruleopt.fieldCeiling)
		if(field==null) field = new
		return new
	}

	/** Call this if the game has ended */
	fun gameEnded() {
		if(endTime==0L) {
			endTime = System.nanoTime()
			statistics.gamerate = (replayTimer/(0.00000006*(endTime-startTime))).toFloat()
		}
		gameActive = false
		timerActive = false
		isInGame = false
		ai?.also {it.shutdown()}
	}

	/** ゲーム stateの更新 */
	fun update() {
		if(gameActive) {
			// リプレイ関連の処理
			if(!owner.replayMode||owner.replayRerecord) {
				// AIの button処理
				ai?.let {ai ->
					if(!aiShowHint) ai.setControl(this, playerID, ctrl)
					else {
						aiHintReady = ai.thinkComplete||ai.thinkCurrentPieceNo>0&&ai.thinkCurrentPieceNo<=ai.thinkLastPieceNo
						if(aiHintReady) {
							aiHintPiece = null
							if(ai.bestHold) {
								holdPieceObject?.also {aiHintPiece = Piece(it)} ?: run {
									getNextObjectCopy(nextPieceCount)?.also {
										if(!it.offsetApplied)
											it.applyOffsetArray(ruleopt.pieceOffsetX[it.id], ruleopt.pieceOffsetY[it.id])
									}
								}
							} else nowPieceObject?.let {aiHintPiece = Piece(it)}
						}
					}
				}
				// input 状態をリプレイに記録
				replayData.setInputData(ctrl.buttonBit, replayTimer)
			} else // input 状態をリプレイから読み込み
				ctrl.buttonBit = replayData.getInputData(replayTimer)
			replayTimer++
		}

		//  button input timeの更新
		ctrl.updateButtonTime()

		// 最初の処理
		owner.mode?.onFirst(this, playerID)
		owner.receiver.onFirst(this, playerID)
		if(!owner.replayMode||owner.replayRerecord) ai?.onFirst(this, playerID)
		fpf = 0
		// 各ステータスの処理
		if(!lagStop)
			when(stat) {
				Status.SETTING -> statSetting()
				Status.READY -> statReady()
				Status.MOVE -> {
					dasRepeat = true
					dasInstant = false
					while(dasRepeat)
						statMove()
				}
				Status.LOCKFLASH -> statLockFlash()
				Status.LINECLEAR -> statLineClear()
				Status.ARE -> statARE()
				Status.ENDINGSTART -> statEndingStart()
				Status.CUSTOM -> statCustom()
				Status.EXCELLENT -> statExcellent()
				Status.GAMEOVER -> statGameOver()
				Status.RESULT -> statResult()
				Status.FIELDEDIT -> statFieldEdit()
				Status.INTERRUPTITEM -> statInterruptItem()
				Status.NOTHING -> {
				}
			}

		// fieldのBlock stateや統計情報を更新
		fieldUpdate()
		//if(ending==0||staffrollEnableStatistics) statistics.update()

		// 最後の処理
		if(inthanabi>0) inthanabi--
		owner.mode?.onLast(this, playerID)
		owner.receiver.onLast(this, playerID)
		ai?.also {if(!owner.replayMode||owner.replayRerecord) it.onLast(this, playerID)}

		// Timer増加
		if(gameActive&&timerActive) statistics.time++
		if(temphanabi>0&&inthanabi<=0) {
			owner.receiver.shootFireworks(this)
			temphanabi--
			inthanabi += HANABI_INTERVAL
		}

		/* if(startTime > 0 && endTime == 0) {
		 * statistics.gamerate = (float)(replayTimer /
		 * (0.00000006*(System.nanoTime() - startTime)));
		 * } */
	}

	/** Draw the screen
	 * (各Mode や event 処理クラスの event を呼び出すだけで, それ以外にGameEngine自身は何もしません） */
	fun render() {
		// 最初の処理
		owner.mode?.renderFirst(this, playerID)
		owner.receiver.renderFirst(this, playerID)

		if(rainbowAnimate) Block.updateRainbowPhase(this)

		// 各ステータスの処理
		when(stat) {
			Status.NOTHING -> {
			}
			Status.SETTING -> {
				owner.mode?.renderSetting(this, playerID)
				owner.receiver.renderSetting(this, playerID)
			}
			Status.READY -> {
				owner.mode?.renderReady(this, playerID)
				owner.receiver.renderReady(this, playerID)
			}
			Status.MOVE -> {
				owner.mode?.renderMove(this, playerID)
				owner.receiver.renderMove(this, playerID)
			}
			Status.LOCKFLASH -> {
				owner.mode?.renderLockFlash(this, playerID)
				owner.receiver.renderLockFlash(this, playerID)
			}
			Status.LINECLEAR -> {
				owner.mode?.renderLineClear(this, playerID)
				owner.receiver.renderLineClear(this, playerID)
			}
			Status.ARE -> {
				owner.mode?.renderARE(this, playerID)
				owner.receiver.renderARE(this, playerID)
			}
			Status.ENDINGSTART -> {
				owner.mode?.renderEndingStart(this, playerID)
				owner.receiver.renderEndingStart(this, playerID)
			}
			Status.CUSTOM -> {
				owner.mode?.renderCustom(this, playerID)
				owner.receiver.renderCustom(this, playerID)
			}
			Status.EXCELLENT -> {
				owner.mode?.renderExcellent(this, playerID)
				owner.receiver.renderExcellent(this, playerID)
			}
			Status.GAMEOVER -> {
				owner.mode?.renderGameOver(this, playerID)
				owner.receiver.renderGameOver(this, playerID)
			}
			Status.RESULT -> {
				owner.mode?.renderResult(this, playerID)
				owner.receiver.renderResult(this, playerID)
			}
			Status.FIELDEDIT -> {
				owner.mode?.renderFieldEdit(this, playerID)
				owner.receiver.renderFieldEdit(this, playerID)
			}
			Status.INTERRUPTITEM -> {
			}
		}

		if(owner.showInput) {
			owner.mode?.renderInput(this, playerID)
			owner.receiver.renderInput(this, playerID)
		}
		ai?.also {
			if(aiShowState) it.renderState(this, playerID)
			if(aiShowHint) it.renderHint(this, playerID)
		}

		// 最後の処理
		owner.mode?.renderLast(this, playerID)
		owner.receiver.renderLast(this, playerID)
	}

	/** 開始前の設定画面のときの処理 */
	private fun statSetting() {
		//  event 発生
		owner.bgmStatus.fadesw = false
		owner.bgmStatus.bgm = BGM.Menu(4+(owner.mode?.gameIntensity ?: 0))
		owner.mode?.also {if(it.onSetting(this, playerID)) return}
		owner.receiver.onSetting(this, playerID)

		// Mode側が何もしない場合はReady画面へ移動
		stat = Status.READY
		resetStatc()
	}

	/** Ready→Goのときの処理 */
	private fun statReady() {
		//  event 発生
		owner.mode?.also {if(it.onReady(this, playerID)) return}
		owner.receiver.onReady(this, playerID)

		// 横溜め
		if(ruleopt.dasInReady&&gameActive) padRepeat()
		else if(ruleopt.dasRedirectInARE) dasRedirect()

		// Initialization
		if(statc[0]==0) {

			if(!readyDone&&!owner.bgmStatus.fadesw&&owner.bgmStatus.bgm.id<0&&
				owner.bgmStatus.bgm.id !in BGM.Finale(0).id..BGM.Finale(2).id)
				owner.bgmStatus.fadesw = true
			// fieldInitialization
			createFieldIfNeeded()
			if(owner.replayMode) {
				val tempRand = owner.replayProp.getProperty("$playerID.replay.randSeed", "0")
				randSeed = tempRand.toLong(16)
				random = Random(randSeed)
				nextPieceArrayID = IntArray(0)
				nextPieceArrayObject = emptyArray()
			}
			// NEXTピース作成
			if(nextPieceArrayID.isEmpty()) {
				// 出現可能なピースが1つもない場合は全て出現できるようにする
				if(nextPieceEnable.all {false}) nextPieceEnable = BooleanArray(Piece.PIECE_COUNT) {true}

				// NEXTピースの出現順を作成
				randomizer.setState(nextPieceEnable, randSeed)

				nextPieceArrayID = IntArray(nextPieceArraySize) {randomizer.next()}

			}
			// NEXTピースのオブジェクトを作成
			if(nextPieceArrayObject.isEmpty()) {
				nextPieceArrayObject = Array(nextPieceArrayID.size) {
					Piece(nextPieceArrayID[it]).also {p ->
						p.direction = ruleopt.pieceDefaultDirection[p.id]
						if(p.direction>=Piece.DIRECTION_COUNT)
							p.direction = random.nextInt(Piece.DIRECTION_COUNT)
						p.connectBlocks = connectBlocks
						p.setColor(ruleopt.pieceColor[p.id])
						p.setSkin(skin)
						p.updateConnectData()
						p.setAttribute(true, Block.ATTRIBUTE.VISIBLE)
						p.setAttribute(bone, Block.ATTRIBUTE.BONE)

						if(randomBlockColor) {
							if(blockColors.size<numColors||numColors<1) numColors = blockColors.size
							val size = p.maxBlock
							val colors = IntArray(size)
							for(j in 0 until size)
								colors[j] = blockColors[random.nextInt(numColors)]
							p.setColor(colors)
							p.updateConnectData()
						}
						if(clearMode==ClearType.LINE_GEM_BOMB||clearMode==ClearType.LINE_GEM_SPARK)
							p.block[random.nextInt(p.maxBlock)].cint += 7
					}
				}

			}

			if(!readyDone) {
				//  button input状態リセット
				ctrl.reset()
				// ゲーム中 flagON
				gameActive = true
				gameStarted = true
				isInGame = true
			}
		}

		// READY音
		if(statc[0]==readyStart) playSE("start0")

		// GO音
		if(statc[0]==goStart) playSE("start1")

		// NEXTスキップ
		if(statc[0] in 1 until goEnd&&holdButtonNextSkip&&isHoldOK&&ctrl.isPush(Controller.BUTTON_D)) {
			playSE("initialhold")
			holdPieceObject = getNextObjectCopy(nextPieceCount)?.also {
				it.applyOffsetArray(ruleopt.pieceOffsetX[it.id], ruleopt.pieceOffsetY[it.id])
			}
			nextPieceCount++
			if(nextPieceCount<0) nextPieceCount = 0
		}

		// 開始
		if(statc[0]>=goEnd) {
			owner.mode?.startGame(this, playerID)
			owner.receiver.startGame(this, playerID)
			owner.bgmStatus.fadesw = false
			initialRotate()
			stat = Status.MOVE
			resetStatc()
			if(!readyDone) startTime = System.nanoTime()
			//startTime = System.nanoTime()/1000000L;
			readyDone = true
			return
		}

		statc[0]++
	}

	/** Blockピースの移動処理 */
	private fun statMove() {
		dasRepeat = false

		//  event 発生
		owner.mode?.also {if(it.onMove(this, playerID)) return}
		owner.receiver.onMove(this, playerID)
		val field = field ?: return
		// 横溜めInitialization
		val moveDirection = moveDirection

		if(statc[0]>0||ruleopt.dasInMoveFirstFrame)
			if(dasDirection!=moveDirection) {
				dasDirection = moveDirection
				if(!(dasDirection==0&&ruleopt.dasStoreChargeOnNeutral)) dasCount = 0
			}

		// 出現時の処理
		if(statc[0]==0) {
			if(statc[1]==0&&!initialHoldFlag) {
				// 通常出現
				nowPieceObject = getNextObjectCopy(nextPieceCount)
				nextPieceCount++
				if(nextPieceCount<0) nextPieceCount = 0
				holdDisable = false
			} else {
				// ホールド出現
				if(initialHoldFlag) {
					// 先行ホールド
					if(holdPieceObject==null) {
						// 1回目
						holdPieceObject = getNextObjectCopy(nextPieceCount)?.also {
							it.applyOffsetArray(ruleopt.pieceOffsetX[it.id], ruleopt.pieceOffsetY[it.id])
						}
						nextPieceCount++
						if(nextPieceCount<0) nextPieceCount = 0

						if(bone)
							getNextObject(nextPieceCount+ruleopt.nextDisplay-1)?.setAttribute(true, Block.ATTRIBUTE.BONE)

						nowPieceObject = getNextObjectCopy(nextPieceCount)
						nextPieceCount++
						if(nextPieceCount<0) nextPieceCount = 0
					} else {
						// 2回目以降
						val pieceTemp = holdPieceObject
						holdPieceObject = getNextObjectCopy(nextPieceCount)?.also {
							it.applyOffsetArray(ruleopt.pieceOffsetX[it.id], ruleopt.pieceOffsetY[it.id])
						}
						nowPieceObject = pieceTemp
						nextPieceCount++
						if(nextPieceCount<0) nextPieceCount = 0
					}
				} else // 通常ホールド
					if(holdPieceObject==null) {
						// 1回目
						nowPieceObject?.big = false
						holdPieceObject = nowPieceObject
						nowPieceObject = getNextObjectCopy(nextPieceCount)
						nextPieceCount++
						if(nextPieceCount<0) nextPieceCount = 0
					} else {
						// 2回目以降
						nowPieceObject?.big = false
						val pieceTemp = holdPieceObject
						holdPieceObject = nowPieceObject
						nowPieceObject = pieceTemp
					}

				// Directionを戻す
				holdPieceObject?.let {
					if(ruleopt.holdResetDirection&&ruleopt.pieceDefaultDirection[it.id]<Piece.DIRECTION_COUNT) {
						it.direction = ruleopt.pieceDefaultDirection[it.id]
						it.updateConnectData()
					}
				}

				// 使用した count+1
				holdUsedCount++
				statistics.totalHoldUsed++

				// ホールド無効化
				initialHoldFlag = false
				holdDisable = true
			}
			getNextObject(nextPieceCount)?.let {
				if(framecolor !in FRAME_SKIN_SG..FRAME_SKIN_GB) playSE("piece_${it.type.name.lowercase(Locale.getDefault())}")
			}
			nowPieceObject?.let {
				if(!it.offsetApplied)
					it.applyOffsetArray(ruleopt.pieceOffsetX[it.id], ruleopt.pieceOffsetY[it.id])
				it.big = big
				// 出現位置 (横）
				nowPieceX = getSpawnPosX(field, it)
				nowPieceY = getSpawnPosY(it)
				nowPieceBottomY = it.getBottom(nowPieceX, nowPieceY, field)
			}
			nowPieceColorOverride = -1

			if(itemRollRollEnable) nowPieceColorOverride = Block.BLOCK_COLOR_GRAY

			gcount = if(speed.gravity>speed.denominator&&speed.denominator>0)
				speed.gravity%speed.denominator
			else 0

			lockDelayNow = 0
			dasSpeedCount = dasDelay
			dasRepeat = false
			dasInstant = false
			extendedMoveCount = 0
			extendedRotateCount = 0
			softdropFall = 0
			harddropFall = 0
			manualLock = false
			nowPieceMoveCount = 0
			nowPieceRotateCount = 0
			nowPieceRotateFailCount = 0
			nowWallkickCount = 0
			nowUpwardWallkickCount = 0
			lineClearing = 0
			garbageClearing = 0
			lastmove = LastMove.NONE
			kickused = false
			twistType = null

			getNextObject(nextPieceCount+ruleopt.nextDisplay-1)?.setAttribute(bone, Block.ATTRIBUTE.BONE)

			if(ending==0) timerActive = true

			if(!owner.replayMode||owner.replayRerecord) ai?.newPiece(this, playerID)
		}

		checkDropContinuousUse()

		var softdropUsed = false // この frame にSoft dropを使ったらtrue
		var softdropFallNow = 0 // この frame のSoft dropで落下した段count

		var updown = false // Up下同時押し flag
		ctrl.let {if(it.isPress(up)&&it.isPress(down)) updown = true}

		if(!dasInstant) {

			// ホールド
			if(ctrl.isPush(Controller.BUTTON_D)||initialHoldFlag)
				if(isHoldOK) {
					statc[0] = 0
					statc[1] = 1
					if(!initialHoldFlag) playSE("hold")
					initialHoldContinuousUse = true
					initialHoldFlag = false
					holdDisable = true
					initialRotate() //Hold swap triggered IRS
					statMove()
					return
				} else if(statc[0]>0&&!initialHoldFlag) playSE("holdfail")

			// rotation
			val onGroundBeforeRotate = nowPieceObject?.checkCollision(nowPieceX, nowPieceY+1, field) ?: false
			var move = 0
			var rotated = false

			if(initialRotateDirection!=0) {
				move = initialRotateDirection
				initialRotateLastDirection = initialRotateDirection
				initialRotateContinuousUse = true
				playSE("initialrotate")
			} else if(statc[0]>0||ruleopt.moveFirstFrame) {
				if(itemRollRollEnable&&replayTimer%itemRollRollInterval==0) move = 1 // Roll Roll

				//  button input
				ctrl.let {
					when {
						it.isPush(Controller.BUTTON_A)||it.isPush(Controller.BUTTON_C) -> move = -1
						it.isPush(Controller.BUTTON_B) -> move = 1
						it.isPush(Controller.BUTTON_E) -> move = 2
					}
				}

				if(move!=0) {
					initialRotateLastDirection = move
					initialRotateContinuousUse = true
				}
			}

			if(!ruleopt.rotateButtonAllowDouble&&move==2) move = -1
			if(!ruleopt.rotateButtonAllowReverse&&move==1) move = -1
			if(isRotateButtonDefaultRight&&move!=2) move *= -1

			nowPieceObject?.also {piece ->
				if(move!=0) {
					// Direction after rotationを決める
					var rt = getRotateDirection(move)

					// rotationできるか判定
					if(!piece.checkCollision(nowPieceX, nowPieceY, rt, field)) {
						// Wallkickなしでrotationできるとき
						rotated = true
						kickused = false
						piece.direction = rt
						piece.updateConnectData()
					} else if(ruleopt.rotateWallkick&&wallkick!=null&&(initialRotateDirection==0||ruleopt.rotateInitialWallkick)
						&&(ruleopt.lockresetLimitOver!=RuleOptions.LOCKRESET_LIMIT_OVER_NOWALLKICK||!isRotateCountExceed)) {
						// Wallkickを試みる
						val allowUpward = ruleopt.rotateMaxUpwardWallkick<0||nowUpwardWallkickCount<ruleopt.rotateMaxUpwardWallkick

						wallkick?.executeWallkick(nowPieceX, nowPieceY, move, piece.direction, rt, allowUpward, piece, field, ctrl)
							?.let {kick ->
								rotated = true
								kickused = true
								playSE("wallkick")
								nowWallkickCount++
								if(kick.isUpward) nowUpwardWallkickCount++
								piece.direction = kick.direction
								piece.updateConnectData()
								nowPieceX += kick.offsetX
								nowPieceY += kick.offsetY

								if(ruleopt.lockresetWallkick&&!isRotateCountExceed) {
									lockDelayNow = 0
									piece.setDarkness(0f)
								}
							}
					} else if(!rotated&&dominoQuickTurn&&piece.id==Piece.PIECE_I2&&nowPieceRotateFailCount>=1) {
						// Domino Quick Turn
						rt = getRotateDirection(2)
						rotated = true
						piece.direction = rt
						piece.updateConnectData()
						nowPieceRotateFailCount = 0

						if(piece.checkCollision(nowPieceX, nowPieceY, rt, field))
							nowPieceY--
						else if(onGroundBeforeRotate) nowPieceY++
					}

					if(rotated) {
						// rotation成功
						nowPieceBottomY = piece.getBottom(nowPieceX, nowPieceY, field)

						if(ruleopt.lockresetRotate&&!isRotateCountExceed) {
							lockDelayNow = 0
							piece.setDarkness(0f)
						}

						lastmove = if(onGroundBeforeRotate) {
							extendedRotateCount++
							LastMove.ROTATE_GROUND
						} else LastMove.ROTATE_AIR

						if(initialRotateDirection==0) playSE("rotate")
						if(checkTwisted(nowPieceX, nowPieceY, piece, field)!=null) playSE("twist")
						nowPieceRotateCount++
						if(ending==0||staffrollEnableStatistics) statistics.totalPieceRotate++
					} else {
						// rotation失敗
						playSE("rotfail")
						nowPieceRotateFailCount++
					}
				}

				initialRotateDirection = 0

				// game over check
				if(statc[0]==0&&piece.checkCollision(nowPieceX, nowPieceY, field)) {
					val mass = if(piece.big) 2 else 1
					while(nowPieceX+piece.maximumBlockX*mass>field.width)
						nowPieceX--

					while(nowPieceX-piece.minimumBlockX*mass<0)
						nowPieceX++

					while((nowPieceBottomY-piece.height)<-field.hiddenHeight) {
						nowPieceY++
						nowPieceBottomY = piece.getBottom(nowPieceX, nowPieceY, field)
					}
					// Blockの出現位置を上にずらすことができる場合はそうする
					for(i in 0 until ruleopt.pieceEnterMaxDistanceY) {
						nowPieceY--

						if(!piece.checkCollision(nowPieceX, nowPieceY, field)) {
							nowPieceBottomY = piece.getBottom(nowPieceX, nowPieceY, field)
							break
						}
					}

					// 死亡
					if(piece.checkCollision(nowPieceX, nowPieceY, field)) {
						piece.placeToField(nowPieceX, nowPieceY, field)
						nowPieceObject = null
						stat = Status.GAMEOVER
						stopSE("danger")
						if(ending==2&&staffrollNoDeath) stat = Status.NOTHING
						resetStatc()
						return
					}
				}
			}
		}

		var sidemoveflag = false // この frame に横移動したらtrue

		if(statc[0]>0||ruleopt.moveFirstFrame) {
			// 横移動
			val onGroundBeforeMove = nowPieceObject?.checkCollision(nowPieceX, nowPieceY+1, field) ?: false

			var move = moveDirection

			fpf = 0
			if(statc[0]==0&&delayCancel) {
				if(delayCancelMoveLeft) move = -1
				if(delayCancelMoveRight) move = 1
				dasCount = 0
				// delayCancel = false;
				delayCancelMoveLeft = false
				delayCancelMoveRight = false
			} else if(statc[0]==1&&delayCancel&&dasCount<das) delayCancel = false


			if(move!=0) sidemoveflag = true

			if(big&&bigmove) move *= 2

			if(move!=0&&dasCount==0) shiftLock = 0

			if(move!=0&&(dasCount==0||dasCount>=das)) {
				shiftLock = shiftLock and ctrl.buttonBit

				if(shiftLock==0)
					if(dasSpeedCount>=dasDelay||dasCount==0) {
						if(dasCount>0) dasSpeedCount = 1
						nowPieceObject?.also {
							if(!it.checkCollision(nowPieceX+move, nowPieceY, field)) {
								nowPieceX += move

								if(dasDelay==0&&dasCount>0&&
									!it.checkCollision(nowPieceX+move, nowPieceY, field)) {
									if(!dasInstant) playSE("move")
									dasRepeat = true
									dasInstant = true
								}

								//log.debug("Successful movement: move="+move);

								if(ruleopt.lockresetMove&&!isMoveCountExceed) {
									lockDelayNow = 0
									it.setDarkness(0f)
								}

								nowPieceMoveCount++
								if(ending==0||staffrollEnableStatistics) statistics.totalPieceMove++
								nowPieceBottomY = it.getBottom(nowPieceX, nowPieceY, field)

								lastmove = if(onGroundBeforeMove) {
									extendedMoveCount++
									LastMove.SLIDE_GROUND
								} else LastMove.SLIDE_AIR
								if(!dasInstant) playSE("move")

							} else if(ruleopt.dasChargeOnBlockedMove) {
								dasCount = das
								dasSpeedCount = dasDelay
							}
						}
					} else dasSpeedCount++
			}

			if(!dasRepeat) {
				// Hard drop
				if(ctrl.isPress(up)&&!harddropContinuousUse&&ruleopt.harddropEnable&&
					(isDiagonalMoveEnabled||!sidemoveflag)&&(ruleopt.moveUpAndDown||!updown)&&nowPieceY<nowPieceBottomY) {
					harddropFall += nowPieceBottomY-nowPieceY
					fpf = nowPieceBottomY-nowPieceY
					if(nowPieceY!=nowPieceBottomY) {
						nowPieceY = nowPieceBottomY
						playSE("harddrop")
					}
					harddropContinuousUse = !ruleopt.harddropLock
					owner.mode?.afterHardDropFall(this, playerID, harddropFall)
					owner.receiver.afterHardDropFall(this, playerID, harddropFall)

					lastmove = LastMove.FALL_SELF
					if(ruleopt.lockresetFall) {
						lockDelayNow = 0
						nowPieceObject?.setDarkness(0f)
						extendedMoveCount = 0
						extendedRotateCount = 0
					}
				}
				if(ruleopt.softdropEnable&&ctrl.isPress(down)&&
					!softdropContinuousUse&&(isDiagonalMoveEnabled||!sidemoveflag)&&
					(ruleopt.moveUpAndDown||!updown)&&
					(!onGroundBeforeMove&&!harddropContinuousUse)) {
					if(!ruleopt.softdropGravitySpeedLimit||(softDropSpd<speed.denominator)) {// Old Soft Drop codes
						gcount += softDropSpd.toInt()
						softdropUsed = true
					} else {// New Soft Drop codes
						gcount = softDropSpd.toInt()
						softdropUsed = true
					}
				} else if(!ruleopt.softdropGravitySpeedLimit&&softdropUsed) gcount = 0 // This prevents soft drop from adding to the gravity speed.
			}
			if(ending==0||staffrollEnableStatistics) statistics.totalPieceActiveTime++
		}
		if(!ruleopt.softdropGravitySpeedLimit||softDropSpd<1f||
			!softdropUsed) gcount += speed.gravity // Part of Old Soft Drop

		while((gcount>=speed.denominator||speed.gravity<0)&&
			nowPieceObject?.checkCollision(nowPieceX, nowPieceY+1, field)==false) {
			if(speed.gravity>=0) gcount -= speed.denominator
			if(ruleopt.softdropGravitySpeedLimit) gcount -= gcount%speed.denominator
			nowPieceY++
			if(speed.gravity>speed.denominator/2||speed.gravity<0||softdropUsed) fpf++
			if(ruleopt.lockresetFall) {
				lockDelayNow = 0
				nowPieceObject?.setDarkness(0f)
			}

			if(lastmove!=LastMove.ROTATE_GROUND&&lastmove!=LastMove.SLIDE_GROUND&&lastmove!=LastMove.FALL_SELF) {
				extendedMoveCount = 0
				extendedRotateCount = 0
			}

			if(softdropUsed) {
				lastmove = LastMove.FALL_SELF
				softdropFall++
				softdropFallNow++
			} else lastmove = LastMove.FALL_AUTO
		}

		if(softdropFallNow>0) {
			playSE("softdrop")
			owner.mode?.afterSoftDropFall(this, playerID, softdropFallNow)
			owner.receiver.afterSoftDropFall(this, playerID, softdropFallNow)
		}

		// 接地と固定
		if(nowPieceObject?.checkCollision(nowPieceX, nowPieceY+1, field)==true&&(statc[0]>0||ruleopt.moveFirstFrame)) {
			if(lockDelayNow==0&&lockDelay>0&&lastmove!=LastMove.SLIDE_GROUND&&lastmove!=LastMove.ROTATE_GROUND) {
				playSE("step")
				if(!ruleopt.softdropLock&&ruleopt.softdropSurfaceLock&&softdropUsed) softdropContinuousUse = true
			}
			if(lockDelayNow<lockDelay) lockDelayNow++

			if(lockDelay>=99&&lockDelayNow>98) lockDelayNow = 98

			nowPieceObject?.run {
				if(lockDelayNow<lockDelay)
					if(lockDelayNow>=lockDelay-1) setDarkness(.5f)
					else setDarkness(0.35f*lockDelayNow/lockDelay)
			}
			if(lockDelay!=0) gcount = speed.gravity

			// trueになると即固定
			var instantlock = false

			// Hard drop固定
			ctrl.let {
				if(ruleopt.harddropEnable&&!harddropContinuousUse&&
					it.isPress(up)&&ruleopt.harddropLock&&
					(isDiagonalMoveEnabled||!sidemoveflag)&&(ruleopt.moveUpAndDown||!updown)) {
					harddropContinuousUse = true
					manualLock = true
					instantlock = true
				}

				// Soft drop固定
				if(ruleopt.softdropEnable&&
					(ruleopt.softdropLock&&it.isPress(down)||it.isPush(down)&&
						(ruleopt.softdropSurfaceLock||speed.gravity<0)&&!softdropUsed)
					&&!softdropContinuousUse&&(isDiagonalMoveEnabled||!sidemoveflag)&&(ruleopt.moveUpAndDown||!updown)) {
					softdropContinuousUse = true
					manualLock = true
					instantlock = true
				}
				if(manualLock&&ruleopt.shiftLockEnable)
				// bit 1 and 2 are button_up and button_down currently
					shiftLock = it.buttonBit and 3
			}
			// 移動＆rotationcount制限超過
			if(ruleopt.lockresetLimitOver==RuleOptions.LOCKRESET_LIMIT_OVER_INSTANT&&(isMoveCountExceed||isRotateCountExceed))
				instantlock = true

			// 接地即固定
			if(lockDelay==0&&(gcount>=speed.denominator||speed.gravity<0)) instantlock = true

			// 固定
			if(lockDelay in 1..lockDelayNow||instantlock) {
				if(ruleopt.lockflash>0) nowPieceObject?.setDarkness(-.8f)

				// Twister判定
				if(lastmove==LastMove.ROTATE_GROUND&&twistEnable)
					if(useAllSpinBonus) setAllSpin(nowPieceX, nowPieceY, nowPieceObject, field)
					else setTWIST(nowPieceX, nowPieceY, nowPieceObject, field)

				nowPieceObject?.setAttribute(true, Block.ATTRIBUTE.SELFPLACED)

				val partialLockOut = nowPieceObject?.isPartialLockOut(nowPieceX, nowPieceY) ?: false
				val put = nowPieceObject?.placeToField(nowPieceX, nowPieceY, field) ?: false

				playSE("lock", 1.5f-(nowPieceY+nowPieceObject!!.height)*.5f/fieldHeight)

				holdDisable = false

				if(ending==0||staffrollEnableStatistics) statistics.totalPieceLocked++// AREなし
				lineClearing = when(clearMode) {
					ClearType.LINE -> field.checkLineNoFlag()
					ClearType.COLOR -> field.checkColor(colorClearSize, false, garbageColorClear, gemSameColor, ignoreHidden)
					ClearType.LINE_COLOR -> field.checkLineColor(colorClearSize, false, lineColorDiagonals, gemSameColor)
					ClearType.GEM_COLOR -> field.gemColorCheck(colorClearSize, false, garbageColorClear, ignoreHidden)
					ClearType.LINE_GEM_BOMB, ClearType.LINE_GEM_SPARK -> field.checkBombOnLine(true)
				}

				chain = 0
				lineGravityTotalLines = 0
				garbageClearing = field.garbageCleared
				//if(combo==0 && lastevent!=EVENT_NONE)lastevent=EVENT_NONE;
				if(lineClearing==0) {

					if(twist) {
						playSE("twister")
						lastevent = ScoreEvent(0, false, nowPieceObject, twistType!!)
						if(b2bcount==0) {
							b2bcount = 1
							playSE("b2b_start")
						}
						if(ending==0||staffrollEnableStatistics)
							if(twistmini) statistics.totalTwistZeroMini++
							else statistics.totalTwistZero++
						owner.receiver.calcScore(this, lastevent)
					} else combo = 0

					owner.mode?.calcScore(this, playerID, lineClearing)?.let {
						if(it>0)
							owner.receiver.addScore(this, nowPieceX, nowPieceBottomY, it, EventReceiver.getPlayerColor(playerID))
					}
				}

				owner.mode?.pieceLocked(this, playerID, lineClearing)
				owner.receiver.pieceLocked(this, playerID, lineClearing)

				dasRepeat = false
				dasInstant = false

				// Next 処理を決める(Mode 側でステータスを弄っている場合は何もしない)
				if(stat==Status.MOVE) {
					resetStatc()

					when {
						ending==1 -> stat = Status.ENDINGSTART // Ending
						!put&&ruleopt.fieldLockoutDeath||partialLockOut&&ruleopt.fieldPartialLockoutDeath -> {
							// 画面外に置いて死亡
							stat = Status.GAMEOVER
							stopSE("danger")
							if(ending==2&&staffrollNoDeath) stat = Status.NOTHING
						}
						(lineGravityType==LineGravity.CASCADE||lineGravityType==LineGravity.CASCADE_SLOW)&&!connectBlocks -> {
							stat = Status.LINECLEAR
							statc[0] = lineDelay
							statLineClear()
						}
						lineClearing>0&&(ruleopt.lockflash<=0||!ruleopt.lockflashBeforeLineClear) -> {
							// Line clear
							stat = Status.LINECLEAR
							statLineClear()
						}
						(are>0||lagARE||ruleopt.lockflashBeforeLineClear)&&
							ruleopt.lockflash>0&&ruleopt.lockflashOnlyFrame
							// AREあり (光あり）
						-> stat = Status.LOCKFLASH
						are>0||lagARE -> {
							// AREあり (光なし）
							statc[1] = are
							stat = Status.ARE
						}
						interruptItemNumber!=INTERRUPTITEM_NONE -> {
							// 中断効果のあるアイテム処理
							nowPieceObject = null
							interruptItemPreviousStat = Status.MOVE
							stat = Status.INTERRUPTITEM
						}
						else -> {
							// AREなし
							stat = Status.MOVE
							if(!ruleopt.moveFirstFrame) statMove()
						}
					}
				}
				return
			}
		}

		// 横溜め
		if(statc[0]>0||ruleopt.dasInMoveFirstFrame)
			if(moveDirection!=0&&moveDirection==dasDirection&&(dasCount<das||das<=0))
				dasCount++

		statc[0]++
	}

	/** Block固定直後の光っているときの処理 */
	private fun statLockFlash() {
		//  event 発生
		owner.mode?.also {if(it.onLockFlash(this, playerID)) return}
		owner.receiver.onLockFlash(this, playerID)

		statc[0]++

		checkDropContinuousUse()

		// 横溜め
		if(ruleopt.dasInLockFlash)
			padRepeat()
		else if(ruleopt.dasRedirectInARE) dasRedirect()

		// Next ステータス
		if(statc[0]>=ruleopt.lockflash) {
			resetStatc()

			if(lineClearing>0) {
				// Line clear
				stat = Status.LINECLEAR
				statLineClear()
			} else {
				// ARE
				statc[1] = are
				stat = Status.ARE
			}
			return
		}
	}

	/** Line clear処理 */
	private fun statLineClear() {
		if(field==null) return
		//  event 発生
		owner.mode?.also {if(it.onLineClear(this, playerID)) return}
		owner.receiver.onLineClear(this, playerID)

		checkDropContinuousUse()

		// 横溜め
		if(ruleopt.dasInLineClear)
			padRepeat()
		else if(ruleopt.dasRedirectInARE) dasRedirect()

		// 最初の frame
		if(statc[0]==0) {
			field?.also {field ->
				when(clearMode) {
					ClearType.LINE -> lineClearing = field.checkLine()// Line clear flagを設定
					ClearType.COLOR ->// Set cint clear flags
						lineClearing = field.checkColor(colorClearSize, true, garbageColorClear, gemSameColor, ignoreHidden)
					ClearType.LINE_COLOR ->// Set line cint clear flags
						lineClearing = field.checkLineColor(colorClearSize, true, lineColorDiagonals, gemSameColor)
					ClearType.GEM_COLOR -> lineClearing = field.gemColorCheck(colorClearSize, true, garbageColorClear, ignoreHidden)
					ClearType.LINE_GEM_BOMB, ClearType.LINE_GEM_SPARK -> {
						lineClearing = field.checkBombIgnited()
						if(clearMode==ClearType.LINE_GEM_BOMB) statc[3] = chain
						val force = statc[3]+field.checkLineNoFlag()
						if(clearMode==ClearType.LINE_GEM_SPARK) statc[3] = force
						field.igniteBomb(explodSize[force][0], explodSize[force][1], explodSize[0][0], explodSize[0][1])

					}
				}
				val ingame = ending==0||staffrollEnableStatistics
				// Linescountを決める
				var li = lineClearing
				if(big&&bighalf) li = li shr 1
				if(clearMode==ClearType.LINE) {
					split = field.lastLinesSplited

					if(li>0) {
						playSE("erase")
						playSE(when {
							li>=(if(twist) 3 else 4) -> "erase2"
							li>=(if(twist||combo>=1) 2 else 3) -> "erase1"
							else -> "erase0"
						}, vol = if(li>=(if(twist) 3 else 4)) .7f else 1f)
						lastlines = field.lastLinesHeight
						lastline = field.lastLinesBottom
						playSE("line${maxOf(1, minOf(li, 4))}")
						if(li>=4) playSE("applause${maxOf(0, minOf(1+b2bcount, 4))}")
						if(twist) {
							playSE("twister")
							if(li>=3||li>=2&&b2b) playSE("crowd1") else playSE("crowd0")
							if(ingame)
								when(li) {
									1 -> if(twistmini) statistics.totalTwistSingleMini++
									else statistics.totalTwistSingle++
									2 -> if(split) statistics.totalTwistSplitDouble++ else if(twistmini) statistics.totalTwistDoubleMini++
									else statistics.totalTwistDouble++
									3 -> if(split) statistics.totalTwistSplitTriple++ else statistics.totalTwistTriple++
								}
						} else if(ingame)
							when(li) {
								1 -> statistics.totalSingle++
								2 -> if(split) statistics.totalSplitDouble++
								else statistics.totalDouble++
								3 -> if(split) statistics.totalSplitTriple++
								else statistics.totalTriple++
								4 -> statistics.totalQuadruple++
							}

					}
					// B2B bonus

					if(b2bEnable)
						if(li>=4||(split&&splitb2b)||twist) {
							b2bcount++

							if(b2bcount==1) playSE("b2b_start")
							else {
								b2b = true
								playSE("b2b_combo", minOf(1.5f, 1f+(b2bbuf-1)/13f))
								if(ingame)
									when {
										li==4 -> statistics.totalB2BQuad++
										split -> statistics.totalB2BSplit++
										twist -> statistics.totalB2BTwist++
									}
								owner.receiver.addCombo(this, nowPieceX, nowPieceBottomY-(combobuf>=1).toInt(), b2bcount-1, CHAIN.B2B)
							}
							if(ingame) if(b2bcount>=statistics.maxB2B) statistics.maxB2B = b2bcount-1
							b2bbuf = b2bcount
						} else if(b2bcount!=0&&combo<=0) {
							b2b = false
							b2bcount = 0
							playSE("b2b_end")
						}
					// Combo
					if(comboType!=COMBO_TYPE_DISABLE&&chain==0) {
						if(comboType==COMBO_TYPE_NORMAL||comboType==COMBO_TYPE_DOUBLE&&li>=2) combo++
						if(combo>=2) {
							playSE("combo", minOf(2f, 1f+(combo-2)/7f))
							owner.receiver.addCombo(this, nowPieceX, nowPieceBottomY+b2b.toInt(), combo-1, CHAIN.COMBO)
						}
						if(ingame) if(combo>=statistics.maxCombo) statistics.maxCombo = combo-1
						combobuf = combo
					}

					lastevent = ScoreEvent(li, split, nowPieceObject, twistType, b2b)
					lineGravityTotalLines += lineClearing
					if(ingame) statistics.lines += li

				} else if(clearMode==ClearType.LINE_GEM_BOMB) {
					playSE("bomb")
					playSE("erase")
				}
				if(field.howManyGemClears>0) playSE("gem")
				// All clear
				if(li>=1&&field.isEmpty) {
					owner.receiver.bravo(this)
					temphanabi += 6
				}
				// Calculate score
				owner.mode?.calcScore(this, playerID, li)?.let {
					if(it>0)
						owner.receiver.addScore(this, nowPieceX, field.lastLinesBottom, it, EventReceiver.getPlayerColor(playerID))
				}
				if(li>0) owner.receiver.calcScore(this, lastevent)

				// Blockを消す演出を出す (まだ実際には消えていない）
				for(i in 0 until field.height) {
					if(clearMode==ClearType.LINE&&field.getLineFlag(i)) {
						owner.mode?.lineClear(this, playerID, i)
						owner.receiver.lineClear(this, playerID, i)
					}
					field.getRow(i).filterNotNull().forEachIndexed {j, b ->
						if(b.getAttribute(Block.ATTRIBUTE.ERASE)) {
							owner.mode?.blockBreak(this, playerID, j, i, b)
							owner.receiver.also {r ->
								if(displaysize==1) {
									r.blockBreak(this, 2*j, 2*i, b)
									r.blockBreak(this, 2*j+1, 2*i, b)
									r.blockBreak(this, 2*j, 2*i+1, b)
									r.blockBreak(this, 2*j+1, 2*i+1, b)
								} else r.blockBreak(this, j, i, b)
								if(b.isGemBlock&&clearMode==ClearType.LINE_GEM_BOMB)
									r.blockBreak(this, j, i, b)
							}
						}
					}

				}

				// Blockを消す
				when(clearMode) {
					ClearType.LINE -> field.clearLine()
					ClearType.COLOR -> field.clearColor(colorClearSize, garbageColorClear, gemSameColor, ignoreHidden)
					ClearType.LINE_COLOR -> field.clearProceed()
					ClearType.GEM_COLOR -> lineClearing = field.gemClearColor(colorClearSize, garbageColorClear, ignoreHidden)
					ClearType.LINE_GEM_BOMB -> lineClearing = field.clearProceed(1)
					ClearType.LINE_GEM_SPARK -> lineClearing = field.clearProceed(2)
				}
			}
		}

		// Linesを1段落とす
		if(lineGravityType==LineGravity.NATIVE&&lineDelay>=lineClearing-1&&statc[0]>=lineDelay-(lineClearing-1)
			&&ruleopt.lineFallAnim)
			field?.downFloatingBlocksSingleLine()

		// Line delay cancel check
		delayCancelMoveLeft = ctrl.isPush(Controller.BUTTON_LEFT)
		delayCancelMoveRight = ctrl.isPush(Controller.BUTTON_RIGHT)

		val moveCancel = ruleopt.lineCancelMove&&(ctrl.isPush(up)||ctrl.isPush(down)
			||delayCancelMoveLeft||delayCancelMoveRight)
		val rotateCancel = ruleopt.lineCancelRotate&&(ctrl.isPush(Controller.BUTTON_A)||ctrl.isPush(Controller.BUTTON_B)
			||ctrl.isPush(Controller.BUTTON_C)||ctrl.isPush(Controller.BUTTON_E))
		val holdCancel = ruleopt.lineCancelHold&&ctrl.isPush(Controller.BUTTON_D)

		delayCancel = moveCancel||rotateCancel||holdCancel

		if(statc[0]<lineDelay&&delayCancel) statc[0] = lineDelay

		// Next ステータス
		if(statc[0]>=lineDelay) {
			field?.also {field ->
				if((clearMode==ClearType.LINE_GEM_BOMB||clearMode==ClearType.LINE_GEM_SPARK)&&field.checkBombIgnited()>0) {
					statc[0] = 0
					statc[6] = 0
					return
				} else if(lineGravityType==LineGravity.CASCADE||lineGravityType==LineGravity.CASCADE_SLOW) // Cascade
					when {
						statc[6]<cascadeDelay -> {
							statc[6]++
							return
						}
						field.doCascadeGravity(lineGravityType) -> {
							statc[6] = 0
							return
						}
						statc[6]<cascadeClearDelay -> {
							statc[6]++
							return
						}
						clearMode==ClearType.LINE&&field.checkLineNoFlag()>0
							||clearMode==ClearType.COLOR&&
							field.checkColor(colorClearSize, false, garbageColorClear, gemSameColor, ignoreHidden)>0
							||clearMode==ClearType.LINE_COLOR&&field.checkLineColor(colorClearSize, false, lineColorDiagonals, gemSameColor)>0
							||clearMode==ClearType.GEM_COLOR&&field.gemColorCheck(colorClearSize, false, garbageColorClear, ignoreHidden)>0
							||(clearMode==ClearType.LINE_GEM_BOMB||clearMode==ClearType.LINE_GEM_SPARK)&&field.checkBombOnLine(true)>0 -> {
							twistType = null
							chain++
							if(chain>statistics.maxChain) statistics.maxChain = chain
							statc[0] = 0
							statc[6] = 0
							combobuf = chain
							return
						}
					}

			}
			val skip = owner.mode?.lineClearEnd(this, playerID) ?: false
			owner.receiver.lineClearEnd(this, playerID)
			if(sticky>0) field?.setBlockLinkByColor()
			if(sticky==2) field?.setAllAttribute(true, Block.ATTRIBUTE.IGNORE_BLOCKLINK)

			if(!skip) {
				if(lineGravityType==LineGravity.NATIVE) field?.downFloatingBlocks()
				field?.run {
					if((lastLinesBottom>=highestBlockY&&lineClearing>=3)||lastLinesSplited)
						playSE("linefall", maxOf(
							0.8f, 1.2f-lastLinesBottom/3f/fieldHeight), minOf(1f, 0.4f+speed.lineDelay*0.1f))
					if((lastLinesBottom>=highestBlockY&&lineClearing<=2)||lastLinesSplited)
						playSE("linefall1", maxOf(
							0.8f, 1.2f-lastLinesBottom/3f/fieldHeight), minOf(1f, 0.4f+speed.lineDelay*0.1f))
				}

				field?.lineColorsCleared = IntArray(0)

				if(stat==Status.LINECLEAR) {
					resetStatc()
					when {
						ending==1 -> stat = Status.ENDINGSTART// Ending
						areLine>0||lagARE -> {
							// AREあり
							statc[0] = 0
							statc[1] = areLine
							statc[2] = 1
							stat = Status.ARE
						}
						interruptItemNumber!=INTERRUPTITEM_NONE -> {
							// 中断効果のあるアイテム処理
							nowPieceObject = null
							interruptItemPreviousStat = Status.MOVE
							stat = Status.INTERRUPTITEM
						}
						else -> {
							// AREなし
							nowPieceObject = null
							stat = Status.MOVE
						}
					}
				}
			}

			return
		}

		statc[0]++
	}

	/** ARE中の処理 */
	private fun statARE() {
		//  event 発生
		owner.mode?.also {if(it.onARE(this, playerID)) return}

		owner.receiver.onARE(this, playerID)
		if(statc[0]==0)
			if(field?.danger==true) loopSE("danger") else stopSE("danger")

		statc[0]++

		checkDropContinuousUse()

		// ARE cancel check
		delayCancelMoveLeft = ctrl.isPush(Controller.BUTTON_LEFT)
		delayCancelMoveRight = ctrl.isPush(Controller.BUTTON_RIGHT)

		val moveCancel = ruleopt.areCancelMove&&(ctrl.isPush(up)||ctrl.isPush(down)
			||delayCancelMoveLeft||delayCancelMoveRight)
		val rotateCancel = ruleopt.areCancelRotate&&(ctrl.isPush(Controller.BUTTON_A)||ctrl.isPush(Controller.BUTTON_B)
			||ctrl.isPush(Controller.BUTTON_C)||ctrl.isPush(Controller.BUTTON_E))
		val holdCancel = ruleopt.areCancelHold&&ctrl.isPush(Controller.BUTTON_D)

		delayCancel = moveCancel||rotateCancel||holdCancel

		if(statc[0]<statc[1]&&delayCancel) statc[0] = statc[1]

		// 横溜め
		if(ruleopt.dasInARE&&(statc[0]<statc[1]-1||ruleopt.dasInARELastFrame))
			padRepeat()
		else if(ruleopt.dasRedirectInARE) dasRedirect()

		// Next ステータス
		if(statc[0]>=statc[1]&&!lagARE) {
			nowPieceObject = null
			resetStatc()

			if(interruptItemNumber!=INTERRUPTITEM_NONE) {
				// 中断効果のあるアイテム処理
				interruptItemPreviousStat = Status.MOVE
				stat = Status.INTERRUPTITEM
			} else {
				// Blockピース移動処理
				initialRotate()
				stat = Status.MOVE
			}
		}
	}

	/** Ending突入処理 */
	private fun statEndingStart() {
		//  event 発生
		val animint = 6
		statc[3] = (field?.height ?: 0)*6
		owner.mode?.also {if(it.onEndingStart(this, playerID)) return}
		owner.receiver.onEndingStart(this, playerID)

		checkDropContinuousUse()
		// 横溜め
		if(ruleopt.dasInEndingStart) padRepeat()
		else if(ruleopt.dasRedirectInARE) dasRedirect()

		if(statc[2]==0) {
			timerActive = false
			owner.bgmStatus.bgm = BGM.Silent
			playSE("endingstart")
			statc[2] = 1
		}
		if(statc[0]<lineDelay) statc[0]++
		else if(statc[1]<statc[3]) {
			field?.let {field ->
				if(statc[1]%animint==0) {
					val y = field.height-statc[1]/animint
					field.setLineFlag(y, true)

					field.getRow(y).filterNotNull().filter {!it.isEmpty}.forEachIndexed {x, blk ->
						owner.mode?.blockBreak(this, playerID, x, y, blk)
						owner.receiver.blockBreak(this, x, y, blk)
						blk.color = null
					}
				}
			}

			statc[1]++
		} else if(statc[0]<lineDelay+2) statc[0]++
		else {
			ending = 2
			resetStatc()

			if(staffrollEnable&&gameActive&&isInGame) {
				field?.reset()
				nowPieceObject = null
				stat = Status.MOVE
			} else stat = Status.EXCELLENT
		}
	}

	/** 各ゲームMode が自由に使えるステータスの処理 */
	private fun statCustom() {
		//  event 発生
		owner.mode?.also {if(it.onCustom(this, playerID)) return}
		owner.receiver.onCustom(this, playerID)
	}

	/** Ending画面 */
	private fun statExcellent() {
		//  event 発生
		owner.mode?.also {if(it.onExcellent(this, playerID)) return}
		owner.receiver.onExcellent(this, playerID)

		if(statc[0]==0) {
			stopSE("danger")
			gameEnded()
			owner.bgmStatus.fadesw = true
			resetFieldVisible()
			temphanabi += 24
			playSE("excellent")
		}

		if(statc[0]>=120&&statc[1]<=0&&ctrl.isPush(Controller.BUTTON_A)) statc[0] = 600
		if(statc[0]>=600) {
			resetStatc()
			stat = Status.GAMEOVER
		} else statc[0]++
	}

	/** game overの処理 */
	private fun statGameOver() {
		//  event 発生
		owner.mode?.also {if(it.onGameOver(this, playerID)) return}
		owner.receiver.onGameOver(this, playerID)
		if(statc[0]==0) {
			//死亡時はgameActive中にStatus.GAMEOVERになる
			statc[2] = if(gameActive&&!staffrollNoDeath) 0 else 1
			stopSE("danger")
		}

		val topout = statc[2]==0
		if(!topout||lives<=0) {
			// もう復活できないとき
			val animint = 6
			statc[1] = animint*(field!!.height+1)
			if(statc[0]==0) {
				if(topout) playSE("dead_last")
				gameEnded()
				blockShowOutlineOnly = false
				if(owner.players<2) owner.bgmStatus.bgm = BGM.Silent

				if(field!!.isEmpty) statc[0] = statc[1]
				else {
					resetFieldVisible()
					if(ending==2&&!topout) playSE("gamewon")
					else playSE("shutter")
				}
			}
			when {
				statc[0]<statc[1] -> {
					for(x in 0 until field!!.width)
						if(field!!.getBlockColor(x, field!!.height-statc[0]/animint)!=Block.BLOCK_COLOR_NONE) {
							field!!.getBlock(x, field!!.height-statc[0]/animint)?.apply {
								if(ending==2&&!topout) {
									if(statc[0]%animint==0) {
										setAttribute(false, Block.ATTRIBUTE.OUTLINE)
										darkness = -.1f
										elapsedFrames = -1
									}
									alpha = 1f-(1+statc[0]%animint)*1f/animint
								} else if(statc[0]%animint==0) {
									if(!getAttribute(Block.ATTRIBUTE.GARBAGE)) {
										cint = Block.BLOCK_COLOR_GRAY
										setAttribute(true, Block.ATTRIBUTE.GARBAGE)
									}
									darkness = .3f
									elapsedFrames = -1
								}
							}
						}
					statc[0]++
				}
				statc[0]==statc[1] -> {
					if(topout) playSE("gamelost")
					else if(ending==2&&field!!.isEmpty) playSE("gamewon")
					statc[0]++
				}
				statc[0]<statc[1]+180 -> {
					if(statc[0]>=statc[1]+60&&ctrl.isPush(Controller.BUTTON_A)) statc[0] = statc[1]+180
					statc[0]++
				}
				else -> {
					if(!owner.replayMode||owner.replayRerecord) owner.saveReplay()

					for(i in 0 until owner.players)
						if(i==playerID||gameoverAll) {
							if(owner.engine[i].field!=null) owner.engine[i].field!!.reset()
							owner.engine[i].resetStatc()
							owner.engine[i].stat = Status.RESULT
						}
				}
			}
		} else {
			// 復活できるとき
			if(statc[0]==0) {
				if(topout) playSE("dead")
				//blockShowOutlineOnly=false;
				resetFieldVisible()
				for(i in field!!.hiddenHeight*-1 until field!!.height)
					for(j in 0 until field!!.width)
						if(field!!.getBlockColor(j, i)!=Block.BLOCK_COLOR_NONE) field!!.setBlockColor(j, i, Block.BLOCK_COLOR_GRAY)
				statc[0] = 1
			}
			if(!field!!.isEmpty) {
				val y = field!!.highestBlockY
				for(i in 0 until field!!.width) {
					field!!.getBlock(i, y)?.let {b ->
						b.color?.let {
							owner.mode?.blockBreak(this, playerID, i, y, b)
							owner.receiver.blockBreak(this, i, y, b)
							field!!.setBlockColor(i, y, Block.BLOCK_COLOR_NONE)
						}
					}
				}

			} else if(statc[1]<are) statc[1]++
			else {
				lives--
				resetStatc()
				stat = Status.MOVE
			}
		}
	}

	/** Results screen */
	private fun statResult() {
		// Event
		owner.bgmStatus.fadesw = false
		owner.bgmStatus.bgm = BGM.Result(
			when {
				ending==2 -> if(owner.mode?.gameIntensity==1) (if(statistics.time<10800) 1 else 2) else 3
				ending!=0 -> if(statistics.time<10800) 1 else 2
				else -> 0
			})

		owner.mode?.also {if(it.onResult(this, playerID)) return}
		owner.receiver.onResult(this, playerID)

		// Turn-off in-game flags
		gameActive = false
		timerActive = false
		isInGame = false

		// Cursor movement
		if(ctrl.isMenuRepeatKey(Controller.BUTTON_LEFT)||ctrl.isMenuRepeatKey(Controller.BUTTON_RIGHT)) {
			statc[0] = if(statc[0]==0) 1 else 0
			playSE("cursor")
		}

		// Confirm
		if(ctrl.isPush(Controller.BUTTON_A)) {
			playSE("decide")

			if(statc[0]==0) owner.reset()
			else quitflag = true

		}
	}

	/** fieldエディット画面 */
	private fun statFieldEdit() {
		//  event 発生
		owner.mode?.also {if(it.onFieldEdit(this, playerID)) return}
		owner.receiver.onFieldEdit(this, playerID)

		fldeditFrames++

		// Cursor movement
		if(ctrl.isMenuRepeatKey(Controller.BUTTON_LEFT, false)&&!ctrl.isPress(Controller.BUTTON_C)) {
			playSE("move")
			fldeditX--
			if(fldeditX<0) fldeditX = fieldWidth-1
		}
		if(ctrl.isMenuRepeatKey(Controller.BUTTON_RIGHT, false)&&!ctrl.isPress(Controller.BUTTON_C)) {
			playSE("move")
			fldeditX++
			if(fldeditX>fieldWidth-1) fldeditX = 0
		}
		if(ctrl.isMenuRepeatKey(up, false)) {
			playSE("move")
			fldeditY--
			if(fldeditY<0) fldeditY = fieldHeight-1
		}
		if(ctrl.isMenuRepeatKey(down, false)) {
			playSE("move")
			fldeditY++
			if(fldeditY>fieldHeight-1) fldeditY = 0
		}

		// 色選択
		if(ctrl.isMenuRepeatKey(Controller.BUTTON_LEFT, false)&&ctrl.isPress(Controller.BUTTON_C)) {
			playSE("cursor")
			fldeditColor--
			if(fldeditColor<Block.BLOCK_COLOR_GRAY) fldeditColor = Block.BLOCK_COLOR_GEM_PURPLE
		}
		if(ctrl.isMenuRepeatKey(Controller.BUTTON_RIGHT, false)&&ctrl.isPress(Controller.BUTTON_C)) {
			playSE("cursor")
			fldeditColor++
			if(fldeditColor>Block.BLOCK_COLOR_GEM_PURPLE) fldeditColor = Block.BLOCK_COLOR_GRAY
		}

		field?.let {field ->
			// 配置
			if(ctrl.isPress(Controller.BUTTON_A)&&fldeditFrames>10)
				try {
					if(field.getBlockColor(fldeditX, fldeditY)!=fldeditColor) {
						field.setBlock(fldeditX, fldeditY,
							Block(fldeditColor, skin, Block.ATTRIBUTE.VISIBLE, Block.ATTRIBUTE.OUTLINE))
						playSE("change")
					}
				} catch(e:Exception) {
				}

			// 消去
			if(ctrl.isPress(Controller.BUTTON_D)&&fldeditFrames>10)
				try {
					if(!field.getBlockEmpty(fldeditX, fldeditY)) {
						field.setBlock(fldeditX, fldeditY, null)
						playSE("change")
					}
				} catch(e:Exception) {
				}
		}
		// 終了
		if(ctrl.isPush(Controller.BUTTON_B)&&fldeditFrames>10) {
			stat = fldeditPreviousStat
			owner.mode?.fieldEditExit(this, playerID)
			owner.receiver.fieldEditExit(this, playerID)
		}
	}

	/** プレイ中断効果のあるアイテム処理 */
	private fun statInterruptItem() {
		var contFlag = false // 続行 flag

		when(interruptItemNumber) {
			INTERRUPTITEM_MIRROR // ミラー
			-> contFlag = interruptItemMirrorProc()
		}

		if(!contFlag) {
			interruptItemNumber = INTERRUPTITEM_NONE
			resetStatc()
			stat = interruptItemPreviousStat
		}
	}

	/** ミラー処理
	 * @return When true,ミラー処理続行
	 */
	fun interruptItemMirrorProc():Boolean {
		when {
			statc[0]==0 -> {
				// fieldをバックアップにコピー
				interruptItemMirrorField = field?.also {Field(it)} ?: Field()
				// fieldのBlockを全部消す
				field!!.reset()
			}
			statc[0]>=21&&statc[0]<21+field!!.width*2&&statc[0]%2==0 -> {
				// 反転
				val x = (statc[0]-20)/2-1

				for(y in field!!.hiddenHeight*-1 until field!!.height)
					field!!.setBlock(field!!.width-x-1, y, interruptItemMirrorField!!.getBlock(x, y))
			}
			statc[0]<21+field!!.width*2+5 -> {
				// 待ち time
			}
			else -> {
				// 終了
				statc[0] = 0
				interruptItemMirrorField = null
				return false
			}
		}

		statc[0]++
		return true
	}

	companion object {
		/** Log (Apache log4j) */
		internal var log = Logger.getLogger(GameEngine::class.java)

		/** Constants of game style
		 * (Currently not directly used by GameEngine, but from game modes) */
		const val GAMESTYLE_TETROMINO = 0
		const val GAMESTYLE_AVALANCHE = 1
		const val GAMESTYLE_PHYSICIAN = 2
		const val GAMESTYLE_SPF = 3

		/** Max number of game style */
		const val MAX_GAMESTYLE = 4

		/** Game style names */
		enum class GameStyle {
			TETROMINO, AVALANCHE, PHYSICIAN, SPF
		}

		val GAMESTYLE_NAMES = GameStyle.values().map {it.name}.toTypedArray()

		/** Number of free status counters (used by statc array) */
		const val MAX_STATC = 10

		/** Constants of block outline type */
		const val BLOCK_OUTLINE_AUTO = -1
		const val BLOCK_OUTLINE_NONE = 0
		const val BLOCK_OUTLINE_NORMAL = 1
		const val BLOCK_OUTLINE_CONNECT = 2
		const val BLOCK_OUTLINE_SAMECOLOR = 3

		/** Default duration of Ready->Go */
		const val READY_START = 0
		const val READY_END = 49
		const val GO_START = 50
		const val GO_END = 100

		/** Constants of frame colors */
		const val FRAME_COLOR_WHITE = 0
		const val FRAME_COLOR_GREEN = 1
		const val FRAME_COLOR_SILVER = 2
		const val FRAME_COLOR_RED = 3
		const val FRAME_COLOR_PINK = 4
		const val FRAME_COLOR_CYAN = 5
		const val FRAME_COLOR_BRONZE = 6
		const val FRAME_COLOR_PURPLE = 7
		const val FRAME_COLOR_BLUE = 8
		const val FRAME_COLOR_GRAY = 9
		const val FRAME_SKIN_GRADE = -1
		const val FRAME_SKIN_GB = -2
		const val FRAME_SKIN_SG = -3
		const val FRAME_SKIN_HEBO = -4
		const val FRAME_SKIN_METAL = -5

		/** Constants of meter colors */
		const val METER_COLOR_LEVEL = -1
		const val METER_COLOR_LIMIT = -2
		const val METER_COLOR_RED = 0x10000
		const val METER_COLOR_ORANGE = 0x8000
		const val METER_COLOR_YELLOW = 0x100
		const val METER_COLOR_GREEN = 0xff0100
		const val METER_COLOR_DARKGREEN = 0xff8000
		const val METER_COLOR_CYAN = 0xff0001
		const val METER_COLOR_DARKBLUE = 0xffff80
		const val METER_COLOR_BLUE = 0xffff01
		const val METER_COLOR_PURPLE = 0x7fff01
		const val METER_COLOR_PINK = 0xff01

		/** Constants of Twister Mini detection type */
		const val TWISTMINI_TYPE_ROTATECHECK = 0
		const val TWISTMINI_TYPE_WALLKICKFLAG = 1

		/** Constants of combo type */
		const val COMBO_TYPE_DISABLE = 0
		const val COMBO_TYPE_NORMAL = 1
		const val COMBO_TYPE_DOUBLE = 2

		/** Constants of gameplay-interruptable items */
		const val INTERRUPTITEM_NONE = 0
		const val INTERRUPTITEM_MIRROR = 1

		/** Table for cint-block item */
		val ITEM_COLOR_BRIGHT_TABLE =
			intArrayOf(10, 10, 9, 9, 8, 8, 8, 7, 7, 7, 6, 6, 6, 5, 5, 5, 4, 4, 4, 4, 3, 3, 3, 3, 2, 2, 2, 2, 1, 1, 1, 1, 0, 0, 0,
				0, 0, 0, 0, 0)

		/** Default list of block colors to use for random block colors. */
		val BLOCK_COLORS_DEFAULT =
			intArrayOf(Block.BLOCK_COLOR_RED, Block.BLOCK_COLOR_ORANGE, Block.BLOCK_COLOR_YELLOW, Block.BLOCK_COLOR_GREEN,
				Block.BLOCK_COLOR_CYAN, Block.BLOCK_COLOR_BLUE, Block.BLOCK_COLOR_PURPLE)

		const val HANABI_INTERVAL = 10

		val EXPLOD_SIZE_DEFAULT =
			arrayOf(intArrayOf(4, 3), intArrayOf(3, 0), intArrayOf(3, 1), intArrayOf(3, 2), intArrayOf(3, 3), intArrayOf(4, 4),
				intArrayOf(5, 5), intArrayOf(5, 5), intArrayOf(6, 6), intArrayOf(6, 6), intArrayOf(7, 7))
	}

	/** Constants of main game status */
	enum class Status {
		NOTHING, SETTING, READY, MOVE, LOCKFLASH, LINECLEAR, ARE, ENDINGSTART, CUSTOM, EXCELLENT, GAMEOVER, RESULT,
		FIELDEDIT, INTERRUPTITEM
	}

	/** Constants of last successful movements */
	enum class LastMove {
		NONE, FALL_AUTO, FALL_SELF, SLIDE_AIR, SLIDE_GROUND, ROTATE_AIR, ROTATE_GROUND
	}

	data class ScoreEvent(val lines:Int = 0, val split:Boolean = false, val piece:Piece? = null, val twist:Twister? = null,
		val b2b:Boolean = false) {

		override fun equals(other:Any?):Boolean {
			if(this===other) return true
			if(javaClass!=other?.javaClass) return false

			other as ScoreEvent

			return !(lines!=other.lines||split!=other.split||piece!=other.piece||twist!=other.twist||b2b!=other.b2b)
		}

		override fun hashCode():Int {
			var result = piece.hashCode()
			result = (Twister.values().size+1)*result+1+(twist?.ordinal ?: -1)
			result = 10*result+minOf(maxOf(0, lines), 9)
			result = 4*result+split.toInt()+b2b.toInt()*2
			return result
		}

		companion object {
			fun parseInt(i:Int):ScoreEvent? = if(i<0) null
			else {
				val lines = (i shr 2)%10
				val twist = (i shr 2)/10%(Twister.values().size+1)
				val piece = (i shr 2)/10/(Twister.values().size+1)%Piece.Shape.values().size
				ScoreEvent(lines, i%2==1, if(piece<=0) null else Piece(piece-1).apply {},
					Twister.values().getOrNull(twist-1), (i shr 1)%2==1)
			}

			fun parseInt(i:String):ScoreEvent? = parseInt(i.toInt())
		}
	}

	/** Line gravity types */
	enum class LineGravity {
		NATIVE, CASCADE, CASCADE_SLOW
	}

	/** Clear mode settings */
	enum class ClearType {
		LINE, COLOR, LINE_COLOR, GEM_COLOR, LINE_GEM_BOMB, LINE_GEM_SPARK
	}

	enum class Twister {
		IMMOBILE_EZ, IMMOBILE_MINI, POINT_MINI, IMMOBILE, POINT;

		val isMini:Boolean get() = this==POINT_MINI||this==IMMOBILE_MINI
	}

	enum class MeterColor(color:Int) {
		LEVEL(-1), LIMIT(-2),
		RED(0xFF0000), ORANGE(0xFF8000), YELLOW(0xFFFF00), GREEN(0x00ff00), DARKGREEN(0x008000),
		CYAN(0x00FFFF), DARKBLUE(0x0000FF), BLUE(0x0080FF), PURPLE(0x8000FF), PINK(0xff0080)
	}
}
