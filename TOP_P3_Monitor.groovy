// PROP: scriptName=TOP_P3_Monitor
// PROP: strict=false
// PROP: startup=true

monitorJobPrio = [
	Job.WHM, Job.AST, 
     Job.BRD, Job.MCH, Job.DNC, 
     Job.BLM, Job.RDM, Job.SMN, 
     Job.SCH, Job.SGE,
     Job.PLD, Job.WAR, Job.GNB, Job.DRK, 
     Job.NIN, Job.SAM, Job.MNK, Job.RPR, Job.DRG
]


groovyTriggers.add({
	named "TOP_P3_Monitor_AM"
	// oversampled wave cannon
	when { AbilityCastStart acs -> acs.abilityIdMatches(0x7B6B, 0x7B6C) }
	sequence { e1, s -> 
		//log.error("Oversampled Wave Cannon Loading")
		// event, context
		s.waitMs(100);
		// D7C is right monitor, D7D is left monitor
		//List<BuffApplied> monitorBuffs = statusEffectRepository.getBuffs().stream().filter(ba -> ba.buffIdMatches(0xD7C, 0xD7D)).toList();
		List<BuffApplied> monitorBuffs = statusEffectRepository.findBuffs(ba -> ba.buffIdMatches(0xD7C, 0xD7D));
		List<XivPlayerCharacter> monitorPlayers = monitorBuffs.stream()
			.map(ba -> ba.getTarget())
			.map(XivPlayerCharacter.class::cast)
			.sorted(Comparator.comparing(player -> monitorJobPrio.indexOf(player.job)))
			.toList();
			
		List<XivPlayerCharacter> nonMonitorPlayers = new ArrayList<>(state.partyList);
		nonMonitorPlayers.removeAll(monitorPlayers);
		nonMonitorPlayers.sort(Comparator.comparing(player -> monitorJobPrio.indexOf(player.job)));

		log.info("[TOP_P3_Monitor_AM] Monitors: {}", monitorPlayers);
		log.info("[TOP_P3_Monitor_AM] Non monitors: {}", nonMonitorPlayers);

		// ------1-|----^----
		// -----2--|----|----
		// --------|----b1---
		// --3-----|---------
		// --------|>>>>>>>>>
		// --4-----|---------
		// --------|----b2---
		// --<-b3--|----|----
		// ------5-|----v----
		s.accept(new SpecificAutoMarkRequest(monitorPlayers.get(0), MarkerSign.BIND1));
		s.accept(new SpecificAutoMarkRequest(monitorPlayers.get(1), MarkerSign.BIND2));
		s.accept(new SpecificAutoMarkRequest(monitorPlayers.get(2), MarkerSign.BIND3));
		s.accept(new SpecificAutoMarkRequest(nonMonitorPlayers.get(0), MarkerSign.ATTACK1));
		s.accept(new SpecificAutoMarkRequest(nonMonitorPlayers.get(1), MarkerSign.ATTACK2));
		s.accept(new SpecificAutoMarkRequest(nonMonitorPlayers.get(2), MarkerSign.ATTACK3));
		s.accept(new SpecificAutoMarkRequest(nonMonitorPlayers.get(3), MarkerSign.ATTACK4));
		s.accept(new SpecificAutoMarkRequest(nonMonitorPlayers.get(4), MarkerSign.ATTACK5));

		// match monitor hit
		s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x7B6B, 0x7B6C));
		s.waitMs(1000);
		s.accept(new ClearAutoMarkRequest());
	}
});