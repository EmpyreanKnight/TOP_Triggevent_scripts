// PROP: scriptName=TOP_P5_Delta_HW
// PROP: strict=false
// PROP: startup=true

import java.time.Duration;
import java.util.stream.Collectors;

def TOP_P5_Delta_HW_AM(s) {
    // near/remote tethers (0xC8=near(christmas), 0xC9=remote (blue))
    List<TetherEvent> tethers = s.waitEventsQuickSuccession(4, TetherEvent.class, te -> te.tetherIdMatches(0xC8, 0xC9), Duration.ofMillis(300));
    log.info("[TOP_P5_Delta_HW_AM] Tethers: {}", tethers);
    
    s.waitMs(100);
    // near/dist HW debuff
    XivPlayerCharacter near = (XivPlayerCharacter) statusEffectRepository.findBuffById(0xD72).getTarget();
    XivPlayerCharacter dist = (XivPlayerCharacter) statusEffectRepository.findBuffById(0xD73).getTarget();
    log.info("[TOP_P5_Delta_HW_AM] HW near: {}", near);
    log.info("[TOP_P5_Delta_HW_AM] HW dist: {}", dist);
    
    // find remote tethers
    List<TetherEvent> remoteTethers = tethers.stream().filter(tether -> tether.tetherIdMatches(0xC8)).toList();
    List<TetherEvent> nearTethers = tethers.stream().filter(tether -> tether.tetherIdMatches(0xC9)).toList();
    log.info("[TOP_P5_Delta_HW_AM] Remote tethers: {}", remoteTethers);
    s.accept(new SpecificAutoMarkRequest(remoteTethers.get(0).getSource(), MarkerSign.ATTACK1)); // dist baiter 1
    s.accept(new SpecificAutoMarkRequest(remoteTethers.get(0).getTarget(), MarkerSign.ATTACK2)); // dist baiter 2
    s.accept(new SpecificAutoMarkRequest(remoteTethers.get(1).getSource(), MarkerSign.BIND1));   // near baiter 1
    s.accept(new SpecificAutoMarkRequest(remoteTethers.get(1).getTarget(), MarkerSign.BIND2));   // near baiter 2

    s.accept(new SpecificAutoMarkRequest(nearTethers.get(0).getSource(), MarkerSign.IGNORE1));  // short blue
    s.accept(new SpecificAutoMarkRequest(nearTethers.get(0).getTarget(), MarkerSign.IGNORE2));  // short blue
    
    // find leftover players
    List<XivPlayerCharacter> leftovers = new ArrayList<>(state.partyList);
    leftovers.remove(near);
    leftovers.remove(dist);
    leftovers.removeAll(remoteTethers.stream().map(tether -> tether.getTargets()).toList().flatten());
    log.info("[TOP_P5_Delta_HW_AM] Leftover players: {}", leftovers);
    
    //s.waitMs(22000);
    // optical laser
    s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x7B21));
    s.accept(new SpecificAutoMarkRequest(nearTethers.get(0).getSource(), MarkerSign.IGNORE1));  // short blue cancel
    s.accept(new SpecificAutoMarkRequest(nearTethers.get(0).getTarget(), MarkerSign.IGNORE2));  // short blue cancel
    
    // swivel cannon
    s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x7B94, 0x7B95));
    
    s.accept(new SpecificAutoMarkRequest(near, MarkerSign.CIRCLE)); // HW near
    s.accept(new SpecificAutoMarkRequest(dist, MarkerSign.CROSS));  // HW dist
    s.accept(new SpecificAutoMarkRequest(leftovers.get(0), MarkerSign.IGNORE1)); // avoid aoe 1
    s.accept(new SpecificAutoMarkRequest(leftovers.get(1), MarkerSign.IGNORE2)); // avoid aoe 2

    s.waitEvent(BuffRemoved.class, br -> br.buffIdMatches(0xD72));
    s.accept(new ClearAutoMarkRequest());
}


groovyTriggers.add({	
	named "TOP_P5_Delta_HW_AM"
	// Run: Dynamis (Delta)
	when { AbilityCastStart acs -> acs.abilityIdMatches(0x7B88) }  
	sequence { e1, s -> {
        TOP_P5_Delta_HW_AM(s);
	}}
});
