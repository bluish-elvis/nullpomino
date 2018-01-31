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
package mu.nu.nullpo.game.subsystem.wallkick

import mu.nu.nullpo.game.component.*

/** ClassicPlusWallkick - I型もWallkickができるクラシックルールなWallkick
 * (旧VersionのCLASSIC3相当） */
class ClassicPlusWallkick:Wallkick {
	/* Wallkick */
	override fun executeWallkick(x:Int, y:Int, rtDir:Int, rtOld:Int, rtNew:Int, allowUpward:Boolean, piece:Piece,
		field:Field, ctrl:Controller?):WallkickResult? {
		var check = 0
		if(piece.big) check = 1

		// 通常のWallkick (I以外）
		if(piece.id!=Piece.PIECE_I)
			if(checkCollisionKick(piece, x, y, rtNew, field)||piece.id==Piece.PIECE_I2
				||piece.id==Piece.PIECE_L3) {
				var temp = 0

				if(!piece.checkCollision(x-1-check, y, rtNew, field)) temp = -1-check
				if(!piece.checkCollision(x+1+check, y, rtNew, field)) temp = 1+check

				if(temp!=0) return WallkickResult(temp, 0, rtNew)
			}

		// Tの脱出
		if(piece.id==Piece.PIECE_T&&allowUpward&&rtNew==Piece.DIRECTION_UP)
			if(!piece.checkCollision(x, y-1
					-check, rtNew, field))
				return WallkickResult(0, -1-check, rtNew)

		// IのWallkick
		if(piece.id==Piece.PIECE_I&&(rtNew==Piece.DIRECTION_UP||rtNew==Piece.DIRECTION_DOWN))
			for(i in check..check*2) {
				var temp = 0

				if(!piece.checkCollision(x-1-i, y, rtNew, field))
					temp = -1-i
				else if(!piece.checkCollision(x+1+i, y, rtNew, field))
					temp = 1+i
				else if(!piece.checkCollision(x+2+i, y, rtNew, field)) temp = 2+i

				if(temp!=0) return WallkickResult(temp, 0, rtNew)
			}

		// Iの床蹴り (接地している場合のみ）
		if(piece.id==Piece.PIECE_I&&allowUpward&&(rtNew==Piece.DIRECTION_LEFT||rtNew==Piece.DIRECTION_RIGHT)&&
			piece.checkCollision(x, y+1, field))

			for(i in check..check*2) {
				var temp = 0

				if(!piece.checkCollision(x, y-1-i, rtNew, field))
					temp = -1-i
				else if(!piece.checkCollision(x, y-2-i, rtNew, field)) temp = -2-i

				if(temp!=0) return WallkickResult(0, temp, rtNew)
			}

		return null
	}

	/** Wallkick可能かどうか調べる
	 * @param piece Blockピース
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param rt Direction
	 * @param fld field
	 * @return Wallkick可能ならtrue
	 */
	private fun checkCollisionKick(piece:Piece, x:Int, y:Int, rt:Int, fld:Field):Boolean {
		// Bigでは専用処理
		if(piece.big) return checkCollisionKickBig(piece, x, y, rt, fld)

		for(i in 0 until piece.maxBlock)
			if(piece.dataX[rt][i]!=1+piece.dataOffsetX[rt]) {
				val x2 = x+piece.dataX[rt][i]
				val y2 = y+piece.dataY[rt][i]

				if(x2>=fld.width) return true
				if(y2>=fld.height) return true
				if(fld.getCoordAttribute(x2, y2)==Field.COORD_WALL) return true
				if(fld.getCoordAttribute(x2, y2)!=Field.COORD_VANISH&&fld.getBlockColor(x2, y2)!=Block.BLOCK_COLOR_NONE)
					return true
			}

		return false
	}

	/** Wallkick可能かどうか調べる (Big用）
	 * @param piece Blockピース
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param rt Direction
	 * @param fld field
	 * @return Wallkick可能ならtrue
	 */
	private fun checkCollisionKickBig(piece:Piece, x:Int, y:Int, rt:Int, fld:Field):Boolean {
		for(i in 0 until piece.maxBlock)
			if(piece.dataX[rt][i]!=1+piece.dataOffsetX[rt]) {
				val x2 = x+piece.dataX[rt][i]*2
				val y2 = y+piece.dataY[rt][i]*2

				// 4Block分調べる
				for(k in 0..1)
					for(l in 0..1) {
						val x3 = x2+k
						val y3 = y2+l

						if(x3>=fld.width) return true
						if(y3>=fld.height) return true
						if(fld.getCoordAttribute(x3, y3)==Field.COORD_WALL) return true
						if(fld.getCoordAttribute(x3, y3)!=Field.COORD_VANISH&&fld.getBlockColor(x3, y3)!=Block.BLOCK_COLOR_NONE)
							return true
					}
			}

		return false
	}
}
