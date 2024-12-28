// PROP: scriptName=TOP_P3_Sniper
// PROP: strict=false
// PROP: startup=true

generalJobPrio = [
	Job.WHM, Job.AST, 
	Job.PLD, Job.WAR, Job.GNB, Job.DRK, 
	Job.NIN, Job.SAM, Job.MNK, Job.RPR, Job.DRG,
	Job.BRD, Job.MCH, Job.DNC, 
	Job.BLM, Job.RDM, Job.SMN, 
	Job.SCH, Job.SGE
]

def TOP_P3_Sniper_AM(s) {
    s.waitMs(100);

	// D61 = sniper (spread), D62 = high pitch sniper (2p stack)
	List<XivPlayerCharacter> sniperPlayers = statusEffectRepository.findBuffs(ba -> ba.buffIdMatches(0xD61)).stream()
		.map(ba -> ba.getTarget()).map(XivPlayerCharacter.class::cast)
		.sorted(Comparator.comparing(player -> generalJobPrio.indexOf(player.job)))
		.toList();
	log.info("[TOP_P3_Monitor_AM] Snipers: {}", sniperPlayers);
		
	List<XivPlayerCharacter> hpSniperPlayers = statusEffectRepository.findBuffs(ba -> ba.buffIdMatches(0xD62)).stream()
		.map(ba -> ba.getTarget()).map(XivPlayerCharacter.class::cast)
		.sorted(Comparator.comparing(player -> generalJobPrio.indexOf(player.job)))
		.toList();
	log.info("[TOP_P3_Monitor_AM] HP Sniper: {}", hpSniperPlayers);
		
	List<XivPlayerCharacter> nonTargetPlayers = new ArrayList<>(state.partyList);
	nonTargetPlayers.removeAll(sniperPlayers);
	nonTargetPlayers.removeAll(hpSniperPlayers);
	nonTargetPlayers.sort(Comparator.comparing(player -> generalJobPrio.indexOf(player.job)));
	log.info("[TOP_P3_Monitor_AM] Non target: {}", nonTargetPlayers);

	eventMaster.pushEvent(new SpecificAutoMarkRequest(sniperPlayers.get(0), MarkerSign.ATTACK1));
	eventMaster.pushEvent(new SpecificAutoMarkRequest(sniperPlayers.get(1), MarkerSign.ATTACK2));
	eventMaster.pushEvent(new SpecificAutoMarkRequest(sniperPlayers.get(2), MarkerSign.ATTACK3));
	eventMaster.pushEvent(new SpecificAutoMarkRequest(sniperPlayers.get(3), MarkerSign.ATTACK4));
	eventMaster.pushEvent(new SpecificAutoMarkRequest(hpSniperPlayers.get(0), MarkerSign.BIND1));
	eventMaster.pushEvent(new SpecificAutoMarkRequest(hpSniperPlayers.get(1), MarkerSign.BIND2));
	eventMaster.pushEvent(new SpecificAutoMarkRequest(nonTargetPlayers.get(0), MarkerSign.IGNORE1));
	eventMaster.pushEvent(new SpecificAutoMarkRequest(nonTargetPlayers.get(1), MarkerSign.IGNORE2));

    // Wait for rings to go off
    // 7B4F = initial cast (inner circle)
    // 7B50 = second circle
    // 7B51 = third circle
    // 7B52 = outermost circle
    s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x7B52) && aue.isFirstTarget()); // 1st time ring explosion
    s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x7B52) && aue.isFirstTarget()); // 2nd time ring explosion
    s.accept(new ClearAutoMarkRequest());
}

groovyTriggers.add({
	named "TOP_P3_Sniper_AM"
	// sniper cannon
	when { BuffApplied ba -> ba.buffIdMatches(0xD61) }
	sequence { e1, s -> {
        TOP_P3_Sniper_AM(s);
	}}
});