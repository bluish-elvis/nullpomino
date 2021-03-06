package mu.nu.nullpo.gui.slick.img.bg

import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.gui.slick.ResourceHolderCustomAssetExtension
import kotlin.random.Random

class BackgroundFakeScanlines:AnimatedBackgroundHook {
	// private ResourceHolderCustomAssetExtension customHolder;
	private var colorRandom:Random = Random.Default
	private var chunks:Array<ImageChunk> = emptyArray()
	private var phase = 0

	constructor(bgNumber:Int) {
		var bgNumber = bgNumber
		if(bgNumber<0||bgNumber>19) bgNumber = 0
		customHolder = ResourceHolderCustomAssetExtension()
		customHolder.loadImage("res/graphics/back$bgNumber.png", imageName)
		setup()
		log.debug("Non-custom fake scanline background ($bgNumber) created.")
	}

	constructor(filePath:String) {
		customHolder = ResourceHolderCustomAssetExtension()
		customHolder.loadImage(filePath, imageName)
		setup()
		log.debug("Custom fake scanline background created (File Path: $filePath).")
	}

	override fun setBG(bg:Int) {
		customHolder.loadImage("res/graphics/back$bg.png", imageName)
		log.debug("Non-custom horizontal bars background modified (New BG: $bg).")
	}

	override fun setBG(filePath:String) {
		customHolder.loadImage(filePath, imageName)
		log.debug("Custom horizontal bars background modified (New File Path: $filePath).")
	}
	/**
	 * Allows the hot-swapping of pre-loaded BGs from a storage instance of a `ResourceHolderCustomAssetExtension`.
	 *
	 * @param holder Storage instance
	 * @param name   Image name
	 */
	override fun setBGFromHolder(holder:ResourceHolderCustomAssetExtension, name:String) {
		customHolder.putImageAt(holder.getImageAt(name), imageName)
		log.debug("Custom horizontal bars background modified (New Image Reference: $name).")
	}

	private fun setup() {

		// Generate chunks
		chunks = Array(AMT) {i ->
			ImageChunk(ImageChunk.ANCHOR_POINT_TL, intArrayOf(0, 480/AMT*i+480/AMT/2), intArrayOf(0, 480/AMT*i),
				intArrayOf(640, 480/AMT), floatArrayOf(1f, 1f))
		}
		phase = 0
	}

	override fun update() {
		for(chunk in chunks) {
			val newScale = (0.01f*colorRandom.nextDouble()).toFloat()+0.995f
			chunk.scale = floatArrayOf(newScale, 1f)
		}
		phase = (phase+1)%PERIOD
	}

	override fun reset() {
		phase = 0
		update()
	}

	override fun draw(engine:GameEngine, playerID:Int) {
		for(id in chunks.indices) {
			var col = 1f-BASE_LUMINANCE_OFFSET
			if(id and 2==0) col -= BASE_LUMINANCE_OFFSET
			if(phase>=PERIOD/2&&(id==phase-PERIOD/2||id==1+phase-PERIOD/2||id==-1+phase-PERIOD/2)) {
				col += BASE_LUMINANCE_OFFSET
			}

			// Randomness offset
			col -= (0.025*colorRandom.nextDouble()).toFloat()
			val color = (255*col).toInt()
			val pos = chunks[id].drawLocation
			val ddim = chunks[id].drawDimensions
			val sloc = chunks[id].sourceLocation
			val sdim = chunks[id].sourceDimensions
			customHolder.drawImage(imageName, pos[0], pos[1], ddim[0], ddim[1], sloc[0], sloc[1], sdim[0],
				sdim[1], color, color, color, 255)
		}
	}
	/**
	 * This last one is important. In the case that any of the child types are used, it allows identification.
	 * The identification can be used to allow casting during operations.
	 *
	 * @return Identification number of child class.
	 */
	override val ID:Int = ANIMATION_FAKE_SCANLINES

	companion object {
		private const val AMT = 480/2
		private const val PERIOD = 480 // Frames
		private const val BASE_LUMINANCE_OFFSET = 0.25f
	}

	init {
		imageName = "localBG"
	}
}