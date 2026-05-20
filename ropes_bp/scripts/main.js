import { world, system, BlockPermutation } from "@minecraft/server";
import { ROPE_BLOCK_ID, ROPE_LADDER_BLOCK_ID } from "./util/RopeConstants.js";
import { RopeManager } from "./RopeManager.js";
import { initRopes } from "./init.js";

console.log("[ropes] === Mod initializing ===");

let ropeManager;
let ropeInteraction;
let ropeBreak;

system.beforeEvents.startup.subscribe((ev) => {
  ev.blockComponentRegistry.registerCustomComponent("ropes:rope_component", {
    onPlayerInteract(e) {
      if (ropeInteraction) ropeInteraction.handleInteract(e.block, e.player, e.block.dimension);
    },
    onPlayerBreak(e) {
      if (ropeBreak) ropeBreak.handleBreak(e);
    },
    beforeOnPlayerPlace(e) {
      const face = (e.face ?? "Up").toLowerCase();
      e.permutationToPlace = BlockPermutation.resolve(ROPE_BLOCK_ID, {
        "ropes:rope_state": "coiled",
        "ropes:face": face,
      });
    },
  });

  ev.blockComponentRegistry.registerCustomComponent("ropes:rope_ladder_component", {
    onPlayerInteract(e) {
      if (ropeInteraction) ropeInteraction.handleInteract(e.block, e.player, e.block.dimension);
    },
    onPlayerBreak(e) {
      if (ropeBreak) ropeBreak.handleBreak(e);
    },
    beforeOnPlayerPlace(e) {
      const face = (e.face ?? "North").toLowerCase();
      const wallFaces = new Set(["north", "south", "east", "west"]);
      if (!wallFaces.has(face)) {
        e.cancel = true;
        return;
      }
      e.permutationToPlace = BlockPermutation.resolve(ROPE_LADDER_BLOCK_ID, {
        "ropes:ladder_state": "coiled",
        "ropes:face": face,
      });
    },
  });
});

ropeManager = new RopeManager();

world.afterEvents.worldLoad.subscribe(() => {
  console.log("[ropes] worldLoad fired — loading persistence");
  ropeManager.load();
});

system.run(() => {
  console.log("[ropes] Fallback load triggered");
  ropeManager.load();
});

const ropeHandlers = initRopes(ropeManager);
ropeInteraction = ropeHandlers.interaction;
ropeBreak = ropeHandlers.breakHandler;

console.log("[ropes] === Initialization complete ===");

export { ropeManager };
