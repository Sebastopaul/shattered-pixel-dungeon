/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2024 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.cheatedpixel.cheatedpixeldungeon.levels.traps;

import com.cheatedpixel.cheatedpixeldungeon.Assets;
import com.cheatedpixel.cheatedpixeldungeon.Badges;
import com.cheatedpixel.cheatedpixeldungeon.Dungeon;
import com.cheatedpixel.cheatedpixeldungeon.ShatteredPixelDungeon;
import com.cheatedpixel.cheatedpixeldungeon.actors.Actor;
import com.cheatedpixel.cheatedpixeldungeon.actors.Char;
import com.cheatedpixel.cheatedpixeldungeon.actors.hero.Hero;
import com.cheatedpixel.cheatedpixeldungeon.items.weapon.missiles.darts.Dart;
import com.cheatedpixel.cheatedpixeldungeon.mechanics.Ballistica;
import com.cheatedpixel.cheatedpixeldungeon.messages.Messages;
import com.cheatedpixel.cheatedpixeldungeon.sprites.MissileSprite;
import com.cheatedpixel.cheatedpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

public class WornDartTrap extends Trap {

	{
		color = GREY;
		shape = CROSSHAIR;
		
		canBeHidden = false;
		avoidsHallways = true;
	}

	@Override
	public void activate() {

		//we handle this inside of a separate actor as the trap may produce a visual effect we need to pause for
		Actor.add(new Actor() {

			{
				actPriority = VFX_PRIO;
			}

			@Override
			protected boolean act() {
				Actor.remove(this);
				Char target = Actor.findChar(pos);

				//find the closest char that can be aimed at
				//can't target beyond view distance, with a min of 6 (torch range)
				int range = Math.max(6, Dungeon.level.viewDistance);
				if (target == null){
					float closestDist = Float.MAX_VALUE;
					for (Char ch : Actor.chars()){
						if (!ch.isAlive()) continue;
						float curDist = Dungeon.level.trueDistance(pos, ch.pos);
						//invis targets are considered to be at max range
						if (ch.invisible > 0) curDist = Math.max(curDist, range);
						Ballistica bolt = new Ballistica(pos, ch.pos, Ballistica.PROJECTILE);
						if (bolt.collisionPos == ch.pos
								&& ( curDist < closestDist || (curDist == closestDist && target instanceof Hero))){
							target = ch;
							closestDist = curDist;
						}
					}
					if (closestDist > range){
						target = null;
					}
				}

				if (target != null) {
					final Char finalTarget = target;
					if (Dungeon.level.heroFOV[pos] || Dungeon.level.heroFOV[target.pos]) {
						((MissileSprite) ShatteredPixelDungeon.scene().recycle(MissileSprite.class)).
								reset(pos, finalTarget.sprite, new Dart(), new Callback() {
									@Override
									public void call() {
										int dmg = Random.NormalIntRange(4, 8) - finalTarget.drRoll();
										finalTarget.damage(dmg, WornDartTrap.this);
										if (finalTarget == Dungeon.hero && !finalTarget.isAlive()){
											Dungeon.fail( WornDartTrap.this  );
											GLog.n(Messages.get(WornDartTrap.class, "ondeath"));
											if (reclaimed) Badges.validateDeathFromFriendlyMagic();
										}
										Sample.INSTANCE.play(Assets.Sounds.HIT, 1, 1, Random.Float(0.8f, 1.25f));
										finalTarget.sprite.bloodBurstA(finalTarget.sprite.center(), dmg);
										finalTarget.sprite.flash();
										next();
									}
								});
						return false;
					} else {
						finalTarget.damage(Random.NormalIntRange(4, 8) - finalTarget.drRoll(), WornDartTrap.this);
						return true;
					}
				} else {
					return true;
				}
			}

		});
	}
}