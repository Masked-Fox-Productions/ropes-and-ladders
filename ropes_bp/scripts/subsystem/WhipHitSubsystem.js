import { world } from "@minecraft/server";
import {
  WHIP_ITEM_ID,
  WHIP_HIT_PARTICLE,
  WHIP_HIT_PARTICLE_Y_OFFSET,
  WHIP_KNOCKBACK_HORIZONTAL,
  WHIP_KNOCKBACK_VERTICAL,
} from "../util/RopeConstants.js";

export class WhipHitSubsystem {
  constructor(ropeManager) {
    this._manager = ropeManager;
  }

  register() {
    world.afterEvents.entityHurt.subscribe((event) => this._handleHurt(event));
  }

  _handleHurt(event) {
    const attacker = event.damageSource?.damagingEntity;
    if (attacker?.typeId !== "minecraft:player") return;

    let heldId;
    try {
      const container = attacker.getComponent("inventory")?.container;
      heldId = container?.getItem(attacker.selectedSlotIndex)?.typeId;
    } catch { return; }
    if (heldId !== WHIP_ITEM_ID) return;

    const hurtEntity = event.hurtEntity;
    const loc = hurtEntity?.location;
    if (!loc) return;

    try {
      hurtEntity.dimension.spawnParticle(WHIP_HIT_PARTICLE, {
        x: loc.x,
        y: loc.y + WHIP_HIT_PARTICLE_Y_OFFSET,
        z: loc.z,
      });
    } catch { /* particle spawn may fail in an unloaded chunk */ }

    this._applyKnockback(attacker, hurtEntity, loc);
  }

  _applyKnockback(attacker, hurtEntity, loc) {
    try {
      const origin = attacker.location;
      let dx = loc.x - origin.x;
      let dz = loc.z - origin.z;
      let len = Math.hypot(dx, dz);
      if (len < 0.0001) {
        const dir = attacker.getViewDirection?.() ?? { x: 0, z: 1 };
        dx = dir.x;
        dz = dir.z;
        len = Math.hypot(dx, dz);
      }
      if (len < 0.0001) return;
      dx /= len;
      dz /= len;

      hurtEntity.applyKnockback(
        { x: dx * WHIP_KNOCKBACK_HORIZONTAL, z: dz * WHIP_KNOCKBACK_HORIZONTAL },
        WHIP_KNOCKBACK_VERTICAL
      );
    } catch { /* knockback may fail on some entities */ }
  }
}
