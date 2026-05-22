import { describe, it, beforeEach } from "node:test";
import assert from "node:assert/strict";
import { __reset } from "./stubs/minecraft-server.mjs";
import { RopeManager } from "../ropes_bp/scripts/RopeManager.js";
import { WhipHitSubsystem } from "../ropes_bp/scripts/subsystem/WhipHitSubsystem.js";
import { WHIP_HIT_PARTICLE, WHIP_HIT_PARTICLE_Y_OFFSET } from "../ropes_bp/scripts/util/RopeConstants.js";

function makeAttacker(heldId = "ropes:whip") {
  const items = heldId ? { 0: { typeId: heldId, amount: 1 } } : {};
  return {
    typeId: "minecraft:player",
    selectedSlotIndex: 0,
    getComponent(n) {
      if (n !== "inventory") return null;
      return { container: { getItem(i) { return items[i] ?? null; } } };
    },
  };
}

function makeHurtEntity(location, particleSink) {
  return {
    location,
    dimension: {
      spawnParticle(id, loc) { particleSink.push({ id, loc }); },
    },
  };
}

describe("WhipHitSubsystem", () => {
  let sub;

  beforeEach(() => {
    __reset();
    sub = new WhipHitSubsystem(new RopeManager());
  });

  it("spawns a particle at the hit entity when the attacker holds the whip", () => {
    const particles = [];
    const hurtEntity = makeHurtEntity({ x: 10, y: 64, z: -3 }, particles);

    sub._handleHurt({
      damageSource: { damagingEntity: makeAttacker("ropes:whip") },
      hurtEntity,
    });

    assert.equal(particles.length, 1);
    assert.equal(particles[0].id, WHIP_HIT_PARTICLE);
    assert.deepEqual(particles[0].loc, { x: 10, y: 64 + WHIP_HIT_PARTICLE_Y_OFFSET, z: -3 });
  });

  it("does nothing when the attacker holds a different item", () => {
    const particles = [];
    const hurtEntity = makeHurtEntity({ x: 0, y: 64, z: 0 }, particles);

    sub._handleHurt({
      damageSource: { damagingEntity: makeAttacker("minecraft:diamond_sword") },
      hurtEntity,
    });

    assert.equal(particles.length, 0);
  });

  it("does nothing when the damaging entity is not a player", () => {
    const particles = [];
    const hurtEntity = makeHurtEntity({ x: 0, y: 64, z: 0 }, particles);

    sub._handleHurt({
      damageSource: { damagingEntity: { typeId: "minecraft:zombie" } },
      hurtEntity,
    });

    assert.equal(particles.length, 0);
  });

  it("does nothing when there is no damaging entity", () => {
    const particles = [];
    const hurtEntity = makeHurtEntity({ x: 0, y: 64, z: 0 }, particles);

    sub._handleHurt({ damageSource: {}, hurtEntity });

    assert.equal(particles.length, 0);
  });
});
